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
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.util.ChatComponentText;
import net.minecraft.util.MathHelper;
import net.minecraft.world.World;
import net.minecraft.world.biome.BiomeGenBase;

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
    	if(world.isRemote)
    		return itemstack;
    	DorfMap dorfs = WorldGenerator.instance.dorfs;
    	int x = MathHelper.floor_double(player.posX);
    	int z = MathHelper.floor_double(player.posZ);
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
    	
    	for(Site s: dorfs.sitesById.values())
    	{
    		System.out.println(s);
    	}
    	
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
    	
    	x = MathHelper.floor_double(player.posX)/WorldGenerator.scale;
    	z = MathHelper.floor_double(player.posZ)/WorldGenerator.scale;
    	
    	CachedBicubicInterpolator heightInterpolator = new CachedBicubicInterpolator();
    	BicubicInterpolator			biomeInterpolator	= new BicubicInterpolator();
    	heightInterpolator.updateCoefficients(dorfs.temperatureMap, x, z);
    	int t1 = heightInterpolator.interpolateHeight(8, x, z, 
    			MathHelper.floor_double(player.posX)%8, MathHelper.floor_double(player.posZ)%8, 
    			dorfs.temperatureMap);
    	int v = dorfs.vegitationMap[x][z];
    	mess = t1+" "+v;
    	
    	int n1 = biomeInterpolator.interpolateBiome(8, x, z, 
    			MathHelper.floor_double(player.posX)%8, MathHelper.floor_double(player.posZ)%8, 
    			dorfs.biomeMap);

    	int n2 = BiomeList.getBiomeFromValues(n1, t1, 128, 128, 128, region);
    	
    	mess += " "+BiomeGenBase.getBiome(n1).biomeName+" "+BiomeGenBase.getBiome(n1).theBiomeDecorator.treesPerChunk;
    	player.addChatMessage(new ChatComponentText(mess));
    	
    	return itemstack;
    }
}
