package dorfgen.worldgen.cubic;

import static dorfgen.WorldGenerator.scale;

import java.util.Iterator;
import java.util.Map;
import java.util.Random;

import com.google.common.collect.Maps;

import cubicchunks.util.Box;
import cubicchunks.world.ICubicWorld;
import cubicchunks.world.cube.Cube;
import cubicchunks.worldgen.generator.BasicCubeGenerator;
import cubicchunks.worldgen.generator.CubePrimer;
import cubicchunks.worldgen.generator.ICubePrimer;
import cubicchunks.worldgen.generator.custom.CustomGeneratorSettings;
import cubicchunks.worldgen.generator.custom.biome.replacer.IBiomeBlockReplacer;
import dorfgen.WorldGenerator;
import dorfgen.conversion.ISigmoid;
import dorfgen.conversion.Interpolator.BicubicInterpolator;
import dorfgen.conversion.Interpolator.CachedBicubicInterpolator;
import dorfgen.worldgen.common.RiverMaker;
import dorfgen.worldgen.common.WorldConstructionMaker;
import net.minecraft.block.Block;
import net.minecraft.block.state.IBlockState;
import net.minecraft.init.Blocks;
import net.minecraft.util.ResourceLocation;
import net.minecraft.world.World;
import net.minecraft.world.biome.Biome;

public class CubeGeneratorFinite extends BasicCubeGenerator
{

    public BicubicInterpolator                         bicubicInterpolator = new BicubicInterpolator();
    public CachedBicubicInterpolator                   cachedInterpolator  = new CachedBicubicInterpolator();
    private RiverMaker                                 riverMaker          = new RiverMaker();
    private WorldConstructionMaker                     constructor         = new WorldConstructionMaker();

    private int                                        lastX               = Integer.MIN_VALUE,
            lastZ = Integer.MIN_VALUE;
    private Random                                     rand;
    private Biome[]                                    biomesForGeneration;

    private Map<ResourceLocation, IBiomeBlockReplacer> replacerMap         = Maps.newHashMap();

    private final CustomGeneratorSettings              conf;

    public CubeGeneratorFinite(ICubicWorld world)
    {
        super(world);
        this.rand = new Random();

        String json = world.getWorldInfo().getGeneratorOptions();
        this.conf = CustomGeneratorSettings.fromJson(json);
        WorldGenerator.scale = 51;
        WorldGenerator.instance.dorfs.setElevationSigmoid(new ISigmoid()
        {
            @Override
            public int elevationSigmoid(int preHeight)
            {
                return (preHeight) * 8;
            }
        });
        initGenerator(world.getSeed());
    }

    private void initGenerator(long seed)
    {
        Iterator<Biome> iter = Biome.REGISTRY.iterator();
        while (iter.hasNext())
        {
            Biome current = iter.next();
            final IBlockState topBlock = current.topBlock;
            final IBlockState fillerBlock = current.fillerBlock;
            final int depth = 3;
            replacerMap.put(current.getRegistryName(), new IBiomeBlockReplacer()
            {
                @Override
                public IBlockState getReplacedBlock(IBlockState previousBlock, int x, int y, int z, double dx,
                        double dy, double dz, double density)
                {
                    if (previousBlock.getMaterial().isLiquid()) return previousBlock;
                    if (x >= 0 && z >= 0)
                    {
                        int x1 = x / scale;
                        int z1 = z / scale;
                        int h1 = -1;
                        if (x1 >= WorldGenerator.instance.dorfs.waterMap.length
                                || z1 >= WorldGenerator.instance.dorfs.waterMap[0].length)
                        {

                        }
                        else
                        {
                            h1 = bicubicInterpolator.interpolate(WorldGenerator.instance.dorfs.elevationMap, x, z,
                                    scale);
                            h1 -= y;
                            // Cube at this point is above ground.
                            if (h1 > depth) return previousBlock;
                            // Cube at this point is below ground
                            if (h1 < 0) return previousBlock;
                            // This is surface of ground.
                            if (h1 == 0) return topBlock;
                            // Otherwise fill.
                            return fillerBlock;
                        }

                    }
                    return previousBlock;
                }
            });
        }
    }

