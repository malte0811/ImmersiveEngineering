package blusunrize.immersiveengineering.common.blocks.metal;

import blusunrize.immersiveengineering.ImmersiveEngineering;
import blusunrize.immersiveengineering.api.ApiUtils;
import blusunrize.immersiveengineering.api.IEEnums.SideConfig;
import blusunrize.immersiveengineering.api.Lib;
import blusunrize.immersiveengineering.api.energy.immersiveflux.FluxStorage;
import blusunrize.immersiveengineering.api.tool.ITeslaEntity;
import blusunrize.immersiveengineering.common.Config.IEConfig;
import blusunrize.immersiveengineering.common.blocks.IEBlockInterfaces.IBlockBounds;
import blusunrize.immersiveengineering.common.blocks.IEBlockInterfaces.IDirectionalTile;
import blusunrize.immersiveengineering.common.blocks.IEBlockInterfaces.IHammerInteraction;
import blusunrize.immersiveengineering.common.blocks.IEBlockInterfaces.IHasDummyBlocks;
import blusunrize.immersiveengineering.common.blocks.TileEntityIEBase;
import blusunrize.immersiveengineering.common.util.*;
import blusunrize.immersiveengineering.common.util.EnergyHelper.IEForgeEnergyWrapper;
import blusunrize.immersiveengineering.common.util.EnergyHelper.IIEInternalFluxHandler;
import blusunrize.immersiveengineering.common.util.IEDamageSources.TeslaDamageSource;
import blusunrize.immersiveengineering.common.util.network.MessageTileSync;
import com.google.common.collect.ArrayListMultimap;
import net.minecraft.block.state.IBlockState;
import net.minecraft.client.Minecraft;
import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityLivingBase;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.potion.PotionEffect;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.EnumFacing;
import net.minecraft.util.EnumFacing.Axis;
import net.minecraft.util.ITickable;
import net.minecraft.util.SoundCategory;
import net.minecraft.util.math.AxisAlignedBB;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Vec3d;
import net.minecraft.util.text.TextComponentTranslation;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;

import javax.annotation.Nonnull;
import java.util.List;
import java.util.stream.Collectors;

import static blusunrize.immersiveengineering.common.util.Utils.RAND;

public class TileEntityTeslaCoil extends TileEntityIEBase implements ITickable, IIEInternalFluxHandler, IHasDummyBlocks, IDirectionalTile, IBlockBounds, IHammerInteraction
{
	public boolean dummy = false;
	public FluxStorage energyStorage = new FluxStorage(48000);
	public boolean redstoneControlInverted = false;
	public EnumFacing facing = EnumFacing.UP;
	public boolean lowPower = false;
	private Vec3d soundPos = null;
	@SideOnly(Side.CLIENT)
	public static ArrayListMultimap<BlockPos, LightningAnimation> effectMap;

