package dorfgen.world.feature;

import static net.minecraft.util.Direction.EAST;
import static net.minecraft.util.Direction.NORTH;
import static net.minecraft.util.Direction.SOUTH;
import static net.minecraft.util.Direction.WEST;

import java.awt.Color;
import java.util.Set;

import dorfgen.conversion.DorfMap;
import dorfgen.conversion.DorfMap.Site;
import dorfgen.conversion.SiteStructureGenerator;
import dorfgen.conversion.SiteStructureGenerator.RiverExit;
import dorfgen.conversion.SiteStructureGenerator.SiteStructures;
import net.minecraft.block.BlockState;
import net.minecraft.block.Blocks;
import net.minecraft.util.Direction;
import net.minecraft.util.math.BlockPos.Mutable;
import net.minecraft.util.math.ChunkPos;
import net.minecraft.util.math.Vec3d;
import net.minecraft.world.World;
import net.minecraft.world.chunk.IChunk;

public class RiverMaker extends PathMaker
{
    public RiverMaker(final DorfMap map, final SiteStructureGenerator gen)
    {
        super(map, gen);
    }

    public void postInitRivers(final World world, final int chunkX, final int chunkZ, final int minY, final int maxY)
    {
        final int x0 = chunkX * 16;
        final int z0 = chunkZ * 16;
        final int x = this.dorfs.shiftX(x0);
        final int z = this.dorfs.shiftZ(z0);
        int x1, z1;
        final Mutable pos = new Mutable();
        final Mutable pos2 = new Mutable();
        final int width = this.getWidth();
        for (int i1 = 1; i1 < 15; i1++)
            for (int k1 = 1; k1 < 15; k1++)
            {
                x1 = x + i1;
                z1 = z + k1;
                if (this.isInRiver(x1, z1, width)) for (int y = minY; y <= maxY; y++)
                {
                    pos.setPos(x0 + i1, y, z0 + k1);
                    pos2.setPos(x0 + i1, y == minY ? y + 1 : y - 1, z0 + k1);
                    final BlockState state = world.getBlockState(pos);
                    final BlockState state2 = world.getBlockState(pos);
                    if (state.getMaterial().isLiquid()) world.neighborChanged(pos, state2.getBlock(), pos2);
                }
            }
    }

    public void makeRiversForChunk(final IChunk primer, final Mutable pos, final int minY, final int maxY)
    {
        final ChunkPos cpos = primer.getPos();
        final int x = this.dorfs.shiftX(cpos.getXStart());
        final int z = this.dorfs.shiftZ(cpos.getZStart());

        // These are in dorfmap coords
        int x1, z1;

        // These are same coords shifted over 1
        int x2, z2;

        int h;

        boolean skip = false;
        boolean oob = false;
        final int width = this.getWidth();
        for (int i1 = 0; i1 < 16; i1++)
            for (int k1 = 0; k1 < 16; k1++)
            {
                x1 = x + i1;
                z1 = z + k1;
                x2 = x + i1 + 1;
                z2 = z + k1 + 1;

                if (x1 < 0) x1 = 0;
                if (z1 < 0) z1 = 0;
                if (x1 / this.scale >= this.dorfs.biomeMap.length) x1 = this.dorfs.biomeMap.length * this.scale - 1;
                if (z1 / this.scale >= this.dorfs.biomeMap[0].length) z1 = this.dorfs.biomeMap[0].length * this.scale
                        - 1;

                if (x2 / this.scale + x2 % this.scale >= this.dorfs.riverMap.length || z2 / this.scale + z2
                        % this.scale >= this.dorfs.riverMap[0].length)
                {
                    h = 1;
                    skip = true;
                }
                else h = this.riverInterpolator.interpolate(this.dorfs.riverMap, x1, z1, this.scale);
                oob = h < minY - 16 || h > maxY + 32 * this.dorfs.cubicHeightScale;
                if (oob || skip) continue;
                final boolean river = this.isInRiver(x1, z1, 4 * width);
                if (!river) continue;
                if (!this.isInRiver(x1, z1, width)) continue;
                int y = h = h - 1 - minY;
                for (int i = 2; i < 5; i++)
                {
                    y = h - i;
                    pos.setPos(i1, y, k1);
                    if (y >= this.dorfs.yMin) primer.setBlockState(pos, Blocks.WATER.getDefaultState(), false);
                }
                for (int i = -1; i < 32 * this.dorfs.cubicHeightScale; i++)
                {
                    y = h + i;
                    pos.setPos(i1, y, k1);
                    if (y >= this.dorfs.yMin) primer.setBlockState(pos, Blocks.AIR.getDefaultState(), false);
                }
            }
    }

