package dorfgen.worldgen.common;

import java.util.List;

import dorfgen.WorldGenerator;
import dorfgen.conversion.DorfMap;
import dorfgen.conversion.Interpolator.BicubicInterpolator;
import dorfgen.conversion.Interpolator.CachedBicubicInterpolator;
import net.minecraft.init.Biomes;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;
import net.minecraft.world.biome.Biome;
import net.minecraft.world.biome.BiomeProvider;
import net.minecraft.world.gen.structure.MapGenVillage;

public class BiomeProviderFinite extends BiomeProvider
{
    /** The biomes that are used to generate the chunk */
    private Biome[]                  biomesForGeneration = new Biome[256];

    // TODO cache the results of these for x,z coords, see if that speeds up
    // cubic chunks support.
    public BicubicInterpolator       biomeInterpolator   = new BicubicInterpolator();
    public CachedBicubicInterpolator heightInterpolator  = new CachedBicubicInterpolator();
    public CachedBicubicInterpolator miscInterpolator    = new CachedBicubicInterpolator();
    public final DorfMap             map;
    public int                       scale;

    public BiomeProviderFinite()
    {
        super();
        map = null;
    }

    public BiomeProviderFinite(World world)
    {
        super(world.getWorldInfo());
        String var = world.getWorldInfo().getGeneratorOptions();
        GeneratorInfo info = GeneratorInfo.fromJson(var);
        map = WorldGenerator.getDorfMap(info.region);
        this.scale = map.scale;
    }

    @Override
    /** Returns the Biome related to the x, z position on the world. */
    public Biome getBiome(BlockPos pos)
    {
        try
        {
            return super.getBiome(pos);
        }
        catch (Exception e)
        {
            System.out.println(pos);
            e.printStackTrace();
        }
        return Biomes.OCEAN;
    }

    @Override
    /** Returns an array of biomes for the location input. */
    public Biome[] getBiomesForGeneration(Biome[] biomes, int x, int z, int width, int length)
    {
        biomes = super.getBiomesForGeneration(biomes, x, z, width, length);
        if (x >= 0 && z >= 0 && (x + 16) / scale <= map.biomeMap.length && (z + 16) / scale <= map.biomeMap[0].length)
        {
            makeBiomes(biomes, scale, x, z);
        }

        return biomes;
    }

    @Override
    /** Returns biomes to use for the blocks and loads the other data like
     * temperature and humidity onto the WorldChunkManager Args: oldBiomeList,
     * x, z, width, depth */
    public Biome[] getBiomes(Biome[] biomes, int x, int z, int width, int depth)
    {
        biomes = super.getBiomesForGeneration(biomes, x, z, width, depth);
        x = map.shiftX(x);
        z = map.shiftZ(z);

        if (x >= 0 && z >= 0 && (x + 16) / scale <= map.biomeMap.length && (z + 16) / scale <= map.biomeMap[0].length)
        {
            x = map.unShiftX(x);
            z = map.unShiftZ(z);
            return biomes = makeBiomes(biomes, scale, x, z);
        }
        return biomes;
    }

    /** Takes Blocks Coordinates
     * 
     * @param scale
     *            - number of blocks per pixel
     * @param x
     *            - x coordinate of the block being used
     * @param z
     *            - y coordinate of the block being used
     * @param blocks */
    private Biome[] makeBiomes(Biome[] biomes, int scale, int x, int z)
    {
        int index;
        for (int i1 = 0; i1 < 16; i1++)
        {
            for (int k1 = 0; k1 < 16; k1++)
            {
                index = (i1) + (k1) * 16;
                biomes[index] = getBiomeFromMaps(map.shiftX(x + i1), map.shiftZ(z + k1));
            }
        }
        return biomes;
    }

    private Biome getBiomeFromMaps(int x, int z)
    {
        return map.biomeList.mutateBiome(null, x, z, map);
    }

    @Override
    /** checks given Chunk's Biomes against List of allowed ones */
    public boolean areBiomesViable(int p1_, int p2_, int p3_, List<Biome> biomes)
    {
        if (biomes == MapGenVillage.VILLAGE_SPAWN_BIOMES) return true;

        biomesForGeneration = getBiomesForGeneration(biomesForGeneration, p1_, p2_, 0, 0);
        for (Biome b : biomesForGeneration)
        {
            if (b != null)
            {
                if (biomes.contains(b)) return true;
            }
        }
        return false;
    }
}
