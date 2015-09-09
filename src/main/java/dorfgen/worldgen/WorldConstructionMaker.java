package dorfgen.worldgen;

import static dorfgen.WorldGenerator.scale;
import static net.minecraft.util.EnumFacing.EAST;
import static net.minecraft.util.EnumFacing.NORTH;
import static net.minecraft.util.EnumFacing.SOUTH;
import static net.minecraft.util.EnumFacing.WEST;

import java.util.HashSet;

import dorfgen.WorldGenerator;
import dorfgen.conversion.DorfMap;
import dorfgen.conversion.DorfMap.ConstructionType;
import dorfgen.conversion.DorfMap.WorldConstruction;
import dorfgen.conversion.Interpolator.BicubicInterpolator;
import net.minecraft.block.Block;
import net.minecraft.init.Blocks;
import net.minecraft.util.EnumFacing;
import net.minecraft.world.World;

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
			x = x / scale;
			z = z / scale;
			int x1 = (x) / 16;
			int z1 = (z) / 16;
			int x2 = (x) / 16;
			int z2 = (z + dz / scale) / 16;
			int x3 = (x) / 16;
			int z3 = (z - dz / scale) / 16;
			int x4 = (x + dx / scale) / 16;
			int z4 = (z) / 16;
			int x5 = (x - dx / scale) / 16;
			int z5 = (z) / 16;
			int key = (x1) + 2048 * (z1);
			int key2 = (x2) + 2048 * (z2);
			int key3 = (x3) + 2048 * (z3);
			int key4 = (x4) + 2048 * (z4);
			int key5 = (x5) + 2048 * (z5);

			boolean mid = true;
			EnumFacing[] dirs = getRoadDirection(chunkX * 16, chunkZ * 16);
			if (dirs[0] != null)
			{
				mid = mid && DorfMap.constructionsByCoord.containsKey(key5);
			}
			else if (dirs[1] == null)
			{
				mid = mid && key != key5;
			}
			if (dirs[1] != null)
			{
				mid = mid && DorfMap.constructionsByCoord.containsKey(key4);

			}
			else if (dirs[0] == null)
			{
				mid = mid && key != key4;
			}
			if (dirs[2] != null)
			{
				mid = mid && DorfMap.constructionsByCoord.containsKey(key3);
			}
			else if (dirs[3] == null)
			{
				mid = mid && key != key3;
			}
			if (dirs[3] != null)
			{
				mid = mid && DorfMap.constructionsByCoord.containsKey(key2);
			}
			else if (dirs[2] == null)
			{
				mid = mid && key != key2;
			}
			if (mid && DorfMap.constructionsByCoord.containsKey(key))
			{
				for (WorldConstruction construct : DorfMap.constructionsByCoord.get(key))
				{
					if (construct.type == type) return true;
				}
				return false;
			}
			return false;
		}
		return false;
	}

	public void buildRoads(World world, int chunkX, int chunkZ, Block[] blocks)
	{
		if (!shouldConstruct(chunkX, chunkZ, ConstructionType.ROAD)) return;
		EnumFacing[] dirs = getRoadDirection(chunkX * 16, chunkZ * 16);

		//Deal with corners
		if ((dirs[1] == null && dirs[0] != null))
		{
			int z1 = ((chunkZ / scale) * scale + scale) - chunkZ;
			if (z1 > scale / 2) return;
		}
		if ((dirs[2] == null && dirs[3] != null))
		{
			int x1 = ((chunkX / scale) * scale + scale) - chunkX;
			if (x1 < scale / 2) return;
		}
		if ((dirs[0] == null && dirs[1] != null))
		{
			int z1 = ((chunkZ / scale) * scale + scale) - chunkZ;
			if (z1 < scale / 2) return;
		}
		if ((dirs[3] == null && dirs[2] != null))
		{
			int x1 = ((chunkX / scale) * scale + scale) - chunkX;
			if (x1 > scale / 2) return;
		}

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

				dx2 = dz2 = 0;
				int j = h - 1;

				if (!shouldRoadPlace(x + i1, z + k1, dirs, 4)) continue;

				index = j << 0 | (i1) << 12 | (k1) << 8;

				blocks[index] = Blocks.gravel;
				blocks[index - 1] = Blocks.cobblestone;

			}
		}
	}

	public boolean shouldRoadPlace(int xAbs, int zAbs, EnumFacing[] dirs, int width)
	{
		int xRel = xAbs % (16 * scale);
		int zRel = zAbs % (16 * scale);

		int shift = scale * 16 / 2;

		// System.out.println("z:" + zRel + " x:" + xRel);

		// no NS
		if (dirs[2] == null && dirs[3] == null)
		{
			// EW
			if (dirs[0] != null && dirs[1] != null)
			{
				return zRel < shift + width / 2 && zRel > shift - width / 2;
			}
			else if (dirs[0] != null) // E
			{
				return zRel < shift + width / 2;
			}
			else if (dirs[1] != null) // W
			{ return zRel > shift - width / 2; }
		}
		// no EW
		if (dirs[0] == null && dirs[1] == null)
		{
			// NS
			if (dirs[2] != null && dirs[3] != null)
			{
				return xRel < shift + width / 2 && xRel > shift - width / 2;
			}
			else if (dirs[2] != null) // N
			{
				return xRel < shift + width / 2;
			}
			else if (dirs[3] != null) // S
			{ return xRel > shift - width / 2; }
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
		if (dirs[0] != null && dirs[3] != null && dirs[1] == null && dirs[2] == null)
		{
			if (xRel <= shift + width / 2 && xRel >= shift - width / 2 && zRel >= shift + width / 2) return true;
			if (zRel <= shift + width / 2 && zRel >= shift - width / 2 && xRel <= shift + width / 2) return true;
			return false;
		}
		// SW
		if (dirs[1] != null && dirs[3] != null && dirs[0] == null && dirs[2] == null)
		{
			if (xRel <= shift + width / 2 && xRel >= shift - width / 2 && zRel >= shift + width / 2) return true;
			if (zRel <= shift + width / 2 && zRel >= shift - width / 2 && xRel >= shift + width / 2) return true;
			return false;
		}

		return true;
	}

	static EnumFacing[] dirs = { EAST, WEST, NORTH, SOUTH };

	public static EnumFacing[] getRoadDirection(int xAbs, int zAbs)
	{
		int pixelX = xAbs / (scale * 16);
		int pixelZ = zAbs / (scale * 16);

		EnumFacing[] ret = new EnumFacing[4];
		int b;
		for (int i = 0; i < 4; i++)
		{
			int x1 = pixelX + dirs[i].getFrontOffsetX();
			int z1 = pixelZ + dirs[i].getFrontOffsetZ();
			int index = x1 + 2048 * z1;
			HashSet<WorldConstruction> constructs = DorfMap.constructionsByCoord.get(index);
			if (constructs != null)
			{
				for (WorldConstruction c : constructs)
				{
					if (c.type == ConstructionType.ROAD)
					{
						ret[i] = dirs[i];
						break;
					}
				}
			}
		}
		return ret;
	}
}
