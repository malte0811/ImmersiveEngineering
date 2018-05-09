/*
 * BluSunrize
 * Copyright (c) 2017
 *
 * This code is licensed under "Blu's License of Common Sense"
 * Details can be found in the license file in the root folder of this project
 */

package blusunrize.immersiveengineering.common.blocks.multiblocks;

import blusunrize.immersiveengineering.api.IEProperties;
import blusunrize.immersiveengineering.api.MultiblockHandler;
import blusunrize.immersiveengineering.api.crafting.IngredientStack;
import blusunrize.immersiveengineering.client.ClientUtils;
import blusunrize.immersiveengineering.common.IEContent;
import blusunrize.immersiveengineering.common.blocks.IEBlockInterfaces;
import blusunrize.immersiveengineering.common.blocks.IEBlockInterfaces.IDirectionalTile;
import blusunrize.immersiveengineering.common.blocks.metal.BlockTypes_MetalDecoration0;
import blusunrize.immersiveengineering.common.blocks.metal.BlockTypes_MetalMultiblock;
import blusunrize.immersiveengineering.common.util.IELogger;
import blusunrize.immersiveengineering.common.util.Utils;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableSet;
import net.minecraft.block.properties.IProperty;
import net.minecraft.block.state.IBlockState;
import net.minecraft.client.renderer.GlStateManager;
import net.minecraft.client.renderer.block.model.ItemCameraTransforms;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.init.Blocks;
import net.minecraft.item.ItemStack;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.EnumFacing;
import net.minecraft.util.Mirror;
import net.minecraft.util.ResourceLocation;
import net.minecraft.util.Rotation;
import net.minecraft.util.datafix.DataFixesManager;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Vec3i;
import net.minecraft.world.World;
import net.minecraft.world.WorldServer;
import net.minecraft.world.gen.structure.template.PlacementSettings;
import net.minecraft.world.gen.structure.template.Template;
import net.minecraft.world.gen.structure.template.TemplateManager;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.util.List;
import java.util.Set;

public class MultiblockTemplate implements MultiblockHandler.IMultiblock
{
	public static final TemplateManager RES_LOC_TEMPLATE_MANAGER = new TemplateManager("/dev/null/should not exist",
			DataFixesManager.createFixer());

	private final ResourceLocation loc;
	@Nullable
	private Template template;
	private IBlockState trigger = Blocks.AIR.getDefaultState();
	private BlockPos offset;

	public MultiblockTemplate(ResourceLocation loc, BlockPos offset)
	{
		this.loc = loc;
		this.offset = offset;
	}

	@Nonnull
	private Template getTemplate()
	{
		if (template==null)//TODO reset on resource reload (Does that even affect templates?)
		{
			template = RES_LOC_TEMPLATE_MANAGER.getTemplate(null, loc);
			List<Template.BlockInfo> blocks = template.blocks;
			for (int i = 0; i < blocks.size(); i++)
			{
				Template.BlockInfo info = blocks.get(i);
				if (info.pos.equals(offset))
					trigger = info.blockState;
				if (info.blockState==Blocks.AIR.getDefaultState())
				{
					blocks.remove(i);
					i--;
				}
			}
		}
		return template;
	}

	@Override
	public ResourceLocation getUniqueName()
	{
		return loc;
	}

	@Override
	public boolean isBlockTrigger(IBlockState state)
	{
		getTemplate();
		return Utils.areStatesEqual(state, trigger, ImmutableSet.of(), false);//TODO does this crash on a dedi server?
	}

	private static final List<Mirror> MIRROR_STATES = ImmutableList.of(Mirror.NONE, Mirror.LEFT_RIGHT);

