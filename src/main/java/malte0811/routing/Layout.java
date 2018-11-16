/*
 * BluSunrize
 * Copyright (c) 2017
 *
 * This code is licensed under "Blu's License of Common Sense"
 * Details can be found in the license file in the root folder of this project
 */

package malte0811.routing;

import it.unimi.dsi.fastutil.ints.Int2IntMap;
import it.unimi.dsi.fastutil.objects.Object2IntMap;
import it.unimi.dsi.fastutil.objects.Object2IntOpenHashMap;

import java.awt.image.BufferedImage;
import java.util.Collection;
import java.util.HashSet;
import java.util.Set;

import static java.awt.image.BufferedImage.TYPE_INT_RGB;

public class Layout
{
	private final Rectangle[][] containingRects;
	private final Object2IntMap<Rectangle> types = new Object2IntOpenHashMap<>();

	public Layout(int xSize, int ySize)
	{
		containingRects = new Rectangle[xSize][ySize];
		Rectangle all = new Rectangle(0, xSize, 0, ySize);
		addRectInternal(all, 0);
	}

	private void addRectInternal(Rectangle r, int type)
	{
		r.forEachPoint((x, y) -> containingRects[x][y] = r);
		types.put(r, type);
	}

	public void addRect(Rectangle r, int type)
	{
		Set<Rectangle> known = new HashSet<>();
		r.forEachPoint((x, y) -> {
			if(!known.contains(containingRects[x][y]))
			{
				known.add(containingRects[x][y]);
				Collection<Rectangle> newRects = containingRects[x][y].split(r);
				int prevType = types.get(containingRects[x][y]);
				for(Rectangle newR : newRects)
				{
					addRectInternal(newR, prevType);
				}
			}
		});
		addRectInternal(r, type);
	}

	public int getDistToIntersect(int xStart, int yStart, EnumDirection dir, int ignored, int sideProhibited)
	{
		xStart += dir.x;
		yStart += dir.y;
		int dist = 0;
		EnumDirection ort1 = dir.ort1();
		EnumDirection ort2 = dir.ort2();
		assert ort1!=null&&ort2!=null;
		while(hasColor(xStart, yStart, ignored, false)
				&&!hasColor(xStart+ort1.x, yStart+ort1.y, sideProhibited, true)
				&&!hasColor(xStart+ort2.x, yStart+ort2.y, sideProhibited, true))
		{
			xStart += dir.x;
			yStart += dir.y;
			++dist;
		}
		return dist;
	}

	public BufferedImage toImage(Int2IntMap colors)
	{
		BufferedImage ret = new BufferedImage(containingRects.length, containingRects[0].length,
				TYPE_INT_RGB);
		for(int x = 0; x < ret.getWidth(); ++x)
		{
			for(int y = 0; y < ret.getHeight(); ++y)
			{
				ret.setRGB(x, y, colors.get(types.get(containingRects[x][y])));
			}
		}
		return ret;
	}

	public boolean hasColor(int x, int y, int type, boolean def)
	{
		if(x < 0||x >= containingRects.length||y < 0||y >= containingRects[0].length)
			return def;
		return types.getInt(containingRects[x][y])==type;
	}

	public enum EnumDirection
	{
		UP(0, 1),
		DOWN(0, -1),
		LEFT(-1, 0),
		RIGHT(1, 0);
		final int x, y;

		EnumDirection(int x, int y)
		{
			this.x = x;
			this.y = y;
		}

		public int startPoint(int distance)
		{
			int d = x+y;
			if(d==1)
			{
				return 1;
			}
			else if(d==-1)
			{
				return -distance;
			}
			return 0;
		}

		public int endPoint(int distance)
		{
			int d = x+y;
			if(d==1)
			{
				return distance+1;
			}
			else if(d==-1)
			{
				return 0;
			}
			return 0;
		}

		public EnumDirection ort1()
		{
			switch(this)
			{
				case UP:
					return DOWN.ort2();
				case DOWN:
					return LEFT;
				case LEFT:
					return RIGHT.ort2();
				case RIGHT:
					return UP;
			}
			return null;
		}

		public EnumDirection ort2()
		{
			switch(this)
			{
				case UP:
					return DOWN.ort1();
				case DOWN:
					return RIGHT;
				case LEFT:
					return RIGHT.ort1();
				case RIGHT:
					return DOWN;
			}
			return null;
		}
	}
}
