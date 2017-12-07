package dorfgen.worldgen.common;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;

import dorfgen.WorldGenerator;

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
    public int     scaleh     = 51;
    public int     scalev     = 8;
    public boolean rivers     = true;
    public boolean sites      = true;
    public boolean constructs = true;

    public GeneratorInfo()
    {
    }

    @Override
    public String toString()
    {
        return gson.toJson(this);
    }
}