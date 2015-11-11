package dorfgen.conversion;

import static dorfgen.WorldGenerator.scale;

import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;

import dorfgen.WorldGenerator;
import dorfgen.conversion.DorfMap.Site;
import dorfgen.conversion.Interpolator.BicubicInterpolator;
import dorfgen.worldgen.WorldConstructionMaker;
import net.minecraft.block.Block;
import net.minecraft.entity.passive.EntityVillager;
import net.minecraft.init.Blocks;
import net.minecraft.item.ItemDoor;
import net.minecraft.world.World;

public class SiteStructureGenerator
{
	static DorfMap dorfs;
	public static int SITETOBLOCK = 51;
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
	
	/**
	 * Takes Chunk Coordinates
	 * @param s
	 * @param x
	 * @param z
	 * @param world
	 */
	public void generate(int chunkX, int chunkZ, World world)
	{
		
		int scale = WorldGenerator.scale;
		BicubicInterpolator	bicubicInterpolator = WorldConstructionMaker.bicubicInterpolator;
		int x = chunkX, z = chunkZ, x1, x2, z1, z2;
		x *= 16;
		z *= 16;
		x -= WorldGenerator.shift.posX;
		z -= WorldGenerator.shift.posZ;
		int h;
		for(int i = 0; i<16; i++)
		{
			for(int j = 0; j<16; j++)
			{
				x1 = x + i;
				z1 = z + j;
				
				x2 = x1 + WorldGenerator.shift.posX;
				z2 = z1 + WorldGenerator.shift.posZ;
				
				HashSet<Site> sites = WorldGenerator.instance.dorfs.getSiteForCoords(x2, z2);
				Site site = null;
				if(sites==null)
					continue;
				for(Site s: sites)
				{
					site = s;
					SiteStructures structures = getStructuresForSite(site);
					if(structures==null)
						continue;
					StructureSpace struct = structures.getStructure(x1, z1, scale);
					if(struct!=null)
					{
						Block material = Blocks.planks;
						int height = 3;
						boolean villager = x1 == struct.getMid(site, scale)[0] && z1 == struct.getMid(site, scale)[1];
						if(struct.roofType == SiteMapColours.TOWERROOF)
						{
							material = Blocks.stonebrick;
							height = 10;
							villager = false;
						}
						
						h = struct.getFloor(site, scale);

						if(struct.inWall(site, x1, z1, scale))
						{
							for(int l = 0; l<height; l++)
							{
								world.setBlock(x1, h+l, z2, material);
							}
						}
						else
						{
							for(int l = -1; l<height; l++)
							{
								world.setBlock(x1, h+l, z2, Blocks.air);
							}
						}
						world.setBlock(x1, h - 1, z2, material);
						world.setBlock(x1, h + height, z2, material);
						if(struct.shouldBeDoor(site, x1, z1, scale))
						{
							int meta = 0;//TODO determine direction of door  see 833 of StructureComponent
							ItemDoor.placeDoorBlock(world, x2, h, z2, meta, Blocks.wooden_door);
						}
						if(villager)
						{
	                        EntityVillager entityvillager = new EntityVillager(world, 0);
	                        entityvillager.setLocationAndAngles((double)x2 + 0.5D, (double)h, (double)z2 + 0.5D, 0.0F, 0.0F);
	                        world.spawnEntityInWorld(entityvillager);
						}
					}
				}
			}
		}
	}
	
	public static class SiteStructures
	{
		public final Site site;
		public final HashSet<StructureSpace> structures = new HashSet();
		public final HashSet<RoadExit> roads = new HashSet();
		public final HashSet<RiverExit> rivers = new HashSet();
		
		public SiteStructures(Site site_)
		{
			site = site_;
			initStructures();
			initRoadsAndRivers();
		}
		
