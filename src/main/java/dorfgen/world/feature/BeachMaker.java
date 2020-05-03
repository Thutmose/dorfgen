package dorfgen.world.feature;

import net.minecraft.world.biome.Biome;
import net.minecraft.world.biome.Biome.Category;
import net.minecraftforge.common.BiomeDictionary;
import net.minecraftforge.common.BiomeDictionary.Type;

public class BeachMaker
{
    public static boolean isBeachOrOcean(final Biome b1)
    {
        final boolean beach = BeachMaker.isBeach(b1);
        final boolean ocean = BeachMaker.isOcean(b1);
        return beach || ocean;
    }

    public static boolean isBeach(final Biome b1)
    {
        final Category cat = b1.getCategory();
        final boolean beach = cat == Category.BEACH || BiomeDictionary.hasType(b1, Type.BEACH);
        return beach;
    }

    public static boolean isOcean(final Biome b1)
    {
        final Category cat = b1.getCategory();
        final boolean ocean = cat == Category.OCEAN || BiomeDictionary.hasType(b1, Type.OCEAN);
        return ocean;
    }
}
