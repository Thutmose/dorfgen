package dorfgen.conversion;

import dorfgen.WorldGenerator;
import net.minecraft.util.math.BlockPos;
import net.minecraftforge.common.config.Configuration;
import net.minecraftforge.fml.common.event.FMLPreInitializationEvent;

public class Config
{

    public Config(FMLPreInitializationEvent e)
    {
        loadConfig(e);
    }

    void loadConfig(FMLPreInitializationEvent e)
    {
        Configuration config = new Configuration(e.getSuggestedConfigurationFile());
        config.load();

        WorldGenerator.scale = config.getInt("scale", Configuration.CATEGORY_GENERAL, 51, 1, 256,
                "number of blocks per pixel, for best results, use a multiple of 51");
        WorldGenerator.cubicHeightScale = config.getInt("cubicHeightScale", Configuration.CATEGORY_GENERAL, 8, 1, 256,
                "Scaling factor for world height when cubic chunks mod is being used.");
        WorldGenerator.finite = config.getBoolean("finite", Configuration.CATEGORY_GENERAL, true,
                "Whether everything outside the bounds of the image is deep ocean");
        WorldGenerator.roadBlock = config.getBoolean("roadBlock", Configuration.CATEGORY_GENERAL, true,
                "Is there a custom block for roads, set to false to enable server-side only.");
        boolean spawnpixel = config.getBoolean("pixel", Configuration.CATEGORY_GENERAL, false,
                "Whether the x and z coordinates for spawn given are pixel or block locations");
        String[] spawnLoc = config.getStringList("worldspawn", Configuration.CATEGORY_GENERAL,
                new String[] { "0", "64", "0" }, "spawn location for the world");
        WorldGenerator.randomSpawn = config.getBoolean("randomSpawn", Configuration.CATEGORY_GENERAL, true,
                "Whether spawn will be set to a random village, if this is true, worldspawn and pixel are ignored");
        int x = Integer.parseInt(spawnLoc[0]);
        int z = Integer.parseInt(spawnLoc[2]);

        WorldGenerator.finite = config.getBoolean("wrap", Configuration.CATEGORY_GENERAL, false,
                "should entities be wrapped around the map if they stray off the loaded image.");
        new dorfgen.finite.FiniteHandler();

        if (spawnpixel)
        {
            x *= WorldGenerator.scale;
            z *= WorldGenerator.scale;
        }

        WorldGenerator.spawn = new BlockPos(x, Integer.parseInt(spawnLoc[1]), z);

        WorldGenerator.spawnSite = config.getString("spawnSite", Configuration.CATEGORY_GENERAL, "",
                "Default Site for Spawning in, overrides random spawn and coord based spawn.");

        spawnLoc = config.getStringList("imageShift", Configuration.CATEGORY_GENERAL, new String[] { "0", "0" },
                "offset of the image in world in blocks");
        WorldGenerator.shift = new BlockPos(Integer.parseInt(spawnLoc[0]), 0, Integer.parseInt(spawnLoc[1]));

        config.save();
    }

}
