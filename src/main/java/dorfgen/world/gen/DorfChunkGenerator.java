package dorfgen.world.gen;

import java.util.List;

import com.google.common.collect.Lists;

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
import net.minecraft.world.World;
import net.minecraft.world.biome.Biome;
import net.minecraft.world.biome.BiomeManager;
import net.minecraft.world.biome.provider.BiomeProvider;
import net.minecraft.world.chunk.ChunkPrimer;
import net.minecraft.world.chunk.ChunkSection;
import net.minecraft.world.chunk.IChunk;
import net.minecraft.world.gen.ChunkGenerator;
import net.minecraft.world.gen.GenerationStage;
import net.minecraft.world.gen.GenerationStage.Carving;
import net.minecraft.world.gen.Heightmap.Type;
import net.minecraft.world.gen.INoiseGenerator;
import net.minecraft.world.gen.PerlinNoiseGenerator;
import net.minecraft.world.gen.WorldGenRegion;
import net.minecraft.world.gen.feature.ConfiguredFeature;
import net.minecraft.world.gen.feature.DecoratedFeatureConfig;
import net.minecraft.world.gen.feature.Feature;
import net.minecraft.world.gen.feature.IFeatureConfig;
import net.minecraft.world.gen.feature.structure.Structure;
import net.minecraft.world.gen.feature.template.TemplateManager;

public class DorfChunkGenerator extends ChunkGenerator<DorfSettings>
{
    public static List<Structure<?>> VILLAGETYPE = Lists.newArrayList();

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
        if (DorfChunkGenerator.VILLAGETYPE.isEmpty()) DorfChunkGenerator.VILLAGETYPE.addAll(Feature.ILLAGER_STRUCTURES);

        this.info = generationSettingsIn.getInfo();
        this.map = this.info.create(true);
        this.map.biomeList.noiseGen = new PerlinNoiseGenerator(new SharedSeedRandom(this.world.getSeed()), 3, 0);
        this.biomes = (DorfBiomeProvider) biomeProviderIn;
        this.surfaceDepthNoise = this.map.biomeList.noiseGen;
        if (this.map.getScale() < SiteStructureGenerator.SITETOBLOCK) this.info.sites = false;

        this.biomes.map = this.map;
        this.roadMaker = new RoadMaker(this.map, this.map.structureGen);
        this.riverMaker = new RiverMaker(this.map, this.map.structureGen);
        this.siteMaker = new SiteMaker(this.map, this.map.structureGen);

        this.riverMaker.setRespectsSites(this.info.sites).setScale(this.map.getScale());
        this.roadMaker.setRespectsSites(this.info.sites).setScale(this.map.getScale());
        this.siteMaker.setScale(this.map.getScale());

        this.siteMaker.bicubicInterpolator = this.elevationInterpolator;
        this.riverMaker.riverInterpolator = this.riverInterpolator;
        this.riverMaker.waterInterpolator = this.waterInterpolator;
        this.riverMaker.elevationInterpolator = this.elevationInterpolator;
        this.roadMaker.riverInterpolator = this.riverInterpolator;
        this.roadMaker.waterInterpolator = this.waterInterpolator;
        this.roadMaker.elevationInterpolator = this.elevationInterpolator;

