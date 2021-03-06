package dorfgen;

import java.io.File;
import java.io.IOException;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.text.SimpleDateFormat;
import java.util.List;
import java.util.Map;
import java.util.logging.FileHandler;
import java.util.logging.Formatter;
import java.util.logging.Level;
import java.util.logging.LogRecord;
import java.util.logging.Logger;

import com.google.common.collect.Lists;
import com.google.common.collect.Maps;

import dorfgen.conversion.Config;
import dorfgen.conversion.DorfMap;
import dorfgen.conversion.FileLoader;
import dorfgen.conversion.SiteStructureGenerator;
import dorfgen.worldgen.common.IDorfgenProvider;
import dorfgen.worldgen.common.MapGenSites.Start;
import dorfgen.worldgen.structures.village.VillageWrapper;
import dorfgen.worldgen.vanilla.WorldTypeFinite;
import io.github.opencubicchunks.cubicchunks.api.world.ICubicWorldServer;
import net.minecraft.block.Block;
import net.minecraft.util.ResourceLocation;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;
import net.minecraft.world.WorldServer;
import net.minecraft.world.WorldType;
import net.minecraft.world.gen.structure.MapGenStructureIO;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.common.Mod.EventHandler;
import net.minecraftforge.fml.common.Optional;
import net.minecraftforge.fml.common.event.FMLInitializationEvent;
import net.minecraftforge.fml.common.event.FMLLoadCompleteEvent;
import net.minecraftforge.fml.common.event.FMLPostInitializationEvent;
import net.minecraftforge.fml.common.event.FMLPreInitializationEvent;
import net.minecraftforge.fml.common.event.FMLServerAboutToStartEvent;
import net.minecraftforge.fml.common.event.FMLServerStartingEvent;

@Mod(modid = WorldGenerator.MODID, name = WorldGenerator.NAME, version = Reference.VERSION, acceptableRemoteVersions = "*")
public class WorldGenerator
{

    public static final String   MODID         = Reference.MOD_ID;
    public static final String   NAME          = Reference.MOD_NAME;

    @Mod.Instance(MODID)
    public static WorldGenerator instance;

    private Map<String, DorfMap> regionMaps    = Maps.newConcurrentMap();
    public String                defaultRegion = "";

    public static boolean        roadBlock     = true;

    public WorldType             finiteWorldType;

    public static String         configLocation;
    public static String         biomes;

    public static Block          roadSurface   = new BlockRoadSurface()
            .setRegistryName(new ResourceLocation(MODID, "road"));
    public static int            scale;
    public static int            cubicHeightScale;
    public static boolean        finite;
    public static boolean        randomSpawn;
    public static BlockPos       spawn;
    public static String         spawnSite;
    public static BlockPos       shift         = BlockPos.ORIGIN;
    public static IGenGetter     getter        = new IGenGetter()
                                               {
                                               };

    public static interface IGenGetter
    {
        default IDorfgenProvider getProvider(World entityWorld)
        {
            if (entityWorld instanceof WorldServer && ((WorldServer) entityWorld)
                    .getChunkProvider().chunkGenerator instanceof IDorfgenProvider) { return (IDorfgenProvider) ((WorldServer) entityWorld)
                            .getChunkProvider().chunkGenerator; }
            return null;
        }
    }

    public static IDorfgenProvider getProvider(World entityWorld)
    {
        return getter.getProvider(entityWorld);
    }

    public static DorfMap getDorfMap(String key)
    {
        DorfMap map = instance.regionMaps.get(key);
        if (map == null)
        {
            map = instance.regionMaps.get(instance.defaultRegion);
            instance.regionMaps.put(key, map);
            if (map.structureGen == null) map.structureGen = new SiteStructureGenerator(map);
        }
        return map;
    }

    public static SiteStructureGenerator getStructureGen(String key)
    {
        return getDorfMap(key).structureGen;
    }

    public static void setMap(String key, DorfMap map)
    {
        if (instance.regionMaps.isEmpty())
        {
            instance.defaultRegion = key;
        }
        log("Set Map for " + key);
        instance.regionMaps.put(key, map);

    }

    List<Thread> mapThreads = Lists.newArrayList();

    public WorldGenerator()
    {
        MinecraftForge.EVENT_BUS.register(new EventListener());
        MinecraftForge.TERRAIN_GEN_BUS.register(new EventListener());
        instance = this;
    }

