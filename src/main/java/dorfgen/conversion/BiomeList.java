package dorfgen.conversion;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Random;
import java.util.logging.Level;

import com.google.common.collect.Lists;

import dorfgen.WorldGenerator;
import dorfgen.conversion.DorfMap.Region;
import net.minecraft.init.Biomes;
import net.minecraft.world.biome.Biome;
import net.minecraftforge.common.BiomeDictionary;
import net.minecraftforge.common.BiomeDictionary.Type;

public class BiomeList
{

    public static int                        FREEZING  = 67;
    public static int                        COLD      = 80;
    public static int                        TEMPERATE = 128;
    public static int                        WARM      = 155;
    public static int                        HOT       = 180;
    public static int                        SCORCHING = 255;

    public static int                        DRY       = 100;
    public static int                        WET       = 200;

    private static ArrayList<Biome>          biomeArray;

    public HashMap<Integer, BiomeConversion> biomes    = new HashMap<Integer, BiomeConversion>();

    public void clear()
    {
        biomeArray = null;
        for (BiomeConversion con : biomes.values())
        {
            con.clear();
        }
    }

    // Takes relative map coordinates.
    public Biome mutateBiome(Biome input, int x, int z, DorfMap dorfs)
    {
        initArr();
        int scale = dorfs.scale;
        if (input == null)
        {
            int value = dorfs.biomeInterpolator.interpolateBiome(dorfs.biomeMap, x, z, scale);
            BiomeConversion conversion = biomes.get(value);
            if (conversion != null) input = conversion.getBestMatch();
            else
            {
                if (DorfMap.inBounds(x / scale, z / scale, dorfs.biomeMap))
                {
                    WorldGenerator.log(Level.WARNING, "No Biome found for " + value + " at " + x + " " + z);
                    System.out.println(biomes.keySet());
                }
                return Biomes.OCEAN;
            }
        }
        boolean hasHeightmap = dorfs.elevationMap.length > 0;
        boolean hasThermalMap = dorfs.temperatureMap.length > 0;
        boolean hasRainMap = dorfs.rainMap.length > 0;
        boolean hasVolcanismMap = dorfs.volcanismMap.length > 0;
        boolean hasVegMap = dorfs.vegitationMap.length > 0;
        int seaLevel = dorfs.sigmoid.elevationSigmoid(dorfs.seaLevel);
        Region region = dorfs.getRegionForCoords(dorfs.unShiftX(x), dorfs.unShiftZ(z));
        long key = region.name.hashCode();
        Random rand = new Random(key);
        int deepSea = (int) (seaLevel * 0.7);
        int height = hasHeightmap ? dorfs.heightInterpolator.interpolateHeight(scale, x, z, dorfs.elevationMap)
                : seaLevel;
        int temperature = hasThermalMap ? dorfs.miscInterpolator.interpolateHeight(scale, x, z, dorfs.temperatureMap)
                : 128;
        if (BiomeDictionary.hasType(input, Type.BEACH))
        {
            List<Biome> beaches = Lists.newArrayList(BiomeDictionary.getBiomes(Type.BEACH));
            beaches.sort(new Comparator<Biome>()
            {
                @Override
                public int compare(Biome o1, Biome o2)
                {
                    return o1.getRegistryName().compareTo(o2.getRegistryName());
                }
            });
            Collections.shuffle(beaches, rand);
            if (temperature < 80)
            {
                for (Biome b : beaches)
                {
                    if (BiomeDictionary.hasType(b, Type.COLD))
                    {
                        input = b;
                        break;
                    }
                }
            }
            else
            {
                for (Biome b : beaches)
                {
                    if (!BiomeDictionary.hasType(b, Type.COLD))
                    {
                        input = b;
                        break;
                    }
                }
            }
        }
        if (height > deepSea && (input == Biomes.DEEP_OCEAN || input == Biomes.OCEAN))
        {
            input = Biomes.OCEAN;
            if (temperature < 65)
            {
                input = Biomes.FROZEN_OCEAN;
            }
        }
        if (height <= deepSea && (input == Biomes.OCEAN))
        {
            input = Biomes.DEEP_OCEAN;
        }
        return input;
    }

    private void initArr()
    {
        if (biomeArray == null)
        {
            biomeArray = new ArrayList<Biome>();
            Iterator<Biome> iter = Biome.REGISTRY.iterator();
            while (iter.hasNext())
            {
                Biome b = iter.next();
                if (b != null) biomeArray.add(b);
            }
        }
    }

}