		private void initRoadsAndRivers()
		{
			if(site.rgbmap != null)
			{
				int h = site.rgbmap.length;
				int w = site.rgbmap[0].length;
				
				boolean found1 = false;
				boolean found2 = false;
				int i1 = -1, i2 = -1;
				int n1 = 0, n2 = 0;
				
				//first 2 Edges
				for(int i = 0; i<h; i++)
				{
					int side1 = site.rgbmap[i][0];
					int side2 = site.rgbmap[i][w-1];

					SiteMapColours colour1 = SiteMapColours.getMatch(side1);
					SiteMapColours colour2 = SiteMapColours.getMatch(side2);

					//Roads
					if(!found1 && colour1 == SiteMapColours.ROAD)
					{
						found1 = true;
						roads.add(new RoadExit(i + 3, 0));
					}
					else if(found1 && colour1 != SiteMapColours.ROAD)
					{
						found1 = false;
					}
					if(!found2 && colour2 == SiteMapColours.ROAD)
					{
						found2 = true;
						roads.add(new RoadExit(i + 3, w - 1));
					}
					else if(found2 && colour2 != SiteMapColours.ROAD)
					{
						found2 = false;
					}
					//Rivers
					if(i1==-1 && colour1 == SiteMapColours.RIVER)
					{
						i1 = i;
						n1 = 0;
					}
					else if(i1!=-1 && colour1 != SiteMapColours.RIVER)
					{
						rivers.add(new RiverExit(i1, 0, n1++, false));
						i1 = -1;
					}
					else if(i1 != -1)
					{
						n1++;
					}
					if(i2==-1 && colour2 == SiteMapColours.RIVER)
					{
						i2 = i;
						n2 = 0;
					}
					else if(i2 != -1 && colour2 != SiteMapColours.RIVER)
					{
						rivers.add(new RiverExit(i2, w-1, n2++, false));
						i2 = -1;
					}
					else if(i2 != -1)
					{
						n2++;
					}
				}
				found1 = false;
				found2 = false;
				i1  = i2 = -1;
				n1 = n2 = 0;
				//second 2 Edges
				for(int i = 0; i<w; i++)
				{
					int side1 = site.rgbmap[0][i];
					int side2 = site.rgbmap[h - 1][i];

					SiteMapColours colour1 = SiteMapColours.getMatch(side1);
					SiteMapColours colour2 = SiteMapColours.getMatch(side2);
					//Roads
					if(!found1 && colour1 == SiteMapColours.ROAD)
					{
						found1 = true;
						roads.add(new RoadExit(0, i + 3));
					}
					else if(found1 && colour1 != SiteMapColours.ROAD)
					{
						found1 = false;
					}
					if(!found2 && colour2 == SiteMapColours.ROAD)
					{
						found2 = true;
						roads.add(new RoadExit(h - 1, i + 3));
					}
					else if(found2 && colour2 != SiteMapColours.ROAD)
					{
						found2 = false;
					}
					//Rivers
					if(i1==-1 && colour1 == SiteMapColours.RIVER)
					{
						i1 = i;
						n1 = 0;
					}
					else if(i1!=-1 && colour1 != SiteMapColours.RIVER)
					{
						rivers.add(new RiverExit(0, i1, n1++, true));
						i1 = -1;
					}
					else if(i1 != -1)
					{
						n1++;
					}
					if(i2==-1 && colour2 == SiteMapColours.RIVER)
					{
						i2 = i;
						n2 = 0;
					}
					else if(i2 != -1 && colour2 != SiteMapColours.RIVER)
					{
						rivers.add(new RiverExit(h-1, i2, n2++, true));
						i2 = -1;
					}
					else if(i2 != -1)
					{
						n2++;
					}
				}
				
			}
		}
		
