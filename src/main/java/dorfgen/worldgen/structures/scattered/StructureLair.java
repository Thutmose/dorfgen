package dorfgen.worldgen.structures.scattered;

import java.util.Iterator;
import java.util.Random;

import dorfgen.WorldGenerator;
import dorfgen.conversion.DorfMap;
import dorfgen.conversion.DorfMap.Site;
import net.minecraft.block.BlockLadder;
import net.minecraft.init.Blocks;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.tileentity.TileEntityChest;
import net.minecraft.tileentity.TileEntityMobSpawner;
import net.minecraft.util.EnumFacing;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;
import net.minecraft.world.gen.structure.StructureBoundingBox;
import net.minecraft.world.gen.structure.StructureComponent;
import net.minecraft.world.gen.structure.template.TemplateManager;
import net.minecraft.world.storage.loot.LootTableList;
import net.minecraftforge.common.DungeonHooks;

public class StructureLair extends StructureComponent
{
    private DorfMap map;

    public StructureLair()
    {
    }

    public StructureLair(DorfMap map, Site lair)
    {
        this.map = map;
        int[] mid = lair.getSiteMid();
        int x = mid[0];
        int z = mid[1];
        this.boundingBox = new StructureBoundingBox(x - 32, z - 32, x + 32, z + 32);
    }

    @Override
    protected void writeStructureToNBT(NBTTagCompound tagCompound)
    {
        tagCompound.setString("M", map.name);
    }

    @Override
    protected void readStructureFromNBT(NBTTagCompound tagCompound, TemplateManager p_143011_2_)
    {
        this.map = WorldGenerator.getDorfMap(tagCompound.getString("M"));
    }

    @Override
    public boolean addComponentParts(World worldIn, Random randomIn, StructureBoundingBox boxIn)
    {
        Random rand = new Random(worldIn.getSeed() + this.getBoundingBox().minX
                + this.getBoundingBox().minZ * this.getBoundingBox().maxY);
        buildLair(worldIn, map, rand, this.getBoundingBox().minX + getBoundingBox().getXSize() / 2,
                this.getBoundingBox().minZ + getBoundingBox().getZSize() / 2, boxIn);
        return true;
    }

    private void buildLair(World world_, DorfMap map, Random rand, int x, int z, StructureBoundingBox boxIn)
    {
        int h = map.heightInterpolator.interpolate(map.elevationMap, map.shiftX(x), map.shiftZ(z), map.scale);
        BlockPos pos = new BlockPos(x, h - 15, z);
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
                    if (boxIn.isVecInside(blockpos1))
                        world_.setBlockState(blockpos1, Blocks.STONE.getDefaultState(), 2);
                }
            }
        }

        for (l1 = j; l1 <= k; ++l1)
        {
            for (i2 = 3; i2 >= -1; --i2)
            {
                for (j2 = i1; j2 <= j1; ++j2)
                {
                    blockpos1 = pos.add(l1, i2, j2);
                    if (!boxIn.isVecInside(blockpos1)) continue;

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
                        if (boxIn.isVecInside(blockpos2)) if (world_.isAirBlock(blockpos2))
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
                                world_.setBlockState(blockpos2,
                                        Blocks.CHEST.correctFacing(world_, blockpos2, Blocks.CHEST.getDefaultState()),
                                        2);
                                TileEntity tileentity1 = world_.getTileEntity(blockpos2);

                                if (tileentity1 instanceof TileEntityChest)
                                {
                                    ((TileEntityChest) tileentity1).setLootTable(LootTableList.CHESTS_SIMPLE_DUNGEON,
                                            rand.nextLong());
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
        for (int h1 = 0; h1 < 15; h1++)
        {
            blockpos1 = pos.add(k - 1, h1, 0);
            if (boxIn.isVecInside(blockpos1)) world_.setBlockState(blockpos1,
                    Blocks.LADDER.getDefaultState().withProperty(BlockLadder.FACING, EnumFacing.WEST), 2);
        }
        // Build a trap door.
        blockpos1 = pos.add(k - 1, 14, 0);
        if (boxIn.isVecInside(blockpos1)) world_.setBlockState(blockpos1, Blocks.TRAPDOOR.getDefaultState(), 2);

        if (boxIn.isVecInside(pos))
        {
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
    }

}
