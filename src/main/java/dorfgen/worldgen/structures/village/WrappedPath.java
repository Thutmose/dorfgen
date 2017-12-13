package dorfgen.worldgen.structures.village;

import java.util.Random;

import dorfgen.conversion.DorfMap;
import net.minecraft.block.state.IBlockState;
import net.minecraft.init.Blocks;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;
import net.minecraft.world.gen.structure.StructureBoundingBox;
import net.minecraft.world.gen.structure.StructureVillagePieces.Path;

public class WrappedPath extends Path
{
    DorfMap map;

    public WrappedPath()
    {
        // TODO Auto-generated constructor stub
    }

    public WrappedPath(DorfMap map)
    {
    }

    /** second Part of Structure generating, this for example places Spiderwebs,
     * Mob Spawners, it closes Mineshafts at the end, it adds Fences... */
    public boolean addComponentParts(World worldIn, Random randomIn, StructureBoundingBox structureBoundingBoxIn)
    {
        IBlockState iblockstate = this.getBiomeSpecificBlockState(Blocks.GRASS_PATH.getDefaultState());
        IBlockState iblockstate1 = this.getBiomeSpecificBlockState(Blocks.PLANKS.getDefaultState());
        IBlockState iblockstate2 = this.getBiomeSpecificBlockState(Blocks.GRAVEL.getDefaultState());
        IBlockState iblockstate3 = this.getBiomeSpecificBlockState(Blocks.COBBLESTONE.getDefaultState());

        for (int i = this.boundingBox.minX; i <= this.boundingBox.maxX; ++i)
        {
            for (int j = this.boundingBox.minZ; j <= this.boundingBox.maxZ; ++j)
            {
                BlockPos blockpos = new BlockPos(i, 64, j);

                if (structureBoundingBoxIn.isVecInside(blockpos))
                {
                    blockpos = worldIn.getTopSolidOrLiquidBlock(blockpos).down();

                    if (blockpos.getY() < worldIn.getSeaLevel())
                    {
                        blockpos = new BlockPos(blockpos.getX(), worldIn.getSeaLevel() - 1, blockpos.getZ());
                    }

                    while (blockpos.getY() >= worldIn.getSeaLevel() - 1)
                    {
                        IBlockState iblockstate4 = worldIn.getBlockState(blockpos);

                        if (iblockstate4.getBlock() == Blocks.GRASS && worldIn.isAirBlock(blockpos.up()))
                        {
                            worldIn.setBlockState(blockpos, iblockstate, 2);
                            break;
                        }

                        if (iblockstate4.getMaterial().isLiquid())
                        {
                            worldIn.setBlockState(blockpos, iblockstate1, 2);
                            break;
                        }

                        if (iblockstate4.getBlock() == Blocks.SAND || iblockstate4.getBlock() == Blocks.SANDSTONE
                                || iblockstate4.getBlock() == Blocks.RED_SANDSTONE)
                        {
                            worldIn.setBlockState(blockpos, iblockstate2, 2);
                            worldIn.setBlockState(blockpos.down(), iblockstate3, 2);
                            break;
                        }

                        blockpos = blockpos.down();
                    }
                }
            }
        }

        return true;
    }

}
