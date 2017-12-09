package dorfgen.worldgen.common;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;

import dorfgen.WorldGenerator;
import dorfgen.conversion.DorfMap;
import dorfgen.conversion.ISigmoid;
import net.minecraft.util.math.BlockPos;

public class GeneratorInfo
{
    public static final Gson gson = new GsonBuilder().create();

    public static GeneratorInfo fromJson(String json)
    {
        if (json == null || json.isEmpty()) return getDefault();
        return gson.fromJson(json, GeneratorInfo.class);
    }

    public static GeneratorInfo getDefault()
    {
        GeneratorInfo info = new GeneratorInfo();
        return info;
    }

    public String  region     = WorldGenerator.instance.defaultRegion;
    // Mapping of embark tiles -> blocks
    public int     scaleh     = 51;
    // Mapping of vertical scale -> blocks
    public int     scalev     = 8;
    // Shift of the world map, in blocks.
    public int     dx         = 0;
    public int     dz         = 0;
    // Spawn site of the world, in Block coords
    public int     sx         = 0;
    public int     sy         = 0;
    public int     sz         = 0;
    // Site to spawn at, if random spawn is false.
    public String  spawn      = "";
    // Should a random site be picked for spawn.
    public boolean random     = true;
    // Are rivers made
    public boolean rivers     = true;
    // Are sites made
    public boolean sites      = true;
    // Are constructions made
    public boolean constructs = true;
    // Do vanilla villages spawn.
    public boolean villages   = false;

    public GeneratorInfo()
    {
    }

    public DorfMap create(boolean vanilla)
    {
        DorfMap map = WorldGenerator.getDorfMap(region);
        if (vanilla)
        {
            if (scalev == 1)
            {
                map.setElevationSigmoid(new ISigmoid()
                {
                    @Override
                    public int elevationSigmoid(int preHeight)
                    {
                        return (preHeight) * scalev;
                    }
                });
            }
            else
            {
                map.setElevationSigmoid(new ISigmoid()
                {
                });
            }
        }
        else map.setElevationSigmoid(new ISigmoid()
        {
            @Override
            public int elevationSigmoid(int preHeight)
            {
                return (preHeight) * scalev;
            }
        });
        map.setScale(scaleh);
        map.spawn = new BlockPos(sx, sy, sz);
        map.shift = new BlockPos(dx, 0, dz);
        map.randomSpawn = random;
        map.spawnSite = spawn;
        return map;
    }

    @Override
    public String toString()
    {
        return gson.toJson(this);
    }
}
