package dorfgen.worldgen.cubic;

import dorfgen.worldgen.common.IPrimerWrapper;
import io.github.opencubicchunks.cubicchunks.api.worldgen.CubePrimer;
import net.minecraft.block.state.IBlockState;
import net.minecraft.world.chunk.ChunkPrimer;

public class PrimerWrapper extends ChunkPrimer implements IPrimerWrapper
{
    final CubePrimer wrapped;
    int              minY = 0;
    int              maxY = 2047;

    public PrimerWrapper()
    {
        this.wrapped = new CubePrimer();
    }

    @Override
    public IBlockState getBlockState(int x, int y, int z)
    {
        if (y < minY || y > maxY) return CubePrimer.DEFAULT_STATE;
        return wrapped.getBlockState(x, y, z);
    }

    @Override
    public void setBlockState(int x, int y, int z, IBlockState state)
    {
        if (y < minY || y > maxY) return;
        wrapped.setBlockState(x, y, z, state);
    }

    int x, z;

    @Override
    public void setX(int x)
    {
        this.x = x;
    }

    @Override
    public void setZ(int z)
    {
        this.z = z;
    }

    @Override
    public int getX()
    {
        return x;
    }

    @Override
    public int getZ()
    {
        return z;
    }
}
