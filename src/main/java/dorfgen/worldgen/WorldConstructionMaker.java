package dorfgen.worldgen;

import static dorfgen.WorldGenerator.scale;
import static net.minecraft.util.EnumFacing.EAST;
import static net.minecraft.util.EnumFacing.NORTH;
import static net.minecraft.util.EnumFacing.SOUTH;
import static net.minecraft.util.EnumFacing.WEST;

import java.util.HashSet;

import dorfgen.BlockRoadSurface;
import dorfgen.WorldGenerator;
import dorfgen.conversion.DorfMap;
import dorfgen.conversion.DorfMap.ConstructionType;
import dorfgen.conversion.DorfMap.Site;
import dorfgen.conversion.DorfMap.WorldConstruction;
import dorfgen.conversion.Interpolator.BicubicInterpolator;
import dorfgen.conversion.SiteMapColours;
import dorfgen.conversion.SiteStructureGenerator;
import dorfgen.conversion.SiteStructureGenerator.SiteStructures;
import dorfgen.conversion.SiteStructureGenerator.StructureSpace;
import dorfgen.conversion.SiteTerrain;
import net.minecraft.block.Block;
import net.minecraft.init.Blocks;
import net.minecraft.util.EnumFacing;
import net.minecraft.world.World;
import net.minecraft.world.biome.BiomeGenBase;
import net.minecraftforge.common.BiomeDictionary;
import net.minecraftforge.common.BiomeDictionary.Type;

public class WorldConstructionMaker
{
	public static BicubicInterpolator	bicubicInterpolator	= new BicubicInterpolator();
	final DorfMap						dorfs;

	public WorldConstructionMaker()
	{
		dorfs = WorldGenerator.instance.dorfs;
	}

	public boolean shouldConstruct(int chunkX, int chunkZ, ConstructionType type)
	{
		int x = chunkX * 16;
		int z = chunkZ * 16;
		x -= WorldGenerator.shift.posX;
		z -= WorldGenerator.shift.posZ;

		int dx = 16 * scale / 2 + scale;
		int dz = 16 * scale / 2 + scale;

		if (x >= 0 && z >= 0 && (x + 16) / scale <= WorldGenerator.instance.dorfs.biomeMap.length
				&& (z + 16) / scale <= WorldGenerator.instance.dorfs.biomeMap[0].length)
		{
			int x1 = (x/ scale) / 16;
			int z1 = (z/ scale) / 16;
			int key = (x1) + 2048 * (z1);
			
			if (DorfMap.constructionsByCoord.containsKey(key))
			{
				for (WorldConstruction construct : DorfMap.constructionsByCoord.get(key))
				{
					if (construct.type == type && construct.isInConstruct(x, 0, z)) return true;
				}
				return false;
			}
			return false;
		}
		return false;
	}
	
