package dorfgen.conversion;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Random;
import java.util.Set;

import dorfgen.conversion.DorfMap.Region;
import net.minecraft.init.Biomes;
import net.minecraft.world.biome.Biome;
import net.minecraftforge.common.BiomeDictionary;
import net.minecraftforge.common.BiomeDictionary.Type;

public class BiomeList
{

    public static HashMap<Integer, BiomeConversion> biomes    = new HashMap<Integer, BiomeConversion>();
    public static int                               FREEZING  = 67;
    public static int                               COLD      = 80;
    public static int                               TEMPERATE = 128;
    public static int                               WARM      = 155;
    public static int                               HOT       = 180;
    public static int                               SCORCHING = 255;

    public static int                               DRY       = 100;
    public static int                               WET       = 200;

    private static ArrayList<Biome>                 biomeArray;

    public static int GetBiomeIndex(int rgb)
    {
        if (biomes.containsKey(rgb)) return biomes.get(rgb).mineCraftBiome;
        return 0;
    }

    public static int getBiomeFromValues(int biome, int temperature, int drainage, int rainfall, int evil,
            Region region)
    {
        Biome b = Biome.getBiome(biome);

        if (temperature < TEMPERATE && !BiomeDictionary.hasType(b, Type.COLD))
        {
            boolean freezing = temperature < FREEZING;
            boolean matched = false;
            if (freezing && (BiomeDictionary.hasType(b, Type.OCEAN) || BiomeDictionary.hasType(b, Type.RIVER)
                    || BiomeDictionary.hasType(b, Type.BEACH)))
            {
                Biome temp = getMatch(b, Type.SNOWY);
                if (temp != b)
                {
                    b = temp;
                    matched = true;
                }
                if (!matched)
                {
                    b = getMatch(b, Type.COLD);
                }
            }
            else if (b != Biomes.RIVER)
            {
                // b = getMatch(b, Type.COLD);
            }
        }

        // if(true)
        return Biome.getIdForBiome(b);
        // //TODO finish this
        //
        // if(temperature > WARM && !BiomeDictionary.hasType(b, Type.HOT))
        // {
        // b = getMatch(b, Type.HOT);
        // }
        // if(rainfall > WET && !BiomeDictionary.hasType(b, Type.WET))
        // {
        // b = getMatch(b, Type.WET);
        // }
        // if(rainfall < DRY && !BiomeDictionary.hasType(b, Type.DRY))
        // {
        // b = getMatch(b, Type.DRY);
        // }
        // int newBiome = b.biomeID;
        // if(region!=null)
        // {
        // if(region.type==RegionType.GLACIER &&
        // BiomeDictionary.hasType(b, Type.PLAINS))
        // {
        // boolean cold = BiomeDictionary.hasType(b, Type.COLD) ||
        // BiomeDictionary.hasType(b, Type.SNOWY);
        // if(!cold)
        // {
        // b = getMatch(Biome.icePlains, Type.SNOWY);
        // }
        // }
        // if(region.biomeMap.containsKey(biome))
        // {
        // newBiome = region.biomeMap.get(biome);
        // }
        // else
        // {
        // region.biomeMap.put(biome, newBiome);
        // }
        // }
        //
        // return newBiome;
    }

    private static Random rand = new Random(1234);

    private static Biome getMatch(Biome toMatch, Type type)
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
        Set<Type> existing = BiomeDictionary.getTypes(toMatch);
        int i = rand.nextInt(123456);
        biomes:
        for (int j = 0; j < biomeArray.size(); j++)
        {
            Biome b = biomeArray.get((j + i) % biomeArray.size());
            if (b != toMatch && b != null)
            {
                if (!BiomeDictionary.hasType(b, type)) continue;
                for (Type t : existing)
                {
                    if (!BiomeDictionary.hasType(b, t)) continue biomes;
                }
                return b;
            }
        }
        return toMatch;
    }

}