    /** Takes Chunk Coordinates
     * 
     * @param cubeZ */
    public void populateBlocksFromImage(int scale, int cubeX, int cubeY, int cubeZ, CubePrimer primer)
    {
        int x1, z1;
        int x = cubeX * 16;
        int yMin = cubeY * 16;
        int z = cubeZ * 16;
        for (int i1 = 0; i1 < 16; i1++)
        {
            for (int k1 = 0; k1 < 16; k1++)
            {
                x1 = (x + i1) / scale;
                z1 = (z + k1) / scale;
                int h1 = -1;
                if (x1 >= WorldGenerator.instance.dorfs.waterMap.length
                        || z1 >= WorldGenerator.instance.dorfs.waterMap[0].length)
                {

                }
                else
                {
                    h1 = bicubicInterpolator.interpolate(WorldGenerator.instance.dorfs.elevationMap, x + i1, z + k1,
                            scale);
                }
                h1 -= yMin;
                // Cube at this point is above ground.
                if (h1 < 0) continue;

                // Cube at this point is below ground
                if (h1 > 15) h1 = 15;
                for (int j = 0; j <= h1; j++)
                {
                    primer.setBlockState(i1, j, k1, Blocks.STONE.getDefaultState());
                }
            }
        }
    }

    public void fillOceans(int x, int y, int z, CubePrimer primer)
    {
        int b0 = (int) (world.getProvider().getHorizon());
        int yMin = y * 16;
        b0 -= yMin;
        int x1, z1, w = 0;
        int rx = x * 16;
        int rz = z * 16;

        for (int i = 0; i < 16; i++)
            for (int k = 0; k < 16; k++)
            {
                x1 = (rx + i) / scale;
                z1 = (rz + k) / scale;
                if (rx + i < 0 || rz + k < 0 || x1 >= WorldGenerator.instance.dorfs.waterMap.length
                        || z1 >= WorldGenerator.instance.dorfs.waterMap[0].length)
                {

                }
                else
                {
                    // w =
                    // bicubicInterpolator.interpolate(WorldGenerator.instance.dorfs.elevationMap,
                    // rx + i, rz + k,
                    // scale);
                    // if (w > 0)
                    // {
                    // w =
                    // WorldGenerator.instance.dorfs.sigmoid.elevationSigmoid(w)
                    // - yMin;
                    // if (w > b0) b0 = w;
                    // }
                }
                b0 = Math.min(b0, 16);
                for (int j = 0; j < b0; j++)
                {
                    if (primer.getBlockState(i, j, k) == ICubePrimer.DEFAULT_STATE)
                        primer.setBlockState(i, j, k, Blocks.WATER.getDefaultState());
                }
            }

    }

    @Override
    public ICubePrimer generateCube(int cubeX, int cubeY, int cubeZ)
    {
        this.rand.setSeed((long) cubeX * 341873128712L + (long) cubeZ * 132897987541L);
        CubePrimer primer = new CubePrimer();
        if (lastX != cubeX || lastZ != cubeZ) this.biomesForGeneration = this.world.getBiomeProvider()
                .getBiomes(this.biomesForGeneration, cubeX * 16, cubeZ * 16, 16, 16);
        lastX = cubeX;
        lastZ = cubeZ;
        if (WorldGenerator.instance.dorfs.elevationMap.length == 0) WorldGenerator.finite = false;

        int imgX = cubeX * 16 - WorldGenerator.shift.getX();
        int imgZ = cubeZ * 16 - WorldGenerator.shift.getZ();
        if (imgX >= 0 && imgZ >= 0 && (imgX + 16) / scale <= WorldGenerator.instance.dorfs.elevationMap.length
                && (imgZ + 16) / scale <= WorldGenerator.instance.dorfs.elevationMap[0].length)
        {
            int x = imgX;
            int z = imgZ;
            int yMin = cubeY * 16;
            int yMax = cubeY * 16 + 15;
            PrimerWrapper wrapper = new PrimerWrapper(primer);
            populateBlocksFromImage(scale, cubeX, cubeY, cubeZ, primer);
//            riverMaker.makeRiversForChunk((World) world, cubeX, cubeZ, wrapper, biomesForGeneration, yMin, yMax);
            constructor.buildSites((World) world, cubeX, cubeZ, wrapper, biomesForGeneration, yMin, yMax);
//            constructor.buildRoads((World) world, cubeX, cubeZ, wrapper, biomesForGeneration, yMin, yMax);
            makeBeaches(scale, x / scale, cubeY, z / scale, primer);
        }
        this.replaceBiomeBlocks(world, cubeX, cubeY, cubeZ, primer, biomesForGeneration);
        fillOceans(cubeX, cubeY, cubeZ, primer);
        return primer;
    }