	@Override
	public boolean createStructure(World world, BlockPos pos, EnumFacing side, EntityPlayer player)
	{
		Rotation rot = Utils.getRotationBetweenFacings(EnumFacing.NORTH, side.getOpposite());
		if (rot==null)
			return false;
		Template template = getTemplate();
		Mirror found = null;
		mirrorLoop:for (Mirror mirror:MIRROR_STATES)
		{
			PlacementSettings placeSet = new PlacementSettings().setMirror(mirror).setRotation(rot);
			BlockPos origin = pos.subtract(Template.transformedBlockPos(placeSet, offset));
			IELogger.info(rot+", "+mirror+", "+origin);
			for (Template.BlockInfo info:template.blocks)
			{
				BlockPos realRelPos = Template.transformedBlockPos(placeSet, info.pos);
				BlockPos here = origin.add(realRelPos);

				IBlockState inWorld = world.getBlockState(here);
				//Both states are "default"/non-actual states, since templates don't store the additional info (except in the TE data)
				if (!Utils.areStatesEqual(info.blockState, inWorld, ImmutableSet.of(), false))
				{
					IELogger.info("Cancel: {} at {} vs {} at {}/{}", info.blockState, info.pos, inWorld, here, realRelPos);
					continue mirrorLoop;
				}
				if (info.tileentityData!=null)
				{
					//Check facing using TE's
					TileEntity teStored = TileEntity.create(world, info.tileentityData);
					TileEntity teWorld = world.getTileEntity(here);
					if (teStored instanceof IDirectionalTile && teWorld instanceof IDirectionalTile)
					{
						IDirectionalTile dirStored = (IDirectionalTile) teStored;
						IDirectionalTile dirWorld = (IDirectionalTile) teWorld;
						if (dirStored.getFacing()!=dirWorld.getFacing())
						{
							IELogger.info("Cancel direction: {} vs {}", info.blockState, inWorld);
							continue mirrorLoop;
						}
					}
					//TODO any other checks
				}
			}
			found = mirror;
			break;
		}
		if (found==null)
			return false;
		IELogger.info("Would form!");
		return true;
	}

	@Override
	public List<Template.BlockInfo> getStructureManual()
	{
		return getTemplate().blocks;
	}

	@Override
	public Vec3i getSize()
	{
		return getTemplate().getSize();
	}

	//TODO All of this is crusher-specific!
	private static final IngredientStack[] materials = new IngredientStack[]{
			new IngredientStack("scaffoldingSteel", 10),
			new IngredientStack(new ItemStack(IEContent.blockMetalDecoration0, 1, BlockTypes_MetalDecoration0.RS_ENGINEERING.getMeta())),
			new IngredientStack(new ItemStack(IEContent.blockMetalDecoration0, 10, BlockTypes_MetalDecoration0.LIGHT_ENGINEERING.getMeta())),
			new IngredientStack("fenceSteel", 8),
			new IngredientStack(new ItemStack(Blocks.HOPPER, 9))};
	@Override
	public IngredientStack[] getTotalMaterials()
	{
		return materials;
	}

	@Override
	public boolean overwriteBlockRender(IBlockState stack, int iterator)
	{
		return false;
	}

	@Override
	public float getManualScale()
	{
		return 12;
	}

	@Override
	@SideOnly(Side.CLIENT)
	public boolean canRenderFormedStructure()
	{
		return true;
	}
	//@SideOnly(Side.CLIENT)
	private static ItemStack renderStack = ItemStack.EMPTY;
	@Override
	@SideOnly(Side.CLIENT)
	public void renderFormedStructure()
	{
		if(renderStack.isEmpty())
			renderStack = new ItemStack(IEContent.blockMetalMultiblock,1,BlockTypes_MetalMultiblock.CRUSHER.getMeta());
		GlStateManager.translate(1.5, 1.5, 2.5);
		GlStateManager.rotate(-45, 0, 1, 0);
		GlStateManager.rotate(-20, 1, 0, 0);
		GlStateManager.scale(5.5, 5.5, 5.5);

		GlStateManager.disableCull();
		ClientUtils.mc().getRenderItem().renderItem(renderStack, ItemCameraTransforms.TransformType.GUI);
		GlStateManager.enableCull();
	}
}
