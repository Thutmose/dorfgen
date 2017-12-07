package dorfgen;

import java.awt.image.BufferedImage;
import java.io.File;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;

import dorfgen.conversion.Config;
import dorfgen.conversion.DorfMap;
import dorfgen.conversion.DorfMap.Site;
import dorfgen.conversion.DorfMap.SiteType;
import dorfgen.conversion.FileLoader;
import dorfgen.conversion.SiteMapColours;
import dorfgen.conversion.SiteStructureGenerator;
import dorfgen.worldgen.common.BiomeProviderFinite;
import dorfgen.worldgen.common.MapGenSites.Start;
import dorfgen.worldgen.cubic.WorldTypeCubic;
import dorfgen.worldgen.vanilla.WorldTypeFinite;
import net.minecraft.block.Block;
import net.minecraft.client.renderer.block.model.ModelResourceLocation;
import net.minecraft.init.Blocks;
import net.minecraft.item.Item;
import net.minecraft.item.ItemBlock;
import net.minecraft.util.ResourceLocation;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.WorldType;
import net.minecraft.world.gen.structure.MapGenStructureIO;
import net.minecraftforge.client.model.ModelLoader;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.event.RegistryEvent;
import net.minecraftforge.event.terraingen.DecorateBiomeEvent.Decorate;
import net.minecraftforge.event.terraingen.DecorateBiomeEvent.Decorate.EventType;
import net.minecraftforge.event.world.WorldEvent.Load;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.common.Mod.EventHandler;
import net.minecraftforge.fml.common.Optional;
import net.minecraftforge.fml.common.event.FMLInitializationEvent;
import net.minecraftforge.fml.common.event.FMLLoadCompleteEvent;
import net.minecraftforge.fml.common.event.FMLPostInitializationEvent;
import net.minecraftforge.fml.common.event.FMLPreInitializationEvent;
import net.minecraftforge.fml.common.event.FMLServerStartingEvent;
import net.minecraftforge.fml.common.eventhandler.Event.Result;
import net.minecraftforge.fml.common.eventhandler.EventPriority;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;

@Mod(modid = WorldGenerator.MODID, name = WorldGenerator.NAME, version = "1.8", acceptableRemoteVersions = "*")
public class WorldGenerator
{

    public static final String          MODID            = "dorfgen";
    public static final String          NAME             = "DF World Generator";

    @Mod.Instance(MODID)
    public static WorldGenerator        instance;

    public BufferedImage                elevationMap;
    public BufferedImage                elevationWaterMap;
    public BufferedImage                biomeMap;
    public BufferedImage                evilMap;
    public BufferedImage                temperatureMap;
    public BufferedImage                rainMap;
    public BufferedImage                drainageMap;
    public BufferedImage                vegitationMap;
    public BufferedImage                structuresMap;

    public final DorfMap                dorfs            = new DorfMap();
    public final SiteStructureGenerator structureGen     = new SiteStructureGenerator(dorfs);

    public static int                   scale;
    public static int                   cubicHeightScale = 8;
    public static boolean               finite;
    public static int                   yMin             = 0;
    public static BlockPos              spawn;
    public static BlockPos              shift;
    public static String                spawnSite        = "";
    public static boolean               randomSpawn;
    public static boolean               roadBlock        = true;

    public WorldType                    finiteWorldType;

    public static String                configLocation;
    public static String                biomes;

    public static Block                 roadSurface      = new BlockRoadSurface()
            .setRegistryName(new ResourceLocation(MODID, "road"));

    private final boolean[]             done             = { false };

    public WorldGenerator()
    {
        MinecraftForge.EVENT_BUS.register(this);
        MinecraftForge.TERRAIN_GEN_BUS.register(this);
        instance = this;
    }

    @SubscribeEvent
    public void registerI(RegistryEvent.Register<Item> event)
    {
        if (roadBlock)
        {
            ItemBlock road = new ItemBlock(roadSurface);
            road.setRegistryName(roadSurface.getRegistryName());
            event.getRegistry().register(road);
        }
    }

    @SideOnly(Side.CLIENT)
    @SubscribeEvent(priority = EventPriority.LOW)
    public void registerIT(RegistryEvent.Register<Item> event)
    {
        if (roadBlock)
        {
            Item item2 = Item.getItemFromBlock(roadSurface);
            ModelLoader.setCustomModelResourceLocation(item2, 0,
                    new ModelResourceLocation(MODID + ":road", "inventory"));
        }
    }

    @SubscribeEvent
    public void registerB(RegistryEvent.Register<Block> event)
    {
        if (roadBlock) event.getRegistry().register(roadSurface);
        else roadSurface = Blocks.GRASS_PATH;
    }