    @Override
    public void populate(Cube cube)
    {
        // TODO site structure generator for cubes
    }

    /** Retrieve the blockstate appropriate for the specified builder entry
     *
     * @return The block state */
    private IBlockState getBlock(int x, int y, int z, ICubePrimer cubePrimer, Biome[] biomesIn)
    {
        IBlockState block = cubePrimer.getBlockState(x & 15, y & 15, z & 15);
        Biome biome = biomesIn[(x & 15) + 16 * (z & 15)];
        return replacerMap.get(biome.getRegistryName()).getReplacedBlock(block, x, y, z, 0, 1, 0, 1);
    }

    public void replaceBiomeBlocks(ICubicWorld world, int cubeX, int cubeY, int cubeZ, ICubePrimer cubePrimer,
            Biome[] biomesIn)
    {
        for (int i = 0; i < 16; i++)
            for (int k = 0; k < 16; k++)
                for (int j = 0; j < 16; j++)
                {
                    int absX = cubeX * 16 + i, absY = cubeY * 16 + j, absZ = cubeZ * 16 + k;
                    cubePrimer.setBlockState(i, j, k, getBlock(absX, absY, absZ, cubePrimer, biomesIn));
                }
    }

    @Override
    public Box getPopulationRequirement(Cube cube)
    {
        return RECOMMENDED_POPULATOR_REQUIREMENT;
    }

    /** Takes Blocks Coordinates
     * 
     * @param scale
     *            - number of blocks per pixel
     * @param x
     *            - x coordinate of the pixel being used
     * @param z
     *            - y coordinate of the pixel being used
     * @param blocks */
    private void makeBeaches(int scale, int x, int y, int z, CubePrimer blocks)
    {
        // int x1, z1, h1;
        // for (int i1 = 0; i1 < 16; i1++)
        // {
        // for (int k1 = 0; k1 < 16; k1++)
        // {
        // x1 = x + i1 / scale;
        // z1 = z + k1 / scale;
        // if (x1 >= WorldGenerator.instance.dorfs.elevationMap.length
        // || z1 >= WorldGenerator.instance.dorfs.elevationMap[0].length)
        // {
        // h1 = 10;
        // }
        // else h1 = WorldGenerator.instance.dorfs.elevationMap[x1][z1];
        // Biome b1 = biomesForGeneration[i1 + 16 * k1];
        // boolean beach = false;
        //
        // if (b1 == Biomes.OCEAN || b1 == Biomes.DEEP_OCEAN || b1 ==
        // Biomes.BEACH)
        // {
        // for (int j = 100; j > 10; j--)
        // {
        // if (!isIndexEmpty(blocks, i1, j, k1) && getBlock(blocks, i1, j, k1)
        // != Blocks.WATER)
        // {
        // h1 = j;
        // beach = true;
        // break;
        // }
        // }
        // }
        // if (beach)
        // {
        // for (int j = h1 + 1; j < world.getProvider().getHorizon(); j++)
        // {
        // blocks.setBlockState(i1, j, k1, Blocks.WATER.getDefaultState());
        // }
        // }
        // }
        // }
    }

    public static Block getBlock(CubePrimer primer, int x, int y, int z)
    {
        IBlockState state = primer.getBlockState(x, y, z);
        return state != null ? state.getBlock() : Blocks.AIR;
    }

    public static boolean isIndexEmpty(CubePrimer primer, int x, int y, int z)
    {
        IBlockState state = primer.getBlockState(x, y, z);
        return state == null || state.getBlock() == Blocks.AIR;
    }

}
