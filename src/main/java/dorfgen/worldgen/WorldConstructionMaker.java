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
import dorfgen.conversion.DorfMap.WorldConstruction;
import dorfgen.conversion.Interpolator.BicubicInterpolator;
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
				x1 = (x + i1) / scale;
				z1 = (z + k1) / scale;

				dx = (x + i1 - scale * x1) / (double) scale;
				dz = (z + k1 - scale * z1) / (double) scale;

				h = bicubicInterpolator.interpolate(WorldGenerator.instance.dorfs.elevationMap, x + i1, z + k1, scale);
				
				rgb = bicubicInterpolator.interpolateBiome(dorfs.structureMap,  x + i1, z + k1, scale);
				
				SiteTerrain site = SiteTerrain.getMatch(rgb);
				if(site==null)
					continue;
				
				int j = h - 1;
				
				index = j << 0 | (i1) << 12 | (k1) << 8;

				BiomeGenBase biome = biomes[i1+16*k1];
				
				Block surface = getSurfaceBlockForSite(site, 0);
				Block above = getSurfaceBlockForSite(site, 1);
				if(surface==null || blocks[index - 1] == Blocks.water || blocks[index] == Blocks.water)
					continue;
				blocks[index] = surface;
				index = (j - 1) << 0 | (i1) << 12 | (k1) << 8;
				blocks[index] = Blocks.cobblestone;
				index = (j + 1) << 0 | (i1) << 12 | (k1) << 8;
				blocks[index] = above;

			}
		}
	}

	public void buildRoads(World world, int chunkX, int chunkZ, Block[] blocks, BiomeGenBase[] biomes)
	{
		int index;
		int x = (chunkX * 16 - WorldGenerator.shift.posX);
		int z = (chunkZ * 16 - WorldGenerator.shift.posZ);
		int x1, z1, h, w, r, b1, id;
		double dx, dz, dx2, dz2;
		int n = 0;
		for (int i1 = 0; i1 < 16; i1++)
		{
			for (int k1 = 0; k1 < 16; k1++)
			{
				x1 = (x + i1) / scale;
				z1 = (z + k1) / scale;

				dx = (x + i1 - scale * x1) / (double) scale;
				dz = (z + k1 - scale * z1) / (double) scale;

				h = bicubicInterpolator.interpolate(WorldGenerator.instance.dorfs.elevationMap, x + i1, z + k1, scale);

				HashSet<WorldConstruction> constructs = WorldGenerator.instance.dorfs.getConstructionsForCoords(x + i1, z + k1);
				
				if(constructs == null)
					continue;
				
				EnumFacing[] dirs = getRoadDirection(x + i1, z + k1);
				
				dx2 = dz2 = 0;
				int j = h - 1;

				if (!shouldRoadPlace(x + i1, z + k1, dirs, 2)) continue;

				index = j << 0 | (i1) << 12 | (k1) << 8;

				BiomeGenBase biome = biomes[i1+16*k1];
				
				Block surface =  BlockRoadSurface.uggrass;
				
				blocks[index] = surface;
				blocks[index - 1] = Blocks.cobblestone;

			}
		}
	}

	public boolean shouldRoadPlace(int xAbs, int zAbs, EnumFacing[] dirs, int width)
	{
		int xRel = xAbs % (scale);
		int zRel = zAbs % (scale);

		int shift = scale / 2;

		boolean in = false;
		
		HashSet<WorldConstruction> constructs = WorldGenerator.instance.dorfs.getConstructionsForCoords(xAbs, zAbs);
		
		for(WorldConstruction con: constructs)
		{
			if(con.isInConstruct(xAbs, 0, zAbs))
			{
				in = true;
				break;
			}
		}
		
		if(!in)
			return false;
		
		
		// no NS
		if (dirs[2] == null && dirs[3] == null)
		{
			// EW
			if (dirs[0] != null && dirs[1] != null)
			{
				return zRel <= shift + width / 2 && zRel >= shift - width / 2;
			}
			else if (dirs[0] != null) // E
			{
				return zRel <=shift + width / 2;
			}
			else if (dirs[1] != null) // W
			{ return zRel >= shift - width / 2; }
		}
		// no EW
		if (dirs[0] == null && dirs[1] == null)
		{
			// NS
			if (dirs[2] != null && dirs[3] != null)
			{
				return xRel <= shift + width / 2 && xRel >= shift - width / 2;
			}
			else if (dirs[2] != null) // N
			{
				return xRel <= shift + width / 2;
			}
			else if (dirs[3] != null) // S
			{ return xRel >= shift - width / 2; }
		}
		// NE
		if (dirs[0] != null && dirs[2] != null && dirs[1] == null && dirs[3] == null)
		{
			if (xRel <= shift + width / 2 && xRel >= shift - width / 2 && zRel <= shift + width / 2) return true;
			if (zRel <= shift + width / 2 && zRel >= shift - width / 2 && xRel >= shift + width / 2) return true;
			return false;
		}
		// NW
		if (dirs[1] != null && dirs[2] != null && dirs[0] == null && dirs[3] == null)
		{
			if (xRel <= shift + width / 2 && xRel >= shift - width / 2 && zRel <= shift + width / 2) return true;
			if (zRel <= shift + width / 2 && zRel >= shift - width / 2 && xRel <= shift + width / 2) return true;
			return false;
		}
		// SE
		if (dirs[1] != null && dirs[3] != null && dirs[0] == null && dirs[2] == null)
		{
			if (xRel <= shift + width / 2 && xRel >= shift - width / 2 && zRel >= shift + width / 2) return true;
			if (zRel <= shift + width / 2 && zRel >= shift - width / 2 && xRel <= shift + width / 2) return true;
			return false;
		}
		// SW
		if (dirs[0] != null && dirs[3] != null && dirs[1] == null && dirs[2] == null)
		{
			if (xRel <= shift + width / 2 && xRel >= shift - width / 2 && zRel >= shift - width / 2) return true;
			if (zRel <= shift + width / 2 && zRel >= shift - width / 2 && xRel >= shift + width / 2) return true;
			return false;
		}
		

		// NSE
		if (dirs[0] != null && dirs[2] != null && dirs[1] == null && dirs[3] != null)
		{
			if (xRel <= shift + width / 2 && xRel >= shift - width / 2 && zRel <= shift + width / 2) return true;
			if (zRel <= shift + width / 2 && zRel >= shift - width / 2 && xRel >= shift + width / 2) return true;
			if (xRel <= shift + width / 2 && xRel >= shift - width / 2) return true;//This part is just NS check
			return false;
		}
		// NSW
		if (dirs[1] != null && dirs[2] != null && dirs[0] == null && dirs[3] != null)
		{
			if (xRel <= shift + width / 2 && xRel >= shift - width / 2 && zRel <= shift + width / 2) return true;
			if (zRel <= shift + width / 2 && zRel >= shift - width / 2 && xRel <= shift + width / 2) return true;
			if (xRel <= shift + width / 2 && xRel >= shift - width / 2) return true;//This part is just NS check
			return false;
		}
		
		// SEW
		if (dirs[0] != null && dirs[3] != null && dirs[1] != null && dirs[2] == null)
		{
			if (xRel <= shift + width / 2 && xRel >= shift - width / 2 && zRel >= shift + width / 2) return true;
			if (zRel <= shift + width / 2 && zRel >= shift - width / 2 && xRel <= shift + width / 2) return true;
			if (zRel <= shift + width / 2 && zRel >= shift - width / 2) return true;//This part is just EW check
			return false;
		}

		// NWE
		if (dirs[1] != null && dirs[2] != null && dirs[0] != null && dirs[3] == null)
		{
			if (xRel <= shift + width / 2 && xRel >= shift - width / 2 && zRel <= shift + width / 2) return true;
			if (zRel <= shift + width / 2 && zRel >= shift - width / 2 && xRel <= shift + width / 2) return true;
			if (zRel <= shift + width / 2 && zRel >= shift - width / 2) return true;//This part is just EW check
			return false;
		}
		return false;
	}

	static EnumFacing[] dirs = { EAST, WEST, NORTH, SOUTH };

	public static EnumFacing[] getRoadDirection(int xAbs, int zAbs)
	{
		int pixelX = xAbs / (scale * 16);
		int pixelZ = zAbs / (scale * 16);
		
		EnumFacing[] ret = new EnumFacing[4];
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
				ret[1] = dirs[1];
			}
			if(con.isInConstruct(xAbs + scale, 0, zAbs))
			{
				ret[0] = dirs[0];
			}
			if(con.isInConstruct(xAbs, 0, zAbs - scale))
			{
				ret[2] = dirs[2];
			}
			if(con.isInConstruct(xAbs, 0, zAbs + scale))
			{
				ret[3] = dirs[3];
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
