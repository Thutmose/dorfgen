package dorfgen.world.feature;

import static net.minecraft.util.Direction.EAST;
import static net.minecraft.util.Direction.NORTH;
import static net.minecraft.util.Direction.SOUTH;
import static net.minecraft.util.Direction.WEST;

import java.util.HashSet;
import java.util.Set;

import dorfgen.Dorfgen;
import dorfgen.conversion.DorfMap;
import dorfgen.conversion.DorfMap.ConstructionType;
import dorfgen.conversion.DorfMap.Site;
import dorfgen.conversion.DorfMap.WorldConstruction;
import dorfgen.conversion.SiteStructureGenerator;
import dorfgen.conversion.SiteStructureGenerator.RoadExit;
import dorfgen.conversion.SiteStructureGenerator.SiteStructures;
import net.minecraft.block.Block;
import net.minecraft.block.BlockState;
import net.minecraft.block.Blocks;
import net.minecraft.util.Direction;
import net.minecraft.util.math.BlockPos.Mutable;
import net.minecraft.util.math.ChunkPos;
import net.minecraft.util.math.Vec3d;
import net.minecraft.world.chunk.IChunk;

public class RoadMaker extends PathMaker
{
    public int                      minY          = 0;
    public int                      maxY          = 255;
    public double[]                 DIR_TO_RELX;
    public double[]                 DIR_TO_RELZ;
    public boolean                  respectsSites = true;
    public static final Direction[] DIRS          = { EAST, WEST, NORTH, SOUTH };

    public RoadMaker(final DorfMap map, final SiteStructureGenerator gen)
    {
        super(map, gen);
    }

    @Override
    public PathMaker setScale(final int scale)
    {
        this.scale = scale;
        this.DIR_TO_RELX = new double[] { scale, 0., scale / 2., scale / 2. };
        this.DIR_TO_RELZ = new double[] { scale / 2., scale / 2., 0, scale };
        return this;
    }

    public int dirToIndex(final Direction dir)
    {
        if (dir == EAST) return 0;
        if (dir == WEST) return 1;
        if (dir == NORTH) return 2;
        if (dir == SOUTH) return 3;
        return 0;
    }

    public void safeSetToRoad(final int x, final int z, int h, final IChunk blocks, final Block block,
            final Mutable pos)
    {
        final int x1 = x;
        final int z1 = z;
        h -= this.minY;
        for (int i = 5; i > -2; i--)
        {
            final BlockState state = i > 0 ? Blocks.AIR.getDefaultState()
                    : i == -1 ? Blocks.COBBLESTONE.getDefaultState()
                            : i != 0 ? Blocks.AIR.getDefaultState() : block.getDefaultState();
            blocks.setBlockState(pos.setPos(x1, h + i, z1), state, false);
        }
    }

    public void safeSetToRoad(final int x, final int z, final int h, final IChunk blocks, final Mutable pos)
    {
        this.safeSetToRoad(x, z, h, blocks, Blocks.GRASS_PATH, pos);
    }

    public int[] getClosestRoadEnd(final int x, final int z, final Site site)
    {
        int[] edge = null;
        int[] result = null;

        int minDistanceSqr = Integer.MAX_VALUE;

        final SiteStructures structures = this.structureGen.getStructuresForSite(site);
        for (final RoadExit exit : structures.roads)
        {
            edge = exit.getEdgeMid(site, this.scale);
            if (minDistanceSqr > (x - edge[0]) * (x - edge[0]) + (z - edge[1]) * (z - edge[1]))
            {
                minDistanceSqr = (x - edge[0]) * (x - edge[0]) + (z - edge[1]) * (z - edge[1]);
                result = edge;
            }
        }

        return result;
    }

    public int roundToEmbark(final int a)
    {
        return a - a % this.scale;
    }

    static public final int ROAD_SEARCH_AREA = 3;

