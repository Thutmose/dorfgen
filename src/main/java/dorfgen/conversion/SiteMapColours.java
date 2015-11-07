package dorfgen.conversion;

import java.awt.Color;
import java.util.HashMap;

import dorfgen.BlockRoadSurface;
import net.minecraft.block.Block;
import net.minecraft.init.Blocks;

public enum SiteMapColours
{
	GENERIC(50,80,15),
	
	LIGHTYELLOWFARM(195, 255, 0),
	LIGHTYELLOWFARMSPACER(97,127,0),
	YELLOWFARM(255, 195, 0),
	YELLOWFARMSPACER(127,97,0),
	BROWNFARM(100,60,20),
	BROWNFARMSPACER(50,30,10),
	
	ROAD(50,30,15),
	RIVEREDGE(160,120,50),
	RIVER(20,20,80),
	LIMEAREA(100,255,0),
	BRIGHTGREENPASTURE(0,255,0),
	GREENPASTURE(0,95,0),
	DARKGREENAREA(0,20,0),
	
	LIGHTBROWNBUILDINGWALL(170, 120, 30),
	GREENROOF(30,100,10),//Roof on some light brown wall buildings
	BROWNROOF(100,70,10),//Roof on some other light bown buildings, smaller ones
	DARKGREYBUILDINGWALL(50,50,50),
	DARKBROWNROOF(75, 50, 20),
	
	WALL(70,70,70),
	WALLMID(40,40,40),
	TOWERWALL(55,55,55),
	TOWERROOF(110,110,110),
	
	TOWNBUILDINGWHITEWALL(255,255,255),
	;

	private static HashMap<Integer, SiteMapColours> colourMap = new HashMap();
	public static boolean init = false;
	public final Color colour;
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
		if(!init)
		{
			init = true;
			colourMap.clear();
			for(SiteMapColours t: values())
			{
				colourMap.put(t.colour.getRGB(), t);
			}
		}
		return colourMap.get(rgb);
	}
	
	public static Block[] getSurfaceBlocks(SiteMapColours point)
	{
		Block[] ret = new Block[3];
		ret[0] = Blocks.dirt;
		
		if(point==ROAD)
		{
			ret[0] = Blocks.cobblestone;
			ret[1] = BlockRoadSurface.uggrass;
		}
		if(point==LIGHTYELLOWFARM)
		{
			ret[1] = Blocks.farmland;
			ret[2] = Blocks.carrots;
		}
		if(point==LIGHTYELLOWFARMSPACER)
		{
			ret[0] = Blocks.water;
			ret[1] = Blocks.dirt;
		}
		if(point==YELLOWFARM)
		{
			ret[1] = Blocks.farmland;
			ret[2] = Blocks.potatoes;
		}
		if(point==YELLOWFARMSPACER)
		{
			ret[0] = Blocks.water;
			ret[1] = Blocks.dirt;
		}
		if(point==BROWNFARM)
		{
			ret[1] = Blocks.farmland;;
			ret[2] = Blocks.wheat;
		}
		if(point==BROWNFARMSPACER)
		{
			ret[0] = Blocks.water;
			ret[1] = Blocks.dirt;
		}
		if(point==SiteMapColours.LIGHTBROWNBUILDINGWALL)
		{
			ret[0] = Blocks.cobblestone;
			ret[1] = Blocks.planks;
			ret[2] = Blocks.planks;
		}
		if(point==DARKGREYBUILDINGWALL)
		{
			ret[0] = Blocks.cobblestone;
			ret[1] = Blocks.stonebrick;
			ret[2] = Blocks.stonebrick;
		}
		if(point==SiteMapColours.TOWNBUILDINGWHITEWALL)
		{
			ret[0] = Blocks.cobblestone;
			ret[1] = Blocks.stained_hardened_clay;
			ret[2] = Blocks.stained_hardened_clay;
		}
		if(point==DARKBROWNROOF || point==GREENROOF || point == BROWNROOF)
		{
			ret[1] = Blocks.planks;
		}
		if(point==WALL || point==WALLMID || point == TOWERWALL || point == SiteMapColours.TOWERROOF)
		{
			ret[0] = Blocks.stonebrick;
			ret[1] = Blocks.stonebrick;
			ret[2] = Blocks.stonebrick;
		}
		if(point==RIVER)
		{
			ret[0] = Blocks.water;
			ret[1] = Blocks.water;
		}
		
		return ret;
	}
}
