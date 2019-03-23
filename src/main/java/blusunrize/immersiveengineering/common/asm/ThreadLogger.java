/*
 * BluSunrize
 * Copyright (c) 2017
 *
 * This code is licensed under "Blu's License of Common Sense"
 * Details can be found in the license file in the root folder of this project
 */

package blusunrize.immersiveengineering.common.asm;

public class ThreadLogger
{
	private static boolean firstCall = true;
	public static void checkForThread(String expected)
	{
		if (firstCall)
		{
			System.out.println("Threading checks are active!");
			firstCall = false;
		}
		if(!expected.equals(Thread.currentThread().getName()))
			Thread.dumpStack();
	}
}