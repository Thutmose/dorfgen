package dorfgen.conversion;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Random;

import com.google.common.collect.Lists;

import dorfgen.Dorfgen;
import dorfgen.conversion.DorfMap.Region;
import dorfgen.world.feature.BeachMaker;
import net.minecraft.world.biome.Biome;
import net.minecraft.world.biome.Biomes;
import net.minecraftforge.common.BiomeDictionary;
import net.minecraftforge.common.BiomeDictionary.Type;
import net.minecraftforge.registries.ForgeRegistries;

public class BiomeList
{

    public static int FREEZING  = 67;
    public static int COLD      = 80;
    public static int TEMPERATE = 128;
    public static int WARM      = 155;
    public static int HOT       = 180;
    public static int SCORCHING = 255;

    public static int DRY = 100;
    public static int WET = 200;

    private static ArrayList<Biome> biomeArray;

    public HashMap<Integer, BiomeConversion> biomes = new HashMap<>();

    public void clear()
    {
        BiomeList.biomeArray = null;
        for (final BiomeConversion con : this.biomes.values())
            con.clear();
    }

    // Takes relative map coordinates.
    public Biome mutateBiome(Biome input, final int x, final int z, final DorfMap dorfs)
    {
        this.initArr();
        final int scale = dorfs.scale;
        if (input == null)
        {
            final int value = dorfs.biomeInterpolator.interpolateBiome(dorfs.biomeMap, x, z, scale);
            final BiomeConversion conversion = this.biomes.get(value);
            if (conversion != null) input = conversion.getBestMatch();
            else
            {
                if (value == 0) return Biomes.OCEAN;
                else if (DorfMap.inBounds(x / scale, z / scale, dorfs.biomeMap))
                {
                    Dorfgen.LOGGER.warn("No Biome found for " + value + " at " + x + " " + z);
                    System.out.println(this.biomes.keySet());
                }
                return Biomes.OCEAN;
            }
        }
        final boolean hasHeightmap = dorfs.elevationMap.length > 0;
        final boolean hasThermalMap = dorfs.temperatureMap.length > 0;
        // final boolean hasRainMap = dorfs.rainMap.length > 0;
        // final boolean hasVolcanismMap = dorfs.volcanismMap.length > 0;
        // final boolean hasVegMap = dorfs.vegitationMap.length > 0;
        final int seaLevel = dorfs.sigmoid.elevationSigmoid(dorfs.seaLevel);
        final Region region = dorfs.getRegionForCoords(dorfs.unShiftX(x), dorfs.unShiftZ(z));
        final long key = region.name.hashCode();
        final Random rand = new Random(key);
        final int deepSea = (int) (seaLevel * 0.7);
        final int height = hasHeightmap ? dorfs.heightInterpolator.interpolate(dorfs.elevationMap, x, z, scale)
                : seaLevel;
        final int temperature = hasThermalMap ? dorfs.miscInterpolator.interpolate(dorfs.temperatureMap, x, z, scale)
                : 128;
        boolean beach = BeachMaker.isBeach(input);
        final boolean ocean = BeachMaker.isOcean(input);

        final boolean beachOrOcean = beach || ocean;
        if (height > 60 && beachOrOcean) beach = true;
        else if (height > deepSea && beachOrOcean)
        {
            beach = false;
            input = Biomes.WARM_OCEAN;
            if (temperature < BiomeList.WARM) input = Biomes.LUKEWARM_OCEAN;
            if (temperature < BiomeList.TEMPERATE) input = Biomes.OCEAN;
            if (temperature < BiomeList.COLD) input = Biomes.COLD_OCEAN;
            if (temperature < BiomeList.FREEZING) input = Biomes.FROZEN_OCEAN;
        }
        else if (beachOrOcean)
        {
            beach = false;
            input = Biomes.DEEP_WARM_OCEAN;
            if (temperature < BiomeList.WARM) input = Biomes.DEEP_LUKEWARM_OCEAN;
            if (temperature < BiomeList.TEMPERATE) input = Biomes.DEEP_OCEAN;
            if (temperature < BiomeList.COLD) input = Biomes.DEEP_COLD_OCEAN;
            if (temperature < BiomeList.FREEZING) input = Biomes.DEEP_FROZEN_OCEAN;
        }
        if (beach)
        {
            final List<Biome> beaches = Lists.newArrayList(BiomeDictionary.getBiomes(Type.BEACH));
            beaches.sort((o1, o2) -> o1.getRegistryName().compareTo(o2.getRegistryName()));
            Collections.shuffle(beaches, rand);
            if (temperature < BiomeList.COLD)
            {
                for (final Biome b : beaches)
                    if (BiomeDictionary.hasType(b, Type.COLD))
                    {
                        input = b;
                        break;
                    }
            }
            else for (final Biome b : beaches)
                if (!BiomeDictionary.hasType(b, Type.COLD))
                {
                    input = b;
                    break;
                }
        }
        return input;
    }

    private void initArr()
    {
        if (BiomeList.biomeArray == null) BiomeList.biomeArray = new ArrayList<>(ForgeRegistries.BIOMES.getValues());
    }
}