    public boolean isNearSiteRoadEnd(final int x, final int z)
    {
        if (!this.respectsSites) return false;
        final Set<Site> sites = new HashSet<>();
        Set<Site> subSites;

        final int kx = x / this.scale;
        final int kz = z / this.scale;

        for (int xsearch = -RoadMaker.ROAD_SEARCH_AREA; xsearch <= RoadMaker.ROAD_SEARCH_AREA; xsearch++)
            for (int zsearch = -RoadMaker.ROAD_SEARCH_AREA; zsearch <= RoadMaker.ROAD_SEARCH_AREA; zsearch++)
            {
                subSites = this.dorfs.sitesByCoord.get(kx + xsearch + 8192 * (kz + zsearch));
                if (subSites != null) sites.addAll(subSites);
            }

        if (sites.size() == 0) return false;

        for (final Site site : sites)
        {
            final int[] edge = this.getClosestRoadEnd(x, z, site);
            if (edge == null) continue;
            if (this.roundToEmbark(x) == this.roundToEmbark(edge[0]) && this.roundToEmbark(z) == this.roundToEmbark(
                    edge[1]) || this.roundToEmbark(x) - this.scale == this.roundToEmbark(edge[0]) && this.roundToEmbark(
                            z) == this.roundToEmbark(edge[1]) || this.roundToEmbark(x) + this.scale == this
                                    .roundToEmbark(edge[0]) && this.roundToEmbark(z) == this.roundToEmbark(edge[1])
                    || this.roundToEmbark(x) == this.roundToEmbark(edge[0]) && this.roundToEmbark(z)
                            + this.scale == this.roundToEmbark(edge[1]) || this.roundToEmbark(x) == this.roundToEmbark(
                                    edge[0]) && this.roundToEmbark(z) - this.scale == this.roundToEmbark(edge[1]))
                return true;
        }

        return false;
    }

    public int[] getSiteRoadEnd(final int x, final int z)
    {
        if (!this.respectsSites) return null;
        final Set<Site> sites = new HashSet<>();
        Set<Site> subSites;

        final int kx = x / this.scale;
        final int kz = z / this.scale;

        for (int xsearch = -RoadMaker.ROAD_SEARCH_AREA; xsearch <= RoadMaker.ROAD_SEARCH_AREA; xsearch++)
            for (int zsearch = -RoadMaker.ROAD_SEARCH_AREA; zsearch <= RoadMaker.ROAD_SEARCH_AREA; zsearch++)
            {
                subSites = this.dorfs.sitesByCoord.get(kx + xsearch + 8192 * (kz + zsearch));
                if (subSites != null) sites.addAll(subSites);
            }

        if (sites.size() == 0) return null;

        for (final Site site : sites)
        {
            final int[] edge = this.getClosestRoadEnd(x, z, site);
            if (edge == null) continue;
            if (this.roundToEmbark(x) == this.roundToEmbark(edge[0]) && this.roundToEmbark(z) == this.roundToEmbark(
                    edge[1]) || this.roundToEmbark(x) - this.scale == this.roundToEmbark(edge[0]) && this.roundToEmbark(
                            z) == this.roundToEmbark(edge[1]) || this.roundToEmbark(x) + this.scale == this
                                    .roundToEmbark(edge[0]) && this.roundToEmbark(z) == this.roundToEmbark(edge[1])
                    || this.roundToEmbark(x) == this.roundToEmbark(edge[0]) && this.roundToEmbark(z)
                            + this.scale == this.roundToEmbark(edge[1]) || this.roundToEmbark(x) == this.roundToEmbark(
                                    edge[0]) && this.roundToEmbark(z) - this.scale == this.roundToEmbark(edge[1]))
                return edge;
        }

        return null;
    }

