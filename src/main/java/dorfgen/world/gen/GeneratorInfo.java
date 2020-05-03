package dorfgen.world.gen;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;

import dorfgen.Dorfgen;
import dorfgen.conversion.DorfMap;
import dorfgen.util.ISigmoid;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.IWorld;

public class GeneratorInfo
{
    public static final Gson gson = new GsonBuilder().create();

    public static GeneratorInfo fromJson(final String json)
    {
        if (json == null || json.isEmpty()) return GeneratorInfo.getDefault();
        return GeneratorInfo.gson.fromJson(json, GeneratorInfo.class);
    }

    public static GeneratorInfo getDefault()
    {
        final GeneratorInfo info = new GeneratorInfo();
        return info;
    }

    public String region = Dorfgen.instance.getDorfs((IWorld) null).name;
    // Mapping of embark tiles -> blocks
    public int scaleh = 51;
    // Mapping of vertical scale -> blocks
    public int scalev = 8;
    // Shift of the world map, in blocks.
    public int dx = 0;
    public int dz = 0;
    // Spawn site of the world, in Block coords
    public int sx = 0;
    public int sy = 0;
    public int sz = 0;
    // Site to spawn at, if random spawn is false.
    public String spawn = "";
    // Should a random site be picked for spawn.
    public boolean random = true;
    // Are rivers made
    public boolean rivers = true;
    // Are sites made
    public boolean sites = true;
    // Are constructions made
    public boolean constructs = true;
    // Do vanilla villages spawn.
    public boolean villages = false;

    public GeneratorInfo()
    {
    }

    public DorfMap create(final boolean vanilla)
    {
        final DorfMap map = Dorfgen.instance.getDorfs(this.region);
        if (vanilla)
        {
            if (this.scalev == 1) map.setElevationSigmoid(new ISigmoid()
            {
                @Override
                public int elevationSigmoid(final int preHeight)
                {
                    return preHeight * GeneratorInfo.this.scalev;
                }
            });
            else map.setElevationSigmoid(new ISigmoid()
            {
            });
        }
        else map.setElevationSigmoid(new ISigmoid()
        {
            @Override
            public int elevationSigmoid(final int preHeight)
            {
                return preHeight * GeneratorInfo.this.scalev;
            }
        });
        map.setScale(this.scaleh);
        map.spawn = new BlockPos(this.sx, this.sy, this.sz);
        map.shift = new BlockPos(this.dx, 0, this.dz);
        map.randomSpawn = this.random;
        map.spawnSite = this.spawn;
        return map;
    }

    @Override
    public String toString()
    {
        return GeneratorInfo.gson.toJson(this);
    }
}