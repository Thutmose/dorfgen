package dorfgen.worldgen.common;

import static net.minecraft.util.EnumFacing.EAST;
import static net.minecraft.util.EnumFacing.NORTH;
import static net.minecraft.util.EnumFacing.SOUTH;
import static net.minecraft.util.EnumFacing.WEST;

import java.awt.Color;
import java.util.HashSet;

import javax.vecmath.Vector3d;

import dorfgen.conversion.DorfMap;
import dorfgen.conversion.DorfMap.Site;
import dorfgen.conversion.SiteStructureGenerator;
import dorfgen.conversion.SiteStructureGenerator.RiverExit;
import dorfgen.conversion.SiteStructureGenerator.SiteStructures;
import net.minecraft.block.state.IBlockState;
import net.minecraft.init.Biomes;
import net.minecraft.init.Blocks;
import net.minecraft.util.EnumFacing;
import net.minecraft.util.math.BlockPos.MutableBlockPos;
import net.minecraft.world.World;
import net.minecraft.world.biome.Biome;
import net.minecraft.world.chunk.ChunkPrimer;

public class RiverMaker extends PathMaker
{

    public RiverMaker(DorfMap map, SiteStructureGenerator gen)
    {
        super(map, gen);
    }

    public void postInitRivers(World world, int chunkX, int chunkZ, int minY, int maxY)
    {
        int x0 = chunkX * 16;
        int z0 = chunkZ * 16;
        int x = dorfs.shiftX(x0);
        int z = dorfs.shiftZ(z0);
        int x1, z1;
        MutableBlockPos pos = new MutableBlockPos();
        MutableBlockPos pos2 = new MutableBlockPos();
        int width = getWidth();
        for (int i1 = 1; i1 < 15; i1++)
        {
            for (int k1 = 1; k1 < 15; k1++)
            {
                x1 = (x + i1);
                z1 = (z + k1);
                if (isInRiver(x1, z1, width)) for (int y = minY; y <= maxY; y++)
                {
                    pos.setPos(x0 + i1, y, z0 + k1);
                    pos2.setPos(x0 + i1, y == minY ? y + 1 : y - 1, z0 + k1);
                    IBlockState state = world.getBlockState(pos);
                    IBlockState state2 = world.getBlockState(pos);
                    if (state.getMaterial().isLiquid())
                    {
                        world.neighborChanged(pos, state2.getBlock(), pos2);
                    }
                }
            }
        }
    }

    public void makeRiversForChunk(World world, int chunkX, int chunkZ, ChunkPrimer primer, Biome[] biomes, int minY,
            int maxY)
    {
        int x = dorfs.shiftX(chunkX * 16);
        int z = dorfs.shiftZ(chunkZ * 16);
        int x1, z1, x2, z2, h;
        boolean skip = false;
        boolean oob = false;
        int width = getWidth();
        for (int i1 = 0; i1 < 16; i1++)
        {
            for (int k1 = 0; k1 < 16; k1++)
            {
                x1 = (x + i1);
                z1 = (z + k1);
                x2 = (x + i1 + 1);
                z2 = (z + k1 + 1);

                if (x2 / scale + x2 % scale >= dorfs.riverMap.length
                        || z2 / scale + z2 % scale >= dorfs.riverMap[0].length)
                {
                    h = 1;
                    skip = true;
                }
                else
                {
                    h = riverInterpolator.interpolate(dorfs.riverMap, x1, z1, scale);
                }
                oob = (h < minY - 16) || (h > maxY + 32 * dorfs.cubicHeightScale);
                if (oob || skip) continue;
                boolean river = isInRiver(x1, z1, 4 * width);
                if (!river) continue;
                biomes[i1 + k1 * 16] = dorfs.biomeList.mutateBiome(Biomes.RIVER, x1, z1, dorfs);
                if (!isInRiver(x1, z1, width)) continue;
                int y = h = h - 1 - minY;
                for (int i = 2; i < 5; i++)
                {
                    y = h - i;
                    if (y >= dorfs.yMin) primer.setBlockState(i1, y, k1, Blocks.WATER.getDefaultState());
                }
                for (int i = -1; i < 32 * dorfs.cubicHeightScale; i++)
                {
                    y = h + i;
                    if (y >= dorfs.yMin) primer.setBlockState(i1, y, k1, Blocks.AIR.getDefaultState());
                }
            }
        }
    }

