package dorfgen.worldgen.cubic;

import net.minecraft.world.DimensionType;
import net.minecraft.world.WorldProvider;

public class WorldProviderCubic extends WorldProvider
{

    public WorldProviderCubic()
    {
        super();
    }

    @Override
    public DimensionType getDimensionType()
    {
        return WorldTypeCubic.TYPE;
    }

    @Override
    public boolean canCoordinateBeSpawn(int x, int z)
    {
        return true;
    }
}
