package dorfgen.world.gen;

import java.util.Collections;

import dorfgen.conversion.DorfMap;
import dorfgen.util.Interpolator.BicubicInterpolator;
import dorfgen.util.Interpolator.CachedBicubicInterpolator;
import net.minecraft.util.math.MathHelper;
import net.minecraft.world.biome.Biome;
import net.minecraft.world.biome.BiomeContainer;
import net.minecraft.world.biome.Biomes;
import net.minecraft.world.biome.provider.BiomeProvider;
import net.minecraft.world.chunk.IChunk;

public class DorfBiomeProvider extends BiomeProvider
{
    public static int getBiomeContainerIndex(final int worldX, final int worldY, final int worldZ)
    {
        final int i = worldX & BiomeContainer.HORIZONTAL_MASK;
        final int j = MathHelper.clamp(worldY, 0, BiomeContainer.VERTICAL_MASK);
        final int k = worldZ & BiomeContainer.HORIZONTAL_MASK;
        return j << BiomeContainer.WIDTH_BITS + BiomeContainer.WIDTH_BITS | k << BiomeContainer.WIDTH_BITS | i;
    }

    public static void setBiome(final IChunk c, final int worldX, final int worldY, final int worldZ, final Biome b)
    {

    }

    public BicubicInterpolator       biomeInterpolator  = new BicubicInterpolator();
    public CachedBicubicInterpolator heightInterpolator = new CachedBicubicInterpolator();
    public CachedBicubicInterpolator miscInterpolator   = new CachedBicubicInterpolator();

    int            scale;
    public boolean forGen = false;
    public DorfMap map;

    protected DorfBiomeProvider(final DorfSettings settings)
    {
        super(Collections.emptySet());
    }

    private Biome getBiomeFromMaps(final int x, final int z)
    {
        return this.map.biomeList.mutateBiome(null, x, z, this.map);
    }

    @Override
    public Biome getNoiseBiome(int x, final int y, int z)
    {
        // This accounts for the effects of the biome magnifier during worldgen
        if (this.forGen)
        {
            x = x << 2;// + (x & 2);
            z = z << 2;// + (z & 2);
        }
        x = this.map.shiftX(x);
        z = this.map.shiftZ(z);
        this.scale = this.map.scale;

        if (x < 0) x = 0;
        if (z < 0) z = 0;
        if (x / this.scale >= this.map.biomeMap.length) x = this.map.biomeMap.length * this.scale - 1;
        if (z / this.scale >= this.map.biomeMap[0].length) z = this.map.biomeMap[0].length * this.scale - 1;

        if (x >= 0 && z >= 0 && x / this.scale <= this.map.biomeMap.length && z
                / this.scale <= this.map.biomeMap[0].length)
        {
            x = this.map.unShiftX(x);
            z = this.map.unShiftZ(z);
            return this.getBiomeFromMaps(this.map.shiftX(x), this.map.shiftZ(z));
        }
        return Biomes.DEFAULT;
    }

}
