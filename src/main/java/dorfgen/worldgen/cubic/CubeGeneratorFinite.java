package dorfgen.worldgen.cubic;

import java.util.List;
import java.util.Random;

import javax.annotation.Nonnull;

import dorfgen.conversion.DorfMap;
import dorfgen.worldgen.common.IDorfgenProvider;
import dorfgen.worldgen.common.RiverMaker;
import dorfgen.worldgen.common.RoadMaker;
import dorfgen.worldgen.common.SiteMaker;
import dorfgen.worldgen.vanilla.ChunkGeneratorFinite;
import io.github.opencubicchunks.cubicchunks.api.util.Box;
import io.github.opencubicchunks.cubicchunks.api.util.Coords;
import io.github.opencubicchunks.cubicchunks.api.world.ICube;
import io.github.opencubicchunks.cubicchunks.api.world.ICubicWorld;
import io.github.opencubicchunks.cubicchunks.api.worldgen.CubeGeneratorsRegistry;
import io.github.opencubicchunks.cubicchunks.api.worldgen.CubePrimer;
import io.github.opencubicchunks.cubicchunks.api.worldgen.ICubeGenerator;
import net.minecraft.block.state.IBlockState;
import net.minecraft.entity.EnumCreatureType;
import net.minecraft.init.Blocks;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;
import net.minecraft.world.biome.Biome;
import net.minecraft.world.biome.Biome.SpawnListEntry;
import net.minecraft.world.chunk.Chunk;
import net.minecraft.world.chunk.ChunkPrimer;
import net.minecraft.world.gen.IChunkGenerator;
import net.minecraftforge.fml.common.registry.GameRegistry;

public class CubeGeneratorFinite implements IDorfgenProvider, ICubeGenerator
{
    final ChunkGeneratorFinite wrapped;
    @Nonnull
    private IChunkGenerator    vanilla;
    @Nonnull
    private World              world;
    private int                worldHeightCubes;
    /** Last chunk that was generated from the vanilla world gen */
    @Nonnull
    private ChunkPrimer        lastChunk;
    /** We generate all the chunks in the vanilla range at once. This variable
     * prevents infinite recursion */
    private boolean            optimizationHack;
    private Biome[]            biomes;
    /** Detected block for filling cubes below the world */
    @Nonnull
    private IBlockState        extensionBlockBottom = Blocks.STONE.getDefaultState();
    /** Detected block for filling cubes above the world */
    @Nonnull
    private IBlockState        extensionBlockTop    = Blocks.AIR.getDefaultState();

    /** Create a new VanillaCompatibilityGenerator
     *
     * @param vanilla
     *            The vanilla generator to mirror
     * @param world
     *            The world in which cubes are being generated */
    public CubeGeneratorFinite(IChunkGenerator vanilla, World world)
    {
        wrapped = (ChunkGeneratorFinite) vanilla;
        wrapped.wrapperClass = PrimerWrapper.class;
        this.vanilla = vanilla;
        this.world = world;
        this.worldHeightCubes = 256 * 8;
    }

    @Override
    public void generateColumn(Chunk column)
    {
        this.biomes = this.world.getBiomeProvider().getBiomes(this.biomes, Coords.cubeToMinBlock(column.x),
                Coords.cubeToMinBlock(column.z), ICube.SIZE, ICube.SIZE);
        byte[] abyte = column.getBiomeArray();
        for (int i = 0; i < abyte.length; ++i)
        {
            abyte[i] = (byte) Biome.getIdForBiome(this.biomes[i]);
        }
    }

    @Override
    public void recreateStructures(Chunk column)
    {
        vanilla.recreateStructures(column, column.x, column.z);
    }

    private Random getCubeSpecificRandom(int cubeX, int cubeY, int cubeZ)
    {
        Random rand = new Random(world.getSeed());
        rand.setSeed(rand.nextInt() ^ cubeX);
        rand.setSeed(rand.nextInt() ^ cubeZ);
        rand.setSeed(rand.nextInt() ^ cubeY);
        return rand;
    }

