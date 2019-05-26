package dorfgen.worldgen.vanilla;

import static net.minecraftforge.event.terraingen.InitMapGenEvent.EventType.MINESHAFT;
import static net.minecraftforge.event.terraingen.InitMapGenEvent.EventType.RAVINE;
import static net.minecraftforge.event.terraingen.PopulateChunkEvent.Populate.EventType.ANIMALS;
import static net.minecraftforge.event.terraingen.PopulateChunkEvent.Populate.EventType.DUNGEON;
import static net.minecraftforge.event.terraingen.PopulateChunkEvent.Populate.EventType.ICE;

import java.util.Random;

import dorfgen.WorldGenerator;
import dorfgen.conversion.DorfMap;
import dorfgen.conversion.SiteStructureGenerator;
import dorfgen.worldgen.common.BiomeProviderFinite;
import dorfgen.worldgen.common.CachedInterpolator;
import dorfgen.worldgen.common.GeneratorInfo;
import dorfgen.worldgen.common.IDorfgenProvider;
import dorfgen.worldgen.common.IPrimerWrapper;
import dorfgen.worldgen.common.MapGenSites;
import dorfgen.worldgen.common.RiverMaker;
import dorfgen.worldgen.common.RoadMaker;
import dorfgen.worldgen.common.SiteMaker;
import net.minecraft.block.Block;
import net.minecraft.block.BlockFalling;
import net.minecraft.block.material.Material;
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
import net.minecraftforge.common.BiomeDictionary;
import net.minecraftforge.common.BiomeDictionary.Type;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.event.terraingen.PopulateChunkEvent;
import net.minecraftforge.event.terraingen.TerrainGen;

public class ChunkGeneratorFinite extends ChunkGeneratorOverworld implements IDorfgenProvider
{
    /** RNG. */
    private Random                         rand;
    /** Reference to the World object. */
    private World                          worldObj;
    /** are map structures going to be generated (e.g. strongholds) */
    private final boolean                  mapFeaturesEnabled;
    // private MapGenBase caveGenerator = new MapGenUGRegions();
    // /** Holds Stronghold Generator */
    // private MapGenStronghold strongholdGenerator = new MapGenStronghold();
    /** Holds Village Generator */
    public MapGenSites                     villageGenerator;
    /** Holds Mineshaft Generator */
    private MapGenMineshaft                mineshaftGenerator    = new MapGenMineshaft();
    // private MapGenScatteredFeature scatteredFeatureGenerator = new
    // MapGenScatteredFeature();
    /** Holds ravine generator */
    private MapGenBase                     ravineGenerator       = new MapGenRavine();
    public final RiverMaker                riverMaker;
    public final RoadMaker                 roadMaker;
    public final SiteMaker                 constructor;
    public final SiteStructureGenerator    structuregen;
    public final DorfMap                   map;
    /** The biomes that are used to generate the chunk */
    private Biome[]                        biomesForGeneration;
    private int                            scale;
    final GeneratorInfo                    info;
    public CachedInterpolator              elevationInterpolator = new CachedInterpolator();
    public CachedInterpolator              waterInterpolator     = new CachedInterpolator();
    public CachedInterpolator              riverInterpolator     = new CachedInterpolator();
    public CachedInterpolator              biomeInterpolator     = new CachedInterpolator();

    public Class<? extends IPrimerWrapper> wrapperClass          = PrimerWrapper.class;
    public IPrimerWrapper                  lastWrapper           = null;

    {
        mineshaftGenerator = (MapGenMineshaft) TerrainGen.getModdedMapGen(mineshaftGenerator, MINESHAFT);
        ravineGenerator = TerrainGen.getModdedMapGen(ravineGenerator, RAVINE);
    }

