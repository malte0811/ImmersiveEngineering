/*
 * BluSunrize
 * Copyright (c) 2017
 *
 * This code is licensed under "Blu's License of Common Sense"
 * Details can be found in the license file in the root folder of this project
 */

package malte0811.routing;

import it.unimi.dsi.fastutil.objects.Object2IntMap;
import it.unimi.dsi.fastutil.objects.Object2IntOpenHashMap;
import malte0811.routing.Layout.EnumDirection;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;

public class Puzzle
{
	public final Layout solved;
	public final Layout unsolved;

	public Puzzle(Layout solved, Layout unsolved)
	{
		this.solved = solved;
		this.unsolved = unsolved;
	}

	//TODO spaces between different traces?
	@SuppressWarnings("unchecked")
	public static Puzzle generate(int sizeX, int sizeY, int netCount, int pinsPerNet)
	{
		Layout solved = new Layout(sizeX, sizeY);
		Layout unsolved = new Layout(sizeX, sizeY);
		List<int[]>[] nets = new List[netCount];
		Random r = new Random();//12);//TODO remove seed!
		for(int i = 0; i < netCount; ++i)
		{
			nets[i] = new ArrayList<>();
			int[] insert = new int[2];
			do
			{
				randomize(insert, sizeX, sizeY, r);
			} while(!solved.hasColor(insert[0], insert[1], 0, false));
			nets[i].add(insert);
			Rectangle pin = new Rectangle(insert, new int[]{insert[0]+1, insert[1]+1});
			solved.addRect(pin, i+1);
			unsolved.addRect(pin, i+1);
		}
		for(int i = 1; i < pinsPerNet; ++i)
		{
			for(int j = 0; j < netCount; j++)
			{
				int[] pos = nets[j].get(r.nextInt(nets[j].size()));
				int maxIt = r.nextInt(5)+2;//TODO parameter?
				EnumDirection lastDir = null;
				for(int k = 0; k < maxIt; ++k)
				{
					Object2IntMap<EnumDirection> possible = new Object2IntOpenHashMap<>();
					for(EnumDirection dir : EnumDirection.values())
					{//TODO make a constant for that
						if(lastDir!=dir)
						{
							int maxInDir = solved.getDistToIntersect(pos[0], pos[1], dir, 0, j+1);
							if(maxInDir > 0)
							{
								possible.put(dir, maxInDir);
							}
						}
					}
					if(possible.isEmpty())
					{
						break;
					}
					EnumDirection[] possibleArr = possible.keySet().toArray(new EnumDirection[0]);
					EnumDirection chosen = possibleArr[r.nextInt(possibleArr.length)];
					lastDir = chosen;
					int minDist = Math.min(possible.getInt(chosen)/2, 4);
					int distance = r.nextInt(possible.getInt(chosen)-minDist)+minDist;
					Rectangle rect;
					if(chosen.x==0)
					{
						rect = new Rectangle(pos[0], pos[0]+1,
								pos[1]+chosen.startPoint(distance), pos[1]+chosen.endPoint(distance));
					}
					else
					{
						rect = new Rectangle(pos[0]+chosen.startPoint(distance), pos[0]+chosen.endPoint(distance),
								pos[1], pos[1]+1);
					}
					int[] end = new int[]{pos[0]+chosen.x*distance, pos[1]+chosen.y*distance};
					List<int[]> currNet = nets[j];
					rect.forEachPoint((x, y) -> currNet.add(new int[]{x, y}));
					solved.addRect(rect, j+1);
					pos = end;
				}
				unsolved.addRect(new Rectangle(pos, new int[]{pos[0]+1, pos[1]+1}), j+1);
			}
		}
		return new Puzzle(solved, unsolved);
	}

	private static void randomize(int[] pair, int maxX, int maxY, Random r)
	{
		pair[0] = r.nextInt(maxX);
		pair[1] = r.nextInt(maxY);
	}
}
