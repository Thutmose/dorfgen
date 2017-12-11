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
        if (image != lastMap) map0.clear();
        long key = xAbs + ((long) yAbs) * ZOFFSET;
        if (map0.containsKey(key)) return map0.get(key);
        int value = interpolate2(image, xAbs, yAbs, scale);
        map0.put(key, value);
        return value;
    }

    private int interpolate2(int[][] image, int xAbs, int yAbs, int scale)
    {
        int pixelX = xAbs / scale;
        int pixelY = yAbs / scale;
        double x = (xAbs - scale * pixelX) / (double) scale, y = (yAbs - scale * pixelY) / (double) scale;
        double[][] arr = new double[4][4];
        int num = 0;
        double sum = 0;
        int min = Integer.MAX_VALUE;
        for (int i = -1; i <= 2; i++)
            for (int k = -1; k <= 2; k++)
            {
                int locX = pixelX + i;
                int locY = pixelY + k;
                int value;
                if (locX >= 0 && locX < image.length && locY >= 0 && locY < image[0].length)
                {
                    value = image[locX][locY];
                }
                else
                {
                    value = image[pixelX][pixelY];
                }
                if (value != -1)
                {
                    num++;
                    sum += value;
                    min = Math.min(value, min);
                }
                arr[i + 1][k + 1] = value;
            }
        if (min == Integer.MAX_VALUE) min = 0;

        // Values of -1 are used for "no data", so replace them with an average.
        // Maybe this should replace with nearest neighbour instead?
        int avg = (int) Math.round(sum / num);
        for (int i = -1; i <= 2; i++)
            for (int k = -1; k <= 2; k++)
            {
                if (arr[i + 1][k + 1] == -1) arr[i + 1][k + 1] = avg;
            }
        // Cubic splines result in some areas being dipped down, we don't want
        // that, so cap this at the minimum value found.
        num = (int) Math.round(getValue(arr, x, y));
        return Math.max(min, num);
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
