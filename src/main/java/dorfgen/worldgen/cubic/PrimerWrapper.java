package dorfgen.worldgen.cubic;

import cubicchunks.worldgen.generator.ICubePrimer;
import net.minecraft.block.state.IBlockState;
import net.minecraft.world.chunk.ChunkPrimer;

public class PrimerWrapper extends ChunkPrimer
{
    final ICubePrimer wrapped;

    public PrimerWrapper(ICubePrimer wrapped)
    {
        this.wrapped = wrapped;
    }

    @Override
    public IBlockState getBlockState(int x, int y, int z)
    {
        if (y < 0 || y > 15) return ICubePrimer.DEFAULT_STATE;
        return wrapped.getBlockState(x, y, z);
    }

    @Override
    public void setBlockState(int x, int y, int z, IBlockState state)
    {
        if (y < 0 || y > 15) return;
        wrapped.setBlockState(x, y, z, state);
    }
}
