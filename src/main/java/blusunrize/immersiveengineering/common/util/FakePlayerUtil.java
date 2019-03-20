/*
 * BluSunrize
 * Copyright (c) 2017
 *
 * This code is licensed under "Blu's License of Common Sense"
 * Details can be found in the license file in the root folder of this project
 */

package blusunrize.immersiveengineering.common.util;

import com.mojang.authlib.GameProfile;
import net.minecraft.world.World;
import net.minecraft.world.WorldServer;
import net.minecraftforge.common.util.FakePlayer;
import net.minecraftforge.common.util.FakePlayerFactory;
import net.minecraftforge.event.world.WorldEvent;
import net.minecraftforge.fml.common.Mod.EventBusSubscriber;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;

import java.util.Map;
import java.util.UUID;
import java.util.WeakHashMap;

@EventBusSubscriber
public class FakePlayerUtil
{
	private static GameProfile IE_PROFILE = new GameProfile(UUID.fromString("99562b85-bd1a-4ded-bb1a-c307bf0c0133"), "[ImmersiveEngineering]");
	private static Map<World, FakePlayer> fakePlayerInstances = new WeakHashMap<>();

	public static FakePlayer getAnyFakePlayer()
	{
		if(fakePlayerInstances.isEmpty())
			return null;
		else
			return fakePlayerInstances.values().iterator().next();
	}

	public static FakePlayer getFakePlayer(World w)
	{
		return fakePlayerInstances.get(w);
	}

	@SubscribeEvent
	public static void onLoad(WorldEvent.Load ev)
	{
		World world = ev.getWorld();
		if(world instanceof WorldServer)
			fakePlayerInstances.put(world, FakePlayerFactory.get((WorldServer)world, IE_PROFILE));
	}

}