	@Override
	public void update()
	{
		ApiUtils.checkForNeedlessTicking(this);
		if (dummy)
			return;
		synchronized (this)
		{
			if (world.isRemote && soundPos != null)
			{
				world.playSound(soundPos.x, soundPos.y, soundPos.z, IESounds.tesla, SoundCategory.BLOCKS, 2.5F, 0.5F + RAND.nextFloat(), true);
				soundPos = null;
			}
		}
		if (world.isRemote && effectMap.containsKey(pos))
			effectMap.get(pos).removeIf(LightningAnimation::tick);

		int timeKey = getPos().getX() ^ getPos().getZ();
		int energyDrain = IEConfig.Machines.teslacoil_consumption;
		if (lowPower)
			energyDrain /= 2;
		if (world.getTotalWorldTime() % 32 == (timeKey & 31) && canRun(energyDrain))
		{
			if (!world.isRemote)
				this.energyStorage.extractEnergy(energyDrain, false);

			double radius = 6;
			if (lowPower)
				radius /= 2;
			AxisAlignedBB aabbSmall = new AxisAlignedBB(getPos().getX() + .5 - radius, getPos().getY() + .5 - radius, getPos().getZ() + .5 - radius, getPos().getX() + .5 + radius, getPos().getY() + .5 + radius, getPos().getZ() + .5 + radius);
			AxisAlignedBB aabb = aabbSmall.grow(radius / 2);
			List<Entity> targetsAll = world.getEntitiesWithinAABB(Entity.class, aabb);
			if (!world.isRemote)
				for (Entity e : targetsAll)
					if (e instanceof ITeslaEntity)
						((ITeslaEntity) e).onHit(this, lowPower);
			List<Entity> targets = targetsAll.stream().filter((e) -> (e instanceof EntityLivingBase && aabbSmall.intersects(e.getEntityBoundingBox()))).collect(Collectors.toList());
			if (!targets.isEmpty())
			{
				TeslaDamageSource dmgsrc = IEDamageSources.causeTeslaDamage(IEConfig.Machines.teslacoil_damage, lowPower);
				int randomTarget = RAND.nextInt(targets.size());
				EntityLivingBase target = (EntityLivingBase) targets.get(randomTarget);
				if (target != null)
				{
					if (!world.isRemote)
					{
						energyDrain = IEConfig.Machines.teslacoil_consumption_active;
						if (lowPower)
							energyDrain /= 2;
						if (energyStorage.extractEnergy(energyDrain, true) == energyDrain)
						{
							energyStorage.extractEnergy(energyDrain, false);
							if (dmgsrc.apply(target))
							{
								int prevFire = target.fire;
								target.fire = 1;
								target.addPotionEffect(new PotionEffect(IEPotions.stunned, 128));
								target.fire = prevFire;
							}
							this.sendRenderPacket(target);
						}
					}
				}
			}
			else if (!world.isRemote && world.getTotalWorldTime() % 128 == (timeKey & 127))
			{
				//target up to 4 blocks away
				double tV = (RAND.nextDouble() - .5) * 8;
				double tH = (RAND.nextDouble() - .5) * 8;
				if (lowPower)
				{
					tV /= 2;
					tH /= 2;
				}
				//Minimal distance to the coil is 2 blocks
				tV += tV < 0 ? -2 : 2;
				tH += tH < 0 ? -2 : 2;

				BlockPos targetBlock = getPos().add(facing.getAxis() == Axis.X ? 0 : tH, facing.getAxis() == Axis.Y ? 0 : tV, facing.getAxis() == Axis.Y ? tV : facing.getAxis() == Axis.X ? tH : 0);
				double tL = 0;
				boolean targetFound = false;
				if (!world.isAirBlock(targetBlock))
				{
					IBlockState state = world.getBlockState(targetBlock);
					AxisAlignedBB blockBounds = state.getBoundingBox(world, targetBlock);
					//					ty = (blockY-getPos().getY())+state.getBlock().getBlockBoundsMaxY();
					if (facing == EnumFacing.UP)
						tL = targetBlock.getY() - getPos().getY() + blockBounds.maxY;
					else if (facing == EnumFacing.DOWN)
						tL = targetBlock.getY() - getPos().getY() + blockBounds.minY;
					else if (facing == EnumFacing.NORTH)
						tL = targetBlock.getZ() - getPos().getZ() + blockBounds.minZ;
					else if (facing == EnumFacing.SOUTH)
						tL = targetBlock.getZ() - getPos().getZ() + blockBounds.maxZ;
					else if (facing == EnumFacing.WEST)
						tL = targetBlock.getX() - getPos().getX() + blockBounds.minX;
					else
						tL = targetBlock.getX() - getPos().getX() + blockBounds.maxX;
					targetFound = true;
				}
				else
				{
					boolean positiveFirst = RAND.nextBoolean();
					for (int i = 0; i < 2; i++)
					{
						for (int ll = 0; ll <= 6; ll++)
						{
							BlockPos targetBlock2 = targetBlock.offset(positiveFirst ? facing : facing.getOpposite(), ll);
							if (!world.isAirBlock(targetBlock2))
							{
								IBlockState state = world.getBlockState(targetBlock2);
								AxisAlignedBB blockBounds = state.getBoundingBox(world, targetBlock2);
								tL = facing.getAxis() == Axis.Y ? (targetBlock2.getY() - getPos().getY()) : facing.getAxis() == Axis.Z ? (targetBlock2.getZ() - getPos().getZ()) : (targetBlock2.getZ() - getPos().getZ());
								EnumFacing tempF = positiveFirst ? facing : facing.getOpposite();
								if (tempF == EnumFacing.UP)
									tL += blockBounds.maxY;
								else if (tempF == EnumFacing.DOWN)
									tL += blockBounds.minY;
								else if (tempF == EnumFacing.NORTH)
									tL += blockBounds.minZ;
								else if (tempF == EnumFacing.SOUTH)
									tL += blockBounds.maxZ;
								else if (tempF == EnumFacing.WEST)
									tL += blockBounds.minX;
								else
									tL += blockBounds.maxX;
								targetFound = true;
								break;
							}
						}
						if (targetFound)
							break;
						positiveFirst = !positiveFirst;
					}
				}
				if (targetFound)
					sendFreePacket(tL, tH, tV);
			}
			this.markDirty();
		}
	}

