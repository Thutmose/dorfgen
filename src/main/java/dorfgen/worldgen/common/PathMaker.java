package dorfgen.worldgen.common;

import java.util.HashSet;

import dorfgen.conversion.DorfMap;
import dorfgen.conversion.DorfMap.Site;
import dorfgen.conversion.Interpolator.BicubicInterpolator;
import dorfgen.conversion.SiteStructureGenerator;
import dorfgen.conversion.SiteStructureGenerator.SiteStructures;

public class PathMaker
{
    public final SiteStructureGenerator structureGen;
    public final DorfMap                dorfs;
    public BicubicInterpolator          bicubicInterpolator = new BicubicInterpolator();
    protected int                       scale;
    protected boolean                   respectsSites       = true;

    public PathMaker(DorfMap map, SiteStructureGenerator gen)
    {
        this.dorfs = map;
        this.structureGen = gen;
        this.setScale(dorfs.scale);
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

        HashSet<Site> sites = dorfs.sitesByCoord.get(key);

        if (sites != null)
        {
            for (Site site : sites)
            {
                SiteStructures structs = structureGen.getStructuresForSite(site);
                if (structs != null && !structs.roads.isEmpty()) return true;
            }
        }

        return false;
    }

}
