package dorfgen.conversion;

import java.awt.Color;
import java.util.Comparator;
import java.util.Iterator;
import java.util.List;
import java.util.Set;
import java.util.logging.Level;

import com.google.common.collect.Lists;
import com.google.common.collect.Sets;

import dorfgen.WorldGenerator;
import net.minecraft.init.Biomes;
import net.minecraft.world.biome.Biome;
import net.minecraftforge.common.BiomeDictionary;
import net.minecraftforge.common.BiomeDictionary.Type;

public class BiomeConversion
{
    int               rgb;
    private Biome     match;
    public List<Type> types = Lists.newArrayList();

    public BiomeConversion(Color color, Type... types)
    {
        this.rgb = color.getRGB();
        for (Type t : types)
            this.types.add(t);
    }

    public boolean matches(int rgb)
    {
        return this.rgb == rgb;
    }

    public Biome getBestMatch()
    {
        if (match == null)
        {
            Set<Biome> biomes;
            List<Biome> sorted = Lists.newArrayList();
            Comparator<Biome> comparator = new Comparator<Biome>()
            {
                @Override
                public int compare(Biome o1, Biome o2)
                {
                    return o1.getRegistryName().compareTo(o2.getRegistryName());
                }
            };
            for (Type type : types)
            {
                if (sorted.isEmpty())
                {
                    biomes = BiomeDictionary.getBiomes(type);
                    if (biomes.isEmpty())
                    {
                        WorldGenerator.log(Level.WARNING, "No Biomes found for type " + type);
                        continue;
                    }
                    if (sorted.isEmpty())
                    {
                        sorted.addAll(biomes);
                        sorted.sort(comparator);
                    }
                    match = sorted.get(0);
                    continue;
                }
                if (sorted.size() == 1)
                {
                    match = sorted.get(0);
                    break;
                }
                Iterator<Biome> iter = sorted.iterator();
                Set<Biome> noMatch = Sets.newHashSet();
                while (iter.hasNext())
                {
                    Biome temp = iter.next();
                    if (!BiomeDictionary.hasType(temp, type)) noMatch.add(temp);
                }
                if (noMatch.size() < sorted.size()) sorted.removeAll(noMatch);
            }
            Set<Biome> noMatch = Sets.newHashSet();
            for (Biome biome : sorted)
            {
                if (!types.contains(Type.COLD) && BiomeDictionary.hasType(biome, Type.COLD)) noMatch.add(biome);
                if (!types.contains(Type.HOT) && BiomeDictionary.hasType(biome, Type.HOT)) noMatch.add(biome);
                if (!types.contains(Type.DRY) && BiomeDictionary.hasType(biome, Type.DRY)) noMatch.add(biome);
                if (!types.contains(Type.WET) && BiomeDictionary.hasType(biome, Type.WET)) noMatch.add(biome);
            }
            if (noMatch.size() < sorted.size()) sorted.removeAll(noMatch);
            if (sorted.size() >= 1)
            {
                match = sorted.get(0);
            }
            if (match == null)
            {
                match = Biomes.OCEAN;
                WorldGenerator.log(Level.WARNING, "No Biome found for types " + types);
            }
        }
        return match;
    }

    public void clear()
    {
        match = null;
    }
}
