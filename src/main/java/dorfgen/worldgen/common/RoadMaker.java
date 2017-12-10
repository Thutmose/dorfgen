package dorfgen.worldgen.common;

import static net.minecraft.util.EnumFacing.EAST;
import static net.minecraft.util.EnumFacing.NORTH;
import static net.minecraft.util.EnumFacing.SOUTH;
import static net.minecraft.util.EnumFacing.WEST;

import java.util.HashSet;

import javax.vecmath.Vector3d;

import dorfgen.WorldGenerator;
import dorfgen.conversion.DorfMap;
import dorfgen.conversion.DorfMap.ConstructionType;
import dorfgen.conversion.DorfMap.Site;
import dorfgen.conversion.DorfMap.WorldConstruction;
import dorfgen.conversion.SiteStructureGenerator;
import dorfgen.conversion.SiteStructureGenerator.RoadExit;
import dorfgen.conversion.SiteStructureGenerator.SiteStructures;
import net.minecraft.block.Block;
import net.minecraft.block.state.IBlockState;
import net.minecraft.init.Blocks;
import net.minecraft.util.EnumFacing;
import net.minecraft.world.World;
import net.minecraft.world.biome.Biome;
import net.minecraft.world.chunk.ChunkPrimer;

public class RoadMaker extends PathMaker
{
    public int                       minY          = 0;
    public int                       maxY          = 255;
    public double[]                  DIR_TO_RELX;
    public double[]                  DIR_TO_RELZ;
    public boolean                   respectsSites = true;
    public static final EnumFacing[] DIRS          = { EAST, WEST, NORTH, SOUTH };

    public RoadMaker(DorfMap map, SiteStructureGenerator gen)
    {
        super(map, gen);
    }

    public PathMaker setScale(int scale)
    {
        this.scale = scale;
        DIR_TO_RELX = new double[] { ((double) scale), 0., ((double) scale) / 2., ((double) scale) / 2. };
        DIR_TO_RELZ = new double[] { ((double) scale) / 2., ((double) scale) / 2., 0, ((double) scale) };
        return this;
    }

    public int dirToIndex(EnumFacing dir)
    {
        if (dir == EAST) return 0;
        if (dir == WEST) return 1;
        if (dir == NORTH) return 2;
        if (dir == SOUTH) return 3;
        return 0;
    }

    public void safeSetToRoad(int x, int z, int h, int chunkX, int chunkZ, ChunkPrimer blocks, Block block)
    {
        int x1 = x;
        int z1 = z;
        h -= minY;
        for (int i = 5; i > -2; i--)
        {
            IBlockState state = i > 0 ? Blocks.AIR.getDefaultState()
                    : i == -1 ? Blocks.COBBLESTONE.getDefaultState()
                            : i != 0 ? Blocks.AIR.getDefaultState() : block.getDefaultState();
            blocks.setBlockState(x1, h + i, z1, state);
        }
    }

    public void safeSetToRoad(int x, int z, int h, int chunkX, int chunkZ, ChunkPrimer blocks)
    {
        safeSetToRoad(x, z, h, chunkX, chunkZ, blocks, WorldGenerator.roadSurface);
    }

    public int[] getClosestRoadEnd(int x, int z, Site site)
    {
        int[] edge = null;
        int[] result = null;

        int minDistanceSqr = Integer.MAX_VALUE;

        SiteStructures structures = structureGen.getStructuresForSite(site);
        for (RoadExit exit : structures.roads)
        {
            edge = exit.getEdgeMid(site, scale);
            if (minDistanceSqr > (x - edge[0]) * (x - edge[0]) + (z - edge[1]) * (z - edge[1]))
            {
                minDistanceSqr = (x - edge[0]) * (x - edge[0]) + (z - edge[1]) * (z - edge[1]);
                result = edge;
            }
        }

        return result;
    }

    public int roundToEmbark(int a)
    {
        return a - (a % scale);
    }

    static public final int ROAD_SEARCH_AREA = 3;

