package dorfgen.worldgen.common;

import java.util.Map;

import com.google.common.collect.Maps;

import dorfgen.conversion.Interpolator.BicubicInterpolator;

public class CachedInterpolator extends BicubicInterpolator
{
    private Map<Long, Integer> map0    = Maps.newHashMap();
    private Map<Long, Integer> map1    = Maps.newHashMap();
    private int[][]            lastMap;
    private int                lastChunkX;
    private int                lastChunkZ;
    private static final int   ZOFFSET = 1 << 30;

    public CachedInterpolator()
    {
        super();
    }

    public void clear()
    {
        map0.clear();
        map1.clear();
    }

    public CachedInterpolator initImage(int[][] image, int chunkX, int chunkZ, int size, int scale)
    {
        if (image == lastMap && chunkX == lastChunkX && chunkZ == lastChunkZ) { return this; }
        lastMap = image;
        lastChunkX = chunkX;
        lastChunkZ = chunkZ;
        map0.clear();
        int x0 = chunkX * 16 + 8;
        int z0 = chunkZ * 16 + 8;
        for (int i = -size; i <= size; i++)
            for (int k = -size; k <= size; k++)
            {
                int x1 = x0 + size;
                int z1 = z0 + size;
                if (x1 > 0 && z1 > 0 && x1 < image.length && z1 < image[0].length)
                {
                    long key = x1 + ((long) z1) * ZOFFSET;
                    int value = interpolate(image, x1, z1, scale);
                    map0.put(key, value);
                }
            }
        return this;
    }

    public CachedInterpolator initBiome(int[][] image, int chunkX, int chunkZ, int size, int scale)
    {
        if (image == lastMap && chunkX == lastChunkX && chunkZ == lastChunkZ) { return this; }
        lastMap = image;
        lastChunkX = chunkX;
        lastChunkZ = chunkZ;
        map1.clear();
        int x0 = chunkX * 16 + 8;
        int z0 = chunkZ * 16 + 8;
        for (int i = -size; i <= size; i++)
            for (int k = -size; k <= size; k++)
            {
                int x1 = x0 + size;
                int z1 = z0 + size;
                if (x1 > 0 && z1 > 0 && x1 < image.length && z1 < image[0].length)
                {
                    long key = x1 + ((long) z1) * ZOFFSET;
                    int value = interpolateBiome(image, x1, z1, scale);
                    map1.put(key, value);
                }
            }
        return this;
    }

    @Override
    public int interpolate(int[][] image, int xAbs, int yAbs, int scale)
    {
        long key = xAbs + ((long) yAbs) * ZOFFSET;
        if (map0.containsKey(key)) return map0.get(key);
        int value = super.interpolate(image, xAbs, yAbs, scale);
        map0.put(key, value);
        return value;
    }

    @Override
    public int interpolateBiome(int[][] image, int xAbs, int yAbs, int scale)
    {
        long key = xAbs + ((long) yAbs) * ZOFFSET;
        if (map1.containsKey(key)) return map1.get(key);
        int value = super.interpolateBiome(image, xAbs, yAbs, scale);
        map1.put(key, value);
        return value;
    }
}