	protected void sendRenderPacket(Entity target)
	{
		NBTTagCompound tag = new NBTTagCompound();
		tag.setInteger("targetEntity", target.getEntityId());
		ImmersiveEngineering.packetHandler.sendToAll(new MessageTileSync(this, tag));
	}

	protected void sendFreePacket(double tL, double tH, double tV)
	{
		NBTTagCompound tag = new NBTTagCompound();
		tag.setDouble("tL", tL);
		tag.setDouble("tV", tV);
		tag.setDouble("tH", tH);
		ImmersiveEngineering.packetHandler.sendToAll(new MessageTileSync(this, tag));
	}

	@Override
	public void receiveMessageFromServer(NBTTagCompound message)
	{
		if (message.hasKey("targetEntity"))
		{
			Entity target = world.getEntityByID(message.getInteger("targetEntity"));
			if (target instanceof EntityLivingBase)
			{
				double dx = target.posX - getPos().getX();
				double dy = target.posY - getPos().getY();
				double dz = target.posZ - getPos().getZ();

				EnumFacing f = null;
				if (facing.getAxis() == Axis.Y)
				{
					if (Math.abs(dz) > Math.abs(dx))
						f = dz < 0 ? EnumFacing.NORTH : EnumFacing.SOUTH;
					else
						f = dx < 0 ? EnumFacing.WEST : EnumFacing.EAST;
				}
				else if (facing.getAxis() == Axis.Z)
				{
					if (Math.abs(dy) > Math.abs(dx))
						f = dy < 0 ? EnumFacing.DOWN : EnumFacing.UP;
					else
						f = dx < 0 ? EnumFacing.WEST : EnumFacing.EAST;
				}
				else
				{
					if (Math.abs(dy) > Math.abs(dz))
						f = dy < 0 ? EnumFacing.DOWN : EnumFacing.UP;
					else
						f = dz < 0 ? EnumFacing.NORTH : EnumFacing.SOUTH;
				}
				double verticalOffset = 1 + RAND.nextDouble() * .25;
				Vec3d coilPos = new Vec3d(getPos()).addVector(.5, .5, .5);
				//Vertical offset
				coilPos = coilPos.addVector(facing.getFrontOffsetX() * verticalOffset, facing.getFrontOffsetY() * verticalOffset, facing.getFrontOffsetZ() * verticalOffset);
				//offset to direction
				if (f != null)
				{
					coilPos = coilPos.addVector(f.getFrontOffsetX() * .375, f.getFrontOffsetY() * .375, f.getFrontOffsetZ() * .375);
					//random side offset
					f = f.rotateAround(facing.getAxis());
					double dShift = (RAND.nextDouble() - .5) * .75;
					coilPos = coilPos.addVector(f.getFrontOffsetX() * dShift, f.getFrontOffsetY() * dShift, f.getFrontOffsetZ() * dShift);
				}

				addAnimation(new LightningAnimation(coilPos, target.getPositionVector()));
				synchronized (this)
				{
					soundPos = coilPos;
				}
			}
		}
		else if (message.hasKey("tL"))
			initFreeStreamer(message.getDouble("tL"), message.getDouble("tV"), message.getDouble("tH"));
	}