    public boolean isNearSiteRoadEnd(int x, int z)
    {
        if (!respectsSites) return false;
        HashSet<Site> sites = new HashSet<Site>(), subSites;

        int kx = x / scale;
        int kz = z / scale;

        for (int xsearch = -ROAD_SEARCH_AREA; xsearch <= ROAD_SEARCH_AREA; xsearch++)
        {
            for (int zsearch = -ROAD_SEARCH_AREA; zsearch <= ROAD_SEARCH_AREA; zsearch++)
            {
                subSites = dorfs.sitesByCoord.get((kx + xsearch) + 8192 * (kz + zsearch));
                if (subSites != null) sites.addAll(subSites);
            }
        }

        if (sites.size() == 0) return false;

        for (Site site : sites)
        {
            int[] edge = getClosestRoadEnd(x, z, site);
            if (edge == null) continue;
            if (roundToEmbark(x) == roundToEmbark(edge[0]) && roundToEmbark(z) == roundToEmbark(edge[1])
                    || roundToEmbark(x) - scale == roundToEmbark(edge[0]) && roundToEmbark(z) == roundToEmbark(edge[1])
                    || roundToEmbark(x) + scale == roundToEmbark(edge[0]) && roundToEmbark(z) == roundToEmbark(edge[1])
                    || roundToEmbark(x) == roundToEmbark(edge[0]) && roundToEmbark(z) + scale == roundToEmbark(edge[1])
                    || roundToEmbark(x) == roundToEmbark(edge[0])
                            && roundToEmbark(z) - scale == roundToEmbark(edge[1])) { return true; }
        }

        return false;
    }

    public int[] getSiteRoadEnd(int x, int z)
    {
        if (!respectsSites) return null;
        HashSet<Site> sites = new HashSet<Site>(), subSites;

        int kx = x / scale;
        int kz = z / scale;

        for (int xsearch = -ROAD_SEARCH_AREA; xsearch <= ROAD_SEARCH_AREA; xsearch++)
        {
            for (int zsearch = -ROAD_SEARCH_AREA; zsearch <= ROAD_SEARCH_AREA; zsearch++)
            {
                subSites = dorfs.sitesByCoord.get((kx + xsearch) + 8192 * (kz + zsearch));
                if (subSites != null) sites.addAll(subSites);
            }
        }

        if (sites.size() == 0) return null;

        for (Site site : sites)
        {
            int[] edge = getClosestRoadEnd(x, z, site);
            if (edge == null) continue;
            if (roundToEmbark(x) == roundToEmbark(edge[0]) && roundToEmbark(z) == roundToEmbark(edge[1])
                    || roundToEmbark(x) - scale == roundToEmbark(edge[0]) && roundToEmbark(z) == roundToEmbark(edge[1])
                    || roundToEmbark(x) + scale == roundToEmbark(edge[0]) && roundToEmbark(z) == roundToEmbark(edge[1])
                    || roundToEmbark(x) == roundToEmbark(edge[0]) && roundToEmbark(z) + scale == roundToEmbark(edge[1])
                    || roundToEmbark(x) == roundToEmbark(edge[0])
                            && roundToEmbark(z) - scale == roundToEmbark(edge[1])) { return edge; }
        }

        return null;
    }

