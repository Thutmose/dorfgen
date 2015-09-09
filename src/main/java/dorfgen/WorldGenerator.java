package dorfgen;

import java.awt.image.BufferedImage;
import java.io.File;
import java.lang.reflect.Field;

import net.minecraft.block.Block;
import net.minecraft.client.Minecraft;
import net.minecraft.init.Blocks;
import net.minecraft.util.ChunkCoordinates;
import net.minecraft.util.EnumFacing;
import net.minecraft.world.World;
import net.minecraft.world.WorldProvider;
import net.minecraft.world.WorldServer;
import net.minecraft.world.WorldType;
import net.minecraft.world.biome.BiomeGenBase;
import net.minecraft.world.chunk.Chunk;
import net.minecraft.world.chunk.storage.IChunkLoader;
import net.minecraft.world.gen.ChunkProviderServer;
import net.minecraft.world.gen.structure.MapGenStructureIO;
import net.minecraft.world.gen.structure.MapGenVillage;
import net.minecraft.world.storage.ISaveHandler;
import net.minecraft.world.storage.MapStorage;
import net.minecraftforge.common.BiomeManager;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.event.terraingen.PopulateChunkEvent;
import net.minecraftforge.event.world.ChunkEvent;
import net.minecraftforge.event.world.WorldEvent;
import net.minecraftforge.event.world.WorldEvent.Load;
import cpw.mods.fml.common.FMLCommonHandler;
import cpw.mods.fml.common.Loader;
import cpw.mods.fml.common.Mod;
import cpw.mods.fml.common.ModMetadata;
import cpw.mods.fml.common.Mod.EventHandler;
import cpw.mods.fml.common.event.FMLInitializationEvent;
import cpw.mods.fml.common.event.FMLPostInitializationEvent;
import cpw.mods.fml.common.event.FMLPreInitializationEvent;
import cpw.mods.fml.common.eventhandler.SubscribeEvent;
import cpw.mods.fml.common.registry.GameRegistry;
import dorfgen.conversion.Config;
import dorfgen.conversion.DorfMap;
import dorfgen.conversion.FileLoader;
import dorfgen.conversion.DorfMap.Site;
import dorfgen.worldgen.ChunkProviderFinite;
import dorfgen.worldgen.MapGenSites;
import dorfgen.worldgen.WorldChunkManagerFinite;
import dorfgen.worldgen.WorldTypeFinite;
import dorfgen.worldgen.MapGenSites.Start;

@Mod(modid = WorldGenerator.MODID, name = WorldGenerator.NAME, version = "1.7.10", acceptableRemoteVersions = "*")
public class WorldGenerator {

	public static final String MODID = "dorfgen";
	public static final String NAME = "DF World Generator";

	@Mod.Instance(MODID)
	public static WorldGenerator instance;

	public BufferedImage elevationMap;
	public BufferedImage elevationWaterMap;
	public BufferedImage biomeMap;
	public BufferedImage evilMap;
	public BufferedImage temperatureMap;
	public BufferedImage rainMap;
	public BufferedImage drainageMap;
	public BufferedImage vegitationMap;

	public DorfMap dorfs;

	public static int scale;
	public static boolean finite;
	public static ChunkCoordinates spawn;
	public static ChunkCoordinates shift;
	public static boolean randomSpawn;

	public WorldType finiteWorldType;

	public static String configLocation;
	public static String biomes;

	public static Class chunkClass = Chunk.class;

	public WorldGenerator() {
		instance = this;
	}

	@EventHandler
	public void preInit(FMLPreInitializationEvent e) {
		MinecraftForge.EVENT_BUS.register(this);
		MinecraftForge.TERRAIN_GEN_BUS.register(this);
		new Config(e);
		File file = e.getSuggestedConfigurationFile();
		String seperator = System.getProperty("file.separator");

		GameRegistry.registerItem(new ItemDebug().setTextureName("diamond"), "debugItem");
		GameRegistry.registerBlock(new BlockUGGrass().setBlockName("darkgrass"), "darkgrass");

		String folder = file.getAbsolutePath();
		String name = file.getName();
		FileLoader.biomes = folder.replace(name, MODID + seperator + "biomes.csv");
		//
		MapGenStructureIO.func_143031_a(Start.class, "dorfsitestart");
		MapGenStructureIO.registerStructure(Start.class, "dorfsitestart");
		//
	}

	@EventHandler
	public void load(FMLInitializationEvent evt) {
		finiteWorldType = new WorldTypeFinite("finite");
		try {
			//chunkClass = Class.forName("bigworld.storage.BigChunk");
		} catch (Exception e) {
		}

	}

	@EventHandler
	public void postInit(FMLPostInitializationEvent e) {
		new FileLoader();
		dorfs = new DorfMap();

		for (BiomeGenBase b : BiomeGenBase.getBiomeGenArray()) {
			if (b != null && !MapGenVillage.villageSpawnBiomes.contains(b)) {
				BiomeManager.addVillageBiome(b, true);
			}
		}
	}

	@SubscribeEvent
	public void genEvent(Load evt) {
		if (evt.world.provider.worldChunkMgr instanceof WorldChunkManagerFinite) {

			if (randomSpawn) {
				for (Site s : dorfs.sitesById.values()) {
					if (s.type.isVillage()) {
						int x = s.x * 16 * scale;
						int y = 0;
						int z = s.z * 16 * scale;
						try
						{
							y = dorfs.elevationMap[(x - shift.posX) / scale][(z - shift.posZ) / scale];
						}
						catch (Exception e)
						{
							System.out.println(s+" "+dorfs.elevationMap.length);
							e.printStackTrace();
						}
						evt.world.setSpawnLocation(x + 16 * scale / 2, y, z + 16 * scale / 2);
						return;
					}
				}
			} else {
				evt.world.setSpawnLocation(spawn.posX, spawn.posY, spawn.posZ);
			}
		}
	}

	@SubscribeEvent
	public void chunkLoadEvent(PopulateChunkEvent.Post evt) {
		World world = evt.world;
		int cX = evt.chunkX * 16;
		int cZ = evt.chunkZ * 16;
		for (int i = 0; i < 16; i++) {
			for (int k = 0; k < 16; k++) {
				BiomeGenBase biome = world.getBiomeGenForCoords(cX + i, cZ + k);
				if (biome != BiomeGenBase.river) // || true)
					continue;

				for (int j = 255; j > 0; j--) {
					Block b = world.getBlock(cX + i, j, cZ + k);
					if (b != Blocks.air && b != null) {
						if (b == Blocks.water) {
							b.onNeighborBlockChange(world, cX + i, j, cZ + k, b);
						}
						break;
					}

				}
			}
		}
	}

}
