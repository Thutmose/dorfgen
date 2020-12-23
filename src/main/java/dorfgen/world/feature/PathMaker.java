package dorfgen.world.feature;

import java.util.Set;

import dorfgen.conversion.DorfMap;
import dorfgen.conversion.DorfMap.Site;
import dorfgen.conversion.SiteStructureGenerator;
import dorfgen.conversion.SiteStructureGenerator.SiteStructures;
import dorfgen.util.Interpolator.BicubicInterpolator;

public class PathMaker
{
    public final SiteStructureGenerator structureGen;
    public final DorfMap                dorfs;
    public BicubicInterpolator          riverInterpolator     = new BicubicInterpolator();
    public BicubicInterpolator          waterInterpolator     = new BicubicInterpolator();
    public BicubicInterpolator          elevationInterpolator = new BicubicInterpolator();
    protected int                       scale;
    protected boolean                   respectsSites         = true;

    public PathMaker(final DorfMap map, final SiteStructureGenerator gen)
    {
        this.dorfs = map;
        this.structureGen = gen;
        this.setScale(this.dorfs.getScale());
    }

    public PathMaker setRespectsSites(final boolean respect)
    {
        this.respectsSites = respect;
        return this;
    }

    public PathMaker setScale(final int scale)
    {
        this.scale = scale;
        return this;
    }

    protected boolean isInSite(final int x, final int z)
    {
        if (!this.respectsSites) return false;
        final int kx = x / this.scale;
        final int kz = z / this.scale;

        final int key = kx + 8192 * kz;

        final Set<Site> sites = this.dorfs.sitesByCoord.get(key);

        if (sites != null) for (final Site site : sites)
        {
            final SiteStructures structs = this.structureGen.getStructuresForSite(site);
            if (structs != null && !structs.roads.isEmpty()) return true;
        }

        return false;
    }

}