	public void initFreeStreamer(double tL, double tV, double tH)
	{
		double tx = facing.getAxis() == Axis.X ? tL : tH;
		double ty = facing.getAxis() == Axis.Y ? tL : tV;
		double tz = facing.getAxis() == Axis.Y ? tV : facing.getAxis() == Axis.X ? tH : tL;

		EnumFacing f = null;
		if (facing.getAxis() == Axis.Y)
		{
			if (Math.abs(tz) > Math.abs(tx))
				f = tz < 0 ? EnumFacing.NORTH : EnumFacing.SOUTH;
			else
				f = tx < 0 ? EnumFacing.WEST : EnumFacing.EAST;
		}
		else if (facing.getAxis() == Axis.Z)
		{
			if (Math.abs(ty) > Math.abs(tx))
				f = ty < 0 ? EnumFacing.DOWN : EnumFacing.UP;
			else
				f = tx < 0 ? EnumFacing.WEST : EnumFacing.EAST;
		}
		else
		{
			if (Math.abs(ty) > Math.abs(tz))
				f = ty < 0 ? EnumFacing.DOWN : EnumFacing.UP;
			else
				f = tz < 0 ? EnumFacing.NORTH : EnumFacing.SOUTH;
		}

		double verticalOffset = 1 + RAND.nextDouble() * .25;
		Vec3d coilPos = new Vec3d(getPos()).addVector(.5, .5, .5);
		//Vertical offset
		coilPos = coilPos.addVector(facing.getFrontOffsetX() * verticalOffset, facing.getFrontOffsetY() * verticalOffset, facing.getFrontOffsetZ() * verticalOffset);
		//offset to direction
		coilPos = coilPos.addVector(f.getFrontOffsetX() * .375, f.getFrontOffsetY() * .375, f.getFrontOffsetZ() * .375);
		//random side offset
		f = f.rotateAround(facing.getAxis());
		double dShift = (RAND.nextDouble() - .5) * .75;
		coilPos = coilPos.addVector(f.getFrontOffsetX() * dShift, f.getFrontOffsetY() * dShift, f.getFrontOffsetZ() * dShift);
		addAnimation(new LightningAnimation(coilPos, new Vec3d(getPos()).addVector(tx, ty, tz)));
//		world.playSound(null, getPos(), IESounds.tesla, SoundCategory.BLOCKS,2.5f, .5f + Utils.RAND.nextFloat());
		world.playSound(getPos().getX(), getPos().getY(), getPos().getZ(), IESounds.tesla, SoundCategory.BLOCKS, 2.5F, 0.5F + RAND.nextFloat(), true);
	}

	private void addAnimation(LightningAnimation ani)
	{
		Minecraft.getMinecraft().addScheduledTask(() -> effectMap.put(getPos(), ani));
	}

	@Override
	public void readCustomNBT(NBTTagCompound nbt, boolean descPacket)
	{
		dummy = nbt.getBoolean("dummy");
		redstoneControlInverted = nbt.getBoolean("redstoneInverted");
		lowPower = nbt.getBoolean("lowPower");
		facing = EnumFacing.getFront(nbt.getInteger("facing"));
		energyStorage.readFromNBT(nbt);
	}

	@Override
	public void writeCustomNBT(NBTTagCompound nbt, boolean descPacket)
	{
		nbt.setBoolean("dummy", dummy);
		nbt.setBoolean("redstoneInverted", redstoneControlInverted);
		nbt.setBoolean("lowPower", lowPower);
		if (facing != null)
			nbt.setInteger("facing", facing.ordinal());
		energyStorage.writeToNBT(nbt);
	}