    @Override
    public CubePrimer generateCube(int cubeX, int cubeY, int cubeZ)
    {
        CubePrimer primer = new CubePrimer();

        if (cubeY < 0)
        {
            Random rand = new Random(world.getSeed());
            rand.setSeed(rand.nextInt() ^ cubeX);
            rand.setSeed(rand.nextInt() ^ cubeZ);
            // Fill with bottom block
            for (int x = 0; x < ICube.SIZE; x++)
            {
                for (int y = 0; y < ICube.SIZE; y++)
                {
                    for (int z = 0; z < ICube.SIZE; z++)
                    {
                        IBlockState state = extensionBlockBottom;
                        primer.setBlockState(x, y, z, state);
                    }
                }
            }
        }
        else if (cubeY >= worldHeightCubes)
        {
            // Fill with top block
            for (int x = 0; x < ICube.SIZE; x++)
            {
                for (int y = 0; y < ICube.SIZE; y++)
                {
                    for (int z = 0; z < ICube.SIZE; z++)
                    {
                        primer.setBlockState(x, y, z, extensionBlockTop);
                    }
                }
            }
        }
        else
        {

            if (!optimizationHack)
            {
                optimizationHack = true;
                // Recusrive generation
                for (int y = worldHeightCubes - 1; y >= 0; y--)
                {
                    if (y == cubeY)
                    {
                        continue;
                    }
                    ((ICubicWorld) world).getCubeFromCubeCoords(cubeX, y, cubeZ);
                }
                optimizationHack = false;
            }
            // Copy from vanilla, replacing bedrock as appropriate
            ChunkPrimer wrap = wrapped.fillPrimer(cubeX, cubeZ, cubeY * 16, cubeY * 16 + ICube.SIZE - 1);
            if (wrap != null)
            {
                for (int x = 0; x < ICube.SIZE; x++)
                {
                    for (int y = 0; y < ICube.SIZE; y++)
                    {
                        int y1 = y + cubeY * 16;
                        for (int z = 0; z < ICube.SIZE; z++)
                        {
                            IBlockState state = wrap.getBlockState(x, y1, z);
                            if (state == Blocks.BEDROCK.getDefaultState())
                            {
                                if (y < ICube.SIZE / 2)
                                {
                                    primer.setBlockState(x, y, z, extensionBlockBottom);
                                }
                                else
                                {
                                    primer.setBlockState(x, y, z, extensionBlockTop);
                                }
                            }
                            else
                            {
                                primer.setBlockState(x, y, z, state);
                            }
                        }
                    }
                }
            }
        }

        return primer;
    }

    @Override
    public void populate(ICube cube)
    {
        Random rand = getCubeSpecificRandom(cube.getX(), cube.getY(), cube.getZ());
        CubeGeneratorsRegistry.populateVanillaCubic(world, rand, cube);
        if (cube.getY() < 0 || cube.getY() >= worldHeightCubes) { return; }
        // Cubes outside this range are only filled with their respective block
        // No population takes place
        if (cube.getY() >= 0 && cube.getY() < worldHeightCubes)
        {
            try
            {
                vanilla.populate(cube.getX(), cube.getZ());
            }
            catch (IllegalArgumentException ex)
            {
                StackTraceElement[] stack = ex.getStackTrace();
                if (stack == null || stack.length < 1 || !stack[0].getClassName().equals(Random.class.getName())
                        || !stack[0].getMethodName().equals("nextInt"))
                {
                    throw ex;
                }
                else
                {

                }
            }
            GameRegistry.generateWorld(cube.getX(), cube.getZ(), world, vanilla, world.getChunkProvider());
        }
    }

    @Override
    public Box getFullPopulationRequirements(ICube cube)
    {
        if (cube.getY() >= 0 && cube.getY() < worldHeightCubes) { return new Box(-1, -cube.getY(), -1, 0,
                worldHeightCubes - cube.getY() - 1, 0); }
        return NO_REQUIREMENT;
    }

    @Override
    public Box getPopulationPregenerationRequirements(ICube cube)
    {
        if (cube.getY() >= 0 && cube.getY() < worldHeightCubes) { return new Box(0, -cube.getY(), 0, 1,
                worldHeightCubes - cube.getY() - 1, 1); }
        return NO_REQUIREMENT;
    }

    @Override
    public void recreateStructures(ICube cube)
    {
    }

    @Override
    public List<SpawnListEntry> getPossibleCreatures(EnumCreatureType creatureType, BlockPos pos)
    {
        return vanilla.getPossibleCreatures(creatureType, pos);
    }

    @Override
    public BlockPos getClosestStructure(String name, BlockPos pos, boolean findUnexplored)
    {
        return vanilla.getNearestStructurePos((World) world, name, pos, findUnexplored);
    }

    @Override
    public RiverMaker getRiverMaker()
    {
        return wrapped.getRiverMaker();
    }

    @Override
    public RoadMaker getRoadMaker()
    {
        return wrapped.getRoadMaker();
    }

    @Override
    public SiteMaker getSiteMaker()
    {
        return wrapped.getSiteMaker();
    }

    @Override
    public DorfMap getDorfMap()
    {
        return wrapped.getDorfMap();
    }

}
