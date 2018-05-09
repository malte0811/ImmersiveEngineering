/*
 * BluSunrize
 * Copyright (c) 2017
 *
 * This code is licensed under "Blu's License of Common Sense"
 * Details can be found in the license file in the root folder of this project
 */

package blusunrize.immersiveengineering.common.blocks.multiblocks;

import blusunrize.immersiveengineering.ImmersiveEngineering;
import blusunrize.immersiveengineering.api.MultiblockHandler;
import blusunrize.immersiveengineering.api.MultiblockHandler.IMultiblock;
import net.minecraft.util.ResourceLocation;
import net.minecraft.util.math.BlockPos;

public class IEMultiblocks
{
	public static IMultiblock CRUSHER;
	public static void register()
	{
		CRUSHER = new MultiblockTemplate(new ResourceLocation(ImmersiveEngineering.MODID, "multiblocks/crusher"),
				new BlockPos(2, 1, 2));
		MultiblockHandler.registerMultiblock(CRUSHER);
	}
}
