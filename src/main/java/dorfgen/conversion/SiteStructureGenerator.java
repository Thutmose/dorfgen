package dorfgen.conversion;

import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;

import dorfgen.conversion.DorfMap.Site;

public class SiteStructureGenerator
{
	static DorfMap dorfs;
	HashMap<Integer, SiteStructures> structureMap = new HashMap();
	
	public SiteStructureGenerator(DorfMap dorfs_)
	{
		dorfs = dorfs_;
		System.out.println("Processing Site Maps for structures");
		for(Integer i: dorfs.sitesById.keySet())
		{
			structureMap.put(i, new SiteStructures(dorfs.sitesById.get(i)));
		}
	}
	
	public SiteStructures getStructuresForSite(Site site)
	{
		return structureMap.get(site.id);
	}
	
	public void generate(Site s)
	{
		
	}
	
	public static class SiteStructures
	{
		public final Site site;
		public final HashSet<StructureSpace> structures = new HashSet();
		
		public SiteStructures(Site site_)
		{
			site = site_;
			
			if(site.rgbmap != null)
			{
				HashSet<Integer> found = new HashSet();
				
				boolean newStruct = true;
				int n = 0;
				while (newStruct && n<10000)
				{
					n++;
					int[] corner1 = {-1,-1};
					int[] corner2 = {-1,-1};
					newStruct = false;
					SiteMapColours roof = null;
					loopToFindNew:
					for(int x = 0; x<site.rgbmap.length; x++)
					{
						for(int y = 0; y<site.rgbmap[0].length; y++)
						{
							int rgb = site.rgbmap[x][y];
							int index = x + 2048*y;

							SiteMapColours colour = SiteMapColours.getMatch(rgb);
							if(isRoof(colour) && !found.contains(index) && !isInStructure(x, y))
							{
								roof = colour;
								newStruct = true;
								corner1[0] = x;
								corner1[1] = y;
								found.add(index);
								break loopToFindNew;
							}
						}
					}
					if(newStruct)//TODO make it look for borders around, to allow the bright green roofs to work as well.
					{
						loopx:
						for(int x = corner1[0]; x<site.rgbmap.length-1; x++)
						{
							int y = corner1[1];
							int rgb = site.rgbmap[x + 1][y];
							SiteMapColours colour = SiteMapColours.getMatch(rgb);
							
							if(colour != roof)
							{
								corner2[0] = x;
								break loopx;
							}
						}
						loopy:
						for(int y = corner1[1]; y<site.rgbmap[0].length-1; y++)
						{
							int x = corner1[0];
							int rgb = site.rgbmap[x][y + 1];
							SiteMapColours colour = SiteMapColours.getMatch(rgb);
							
							if(colour != roof)
							{
								corner2[1] = y;
								break loopy;
							}
						}
						//Expand out to include the walls.
						corner1[0]--;
						corner1[1]--;
						corner2[0]++;
						corner2[1]++;
						StructureSpace structure = new StructureSpace(corner1, corner2, roof);
						structures.add(structure);
					}
					
				}
			}
		}
		
		/**
		 * Takes site map pixel Coordinates.
		 * @param x
		 * @param y
		 * @return
		 */
		boolean isInStructure(int x, int y)
		{
			for(StructureSpace struct: structures)
			{
				if(x >= struct.min[0] && x <= struct.max[0] && y >= struct.min[1] && y <= struct.max[1])
					return true;
			}
			return false;
		}
		
		/**
		 * Takes block coordinates
		 * @param x
		 * @param y
		 * @return
		 */
		public boolean isStructure(int x, int y, int scale)
		{
			return getStructure(x, y, scale)!=null;
		}
		/**
		 * Takes block coordinates
		 * @param x
		 * @param y
		 * @return
		 */
		public StructureSpace getStructure(int x, int y, int scale)
		{
			for(StructureSpace struct: structures)
			{
				int[][] bounds = struct.getBounds(site, scale);
				if(x >= bounds[0][0] && x <= bounds[1][0] && y >= bounds[0][1] && y <= bounds[1][1])
					return struct;
			}
			return null;
		}
		
		boolean isRoof(SiteMapColours colour)
		{
			if(colour==null)
				return false;
			return colour.toString().contains("ROOF");
		}
	}

	public static class StructureSpace
	{
		public final SiteMapColours roofType;
		/**
		 * Pixel Coordinates in the site map image
		 */
		public final int[] min;
		/**
		 * Pixel Coordinates in the site map image
		 */
		public final int[] max;
		
		private int[][] bounds;
		
		public StructureSpace(int[] minCoords, int[] maxCoords, SiteMapColours roof)
		{
			min = minCoords;
			max = maxCoords;
			roofType = roof;
		}
		
		public int[][] getBounds(Site site, int scale)
		{
			if(bounds==null)
			{
				bounds = new int[2][2];
				bounds[0][0] = min[0] * (scale/51) + site.corners[0][0] * scale;
				bounds[0][1] = min[1] * (scale/51) + site.corners[0][1] * scale;
				bounds[1][0] = max[0] * (scale/51) + site.corners[0][0] * scale;
				bounds[1][1] = max[1] * (scale/51) + site.corners[0][1] * scale;
			}
			return bounds;
		}
		
		public int getFloor(Site site, int scale)
		{
			getBounds(site, scale);
			int floor = 0;
			int[] corners = new int[4];
			corners[0] = dorfs.biomeInterpolator.interpolate(dorfs.elevationMap, bounds[0][0], bounds[0][1], scale);
			corners[1] = dorfs.biomeInterpolator.interpolate(dorfs.elevationMap, bounds[1][0], bounds[1][1], scale);
			corners[2] = dorfs.biomeInterpolator.interpolate(dorfs.elevationMap, bounds[1][0], bounds[0][1], scale);
			corners[3] = dorfs.biomeInterpolator.interpolate(dorfs.elevationMap, bounds[0][0], bounds[1][1], scale);
			
			floor = corners[0]+corners[1]+corners[2]+corners[3];
			floor /= 4;
			
			return floor;
		}
	}
	
	public static enum StructureType
	{
		WALL,
		HOUSE,
		TOWER,
	}
}
