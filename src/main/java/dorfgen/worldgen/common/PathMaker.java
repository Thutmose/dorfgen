package dorfgen.worldgen.common;

import java.util.HashSet;

import dorfgen.WorldGenerator;
import dorfgen.conversion.DorfMap;
import dorfgen.conversion.DorfMap.Site;
import dorfgen.conversion.Interpolator.BicubicInterpolator;
import dorfgen.conversion.SiteStructureGenerator.SiteStructures;

public class PathMaker
{
    public BicubicInterpolator bicubicInterpolator = new BicubicInterpolator();
    protected int              scale               = WorldGenerator.scale;
    protected boolean          respectsSites       = true;

    public PathMaker()
    {
        // TODO Auto-generated constructor stub
    }

    public PathMaker setRespectsSites(boolean respect)
    {
        this.respectsSites = respect;
        return this;
    }

    public PathMaker setScale(int scale)
    {
        this.scale = scale;
        return this;
    }

    protected boolean isInSite(int x, int z)
    {
        if (!respectsSites) return false;
        int kx = x / scale;
        int kz = z / scale;

        int key = kx + 8192 * kz;

        HashSet<Site> sites = DorfMap.sitesByCoord.get(key);

        if (sites != null)
        {
            for (Site site : sites)
            {
                SiteStructures structs = WorldGenerator.instance.structureGen.getStructuresForSite(site);
                if (structs != null && !structs.roads.isEmpty()) return true;
            }
        }

        return false;
    }

}
