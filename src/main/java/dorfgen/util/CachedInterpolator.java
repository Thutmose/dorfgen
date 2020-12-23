package dorfgen.util;

import java.util.Map;

import com.google.common.collect.Maps;

import dorfgen.util.Interpolator.BicubicInterpolator;

public class CachedInterpolator extends BicubicInterpolator
{
    private final Map<Long, Integer> map0    = Maps.newHashMap();
    private final Map<Long, Integer> map1    = Maps.newHashMap();
    private int[][]                  lastMap;
    private int                      lastChunkX;
    private int                      lastChunkZ;
    private static final int         ZOFFSET = 1 << 30;

    public CachedInterpolator()
    {
        super();
    }

    public void clear()
    {
        this.map0.clear();
        this.map1.clear();
    }

    public CachedInterpolator initImage(final int[][] image, final int chunkX, final int chunkZ, final int size,
            final int scale)
    {
        if (image == this.lastMap && chunkX == this.lastChunkX && chunkZ == this.lastChunkZ) return this;
        this.lastMap = image;
        this.lastChunkX = chunkX;
        this.lastChunkZ = chunkZ;
        this.map0.clear();
        final int x0 = chunkX * 16 + 8;
        final int z0 = chunkZ * 16 + 8;
        for (int i = -size; i <= size; i++)
            for (int k = -size; k <= size; k++)
            {
                final int x1 = x0 + size;
                final int z1 = z0 + size;
                if (x1 > 0 && z1 > 0 && x1 < image.length && z1 < image[0].length)
                {
                    final long key = x1 + (long) z1 * CachedInterpolator.ZOFFSET;
                    final int value = this.interpolate(image, x1, z1, scale);
                    this.map0.put(key, value);
                }
            }
        return this;
    }

    @Override
    public int interpolate(final int[][] image, final int xAbs, final int yAbs, final int scale)
    {
        if (image != this.lastMap) this.map0.clear();
        final long key = xAbs + (long) yAbs * CachedInterpolator.ZOFFSET;
        if (this.map0.containsKey(key)) return this.map0.get(key);
        final int value = this.interpolate2(image, xAbs, yAbs, scale);
        this.map0.put(key, value);
        return value;
    }

    private int interpolate2(final int[][] image, final int xAbs, final int yAbs, final int scale)
    {
        final int pixelX = xAbs / scale;
        final int pixelY = yAbs / scale;
        final double x = (xAbs - scale * pixelX) / (double) scale, y = (yAbs - scale * pixelY) / (double) scale;
        final double[][] arr = new double[4][4];
        int num = 0;
        double sum = 0;
        int min = Integer.MAX_VALUE;
        for (int i = -1; i <= 2; i++)
            for (int k = -1; k <= 2; k++)
            {
                final int locX = pixelX + i;
                final int locY = pixelY + k;
                int value;
                if (locX >= 0 && locX < image.length && locY >= 0 && locY < image[0].length) value = image[locX][locY];
                else value = image[pixelX][pixelY];
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
        final int avg = (int) Math.round(sum / num);
        for (int i = -1; i <= 2; i++)
            for (int k = -1; k <= 2; k++)
                if (arr[i + 1][k + 1] == -1) arr[i + 1][k + 1] = avg;
        // Cubic splines result in some areas being dipped down, we don't want
        // that, so cap this at the minimum value found.
        num = (int) Math.round(this.getValue(arr, x, y));
        return Math.max(min, num);
    }
}