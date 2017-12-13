package dorfgen.worldgen.common;

import java.util.HashSet;
import java.util.Random;
import java.util.Set;

import com.google.common.collect.Sets;

import dorfgen.WorldGenerator;
import dorfgen.conversion.DorfMap;
import dorfgen.conversion.DorfMap.Site;
import dorfgen.conversion.DorfMap.SiteType;
import dorfgen.worldgen.structures.scattered.StructureLair;
import dorfgen.worldgen.structures.scattered.WoodlandMansionStart;
import dorfgen.worldgen.structures.village.VillageWrappedList;
import net.minecraft.world.World;
import net.minecraft.world.gen.structure.ComponentScatteredFeaturePieces;
import net.minecraft.world.gen.structure.MapGenStronghold;
import net.minecraft.world.gen.structure.MapGenVillage;
import net.minecraft.world.gen.structure.StructureStart;
import net.minecraft.world.gen.structure.StructureStrongholdPieces;
import net.minecraft.world.gen.structure.StructureVillagePieces;

public class MapGenSites extends MapGenVillage
{
    HashSet<Integer> set       = new HashSet<Integer>();
    HashSet<Integer> made      = new HashSet<Integer>();
    Site             siteToGen = null;
    private boolean  villages  = false;
    private boolean  sites     = true;
    final DorfMap    map;

    public MapGenSites(DorfMap map)
    {
        super();
        this.map = map;
    }

    public MapGenSites genVillages(boolean villages)
    {
        this.villages = villages;
        return this;
    }

    public MapGenSites genSites(boolean sites)
    {
        this.sites = sites;
        return this;
    }

    @Override
    protected boolean canSpawnStructureAtCoords(int x, int z)
    {
        Set<Site> sites = Sets.newHashSet();
        x *= 16;
        z *= 16;
        for (int dx = 0; dx < 16; dx += map.scale)
            for (int dz = 0; dz < 16; dz += map.scale)
            {
                Set<Site> here = map.getSiteForCoords(x + dx, z + dz);
                if (here != null) sites.addAll(here);
            }
        if (sites.isEmpty()) return false;
        x = map.shiftX(x);
        z = map.shiftZ(z);
        for (Site site : sites)
        {
            if (!set.contains(site.id) && shouldSiteSpawn(x, z, site))
            {
                set.add(site.id);
                siteToGen = site;
                return true;
            }
        }

        return false;
    }

    public boolean shouldSiteSpawn(int x, int z, Site site)
    {
        boolean middle = false;
        int[] mid = site.getSiteMid();
        x = site.map.unShiftX(x);
        z = site.map.unShiftX(z);
        outer:
        for (int i = 0; i < 16; i++)
        {
            for (int j = 0; j < 16; j++)
            {
                middle = x + i == mid[0] && z + j == mid[1];
                if (middle)
                {
                    break outer;
                }
            }
        }
        WorldGenerator.log(middle + " " + site);
        if (!middle) return false;
        switch (site.type)
        {
        case CAMP:
            return villages;
        case CAVE:
            return sites || villages;
        case DARKFORTRESS:
            return villages;
        case DARKPITS:
            return villages;
        case FORTRESS:
            return sites || villages;
        case HAMLET:
            return sites || villages;
        case HILLOCKS:
            return villages;
        case HIPPYHUTS:
            return sites || villages;
        case LABYRINTH:
            return villages;
        case LAIR:
            return sites || villages;
        case MOUNTAINHALLS:
            return villages;
        case SHRINE:
            return sites || villages;
        case TOMB:
            return sites || villages;
        case TOWER:
            return sites || villages;
        case TOWN:
            return villages;
        case VAULT:
            return sites || villages;
        default:
            break;
        }
        return false;
    }

    @Override
    protected StructureStart getStructureStart(int x, int z)
    {
        Site site = siteToGen;
        siteToGen = null;
        if (site == null) { return super.getStructureStart(x, z); }
        WorldGenerator.log("Generating Site " + site);
        made.add(site.id);

        if (site.type == SiteType.FORTRESS)
        {
            MapGenStronghold.Start start;

            for (start = new MapGenStronghold.Start(this.world, this.rand, x, z); start.getComponents().isEmpty()
                    || ((StructureStrongholdPieces.Stairs2) start.getComponents()
                            .get(0)).strongholdPortalRoom == null; start = new MapGenStronghold.Start(this.world,
                                    this.rand, x, z))
            {
                ;
            }
            return start;
        }

        if (site.type == SiteType.DARKFORTRESS)
        {
            WoodlandMansionStart start = new WoodlandMansionStart(world, map, rand, x, z);
            return start;
        }
        else if (site.type == SiteType.DARKPITS)
        {
            WoodlandMansionStart start = new WoodlandMansionStart(world, map, rand, x, z);
            return start;
        }
        else if (site.type == SiteType.HIPPYHUTS)
        {
            return new Start(map, world, rand, x, z, 0, site);
        }
        else if (site.type == SiteType.SHRINE)
        {
            return new Start(map, world, rand, x, z, 2, site);
        }
        else if (site.type == SiteType.LAIR)
        {
            return new Start(map, world, rand, x, z, 3, site);
        }
        else if (site.type == SiteType.CAVE) { return new Start(map, world, rand, x, z, 1, site); }

        StructureStart start = super.getStructureStart(x, z);

        if (start instanceof MapGenVillage.Start && !start.getComponents().isEmpty())
        {
            MapGenVillage.Start struct = (net.minecraft.world.gen.structure.MapGenVillage.Start) start;
            StructureVillagePieces.Start initial = (net.minecraft.world.gen.structure.StructureVillagePieces.Start) struct
                    .getComponents().get(0);
            VillageWrappedList<?> pendingPaths = new VillageWrappedList<>(map);
            pendingPaths.addAll(initial.pendingRoads);
            initial.pendingRoads = pendingPaths;
            VillageWrappedList<?> pendingHouses = new VillageWrappedList<>(map);
            pendingHouses.addAll(initial.pendingHouses);
            initial.pendingHouses = pendingHouses;
            VillageWrappedList<?> comps = new VillageWrappedList<>(map);
            comps.addAll(struct.getComponents());
            struct.getComponents().clear();
            struct.getComponents().addAll(comps);
        }
        return start;
    }

    public static class Start extends StructureStart
    {
        public Start()
        {
        }

        public Start(DorfMap map, World world_, Random rand, int x, int z, int type, Site site)
        {
            super(x, z);
            if (type == 0)
            {
                for (int k = 0; k < 15; k++)
                {
                    int x1 = 40 - rand.nextInt(40);
                    int z1 = 40 - rand.nextInt(40);

                    for (int i = 0; i < rand.nextInt(20); i++)
                    {

                        ComponentScatteredFeaturePieces.SwampHut swamphut = new ComponentScatteredFeaturePieces.SwampHut(
                                rand, x * 16 + x1, z * 16 + z1);

                        this.components.add(swamphut);
                    }
                }
            }
            else if (type == 1)
            {
                ComponentScatteredFeaturePieces.DesertPyramid desertpyramid = new ComponentScatteredFeaturePieces.DesertPyramid(
                        rand, x * 16, z * 16);
                this.components.add(desertpyramid);
            }
            else if (type == 2)
            {
                ComponentScatteredFeaturePieces.JunglePyramid junglepyramid = new ComponentScatteredFeaturePieces.JunglePyramid(
                        rand, x * 16, z * 16);
                this.components.add(junglepyramid);
            }
            else if (type == 3)
            {
                this.components.add(new StructureLair(map, site));
            }

            this.updateBoundingBox();
        }
    }
}