    public boolean isInRoad(final int xAbs, final int h, final int zAbs)
    {
        final int x = xAbs, z = zAbs;
        final int kx = xAbs / this.scale;
        final int kz = zAbs / this.scale;
        final int offset = this.scale / 2;
        if (this.respectsSites && this.isInSite(xAbs, zAbs)) return false;
        final boolean[] dirs = this.getRoadDirection(x, z);
        int width = 3 * this.scale / SiteStructureGenerator.SITETOBLOCK;
        width = Math.max(3, width);
        final boolean road = dirs[0] || dirs[1] || dirs[2] || dirs[3];
        if (!road) return false;
        // else if (road) return true;
        int[] point1 = null;
        int[] point2 = null;
        int[] point3 = null;
        int[] point4 = null;

        if (dirs[0])
        {
            final int[] nearest = new int[] { (kx + 1) * this.scale, kz * this.scale + offset };
            point1 = nearest;
        }

        if (dirs[1])
        {
            final int[] nearest = new int[] { (kx - 1) * this.scale + this.scale, kz * this.scale + offset };
            point2 = nearest;
        }

        if (dirs[2])
        {
            final int[] nearest = new int[] { kx * this.scale + offset, (kz - 1) * this.scale + this.scale };
            point3 = nearest;
        }

        if (dirs[3])
        {
            final int[] nearest = new int[] { kx * this.scale + offset, (kz + 1) * this.scale };
            point4 = nearest;
        }

        final int dr = 2;
        if (point1 != null && point2 != null)
        {
            Vec3d dir = new Vec3d(point1[0] - point2[0], 0, point1[1] - point2[1]);
            final double distance = dir.length() + dr;
            dir = dir.normalize();
            for (double i = 0; i < distance; i++)
            {
                final int tx = point2[0] + (int) (dir.x * i) - x;
                final int tz = point2[1] + (int) (dir.z * i) - z;
                if (Math.abs(tx) < width && Math.abs(tz) < width) return true;
            }
        }
        if (point1 != null && point3 != null)
        {
            Vec3d dir = new Vec3d(point1[0] - point3[0], 0, point1[1] - point3[1]);
            final double distance = dir.length() + dr;
            dir = dir.normalize();
            for (double i = 0; i < distance; i++)
            {
                int tx = point3[0] + (int) (dir.x * i) - x;
                int tz = point3[1] + (int) (dir.z * i) - z;
                tx = Math.abs(tx);
                tz = Math.abs(tz);
                if (tx < width && tz < width) return true;
            }
        }
        if (point1 != null && point4 != null)
        {
            Vec3d dir = new Vec3d(point1[0] - point4[0], 0, point1[1] - point4[1]);
            final double distance = dir.length() + dr;
            dir = dir.normalize();
            for (double i = 0; i < distance; i++)
            {
                final int tx = point4[0] + (int) (dir.x * i) - x;
                final int tz = point4[1] + (int) (dir.z * i) - z;
                if (Math.abs(tx) < width && Math.abs(tz) < width) return true;
            }
        }
        if (point2 != null && point3 != null)
        {
            Vec3d dir = new Vec3d(point2[0] - point3[0], 0, point2[1] - point3[1]);
            final double distance = dir.length() + dr;
            dir = dir.normalize();
            for (double i = 0; i < distance; i++)
            {
                final int tx = point3[0] + (int) (dir.x * i) - x;
                final int tz = point3[1] + (int) (dir.z * i) - z;
                if (Math.abs(tx) < width && Math.abs(tz) < width) return true;
            }
        }
        if (point2 != null && point4 != null)
        {
            Vec3d dir = new Vec3d(point2[0] - point4[0], 0, point2[1] - point4[1]);
            final double distance = dir.length() + dr;
            dir = dir.normalize();
            for (double i = 0; i < distance; i++)
            {
                final int tx = point4[0] + (int) (dir.x * i) - x;
                final int tz = point4[1] + (int) (dir.z * i) - z;
                if (Math.abs(tx) < width && Math.abs(tz) < width) return true;
            }
        }
        if (point4 != null && point3 != null)
        {
            Vec3d dir = new Vec3d(point4[0] - point3[0], 0, point4[1] - point3[1]);
            final double distance = dir.length() + dr;
            dir = dir.normalize();
            for (double i = 0; i < distance; i++)
            {
                final int tx = point3[0] + (int) (dir.x * i) - x;
                final int tz = point3[1] + (int) (dir.z * i) - z;
                if (Math.abs(tx) < width && Math.abs(tz) < width) return true;
            }
        }

        return false;
    }

    public boolean hasRoad(final int xAbs, final int h, final int zAbs)
    {
        final Set<WorldConstruction> cons = this.dorfs.getConstructionsForCoords(xAbs, zAbs);

        if (cons == null || cons.isEmpty()) return false;

        final boolean in = false;
        for (final WorldConstruction con : cons)
            if (con.type == DorfMap.ConstructionType.ROAD || con.type == ConstructionType.BRIDGE) if (con.isInConstruct(
                    xAbs, h, zAbs)) return true;
        return in;
    }

