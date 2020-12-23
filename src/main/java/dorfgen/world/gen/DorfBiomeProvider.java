package dorfgen.world.gen;

import dorfgen.Dorfgen;
import dorfgen.conversion.DorfMap;
import dorfgen.util.Interpolator.BicubicInterpolator;
import dorfgen.util.Interpolator.CachedBicubicInterpolator;
import net.minecraft.util.math.MathHelper;
import net.minecraft.world.biome.Biome;
import net.minecraft.world.biome.BiomeContainer;
import net.minecraft.world.biome.Biomes;
import net.minecraft.world.biome.provider.BiomeProvider;
import net.minecraft.world.chunk.IChunk;
import net.minecraft.world.gen.feature.structure.Structure;

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

    int                 scale;
    public boolean      forGen = false;
    public DorfMap      map;
    final GeneratorInfo info;

    protected DorfBiomeProvider(final DorfSettings settings)
    {
        // Allow all the biomes!
        super(Biome.BIOMES);
        this.info = settings.getInfo();
    }

    private Biome getBiomeFromMaps(final int x, final int z)
    {
        final Biome input = this.map.biomeInterpolator.interpolateBiome(this.map.biomeList.biomeArr, x, z, this.scale);
        if (input == null)
        {
            Dorfgen.LOGGER.error("Null biome for {}, {}", x, z);
            return Biomes.DEFAULT;
        }
        return input;
    }

    @Override
    public boolean hasStructure(final Structure<?> structureIn)
    {
        if (!this.info.villages && structureIn.getRegistryName().toString().contains("village")) return false;
        return super.hasStructure(structureIn);
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
        this.scale = this.map.getScale();

        if (this.scale == 0)
        {
            System.out.println("wat?");
            return Biomes.DEFAULT;
        }

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
