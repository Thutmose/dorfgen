package dorfgen.worldgen.vanilla;

import static dorfgen.WorldGenerator.scale;

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
    
    //TODO cache the results of these for x,z coords, see if that speeds up cubic chunks support.
    public BicubicInterpolator       biomeInterpolator   = new BicubicInterpolator();
    public CachedBicubicInterpolator heightInterpolator  = new CachedBicubicInterpolator();
    public CachedBicubicInterpolator miscInterpolator    = new CachedBicubicInterpolator();

    public BiomeProviderFinite()
    {
        super();
    }

    public BiomeProviderFinite(World world)
    {
        super(world.getWorldInfo());
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
        if (x >= 0 && z >= 0 && (x + 16) / scale <= WorldGenerator.instance.dorfs.biomeMap.length
                && (z + 16) / scale <= WorldGenerator.instance.dorfs.biomeMap[0].length)
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
        x -= WorldGenerator.shift.getX();
        z -= WorldGenerator.shift.getZ();

        if (x >= 0 && z >= 0 && (x + 16) / scale <= WorldGenerator.instance.dorfs.biomeMap.length
                && (z + 16) / scale <= WorldGenerator.instance.dorfs.biomeMap[0].length)
        {

            x += WorldGenerator.shift.getX();
            z += WorldGenerator.shift.getZ();

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
                int biome = getBiomeFromMaps(x + i1 - WorldGenerator.shift.getX(),
                        z + k1 - WorldGenerator.shift.getZ());

                biomes[index] = Biome.getBiome(biome);
            }
        }
        return biomes;
    }

    private int getBiomeFromMaps(int x, int z)
    {
        DorfMap dorfs = WorldGenerator.instance.dorfs;

        int b1 = biomeInterpolator.interpolateBiome(dorfs.biomeMap, x, z, scale);
        Biome temp = Biome.getBiome(b1);
        if (dorfs.riverMap.length > 0)
        {
            int r1 = miscInterpolator.interpolateHeight(scale, x, z, dorfs.riverMap);
            if (r1 > 0)
            {
                temp = Biomes.RIVER;
            }
        }

        boolean hasHeightmap = dorfs.elevationMap.length > 0;
        boolean hasThermalMap = dorfs.temperatureMap.length > 0;

        int h1 = hasHeightmap ? heightInterpolator.interpolateHeight(scale, x, z, dorfs.elevationMap) : 64;
        int t1 = hasThermalMap ? miscInterpolator.interpolateHeight(scale, x, z, dorfs.temperatureMap) : 128;

        if (h1 > 60 && (temp == Biomes.DEEP_OCEAN || temp == Biomes.OCEAN))
        {
            temp = Biomes.BEACH;
            if (t1 < 80)
            {
                temp = Biomes.COLD_BEACH;
            }
        }
        else if (h1 > 45 && (temp == Biomes.DEEP_OCEAN || temp == Biomes.OCEAN))
        {
            temp = Biomes.OCEAN;
            if (t1 < 65)
            {
                temp = Biomes.FROZEN_OCEAN;
            }
        }
        else if (h1 <= 45 && (temp == Biomes.OCEAN))
        {
            temp = Biomes.DEEP_OCEAN;
        }

        return Biome.getIdForBiome(temp);
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
