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
import net.minecraft.world.IWorld;
import net.minecraft.world.chunk.IChunk;

public class RiverMaker extends PathMaker
{
    public RiverMaker(final DorfMap map, final SiteStructureGenerator gen)
    {
        super(map, gen);
    }

    public void makeRiversForChunk(final IChunk primer, final IWorld worldIn, final Mutable pos, final int minY,
            final int maxY)
    {
        final ChunkPos cpos = primer.getPos();
        // these are world coordinates
        int x0, z0;

        x0 = cpos.getXStart();
        z0 = cpos.getZStart();

        final int x = this.dorfs.shiftX(x0);
        final int z = this.dorfs.shiftZ(z0);

        // These are in dorfmap coords
        int x1, z1;

        int h;
        final int[][] heightmap = this.dorfs.elevationMap;
        boolean oob = false;
        final int width = this.getWidth();
        final int seaLevel = primer.getWorldForge() != null ? primer.getWorldForge().getSeaLevel()
                : this.dorfs.sigmoid.elevationSigmoid(this.dorfs.seaLevel);
        final int topAir = 8;
        for (int dx = 0; dx < 16; dx++)
            for (int dz = 0; dz < 16; dz++)
            {
                x1 = x + dx;
                z1 = z + dz;

                if (x1 < 0) x1 = 0;
                if (z1 < 0) z1 = 0;
                if (x1 / this.scale >= this.dorfs.biomeMap.length) x1 = this.dorfs.biomeMap.length * this.scale - 1;
                if (z1 / this.scale >= this.dorfs.biomeMap[0].length) z1 = this.dorfs.biomeMap[0].length * this.scale
                        - 1;

                h = this.riverInterpolator.interpolate(heightmap, x1, z1, this.scale);
                oob = h < minY - 16 || h > maxY + topAir;
                if (oob) continue;
                final boolean river = this.isInRiver(x1, z1, 4 * width);
                if (!river) continue;
                if (!this.isInRiver(x1, z1, width)) continue;
                int y;
                h = h - 1 - minY;
                for (int i = -2; i < topAir; i++)
                {
                    y = h + i;
                    final boolean water = i < 0 || y <= seaLevel;
                    final BlockState state = water ? Blocks.WATER.getDefaultState() : Blocks.AIR.getDefaultState();
                    // Carve air above, except if below sea level
                    if (y > minY && y < maxY)
                    {
                        primer.setBlockState(pos.setPos(dx, y, dz), state, false);
                        // Tick the top layers of water
                        if (i >= -1 && water) worldIn.getPendingFluidTicks().scheduleTick(pos.setPos(x0 + dx, y, z0
                                + dz), state.getFluidState().getFluid(), 0);
                    }

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
        final int kx = x / this.scale;
        final int kz = z / this.scale;
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