	@Override
	public float[] getBlockBounds()
	{
		if (!dummy)
			return null;
		switch (facing)
		{
			case DOWN:
				return new float[]{.125f, .125f, .125f, .875f, 1, .875f};
			case UP:
				return new float[]{.125f, 0, .125f, .875f, .875f, .875f};
			case NORTH:
				return new float[]{.125f, .125f, .125f, .875f, .875f, 1};
			case SOUTH:
				return new float[]{.125f, .125f, 0, .875f, .875f, .875f};
			case WEST:
				return new float[]{.125f, .125f, .125f, 1, .875f, .875f};
			case EAST:
				return new float[]{0, .125f, .125f, .875f, .875f, .875f};
		}
		return null;
	}

	AxisAlignedBB renderBB;

	@Override
	@SideOnly(Side.CLIENT)
	public AxisAlignedBB getRenderBoundingBox()
	{
		if (renderBB == null)
			renderBB = new AxisAlignedBB(getPos().add(-8, -8, -8), getPos().add(8, 8, 8));
		return renderBB;
	}

	@Override
	public boolean hammerUseSide(EnumFacing side, EntityPlayer player, float hitX, float hitY, float hitZ)
	{
		if (dummy)
		{
			TileEntity te = world.getTileEntity(getPos().offset(facing, -1));
			if (te instanceof TileEntityTeslaCoil)
				return ((TileEntityTeslaCoil) te).hammerUseSide(side, player, hitX, hitY, hitZ);
			return false;
		}
		if (player.isSneaking())
		{
			int energyDrain = IEConfig.Machines.teslacoil_consumption;
			if (lowPower)
				energyDrain /= 2;
			if (canRun(energyDrain))
				player.attackEntityFrom(IEDamageSources.causeTeslaPrimaryDamage(), Float.MAX_VALUE);
			else
			{
				lowPower = !lowPower;
				ChatUtils.sendServerNoSpamMessages(player, new TextComponentTranslation(Lib.CHAT_INFO + "tesla." + (lowPower ? "lowPower" : "highPower")));
				markDirty();
			}
		}
		else
		{
			redstoneControlInverted = !redstoneControlInverted;
			ChatUtils.sendServerNoSpamMessages(player, new TextComponentTranslation(Lib.CHAT_INFO + "rsControl." + (redstoneControlInverted ? "invertedOn" : "invertedOff")));
			markDirty();
			this.markContainingBlockForUpdate(null);
		}
		return true;
	}

	@Override
	public EnumFacing getFacing()
	{
		return facing;
	}

	@Override
	public void setFacing(EnumFacing facing)
	{
		this.facing = facing;
	}

	@Override
	public int getFacingLimitation()
	{
		return 0;
	}

	@Override
	public boolean mirrorFacingOnPlacement(EntityLivingBase placer)
	{
		return false;
	}

	@Override
	public boolean canHammerRotate(EnumFacing side, float hitX, float hitY, float hitZ, EntityLivingBase entity)
	{
		return false;
	}

	@Override
	public boolean canRotate(EnumFacing axis)
	{
		return false;
	}

	@Override
	public boolean isDummy()
	{
		return dummy;
	}

	@Override
	public void placeDummies(BlockPos pos, IBlockState state, EnumFacing side, float hitX, float hitY, float hitZ)
	{
		world.setBlockState(pos.offset(facing), state);
		((TileEntityTeslaCoil) world.getTileEntity(pos.offset(facing))).dummy = true;
		((TileEntityTeslaCoil) world.getTileEntity(pos.offset(facing))).facing = facing;
	}

	@Override
	public void breakDummies(BlockPos pos, IBlockState state)
	{
		for (int i = 0; i <= 1; i++)
			if (world.getTileEntity(getPos().offset(facing, dummy ? -1 : 0).offset(facing, i)) instanceof TileEntityTeslaCoil)
				world.setBlockToAir(getPos().offset(facing, dummy ? -1 : 0).offset(facing, i));
	}

	@Nonnull
	@Override
	public FluxStorage getFluxStorage()
	{
		if (dummy)
		{
			TileEntity te = world.getTileEntity(getPos().offset(facing, -1));
			if (te instanceof TileEntityTeslaCoil)
				return ((TileEntityTeslaCoil) te).getFluxStorage();
		}
		return energyStorage;
	}

