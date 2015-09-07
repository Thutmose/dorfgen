package dorfgen.worldgen;

import static dorfgen.WorldGenerator.*;
import static net.minecraft.util.EnumFacing.*;

import dorfgen.WorldGenerator;
import dorfgen.conversion.Interpolator.BicubicInterpolator;
import dorfgen.conversion.Interpolator.CachedBicubicInterpolator;
import net.minecraft.block.Block;
import net.minecraft.init.Blocks;
import net.minecraft.util.EnumFacing;
import net.minecraft.world.biome.BiomeGenBase;

public class RiverMaker {
	public BicubicInterpolator bicubicInterpolator = new BicubicInterpolator();
	
	public RiverMaker() {
		// TODO Auto-generated constructor stub
	}

	public void makeRiversForChunk(int chunkX, int chunkY, Block[] blocks, BiomeGenBase[] biomes) {
		int index;
		int x = (chunkX * 16 - WorldGenerator.shift.posX)/scale;
		int z = (chunkX * 16 - WorldGenerator.shift.posZ)/scale;
		int x1, z1, h, w, r, h1, b1, id;
		boolean water = false;
		double dx, dz, dx2, dz2;
		int n = 0;
		for (int i1 = 0; i1 < 16; i1++) {
			for (int k1 = 0; k1 < 16; k1++) {
				x1 = x + i1 / scale;
				z1 = z + k1 / scale;
				dx = (i1 % scale) / (double) scale;
				dz = (k1 % scale) / (double) scale;
				h = h1 = WorldGenerator.instance.dorfs.elevationMap[x1][z1];
				r = WorldGenerator.instance.dorfs.riverMap[x1][z1];
				id = biomes[i1+16*k1].biomeID;
				boolean river = id == BiomeGenBase.river.biomeID
						|| id == BiomeGenBase.frozenRiver.biomeID;
				
				if(instance.dorfs.biomeMap.length>0)
				{
				//	river = river || BiomeGenBase.river.biomeID == bicubicInterpolator.interpolateBiome(instance.dorfs.biomeMap, x1, z1, dx, dz);
				}
				
				if(!river)
					continue;
				dx2 = dz2 = 0;
				int j = 0;
				for(j = 255; j>1; j--)
				{
					index = j << 0 | (i1) << 12 | (k1) << 8;
					if(blocks[index]!=null)
					{
						break;
					}
				}
				h = Math.max(j, 63);

				for (j = h - scale / 2; j < 255; j++) {
					index = j << 0 | (i1) << 12 | (k1) << 8;

					if (j == h - scale / 2)
						blocks[index] = Blocks.stone;
					else if (j < h) {
						blocks[index] = Blocks.water;
					} else if(j >= h)
						blocks[index] = null;
				}
			}
		}
	}
	EnumFacing[] dirs = {EAST,WEST,NORTH,SOUTH};
	EnumFacing[] getRiverDirection(int pixelX, int pixelY)
	{
		EnumFacing[] ret = new EnumFacing[4];
		int b;
		for(int i = 0; i<4; i++)
		{
			int x = pixelX + dirs[i].getFrontOffsetX();
			int y = pixelY + dirs[i].getFrontOffsetY();
			if(WorldGenerator.instance.dorfs.riverMap[x][y]>0)
			{
				ret[i] = dirs[(i+2)%4];
			}
		}
		return ret;
	}

}
