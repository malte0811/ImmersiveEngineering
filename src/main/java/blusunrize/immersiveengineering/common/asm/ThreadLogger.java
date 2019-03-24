/*
 * BluSunrize
 * Copyright (c) 2017
 *
 * This code is licensed under "Blu's License of Common Sense"
 * Details can be found in the license file in the root folder of this project
 */

package blusunrize.immersiveengineering.common.asm;

import net.minecraft.world.NextTickListEntry;
import net.minecraft.world.WorldServer;
import net.minecraftforge.fml.common.ObfuscationReflectionHelper;

public class ThreadLogger
{
	private static boolean firstThreadCall = true, firstChangeCall = true;
	public static void checkForThread(String expected)
	{
		if(firstThreadCall)
		{
			System.out.println("Threading checks are active!");
			firstThreadCall = false;
		}
		if(!expected.equals(Thread.currentThread().getName()))
			Thread.dumpStack();
	}

	public static void verifyOrderChangeCall()
	{
		if(firstChangeCall)
		{
			System.out.println("Change caller checks are active!");
			firstChangeCall = false;
		}
		StackTraceElement[] stack = Thread.currentThread().getStackTrace();
		//0: getStackTrace, 1: verifyOrder, 2: set..., 3: caller
		StackTraceElement caller = stack[3];
		if(!"net.minecraft.world.WorldServer".equals(caller.getClassName()))
		{
			Thread.dumpStack();
		}
	}

	public static void logIfFalse(NextTickListEntry e, boolean success, WorldServer world, String info)
	{
		if(!success)
		{
			Object hash = ObfuscationReflectionHelper.getPrivateValue(WorldServer.class, world,
					"pendingTickListEntriesHashSet", "field_73064_N");
			Object tree = ObfuscationReflectionHelper.getPrivateValue(WorldServer.class, world,
					"pendingTickListEntriesTreeSet", "field_73065_O");
			new RuntimeException("Failure related to "+e+" at "+info+". Hash: "+hash+", tree: "+tree).printStackTrace();
		}
	}
}