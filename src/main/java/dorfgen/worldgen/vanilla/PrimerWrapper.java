package dorfgen.worldgen.vanilla;

import net.minecraft.block.state.IBlockState;
import net.minecraft.init.Blocks;
import net.minecraft.world.chunk.ChunkPrimer;

public class PrimerWrapper extends ChunkPrimer
{
    private static final IBlockState DEFAULT_STATE = Blocks.AIR.getDefaultState();
    final ChunkPrimer                wrapped;

    public PrimerWrapper(ChunkPrimer wrapped)
    {
        this.wrapped = wrapped;
    }

    @Override
    public IBlockState getBlockState(int x, int y, int z)
    {
        if (y < 0 || y > 255) return DEFAULT_STATE;
        return wrapped.getBlockState(x, y, z);
    }

    @Override
    public void setBlockState(int x, int y, int z, IBlockState state)
    {
        if (y < 0 || y > 255) return;
        wrapped.setBlockState(x, y, z, state);
    }
}
