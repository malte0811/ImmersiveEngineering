/*
 * BluSunrize
 * Copyright (c) 2017
 *
 * This code is licensed under "Blu's License of Common Sense"
 * Details can be found in the license file in the root folder of this project
 */

package blusunrize.immersiveengineering.common.research.routing;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.function.BiConsumer;

public class Rectangle
{
	public final int[] min;
	public final int[] max;

	public Rectangle(int[] min, int[] max)
	{
		this.min = min;
		this.max = max;
	}

	public Rectangle(int xMin, int xMax, int yMin, int yMax)
	{
		this(new int[]{Math.min(xMin, xMax), Math.min(yMin, yMax)},
				new int[]{Math.max(xMax, xMin), Math.max(yMax, yMin)});
	}

	public Rectangle(int[] pos)
	{
		this(pos, new int[]{pos[0]+1, pos[1]+1});
	}

	public boolean intersects(Rectangle r)
	{
		for(int i = 0; i < 2; ++i)
		{
			if(r.min[i] > max[i]||min[i] > r.max[i])
			{
				return false;
			}
		}
		return true;
	}

	public Rectangle intersect(Rectangle other)
	{
		if(!intersects(other))
			return null;
		int[] newMax = new int[2];
		int[] newMin = new int[2];
		for(int i = 0; i < 2; ++i)
		{
			newMax[i] = Math.min(max[i], other.max[i]);
			newMin[i] = Math.max(min[i], other.min[i]);
		}
		return new Rectangle(newMin, newMax);
	}

	public Collection<Rectangle> split(Rectangle r)
	{
		Collection<Rectangle> ret = new ArrayList<>();
		int above = Math.max(0, max[1]-r.max[1]);
		int below = Math.max(0, r.min[1]-min[1]);
		int right = Math.max(0, max[0]-r.max[0]);
		int left = Math.max(0, r.min[0]-min[0]);
		if(above > 0)
		{
			ret.add(new Rectangle(min[0], max[0], max[1]-above, max[1]));
		}
		if(below > 0)
		{
			ret.add(new Rectangle(min[0], max[0], min[1], min[1]+below));
		}
		if(left > 0)
		{
			ret.add(new Rectangle(min[0], min[0]+left, min[1]+below, max[1]-above));
		}
		if(right > 0)
		{
			ret.add(new Rectangle(max[0]-right, max[0], min[1]+below, max[1]-above));
		}
		return ret;
	}

	@Override
	public String toString()
	{
		return "Rect: ["+min[0]+", "+max[0]+")x["+min[1]+", "+max[1]+")";
	}

	public int area()
	{
		int ret = 1;
		for(int i = 0; i < 2; ++i)
		{
			ret *= max[i]-min[i];
		}
		return ret;
	}

	public void forEachPoint(BiConsumer<Integer, Integer> out)
	{
		for(int x = min[0]; x < max[0]; ++x)
		{
			for(int y = min[1]; y < max[1]; ++y)
			{
				out.accept(x, y);
			}
		}
	}

	@Override
	public int hashCode()
	{
		final int prime = 31;
		int result = 1;
		result = prime*result+Arrays.hashCode(max);
		result = prime*result+Arrays.hashCode(min);
		return result;
	}

	@Override
	public boolean equals(Object obj)
	{
		if(this==obj)
			return true;
		if(obj==null)
			return false;
		if(getClass()!=obj.getClass())
			return false;
		Rectangle other = (Rectangle)obj;
		return Arrays.equals(max, other.max)&&Arrays.equals(min, other.min);
	}


}
