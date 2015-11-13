package dorfgen.conversion;

import static dorfgen.WorldGenerator.scale;

import java.awt.Color;
import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileWriter;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.util.Arrays;
import java.util.BitSet;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;

import dorfgen.WorldGenerator;
import dorfgen.conversion.DorfMap.Site;
import dorfgen.conversion.Interpolator.BicubicInterpolator;
import dorfgen.worldgen.WorldConstructionMaker;
import net.minecraft.block.Block;
import net.minecraft.block.BlockLever;
import net.minecraft.block.BlockTorch;
import net.minecraft.block.properties.IProperty;
import net.minecraft.block.state.IBlockState;
import net.minecraft.entity.passive.EntityVillager;
import net.minecraft.init.Blocks;
import net.minecraft.item.Item;
import net.minecraft.item.ItemBlock;
import net.minecraft.item.ItemDoor;
import net.minecraft.util.BlockPos;
import net.minecraft.util.EnumFacing;
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
	
	private void turnOnLever(World world, int x2, int y, int z2)
	{
//		int h = y + 2;//TODO get the levers turning on again with whatever 1.8 uses for that.
		BlockPos pos = new BlockPos(x2,y,z2);
		IBlockState state = world.getBlockState(pos);
		
        state = state.cycleProperty(BlockLever.POWERED);
        world.setBlockState(pos, state, 3);
        world.notifyNeighborsOfStateChange(pos, state.getBlock());
        EnumFacing enumfacing1 = ((BlockLever.EnumOrientation)state.getValue(BlockLever.FACING)).getFacing();
        world.notifyNeighborsOfStateChange(pos.offset(enumfacing1.getOpposite()), state.getBlock());
	}
	
	private boolean isBlockSurroundedByWall(Site site, SiteStructures structures, WallSegment wall, int x1, int z1)
	{
		boolean surrounded = true;
		boolean nearStruct = false;
		if(!nearStruct)
		{
			nearStruct = structures.isStructure(x1 - 1, z1, scale);
			if(nearStruct)
			{
				boolean t1 = !wall.isInWall(site, x1, z1-1, scale);
				boolean t2 = !wall.isInWall(site, x1, z1+1, scale);
				surrounded = !(t1||t2);
			}
		}
		if(!nearStruct)
		{
			nearStruct = structures.isStructure(x1 + 1, z1, scale);
			if(nearStruct)
			{
				boolean t1 = !wall.isInWall(site, x1, z1-1, scale);
				boolean t2 = !wall.isInWall(site, x1, z1+1, scale);
				surrounded = !(t1||t2);
			}
		}
		if(!nearStruct)
		{
			nearStruct = structures.isStructure(x1, z1 - 1, scale);
			if(nearStruct)
			{
				boolean t1 = !wall.isInWall(site, x1-1, z1, scale);
				boolean t2 = !wall.isInWall(site, x1+1, z1, scale);
				surrounded = !(t1||t2);
			}
		}
		if(!nearStruct)
		{
			nearStruct = structures.isStructure(x1, z1 + 1, scale);
			if(nearStruct)
			{
				boolean t1 = !wall.isInWall(site, x1-1, z1, scale);
				boolean t2 = !wall.isInWall(site, x1+1, z1, scale);
				surrounded = !(t1||t2);
			}
		}
		if(!nearStruct)
		{
			if(surrounded)
				surrounded = wall.isInWall(site, x1 - 1, z1 - 1, scale);
			if(surrounded)
				surrounded = wall.isInWall(site, x1 + 1, z1 - 1, scale);
			if(surrounded)
				surrounded = wall.isInWall(site, x1 - 1, z1 + 1, scale);
			if(surrounded)
				surrounded = wall.isInWall(site, x1 + 1, z1 + 1, scale);
		}
		
		return surrounded;
	}
	
	public void placeTorch(World world, int x, int y, int z)
	{
		BlockPos pos = new BlockPos(x, y, z);
		IBlockState state = Blocks.torch.getDefaultState();
		Iterator iterator = EnumFacing.Plane.HORIZONTAL.iterator();
		EnumFacing enumfacing1 = EnumFacing.EAST;
		
		do
		{
			if (!iterator.hasNext())
			{
				break;
			}
			
			enumfacing1 = (EnumFacing)iterator.next();
		}
		while (!world.isSideSolid(pos.offset(enumfacing1.getOpposite()), enumfacing1, true));
		
		state = Blocks.torch.getDefaultState().withProperty(BlockTorch.FACING, enumfacing1);
       
		world.setBlockState(pos, state);
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
		x -= WorldGenerator.shift.getX();
		z -= WorldGenerator.shift.getZ();
		int h;
		for(int i = 0; i<16; i++)
		{
			for(int j = 0; j<16; j++)
			{
				x1 = x + i;
				z1 = z + j;
				
				x2 = x1 + WorldGenerator.shift.getX();
				z2 = z1 + WorldGenerator.shift.getZ();
				
				HashSet<Site> sites = WorldGenerator.instance.dorfs.getSiteForCoords(x2, z2);
				Site site = null;
				if(sites==null)
					continue;
				//Loop Over sites and do the structures
				for(Site s: sites)
				{
					site = s;
					SiteStructures structures = getStructuresForSite(site);
					if(structures==null)
						continue;
					
					//Generate walls first, so towers can make doors into them
					WallSegment wall = structures.getWall(x1, z1, scale);
					if(wall!=null)
					{
						h = dorfs.biomeInterpolator.interpolate(dorfs.elevationMap, x1, z1, scale);

						boolean surrounded = isBlockSurroundedByWall(site, structures, wall, x1, z1);
						
						world.setBlockState(new BlockPos(x2, h-1, z2), Blocks.stonebrick.getDefaultState());
						for(int k = h; k<h+6; k++)
						{
							if(k < h + 3 || k >= h + 4)
							{
								if(!surrounded)
								{
									if(k==h+5)
									{
										if((x1 + z1) % 3 > 0)
											world.setBlockState(new BlockPos(x2, k, z2), Blocks.stonebrick.getDefaultState());
									}
									else
									{
										world.setBlockState(new BlockPos(x2, k, z2), Blocks.stonebrick.getDefaultState());
									}
								}
							}
							else
							{
								world.setBlockState(new BlockPos(x2, k, z2), Blocks.stonebrick.getDefaultState());
							}
						}
					}
					
					StructureSpace struct = structures.getStructure(x1, z1, scale);
					if(struct!=null)//Generate Building
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
								world.setBlockState(new BlockPos(x2, h+l, z2), material.getDefaultState());
							}
						}
						else
						{
							for(int l = -1; l<height; l++)
							{
								world.setBlockState(new BlockPos(x2, h+l, z2), Blocks.air.getDefaultState());
							}
							if(struct.roofType != SiteMapColours.TOWERROOF)
							{
								world.setBlockState(new BlockPos(x2, h, z2), Blocks.carpet.getDefaultState());
							}
						}
						
						world.setBlockState(new BlockPos(x2, h - 1, z2), material.getDefaultState());
						world.setBlockState(new BlockPos(x2, h + height + 1, z2), material.getDefaultState());
						
						if(struct.shouldBeDoor(site, x1, z1, scale))
						{
							ItemDoor.placeDoor(world, new BlockPos(x2, h, z2), struct.getDoorDirection(site, x1, z1, scale, structures), Blocks.oak_door);
						}
						
						if(struct.shouldBeTorch(site, x1, z1, scale))
						{
							if(struct.roofType != SiteMapColours.TOWERROOF)
							{
								placeTorch(world, x2, h + height, z2);
							}
							else
							{
								world.setBlockState(new BlockPos(x2, h - 1, z2), Blocks.lit_redstone_lamp.getDefaultState());
								world.setBlockState(new BlockPos(x2, h - 3, z2), material.getDefaultState());
								world.setBlockState(new BlockPos(x2,  h - 2, z2), Blocks.lever.getDefaultState());
								turnOnLever(world, x2, h - 2, z2);
								world.setBlockState(new BlockPos(x2, h + height, z2), material.getDefaultState());
							}
						}
						else
						{
							world.setBlockState(new BlockPos(x2, h + height, z2), material.getDefaultState());
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
		
		// second pass
		for(int i = 0; i<16; i++)
		{
			for(int j = 0; j<16; j++)
			{
				x1 = x + i;
				z1 = z + j;
				
				x2 = x1 + WorldGenerator.shift.getX();
				z2 = z1 + WorldGenerator.shift.getZ();
				
				HashSet<Site> sites = WorldGenerator.instance.dorfs.getSiteForCoords(x2, z2);
				Site site = null;
				if(sites==null)
					continue;
				//Loop Over sites and do the structures
				for(Site s: sites)
				{
					site = s;
					SiteStructures structures = getStructuresForSite(site);
					if(structures==null)
						continue;
					
					//Generate walls first, so towers can make doors into them
					WallSegment wall = structures.getWall(x1, z1, scale);
					if(wall!=null)
					{
						h = dorfs.biomeInterpolator.interpolate(dorfs.elevationMap, x1, z1, scale);

						boolean surrounded = isBlockSurroundedByWall(site, structures, wall, x1, z1);
						
						for(int k = h; k<h+6; k++)
						{
							if(!(k < h + 3 || k >= h + 4))
							{
								if(surrounded)
								{
									boolean pos = false, neg = false;
									
									// if this block is surrounded by wall, but the block next to it is not,
									// then we must be on the edge of the inside of the corridor:
									// this is where we want to place torches
									
								    if(!(pos = isBlockSurroundedByWall(site, structures, wall, x1 + 1, z1)) ||
								    		(neg = !isBlockSurroundedByWall(site, structures, wall, x1 - 1, z1)))
									{
								    	// place torches every n blocks
								    	if(z1 % 3 == 0)
								    	{
								    		// this is a second pass, so the buildings connected to the corridors
								    		// have already been generated. We want avoid erasing the walls, but we
								    		// don't want to not place torches on those walls, either. By checking pos
								    		// and neg, we can move one block toward the center of the corridor, and
								    		// place a torch there
											if(world.getBlockState(new BlockPos(x2, h + 1, z2)).getBlock() != Blocks.stonebrick)
											{
												placeTorch(world, x2, h + 1, z2);
											}
											else if(pos && world.getBlockState(new BlockPos(x2 - 1, h + 1, z2)).getBlock() != Blocks.stonebrick)
											{
												placeTorch(world, x2 - 1, h + 1, z2);
											}
											else if(neg && world.getBlockState(new BlockPos(x2 + 1, h + 1, z2)).getBlock() != Blocks.stonebrick)
											{
												placeTorch(world, x2 + 1, h + 1, z2);
											}
								    	}
									}
								    else if(!(pos = isBlockSurroundedByWall(site, structures, wall, x1, z1 + 1)) ||
												!(neg = isBlockSurroundedByWall(site, structures, wall, x1, z1 - 1)))
									{
								    	if(x1 % 3 == 0)
								    	{
											if(world.getBlockState(new BlockPos(x2, h + 1, z2)).getBlock() != Blocks.stonebrick)
											{
												placeTorch(world, x2, h + 1, z2);
											}
											else if(pos && world.getBlockState(new BlockPos(x2, h + 1, z2 - 1)).getBlock() != Blocks.stonebrick)
											{
												placeTorch(world, x2, h + 1, z2 - 1);
											}
											else if(neg && world.getBlockState(new BlockPos(x2 + 1, h + 1, z2)).getBlock() != Blocks.stonebrick)
											{
												placeTorch(world, x2, h + 1, z2 + 1);
											}
								    	}
									}
								}
							}
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
		public final HashSet<WallSegment> walls = new HashSet();
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
			if(site.rgbmap != null && !readFromFile())
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
							else if(WallSegment.WALLBITS.contains(rgb) && !isInWall(x, y))
							{
								walls.add(new WallSegment(site, x, y));
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
						
						StructureSpace structure;
						if(roof == SiteMapColours.TOWERROOF)
						{
							structure = new WallTowerSpace(corner1, corner2);
						}
						else
						{
							structure = new StructureSpace(corner1, corner2, roof);
						}
						structures.add(structure);
					}
					
				}
				writeToFile();
			}
		}
		
		private boolean readFromFile()
		{
			File sites = new File(FileLoader.resourceDir.getAbsolutePath()+File.separator+"sitescaches");
			if(!sites.exists())
			{
				sites.mkdirs();
				return false;
			}
			
			for (File f : sites.listFiles())
			{
				String s = f.getName();
				if(s.contains("sitecache"+site.id))
				{
					BufferedReader br = null;
					
					try {
						InputStream res = new FileInputStream(f);
						br = new BufferedReader(new InputStreamReader(res));
						String line;
						String[] args;
						while ((line = br.readLine()) != null)
						{
							args = line.split(":");
							String type = args[0];
							if(type.equalsIgnoreCase("structurespace"))
							{
								int roofrgb = Integer.parseInt(args[1]);
								int minx = Integer.parseInt(args[2]);
								int miny = Integer.parseInt(args[3]);
								int maxx = Integer.parseInt(args[4]);
								int maxy = Integer.parseInt(args[5]);
								SiteMapColours colour = SiteMapColours.getMatch(roofrgb);
								int[] min = {minx, miny};
								int[] max = {maxx, maxy};
								StructureSpace structure;
								if(colour == SiteMapColours.TOWERROOF)
								{
									structure = new WallTowerSpace(min, max);
								}
								else
								{
									structure = new StructureSpace(min, max, colour);
								}
								structures.add(structure);
							}
							else if(type.equalsIgnoreCase("wallsegment"))
							{
								HashSet<Integer> pixels = new HashSet();
								for(int i = 1; i<args.length; i++)
								{
									pixels.add(Integer.parseInt(args[i]));
								}
								walls.add(new WallSegment(pixels));
							}
						}
						br.close();
						return true;
					} catch (Exception e) {
						return false;
					}
				}
			}
			return false;
		}
		
		private void writeToFile()
		{
			File sites = new File(FileLoader.resourceDir.getAbsolutePath()+File.separator+"sitescaches");
			String file = sites.getAbsolutePath()+File.separator+"sitecache"+site.id;
			
			FileWriter fwriter;
			PrintWriter out;
			try
			{
				fwriter = new FileWriter(file);
				out = new PrintWriter(fwriter);
				
				for(StructureSpace struct : structures)
				{
					String line = "structurespace";
					line += ":"+struct.roofType.colour.getRGB();
					line += ":"+struct.min[0];
					line += ":"+struct.min[1];
					line += ":"+struct.max[0];
					line += ":"+struct.max[1];
					out.println(line);
				}
				for(WallSegment wall: walls)
				{
					String line = "wallsegment";
					for(Integer i: wall.pixels)
					{
						line += ":"+i;
					}
					out.println(line);
				}
				
				out.close();
				fwriter.close();
			}
			catch(Exception e)
			{
				
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
		 * Takes site map pixel Coordinates
		 * @param x
		 * @param y
		 * @return
		 */
		boolean isInWall(int x, int y)
		{
			for(WallSegment wall: walls)
			{
				if(wall.isInSegment(x, y))
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
		
		public WallSegment getWall(int x, int y, int scale)
		{
			for(WallSegment wall: walls)
			{
				if(wall.isInWall(site, x, y, scale))
					return wall;
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
//			if(bounds==null)
			{
				int width = (scale/SITETOBLOCK);
				int offset = scale/2;
				bounds = new int[2][2];
				bounds[0][0] = min[0] * width + site.corners[0][0] * scale + offset;// - width/2;
				bounds[0][1] = min[1] * width + site.corners[0][1] * scale + offset;// - width/2;
				bounds[1][0] = max[0] * width + site.corners[0][0] * scale + offset + width;// + width/2 fixes one side
				bounds[1][1] = max[1] * width + site.corners[0][1] * scale + offset + width;
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
			int midz = (bounds[0][1] + bounds[1][1])/2;
			
			//middle of a wall
			if((x == midx && (z==bounds[0][1] || z == bounds[1][1])) || (z == midz && (x==bounds[0][0] || x == bounds[1][0])))
			{
				SiteStructures structs = WorldGenerator.instance.structureGen.getStructuresForSite(site);
				EnumFacing dir = getDoorDirection(site, x, z, scale, structs).getOpposite();
				StructureSpace other = structs.getStructure(x + dir.getFrontOffsetX(), z + dir.getFrontOffsetZ(), scale);
				if(other!=null && other.getFloor(site, scale)!=getFloor(site, scale))
					return false;
				return true;
			}
			return false;
		}
		
		public EnumFacing getDoorDirection(Site site, int x, int z, int scale, SiteStructures structures)
		{
			EnumFacing ret = EnumFacing.UP;
			
			for(EnumFacing dir: EnumFacing.HORIZONTALS)
			{
				StructureSpace other = structures.getStructure(x + dir.getFrontOffsetX(), z + dir.getFrontOffsetZ(), scale);
				if(other!=this)
				{
					return dir.getOpposite();
				}
			}
			return ret;
		}
		
		public boolean shouldBeTorch(Site site, int x, int z, int scale)
		{
			getBounds(site, scale);
			
			if(z > bounds[0][1] && z < bounds[1][1] && x > bounds[0][0] && x < bounds[1][0])
			{
				return (z % 4 == 0) && (x % 4 == 0);
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
	
	public static class WallTowerSpace extends StructureSpace
	{
		static int WALLCOLOUR = SiteMapColours.TOWNWALL.colour.getRGB();
		/**
		 * The two other towers connected to this one by wall segments
		 */
		public WallTowerSpace[] connected = new WallTowerSpace[2];
		/**
		 * The pixel coordinates of the centre of a face of this tower, 
		 * which are closest to the center of the faces of the connected towers
		 */
		int[][] wallConnects = new int[2][2];
		
		public WallTowerSpace(int[] minCoords, int[] maxCoords)
		{
			super(minCoords, maxCoords, SiteMapColours.TOWERROOF);
		}
		
		public int countConnects()
		{
			return connected[0]==null?0:connected[1]==null?1:2;
		}
		
		/**
		 * Adds this tower as a connected tower
		 * @param tower
		 * @param site
		 * @return whether the tower can be added
		 */
		public boolean addConnected(WallTowerSpace tower, Site site)
		{
			if(connected[0]==null)
			{
				connected[0] = tower;
				initWallConnects(tower, site, 0);
				return true;
			}
			if(connected[1]==null)
			{
				connected[1] = tower;
				initWallConnects(tower, site, 1);
				return true;
			}
			return false;
		}
		
		/**
		 * Returns if this Tower actually has any wall pixels near it.
		 * If this is false, it should not be added to other walls.
		 * @return
		 */
		public boolean hasWallConnect(Site site)
		{
			int[][] thisConnects = getWallAttachments(site);
			
			return thisConnects[0][0]!=-1;
		}
		
		private void initWallConnects(WallTowerSpace tower, Site site, int index)
		{
			int[][] towerConnects = tower.getWallAttachments(site);
			int[][] thisConnects = getWallAttachments(site);
			int[] closest = null;
			int distSq = Integer.MAX_VALUE;
			
			for(int[] i: thisConnects)
			{
				for(int[] i1: towerConnects)
				{
					int dx = i[0]-i1[0];
					int dy = i[1]-i1[1];
					int temp = dx*dx + dy*dy;
					if(temp < distSq)
					{
						distSq = temp;
						closest = i;
					}
				}
			}
			if(closest!=null && closest[0]!=-1)
			{
				wallConnects[index] = closest;
			}
		}
		
		int[][] getWallAttachments(Site site)
		{
			int[][] thisConnects = {{-1,-1},{-1,-1}};
			int minx = min[0], miny = min[1], maxx = max[0], maxy = max[1];
			int midx = minx + (maxx - minx)/2;
			int midy = miny + (maxy - miny)/2;
			
			//Check both y edges for wall pixels
			for(int x = minx; x<=maxx; x++)
			{
				int rgbmin = site.rgbmap[x][miny - 1];
				int rgbmax = site.rgbmap[x][maxy + 1];
				if(thisConnects[0][0]==-1 && rgbmin == WALLCOLOUR)
				{
					thisConnects[0][0] = midx;
					thisConnects[0][1] = miny;
				}
				if(thisConnects[0][0]==-1 && rgbmax == WALLCOLOUR)
				{
					thisConnects[0][0] = midx;
					thisConnects[0][1] = maxy;
				}
			}
			for(int x = minx; x<=maxx; x++)
			{
				int rgbmin = site.rgbmap[x][miny - 1];
				int rgbmax = site.rgbmap[x][maxy + 1];
				if(thisConnects[1][0]==-1 && rgbmin == WALLCOLOUR)
				{
					thisConnects[1][0] = midx;
					thisConnects[1][1] = miny;
				}
				if(thisConnects[1][0]==-1 && rgbmax == WALLCOLOUR)
				{
					thisConnects[1][0] = midx;
					thisConnects[1][1] = maxy;
				}
			}
			
			if(thisConnects[0][0]==-1 || thisConnects[1][0]==-1)
			{
				//If there is a missing entry, check both x edges for wall pixels
				for(int y = miny; y<=maxy; y++)
				{
					int rgbmin = site.rgbmap[minx - 1][y];
					int rgbmax = site.rgbmap[maxx + 1][y];
					if(thisConnects[0][0]==-1 && rgbmin == WALLCOLOUR)
					{
						thisConnects[0][0] = minx;
						thisConnects[0][1] = midy;
					}
					if(thisConnects[0][0]==-1 && rgbmax == WALLCOLOUR)
					{
						thisConnects[0][0] = minx;
						thisConnects[0][1] = midy;
					}
				}
				for(int y = miny; y<=maxy; y++)
				{
					int rgbmin = site.rgbmap[minx - 1][y];
					int rgbmax = site.rgbmap[maxx + 1][y];
					if(thisConnects[1][0]==-1 && rgbmin == WALLCOLOUR)
					{
						thisConnects[1][0] = minx;
						thisConnects[1][1] = midy;
					}
					if(thisConnects[1][0]==-1 && rgbmax == WALLCOLOUR)
					{
						thisConnects[1][0] = minx;
						thisConnects[1][1] = midy;
					}
				}
			}
			return thisConnects;
		}
	}
	
	public static class WallSegment
	{
		public static final HashSet<Integer> WALLBITS = new HashSet();
		static {
			WALLBITS.add(SiteMapColours.TOWNWALL.colour.getRGB());
			WALLBITS.add(SiteMapColours.TOWNWALLMID.colour.getRGB());
		}
		public final HashSet<Integer> pixels = new HashSet();
		
		public WallSegment(Site site, int x, int y)
		{
			initWallSegment(site, x, y);
		}
		
		public WallSegment(Collection<Integer> pixels_)
		{
			pixels.addAll(pixels_);
		}
		
		/**
		 * Checks in site map pixel coordinates.
		 * @param x
		 * @param y
		 * @return
		 */
		public boolean isInSegment(int x, int y)
		{
			return pixels.contains(x + 2048*y);
		}
		
		/**
		 * Takes Block Coordinates
		 * @param site
		 * @param x
		 * @param y
		 * @param scale
		 * @return
		 */
		public boolean isInWall(Site site, int x, int y, int scale)
		{
			int offset = scale/2;
			int width = (scale/SITETOBLOCK);
			int pixelX = (x - site.corners[0][0] * scale - scale/2 - width/2)/width;
			int pixelY = (y - site.corners[0][1] * scale - scale/2 - width/2)/width;
			
			return pixels.contains(pixelX + 2048*pixelY);
		}
		
		public void initWallSegment(Site site, int x, int y)
		{
			BitSet blocked = new BitSet();
			BitSet checked = new BitSet();
			HashSet valid = new HashSet();
			int[][] map = site.rgbmap;
			checkAround(map, x, y, blocked, checked, valid);
			pixels.addAll(valid);
			
		}
		
		private boolean checkAround(int[][] map, int x, int y, BitSet blocked, BitSet checked, HashSet valid)
		{
			int[] index = new int[4];
			boolean cont = false;
			int[][] dirs = getDirs(map, x, y);
			index[0] = x-1 + (y)*2048;
			index[1] = x+1 + (y)*2048;
			index[2] = x + (y-1)*2048;
			index[3] = x + (y+1)*2048;
			for(int i = 0; i<4; i++)
			{
				if(checked.get(index[i]))
					continue;
				if(dirs[i]!=null)
				{
					valid.add(index[i]);
					cont = true;
				}
				else
				{
					blocked.set(index[i]);
				}
				if(valid.contains(index[i]))
				{
					checked.set(index[i]);
					checkAround(map, index[i]&2047, index[i]/2048, blocked, checked, valid);
				}
			}
			return cont;
		}
		
		private boolean isValid(int[][] map, int x, int y)
		{
			int rgb = map[x][y];
			return WALLBITS.contains(rgb);
		}
		
		/**
		 * 0 = -x, 1 = x, 2 = -y, 3 = y
		 * @param map
		 * @param x
		 * @param y
		 * @return
		 */
		private int[][] getDirs(int[][] map, int x, int y)
		{
			int[][] ret = new int[4][];
			int rgb = map[x - 1][y];
			if(WALLBITS.contains(rgb))
				ret[0] = new int[]{x-1,y};
			rgb = map[x + 1][y];
			if(WALLBITS.contains(rgb))
				ret[1] = new int[]{x+1,y};
			rgb = map[x][y - 1];
			if(WALLBITS.contains(rgb))
				ret[2] = new int[]{x,y-1};
			rgb = map[x][y + 1];
			if(WALLBITS.contains(rgb))
				ret[3] = new int[]{x,y+1};
			return ret;
		}
	}
	
	public static class TownWall
	{
		final HashSet<WallTowerSpace> towers = new HashSet();
		
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