    @EventHandler
    public void preInit(FMLPreInitializationEvent e)
    {
        new Config(e);
        File file = e.getSuggestedConfigurationFile();
        String seperator = System.getProperty("file.separator");

        String folder = file.getAbsolutePath();
        String name = file.getName();
        File dorfgenFolder = new File(folder.replace(name, MODID + seperator));
        dorfgenFolder.mkdirs();

        //
        MapGenStructureIO.registerStructure(Start.class, "dorfsitestart");

        for (File subDir : dorfgenFolder.listFiles())
        {
            if (!subDir.isDirectory()) continue;
            Thread dorfProcess = new Thread(new Runnable()
            {
                @Override
                public void run()
                {
                    new FileLoader(subDir);
                }
            });
            dorfProcess.setName("dorfgen image processor");
            dorfProcess.start();
            mapThreads.add(dorfProcess);
        }
        if (mapThreads.isEmpty())
        {
            log(Level.SEVERE, "No Maps found.");
        }

        //
    }

    @EventHandler
    public void load(FMLInitializationEvent evt)
    {
        finiteWorldType = new WorldTypeFinite("dorfgen");
    }

    @Optional.Method(modid = "cubicchunks")
    @EventHandler
    public void loadCC(FMLInitializationEvent evt)
    {
        new dorfgen.worldgen.cubic.WorldTypeCubic("dorfgen_cubic");
        getter = new IGenGetter()
        {
            @Override
            public IDorfgenProvider getProvider(World entityWorld)
            {
                if (entityWorld instanceof ICubicWorldServer)
                {
                    ICubicWorldServer world = (ICubicWorldServer) entityWorld;
                    if (world.isCubicWorld())
                    {
                        if (world.getCubeGenerator() instanceof IDorfgenProvider)
                            return (IDorfgenProvider) world.getCubeGenerator();
                    }
                }
                return IGenGetter.super.getProvider(entityWorld);
            }
        };
    }

    @EventHandler
    public void postInit(FMLPostInitializationEvent e)
    {
        VillageWrapper.postInitVillageWrapper();
    }

    @EventHandler
    public void serverLoad(FMLServerAboutToStartEvent event)
    {
        // Reset all of the biome lists on server load. This is to account for
        // worlds with different biome sets.
        for (DorfMap dorf : regionMaps.values())
        {
            dorf.biomeList.clear();
        }
    }

    @EventHandler
    public void serverLoad(FMLServerStartingEvent event)
    {
        event.registerServerCommand(new Commands());
    }

    @EventHandler
    public void LoadComplete(FMLLoadCompleteEvent event)
    {
        boolean done = false;
        while (!done)
        {
            done = true;
            for (Thread t : mapThreads)
            {
                done = done && !t.isAlive();
            }
            if (done) break;
            try
            {
                Thread.sleep(100);
            }
            catch (InterruptedException e)
            {
                e.printStackTrace();
            }
        }
        mapThreads.clear();
    }

    private static Logger        logger     = Logger.getLogger("Dorfgen");
    protected static FileHandler logHandler = null;

    private static void initLogger()
    {
        logger.setLevel(Level.ALL);
        try
        {
            File logfile = new File(".", "dorfgen.log");
            if ((logfile.exists() || logfile.createNewFile()) && logfile.canWrite() && logHandler == null)
            {
                logHandler = new FileHandler(logfile.getPath());
                logHandler.setFormatter(new LogFormatter());
                logger.addHandler(logHandler);
            }
        }
        catch (SecurityException | IOException e)
        {
            e.printStackTrace();
        }
    }

    public static void log(String toLog)
    {
        log(Level.INFO, toLog);
    }

    public static void log(Level level, String toLog)
    {
        if (logHandler == null) initLogger();
        logger.log(level, toLog);
    }

    public static void log(Level level, String toLog, Exception thrown)
    {
        if (logHandler == null) initLogger();
        logger.log(level, toLog, thrown);
    }

    public static void log(String toLog, Exception thrown)
    {
        log(Level.WARNING, toLog, thrown);
    }

    public static final class LogFormatter extends Formatter
    {
        private static final String SEP        = System.getProperty("line.separator");
        private SimpleDateFormat    dateFormat = new SimpleDateFormat("MM-dd HH:mm:ss");

        @Override
        public String format(LogRecord record)
        {
            StringBuilder sb = new StringBuilder();

            sb.append(dateFormat.format(record.getMillis()));
            sb.append(" [").append(record.getLevel().getLocalizedName()).append("] ");

            sb.append(record.getMessage());
            sb.append(SEP);
            Throwable thr = record.getThrown();

            if (thr != null)
            {
                StringWriter thrDump = new StringWriter();
                thr.printStackTrace(new PrintWriter(thrDump));
                sb.append(thrDump.toString());
            }

            return sb.toString();
        }
    }
}
