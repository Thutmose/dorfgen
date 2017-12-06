package dorfgen;

import static dorfgen.WorldGenerator.scale;

import java.util.HashSet;

import dorfgen.conversion.DorfMap;
import dorfgen.conversion.DorfMap.Region;
import dorfgen.conversion.DorfMap.Site;
import dorfgen.worldgen.common.WorldConstructionMaker;
import dorfgen.conversion.SiteMapColours;
import dorfgen.conversion.SiteStructureGenerator;
import net.minecraft.creativetab.CreativeTabs;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.util.ActionResult;
import net.minecraft.util.EnumHand;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.text.TextComponentString;
import net.minecraft.world.World;

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
    public ActionResult<ItemStack> onItemRightClick(World world, EntityPlayer player, EnumHand handIn)
    {
        int x = MathHelper.floor(player.posX);
        int y = (int) player.posY;
        int z = MathHelper.floor(player.posZ);

        if (world.isRemote) return super.onItemRightClick(world, player, handIn);

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
            player.sendMessage(new TextComponentString(mess));
        }

        return super.onItemRightClick(world, player, handIn);
    }

}
