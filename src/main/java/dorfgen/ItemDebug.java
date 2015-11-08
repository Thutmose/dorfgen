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
			}
		}
		
//		if(site!=null)
//		{
//			SiteStructures structures = new SiteStructures(site);
//			System.out.println(structures.structures.size());
//			
//			Set<String> recomplexStructureNames = StructureRegistry.INSTANCE.allStructureIDs();
//			
//			ArrayList<String> names = new ArrayList(recomplexStructureNames);
//			Collections.shuffle(names);
//			for(StructureSpace space: structures.structures)
//			{
//				//if(space.min[0] == 337 && space.min[1] == 608)
//				{
//					int[][] bounds = space.getBounds(site, scale);
//					int[] min = bounds[0];
//					int[] max = bounds[1];
//					
//					int[] bound = new int[2];
//					bound[0] = max[0]-min[0];
//					bound[1] = max[1]-min[1];
//					int[] box = new int[2];
//					StructureInfo structureInfo = null;
//					String structureName = null;
//					for(String name : names)
//					{
//						StructureInfo info = StructureRegistry.INSTANCE.getStructure(name);
//						int[] testbox = info.structureBoundingBox();
//						if(testbox[0]<=bound[0] && testbox[1]<=bound[1]
//								&& testbox[0] > box[0] && testbox[1]>box[1]
//										&& !name.toLowerCase().contains("maze"))
//						{
//							structureInfo = info;
//							structureName = name;
//							box = testbox;
//						}
//					}
//					x = min[0] + bound[0]/2;
//					z = min[1] + bound[1]/2;
//					if(structureInfo!=null)
//					{
//						System.out.println(structureName);
//						
//			            Random random = world.rand;
//
//			            AxisAlignedTransform2D transform = AxisAlignedTransform2D.transform(0, structureInfo.isMirrorable() && random.nextBoolean());
//
//			            int[] size = StructureInfos.structureSize(structureInfo, transform);
//
//			            int genX = x - size[0] / 2;
//			            int genZ = z - size[2] / 2;
//			            int genY;
//			            List<NaturalGenerationInfo> naturalGenerationInfos = structureInfo.generationInfos(NaturalGenerationInfo.class);
//			            if (naturalGenerationInfos.size() > 0)
//			                genY = naturalGenerationInfos.get(0).ySelector.selectY(world, random, StructureInfos.structureBoundingBox(new BlockCoord(genX, 0, genZ), size));
//			            else
//			                genY = world.getHeightValue(x, z);
//
//			            BlockCoord coord = new BlockCoord(genX, genY, genZ);
//
////			            OperationRegistry.queueOperation(new OperationGenerateStructure((GenericStructureInfo) structureInfo, transform, coord, true, structureName), player);
//			            StructureGenerator.instantly(structureInfo, world, random, coord, transform, 0, false, structureName, false);
//			           // StructureGenerator.
//			            
//					//	StructureGenerator.randomInstantly(world, world.rand, toMake, null, x, z, false, structureName);
//					}
//					//break;
//				}
//			}
//		}
		player.addChatMessage(new ChatComponentText(mess));

		return itemstack;
	}
}