    static Color        STRMAPRIVER = new Color(0, 192, 255);
    static EnumFacing[] DIRS        = { EAST, WEST, NORTH, SOUTH };

    public boolean[] getRiverDirection(int xAbs, int zAbs)
    {
        boolean[] ret = new boolean[4];

        if (!isRiver(xAbs, zAbs, true)) { return ret; }
        if (isRiver(xAbs - scale, zAbs, false))
        {
            ret[1] = true;
        }
        if (isRiver(xAbs + scale, zAbs, false))
        {
            ret[0] = true;
        }
        if (isRiver(xAbs, zAbs - scale, false))
        {
            ret[2] = true;
        }
        if (isRiver(xAbs, zAbs + scale, false))
        {
            ret[3] = true;
        }
        return ret;
    }

    public boolean isRiver(int x, int z, boolean onlyRiverMap)
    {
        int kx = x / scale;// Abs/(scale);
        int kz = z / scale;// Abs/(scale);
        int key = kx + 8192 * kz;
        if (kx >= dorfs.waterMap.length || kz >= dorfs.waterMap[0].length) { return false; }
        if (kx < 0 || kz < 0) return false;
        int r = dorfs.riverMap[kx][kz];
        boolean river = r > 0;
        if (!river && !onlyRiverMap) river = waterInterpolator.interpolate(dorfs.waterMap, x, z, scale) > 0;
        if (river || respectsSites) return river;
        HashSet<Site> ret = dorfs.sitesByCoord.get(key);
        if (ret != null)
        {
            for (Site s : ret)
            {
                if (!s.isInSite(x, z)) continue;

                SiteStructures structs = structureGen.getStructuresForSite(s);
                if (!structs.rivers.isEmpty())
                {
                    for (RiverExit riv : structs.rivers)
                    {
                        int[] exit = riv.getEdgeMid(s, scale);
                        int dx = exit[0] - x;
                        int dz = exit[1] - z;
                        if (dx * dx + dz * dz < scale * scale / 4) { return true; }
                    }
                }
            }
        }
        return river;
    }

