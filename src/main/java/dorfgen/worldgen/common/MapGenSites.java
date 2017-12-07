package dorfgen.worldgen.common;

import java.util.HashSet;
import java.util.Iterator;
import java.util.Random;

import dorfgen.conversion.DorfMap;
import dorfgen.conversion.DorfMap.Site;
import dorfgen.conversion.DorfMap.SiteType;
import net.minecraft.block.BlockLadder;
import net.minecraft.init.Blocks;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.tileentity.TileEntityChest;
import net.minecraft.tileentity.TileEntityMobSpawner;
import net.minecraft.util.EnumFacing;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;
import net.minecraft.world.gen.structure.ComponentScatteredFeaturePieces;
import net.minecraft.world.gen.structure.MapGenStronghold;
import net.minecraft.world.gen.structure.MapGenVillage;
import net.minecraft.world.gen.structure.StructureStart;
import net.minecraft.world.gen.structure.StructureStrongholdPieces;
import net.minecraft.world.storage.loot.LootTableList;
import net.minecraftforge.common.DungeonHooks;

public class MapGenSites extends MapGenVillage
{
    HashSet<Integer> set       = new HashSet<Integer>();
    HashSet<Integer> made      = new HashSet<Integer>();
    Site             siteToGen = null;
    private int      scale     = 0;
    final DorfMap    map;

    public MapGenSites(DorfMap map)
    {
        super();
        this.map = map;
    }

    public void setScale(int scale)
    {
        this.scale = scale;
    }

    @Override
    protected boolean canSpawnStructureAtCoords(int x, int z)
    {
        x *= 16;
        z *= 16;
        x -= map.shift.getX();
        z -= map.shift.getZ();

        HashSet<Site> sites = map.getSiteForCoords(x, z);

        if (sites == null) return false;

        for (Site site : sites)
        {
            if (!set.contains(site.id) && shouldSiteSpawn(x, z, site))
            {
                set.add(site.id);
                siteToGen = site;
                System.out.println("Chosen to gen " + site);
                return true;
            }
        }

        return false;
    }

    public boolean shouldSiteSpawn(int x, int z, Site site)
    {
        if (site.type == SiteType.LAIR)
        {
            int embarkX = (x / scale) * scale;
            int embarkZ = (z / scale) * scale;

            if (embarkX / scale != site.x || embarkZ / scale != site.z) return false;
            for (int i = 0; i < 16; i++)
            {
                for (int j = 0; j < 16; j++)
                {
                    int relX = (x + i) % scale + 8;
                    int relZ = (z + j) % scale + 8;
                    boolean middle = relX / 16 == scale / 32 && relZ / 16 == scale / 32;
                    if (middle)
                    {
                        System.out.println(site);
                        return true;
                    }
                }
            }
            return false;

        }
        return false;
    }

    @Override
    protected StructureStart getStructureStart(int x, int z)
    {
        Site site = siteToGen;
        siteToGen = null;
        if (site == null) { return super.getStructureStart(x, z); }
        System.out.println("Generating Site " + site);
        made.add(site.id);
        if (site.type == SiteType.FORTRESS)
        {
            MapGenStronghold.Start start;

            for (start = new MapGenStronghold.Start(this.world, this.rand, x, z); start.getComponents().isEmpty()
                    || ((StructureStrongholdPieces.Stairs2) start.getComponents()
                            .get(0)).strongholdPortalRoom == null; start = new MapGenStronghold.Start(this.world,
                                    this.rand, x, z))
            {
                ;
            }
            return start;
        }
        else if (site.type == SiteType.DARKFORTRESS)
        {

        }
        else if (site.type == SiteType.DARKPITS)
        {

        }
        else if (site.type == SiteType.HIPPYHUTS)
        {
            return new Start(map, world, rand, x, z, 0);
        }
        else if (site.type == SiteType.SHRINE)
        {
            return new Start(map, world, rand, x, z, 2);
        }
        else if (site.type == SiteType.LAIR)
        {
            return new Start(map, world, rand, x, z, 3);
        }
        else if (site.type == SiteType.CAVE) { return new Start(map, world, rand, x, z, 1); }
        return super.getStructureStart(x, z);
    }

    public static class Start extends StructureStart
    {
        public Start()
        {
        }