    public ChunkGeneratorFinite(World world, long seed, boolean features, String generatorOptions)
    {
        super(world, seed, features, generatorOptions);
        this.worldObj = world;
        this.mapFeaturesEnabled = features;
        this.rand = new Random(seed);
        String json = world.getWorldInfo().getGeneratorOptions();
        info = GeneratorInfo.fromJson(json);
        this.map = info.create(true);
        this.structuregen = WorldGenerator.getStructureGen(info.region);
        this.riverMaker = new RiverMaker(map, structuregen);
        this.roadMaker = new RoadMaker(map, structuregen);
        this.constructor = new SiteMaker(map, structuregen);
        scale = info.scaleh;
        this.villageGenerator = new MapGenSites(map);
        villageGenerator.genSites(info.sites).genVillages(info.villages);
        riverMaker.setRespectsSites(info.sites).setScale(scale);
        roadMaker.setRespectsSites(info.sites).setScale(scale);
        riverMaker.riverInterpolator = riverInterpolator;
        riverMaker.waterInterpolator = waterInterpolator;
        riverMaker.elevationInterpolator = elevationInterpolator;
        roadMaker.riverInterpolator = riverInterpolator;
        roadMaker.waterInterpolator = waterInterpolator;
        roadMaker.elevationInterpolator = elevationInterpolator;
        constructor.bicubicInterpolator = elevationInterpolator;
        map.heightInterpolator = elevationInterpolator;
        map.biomeInterpolator = biomeInterpolator;
        constructor.setScale(scale);
        structuregen.setScale(scale);
        ((BiomeProviderFinite) world.getBiomeProvider()).scale = scale;
        world.setSeaLevel((int) world.provider.getHorizon());
    }

    public void preSetSeaLevel(int x, int z)
    {
        int maxWater = worldObj.getSeaLevel();
        for (int i = -32; i >= 32; i++)
            for (int j = -32; j <= 32; j++)
            {
                int imgX = map.shiftX(x * 16 + 8 + i);
                int imgZ = map.shiftZ(z * 16 + 8 + i);
                if (DorfMap.inBounds(imgX, imgZ, getDorfMap().riverMap))
                {
                    int r = getDorfMap().riverMap[imgX][imgZ] - 1;
                    int w = getDorfMap().waterMap[imgX][imgZ] - 1;
                    maxWater = Math.max(maxWater, Math.max(w, r));
                }
            }
        worldObj.setSeaLevel(maxWater);
    }

    public void postSetSeaLevel()
    {
        worldObj.setSeaLevel((int) worldObj.provider.getHorizon());
    }

    public void prepInterpolators(int chunkX, int chunkZ)
    {
        elevationInterpolator.initImage(map.elevationMap, chunkX, chunkZ, 32, scale);
        biomeInterpolator.initBiome(map.biomeMap, chunkX, chunkZ, 16, scale);
        waterInterpolator.initImage(map.waterMap, chunkX, chunkZ, 32, scale);
        riverInterpolator.initImage(map.riverMap, chunkX, chunkZ, 32, scale);
        this.biomesForGeneration = this.worldObj.getBiomeProvider().getBiomes(this.biomesForGeneration, chunkX * 16,
                chunkZ * 16, 16, 16);
        if (map.elevationMap.length == 0) map.finite = false;
    }

    public void fillPrimer(ChunkPrimer primer, int chunkX, int chunkZ, int minY, int maxY)
    {
        int imgX = map.shiftX(chunkX * 16);
        int imgZ = map.shiftZ(chunkZ * 16);
        boolean inImg = false;
        if (imgX >= 0 && imgZ >= 0 && (imgX + 16) / scale <= map.elevationMap.length
                && (imgZ + 16) / scale <= map.elevationMap[0].length)
        {
            populateBlocksFromImage(scale, chunkX, chunkZ, primer, minY, maxY);
            inImg = true;
            fillOceansAndLakes(worldObj, chunkX, chunkZ, biomesForGeneration, primer, false, minY, maxY);
            // Doesn't do anything with y coord.
            makeBeaches(scale, chunkX, chunkZ, primer, biomesForGeneration);
        }
        else
        {
            fillOceansAndLakes(worldObj, chunkX, chunkZ, biomesForGeneration, primer, true, minY, maxY);
        }
        if (inImg)
        {
            if (info.rivers)
                riverMaker.makeRiversForChunk(worldObj, chunkX, chunkZ, primer, biomesForGeneration, minY, maxY);
            if (info.sites) constructor.buildSites(worldObj, chunkX, chunkZ, primer, biomesForGeneration, minY, maxY);
            if (info.constructs)
                roadMaker.buildRoads(worldObj, chunkX, chunkZ, primer, biomesForGeneration, minY, maxY);
        }
    }