    public boolean isInRoad(int xAbs, int h, int zAbs)
    {
        int x = xAbs, z = zAbs;
        int kx = xAbs / scale;
        int kz = zAbs / scale;
        int offset = scale / 2;
        if (respectsSites && isInSite(xAbs, zAbs)) return false;
        boolean[] dirs = getRoadDirection(x, z);
        int width = 3 * scale / SiteStructureGenerator.SITETOBLOCK;
        width = Math.max(3, width);
        boolean road = dirs[0] || dirs[1] || dirs[2] || dirs[3];
        if (!road) return false;
        // else if (road) return true;
        int[] point1 = null;
        int[] point2 = null;
        int[] point3 = null;
        int[] point4 = null;

        if (dirs[0])
        {
            int[] nearest = new int[] { (kx + 1) * scale, (kz) * scale + offset };
            point1 = nearest;
        }

        if (dirs[1])
        {
            int[] nearest = new int[] { (kx - 1) * scale + scale, (kz) * scale + offset };
            point2 = nearest;
        }

        if (dirs[2])
        {
            int[] nearest = new int[] { kx * scale + offset, (kz - 1) * scale + scale };
            point3 = nearest;
        }

        if (dirs[3])
        {
            int[] nearest = new int[] { kx * scale + offset, (kz + 1) * scale };
            point4 = nearest;
        }
        if (point1 != null || point2 != null || point3 != null || point4 != null)
        {
           //@formatter:off
//            int num = 0;
//            for(int i = 0; i<dirs.length; i++) if(dirs[i]) num++;
//            if(num==1)
//                System.out.println(Arrays.toString(point1) + 
//                  "  " + Arrays.toString(point2) + 
//                  "  " + Arrays.toString(point3)+ 
//                  "  " + Arrays.toString(point4));
          //@formatter:on
        }
        int dr = 2;
        if (point1 != null && point2 != null)
        {
            Vector3d dir = new Vector3d(point1[0] - point2[0], 0, point1[1] - point2[1]);
            double distance = dir.length() + dr;
            dir.normalize();
            for (double i = 0; i < distance; i++)
            {
                int tx = (point2[0] + (int) (dir.x * i)) - x;
                int tz = (point2[1] + (int) (dir.z * i)) - z;
                if (Math.abs(tx) < width && Math.abs(tz) < width) return true;
            }
        }
        if (point1 != null && point3 != null)
        {
            Vector3d dir = new Vector3d(point1[0] - point3[0], 0, point1[1] - point3[1]);
            double distance = dir.length() + dr;
            dir.normalize();
            for (double i = 0; i < distance; i++)
            {
                int tx = (point3[0] + (int) (dir.x * i)) - x;
                int tz = (point3[1] + (int) (dir.z * i)) - z;
                tx = Math.abs(tx);
                tz = Math.abs(tz);
                if (tx < width && tz < width) return true;
            }
        }
        if (point1 != null && point4 != null)
        {
            Vector3d dir = new Vector3d(point1[0] - point4[0], 0, point1[1] - point4[1]);
            double distance = dir.length() + dr;
            dir.normalize();
            for (double i = 0; i < distance; i++)
            {
                int tx = (point4[0] + (int) (dir.x * i)) - x;
                int tz = (point4[1] + (int) (dir.z * i)) - z;
                if (Math.abs(tx) < width && Math.abs(tz) < width) return true;
            }
        }
        if (point2 != null && point3 != null)
        {
            Vector3d dir = new Vector3d(point2[0] - point3[0], 0, point2[1] - point3[1]);
            double distance = dir.length() + dr;
            dir.normalize();
            for (double i = 0; i < distance; i++)
            {
                int tx = (point3[0] + (int) (dir.x * i)) - x;
                int tz = (point3[1] + (int) (dir.z * i)) - z;
                if (Math.abs(tx) < width && Math.abs(tz) < width) return true;
            }
        }
        if (point2 != null && point4 != null)
        {
            Vector3d dir = new Vector3d(point2[0] - point4[0], 0, point2[1] - point4[1]);
            double distance = dir.length() + dr;
            dir.normalize();
            for (double i = 0; i < distance; i++)
            {
                int tx = (point4[0] + (int) (dir.x * i)) - x;
                int tz = (point4[1] + (int) (dir.z * i)) - z;
                if (Math.abs(tx) < width && Math.abs(tz) < width) return true;
            }
        }
        if (point4 != null && point3 != null)
        {
            Vector3d dir = new Vector3d(point4[0] - point3[0], 0, point4[1] - point3[1]);
            double distance = dir.length() + dr;
            dir.normalize();
            for (double i = 0; i < distance; i++)
            {
                int tx = (point3[0] + (int) (dir.x * i)) - x;
                int tz = (point3[1] + (int) (dir.z * i)) - z;
                if (Math.abs(tx) < width && Math.abs(tz) < width) return true;
            }
        }

        return false;
    }

    public boolean hasRoad(int xAbs, int h, int zAbs)
    {
        HashSet<WorldConstruction> cons = dorfs.getConstructionsForCoords(xAbs, zAbs);

        if (cons == null || cons.isEmpty()) return false;

        boolean in = false;
        for (WorldConstruction con : cons)
        {
            if (con.type == DorfMap.ConstructionType.ROAD || con.type == ConstructionType.BRIDGE)
            {
                if (con.isInConstruct(xAbs, h, zAbs)) { return true; }
            }
        }
        return in;
    }

