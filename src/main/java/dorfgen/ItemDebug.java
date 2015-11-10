package dorfgen;

import static dorfgen.WorldGenerator.scale;
import static net.minecraft.util.EnumFacing.EAST;
import static net.minecraft.util.EnumFacing.NORTH;
import static net.minecraft.util.EnumFacing.SOUTH;
import static net.minecraft.util.EnumFacing.WEST;

import java.awt.Color;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Random;
import java.util.Set;

import dorfgen.conversion.BiomeList;
import dorfgen.conversion.DorfMap;
import dorfgen.conversion.FileLoader;
import dorfgen.conversion.SiteMapColours;
import dorfgen.conversion.SiteTerrain;
import dorfgen.conversion.DorfMap.Region;
import dorfgen.conversion.DorfMap.Site;
import dorfgen.conversion.DorfMap.SiteType;
import dorfgen.conversion.DorfMap.WorldConstruction;
import dorfgen.conversion.Interpolator.BicubicInterpolator;
import dorfgen.conversion.Interpolator.CachedBicubicInterpolator;
import dorfgen.conversion.SiteStructureGenerator.RoadExit;
import dorfgen.conversion.SiteStructureGenerator.SiteStructures;
import dorfgen.conversion.SiteStructureGenerator.StructureSpace;
import dorfgen.worldgen.RiverMaker;
import dorfgen.worldgen.WorldConstructionMaker;
import net.minecraft.block.Block;
import net.minecraft.creativetab.CreativeTabs;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.init.Blocks;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.util.ChatComponentText;
import net.minecraft.util.EnumFacing;
import net.minecraft.util.MathHelper;
import net.minecraft.world.ChunkCoordIntPair;
import net.minecraft.world.World;
import net.minecraft.world.biome.BiomeGenBase;
import net.minecraft.world.chunk.Chunk;
import net.minecraft.world.gen.ChunkProviderServer;

public class ItemDebug extends Item {

	public ItemDebug() {
		super();
		this.setUnlocalizedName("debug");
		this.setCreativeTab(CreativeTabs.tabTools);
	}

	@Override
	public ItemStack onItemRightClick(ItemStack itemstack, World world, EntityPlayer player) {

		int x = MathHelper.floor_double(player.posX);
		int y = (int) player.posY;
		int z = MathHelper.floor_double(player.posZ);

		if (world.isRemote)
			return itemstack;

		DorfMap dorfs = WorldGenerator.instance.dorfs;
		int n = 0;
		Region region = dorfs.getRegionForCoords(x, z);
		HashSet<Site> sites = dorfs.getSiteForCoords(x, z);
		String mess = "";
		if (region != null) {
			mess += region.name + " " + region.type + " ";
		}
		WorldConstructionMaker maker = new WorldConstructionMaker();

		int h = maker.bicubicInterpolator.interpolate(WorldGenerator.instance.dorfs.elevationMap, x, z, scale);
		int r = maker.bicubicInterpolator.interpolate(WorldGenerator.instance.dorfs.riverMap, x, z, scale);
		mess = "";
//		
		int embarkX = (x/scale)*scale;
		int embarkZ = (z/scale)*scale;
		Site site = null;
		if(sites!=null)
		{
			for(Site s: sites)
			{
				site = s;
				mess += site;
				mess += site.isInSite(x, z);
			}
		}
		
    	int kx = x/(scale);
    	int kz = z/(scale);
    	int key = kx + 8192 * kz;
    	
    	HashSet<Site> ret = dorfs.sitesByCoord.get(key);
    	
		if(site!=null && site.rgbmap != null)
		{
			SiteStructures stuff = WorldGenerator.instance.structureGen.getStructuresForSite(site);
			for(RoadExit exit: stuff.roads)
			{
				System.out.println(Arrays.toString(exit.getEdgeMid(site, scale)));
			}
			StructureSpace struct = stuff.getStructure(x, z, scale);
			if(struct!=null)
			{
				int[] mid = struct.getMid(site, scale);
				
				
				
				System.out.println(struct.shouldBeDoor(site, x, z, scale)+" "+Arrays.toString(mid));
			}
			int shiftX = (x - site.corners[0][0]*scale)*51/scale;
			int shiftZ = (z - site.corners[0][1]*scale)*51/scale;
			
			int rgb = site.rgbmap[shiftX][shiftZ];
			
			
			
			SiteMapColours col = SiteMapColours.getMatch(rgb);
			System.out.println(col);
			
		}
		player.addChatMessage(new ChatComponentText(mess));

		return itemstack;
	}
}