    @EventHandler
    public void preInit(FMLPreInitializationEvent e)
    {
        new Config(e);
        File file = e.getSuggestedConfigurationFile();
        String seperator = System.getProperty("file.separator");

        String folder = file.getAbsolutePath();
        String name = file.getName();
        FileLoader.biomes = folder.replace(name, MODID + seperator + "biomes.csv");
        //
        MapGenStructureIO.registerStructure(Start.class, "dorfsitestart");

        Thread dorfProcess = new Thread(new Runnable()
        {

            @Override
            public void run()
            {
                new FileLoader();
                dorfs.init();
                structureGen.init();
                done[0] = true;
            }
        });
        dorfProcess.setName("dorfgen image processor");
        dorfProcess.start();
        //
    }

    @EventHandler
    public void load(FMLInitializationEvent evt)
    {
        finiteWorldType = new WorldTypeFinite("finite");
    }

    @Optional.Method(modid = "cubicchunks")
    @EventHandler
    public void loadCC(FMLInitializationEvent evt)
    {
        new WorldTypeCubic("cubic_finite");
    }

    @EventHandler
    public void postInit(FMLPostInitializationEvent e)
    {
    }

    @EventHandler
    public void serverLoad(FMLServerStartingEvent event)
    {
        event.registerServerCommand(new Commands());
    }

    @SubscribeEvent
    public void genEvent(Load evt)
    {
        System.out.println("load");

        // TODO replace sigmoid accordingly here.
        // if (true) return;
        if (evt.getWorld().getBiomeProvider() instanceof BiomeProviderFinite)
        {
            int scale = ((BiomeProviderFinite) evt.getWorld().getBiomeProvider()).scale;
            if (!spawnSite.isEmpty())
            {
                ArrayList<Site> sites = new ArrayList<Site>(DorfMap.sitesById.values());
                for (Site s : sites)
                {
                    if (s.name.equalsIgnoreCase(spawnSite))
                    {
                        int x = s.x * scale;
                        int y = 0;
                        int z = s.z * scale;
                        try
                        {
                            y = dorfs.elevationMap[(x - shift.getX()) / scale][(z - shift.getZ()) / scale];
                        }
                        catch (Exception e)
                        {
                            System.out.println(s + " " + dorfs.elevationMap.length);
                            e.printStackTrace();
                        }
                        evt.getWorld().setSpawnPoint(new BlockPos(x + scale / 2, y, z + scale / 2));
                        return;
                    }
                }
            }
            if (randomSpawn)
            {
                ArrayList<Site> sites = new ArrayList<Site>(DorfMap.sitesById.values());

                Collections.shuffle(sites, evt.getWorld().rand);

                for (Site s : sites)
                {
                    if (s.type.isVillage() && s.type != SiteType.HIPPYHUTS)
                    {
                        int x = s.x * scale;
                        int y = 0;
                        int z = s.z * scale;
                        try
                        {
                            y = dorfs.elevationMap[(x - shift.getX()) / scale][(z - shift.getZ()) / scale];
                        }
                        catch (Exception e)
                        {
                            System.out.println(s + " " + dorfs.elevationMap.length);
                            e.printStackTrace();
                        }
                        evt.getWorld().setSpawnPoint(new BlockPos(x + scale / 2, y, z + scale / 2));
                        return;
                    }
                }
            }
            else
            {
                evt.getWorld().setSpawnPoint(spawn);
            }
        }
    }

    @EventHandler
    public void LoadComplete(FMLLoadCompleteEvent event)
    {
        while (!done[0])
        {
            try
            {
                Thread.sleep(100);
            }
            catch (InterruptedException e)
            {
                e.printStackTrace();
            }
        }
    }

    @SubscribeEvent
    public void decorate(Decorate event)
    {
        if (event.getWorld().getBiomeProvider() instanceof BiomeProviderFinite)
        {
            int scale = ((BiomeProviderFinite) event.getWorld().getBiomeProvider()).scale;
            Collection<Site> sites = dorfs.getSiteForCoords(event.getPos().getX(), event.getPos().getZ());
            if (sites != null && event.getType() == EventType.TREE)
            {
                for (Site site : sites)
                {
                    if (site != null && site.rgbmap != null)
                    {
                        for (int x = event.getPos().getX(); x < event.getPos().getX() + 16; x++)
                            for (int z = event.getPos().getZ(); z < event.getPos().getZ() + 16; z++)
                            {
                                int width = (scale / SiteStructureGenerator.SITETOBLOCK);
                                int pixelX = (x - site.corners[0][0] * scale - scale / 2 - width / 2) / width;
                                int pixelY = (z - site.corners[0][1] * scale - scale / 2 - width / 2) / width;
                                if (pixelX >= site.rgbmap.length || pixelY >= site.rgbmap[0].length)
                                {
                                    continue;
                                }
                                if (SiteMapColours.getMatch(site.rgbmap[pixelX][pixelY]) != SiteMapColours.GENERIC)
                                {
                                    event.setResult(Result.DENY);
                                    return;
                                }
                            }
                    }
                }
            }
        }
    }

}
