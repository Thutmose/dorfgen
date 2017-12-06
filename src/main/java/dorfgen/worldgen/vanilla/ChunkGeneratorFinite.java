package dorfgen.worldgen.vanilla;

import static dorfgen.WorldGenerator.scale;
import static net.minecraftforge.event.terraingen.InitMapGenEvent.EventType.MINESHAFT;
import static net.minecraftforge.event.terraingen.InitMapGenEvent.EventType.RAVINE;
import static net.minecraftforge.event.terraingen.PopulateChunkEvent.Populate.EventType.ANIMALS;
import static net.minecraftforge.event.terraingen.PopulateChunkEvent.Populate.EventType.DUNGEON;
import static net.minecraftforge.event.terraingen.PopulateChunkEvent.Populate.EventType.ICE;

import java.util.Random;

import dorfgen.WorldGenerator;
import dorfgen.conversion.Interpolator.BicubicInterpolator;
import dorfgen.conversion.Interpolator.CachedBicubicInterpolator;
import dorfgen.worldgen.common.MapGenSites;
import dorfgen.worldgen.common.RiverMaker;
import dorfgen.worldgen.common.WorldConstructionMaker;
import net.minecraft.block.Block;
import net.minecraft.block.BlockFalling;
import net.minecraft.block.state.IBlockState;
import net.minecraft.init.Biomes;
import net.minecraft.init.Blocks;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.ChunkPos;
import net.minecraft.world.World;
import net.minecraft.world.biome.Biome;
import net.minecraft.world.chunk.Chunk;
import net.minecraft.world.chunk.ChunkPrimer;
import net.minecraft.world.gen.ChunkGeneratorOverworld;
import net.minecraft.world.gen.MapGenBase;
import net.minecraft.world.gen.MapGenRavine;
import net.minecraft.world.gen.structure.MapGenMineshaft;
import net.minecraft.world.gen.structure.MapGenVillage;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.event.terraingen.PopulateChunkEvent;
import net.minecraftforge.event.terraingen.TerrainGen;

public class ChunkGeneratorFinite extends ChunkGeneratorOverworld
{
    /** RNG. */
    private Random                   rand;
    /** Reference to the World object. */
    private World                    worldObj;
    /** are map structures going to be generated (e.g. strongholds) */
    private final boolean            mapFeaturesEnabled;
    // private MapGenBase caveGenerator = new MapGenUGRegions();
    // /** Holds Stronghold Generator */
    // private MapGenStronghold strongholdGenerator = new MapGenStronghold();
    /** Holds Village Generator */
    private MapGenVillage            villageGenerator    = new MapGenSites();
    /** Holds Mineshaft Generator */
    private MapGenMineshaft          mineshaftGenerator  = new MapGenMineshaft();
    // private MapGenScatteredFeature scatteredFeatureGenerator = new
    // MapGenScatteredFeature();
    /** Holds ravine generator */
    private MapGenBase               ravineGenerator     = new MapGenRavine();
    private RiverMaker               riverMaker          = new RiverMaker();
    private WorldConstructionMaker   constructor         = new WorldConstructionMaker();
    /** The biomes that are used to generate the chunk */
    private Biome[]                  biomesForGeneration;
    public BicubicInterpolator       bicubicInterpolator = new BicubicInterpolator();
    public CachedBicubicInterpolator cachedInterpolator  = new CachedBicubicInterpolator();

    {
        mineshaftGenerator = (MapGenMineshaft) TerrainGen.getModdedMapGen(mineshaftGenerator, MINESHAFT);
        ravineGenerator = TerrainGen.getModdedMapGen(ravineGenerator, RAVINE);
    }

    public ChunkGeneratorFinite(World world, long seed, boolean features, String generatorOptions)
    {
        super(world, seed, features, generatorOptions);
        this.worldObj = world; // remove
        this.mapFeaturesEnabled = features;
        this.rand = new Random(seed);
    }

    /** Takes Chunk Coordinates */
    public void populateBlocksFromImage(int scale, int chunkX, int chunkZ, ChunkPrimer primer)
    {
        int x1, z1, w = 0;
        int x = chunkX * 16;
        int z = chunkZ * 16;
        boolean water = false;

        for (int i1 = 0; i1 < 16; i1++)
        {
            for (int k1 = 0; k1 < 16; k1++)
            {
                x1 = (x + i1) / scale;
                z1 = (z + k1) / scale;
                int h1 = 0;
                if (x1 >= WorldGenerator.instance.dorfs.waterMap.length
                        || z1 >= WorldGenerator.instance.dorfs.waterMap[0].length)
                {
                    w = -1;
                    water = true;
                }
                else
                {
                    w = WorldGenerator.instance.dorfs.waterMap[x1][z1];
                    h1 = bicubicInterpolator.interpolate(WorldGenerator.instance.dorfs.elevationMap, x + i1, z + k1,
                            scale);
                    water = w > 0 || (WorldGenerator.instance.dorfs.countLarger(0,
                            WorldGenerator.instance.dorfs.waterMap, x1, z1, 1) > 0);
                }
                h1 = Math.max(h1, 10);

                double s = worldObj.provider.getHeight() / 256d;
                if (h1 > worldObj.provider.getHorizon()) h1 = (int) (h1 * s);

                for (int j = 0; j < h1; j++)
                {
                    primer.setBlockState(i1, j, k1, Blocks.STONE.getDefaultState());

                }
                if (w <= 0) w = (int) (worldObj.provider.getHorizon());
                if (water) for (int j = h1; j < w; j++)
                {
                    primer.setBlockState(i1, j, k1, Blocks.WATER.getDefaultState());
                }
            }
        }
    }

