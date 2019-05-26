package dorfgen.worldgen.common;

import net.minecraft.world.chunk.ChunkPrimer;

public interface IPrimerWrapper
{
    default ChunkPrimer getPrimer()
    {
        return (ChunkPrimer) this;
    }

    void setX(int x);

    void setZ(int z);

    int getX();

    int getZ();
}
