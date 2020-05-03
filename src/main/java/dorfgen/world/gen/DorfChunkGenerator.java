package dorfgen.world.gen;

import dorfgen.conversion.DorfMap;
import dorfgen.conversion.SiteStructureGenerator;
import dorfgen.util.Interpolator.BicubicInterpolator;
import dorfgen.world.feature.RiverMaker;
import dorfgen.world.feature.RoadMaker;
import dorfgen.world.feature.SiteMaker;
import net.minecraft.block.BlockState;
import net.minecraft.crash.CrashReport;
import net.minecraft.crash.ReportedException;
import net.minecraft.util.SharedSeedRandom;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.BlockPos.Mutable;
import net.minecraft.util.math.ChunkPos;
import net.minecraft.world.IWorld;
import net.minecraft.world.biome.Biome;
import net.minecraft.world.biome.BiomeManager;
import net.minecraft.world.biome.provider.BiomeProvider;
import net.minecraft.world.chunk.ChunkPrimer;
import net.minecraft.world.chunk.ChunkSection;
import net.minecraft.world.chunk.IChunk;
import net.minecraft.world.gen.ChunkGenerator;
import net.minecraft.world.gen.GenerationStage;
import net.minecraft.world.gen.GenerationStage.Carving;
import net.minecraft.world.gen.GenerationStage.Decoration;
import net.minecraft.world.gen.Heightmap.Type;
import net.minecraft.world.gen.INoiseGenerator;
import net.minecraft.world.gen.PerlinNoiseGenerator;
import net.minecraft.world.gen.WorldGenRegion;
import net.minecraft.world.gen.feature.ConfiguredFeature;
import net.minecraft.world.gen.feature.Feature;

public class DorfChunkGenerator extends ChunkGenerator<DorfSettings>
{
    public BicubicInterpolator elevationInterpolator = new BicubicInterpolator();
    public BicubicInterpolator waterInterpolator     = new BicubicInterpolator();
    public BicubicInterpolator riverInterpolator     = new BicubicInterpolator();
    public BicubicInterpolator biomeInterpolator     = new BicubicInterpolator();

    private final INoiseGenerator surfaceDepthNoise;

    private final RiverMaker riverMaker;
    private final RoadMaker  roadMaker;
    private final SiteMaker  siteMaker;
    private final DorfMap    map;

    public final GeneratorInfo info;

    private final DorfBiomeProvider biomes;

    public DorfChunkGenerator(final IWorld worldIn, final BiomeProvider biomeProviderIn,
            final DorfSettings generationSettingsIn)
    {
        super(worldIn, biomeProviderIn, generationSettingsIn);
        this.biomes = (DorfBiomeProvider) biomeProviderIn;
        this.info = generationSettingsIn.getInfo();
        this.map = this.info.create(true);
        this.map.scale = 8;

        if (this.map.scale < SiteStructureGenerator.SITETOBLOCK) this.info.sites = false;

        this.biomes.map = this.map;

        this.roadMaker = new RoadMaker(this.map, this.map.structureGen);
        this.riverMaker = new RiverMaker(this.map, this.map.structureGen);
        this.siteMaker = new SiteMaker(this.map, this.map.structureGen);

        this.riverMaker.setRespectsSites(this.info.sites).setScale(this.map.scale);
        this.roadMaker.setRespectsSites(this.info.sites).setScale(this.map.scale);
        this.siteMaker.setScale(this.map.scale);

        this.siteMaker.bicubicInterpolator = this.elevationInterpolator;
        this.riverMaker.riverInterpolator = this.riverInterpolator;
        this.riverMaker.waterInterpolator = this.waterInterpolator;
        this.riverMaker.elevationInterpolator = this.elevationInterpolator;
        this.roadMaker.riverInterpolator = this.riverInterpolator;
        this.roadMaker.waterInterpolator = this.waterInterpolator;
        this.roadMaker.elevationInterpolator = this.elevationInterpolator;

        this.map.heightInterpolator = this.elevationInterpolator;
        this.map.biomeInterpolator = this.biomeInterpolator;

        this.surfaceDepthNoise = new PerlinNoiseGenerator(new SharedSeedRandom(this.seed), 3, 0);
    }

    @Override
    public void generateBiomes(final IChunk chunkIn)
    {
        this.biomes.forGen = true;
        // this.biomeInterpolator.initBiome(this.map.biomeMap,
        // chunkIn.getPos().x, chunkIn.getPos().z, 16, this.map.scale);
        super.generateBiomes(chunkIn);
        this.biomes.forGen = false;
    }

