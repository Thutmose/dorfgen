package dorfgen.conversion;

import net.minecraft.util.ChunkCoordinates;
import net.minecraftforge.common.config.Configuration;
import cpw.mods.fml.common.event.FMLPreInitializationEvent;
import dorfgen.WorldGenerator;

public class Config {

	public Config(FMLPreInitializationEvent e) {
		loadConfig(e);
	}

	void loadConfig(FMLPreInitializationEvent e)
	{
		Configuration config = new Configuration(e.getSuggestedConfigurationFile());
		config.load();
		
		WorldGenerator.scale = config.getInt("scale", config.CATEGORY_GENERAL, 8, 1, 16, "number of blocks per pixel, allowed values are from 1 to 16");
		WorldGenerator.finite = config.getBoolean("finite", config.CATEGORY_GENERAL, true, "Whether everything outside the bounds of the image is deep ocean");
		boolean spawnpixel = config.getBoolean("pixel", config.CATEGORY_GENERAL, false, "Whether the x and z coordinates for spawn given are pixel or block locations");
		String[] spawnLoc = config.getStringList("worldspawn", config.CATEGORY_GENERAL, new String[]{"0","64","0"}, "spawn location for the world");
		WorldGenerator.randomSpawn = config.getBoolean("randomSpawn", config.CATEGORY_GENERAL, true, "Whether spawn will be set to a random village, if this is true, worldspawn and pixel are ignored");
		int x = Integer.parseInt(spawnLoc[0]);
		int z = Integer.parseInt(spawnLoc[2]);
		
		if(spawnpixel)
		{
			x *= WorldGenerator.scale;
			z *= WorldGenerator.scale;
		}
		
		WorldGenerator.spawn = new ChunkCoordinates(x, Integer.parseInt(spawnLoc[1]), z);
		
		spawnLoc = config.getStringList("imageShift", config.CATEGORY_GENERAL, new String[]{"0","0"}, "offset of the image in world in blocks");
		WorldGenerator.shift = new ChunkCoordinates(Integer.parseInt(spawnLoc[0]), 0, Integer.parseInt(spawnLoc[1]));
		
		config.save();
	}
	
}
