package dorfgen.conversion;

import java.awt.Color;
import java.util.Comparator;
import java.util.Iterator;
import java.util.List;
import java.util.Set;

import com.google.common.collect.Lists;
import com.google.common.collect.Sets;

import dorfgen.Dorfgen;
import net.minecraft.world.biome.Biome;
import net.minecraft.world.biome.Biomes;
import net.minecraftforge.common.BiomeDictionary;
import net.minecraftforge.common.BiomeDictionary.Type;

public class BiomeConversion
{
    int               rgb;
    private Biome     match;
    public List<Type> types = Lists.newArrayList();

    public BiomeConversion(final Color color, final Type... types)
    {
        this.rgb = color.getRGB();
        for (final Type t : types)
            this.types.add(t);
    }

    public boolean matches(final int rgb)
    {
        return this.rgb == rgb;
    }

    public Biome getBestMatch()
    {
        if (this.match == null)
        {
            if (this.types.size() == 1 && this.types.get(0) == Type.RIVER)
            {
                this.match = Biomes.RIVER;
                return this.match;
            }
            Set<Biome> biomes;
            final List<Biome> sorted = Lists.newArrayList();
            final Comparator<Biome> comparator = (o1, o2) -> o1.getRegistryName().compareTo(o2.getRegistryName());
            for (final Type type : this.types)
            {
                if (sorted.isEmpty())
                {
                    biomes = BiomeDictionary.getBiomes(type);
                    if (biomes.isEmpty())
                    {
                        Dorfgen.LOGGER.warn("No Biomes found for type " + type);
                        continue;
                    }
                    if (sorted.isEmpty())
                    {
                        sorted.addAll(biomes);
                        sorted.sort(comparator);
                    }
                    this.match = sorted.get(0);
                    continue;
                }
                if (sorted.size() == 1)
                {
                    this.match = sorted.get(0);
                    break;
                }
                final Iterator<Biome> iter = sorted.iterator();
                final Set<Biome> noMatch = Sets.newHashSet();
                while (iter.hasNext())
                {
                    final Biome temp = iter.next();
                    if (!BiomeDictionary.hasType(temp, type)) noMatch.add(temp);
                }
                if (noMatch.size() < sorted.size()) sorted.removeAll(noMatch);
            }
            final Set<Biome> noMatch = Sets.newHashSet();
            for (final Biome biome : sorted)
            {
                if (!this.types.contains(Type.COLD) && BiomeDictionary.hasType(biome, Type.COLD)) noMatch.add(biome);
                if (!this.types.contains(Type.HOT) && BiomeDictionary.hasType(biome, Type.HOT)) noMatch.add(biome);
                if (!this.types.contains(Type.DRY) && BiomeDictionary.hasType(biome, Type.DRY)) noMatch.add(biome);
                if (!this.types.contains(Type.WET) && BiomeDictionary.hasType(biome, Type.WET)) noMatch.add(biome);
            }
            if (noMatch.size() < sorted.size()) sorted.removeAll(noMatch);
            if (sorted.size() >= 1) this.match = sorted.get(0);
            if (this.match == null)
            {
                this.match = Biomes.OCEAN;
                Dorfgen.LOGGER.warn("No Biomes found for type " + this.types);
            }
        }
        return this.match;
    }

    public void clear()
    {
        this.match = null;
    }
}
