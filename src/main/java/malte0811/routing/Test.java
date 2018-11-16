/*
 * BluSunrize
 * Copyright (c) 2017
 *
 * This code is licensed under "Blu's License of Common Sense"
 * Details can be found in the license file in the root folder of this project
 */

package malte0811.routing;

import it.unimi.dsi.fastutil.ints.Int2IntMap;
import it.unimi.dsi.fastutil.ints.Int2IntOpenHashMap;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;

public class Test
{
	public static void main(String[] args) throws IOException
	{
		final int size = 20;
		Puzzle l = Puzzle.generate(size, size, 4, 5);
		Int2IntMap colors = new Int2IntOpenHashMap();
		colors.put(0, 0xffffff);
		colors.put(1, 0xff0000);
		colors.put(2, 0x0000ff);
		colors.put(3, 0x00ff00);
		colors.put(4, 0xffff00);
		BufferedImage out = l.solved.toImage(colors);
		ImageIO.write(out, "png", new File("./solved.png"));
		out = l.unsolved.toImage(colors);
		ImageIO.write(out, "png", new File("./unsolved.png"));
	}
}