    public void debugPrint(final int x, final int z)
    {
        final int embarkX = this.roundToEmbark(x);
        final int embarkZ = this.roundToEmbark(z);

        if (this.respectsSites && this.isInSite(x, z)) Dorfgen.LOGGER.info("Embark location x: " + embarkX + " z: "
                + embarkZ + " is in a site");

        if (this.hasRoad(x, this.minY, z)) Dorfgen.LOGGER.info("Embark location x: " + embarkX + " z: " + embarkZ
                + " has a road");

        if (this.dorfs.getConstructionsForCoords(x, z) != null) for (final WorldConstruction constr : this.dorfs
                .getConstructionsForCoords(x, z))
            if (constr.isInConstruct(x, this.minY % 16, z))
            {
                Dorfgen.LOGGER.info("Location x: " + x + " z: " + z + " is in a construction");
                Dorfgen.LOGGER.info("    Construction is " + constr.toString());
            }

        if (this.dorfs.getConstructionsForCoords(embarkX, embarkZ) != null)
            for (final WorldConstruction constr : this.dorfs.getConstructionsForCoords(embarkX, embarkZ))
            if (constr.isInConstruct(embarkX, this.minY % 16, embarkZ))
            {
                Dorfgen.LOGGER.info("Location x: " + embarkX + " z: " + embarkZ + " is in a construction");
                Dorfgen.LOGGER.info("    Construction is " + constr.toString());
            }

        if (this.isNearSiteRoadEnd(x, z))
        {
            Dorfgen.LOGGER.info("Embark location x: " + embarkX + " z: " + embarkZ + " is near a site road end");
            final int[] roadEnd = this.getSiteRoadEnd(x, z);
            Dorfgen.LOGGER.info("Site road end is at x: " + roadEnd[0] + " z: " + roadEnd[1]);

            int minDistSqr = Integer.MAX_VALUE, dist;
            int x1, z1;
            int embarkX1 = 0, embarkZ1 = 0;
            final int roadEndX = roadEnd[0];
            final int roadEndZ = roadEnd[1];

            for (int xsearch = -RoadMaker.ROAD_SEARCH_AREA; xsearch <= RoadMaker.ROAD_SEARCH_AREA; xsearch++)
                for (int zsearch = -RoadMaker.ROAD_SEARCH_AREA; zsearch <= RoadMaker.ROAD_SEARCH_AREA; zsearch++)
                {
                    x1 = this.roundToEmbark(roadEndX + xsearch * this.scale);
                    z1 = this.roundToEmbark(roadEndZ + zsearch * this.scale);

                    if (this.respectsSites && this.isInSite(x1, z1)) continue;
                    if (!this.hasRoad(x1, this.minY, z1)) continue;

                    dist = (x1 - roadEndX) * (x1 - roadEndX) + (z1 - roadEndZ) * (z1 - roadEndZ);

                    if (dist < minDistSqr)
                    {
                        minDistSqr = dist;
                        embarkX1 = x1;
                        embarkZ1 = z1;
                    }
                }
            if (minDistSqr != Integer.MAX_VALUE)
            {
                Dorfgen.LOGGER.info("Nearest embark to road end found at x: " + embarkX1 + " z: " + embarkZ1);

                Direction closestdir = EAST;
                double distSqr2;
                double minDistSqr2 = Integer.MAX_VALUE;

                final double endX = roadEndX - embarkX;
                final double endZ = roadEndZ - embarkZ;

                final boolean[] dirs = this.getRoadDirection(embarkX, embarkZ);

                for (final Direction dir : RoadMaker.DIRS)
                {
                    if (!dirs[this.dirToIndex(dir)]) continue;
                    distSqr2 = (this.DIR_TO_RELX[this.dirToIndex(dir)] - endX) * (this.DIR_TO_RELX[this.dirToIndex(dir)]
                            - endX) + (this.DIR_TO_RELZ[this.dirToIndex(dir)] - endZ) * (this.DIR_TO_RELZ[this
                                    .dirToIndex(dir)] - endZ);
                    if (distSqr2 < minDistSqr2)
                    {
                        minDistSqr2 = distSqr2;
                        closestdir = dir;
                    }
                }

                if (closestdir == EAST) Dorfgen.LOGGER.info("Closest dir is east");
                if (closestdir == WEST) Dorfgen.LOGGER.info("Closest dir is west");
                if (closestdir == NORTH) Dorfgen.LOGGER.info("Closest dir is north");
                if (closestdir == SOUTH) Dorfgen.LOGGER.info("Closest dir is south");

                Dorfgen.LOGGER.info("    with distance " + minDistSqr2);
            }
        }
    }

