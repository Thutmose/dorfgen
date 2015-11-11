package dorfgen.worldgen;

import static dorfgen.WorldGenerator.*;
import static net.minecraft.util.EnumFacing.*;

import java.awt.Color;
import java.util.Arrays;
import java.util.HashSet;

import javax.vecmath.Vector3d;

import dorfgen.WorldGenerator;
import dorfgen.conversion.DorfMap;
import dorfgen.conversion.SiteStructureGenerator;
import dorfgen.conversion.DorfMap.Site;
import dorfgen.conversion.DorfMap.WorldConstruction;
import dorfgen.conversion.Interpolator.BicubicInterpolator;
import dorfgen.conversion.Interpolator.CachedBicubicInterpolator;
import dorfgen.conversion.SiteStructureGenerator.RiverExit;
import dorfgen.conversion.SiteStructureGenerator.SiteStructures;
import net.minecraft.block.Block;
import net.minecraft.init.Blocks;
import net.minecraft.util.EnumFacing;
import net.minecraft.world.World;
import net.minecraft.world.biome.BiomeGenBase;

public class RiverMaker {
	public static BicubicInterpolator bicubicInterpolator = new BicubicInterpolator();
	public static DorfMap dorfs;
	
	public RiverMaker() {
		dorfs = WorldGenerator.instance.dorfs;
		// TODO Auto-generated constructor stub
	}

	public void makeRiversForChunk(World world, int chunkX, int chunkZ, Block[] blocks, BiomeGenBase[] biomes) {
		
//		if(true)
//			return;
//		
		int index;
		int x = (chunkX * 16 - WorldGenerator.shift.posX);
		int z = (chunkZ * 16 - WorldGenerator.shift.posZ);
		int x1, z1, h, w, r, b1, id;
		boolean water = false;
		double dx, dz, dx2, dz2;
		int n = 0;
		for (int i1 = 0; i1 < 16; i1++) {
			for (int k1 = 0; k1 < 16; k1++) {
				x1 = (x + i1);
				z1 = (z + k1);
				
				dx = (x + i1 - x1)/(double)scale;
				dz = (z + k1 - z1)/(double)scale;
				
				h = bicubicInterpolator.interpolate(WorldGenerator.instance.dorfs.elevationMap, x + i1, z + k1, scale);
				r = bicubicInterpolator.interpolate(WorldGenerator.instance.dorfs.riverMap, x + i1, z + k1, scale);
				
				id = biomes[i1+16*k1].biomeID;
				boolean river = isInRiver(x1, z1);
				if(!river)
					continue;
				int j = h-1;
				index = j << 0 | (i1) << 12 | (k1) << 8;
				blocks[index] = Blocks.water;
				index = j-- << 0 | (i1) << 12 | (k1) << 8;
				blocks[index] = Blocks.water;
				index = j-- << 0 | (i1) << 12 | (k1) << 8;
				blocks[index] = Blocks.water;
				
				//TODO make rivers that work with the new site code
				
			}
		}
	}
	
	static Color STRMAPRIVER = new Color(0, 192, 255);
	static EnumFacing[] DIRS = { EAST, WEST, NORTH, SOUTH };
	private static double[] DIR_TO_RELX = { ((double)scale), 0., ((double)scale)/2., ((double)scale)/2. };
	private static double[] DIR_TO_RELZ = { ((double)scale)/2., ((double)scale)/2., 0, ((double)scale) };

	private int dirToIndex(EnumFacing dir)
	{
		if(dir == EAST) return 0;
		if(dir == WEST) return 1;
		if(dir == NORTH) return 2;
		if(dir == SOUTH) return 3;
		return 0;
	}
	
