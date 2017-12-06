package dorfgen.worldgen.vanilla;

import net.minecraft.world.DimensionType;
import net.minecraft.world.WorldProvider;

public class WorldProviderFinite extends WorldProvider{

	public WorldProviderFinite() {
		
	}

    @Override
    public DimensionType getDimensionType()
    {
        // TODO Auto-generated method stub
        return DimensionType.OVERWORLD;
    }

}