    public void fillOceans(int x, int z, ChunkPrimer primer)
    {
        byte b0 = (byte) (worldObj.provider.getHorizon());
        for (int i = 0; i < 16; i++)
            for (int k = 0; k < 16; k++)
            {
                for (int j = 0; j < b0; j++)
                {
                    if (j < 10)
                    {
                        primer.setBlockState(i, j, k, Blocks.STONE.getDefaultState());
                    }
                    else
                    {
                        primer.setBlockState(i, j, k, Blocks.WATER.getDefaultState());
                    }
                }
                int index = i + k * 16;
                biomesForGeneration[index] = Biomes.DEEP_OCEAN;
            }

    }

    @Override
    /** Will return back a chunk, if it doesn't exist and its not a MP client it
     * will generates all the blocks for the specified chunk from the map seed
     * and chunk seed */
    public Chunk generateChunk(int chunkX, int chunkZ)
    {
        this.rand.setSeed((long) chunkX * 341873128712L + (long) chunkZ * 132897987541L);

        ChunkPrimer primer = new ChunkPrimer();

        // Block[] ablock = new Block[256 * worldObj.getHeight()];
        // byte[] abyte = new byte[256 * worldObj.getHeight()];

        this.biomesForGeneration = this.worldObj.getBiomeProvider().getBiomes(this.biomesForGeneration, chunkX * 16,
                chunkZ * 16, 16, 16);
        if (WorldGenerator.instance.dorfs.elevationMap.length == 0) WorldGenerator.finite = false;

        int imgX = chunkX * 16 - WorldGenerator.shift.getX();
        int imgZ = chunkZ * 16 - WorldGenerator.shift.getZ();

        if (imgX >= 0 && imgZ >= 0 && (imgX + 16) / scale <= WorldGenerator.instance.dorfs.elevationMap.length
                && (imgZ + 16) / scale <= WorldGenerator.instance.dorfs.elevationMap[0].length)
        {
            int x = imgX;
            int z = imgZ;
            populateBlocksFromImage(scale, chunkX, chunkZ, primer);
            riverMaker.makeRiversForChunk(worldObj, chunkX, chunkZ, primer, biomesForGeneration, 0, 255);
            constructor.buildSites(worldObj, chunkX, chunkZ, primer, biomesForGeneration, 0, 255);
            constructor.buildRoads(worldObj, chunkX, chunkZ, primer, biomesForGeneration, 0, 255);
            makeBeaches(scale, x / scale, z / scale, primer);
        }
        else if (WorldGenerator.finite)
        {
            this.fillOceans(chunkX, chunkZ, primer);
        }
        else
        {
            return super.generateChunk(chunkX, chunkZ);
        }

        this.replaceBiomeBlocks(chunkX, chunkZ, primer, this.biomesForGeneration);

        if (this.mapFeaturesEnabled)
        {
            this.villageGenerator.generate(this.worldObj, chunkX, chunkZ, primer);
        }

        Chunk chunk;

        chunk = new Chunk(worldObj, primer, chunkX, chunkZ);
        byte[] abyte1 = chunk.getBiomeArray();

        for (int k = 0; k < abyte1.length; ++k)
        {
            abyte1[k] = (byte) Biome.getIdForBiome(this.biomesForGeneration[k]);
        }

        chunk.generateSkylightMap();
        return chunk;
    }

    @Override
    public void recreateStructures(Chunk chunk, int p_82695_1_, int p_82695_2_)
    {
        if (true) // TODO find out why this keeps being called, it keeps
                  // spawning lairs.
            return;
        // if (this.mapFeaturesEnabled)
        // {
        // // this.mineshaftGenerator.func_151539_a(this, this.worldObj,
        // // p_82695_1_, p_82695_2_, (Block[])null);
        // // this.villageGenerator.func_151539_a(this, this.worldObj,
        // // p_82695_1_, p_82695_2_, (Block[])null);
        // // this.scatteredFeatureGenerator.func_151539_a(this, this.worldObj,
        // // p_82695_1_, p_82695_2_, (Block[])null);
        // }
    }