    public void debugPrint(int x, int z)
    {
        int embarkX = roundToEmbark(x);
        int embarkZ = roundToEmbark(z);

        if (respectsSites && isInSite(x, z))
        {
            System.out.println("Embark location x: " + embarkX + " z: " + embarkZ + " is in a site");
        }

        if (hasRoad(x, minY, z))
        {
            System.out.println("Embark location x: " + embarkX + " z: " + embarkZ + " has a road");
        }

        if (dorfs.getConstructionsForCoords(x, z) != null)
        {
            for (WorldConstruction constr : dorfs.getConstructionsForCoords(x, z))
            {
                if (constr.isInConstruct(x, minY % 16, z))
                {
                    System.out.println("Location x: " + x + " z: " + z + " is in a construction");
                    System.out.println("    Construction is " + constr.toString());
                }
            }
        }

        if (dorfs.getConstructionsForCoords(embarkX, embarkZ) != null)
        {
            for (WorldConstruction constr : dorfs.getConstructionsForCoords(embarkX, embarkZ))
            {
                if (constr.isInConstruct(embarkX, minY % 16, embarkZ))
                {
                    System.out.println("Location x: " + embarkX + " z: " + embarkZ + " is in a construction");
                    System.out.println("    Construction is " + constr.toString());
                }
            }
        }

        if (isNearSiteRoadEnd(x, z))
        {
            System.out.println("Embark location x: " + embarkX + " z: " + embarkZ + " is near a site road end");
            int[] roadEnd = getSiteRoadEnd(x, z);
            System.out.println("Site road end is at x: " + roadEnd[0] + " z: " + roadEnd[1]);

            int minDistSqr = Integer.MAX_VALUE, dist;
            int x1, z1;
            int embarkX1 = 0, embarkZ1 = 0;
            int roadEndX = roadEnd[0];
            int roadEndZ = roadEnd[1];

            for (int xsearch = -ROAD_SEARCH_AREA; xsearch <= ROAD_SEARCH_AREA; xsearch++)
            {
                for (int zsearch = -ROAD_SEARCH_AREA; zsearch <= ROAD_SEARCH_AREA; zsearch++)
                {
                    x1 = roundToEmbark(roadEndX + (xsearch * scale));
                    z1 = roundToEmbark(roadEndZ + (zsearch * scale));

                    if (respectsSites && isInSite(x1, z1)) continue;
                    if (!hasRoad(x1, minY, z1)) continue;

                    dist = (x1 - roadEndX) * (x1 - roadEndX) + (z1 - roadEndZ) * (z1 - roadEndZ);

                    if (dist < minDistSqr)
                    {
                        minDistSqr = dist;
                        embarkX1 = x1;
                        embarkZ1 = z1;
                    }
                }
            }
            if (minDistSqr != Integer.MAX_VALUE)
            {
                System.out.println("Nearest embark to road end found at x: " + embarkX1 + " z: " + embarkZ1);

                EnumFacing closestdir = EAST;
                double distSqr2;
                double minDistSqr2 = Integer.MAX_VALUE;

                double endX = roadEndX - embarkX;
                double endZ = roadEndZ - embarkZ;

                boolean[] dirs = getRoadDirection(embarkX, embarkZ);

                for (EnumFacing dir : DIRS)
                {
                    if (!dirs[dirToIndex(dir)]) continue;
                    distSqr2 = (DIR_TO_RELX[dirToIndex(dir)] - ((double) endX))
                            * (DIR_TO_RELX[dirToIndex(dir)] - ((double) endX))
                            + (DIR_TO_RELZ[dirToIndex(dir)] - ((double) endZ))
                                    * (DIR_TO_RELZ[dirToIndex(dir)] - ((double) endZ));
                    if (distSqr2 < minDistSqr2)
                    {
                        minDistSqr2 = distSqr2;
                        closestdir = dir;
                    }
                }

                if (closestdir == EAST) System.out.println("Closest dir is east");
                if (closestdir == WEST) System.out.println("Closest dir is west");
                if (closestdir == NORTH) System.out.println("Closest dir is north");
                if (closestdir == SOUTH) System.out.println("Closest dir is south");

                System.out.println("    with distance " + minDistSqr2);
            }
        }
    }