	@Nonnull
	@Override
	public SideConfig getEnergySideConfig(EnumFacing facing)
	{
		return !dummy ? SideConfig.INPUT : SideConfig.NONE;
	}

	IEForgeEnergyWrapper[] wrappers = IEForgeEnergyWrapper.getDefaultWrapperArray(this);

	@Override
	public IEForgeEnergyWrapper getCapabilityWrapper(EnumFacing facing)
	{
		if (!dummy)
			return wrappers[facing == null ? 0 : facing.ordinal()];
		return null;
	}

	public boolean canRun(int energyDrain)
	{
		return (world.isBlockIndirectlyGettingPowered(getPos()) > 0 ^ redstoneControlInverted) && energyStorage.getEnergyStored() >= energyDrain;
	}

	public static class LightningAnimation
	{
		public Vec3d startPos;
		public Vec3d direction;
		private int lifeTimer = 40;//TODO
		public static final int MAX_DEPTH = 10;
		private static final double branchChance = .1;
		private static final double stopChance = 1D/(MAX_DEPTH*MAX_DEPTH);
		private static final double avgLength = .05;
		private static final double lengthStdDev = .02;

		public BinaryTree<StreamerNode> structure;

		public LightningAnimation(Vec3d startPos, Vec3d targetPos)
		{
			this.startPos = startPos;
			this.direction = targetPos.subtract(startPos);
		}

		public void updateRender()
		{
			if (structure == null)
			{
				BinaryTree<StreamerNode> tmp = new BinaryTree<>(new StreamerNode(new Vec3d(0, 1, 0), 0));
				createLightning(tmp);
				structure = tmp;
			} else {
				structure.inOrderTraverse((in) -> {
					double bias = (in.hashCode()>0)?1:-1;
					in.content.angle += RAND.nextGaussian()+bias;
					if (in.getLeft()==null) {
						if (RAND.nextDouble()*in.getDepthUp()<1) {
							BinaryTree<StreamerNode> t = new BinaryTree<>(newNode());
							in.setLeft(t);
							createLightning(t);
						}
					} else if (in.getRight()==null&&RAND.nextDouble()*in.getDepthUp()<2*branchChance) {
						BinaryTree<StreamerNode> t = new BinaryTree<>(newNode());
						in.setRight(t);
						createLightning(t);
					} else if (in.getDepth()<3&&RAND.nextDouble()*in.getDepthUp()>3) {
						in.setRight((BinaryTree<StreamerNode>) null);
						in.setLeft((BinaryTree<StreamerNode>) null);
					}
				});
			}
		}

		private void createLightning(BinaryTree<StreamerNode> node)
		{
			double branchChance = .2;
			double stopChance = 1D/(MAX_DEPTH*MAX_DEPTH);
			double avgLength = 1D/(node.getDepthUp()+1);
			double lengthStdDev = .1/(node.getDepthUp()+1);
			node.content.length = Math.max(0, avgLength+ RAND.nextGaussian()*lengthStdDev);
			if (RAND.nextDouble() > stopChance * node.getDepthUp() * node.getDepthUp())
			{
				BinaryTree<StreamerNode> t = new BinaryTree<>(newNode());
				node.setLeft(t);
				createLightning(t);
				if (RAND.nextDouble()<branchChance) {
					t = new BinaryTree<>(newNode());
					node.setRight(t);
					createLightning(t);
				}
			}
		}

		private StreamerNode newNode() {
			Vec3d axis = new Vec3d(RAND.nextDouble()-.5,
					RAND.nextDouble()-.5,
					RAND.nextDouble()-.5);
			axis.normalize();
			return new StreamerNode(axis, RAND.nextGaussian()*35);
		}

		public boolean tick()
		{
			updateRender();
			lifeTimer--;
			return lifeTimer <= 0;
		}

		public class StreamerNode
		{
			public Vec3d axis;
			public double angle;
			public double length;
			public StreamerNode(Vec3d axis, double angle)
			{
				this.axis = axis;
				this.angle = angle;
			}
		}
	}
}