    public void prepInterpolators(final ChunkPos chunkPos)
    {
        // The cached ones worked nicely in single threaded, however here, that
        // is an issue, as the caches are not threadsafe!

        // final int x0 = chunkPos.x;
        // final int z0 = chunkPos.z;
        // this.elevationInterpolator.initImage(this.map.elevationMap, x0, z0,
        // 32, this.map.scale);
        // this.biomeInterpolator.initBiome(this.map.biomeMap, x0, z0, 16,
        // this.map.scale);
        // this.waterInterpolator.initImage(this.map.waterMap, x0, z0, 32,
        // this.map.scale);
        // this.riverInterpolator.initImage(this.map.riverMap, x0, z0, 32,
        // this.map.scale);
    }

    @Override
    public void func_225551_a_(final WorldGenRegion region, final IChunk chunk)
    {
        // This should apply biome replacements, etc
        final int x0 = chunk.getPos().getXStart();
        final int z0 = chunk.getPos().getZStart();

        this.prepInterpolators(chunk.getPos());

        final SharedSeedRandom sharedseedrandom = new SharedSeedRandom();
        sharedseedrandom.setBaseChunkSeed(chunk.getPos().x, chunk.getPos().z);

        final BlockPos.Mutable pos = new BlockPos.Mutable();

        // Block coordinates
        int x, y, z;

        // map coordinates;
        int x1, z1;

        final int imgX = this.map.shiftX(x0);
        final int imgZ = this.map.shiftX(z0);
        final int scale = this.map.scale;

        for (int dx = 0; dx < 16; dx++)
            for (int dz = 0; dz < 16; dz++)
            {
                x = x0 + dx;
                z = z0 + dz;

                x1 = imgX + dx;
                z1 = imgZ + dz;

                if (x1 < 0) x1 = 0;
                if (z1 < 0) z1 = 0;
                if (x1 / scale >= this.map.biomeMap.length) x1 = this.map.biomeMap.length * scale - 1;
                if (z1 / scale >= this.map.biomeMap[0].length) z1 = this.map.biomeMap[0].length * scale - 1;

                y = this.elevationInterpolator.interpolate(this.map.elevationMap, x1, z1, scale);
                final Biome biome = this.biomes.getNoiseBiome(x, y, z);
                final double surfaceNoise = this.surfaceDepthNoise.noiseAt(x * 0.0625D, z * 0.0625D, 0.0625D, y
                        * 0.0625D) * 15.0D;
                biome.buildSurface(sharedseedrandom, chunk, x, z, y, surfaceNoise, this.getSettings().getDefaultBlock(),
                        this.getSettings().getDefaultFluid(), this.getSeaLevel(), this.world.getSeed());
            }
        this.roadMaker.buildRoads(chunk, pos, this.map.yMin, this.world.getHeight());
        // This only gets done if the sites actually fit
        if (this.map.scale >= SiteStructureGenerator.SITETOBLOCK) this.siteMaker.buildSites(chunk, pos, this.map.yMin,
                this.world.getHeight());
    }

    @Override
    public int getGroundHeight()
    {
        // TODO adjust this?
        return 64;
    }

    @Override
    public void decorate(final WorldGenRegion region)
    {
        final int i = region.getMainChunkX();
        final int j = region.getMainChunkZ();
        final int k = i * 16;
        final int l = j * 16;
        final BlockPos blockpos = new BlockPos(k, 0, l);
        final Biome biome = this.getBiome(region.getBiomeManager(), blockpos.add(8, 8, 8));
        final SharedSeedRandom random = new SharedSeedRandom();
        final long i1 = random.setDecorationSeed(region.getSeed(), k, l);
        for (final GenerationStage.Decoration stage : GenerationStage.Decoration.values())
            try
            {
                if (stage == Decoration.LOCAL_MODIFICATIONS) continue;

                int featureIndex = 0;
                for (final ConfiguredFeature<?, ?> configuredfeature : biome.getFeatures(stage))
                {

                    if (configuredfeature.feature == Feature.LAKE)
                    {
                        ++featureIndex;
                        continue;
                    }
                    random.setFeatureSeed(i1, featureIndex, stage.ordinal());
                    try
                    {
                        configuredfeature.place(region, this, random, blockpos);
                    }
                    catch (final Exception exception)
                    {
                        final CrashReport crashreport = CrashReport.makeCrashReport(exception, "Feature placement");
                        crashreport.makeCategory("Feature").addDetail("Id", configuredfeature.feature.getRegistryName())
                                .addDetail("Description", () ->
                                {
                                    return configuredfeature.feature.toString();
                                });
                        throw new ReportedException(crashreport);
                    }
                    ++featureIndex;
                }
            }
            catch (final Exception exception)
            {
                final CrashReport crashreport = CrashReport.makeCrashReport(exception, "Biome decoration");
                crashreport.makeCategory("Generation").addDetail("CenterX", i).addDetail("CenterZ", j).addDetail("Step",
                        stage).addDetail("Seed", i1).addDetail("Biome", biome.getRegistryName());
                throw new ReportedException(crashreport);
            }

        final IChunk chunk = region.getChunk(i, j);
        final Mutable pos = new Mutable();
        // This only gets done if the sites actually fit
        if (this.map.scale >= SiteStructureGenerator.SITETOBLOCK) this.map.structureGen.generate(chunk, region, pos);
    }

