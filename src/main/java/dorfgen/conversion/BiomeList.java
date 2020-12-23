package dorfgen.conversion;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Random;

import com.google.common.collect.Lists;

import dorfgen.conversion.DorfMap.Region;
import dorfgen.conversion.DorfMap.RegionType;
import dorfgen.world.feature.BeachMaker;
import net.minecraft.util.SharedSeedRandom;
import net.minecraft.world.biome.Biome;
import net.minecraft.world.biome.Biomes;
import net.minecraft.world.gen.PerlinNoiseGenerator;
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

    public PerlinNoiseGenerator noiseGen;
    public Biome[][]            biomeArr;

    public void init(final DorfMap map)
    {
        if (this.biomeArr == null) this.biomeArr = new Biome[map.biomeMap.length][map.biomeMap[0].length];
        for (int x = 0; x < map.biomeMap.length; x++)
            for (int z = 0; z < map.biomeMap[0].length; z++)
            {
                final int rgb = map.biomeMap[x][z];
                final BiomeConversion bc = this.biomes.get(rgb);
                this.biomeArr[x][z] = this.initBiomes(bc.getBestMatch(), x, z, map);
            }
    }

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
        final int scale = dorfs.getScale();
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

    // Takes relative map coordinates.
    public Biome initBiomes(Biome input, final int pixelx, final int pixelz, final DorfMap dorfs)
    {
        this.initArr();
        final boolean hasHeightmap = dorfs.elevationMap.length > 0;
        final boolean hasThermalMap = dorfs.temperatureMap.length > 0;
        // final boolean hasRainMap = dorfs.rainMap.length > 0;
        // final boolean hasVolcanismMap = dorfs.volcanismMap.length > 0;
        // final boolean hasVegMap = dorfs.vegitationMap.length > 0;
        final int seaLevel = dorfs.sigmoid.elevationSigmoid(dorfs.seaLevel);

        final int deepSea = (int) (seaLevel * 0.7);
        final int height = hasHeightmap ? dorfs.elevationMap[pixelx][pixelz] : seaLevel;
        final int temperature = hasThermalMap ? dorfs.temperatureMap[pixelx][pixelz] : 128;
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
            final Region region = this.getNearestNotOcean(pixelx, pixelz, dorfs);
            final long key = region.name.hashCode();
            final SharedSeedRandom rand = new SharedSeedRandom(key);
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

    private Region getNearestNotOcean(final int pixelx, final int pixelz, final DorfMap dorfs)
    {
        final Region region = dorfs.getRegionForPixel(pixelx, pixelz);
        if (region.type == RegionType.OCEAN)
        {
            Region newRegion = null;
            int dist = Integer.MAX_VALUE;
            for (int i = -20; i <= 20; i++)
                for (int j = -20; j <= 20; j++)
                {
                    final int ds2 = i * i + j * j;
                    if (newRegion == null || ds2 < dist)
                    {
                        final Region test = dorfs.getRegionForPixel(pixelx + i, pixelz + j);
                        if (test != null && test.type != RegionType.OCEAN)
                        {
                            newRegion = test;
                            dist = ds2;
                        }
                    }
                }
            if (newRegion != null) return newRegion;
        }
        return region;
    }

    private void initArr()
    {
        if (BiomeList.biomeArray == null) BiomeList.biomeArray = new ArrayList<>(ForgeRegistries.BIOMES.getValues());
    }
}