        public Start(DorfMap map, World world_, Random rand, int x, int z, int type)
        {
            super(x, z);
            if (type == 0)
            {
                for (int k = 0; k < 15; k++)
                {
                    int x1 = 40 - rand.nextInt(40);
                    int z1 = 40 - rand.nextInt(40);

                    for (int i = 0; i < rand.nextInt(20); i++)
                    {

                        ComponentScatteredFeaturePieces.SwampHut swamphut = new ComponentScatteredFeaturePieces.SwampHut(
                                rand, x * 16 + x1, z * 16 + z1);

                        this.components.add(swamphut);
                    }
                }
            }
            else if (type == 1)
            {
                ComponentScatteredFeaturePieces.DesertPyramid desertpyramid = new ComponentScatteredFeaturePieces.DesertPyramid(
                        rand, x * 16, z * 16);
                this.components.add(desertpyramid);
            }
            else if (type == 2)
            {
                ComponentScatteredFeaturePieces.JunglePyramid junglepyramid = new ComponentScatteredFeaturePieces.JunglePyramid(
                        rand, x * 16, z * 16);
                this.components.add(junglepyramid);
            }
            else if (type == 3)
            {
                System.out.println("Making a lair");

                int h = map.biomeInterpolator.interpolate(map.elevationMap, x * 16, z * 16, map.scale);

                BlockPos pos = new BlockPos(x * 16, h - 5, z * 16);
                BlockPos blockpos1;
                int i = rand.nextInt(2) + 2;
                int j = -i - 1;
                int k = i + 1;
                int l = rand.nextInt(2) + 2;
                int i1 = -l - 1;
                int j1 = l + 1;
                int l1;
                int i2;
                int j2;
                for (l1 = j; l1 <= k; ++l1)
                {
                    for (i2 = 3; i2 >= -1; --i2)
                    {
                        for (j2 = i1; j2 <= j1; ++j2)
                        {
                            blockpos1 = pos.add(l1, i2, j2);
                            world_.setBlockState(blockpos1, Blocks.STONE.getDefaultState(), 2);
                        }
                    }
                }
                world_.setBlockState(pos.add(k - 1, 4, 0), Blocks.TRAPDOOR.getDefaultState(), 2);

                // TODO re-copy dungeon code from 1.8 again for this
                for (l1 = j; l1 <= k; ++l1)
                {
                    for (i2 = 3; i2 >= -1; --i2)
                    {
                        for (j2 = i1; j2 <= j1; ++j2)
                        {
                            blockpos1 = pos.add(l1, i2, j2);

                            if (l1 != j && i2 != -1 && j2 != i1 && l1 != k && i2 != 4 && j2 != j1)
                            {
                                if (world_.getBlockState(blockpos1).getBlock() != Blocks.CHEST)
                                {
                                    world_.setBlockToAir(blockpos1);
                                }
                            }
                            else if (blockpos1.getY() >= map.yMin
                                    && !world_.getBlockState(blockpos1.down()).getMaterial().isSolid())
                            {
                                world_.setBlockToAir(blockpos1);
                            }
                            else if (world_.getBlockState(blockpos1).getMaterial().isSolid()
                                    && world_.getBlockState(blockpos1).getBlock() != Blocks.CHEST)
                            {
                                if (i2 == -1 && rand.nextInt(4) != 0)
                                {
                                    world_.setBlockState(blockpos1, Blocks.MOSSY_COBBLESTONE.getDefaultState(), 2);
                                }
                                else
                                {
                                    world_.setBlockState(blockpos1, Blocks.COBBLESTONE.getDefaultState(), 2);
                                }
                            }
                        }
                    }
                }

                l1 = 0;

                while (l1 < 2)
                {
                    i2 = 0;

                    while (true)
                    {
                        if (i2 < 3)
                        {
                            label197:
                            {
                                j2 = pos.getX() + rand.nextInt(i * 2 + 1) - i;
                                int l2 = pos.getY();
                                int i3 = pos.getZ() + rand.nextInt(l * 2 + 1) - l;
                                BlockPos blockpos2 = new BlockPos(j2, l2, i3);

                                if (world_.isAirBlock(blockpos2))
                                {
                                    int k2 = 0;
                                    Iterator<?> iterator = EnumFacing.Plane.HORIZONTAL.iterator();

                                    while (iterator.hasNext())
                                    {
                                        EnumFacing enumfacing = (EnumFacing) iterator.next();

                                        if (world_.getBlockState(blockpos2.offset(enumfacing)).getMaterial().isSolid())
                                        {
                                            ++k2;
                                        }
                                    }

                                    if (k2 == 1)
                                    {
                                        world_.setBlockState(blockpos2, Blocks.CHEST.correctFacing(world_, blockpos2,
                                                Blocks.CHEST.getDefaultState()), 2);
                                        TileEntity tileentity1 = world_.getTileEntity(blockpos2);

                                        if (tileentity1 instanceof TileEntityChest)
                                        {
                                            ((TileEntityChest) tileentity1)
                                                    .setLootTable(LootTableList.CHESTS_SIMPLE_DUNGEON, rand.nextLong());
                                        }

                                        break label197;
                                    }
                                }

                                ++i2;
                                continue;
                            }
                        }

                        ++l1;
                        break;
                    }
                }
                // Build Ladder
                for (int h1 = 0; h1 < 4; h1++)
                {
                    world_.setBlockState(pos.add(k - 1, h1, 0),
                            Blocks.LADDER.getDefaultState().withProperty(BlockLadder.FACING, EnumFacing.WEST), 2);
                }

                world_.setBlockState(pos, Blocks.MOB_SPAWNER.getDefaultState(), 2);
                TileEntity tileentity = world_.getTileEntity(pos);

                if (tileentity instanceof TileEntityMobSpawner)
                {
                    ((TileEntityMobSpawner) tileentity).getSpawnerBaseLogic()
                            .setEntityId(DungeonHooks.getRandomDungeonMob(rand));
                }
                else
                {
                    System.err.println("Failed to fetch mob spawner entity at (" + pos.getX() + ", " + pos.getY() + ", "
                            + pos.getZ() + ")");
                }
            }

            this.updateBoundingBox();
        }
    }
}