	public void buildSites(World world, int chunkX, int chunkZ, Block[] blocks, BiomeGenBase[] biomes)
	{
		if(dorfs.structureMap.length==0)
			return;
		SiteStructureGenerator structureGen = WorldGenerator.instance.structureGen;
		int index;
		int x = (chunkX * 16 - WorldGenerator.shift.posX);
		int z = (chunkZ * 16 - WorldGenerator.shift.posZ);
		int x1, z1, h, rgb, r, b1, id;
		double dx, dz, dx2, dz2;
		int n = 0;
		for (int i1 = 0; i1 < 16; i1++)
		{
			for (int k1 = 0; k1 < 16; k1++)
			{
				x1 = (x + i1);// / scale;
				z1 = (z + k1);// / scale;

				HashSet<Site> sites = dorfs.getSiteForCoords(x1, z1);
				
				if(sites==null)
					continue;
				
				for(Site s: sites)
				{
					if(s.rgbmap==null)
						continue;
					
					int shiftX = (x1 - s.corners[0][0]*scale)*51/scale;
					int shiftZ = (z1 - s.corners[0][1]*scale)*51/scale;
					if(shiftX >= s.rgbmap.length || shiftZ >= s.rgbmap[0].length)
						continue;
					if(shiftX < 0 || shiftZ < 0 )
						continue;
					rgb = s.rgbmap[shiftX][shiftZ];
					SiteMapColours siteCol = SiteMapColours.getMatch(rgb);
					
					if(siteCol==null)
						continue;
					
					h = bicubicInterpolator.interpolate(WorldGenerator.instance.dorfs.elevationMap, x1, z1, scale);
					int j = h - 1;
					SiteStructures structs = structureGen.getStructuresForSite(s);
					StructureSpace struct = structs.getStructure(x1, z1, scale);
					if(struct != null)
					{
						j = struct.getFloor(s, scale) - 1;
						h = j+1;
					}
					
					Block[] repBlocks = SiteMapColours.getSurfaceBlocks(siteCol);
					
					index = j << 0 | (i1) << 12 | (k1) << 8;

					BiomeGenBase biome = biomes[i1+16*k1];
					
					Block surface =  repBlocks[1];
					Block above = repBlocks[2];
					
					if(surface==null && siteCol.toString().contains("ROOF"))
						surface = Blocks.brick_block;
					
					if(surface==null)// || blocks[index - 1] == Blocks.water || blocks[index] == Blocks.water)
						continue;
					blocks[index] = surface;
					index = (j - 1) << 0 | (i1) << 12 | (k1) << 8;
					blocks[index] = repBlocks[0];
					index = (j + 1) << 0 | (i1) << 12 | (k1) << 8;
					blocks[index] = above;
					boolean wall = siteCol.toString().contains("WALL");
					boolean roof = siteCol.toString().contains("ROOF");
					boolean tower = siteCol.toString().contains("TOWER");
					if(wall||roof)
					{
						int j1 = j + 1;
						int num = tower?10:5;
						while(j1<h+1)
						{
							j1 = j1 + 1;
							index = (j1) << 0 | (i1) << 12 | (k1) << 8;
							blocks[index] = null;
							index = (h + num) << 0 | (i1) << 12 | (k1) << 8;
							blocks[index] = surface;
						}
						j1 = j + 1;
						if(wall)
						{
							while(j1 < h + num)
							{
								j1 = j1 + 1;
								index = (j1) << 0 | (i1) << 12 | (k1) << 8;
								blocks[index] = surface;
							}
						}
					}
				}
			}
		}
	}
	
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
	
	private void safeSetToRoad(int x, int z, int chunkX, int chunkZ, Block[] blocks)
	{
		int h, index;
		
		int x1 = x - chunkX;
		int z1 = z - chunkZ;
		
		h = bicubicInterpolator.interpolate(WorldGenerator.instance.dorfs.elevationMap, x, z, scale);
		
		index = (h - 1) << 0 | (x1) << 12 | (z1) << 8;
		
		Block surface =  BlockRoadSurface.uggrass;
		
		if(index >= 0 && x1 < 16 && z1 < 16 && x1 >= 0 && z1 >= 0)
		{
			blocks[index] = surface;
			blocks[index - 1] = Blocks.cobblestone;
			blocks[index - 2] = Blocks.cobblestone;
		}
	}
	
	private void genSingleRoad(EnumFacing begin, EnumFacing end, int x, int z, int chunkX, int chunkZ, Block[] blocks)
	{
		int nearestEmbarkX = x - (x % scale);
		int nearestEmbarkZ = z - (z % scale);
		double interX, interZ;
		int nearestX, nearestZ;
		double startX = DIR_TO_RELX[dirToIndex(begin)];
		double startZ = DIR_TO_RELZ[dirToIndex(begin)];
		double endX = DIR_TO_RELX[dirToIndex(end)];
		double endZ = DIR_TO_RELZ[dirToIndex(end)];
		
		double c = ((double) scale)/2.;
		
		for(double i = -0.2; i <= 1.2; i += 0.02)
		{
			interX = (1.-i)*(1.-i)*startX + 2.*(1.-i)*i*c + i*i*endX;
			interZ = (1.-i)*(1.-i)*startZ + 2.*(1.-i)*i*c + i*i*endZ;
			
			nearestX = (int) interX;
			nearestZ = (int) interZ;
			
			safeSetToRoad(nearestX + nearestEmbarkX, nearestZ + nearestEmbarkZ, chunkX, chunkZ, blocks);
			
			safeSetToRoad(nearestX + nearestEmbarkX + 1, nearestZ + nearestEmbarkZ, chunkX, chunkZ, blocks);
			safeSetToRoad(nearestX + nearestEmbarkX, nearestZ + nearestEmbarkZ + 1, chunkX, chunkZ, blocks);
			safeSetToRoad(nearestX + nearestEmbarkX - 1, nearestZ + nearestEmbarkZ, chunkX, chunkZ, blocks);
			safeSetToRoad(nearestX + nearestEmbarkX, nearestZ + nearestEmbarkZ - 1, chunkX, chunkZ, blocks);
			
			safeSetToRoad(nearestX + nearestEmbarkX + 1, nearestZ + nearestEmbarkZ + 1, chunkX, chunkZ, blocks);
			safeSetToRoad(nearestX + nearestEmbarkX + 1, nearestZ + nearestEmbarkZ - 1, chunkX, chunkZ, blocks);
			safeSetToRoad(nearestX + nearestEmbarkX - 1, nearestZ + nearestEmbarkZ + 1, chunkX, chunkZ, blocks);
			safeSetToRoad(nearestX + nearestEmbarkX - 1, nearestZ + nearestEmbarkZ - 1, chunkX, chunkZ, blocks);
		}
	}
	