	public static boolean[] getRiverDirection(int xAbs, int zAbs)
	{
		int pixelX = xAbs / (scale);
		int pixelZ = zAbs / (scale);
		
		boolean[] ret = new boolean[4];
		int b;
//		ret = new boolean[]{true,true,true,true};
		
//		if(!isRiver(pixelX, pixelZ))
//		{
//			return ret;
//		}
//		
//		if(isRiver(pixelX - 1, pixelZ))
//		{
//			ret[1] = true;
//		}
//		if(isRiver(pixelX + 1, pixelZ))
//		{
//			ret[0] = true;
//		}
//		if(isRiver(pixelX, pixelZ - 1))
//		{
//			ret[2] = true;
//		}
//		if(isRiver(pixelX, pixelZ + 1))
//		{
//			ret[3] = true;
//		}
		
		if(!isRiver(xAbs, zAbs))
		{
			return ret;
		}
		if(isRiver(xAbs - scale, zAbs))
		{
			ret[1] = true;
		}
		if(isRiver(xAbs + scale, zAbs))
		{
			ret[0] = true;
		}
		if(isRiver(xAbs, zAbs - scale))
		{
			ret[2] = true;
		}
		if(isRiver(xAbs, zAbs + scale))
		{
			ret[3] = true;
		}
		return ret;
	}
	
	private static boolean isRiver(int x, int z)
	{
    	int kx = x/scale;//Abs/(scale);
    	int kz = z/scale;//Abs/(scale);
    	int key = kx + 8192 * kz;
    	
    	
		int rgb = dorfs.structureMap[kx][kz];
		Color col1 = new Color(rgb);
		
		rgb = dorfs.riverMap[kx][kz];
//		int biome = dorfs.biomeInterpolator.interpolateBiome(dorfs.biomeMap, x, z, scale);
//		if(biome!= BiomeGenBase.river.biomeID)
//			return false;
		Color col2 = new Color(rgb);
    	
		Color WHITE = new Color(255,255,255);
		
		boolean river = col1.equals(STRMAPRIVER) || (!WHITE.equals(col2) && col2.getBlue() > 0);
		if(river)
			return river;
		
		HashSet<Site> ret = dorfs.sitesByCoord.get(key);
		
    	if(ret!=null)
    	{
    		for(Site s: ret)
    		{
    			if(!s.isInSite(x, z))
    				continue;
    			
    			SiteStructures structs = WorldGenerator.instance.structureGen.getStructuresForSite(s);
    			if(!structs.rivers.isEmpty())
    			{
    				for(RiverExit riv: structs.rivers)
    				{
    					int[] exit = riv.getEdgeMid(s, scale);
    					int dx = exit[0] - x;
    					int dz = exit[1] - z;
    					if(dx*dx + dz*dz < scale * scale / 4)
    					{
    						return true;
    					}
    				}
    			}
    		}
    	}
		
		return river;
	}
	