        this.map.heightInterpolator = this.elevationInterpolator;
        this.map.biomeInterpolator = this.biomeInterpolator;

    }

    @Override
    public void generateBiomes(final IChunk chunkIn)
    {
        this.biomes.forGen = true;
        super.generateBiomes(chunkIn);
        this.biomes.forGen = false;
    }

    public void prepInterpolators(final ChunkPos chunkPos)
    {
        // The cached ones worked nicely in single threaded, however here, that
        // is an issue, as the caches are not threadsafe!
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
        final int scale = this.map.getScale();

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
        if (this.map.getScale() >= SiteStructureGenerator.SITETOBLOCK) this.siteMaker.buildSites(chunk, pos,
                this.map.yMin, this.world.getHeight());
    }

    @Override
    public int getGroundHeight()
    {
        return this.world.getSeaLevel() + 1;
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
                int featureIndex = 0;
                for (final ConfiguredFeature<?, ?> feature : biome.getFeatures(stage))
                {
                    final IFeatureConfig conf = feature.config;
                    if (conf instanceof DecoratedFeatureConfig)
                    {
                        final DecoratedFeatureConfig config = (DecoratedFeatureConfig) conf;
                        if (config.feature.feature == Feature.LAKE)
                        {
                            ++featureIndex;
                            continue;
                        }
                    }
                    if (feature.feature == Feature.LAKE)
                    {
                        ++featureIndex;
                        continue;
                    }
                    random.setFeatureSeed(i1, featureIndex, stage.ordinal());
                    try
                    {

                        feature.place(region, this, random, blockpos);
                    }
                    catch (final Exception exception)
                    {
                        final CrashReport crashreport = CrashReport.makeCrashReport(exception, "Feature placement");
                        crashreport.makeCategory("Feature").addDetail("Id", feature.feature.getRegistryName())
                                .addDetail("Description", () ->
                                {
                                    return feature.feature.toString();
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
        if (this.map.getScale() >= SiteStructureGenerator.SITETOBLOCK) this.map.structureGen.generate(chunk, region,
                pos);
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
        super.func_225550_a_(p_225550_1_, p_225550_2_, p_225550_3_);
    }

    @Override
    public void generateStructureStarts(final IWorld worldIn, final IChunk chunkIn)
    {
        super.generateStructureStarts(worldIn, chunkIn);
    }

    @Override
    public void generateStructures(final BiomeManager p_227058_1_, final IChunk chunkIn,
            final ChunkGenerator<?> p_227058_3_, final TemplateManager p_227058_4_)
    {
        super.generateStructures(p_227058_1_, chunkIn, p_227058_3_, p_227058_4_);
    }

    @Override
    public BlockPos findNearestStructure(final World worldIn, final String name, final BlockPos pos, final int radius,
            final boolean p_211403_5_)
    {
        // TODO Auto-generated method stub
        return super.findNearestStructure(worldIn, name, pos, radius, p_211403_5_);
    }

    @Override
    public <C extends IFeatureConfig> C getStructureConfig(final Biome biomeIn, final Structure<C> structureIn)
    {
        // TODO Auto-generated method stub
        return super.getStructureConfig(biomeIn, structureIn);
    }

    @Override
    public boolean hasStructure(final Biome biomeIn, final Structure<? extends IFeatureConfig> structureIn)
    {
        // TODO Auto-generated method stub
        return super.hasStructure(biomeIn, structureIn);
    }

    @Override
    protected Biome getBiome(final BiomeManager biomeManagerIn, final BlockPos posIn)
    {
        // TODO Auto-generated method stub
        return super.getBiome(biomeManagerIn, posIn);
    }

    // private void initVillageBases(final IWorld worldIn, final IChunk chunkIn)
    // {
    // final ObjectList<AbstractVillagePiece> pieces = new
    // ObjectArrayList<>(10);
    // final ObjectList<JigsawJunction> junctions = new ObjectArrayList<>(32);
    // final ChunkPos chunkpos = chunkIn.getPos();
    // final int cx = chunkpos.x;
    // final int cz = chunkpos.z;
    // final int x0 = cx << 4;
    // final int z0 = cz << 4;
    //
    // for (final Structure<?> structure : DorfChunkGenerator.VILLAGETYPE)
    // {
    // final String s = structure.getStructureName();
    // final LongIterator longiterator =
    // chunkIn.getStructureReferences(s).iterator();
    //
    // while (longiterator.hasNext())
    // {
    // final long posHash = longiterator.nextLong();
    // final ChunkPos chunkpos1 = new ChunkPos(posHash);
    // final IChunk ichunk = worldIn.getChunk(chunkpos1.x, chunkpos1.z);
    // final StructureStart structurestart = ichunk.getStructureStart(s);
    // if (structurestart != null && structurestart.isValid())
    // for (final StructurePiece structurepiece :
    // structurestart.getComponents())
    // if (structurepiece.func_214810_a(chunkpos, 12) && structurepiece
    // instanceof AbstractVillagePiece)
    // {
    // final AbstractVillagePiece abstractvillagepiece = (AbstractVillagePiece)
    // structurepiece;
    // final JigsawPattern.PlacementBehaviour jigsawpattern$placementbehaviour =
    // abstractvillagepiece
    // .getJigsawPiece().getPlacementBehaviour();
    // if (jigsawpattern$placementbehaviour ==
    // JigsawPattern.PlacementBehaviour.RIGID) pieces.add(
    // abstractvillagepiece);
    //
    // for (final JigsawJunction jigsawjunction :
    // abstractvillagepiece.getJunctions())
    // {
    // final int startX = jigsawjunction.getSourceX();
    // final int startZ = jigsawjunction.getSourceZ();
    // if (startX > x0 - 12 && startZ > z0 - 12 && startX < x0 + 15 + 12 &&
    // startZ < z0 + 15 + 12)
    // junctions.add(jigsawjunction);
    // }
    // }
    // }
    // }
    // }

    @Override
    public void makeBase(final IWorld worldIn, final IChunk chunkIn)
    {
        final ChunkPrimer chunkprimer = (ChunkPrimer) chunkIn;

        final int x0 = chunkIn.getPos().getXStart();
        final int z0 = chunkIn.getPos().getZStart();

        this.prepInterpolators(chunkIn.getPos());

        final int imgX = this.map.shiftX(x0);
        final int imgZ = this.map.shiftX(z0);
        final int scale = this.map.getScale();

        // Set up bases for villages, etc
        // if (this.info.villages) this.initVillageBases(worldIn, chunkIn);

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
        final int scale = this.map.getScale();

        if (x < 0) x = 0;
        if (z < 0) z = 0;
        if (x / scale >= this.map.biomeMap.length) x = this.map.biomeMap.length * scale - 1;
        if (z / scale >= this.map.biomeMap[0].length) z = this.map.biomeMap[0].length * scale - 1;

        return this.elevationInterpolator.interpolate(this.map.elevationMap, x, z, scale);
    }

}