	private void genRoads(int x, int z, int chunkX, int chunkZ, Block[] blocks)
	{
		int nearestEmbarkX = x - (x % scale);
		int nearestEmbarkZ = z - (z % scale);
		
		boolean dirs[] = getRoadDirection(nearestEmbarkX, nearestEmbarkZ);
		
		for(int i = 0; i < 3; i++)
		{
			for(int j = i+1; j < 4; j++)
			{
				if(dirs[i] && dirs[j]) genSingleRoad(DIRS[i], DIRS[j], x, z, chunkX, chunkZ, blocks);
			}
		}
	}
	
	public void buildRoads(World world, int chunkX, int chunkZ, Block[] blocks, BiomeGenBase[] biomes)
	{
		int x = (chunkX * 16 - WorldGenerator.shift.posX);
		int z = (chunkZ * 16 - WorldGenerator.shift.posZ);
		
		genRoads(x - (x % scale), z - (z % scale), x, z, blocks);
		
		if((x+16) - ((x+16) % scale) > x - (x % scale))
		{
			if((z+16) - ((z+16) % scale) > z - (z % scale))
			{
				genRoads((x+16) - ((x+16) % scale), (z+16) - ((z+16) % scale), x, z, blocks);
			}
			else
			{
				genRoads((x+16) - ((x+16) % scale), z - (z % scale), x, z, blocks);
			}
		}
		else if((z+16) - ((z+16) % scale) > z - (z % scale))
		{
			genRoads(x - (x % scale), (z+16) - ((z+16) % scale), x, z, blocks);
		}
	}

	public static boolean[] getRoadDirection(int xAbs, int zAbs)
	{
		int pixelX = xAbs / (scale * 16);
		int pixelZ = zAbs / (scale * 16);
		
		boolean[] ret = new boolean[4];
		int b;
		
		HashSet<WorldConstruction> constructs = WorldGenerator.instance.dorfs.getConstructionsForCoords(xAbs, zAbs);
		
		if(constructs==null)
			return ret;
		
		for(WorldConstruction con: constructs)
		{
			if(!con.isInConstruct(xAbs, 0, zAbs))
				continue;
			
			if(con.isInConstruct(xAbs - scale, 0, zAbs))
			{
				ret[1] = true;
			}
			if(con.isInConstruct(xAbs + scale, 0, zAbs))
			{
				ret[0] = true;
			}
			if(con.isInConstruct(xAbs, 0, zAbs - scale))
			{
				ret[2] = true;
			}
			if(con.isInConstruct(xAbs, 0, zAbs + scale))
			{
				ret[3] = true;
			}
		}
		return ret;
	}
	
	public static Block getSurfaceBlockForSite(SiteTerrain site, int num)
	{
		switch (site)
		{
		case BUILDINGS: return num==0?Blocks.brick_block:null;
		case WALLS: return Blocks.stonebrick;
		case FARMYELLOW: return num==0?Blocks.sand:null;
		case FARMORANGE: return num==0?Blocks.dirt:null;
		case FARMLIMEGREEN: return num==0?Blocks.clay:null;
		case FARMORANGELIGHT: return num==0?Blocks.hardened_clay:null;
		case FARMGREEN: return num==0?Blocks.stained_hardened_clay:null;
		default: return null;
		}
	}
}
