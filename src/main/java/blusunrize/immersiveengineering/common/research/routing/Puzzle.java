/*
 * BluSunrize
 * Copyright (c) 2017
 *
 * This code is licensed under "Blu's License of Common Sense"
 * Details can be found in the license file in the root folder of this project
 */

package blusunrize.immersiveengineering.common.research.routing;

import blusunrize.immersiveengineering.common.research.routing.Layout.EnumDirection;

import java.util.*;
import java.util.function.Predicate;

public class Puzzle
{
	public final Layout solved;
	public final Layout unsolved;

	public Puzzle(Layout solved, Layout unsolved)
	{
		this.solved = solved;
		this.unsolved = unsolved;
	}

	public static Puzzle generate(int sizeX, int sizeY, int netCount, int pinsPerNet)
	{
		Layout solved = new Layout(sizeX, sizeY);
		Layout unsolved = new Layout(sizeX, sizeY);
		int[][] netRoots = new int[netCount][2];
		long seed = System.currentTimeMillis();
		Random r = new Random(seed);
		System.out.println(seed);
		//Geerate initial pins
		for(int i = 0; i < netCount; ++i)
		{
			int[] insert = new int[2];
			do
			{
				randomize(insert, sizeX, sizeY, r);
			} while(!solved.hasColor(insert[0], insert[1], 0, false));
			netRoots[i] = insert;
			Rectangle pin = new Rectangle(insert);
			solved.addRect(pin, i+1);
			unsolved.addRect(pin, i+1);
		}
		//Generate further pins and traces connecting them
		for(int i = 1; i < pinsPerNet; ++i)
		{
			for(int j = 0; j < netCount; j++)
			{
				int[] root = netRoots[j];
				int currColor = j+1;
				Predicate<int[]> isValidColor = pos -> {
					int color = solved.getColor(pos[0], pos[1], -1);
					return color==0||color==currColor;
				};
				final int[] lastWithNeighbour = new int[2];
				Predicate<int[]> hasNeighbour = pos -> {
					for(EnumDirection dir : EnumDirection.values())
					{
						if(solved.hasColor(pos[0]+dir.x, pos[1]+dir.y, currColor, false))
						{
							lastWithNeighbour[0] = pos[0];
							lastWithNeighbour[1] = pos[1];
							return true;
						}
					}
					return false;
				};
				int[][][] reachable = bfs(solved, root, isValidColor, pos -> false);
				List<int[]> reachableEmpty = new ArrayList<>();
				for(int x = 0; x < reachable.length; ++x)
				{
					for(int y = 0; y < reachable[x].length; ++y)
					{
						if(reachable[x][y]!=null&&solved.hasColor(x, y, 0, false))
						{
							if(!hasNeighbour.test(new int[]{x, y}))
							{//TODO one less alloc
								reachableEmpty.add(new int[]{x, y});
							}
						}
					}
				}
				if(reachableEmpty.isEmpty())
					continue;
				int[] newPin = reachableEmpty.get(r.nextInt(reachableEmpty.size()));
				int[][][] shortestPath = bfs(solved, newPin, isValidColor, hasNeighbour);
				unsolved.addRect(new Rectangle(newPin), currColor);
				//TODO figure out whether Rectangle is still necessary
				int[] curr = lastWithNeighbour;
				while(curr!=newPin)
				{
					solved.addRect(new Rectangle(curr), currColor);
					curr = shortestPath[curr[0]][curr[1]];
				}
			}
		}
		//Remove unneeded area from the sides of the layout
		for(EnumDirection dir : EnumDirection.values())
		{
			int layersToRemove = 0;
			boolean foundUsed = false;
			int max = dir.choose(solved.getWidth(), solved.getHeight());
			int otherMax = dir.choose(solved.getHeight(), solved.getWidth());
			while(!foundUsed)
			{
				for(int i = 0; i < max; ++i)
				{
					int other = dir.isPositive()?layersToRemove: (otherMax-layersToRemove-1);
					int x = dir.choose(i, other);
					int y = dir.choose(other, i);
					if(!solved.hasColor(x, y, 0, true))
					{
						foundUsed = true;
						break;
					}
				}
				if(!foundUsed)
					++layersToRemove;
			}

			int border = dir.isPositive()?0: otherMax;
			int nearBorder = dir.isPositive()?layersToRemove: (otherMax-layersToRemove);
			unsolved.addRect(
					new Rectangle(dir.choose(0, nearBorder),
							dir.choose(otherMax, border),
							dir.choose(nearBorder, 0),
							dir.choose(border, otherMax)), -1
			);
		}
		return new Puzzle(solved, unsolved);
	}

	private static void randomize(int[] pair, int maxX, int maxY, Random r)
	{
		pair[0] = r.nextInt(maxX);
		pair[1] = r.nextInt(maxY);
	}

	private static int[][][] bfs(Layout l, int[] start, Predicate<int[]> canUse, Predicate<int[]> shouldStop)
	{
		Queue<int[]> open = new LinkedList<>();
		int[][][] ret = new int[l.getWidth()][l.getHeight()][];
		open.add(start);
		ret[start[0]][start[1]] = start;
		while(!open.isEmpty())
		{
			int[] curr = open.poll();
			for(EnumDirection dir : EnumDirection.values())
			{
				int[] newPos = {
						curr[0]+dir.x,
						curr[1]+dir.y
				};
				if(canUse.test(newPos)&&ret[newPos[0]][newPos[1]]==null)
				{
					ret[newPos[0]][newPos[1]] = curr;
					if(shouldStop.test(newPos))
						return ret;
					open.add(newPos);
				}
			}
		}
		return ret;
	}
}