    @Override
    public int func_222531_c(final int p_222531_1_, final int p_222531_2_, final Type heightmapType)
    {
        return super.func_222531_c(p_222531_1_, p_222531_2_, heightmapType);
    }

    @Override
    public int func_222532_b(final int p_222532_1_, final int p_222532_2_, final Type heightmapType)
    {
        return super.func_222532_b(p_222532_1_, p_222532_2_, heightmapType);
    }

    @Override
    public void func_225550_a_(final BiomeManager p_225550_1_, final IChunk p_225550_2_, final Carving p_225550_3_)
    {
        // super.func_225550_a_(p_225550_1_, p_225550_2_, p_225550_3_);
    }

    @Override
    public void makeBase(final IWorld worldIn, final IChunk chunkIn)
    {
        final ChunkPrimer chunkprimer = (ChunkPrimer) chunkIn;

        final int x0 = chunkIn.getPos().getXStart();
        final int z0 = chunkIn.getPos().getZStart();

        this.prepInterpolators(chunkIn.getPos());

        final int imgX = this.map.shiftX(x0);
        final int imgZ = this.map.shiftX(z0);
        final int scale = this.map.scale;

        // Block coordinates
        int x, y, z;

        int ySeg = 0;
        final int ySegprev = 0;
        ChunkSection chunksection = chunkprimer.getSection(ySeg);
        chunksection.lock();
        for (ySeg = 0; ySeg < 16; ySeg++)
        {
            if (ySeg != ySegprev)
            {
                chunksection.unlock();
                chunksection = chunkprimer.getSection(ySeg);
                chunksection.lock();
            }
            for (int dx = 0; dx < 16; dx++)
                for (int dz = 0; dz < 16; dz++)
                {
                    x = imgX + dx;
                    z = imgZ + dz;

                    if (x < 0) x = 0;
                    if (z < 0) z = 0;
                    if (x / scale >= this.map.biomeMap.length) x = this.map.biomeMap.length * scale - 1;
                    if (z / scale >= this.map.biomeMap[0].length) z = this.map.biomeMap[0].length * scale - 1;

                    final int yMax = this.elevationInterpolator.interpolate(this.map.elevationMap, x, z, scale) - 1;
                    for (int dy = 0; dy < 16; dy++)
                    {
                        y = dy + (ySeg << 4);
                        final BlockState state = y > yMax ? y < this.getSeaLevel() ? this.getSettings()
                                .getDefaultFluid() : null : this.getSettings().getDefaultBlock();
                        if (state == null) break;
                        chunksection.setBlockState(dx, dy, dz, state);
                    }
                }
        }
        chunksection.unlock();

        this.riverMaker.makeRiversForChunk(chunkIn, worldIn, new Mutable(), this.map.yMin, this.world.getHeight());
    }

    @Override
    /**
     * x and z are in world coordinates
     */
    public int func_222529_a(int x, int z, final Type heightmapType)
    {
        x = this.map.shiftX(x);
        z = this.map.shiftX(z);
        final int scale = this.map.scale;

        if (x < 0) x = 0;
        if (z < 0) z = 0;
        if (x / scale >= this.map.biomeMap.length) x = this.map.biomeMap.length * scale - 1;
        if (z / scale >= this.map.biomeMap[0].length) z = this.map.biomeMap[0].length * scale - 1;

        return this.elevationInterpolator.interpolate(this.map.elevationMap, x, z, scale);
    }

}