	public static boolean isInRiver(int x1, int z1)
	{
		int x = x1, z = z1;
		boolean river = false;
		DorfMap dorfs = WorldGenerator.instance.dorfs;
		int kx = x1/scale;
		int kz = z1/scale;
		int offset = scale/2;
    	int key = kx + 8192 * kz;
    	Site site;
    	HashSet<Site> ret = dorfs.sitesByCoord.get(key);
    	boolean hasRivers = false;
    	if(ret!=null)
    	{
    		for(Site s: ret)
    		{
    			if(!s.isInSite(x1, z1))
    				continue;
    			
    			SiteStructures structs = WorldGenerator.instance.structureGen.getStructuresForSite(s);
    			if(!structs.rivers.isEmpty())
    			{
    				hasRivers = true;
    				break;
    			}
    		}
    	}
		
		boolean[] dirs = RiverMaker.getRiverDirection(x1, z1);
		int width = 3 * scale / SiteStructureGenerator.SITETOBLOCK;
		int rgb = dorfs.structureMap[kx][kz];
		Color col1 = new Color(rgb);
		rgb = dorfs.riverMap[kx][kz];
		Color col2 = new Color(rgb);
		
		river = dirs[0]||dirs[1]||dirs[2]||dirs[3];

		int[] point1 = null;
		int[] point2 = null;
		int[] point3 = null;
		int[] point4 = null;
		
		if(river && !hasRivers)
		{
			x1 = kx * scale;
			z1 = kz * scale;
			if(dirs[3])
			{
				key = kx + 8192 * (kz+1);
				ret = dorfs.sitesByCoord.get(key);
				int[] nearest = null;
				if(ret!=null)
				{
					for(Site s: ret)
					{
						site = s;
						SiteStructures stuff = WorldGenerator.instance.structureGen.getStructuresForSite(site);
						
						int[] temp;
						int dist = Integer.MAX_VALUE;
						for(RiverExit exit: stuff.rivers)
						{
							temp = exit.getEdgeMid(site, scale);
							int tempDist = (temp[0] - x1)*(temp[0] - x1) + (temp[1] - z1)*(temp[1] - z1);
							if(tempDist < dist)
							{
								nearest = temp;
								dist = tempDist;
							}
						}
					}
				}
				if(nearest==null)
				{
					nearest = new int[] {kx * scale + offset, (kz + 1) * scale };
				}
				if(ret == null||isRiver(nearest[0], nearest[1]))
				point1 = nearest;
			}
			if(dirs[1])
			{
				key = (kx-1) + 8192 * (kz);
				ret = dorfs.sitesByCoord.get(key);
				int[] nearest = null;
				if(ret!=null)
				{
					for(Site s: ret)
					{
						site = s;
						SiteStructures stuff = WorldGenerator.instance.structureGen.getStructuresForSite(site);
						
						int[] temp;
						int dist = Integer.MAX_VALUE;
						for(RiverExit exit: stuff.rivers)
						{
							temp = exit.getEdgeMid(site, scale);
							int tempDist = (temp[0] - x1)*(temp[0] - x1) + (temp[1] - z1)*(temp[1] - z1);
							if(tempDist < dist)
							{
								nearest = temp;
								dist = tempDist;
							}
						}
					}
				}
				if(nearest==null)
				{
					nearest = new int[] {(kx-1) * scale + scale, (kz) * scale + offset};
				}
				if(ret == null||isRiver(nearest[0], nearest[1]))
					point2 = nearest;
			}
			if(dirs[2])
			{
				key = kx + 8192 * (kz-1);
				ret = dorfs.sitesByCoord.get(key);
				int[] nearest = null;
				if(ret!=null)
				{
					for(Site s: ret)
					{
						site = s;
						SiteStructures stuff = WorldGenerator.instance.structureGen.getStructuresForSite(site);
						
						
						int[] temp;
						int dist = Integer.MAX_VALUE;
						for(RiverExit exit: stuff.rivers)
						{
							temp = exit.getEdgeMid(site, scale);
							int tempDist = (temp[0] - x1)*(temp[0] - x1) + (temp[1] - z1)*(temp[1] - z1);
							if(tempDist < dist)
							{
								nearest = temp;
								dist = tempDist;
							}
						}
					}
				}
				if(nearest==null)
				{
					nearest = new int[] {kx * scale + offset, (kz - 1) * scale + scale};
				}
				if(ret == null||isRiver(nearest[0], nearest[1]))
				{
//					System.out.println(Arrays.toString(nearest));
					point3 = nearest;
				}
			}
			if(dirs[0])
			{
				key = (kx+1) + 8192 * (kz);
				ret = dorfs.sitesByCoord.get(key);
				int[] nearest = null;
				if(ret!=null)
				{
					for(Site s: ret)
					{
						site = s;
						SiteStructures stuff = WorldGenerator.instance.structureGen.getStructuresForSite(site);
						
						int[] temp;
						int dist = Integer.MAX_VALUE;
						for(RiverExit exit: stuff.rivers)
						{
							temp = exit.getEdgeMid(site, scale);
							int tempDist = (temp[0] - x1)*(temp[0] - x1) + (temp[1] - z1)*(temp[1] - z1);
							if(tempDist < dist)
							{
								nearest = temp;
								dist = tempDist;
							}
						}
					}
				}
				if(nearest==null)
				{
					nearest = new int[] {(kx+1) * scale, (kz) * scale + offset};
				}
				if(ret == null||isRiver(nearest[0], nearest[1]))
					point4 = nearest;
				
			}
		}
//		
//		try
//		{
//			System.out.println(Arrays.toString(point1)+" "+Arrays.toString(point2)
//			+" "+Arrays.toString(point3)+" "+Arrays.toString(point4)+" "+river);
//		}
//		catch (Exception e)
//		{
//			// TODO Auto-generated catch block
//			e.printStackTrace();
//		}
		
		if(point1!=null && point2!=null)
		{
			Vector3d dir = new Vector3d(point1[0] - point2[0], 0, point1[1] - point2[1]);
			double distance = dir.length();
			dir.normalize();
			for(double i = 0; i<distance; i++)
			{
				int tx = (point2[0] + (int)(dir.x * i)) - x;
				int tz = (point2[1] + (int)(dir.z * i)) - z;
				if(Math.abs(tx) < width && Math.abs(tz) < width)
					return true;
			}
		}
		if(point1!=null && point3!=null)
		{
			Vector3d dir = new Vector3d(point1[0] - point3[0], 0, point1[1] - point3[1]);
			double distance = dir.length();
			dir.normalize();
			for(double i = 0; i<distance; i++)
			{
				int tx = (point3[0] + (int)(dir.x * i)) - x;
				int tz = (point3[1] + (int)(dir.z * i)) - z;
				if(Math.abs(tx) < width && Math.abs(tz) < width)
					return true;
			}
		}
		if(point1!=null && point4!=null)
		{
			Vector3d dir = new Vector3d(point1[0] - point4[0], 0, point1[1] - point4[1]);
			double distance = dir.length();
			dir.normalize();
			for(double i = 0; i<distance; i++)
			{
				int tx = (point4[0] + (int)(dir.x * i)) - x;
				int tz = (point4[1] + (int)(dir.z * i)) - z;
				if(Math.abs(tx) < width && Math.abs(tz) < width)
					return true;
			}
		}
		if(point2!=null && point3!=null)
		{
			Vector3d dir = new Vector3d(point2[0] - point3[0], 0, point2[1] - point3[1]);
			double distance = dir.length();
			dir.normalize();
			for(double i = 0; i<distance; i++)
			{
				int tx = (point3[0] + (int)(dir.x * i)) - x;
				int tz = (point3[1] + (int)(dir.z * i)) - z;
				if(Math.abs(tx) < width && Math.abs(tz) < width)
					return true;
			}
		}
		if(point2!=null && point4!=null)
		{
			Vector3d dir = new Vector3d(point2[0] - point4[0], 0, point2[1] - point4[1]);
			double distance = dir.length();
			dir.normalize();
			for(double i = 0; i<distance; i++)
			{
				int tx = (point4[0] + (int)(dir.x * i)) - x;
				int tz = (point4[1] + (int)(dir.z * i)) - z;
				if(Math.abs(tx) < width && Math.abs(tz) < width)
					return true;
			}
		}
		if(point4!=null && point3!=null)
		{
			Vector3d dir = new Vector3d(point4[0] - point3[0], 0, point4[1] - point3[1]);
			double distance = dir.length();
			dir.normalize();
			for(double i = 0; i<distance; i++)
			{
				int tx = (point3[0] + (int)(dir.x * i)) - x;
				int tz = (point3[1] + (int)(dir.z * i)) - z;
				if(Math.abs(tx) < width && Math.abs(tz) < width)
					return true;
			}
		}
		
		return false;
	}
}