    public boolean isInRiver(int x1, int z1, int width)
    {
        int x = x1, z = z1;
        boolean river = false;
        int kx = x1 / scale;
        int kz = z1 / scale;
        int offset = scale / 2;
        int key = kx + 8192 * kz;
        Site site;
        HashSet<Site> ret = dorfs.sitesByCoord.get(key);
        boolean hasRivers = false;
        if (respectsSites && ret != null)
        {
            for (Site s : ret)
            {
                if (!s.isInSite(x1, z1)) continue;

                SiteStructures structs = structureGen.getStructuresForSite(s);
                if (!structs.rivers.isEmpty())
                {
                    hasRivers = true;
                    break;
                }
            }
        }
        boolean[] dirs = getRiverDirection(x1, z1);
        river = dirs[0] || dirs[1] || dirs[2] || dirs[3];
        int[] point1 = null;
        int[] point2 = null;
        int[] point3 = null;
        int[] point4 = null;

        if (river && !hasRivers)
        {
            x1 = kx * scale;
            z1 = kz * scale;
            if (dirs[3])
            {
                key = kx + 8192 * (kz + 1);
                ret = null;
                if (respectsSites) ret = dorfs.sitesByCoord.get(key);
                int[] nearest = null;
                if (ret != null)
                {
                    for (Site s : ret)
                    {
                        site = s;
                        SiteStructures stuff = structureGen.getStructuresForSite(site);

                        int[] temp;
                        int dist = Integer.MAX_VALUE;
                        for (RiverExit exit : stuff.rivers)
                        {
                            temp = exit.getEdgeMid(site, scale);
                            int tempDist = (temp[0] - x1) * (temp[0] - x1) + (temp[1] - z1) * (temp[1] - z1);
                            if (tempDist < dist)
                            {
                                nearest = temp;
                                dist = tempDist;
                            }
                        }
                    }
                }
                if (nearest == null)
                {
                    nearest = new int[] { kx * scale + offset, (kz + 1) * scale };
                }
                if (ret == null || isRiver(nearest[0], nearest[1], false)) point1 = nearest;
            }
            if (dirs[1])
            {
                key = (kx - 1) + 8192 * (kz);
                ret = null;
                if (respectsSites) ret = dorfs.sitesByCoord.get(key);
                int[] nearest = null;
                if (ret != null)
                {
                    for (Site s : ret)
                    {
                        site = s;
                        SiteStructures stuff = structureGen.getStructuresForSite(site);

                        int[] temp;
                        int dist = Integer.MAX_VALUE;
                        for (RiverExit exit : stuff.rivers)
                        {
                            temp = exit.getEdgeMid(site, scale);
                            int tempDist = (temp[0] - x1) * (temp[0] - x1) + (temp[1] - z1) * (temp[1] - z1);
                            if (tempDist < dist)
                            {
                                nearest = temp;
                                dist = tempDist;
                            }
                        }
                    }
                }
                if (nearest == null)
                {
                    nearest = new int[] { (kx - 1) * scale + scale, (kz) * scale + offset };
                }
                if (ret == null || isRiver(nearest[0], nearest[1], false)) point2 = nearest;
            }
            if (dirs[2])
            {
                key = kx + 8192 * (kz - 1);
                ret = null;
                if (respectsSites) ret = dorfs.sitesByCoord.get(key);
                int[] nearest = null;
                if (ret != null)
                {
                    for (Site s : ret)
                    {
                        site = s;
                        SiteStructures stuff = structureGen.getStructuresForSite(site);

                        int[] temp;
                        int dist = Integer.MAX_VALUE;
                        for (RiverExit exit : stuff.rivers)
                        {
                            temp = exit.getEdgeMid(site, scale);
                            int tempDist = (temp[0] - x1) * (temp[0] - x1) + (temp[1] - z1) * (temp[1] - z1);
                            if (tempDist < dist)
                            {
                                nearest = temp;
                                dist = tempDist;
                            }
                        }
                    }
                }
                if (nearest == null)
                {
                    nearest = new int[] { kx * scale + offset, (kz - 1) * scale + scale };
                }
                if (ret == null || isRiver(nearest[0], nearest[1], false))
                {
                    point3 = nearest;
                }
            }
            if (dirs[0])
            {
                key = (kx + 1) + 8192 * (kz);
                int[] nearest = null;
                ret = null;
                if (respectsSites) ret = dorfs.sitesByCoord.get(key);
                if (ret != null)
                {
                    for (Site s : ret)
                    {
                        site = s;
                        SiteStructures stuff = structureGen.getStructuresForSite(site);

                        int[] temp;
                        int dist = Integer.MAX_VALUE;
                        for (RiverExit exit : stuff.rivers)
                        {
                            temp = exit.getEdgeMid(site, scale);
                            int tempDist = (temp[0] - x1) * (temp[0] - x1) + (temp[1] - z1) * (temp[1] - z1);
                            if (tempDist < dist)
                            {
                                nearest = temp;
                                dist = tempDist;
                            }
                        }
                    }
                }
                if (nearest == null)
                {
                    nearest = new int[] { (kx + 1) * scale, (kz) * scale + offset };
                }

                if (ret == null || isRiver(nearest[0], nearest[1], false)) point4 = nearest;

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
        catch (Exception e)
        {
            // TODO Auto-generated catch block
            e.printStackTrace();
        }

        if (point1 != null && point2 != null)
        {
            Vector3d dir = new Vector3d(point1[0] - point2[0], 0, point1[1] - point2[1]);
            double distance = dir.length();
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
            double distance = dir.length();
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
            double distance = dir.length();
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
            double distance = dir.length();
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
            double distance = dir.length();
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
            double distance = dir.length();
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

    public int getWidth()
    {
        int width = 3 * scale / SiteStructureGenerator.SITETOBLOCK;
        width = Math.max(width, 3);
        return width;
    }
}