    public void buildRoads(World world, int chunkX, int chunkZ, ChunkPrimer blocks, Biome[] biomes, int minY, int maxY)
    {
        this.minY = minY;
        this.maxY = maxY;
        int x = dorfs.shiftX(chunkX * 16);
        int z = dorfs.shiftZ(chunkZ * 16);

        int h = 0;
        int hMin = Integer.MAX_VALUE;
        int hMax = Integer.MIN_VALUE;
        int dh = 2;
        int dr = 16;

        for (int i = 0; i < 16; i++)
            for (int j = 0; j < 16; j++)
            {
                int x1 = x + i;
                int z1 = z + j;
                int x2 = (x + i + dr);
                int z2 = (z + j + dr);
                boolean[] dirs = getRoadDirection(dorfs.unShiftX(x1), dorfs.unShiftZ(z1));
                boolean skip = false;
                if (x2 / scale + x2 % scale >= dorfs.waterMap.length
                        || z2 / scale + z2 % scale >= dorfs.waterMap[0].length)
                {
                    h = 1;
                    skip = true;
                }
                else
                {
                    h = bicubicInterpolator.interpolate(dorfs.elevationMap, x1, z1, scale);
                    if (x1 - dr > 0 && z1 - dr > 0)
                    {
                        if (dirs[0] || dirs[1])
                        {
                            EnumFacing side = DIRS[0].rotateY();
                            for (int r = 1; r <= dr; r++)
                            {
                                x2 = x1 + side.getFrontOffsetX() * r;
                                z2 = z1 + side.getFrontOffsetZ() * r;
                                if (!hasRoad(dorfs.unShiftX(x2), h, dorfs.unShiftZ(z2))) break;
                                int h2 = bicubicInterpolator.interpolate(dorfs.elevationMap, x2, z2, scale);
                                hMin = Math.min(hMin, h2);
                                hMax = Math.max(hMax, h2);
                            }
                            side = DIRS[1].rotateY();
                            for (int r = 1; r <= dr; r++)
                            {
                                x2 = x1 + side.getFrontOffsetX() * r;
                                z2 = z1 + side.getFrontOffsetZ() * r;
                                if (!hasRoad(dorfs.unShiftX(x2), h, dorfs.unShiftZ(z2))) break;
                                int h2 = bicubicInterpolator.interpolate(dorfs.elevationMap, x2, z2, scale);
                                hMin = Math.min(hMin, h2);
                                hMax = Math.max(hMax, h2);
                            }
                        }
                        if (dirs[2] || dirs[3])
                        {
                            EnumFacing side = DIRS[2].rotateY();
                            for (int r = 1; r <= dr; r++)
                            {
                                x2 = x1 + side.getFrontOffsetX() * r;
                                z2 = z1 + side.getFrontOffsetZ() * r;
                                if (!hasRoad(dorfs.unShiftX(x2), h, dorfs.unShiftZ(z2))) break;
                                int h2 = bicubicInterpolator.interpolate(dorfs.elevationMap, x2, z2, scale);
                                hMin = Math.min(hMin, h2);
                                hMax = Math.max(hMax, h2);
                            }
                            side = DIRS[3].rotateY();
                            for (int r = 1; r <= dr; r++)
                            {
                                x2 = x1 + side.getFrontOffsetX() * r;
                                z2 = z1 + side.getFrontOffsetZ() * r;
                                if (!hasRoad(dorfs.unShiftX(x2), h, dorfs.unShiftZ(z2))) break;
                                int h2 = bicubicInterpolator.interpolate(dorfs.elevationMap, x2, z2, scale);
                                hMin = Math.min(hMin, h2);
                                hMax = Math.max(hMax, h2);
                            }
                        }
                    }
                    if (Math.abs(hMax - hMin) < dh) h = hMin;
                }
                if (skip || h > maxY + 32 || h < minY - 32) continue;

                if (respectsSites && isInSite(dorfs.unShiftX(x1), dorfs.unShiftZ(z1))) continue;
                if (isInRoad(dorfs.unShiftX(x1), h, dorfs.unShiftZ(z1)))
                {
                    safeSetToRoad(i, j, h, chunkX, chunkZ, blocks);
                }
            }
    }

    public boolean[] getRoadDirection(int xAbs, int zAbs)
    {
        boolean[] ret = new boolean[4];

        HashSet<WorldConstruction> constructs = dorfs.getConstructionsForCoords(xAbs, zAbs);

        if (constructs == null) return ret;

        for (WorldConstruction con : constructs)
        {
            if (!con.isInConstruct(xAbs, minY % 16, zAbs)) continue;

            if (con.isInConstruct(xAbs - scale, minY % 16, zAbs))
            {
                ret[1] = true;
            }
            if (con.isInConstruct(xAbs + scale, minY % 16, zAbs))
            {
                ret[0] = true;
            }
            if (con.isInConstruct(xAbs, minY % 16, zAbs - scale))
            {
                ret[2] = true;
            }
            if (con.isInConstruct(xAbs, minY % 16, zAbs + scale))
            {
                ret[3] = true;
            }
        }
        return ret;
    }

}