		private void initStructures()
		{
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
					for(int x = 1; x<site.rgbmap.length-1; x++)
					{
						for(int y = 1; y<site.rgbmap[0].length-1; y++)
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
		
		//TODO see if I need to implement this and get it working.
		private boolean isBuildingFirstCorner(int[][] image, int x, int y)
		{
			int rgb1 = image[x][y];
			int rgbpx = image[x+1][y];
			int rgbpy = image[x][y+1];
			int rgbpd = image[x+1][y+1];
			
			if(rgbpd == rgb1)
				return false;
			
			
			
			return true;
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
		boolean isHouseWall(SiteMapColours colour)
		{
			if(colour==null)
				return false;
			return colour.toString().contains("BUILDINGWALL");
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
		private int[] mid;
		
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
				int offset = scale/2;
				bounds = new int[2][2];
				bounds[0][0] = min[0] * (scale/SITETOBLOCK) + site.corners[0][0] * scale + offset;
				bounds[0][1] = min[1] * (scale/SITETOBLOCK) + site.corners[0][1] * scale + offset;
				bounds[1][0] = max[0] * (scale/SITETOBLOCK) + site.corners[0][0] * scale + offset;
				bounds[1][1] = max[1] * (scale/SITETOBLOCK) + site.corners[0][1] * scale + offset;
			}
			return bounds;
		}
		/**
		 * Takes Block Coordinates
		 * @param site
		 * @param x
		 * @param z
		 * @param scale
		 * @return
		 */
		public boolean shouldBeDoor(Site site, int x, int z, int scale)
		{
			getBounds(site, scale);

			int midx = (bounds[0][0] + bounds[1][0])/2;
			int midy = (bounds[0][1] + bounds[1][1])/2;
			
			//middle of a wall
			if((x == midx && (z==bounds[0][1] || z == bounds[1][1])) || (z == midy && (x==bounds[0][0] || x == bounds[1][0])))
			{
			//	SiteStructures structs = WorldGenerator.instance.structureGen.getStructuresForSite(site);
//				System.out.println("door");
				return true;
			}
			return false;
		}
		
		public boolean inWall(Site site, int x, int z, int scale)
		{
			getBounds(site, scale);
			if(((z==bounds[0][1] || z == bounds[1][1])) || ((x==bounds[0][0] || x == bounds[1][0])))
			{
				return true;
			}
			return false;
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
		
		public int[] getMid(Site site, int scale)
		{
			if(mid!=null)
				return mid;
			getBounds(site, scale);
			return mid = new int[] { bounds[0][0] + (bounds[1][0] - bounds[0][0]) / 2 , bounds[0][1] + (bounds[1][1] - bounds[0][1]) / 2};
		}
	}

	public static class RoadExit
	{
		final int midPixelX;
		final int midPixelY;
		int[] location;
		public RoadExit(int x, int y)
		{
			midPixelX = x;
			midPixelY = y;
		}
		
		public int[] getEdgeMid(Site site, int scale)
		{
			if(location==null)
			{
				location = new int[2];
				int offset = scale/2;
				location[0] = midPixelX * (scale/SITETOBLOCK) + site.corners[0][0] * scale + offset;
				location[1] = midPixelY * (scale/SITETOBLOCK) + site.corners[0][1] * scale + offset;
			}
			return location;
		}
	}
	
	public static class RiverExit
	{
		final int midPixelX;
		final int midPixelY;
		final int width;
		final boolean xEdge;
		int[] location;
		public RiverExit(int x, int y, int w, boolean onX)
		{
			midPixelX = x;
			midPixelY = y;
			width = w;
			xEdge = onX;
		}
		
		public int[] getEdgeMid(Site site, int scale)
		{
			if(location==null)
			{
				location = new int[3];
				int offset = scale/2;
				location[0] = (midPixelX * (scale/SITETOBLOCK)) + site.corners[0][0] * scale + offset;
				location[1] = (midPixelY * (scale/SITETOBLOCK)) + site.corners[0][1] * scale + offset;
				location[2] = width * (scale/SITETOBLOCK);
				if(!xEdge)
				{
					location[0] += location[2]/2;
				}
				else
				{
					location[1] += location[2]/2;
				}
			}
			return location;
		}
	}
	
	public static enum StructureType
	{
		WALL,
		HOUSE,
		TOWER,
	}
}
