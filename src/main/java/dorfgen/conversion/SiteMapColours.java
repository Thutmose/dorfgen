package dorfgen.conversion;

import java.awt.Color;
import java.util.HashMap;

import dorfgen.WorldGenerator;
import net.minecraft.block.BlockDirt;
import net.minecraft.block.BlockDirt.DirtType;
import net.minecraft.block.state.IBlockState;
import net.minecraft.init.Blocks;

public enum SiteMapColours
{
    GENERIC(50, 80, 15),

    // Farms
    LIGHTYELLOWFARM(195, 255, 0), LIGHTYELLOWFARMSPACER(97, 127, 0), YELLOWFARM(255, 195, 0), YELLOWFARMSPACER(127, 97,
            0), BROWNFARM(100, 60, 20), BROWNFARMSPACER(50, 30, 10), GREENFARM(0, 50, 0), GREENFARMSPACER(0, 100, 0),

    // Misc Terrain
    RIVEREDGE(160, 120, 50), RIVER(20, 20, 80), LIMEAREA(100, 255, 0), BRIGHTGREENPASTURE(0, 255, 0), GREENPASTURE(0,
            95, 0), DARKGREENAREA(0, 20, 0),

    // Structures
    ROAD(50, 30, 15),

    // roofs
    GREENROOF(30, 100, 10), // Roof on some light brown wall buildings
    BROWNROOF(100, 70, 10), // Roof on some other light bown buildings, smaller
                            // ones
    DARKBROWNROOF(75, 50, 20), DARKBROWNROOF2(50, 20, 20), LIGHTBROWNROOF(170, 120, 30), LIGHTYELLOWROOF(255, 255,
            100), DULLGREENROOF(128, 192, 128), REDROOF(255, 0,
                    0), LIGHTCYANROOF(128, 255, 255), LIGHTORANGEROOF(255, 128, 64), LIGHTGREYROOF(192, 192, 192),

    // Strucure Walls
    LIGHTBROWNBUILDINGWALL(170, 120, 30), TOWNBUILDINGWHITEWALL(255, 255, 255), DARKGREYBUILDINGWALL(50, 50,
            50), GREYBUILDINGWALL(128, 128, 128), YELLOWBUILDINGWALL(255, 255, 0), BROWNBUILDINGWALL(60, 40, 20),

    // Town/Keep Walls
    TOWNWALL(70, 70, 70), TOWNWALLMID(40, 40, 40), TOWERWALL(55, 55, 55), TOWERROOF(110, 110, 110),;

    private static HashMap<Integer, SiteMapColours> colourMap = new HashMap<Integer, SiteMapColours>();
    public static boolean                           init      = false;
    public final Color                              colour;

    SiteMapColours(int red, int green, int blue)
    {
        colour = new Color(red, green, blue);
    }

    public boolean matches(int rgb)
    {
        return colour.getRGB() == rgb;
    }

    public static SiteMapColours getMatch(int rgb)
    {
        if (!init)
        {
            init = true;
            colourMap.clear();
            for (SiteMapColours t : values())
            {
                colourMap.put(t.colour.getRGB(), t);
            }
        }
        return colourMap.get(rgb);
    }

    public static IBlockState[] getSurfaceBlocks(SiteMapColours point)
    {
        IBlockState[] ret = new IBlockState[3];
        ret[0] = Blocks.DIRT.getDefaultState();

        if (point == ROAD)
        {
            ret[0] = Blocks.COBBLESTONE.getDefaultState();
            ret[1] = WorldGenerator.roadSurface.getDefaultState();
        }
        if (point == LIGHTYELLOWFARM)
        {
            ret[1] = Blocks.FARMLAND.getDefaultState();
            ret[2] = Blocks.CARROTS.getDefaultState();
        }
        if (point == YELLOWFARM)
        {
            ret[1] = Blocks.FARMLAND.getDefaultState();
            ret[2] = Blocks.POTATOES.getDefaultState();
        }
        if (point == BROWNFARM)
        {
            ret[1] = Blocks.FARMLAND.getDefaultState();
            ret[2] = Blocks.WHEAT.getDefaultState();
        }
        if (point == BROWNFARMSPACER)
        {
            ret[0] = Blocks.WATER.getDefaultState();
            ret[1] = Blocks.DIRT.getDefaultState().withProperty(BlockDirt.VARIANT, DirtType.PODZOL);
        }
        if (point == YELLOWFARMSPACER)
        {
            ret[0] = Blocks.WATER.getDefaultState();
            ret[1] = Blocks.DIRT.getDefaultState().withProperty(BlockDirt.VARIANT, DirtType.PODZOL);
        }
        if (point == LIGHTYELLOWFARMSPACER)
        {
            ret[0] = Blocks.WATER.getDefaultState();
            ret[1] = Blocks.DIRT.getDefaultState().withProperty(BlockDirt.VARIANT, DirtType.PODZOL);
        }

        if (point == RIVER)
        {
            ret[0] = Blocks.WATER.getDefaultState();
            ret[1] = Blocks.WATER.getDefaultState();
        }
        // if(point==SiteMapColours.LIGHTBROWNBUILDINGWALL)
        // {
        // ret[0] = Blocks.cobblestone;
        // ret[1] = Blocks.stained_hardened_clay;
        // }
        // if(point==DARKGREYBUILDINGWALL || point == GREYBUILDINGWALL)
        // {
        // ret[0] = Blocks.cobblestone;
        // ret[1] = Blocks.stained_hardened_clay;
        // }
        // if(point==SiteMapColours.TOWNBUILDINGWHITEWALL)
        // {
        // ret[0] = Blocks.cobblestone;
        // ret[1] = Blocks.stained_hardened_clay;
        // }
        // if(point==DARKBROWNROOF || point==GREENROOF || point == BROWNROOF)
        // {
        // ret[0] = Blocks.cobblestone;
        // ret[1] = Blocks.stained_hardened_clay;
        // ret[2] = Blocks.stained_hardened_clay;
        // }
        // if(point==TOWNWALL || point==TOWNWALLMID || point == TOWERWALL ||
        // point == SiteMapColours.TOWERROOF)
        // {
        // ret[0] = Blocks.stonebrick;
        // ret[1] = Blocks.stonebrick;
        // ret[2] = Blocks.stonebrick;
        // }

        return ret;
    }
}