    public void buildRoads(final IChunk blocks, final Mutable pos, final int minY, final int maxY)
    {
        this.minY = minY;
        this.maxY = maxY;
        final ChunkPos cpos = blocks.getPos();
        final int x = this.dorfs.shiftX(cpos.getXStart());
        final int z = this.dorfs.shiftZ(cpos.getZStart());

        int h = 0;
        int hMin = Integer.MAX_VALUE;
        int hMax = Integer.MIN_VALUE;
        final int dh = 2;
        final int dr = 16;

        for (int i = 0; i < 16; i++)
            for (int j = 0; j < 16; j++)
            {
                int x1 = x + i;
                int z1 = z + j;
                int x2 = x + i + dr;
                int z2 = z + j + dr;
                final boolean[] dirs = this.getRoadDirection(this.dorfs.unShiftX(x1), this.dorfs.unShiftZ(z1));
                boolean skip = false;
                if (x2 / this.scale + x2 % this.scale >= this.dorfs.waterMap.length || z2 / this.scale + z2
                        % this.scale >= this.dorfs.waterMap[0].length)
                {
                    h = 1;
                    skip = true;
                }
                else
                {
                    if (x1 < 0) x1 = 0;
                    if (z1 < 0) z1 = 0;
                    if (x1 / this.scale >= this.dorfs.biomeMap.length) x1 = this.dorfs.biomeMap.length * this.scale - 1;
                    if (z1 / this.scale >= this.dorfs.biomeMap[0].length) z1 = this.dorfs.biomeMap[0].length
                            * this.scale - 1;

                    h = this.elevationInterpolator.interpolate(this.dorfs.elevationMap, x1, z1, this.scale);
                    if (x1 - dr > 0 && z1 - dr > 0)
                    {
                        if (dirs[0] || dirs[1])
                        {
                            Direction side = RoadMaker.DIRS[0].rotateY();
                            for (int r = 1; r <= dr; r++)
                            {
                                x2 = x1 + side.getXOffset() * r;
                                z2 = z1 + side.getZOffset() * r;
                                if (!this.hasRoad(this.dorfs.unShiftX(x2), h, this.dorfs.unShiftZ(z2))) break;
                                final int h2 = this.elevationInterpolator.interpolate(this.dorfs.elevationMap, x2, z2,
                                        this.scale);
                                hMin = Math.min(hMin, h2);
                                hMax = Math.max(hMax, h2);
                            }
                            side = RoadMaker.DIRS[1].rotateY();
                            for (int r = 1; r <= dr; r++)
                            {
                                x2 = x1 + side.getXOffset() * r;
                                z2 = z1 + side.getZOffset() * r;
                                if (!this.hasRoad(this.dorfs.unShiftX(x2), h, this.dorfs.unShiftZ(z2))) break;
                                final int h2 = this.elevationInterpolator.interpolate(this.dorfs.elevationMap, x2, z2,
                                        this.scale);
                                hMin = Math.min(hMin, h2);
                                hMax = Math.max(hMax, h2);
                            }
                        }
                        if (dirs[2] || dirs[3])
                        {
                            Direction side = RoadMaker.DIRS[2].rotateY();
                            for (int r = 1; r <= dr; r++)
                            {
                                x2 = x1 + side.getXOffset() * r;
                                z2 = z1 + side.getZOffset() * r;
                                if (!this.hasRoad(this.dorfs.unShiftX(x2), h, this.dorfs.unShiftZ(z2))) break;
                                final int h2 = this.elevationInterpolator.interpolate(this.dorfs.elevationMap, x2, z2,
                                        this.scale);
                                hMin = Math.min(hMin, h2);
                                hMax = Math.max(hMax, h2);
                            }
                            side = RoadMaker.DIRS[3].rotateY();
                            for (int r = 1; r <= dr; r++)
                            {
                                x2 = x1 + side.getXOffset() * r;
                                z2 = z1 + side.getZOffset() * r;
                                if (!this.hasRoad(this.dorfs.unShiftX(x2), h, this.dorfs.unShiftZ(z2))) break;
                                final int h2 = this.elevationInterpolator.interpolate(this.dorfs.elevationMap, x2, z2,
                                        this.scale);
                                hMin = Math.min(hMin, h2);
                                hMax = Math.max(hMax, h2);
                            }
                        }
                    }
                    if (Math.abs(hMax - hMin) < dh) h = hMin;
                }
                if (skip || h > maxY + 32 || h < minY - 32) continue;

                if (this.respectsSites && this.isInSite(this.dorfs.unShiftX(x1), this.dorfs.unShiftZ(z1))) continue;
                if (this.isInRoad(this.dorfs.unShiftX(x1), h, this.dorfs.unShiftZ(z1))) this.safeSetToRoad(i, j, h,
                        blocks, pos);
            }
    }

    public boolean[] getRoadDirection(final int xAbs, final int zAbs)
    {
        final boolean[] ret = new boolean[4];

        final Set<WorldConstruction> constructs = this.dorfs.getConstructionsForCoords(xAbs, zAbs);

        if (constructs == null) return ret;

        for (final WorldConstruction con : constructs)
        {
            if (!con.isInConstruct(xAbs, this.minY % 16, zAbs)) continue;

            if (con.isInConstruct(xAbs - this.scale, this.minY % 16, zAbs)) ret[1] = true;
            if (con.isInConstruct(xAbs + this.scale, this.minY % 16, zAbs)) ret[0] = true;
            if (con.isInConstruct(xAbs, this.minY % 16, zAbs - this.scale)) ret[2] = true;
            if (con.isInConstruct(xAbs, this.minY % 16, zAbs + this.scale)) ret[3] = true;
        }
        return ret;
    }

}