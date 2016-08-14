package dorfgen;

import static dorfgen.WorldGenerator.scale;

import java.util.HashSet;

import dorfgen.conversion.DorfMap;
import dorfgen.conversion.DorfMap.Region;
import dorfgen.conversion.DorfMap.Site;
import dorfgen.conversion.SiteMapColours;
import dorfgen.conversion.SiteStructureGenerator;
import dorfgen.worldgen.WorldConstructionMaker;
import net.minecraft.creativetab.CreativeTabs;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.util.ActionResult;
import net.minecraft.util.EnumHand;
import net.minecraft.util.math.ChunkPos;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.text.TextComponentString;
import net.minecraft.world.World;
import net.minecraft.world.chunk.Chunk;
import net.minecraft.world.gen.ChunkProviderServer;

public class ItemDebug extends Item
{

    public ItemDebug()
    {
        super();
        this.setUnlocalizedName("debug");
        this.setCreativeTab(CreativeTabs.TOOLS);
    }

    @SuppressWarnings("unused")
    @Override
    public ActionResult<ItemStack> onItemRightClick(ItemStack itemStackIn, World worldIn, EntityPlayer playerIn,
            EnumHand hand)
    {
        int x = MathHelper.floor_double(playerIn.posX);
        int y = (int) playerIn.posY;
        int z = MathHelper.floor_double(playerIn.posZ);

        if (worldIn.isRemote) return super.onItemRightClick(itemStackIn, worldIn, playerIn, hand);

        DorfMap dorfs = WorldGenerator.instance.dorfs;
        int n = 0;
        Region region = dorfs.getRegionForCoords(x, z);
        HashSet<Site> sites = dorfs.getSiteForCoords(x, z);
        String mess = "";
        if (region != null)
        {
            mess += region.name + " " + region.type + " ";
        }
        WorldConstructionMaker maker = new WorldConstructionMaker();

        int h = WorldConstructionMaker.bicubicInterpolator.interpolate(WorldGenerator.instance.dorfs.elevationMap, x, z,
                scale);
        int r = WorldConstructionMaker.bicubicInterpolator.interpolate(WorldGenerator.instance.dorfs.riverMap, x, z,
                scale);
        mess = "";
        //
        int embarkX = (x / scale);
        int embarkZ = (z / scale);
        Site site = null;
        if (sites != null)
        {
            for (Site s : sites)
            {
                site = s;
                mess += site;
            }
        }

        int kx = x / (scale);
        int kz = z / (scale);
        int key = kx + 8192 * kz;

        boolean middle = false;
        HashSet<Site> ret = DorfMap.sitesByCoord.get(key);
        boolean hasRivers = false;
        if (site != null && site.rgbmap != null)
        {
            int width = (scale / SiteStructureGenerator.SITETOBLOCK);
            int pixelX = (x - site.corners[0][0] * scale - scale / 2 - width / 2) / width;
            int pixelY = (z - site.corners[0][1] * scale - scale / 2 - width / 2) / width;
            mess = "" + SiteMapColours.getMatch(site.rgbmap[pixelX][pixelY]);
            playerIn.addChatMessage(new TextComponentString(mess));
        }
        kx = worldIn.getChunkFromBlockCoords(playerIn.getPosition()).xPosition;
        kz = worldIn.getChunkFromBlockCoords(playerIn.getPosition()).zPosition;
        Chunk c = worldIn.provider.createChunkGenerator().provideChunk(kx, kz);
        ChunkProviderServer provider = (ChunkProviderServer) worldIn.getChunkProvider();
        long id = ChunkPos.chunkXZ2Int(kx, kz);
        provider.id2ChunkMap.put(id, c);
        c.onChunkLoad();
        c.populateChunk(provider, provider.chunkGenerator);
        System.out.println(c + " " + provider);
        return super.onItemRightClick(itemStackIn, worldIn, playerIn, hand);
    }

}