    static Color       STRMAPRIVER = new Color(0, 192, 255);
    static Direction[] DIRS        = { EAST, WEST, NORTH, SOUTH };

    public boolean[] getRiverDirection(final int xAbs, final int zAbs)
    {
        final boolean[] ret = new boolean[4];

        if (!this.isRiver(xAbs, zAbs, true)) return ret;
        if (this.isRiver(xAbs - this.scale, zAbs, false)) ret[1] = true;
        if (this.isRiver(xAbs + this.scale, zAbs, false)) ret[0] = true;
        if (this.isRiver(xAbs, zAbs - this.scale, false)) ret[2] = true;
        if (this.isRiver(xAbs, zAbs + this.scale, false)) ret[3] = true;
        return ret;
    }

    public boolean isRiver(final int x, final int z, final boolean onlyRiverMap)
    {
        final int kx = x / this.scale;// Abs/(scale);
        final int kz = z / this.scale;// Abs/(scale);
        final int key = kx + 8192 * kz;
        if (kx >= this.dorfs.waterMap.length || kz >= this.dorfs.waterMap[0].length) return false;
        if (kx < 0 || kz < 0) return false;
        final int r = this.dorfs.riverMap[kx][kz];
        boolean river = r > 0;
        if (!river && !onlyRiverMap) river = this.waterInterpolator.interpolate(this.dorfs.waterMap, x, z,
                this.scale) > 0;
        if (river || this.respectsSites) return river;
        final Set<Site> ret = this.dorfs.sitesByCoord.get(key);
        if (ret != null) for (final Site s : ret)
        {
            if (!s.isInSite(x, z)) continue;

            final SiteStructures structs = this.structureGen.getStructuresForSite(s);
            if (!structs.rivers.isEmpty()) for (final RiverExit riv : structs.rivers)
            {
                final int[] exit = riv.getEdgeMid(s, this.scale);
                final int dx = exit[0] - x;
                final int dz = exit[1] - z;
                if (dx * dx + dz * dz < this.scale * this.scale / 4) return true;
            }
        }
        return river;
    }

