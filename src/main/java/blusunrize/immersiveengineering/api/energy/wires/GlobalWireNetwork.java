/*
 * BluSunrize
 * Copyright (c) 2017
 *
 * This code is licensed under "Blu's License of Common Sense"
 * Details can be found in the license file in the root folder of this project
 */

package blusunrize.immersiveengineering.api.energy.wires;

import blusunrize.immersiveengineering.common.util.IELogger;
import blusunrize.immersiveengineering.common.wires.WireSyncManager;
import com.google.common.base.Preconditions;
import net.minecraft.entity.item.EntityItem;
import net.minecraft.nbt.NBTBase;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.nbt.NBTTagList;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.ITickable;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.ChunkPos;
import net.minecraft.world.World;
import net.minecraftforge.common.util.Constants.NBT;
import net.minecraftforge.fml.common.FMLCommonHandler;

import javax.annotation.Nonnull;
import java.util.*;
import java.util.function.Consumer;

public class GlobalWireNetwork implements ITickable
{
	private Map<ConnectionPoint, LocalWireNetwork> localNets = new HashMap<>();
	private WireCollisionData collisionData = new WireCollisionData(this);
	private World world;

	@Nonnull
	public static GlobalWireNetwork getNetwork(World w)
	{
		if(!w.hasCapability(NetHandlerCapability.NET_CAPABILITY, null))
			throw new RuntimeException("No net handler found for dimension "+w.provider.getDimension()+", remote: "+w.isRemote);
		return Objects.requireNonNull(w.getCapability(NetHandlerCapability.NET_CAPABILITY, null));
	}

	public GlobalWireNetwork(World w)
	{
		world = w;
	}

	public void addConnection(Connection conn)
	{
		ConnectionPoint posA = conn.getEndA();
		ConnectionPoint posB = conn.getEndB();
		IImmersiveConnectable iicA = getLocalNet(posA).getConnector(posA.getPosition());
		IImmersiveConnectable iicB = getLocalNet(posB).getConnector(posB.getPosition());
		LocalWireNetwork netA = getNullableLocalNet(posA);
		LocalWireNetwork netB = getNullableLocalNet(posB);
		Collection<ConnectionPoint> toSet = new ArrayList<>(2);
		LocalWireNetwork joined;
		if(netA==null&&netB==null)
		{
			IELogger.info("null-null");
			joined = new LocalWireNetwork(this);
			toSet.add(posA);
			toSet.add(posB);
			joined.loadConnector(posA, iicA);
			joined.loadConnector(posB, iicB);
		}
		else if(netA==null)
		{
			IELogger.info("null-non");
			toSet.add(posA);
			joined = netB;
			joined.loadConnector(posA, iicA);
		}
		else if(netB==null)
		{
			IELogger.info("non-null");
			toSet.add(posB);
			joined = netA;
			joined.loadConnector(posB, iicB);
		}
		else if(netA!=netB)
		{
			IELogger.logger.info("non-non-different: {} and {}", netA, netB);
			joined = netA.merge(netB);
			toSet = joined.getConnectionPoints();
		}
		else
		{
			IELogger.info("non-non-same");
			joined = netA;
		}
		IELogger.logger.info("Result: {}, to set: {}", joined, toSet);
		for(ConnectionPoint p : toSet)
			localNets.put(p, joined);
		joined.addConnection(conn);
		WireSyncManager.onConnectionAdded(conn, world);
		IELogger.logger.info("Validating after adding connection...");
		validate();
	}

	public void removeAllConnectionsAt(IImmersiveConnectable iic, Consumer<Connection> handler)
	{
		for(ConnectionPoint cp : iic.getConnectionPoints())
			removeAllConnectionsAt(cp, handler);
	}

	public void removeAllConnectionsAt(ConnectionPoint pos, Consumer<Connection> handler)
	{
		LocalWireNetwork net = getLocalNet(pos);
		List<Connection> conns = new ArrayList<>(net.getConnections(pos));
		//TODO batch removal method
		for(Connection conn : conns)
		{
			handler.accept(conn);
			removeConnection(conn);
		}
		IELogger.logger.info("Validating after removing all connections at {}...", pos);
		validate();
	}

	public void removeConnection(Connection c)
	{
		if(!world.isRemote)
			collisionData.removeConnection(c);
		LocalWireNetwork oldNet = localNets.get(c.getEndA());
		if(oldNet==null||oldNet.getConnector(c.getEndB())==null)
			return;
		oldNet.removeConnection(c);
		splitNet(oldNet);
		WireSyncManager.onConnectionRemoved(c, world);
	}

	public void removeAndDropConnection(Connection c, BlockPos dropAt)
	{
		removeConnection(c);
		double dx = dropAt.getX()+.5;
		double dy = dropAt.getY()+.5;
		double dz = dropAt.getZ()+.5;
		if(world.getGameRules().getBoolean("doTileDrops"))
			world.spawnEntity(new EntityItem(world, dx, dy, dz, c.type.getWireCoil(c)));
	}

	private void splitNet(LocalWireNetwork oldNet)
	{
		Collection<LocalWireNetwork> newNets = oldNet.split();
		for(LocalWireNetwork net : newNets)
			if(net!=oldNet)
				for(ConnectionPoint p : net.getConnectionPoints())
					localNets.put(p, net);
	}

	public void readFromNBT(NBTTagCompound nbt)
	{
		localNets.clear();
		NBTTagList locals = nbt.getTagList("locals", NBT.TAG_COMPOUND);
		for(NBTBase b : locals)
		{
			NBTTagCompound subnet = (NBTTagCompound)b;
			LocalWireNetwork localNet = new LocalWireNetwork(subnet, this);
			IELogger.logger.info("Loading net {}", localNet);
			for(ConnectionPoint p : localNet.getConnectionPoints())
				localNets.put(p, localNet);
		}
	}