    /** Takes Chunk Coordinates */
    public void populateBlocksFromImage(int scale, int chunkX, int chunkZ, ChunkPrimer primer, int minY, int maxY)
    {
        int x1, z1;
        int x = map.shiftX(chunkX * 16);
        int z = map.shiftZ(chunkZ * 16);

        for (int i1 = 0; i1 < 16; i1++)
        {
            for (int k1 = 0; k1 < 16; k1++)
            {
                x1 = (x + i1) / scale;
                z1 = (z + k1) / scale;
                int h1 = 0;
                if (x1 >= map.elevationMap.length || z1 >= map.elevationMap[0].length)
                {
                }
                else h1 = elevationInterpolator.interpolate(map.elevationMap, x + i1, z + k1, scale);
                h1 = Math.max(h1, 10);

                double s = worldObj.provider.getHeight() / 256d;
                if (h1 > worldObj.getSeaLevel()) h1 = (int) (h1 * s);
                h1 = Math.min(maxY, h1);

                for (int j = minY; j < h1; j++)
                {
                    primer.setBlockState(i1, j, k1, Blocks.STONE.getDefaultState());
                }
            }
        }
    }

    public void fillOceansAndLakes(World world, int chunkX, int chunkZ, Biome[] biomes, ChunkPrimer primer, boolean oob,
            int minY, int maxY)
    {
        int x = map.shiftX(chunkX * 16);
        int z = map.shiftZ(chunkZ * 16);
        int b0 = (world.getSeaLevel()) - 1;
        for (int i = 0; i < 16; i++)
            for (int k = 0; k < 16; k++)
            {
                if (oob)
                {
                    int min = Math.max(0, minY);
                    int max = Math.min(maxY, b0);
                    for (int j = min; j <= max; j++)
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
                    biomes[index] = Biomes.DEEP_OCEAN;
                }
                else
                {
                    int x1 = (x + i) / scale;
                    int z1 = (z + k) / scale;
                    int h = 0;
                    if (x1 >= map.waterMap.length || z1 >= map.waterMap[0].length)
                    {
                        h = b0;
                    }
                    else h = waterInterpolator.interpolate(map.waterMap, (x + i), (z + k), scale) - 1;
                    h = Math.max(h, b0);
                    if (h > 0)
                    {
                        int min = Math.max(h, minY);
                        int max = Math.min(maxY, map.yMin);
                        for (int j = min; j > max; j--)
                        {
                            if (primer.getBlockState(i, j, k).getMaterial() != Material.AIR) break;
                            primer.setBlockState(i, j, k, Blocks.WATER.getDefaultState());
                        }
                    }
                }
            }
    }

    public ChunkPrimer fillPrimer(int chunkX, int chunkZ, int minY, int maxY)
    {
        if (lastWrapper != null && lastWrapper.getX() == chunkX && lastWrapper.getZ() == chunkZ)
            return lastWrapper.getPrimer();
        try
        {
            lastWrapper = wrapperClass.newInstance();
        }
        catch (Exception e)
        {
            throw new RuntimeException(e);
        }
        lastWrapper.setX(chunkX);
        lastWrapper.setZ(chunkZ);
        ChunkPrimer primer = lastWrapper.getPrimer();
        prepInterpolators(chunkX, chunkZ);
        fillPrimer(primer, chunkX, chunkZ, minY, maxY);
        this.replaceBiomeBlocks(chunkX, chunkZ, primer, this.biomesForGeneration);
        if (info.villages || info.sites)
        {
            preSetSeaLevel(chunkX, chunkZ);
            this.villageGenerator.generate(this.worldObj, chunkX, chunkZ, primer);
            postSetSeaLevel();
        }
        return primer;
    }