    public boolean isInRiver(int x1, int z1, final int width)
    {
        final int x = x1, z = z1;
        boolean river = false;
        final int kx = x1 / this.scale;
        final int kz = z1 / this.scale;
        final int offset = this.scale / 2;
        int key = kx + 8192 * kz;
        Site site;
        Set<Site> ret = this.dorfs.sitesByCoord.get(key);
        boolean hasRivers = false;
        if (this.respectsSites && ret != null) for (final Site s : ret)
        {
            if (!s.isInSite(x1, z1)) continue;

            final SiteStructures structs = this.structureGen.getStructuresForSite(s);
            if (!structs.rivers.isEmpty())
            {
                hasRivers = true;
                break;
            }
        }
        final boolean[] dirs = this.getRiverDirection(x1, z1);
        river = dirs[0] || dirs[1] || dirs[2] || dirs[3];
        int[] point1 = null;
        int[] point2 = null;
        int[] point3 = null;
        int[] point4 = null;

        if (river && !hasRivers)
        {
            x1 = kx * this.scale;
            z1 = kz * this.scale;
            if (dirs[3])
            {
                key = kx + 8192 * (kz + 1);
                ret = null;
                if (this.respectsSites) ret = this.dorfs.sitesByCoord.get(key);
                int[] nearest = null;
                if (ret != null) for (final Site s : ret)
                {
                    site = s;
                    final SiteStructures stuff = this.structureGen.getStructuresForSite(site);

                    int[] temp;
                    int dist = Integer.MAX_VALUE;
                    for (final RiverExit exit : stuff.rivers)
                    {
                        temp = exit.getEdgeMid(site, this.scale);
                        final int tempDist = (temp[0] - x1) * (temp[0] - x1) + (temp[1] - z1) * (temp[1] - z1);
                        if (tempDist < dist)
                        {
                            nearest = temp;
                            dist = tempDist;
                        }
                    }
                }
                if (nearest == null) nearest = new int[] { kx * this.scale + offset, (kz + 1) * this.scale };
                if (ret == null || this.isRiver(nearest[0], nearest[1], false)) point1 = nearest;
            }
            if (dirs[1])
            {
                key = kx - 1 + 8192 * kz;
                ret = null;
                if (this.respectsSites) ret = this.dorfs.sitesByCoord.get(key);
                int[] nearest = null;
                if (ret != null) for (final Site s : ret)
                {
                    site = s;
                    final SiteStructures stuff = this.structureGen.getStructuresForSite(site);

                    int[] temp;
                    int dist = Integer.MAX_VALUE;
                    for (final RiverExit exit : stuff.rivers)
                    {
                        temp = exit.getEdgeMid(site, this.scale);
                        final int tempDist = (temp[0] - x1) * (temp[0] - x1) + (temp[1] - z1) * (temp[1] - z1);
                        if (tempDist < dist)
                        {
                            nearest = temp;
                            dist = tempDist;
                        }
                    }
                }
                if (nearest == null) nearest = new int[] { (kx - 1) * this.scale + this.scale, kz * this.scale
                        + offset };
                if (ret == null || this.isRiver(nearest[0], nearest[1], false)) point2 = nearest;
            }
            if (dirs[2])
            {
                key = kx + 8192 * (kz - 1);
                ret = null;
                if (this.respectsSites) ret = this.dorfs.sitesByCoord.get(key);
                int[] nearest = null;
                if (ret != null) for (final Site s : ret)
                {
                    site = s;
                    final SiteStructures stuff = this.structureGen.getStructuresForSite(site);

                    int[] temp;
                    int dist = Integer.MAX_VALUE;
                    for (final RiverExit exit : stuff.rivers)
                    {
                        temp = exit.getEdgeMid(site, this.scale);
                        final int tempDist = (temp[0] - x1) * (temp[0] - x1) + (temp[1] - z1) * (temp[1] - z1);
                        if (tempDist < dist)
                        {
                            nearest = temp;
                            dist = tempDist;
                        }
                    }
                }
                if (nearest == null) nearest = new int[] { kx * this.scale + offset, (kz - 1) * this.scale
                        + this.scale };
                if (ret == null || this.isRiver(nearest[0], nearest[1], false)) point3 = nearest;
            }
            if (dirs[0])
            {
                key = kx + 1 + 8192 * kz;
                int[] nearest = null;
                ret = null;
                if (this.respectsSites) ret = this.dorfs.sitesByCoord.get(key);
                if (ret != null) for (final Site s : ret)
                {
                    site = s;
                    final SiteStructures stuff = this.structureGen.getStructuresForSite(site);

                    int[] temp;
                    int dist = Integer.MAX_VALUE;
                    for (final RiverExit exit : stuff.rivers)
                    {
                        temp = exit.getEdgeMid(site, this.scale);
                        final int tempDist = (temp[0] - x1) * (temp[0] - x1) + (temp[1] - z1) * (temp[1] - z1);
                        if (tempDist < dist)
                        {
                            nearest = temp;
                            dist = tempDist;
                        }
                    }
                }
                if (nearest == null) nearest = new int[] { (kx + 1) * this.scale, kz * this.scale + offset };

                if (ret == null || this.isRiver(nearest[0], nearest[1], false)) point4 = nearest;

            }
        }

        try
        {
            //@formatter:off
//            System.out.println(Arrays.toString(point1) +
//                    "  " + Arrays.toString(point2) +
//                    "  " + Arrays.toString(point3)+
//                    "  " + Arrays.toString(point4) +
//                    "  " + river);
            //@formatter:on
        }
        catch (final Exception e)
        {
            // TODO Auto-generated catch block
            e.printStackTrace();
        }

        if (point1 != null && point2 != null)
        {
            Vec3d dir = new Vec3d(point1[0] - point2[0], 0, point1[1] - point2[1]);
            final double distance = dir.length();
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
            final double distance = dir.length();
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
            final double distance = dir.length();
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
            final double distance = dir.length();
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
            final double distance = dir.length();
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
            final double distance = dir.length();
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

    public int getWidth()
    {
        int width = 3 * this.scale / SiteStructureGenerator.SITETOBLOCK;
        width = Math.max(width, 3);
        return width;
    }
}