    @Override
    /** Populates chunk with ores etc etc */
    public void populate(int x, int z)
    {
        BlockFalling.fallInstantly = true;
        int k = x * 16;
        int l = z * 16;
        BlockPos blockpos = new BlockPos(k, 0, l);
        Biome biomegenbase = this.worldObj.getBiome(new BlockPos(k + 16, 0, l + 16));
        this.rand.setSeed(this.worldObj.getSeed());
        long i1 = this.rand.nextLong() / 2L * 2L + 1L;
        long j1 = this.rand.nextLong() / 2L * 2L + 1L;
        this.rand.setSeed((long) x * i1 + (long) z * j1 ^ this.worldObj.getSeed());
        boolean flag = false;

        MinecraftForge.EVENT_BUS.post(new PopulateChunkEvent.Pre(this, worldObj, rand, x, z, flag));

        if (this.mapFeaturesEnabled)
        {
            // TODO more map features if needed
            this.mineshaftGenerator.generateStructure(this.worldObj, this.rand, new ChunkPos(x, z));
            flag = this.villageGenerator.generateStructure(this.worldObj, this.rand, new ChunkPos(x, z));
            WorldGenerator.instance.structureGen.generate(x, z, worldObj);
        }

        int k1;
        int l1;
        // }//TODO ponds and lakes in appropriate biomes

        boolean doGen = TerrainGen.populate(this, worldObj, rand, x, z, flag, DUNGEON);
        for (k1 = 0; doGen && k1 < 8; ++k1)
        {
            // TODO Dungeons
        }

        biomegenbase.decorate(this.worldObj, this.rand, new BlockPos(k, 0, l));
        if (TerrainGen.populate(this, worldObj, rand, x, z, flag, ANIMALS))
        {
            // TODO animals
        }

        blockpos = blockpos.add(8, 0, 8);
        doGen = TerrainGen.populate(this, worldObj, rand, x, z, flag, ICE);
        for (k1 = 0; doGen && k1 < 16; ++k1)
        {
            for (l1 = 0; l1 < 16; ++l1)
            {
                BlockPos blockpos1 = this.worldObj.getPrecipitationHeight(blockpos.add(k1, 0, l1));
                BlockPos blockpos2 = blockpos1.down();

                if (this.worldObj.canBlockFreezeWater(blockpos2)) // .func_175675_v(blockpos2))
                {
                    this.worldObj.setBlockState(blockpos2, Blocks.ICE.getDefaultState(), 2);
                }

                if (this.worldObj.canSnowAt(blockpos1, true))
                {
                    this.worldObj.setBlockState(blockpos1, Blocks.SNOW_LAYER.getDefaultState(), 2);
                }
            }
        }

        MinecraftForge.EVENT_BUS.post(new PopulateChunkEvent.Post(this, worldObj, rand, x, z, flag));

        BlockFalling.fallInstantly = false;
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
    private void makeBeaches(int scale, int x, int z, ChunkPrimer blocks)
    {
        int x1, z1, h1;
        for (int i1 = 0; i1 < 16; i1++)
        {
            for (int k1 = 0; k1 < 16; k1++)
            {
                x1 = x + i1 / scale;
                z1 = z + k1 / scale;
                if (x1 >= WorldGenerator.instance.dorfs.elevationMap.length
                        || z1 >= WorldGenerator.instance.dorfs.elevationMap[0].length)
                {
                    h1 = 10;
                }
                else h1 = WorldGenerator.instance.dorfs.elevationMap[x1][z1];
                Biome b1 = biomesForGeneration[i1 + 16 * k1];
                boolean beach = false;

                if (b1 == Biomes.OCEAN || b1 == Biomes.DEEP_OCEAN || b1 == Biomes.BEACH)
                {
                    for (int j = 100; j > 10; j--)
                    {
                        if (!isIndexEmpty(blocks, i1, j, k1) && getBlock(blocks, i1, j, k1) != Blocks.WATER)
                        {
                            h1 = j;
                            beach = true;
                            break;
                        }
                    }
                }
                if (beach)
                {
                    for (int j = h1 + 1; j < worldObj.provider.getHorizon(); j++)
                    {
                        blocks.setBlockState(i1, j, k1, Blocks.WATER.getDefaultState());
                    }
                }
            }
        }
    }

    public static Block getBlock(ChunkPrimer primer, int x, int y, int z)
    {
        IBlockState state = primer.getBlockState(x, y, z);
        return state != null ? state.getBlock() : Blocks.AIR;
    }

    public static boolean isIndexEmpty(ChunkPrimer primer, int x, int y, int z)
    {
        IBlockState state = primer.getBlockState(x, y, z);
        return state == null || state.getBlock() == Blocks.AIR;
    }

    /** Converts the instance data to a readable string. */
    public String makeString()
    {
        return "FiniteLevelSource";
    }
}