    public ChunkPrimer fillPrimer(int chunkX, int chunkZ)
    {
        return fillPrimer(chunkX, chunkZ, 0, worldObj.getHeight());
    }

    @Override
    /** Will return back a chunk, if it doesn't exist and its not a MP client it
     * will generates all the blocks for the specified chunk from the map seed
     * and chunk seed */
    public Chunk generateChunk(int chunkX, int chunkZ)
    {
        this.rand.setSeed((long) chunkX * 341873128712L + (long) chunkZ * 132897987541L);

        ChunkPrimer primer = fillPrimer(chunkX, chunkZ);

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
    public void recreateStructures(Chunk chunk, int x, int z)
    {
        if (info.villages || info.sites)
        {
            preSetSeaLevel(x, z);
            this.villageGenerator.generate(this.worldObj, x, z, null);
            postSetSeaLevel();
        }
    }

    @Override
    /** Populates chunk with ores etc etc */
    public void populate(int x, int z)
    {
        elevationInterpolator.initImage(map.elevationMap, x, z, 32, scale);
        waterInterpolator.initImage(map.waterMap, x, z, 32, scale);
        riverInterpolator.initImage(map.riverMap, x, z, 32, scale);
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

        if (info.sites || info.villages)
        {
            preSetSeaLevel(x, z);
            if (mapFeaturesEnabled)
                this.mineshaftGenerator.generateStructure(this.worldObj, this.rand, new ChunkPos(x, z));
            flag = this.villageGenerator.generateStructure(this.worldObj, this.rand, new ChunkPos(x, z));
            if (info.sites) structuregen.generate(x, z, worldObj, 0, 255);
            postSetSeaLevel();
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
        if (info.rivers)
        {
            riverMaker.postInitRivers(worldObj, x, z, 0, 255);
        }

        MinecraftForge.EVENT_BUS.post(new PopulateChunkEvent.Post(this, worldObj, rand, x, z, flag));

        BlockFalling.fallInstantly = false;
    }

    private void makeBeaches(int scale, int chunkX, int chunkZ, ChunkPrimer blocks, Biome[] biomes)
    {
        if (map.elevationMap.length == 0) return;
        int x1, z1;
        int x = map.shiftX(chunkX * 16);
        int z = map.shiftZ(chunkZ * 16);
        int seaLevel = worldObj.getSeaLevel();
        for (int i = 0; i < 16; i++)
        {
            for (int k = 0; k < 16; k++)
            {
                x1 = x + i;
                z1 = z + k;
                if (DorfMap.inBounds(x1 / scale, z1 / scale, map.elevationMap))
                {
                    int h = elevationInterpolator.interpolate(map.elevationMap, x1, z1, scale);
                    if (Math.abs(h - seaLevel) < 4)
                    {
                        int index = i + 16 * k;
                        Biome old = biomes[index];
                        if (BiomeDictionary.hasType(old, Type.OCEAN))
                            biomes[index] = map.biomeList.mutateBiome(Biomes.BEACH, x1, z1, map);
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

    @Override
    public RiverMaker getRiverMaker()
    {
        return riverMaker;
    }

    @Override
    public RoadMaker getRoadMaker()
    {
        return roadMaker;
    }

    @Override
    public SiteMaker getSiteMaker()
    {
        return constructor;
    }

    @Override
    public DorfMap getDorfMap()
    {
        return map;
    }
}