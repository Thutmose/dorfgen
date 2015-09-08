package dorfgen;

import static dorfgen.WorldGenerator.scale;

import dorfgen.conversion.BiomeList;
import dorfgen.conversion.DorfMap;
import dorfgen.conversion.FileLoader;
import dorfgen.conversion.DorfMap.Region;
import dorfgen.conversion.DorfMap.Site;
import dorfgen.conversion.Interpolator.BicubicInterpolator;
import dorfgen.conversion.Interpolator.CachedBicubicInterpolator;
import net.minecraft.creativetab.CreativeTabs;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.init.Blocks;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.util.ChatComponentText;
import net.minecraft.util.MathHelper;
import net.minecraft.world.ChunkCoordIntPair;
import net.minecraft.world.World;
import net.minecraft.world.biome.BiomeGenBase;
import net.minecraft.world.chunk.Chunk;
import net.minecraft.world.gen.ChunkProviderServer;

public class ItemDebug extends Item
{

	public ItemDebug()
	{
		super();
		this.setUnlocalizedName("debug");
		this.setCreativeTab(CreativeTabs.tabTools);
	}
	
    @Override
    public ItemStack onItemRightClick(ItemStack itemstack, World world, EntityPlayer player)
    {

    	int x = MathHelper.floor_double(player.posX);
    	int y = (int) player.posY;
    	int z = MathHelper.floor_double(player.posZ);
//    	Chunk chunk;
//    	System.out.println((chunk = world.getChunkFromBlockCoords(x, z))+" "+world);
//    	
//    	try {
//        	System.out.println(world.getChunkProvider()+" "+chunk.getBlock(x&15, y, z&15)+" "+y);
//		} catch (Exception e) {
//			// TODO Auto-generated catch block
//			e.printStackTrace();
//		}
//		for(int i = (int) player.posY - 1; i<300; i++)
//    	try {
//			//chunk.func_150807_a(x&15, i, z&15, Blocks.gold_block, 0);
//		} catch (Exception e) {
//			// TODO Auto-generated catch block
//			e.printStackTrace();
//		}

    	if(world.isRemote)
    		return itemstack;
    	
    	DorfMap dorfs = WorldGenerator.instance.dorfs;
    	int n = 0;
    	Region region = dorfs.getRegionForCoords(x, z);
    	Site site = dorfs.getSiteForCoords(x, z);
    	String mess = "";
    	if(region!=null)
    	{
    		mess += region.name+" "+region.type+" ";
    	}
    	if(site!=null)
    	{
    		mess += site.name+" "+site.type;
    	}
    	
    	if(!mess.isEmpty())
    		player.addChatMessage(new ChatComponentText(mess));
    	
//    	for(Site s: dorfs.sitesById.values())
//    	{
//    		System.out.println(s);
//    	}
    	
//    	int x = MathHelper.floor_double(player.posX)/(scale * 16);
//    	int z = MathHelper.floor_double(player.posZ)/(scale * 16);
//    	
//    	String key = x+","+z;
//    	System.out.println(DorfMap.sitesByCoord.containsKey(key)+" "+key);
//    	if(DorfMap.sitesByCoord.containsKey(key))
//    	{
//    		String mess = DorfMap.sitesByCoord.get(key).name+" "+DorfMap.sitesByCoord.get(key).type;
//    		player.addChatMessage(new ChatComponentText(mess));
//    	}
//    	else
//    	{
//    		String mess = "nothing here";
//    		player.addChatMessage(new ChatComponentText(mess));
//    	}
    	
//    	x = MathHelper.floor_double(player.posX)/WorldGenerator.scale;
//    	z = MathHelper.floor_double(player.posZ)/WorldGenerator.scale;
    	
//    	CachedBicubicInterpolator heightInterpolator = new CachedBicubicInterpolator();
//    	BicubicInterpolator			biomeInterpolator	= new BicubicInterpolator();
//    	
//    	int t1 = heightInterpolator.interpolateHeight(WorldGenerator.scale, x, z, dorfs.elevationMap);
//    	int v = dorfs.vegitationMap[x/scale][z/scale];
//    	mess = t1+" "+v;
//    	player.addChatMessage(new ChatComponentText(mess));
    	
    	return itemstack;
    }
}
