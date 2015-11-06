package dorfgen.worldgen;

import static dorfgen.WorldGenerator.*;
import static net.minecraft.util.EnumFacing.*;

import dorfgen.WorldGenerator;
import dorfgen.conversion.Interpolator.BicubicInterpolator;
import dorfgen.conversion.Interpolator.CachedBicubicInterpolator;
import net.minecraft.block.Block;
import net.minecraft.init.Blocks;
import net.minecraft.util.EnumFacing;
import net.minecraft.world.World;
import net.minecraft.world.biome.BiomeGenBase;

public class RiverMaker {
	public static BicubicInterpolator bicubicInterpolator = new BicubicInterpolator();
	
	public RiverMaker() {
		// TODO Auto-generated constructor stub
	}

	public void makeRiversForChunk(World world, int chunkX, int chunkZ, Block[] blocks, BiomeGenBase[] biomes) {
		int index;
		int x = (chunkX * 16 - WorldGenerator.shift.posX);
		int z = (chunkZ * 16 - WorldGenerator.shift.posZ);
		int x1, z1, h, w, r, b1, id;
		boolean water = false;
		double dx, dz, dx2, dz2;
		int n = 0;
		for (int i1 = 0; i1 < 16; i1++) {
			for (int k1 = 0; k1 < 16; k1++) {
				x1 = (x + i1) / scale;
				z1 = (z + k1) / scale;
				
				dx = (x + i1 - scale * x1)/(double)scale;
				dz = (z + k1 - scale * z1)/(double)scale;
				
				h = bicubicInterpolator.interpolate(WorldGenerator.instance.dorfs.elevationMap, x + i1, z + k1, scale);
				r = bicubicInterpolator.interpolate(WorldGenerator.instance.dorfs.riverMap, x + i1, z + k1, scale);
				
				id = biomes[i1+16*k1].biomeID;
				boolean river;
				river = r > 0;
				if(!river)
					continue;
				
				dx2 = dz2 = 0;
				int j = 0;
				for(j = world.getHeight()-1; j>1; j--)
				{
					index = j << 0 | (i1) << 12 | (k1) << 8;
					if(blocks[index]!=null)
					{
						break;
					}
				}
				h = Math.max(j, (int) (world.provider.getHorizon()));
				int dh = Math.max(0, ((r - 80)/8));
				dh = Math.min(dh, 8);
				for (j = h - dh; j < world.getHeight()-1; j++) {
					index = j << 0 | (i1) << 12 | (k1) << 8;

					if (j == dh)
						blocks[index] = Blocks.stone;
					else if (j < h) {
						blocks[index] = Blocks.water;
					} else if(j >= h && r > 80)
						blocks[index] = null;
				}
			}
		}
	}
	static EnumFacing[] dirs = {EAST,WEST,NORTH,SOUTH};
	public static EnumFacing[] getRiverDirection(int xAbs, int zAbs, int width)
	{
		int pixelX = xAbs/scale;
		int pixelY = zAbs/scale;
		int r = bicubicInterpolator.interpolate(WorldGenerator.instance.dorfs.riverMap, xAbs, zAbs, scale);
		
		EnumFacing[] ret = new EnumFacing[4];
		int b;
		for(int i = 0; i<4; i++)
		{
			int x1 = xAbs + dirs[i].getFrontOffsetX() * width;
			int z1 = zAbs + dirs[i].getFrontOffsetZ() * width;
			
			r = bicubicInterpolator.interpolate(WorldGenerator.instance.dorfs.riverMap, x1, z1, scale);
			
			if(r>0)
			{
				ret[i] = dirs[i];
			}
		}
		return ret;
	}

}
