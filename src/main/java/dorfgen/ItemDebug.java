package dorfgen;

import static dorfgen.WorldGenerator.scale;
import static net.minecraft.util.EnumFacing.EAST;
import static net.minecraft.util.EnumFacing.NORTH;
import static net.minecraft.util.EnumFacing.SOUTH;
import static net.minecraft.util.EnumFacing.WEST;

import java.util.Arrays;
import java.util.HashSet;

import dorfgen.conversion.BiomeList;
import dorfgen.conversion.DorfMap;
import dorfgen.conversion.FileLoader;
import dorfgen.conversion.DorfMap.Region;
import dorfgen.conversion.DorfMap.Site;
import dorfgen.conversion.DorfMap.SiteType;
import dorfgen.conversion.DorfMap.WorldConstruction;
import dorfgen.conversion.Interpolator.BicubicInterpolator;
import dorfgen.conversion.Interpolator.CachedBicubicInterpolator;
import dorfgen.worldgen.RiverMaker;
import dorfgen.worldgen.WorldConstructionMaker;
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
		Site site = dorfs.getSiteForCoords(x, z);
		String mess = "";
		if (region != null) {
			mess += region.name + " " + region.type + " ";
		}
		if (site != null) {
			mess += site.name + " " + site.type + " " + site.id;

			int x1 = site.x * 16 * scale + 16 * scale / 2;
			int z1 = site.z * 16 * scale + 16 * scale / 2;
		}
		
		for(Site s: WorldGenerator.instance.dorfs.sitesById.values())
		{
			if(s.name.contains("drums"))
				System.out.println(s);
		}
		
		
//		WorldGenerator.spawn.posX = 2629;
//		WorldGenerator.spawn.posY = 100;
//		WorldGenerator.spawn.posZ = 2502;
		
		
		WorldConstructionMaker maker = new WorldConstructionMaker();
//		int chunkX = x/16;
//		int chunkZ = z/16;
		HashSet<WorldConstruction> constructs;
		mess += " "+(constructs = dorfs.getConstructionsForCoords(x, z));
		
	//	EnumFacing[] dirs = maker.getRoadDirection(x, z);
		EnumFacing[] dirs = { EAST, WEST, NORTH, SOUTH };
		EnumFacing[] ret = maker.getRoadDirection(x, z);

		
//		for(WorldConstruction con: constructs)
//		{
//			if(!con.isInConstruct(x, 0, z))
//				continue;
//			if(con.isInConstruct(x - scale, 0, z))
//			{
//				ret[1] = dirs[1];
//			}
//			if(con.isInConstruct(x + scale, 0, z))
//			{
//				ret[0] = dirs[0];
//			}
//			if(con.isInConstruct(x, 0, z - scale))
//			{
//				ret[2] = dirs[2];
//			}
//			if(con.isInConstruct(x, 0, z + scale))
//			{
//				ret[3] = dirs[3];
//			}
//		}
		
		System.out.println(Arrays.toString(ret)+" "+maker.shouldRoadPlace(x, z, ret, 4));
		
		
		
		if (!mess.isEmpty())
			player.addChatMessage(new ChatComponentText(mess));

		return itemstack;
	}
}