	public NBTTagCompound writeToNBT()
	{
		NBTTagCompound ret = new NBTTagCompound();
		NBTTagList locals = new NBTTagList();
		Set<LocalWireNetwork> savedNets = Collections.newSetFromMap(new IdentityHashMap<>());
		for(LocalWireNetwork local : localNets.values())
			if(savedNets.add(local))
				locals.appendTag(local.writeToNBT());
		ret.setTag("locals", locals);
		return ret;
	}

	public LocalWireNetwork getLocalNet(BlockPos pos)
	{
		return getLocalNet(new ConnectionPoint(pos, 0));
	}

	public LocalWireNetwork getLocalNet(ConnectionPoint pos)
	{
		return localNets.computeIfAbsent(pos, p -> new LocalWireNetwork(this));
	}

	public LocalWireNetwork getNullableLocalNet(ConnectionPoint pos)
	{
		return localNets.get(pos);
	}

	public void removeConnector(IImmersiveConnectable iic)
	{
		IELogger.logger.info("Removing {}", iic);
		for(ConnectionPoint c : iic.getConnectionPoints())
		{
			IELogger.logger.info("Sub-point {}", c);
			LocalWireNetwork local = getNullableLocalNet(c);
			if(local!=null)
			{
				IELogger.logger.info("Removing");
				local.removeConnector(c.getPosition());
				localNets.remove(c);
				splitNet(local);
			}
		}
		IELogger.logger.info("Validating after removal...");
		validate();
	}

	public void onConnectorLoad(IImmersiveConnectable iic, World w)
	{
		boolean isNew = false;
		for(ConnectionPoint cp : iic.getConnectionPoints())
		{
			if(getNullableLocalNet(cp)==null)
				isNew = true;
			LocalWireNetwork local = getLocalNet(cp);
			local.loadConnector(cp, iic);
		}
		if(isNew)
		{
			for(Connection c : iic.getInternalConnections())
			{
				Preconditions.checkArgument(c.isInternal(), "Internal connection for "+iic+"was not marked as internal!");
				addConnection(c);
			}
		}
		for(ConnectionPoint cp : iic.getConnectionPoints())
			for(Connection c : getLocalNet(cp).getConnections(cp))
			{
				ConnectionPoint otherEnd = c.getOtherEnd(cp);
				LocalWireNetwork otherLocal = getNullableLocalNet(otherEnd);
				if(otherLocal!=null)
				{
					IImmersiveConnectable iicEnd = otherLocal.getConnector(otherEnd);
					if(!(iicEnd instanceof IICProxy))
					{
						c.generateCatenaryData(w);
						if(!w.isRemote)
						{
							IELogger.logger.info("Here: {}, other end: {}", iic, iicEnd);
							collisionData.addConnection(c);
						}
					}
				}
			}
		IELogger.logger.info("Validating after load...");
		validate();
	}

	public void onConnectorUnload(BlockPos pos, IImmersiveConnectable iic)
	{
		Set<LocalWireNetwork> added = new HashSet<>();
		for(ConnectionPoint connectionPoint : iic.getConnectionPoints())
		{
			LocalWireNetwork local = getLocalNet(connectionPoint);
			if(added.add(local))
				local.unloadConnector(pos, iic);
		}
		if(!world.isRemote)
			for(ConnectionPoint cp : iic.getConnectionPoints())
			for(Connection c : getLocalNet(cp).getConnections(cp))
				collisionData.removeConnection(c);
		IELogger.logger.info("Validating after unload...");
		validate();
	}

	@Override
	public void update()
	{
		Set<LocalWireNetwork> ticked = new HashSet<>();
		for(LocalWireNetwork net : localNets.values())
			if(ticked.add(net))
				net.update(world);
	}

	private void validate()
	{
		if(FMLCommonHandler.instance().getEffectiveSide().isClient()) return;
		localNets.values().stream().distinct().forEach(
				(local) -> {
					for(ConnectionPoint cp : local.getConnectionPoints())
					{
						if(localNets.get(cp)!=local)
							IELogger.logger.warn("{} has net {}, but is in net {}", cp, localNets.get(cp), local);
						else
							for(Connection c : local.getConnections(cp))
							{
								if(localNets.get(c.getOtherEnd(cp))!=local)
									IELogger.logger.warn("{} is connected to {}, but nets are {} and {}", cp,
											c.getOtherEnd(cp), localNets.get(c.getOtherEnd(cp)), local);
								else if(!local.getConnections(c.getOtherEnd(cp)).contains(c))
									IELogger.logger.warn("Connection {} from {} to {} is a diode!", c, cp,
											c.getOtherEnd(cp));
							}
					}
					for(BlockPos p : local.getConnectors())
						if(world.isBlockLoaded(p))
						{
							IImmersiveConnectable inNet = local.getConnector(p);
							TileEntity inWorld = world.getTileEntity(p);
							if(inNet!=inWorld)
								IELogger.logger.warn("Connector at {}: {} in Net, {} in World (Net is {})", p, inNet, inWorld, local);
						}
				}
		);
		IELogger.logger.info("Validated!");
	}

	public WireCollisionData getCollisionData()
	{
		return collisionData;
	}

	public Collection<ConnectionPoint> getAllConnectorsIn(ChunkPos pos)
	{
		//TODO better way of finding all connectors in a chunk
		Collection<ConnectionPoint> ret = new ArrayList<>();
		for(ConnectionPoint cp : localNets.keySet())
			if(pos.equals(new ChunkPos(cp.getPosition())))
				ret.add(cp);
		return ret;
	}
}