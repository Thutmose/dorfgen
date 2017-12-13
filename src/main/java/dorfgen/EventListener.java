package dorfgen;

import static dorfgen.WorldGenerator.MODID;
import static dorfgen.WorldGenerator.log;
import static dorfgen.WorldGenerator.roadBlock;
import static dorfgen.WorldGenerator.roadSurface;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;

import dorfgen.conversion.DorfMap;
import dorfgen.conversion.DorfMap.Site;
import dorfgen.conversion.DorfMap.SiteType;
import dorfgen.conversion.SiteMapColours;
import dorfgen.conversion.SiteStructureGenerator;
import dorfgen.worldgen.common.BiomeProviderFinite;
import net.minecraft.block.Block;
import net.minecraft.client.renderer.block.model.ModelResourceLocation;
import net.minecraft.init.Blocks;
import net.minecraft.item.Item;
import net.minecraft.item.ItemBlock;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.border.WorldBorder;
import net.minecraftforge.client.model.ModelLoader;
import net.minecraftforge.event.RegistryEvent;
import net.minecraftforge.event.terraingen.DecorateBiomeEvent.Decorate;
import net.minecraftforge.event.terraingen.DecorateBiomeEvent.Decorate.EventType;
import net.minecraftforge.event.world.WorldEvent.Load;
import net.minecraftforge.fml.common.eventhandler.Event.Result;
import net.minecraftforge.fml.common.eventhandler.EventPriority;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;

public class EventListener
{

    public EventListener()
    {
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

    @SubscribeEvent
    public void genEvent(Load evt)
    {
        if (evt.getWorld().getBiomeProvider() instanceof BiomeProviderFinite)
        {
            int scale = ((BiomeProviderFinite) evt.getWorld().getBiomeProvider()).scale;
            DorfMap dorfs = ((BiomeProviderFinite) evt.getWorld().getBiomeProvider()).map;

            // Setup a world border of the maximum bounds of the map.
            WorldBorder dorfBorder = evt.getWorld().getWorldBorder();
            int xMax = dorfs.scale * dorfs.elevationMap.length;
            int zMax = dorfs.scale * dorfs.elevationMap[0].length;
            dorfBorder.setSize(Math.max(xMax, zMax) / 2 + 16);
            dorfBorder.setCenter(dorfs.shiftX(0) + xMax / 2, dorfs.shiftZ(0) + zMax / 2);
            log("set world border " + dorfBorder.maxX() + " " + dorfBorder.minX() + " " + dorfBorder.maxZ() + " "
                    + dorfBorder.minZ() + " " + dorfs.shiftX(0) + " " + dorfs.shiftZ(0));

            if (dorfs.randomSpawn)
            {
                ArrayList<Site> sites = new ArrayList<Site>(dorfs.sitesById.values());

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
                            y = dorfs.elevationMap[x / scale][z / scale];
                        }
                        catch (Exception e)
                        {
                            log(s + " " + dorfs.elevationMap.length, e);
                        }
                        evt.getWorld().setSpawnPoint(new BlockPos(x + scale / 2, y, z + scale / 2));
                        return;
                    }
                }
            }
            else if (!dorfs.spawnSite.isEmpty())
            {
                ArrayList<Site> sites = new ArrayList<Site>(dorfs.sitesById.values());
                for (Site s : sites)
                {
                    if (s.name.equalsIgnoreCase(dorfs.spawnSite))
                    {
                        int x = s.x * scale;
                        int y = 0;
                        int z = s.z * scale;
                        try
                        {
                            y = dorfs.elevationMap[x / scale][z / scale];
                        }
                        catch (Exception e)
                        {
                            log(s + " " + dorfs.elevationMap.length, e);
                        }
                        evt.getWorld().setSpawnPoint(
                                new BlockPos(dorfs.shiftX(x) + scale / 2, y, dorfs.shiftZ(z) + scale / 2));
                        return;
                    }
                }
            }
            else
            {
                evt.getWorld().setSpawnPoint(dorfs.spawn);
            }
        }
    }

    @SubscribeEvent
    public void decorate(Decorate event)
    {
        if (event.getWorld().getBiomeProvider() instanceof BiomeProviderFinite)
        {
            int scale = ((BiomeProviderFinite) event.getWorld().getBiomeProvider()).scale;
            DorfMap dorfs = ((BiomeProviderFinite) event.getWorld().getBiomeProvider()).map;
            int width = (scale / SiteStructureGenerator.SITETOBLOCK);
            if (width == 0) return;
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
                                int pixelX = (x - site.corners[0][0] * scale - scale / 2 - width / 2) / width;
                                int pixelY = (z - site.corners[0][1] * scale - scale / 2 - width / 2) / width;
                                if (pixelX >= site.rgbmap.length || pixelY >= site.rgbmap[0].length || pixelX < 0
                                        || pixelY < 0)
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
