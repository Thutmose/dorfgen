package dorfgen.worldgen.common;

import java.util.HashSet;

import dorfgen.conversion.DorfMap;
import dorfgen.conversion.DorfMap.Site;
import dorfgen.conversion.Interpolator.BicubicInterpolator;
import dorfgen.conversion.SiteMapColours;
import dorfgen.conversion.SiteStructureGenerator;
import dorfgen.conversion.SiteStructureGenerator.SiteStructures;
import dorfgen.conversion.SiteStructureGenerator.StructureSpace;
import dorfgen.conversion.SiteTerrain;
import net.minecraft.block.Block;
import net.minecraft.block.state.IBlockState;
import net.minecraft.init.Biomes;
import net.minecraft.init.Blocks;
import net.minecraft.world.World;
import net.minecraft.world.biome.Biome;
import net.minecraft.world.chunk.ChunkPrimer;

public class SiteMaker
{

    public BicubicInterpolator   bicubicInterpolator = new BicubicInterpolator();
    final DorfMap                dorfs;
    final SiteStructureGenerator structureGen;
    private int                  scale               = 0;

    public SiteMaker(DorfMap map, SiteStructureGenerator gen)
    {
        dorfs = map;
        this.structureGen = gen;
        setScale(map.scale);
    }

    public void setScale(int scale)
    {
        this.scale = scale;
    }

    private SiteMapColours getSiteMapColour(Site s, int x, int z)
    {
        int offset = scale / 2, rgb;

        if (s.rgbmap == null || !s.isInSite(x, z)) return null;

        int shiftX = (x - s.corners[0][0] * scale - offset) * SiteStructureGenerator.SITETOBLOCK / scale;
        int shiftZ = (z - s.corners[0][1] * scale - offset) * SiteStructureGenerator.SITETOBLOCK / scale;
        if (shiftX >= s.rgbmap.length || shiftZ >= s.rgbmap[0].length) return null;
        if (shiftX < 0 || shiftZ < 0) return null;
        rgb = s.rgbmap[shiftX][shiftZ];
        SiteMapColours siteCol = SiteMapColours.getMatch(rgb);

        return siteCol;
    }

    public void buildSites(World world, int chunkX, int chunkZ, ChunkPrimer blocks, Biome[] biomes, int minY, int maxY)
    {
        int width = (scale / SiteStructureGenerator.SITETOBLOCK);
        if (dorfs.structureMap.length == 0 || width == 0) return;
        int x = dorfs.shiftX(chunkX * 16);
        int z = dorfs.shiftZ(chunkZ * 16);
        int x1, z1, h;

        for (int i1 = 0; i1 < 16; i1++)
        {
            for (int k1 = 0; k1 < 16; k1++)
            {
                x1 = (x + i1);// / scale;
                z1 = (z + k1);// / scale;

                HashSet<Site> sites = dorfs.getSiteForCoords(dorfs.unShiftX(x1), dorfs.unShiftZ(z1));

                if (sites == null) continue;
                for (Site s : sites)
                {
                    SiteMapColours siteCol = getSiteMapColour(s, x1, z1);
                    if (siteCol == null) continue;

                    h = bicubicInterpolator.interpolate(dorfs.elevationMap, x1, z1, scale);
                    int j = h - 1;

                    if (Math.abs(j - minY) > 8) return;
                    if (Math.abs(maxY - j) > 8) return;

                    SiteStructures structs = structureGen.getStructuresForSite(s);
                    StructureSpace struct = structs.getStructure(x1, z1, scale);
                    if (struct != null)
                    {// There is a stucture here, it should handle connections
                     // itself.
                        continue;
                    }

                    IBlockState[] repBlocks = SiteMapColours.getSurfaceBlocks(siteCol);

                    IBlockState surface = repBlocks[1];
                    IBlockState above = repBlocks[2];

                    boolean wall = siteCol == SiteMapColours.TOWNWALL;
                    boolean roof = siteCol.toString().contains("ROOF");
                    boolean farm = siteCol.toString().contains("FARM");
                    if (farm) biomes[i1 + 16 * k1] = Biomes.PLAINS;

                    if (surface == null && siteCol.toString().contains("ROOF"))
                        surface = Blocks.BRICK_BLOCK.getDefaultState();

                    if (surface == null) continue;
                    blocks.setBlockState(i1, j - minY, k1, surface);
                    blocks.setBlockState(i1, j - 1 - minY, k1, repBlocks[0]);
                    if (above != null) blocks.setBlockState(i1, j + 1, k1, above);
                    boolean tower = siteCol.toString().contains("TOWER");
                    if (wall || roof)
                    {
                        int j1 = j;
                        int num = tower ? 10 : 3;
                        while (j1 < h + 1)
                        {
                            j1 = j1 + 1;
                            blocks.setBlockState(i1, j1 - minY, k1, Blocks.AIR.getDefaultState());
                            blocks.setBlockState(i1, h + num - minY, k1, surface);
                        }
                        j1 = j;
                        if (wall)
                        {
                            while (j1 < h + num)
                            {
                                j1 = j1 + 1;
                                blocks.setBlockState(i1, j1 - minY, k1, surface);
                            }
                        }
                    }

                    if (siteCol.toString().contains("ROAD"))
                    {
                        if (i1 > 0 && i1 < 15 && k1 > 0 && k1 < 15)
                        {
                            h = bicubicInterpolator.interpolate(dorfs.elevationMap, x1, z1, scale);
                            int h2;

                            SiteMapColours px, nx, pz, nz;
                            px = getSiteMapColour(s, x1 + 1, z1);
                            nx = getSiteMapColour(s, x1 - 1, z1);
                            pz = getSiteMapColour(s, x1, z1 + 1);
                            nz = getSiteMapColour(s, x1, z1 - 1);

                            if (px != null && !px.toString().contains("ROAD") && z1 % 8 == 0)
                            {
                                h2 = bicubicInterpolator.interpolate(dorfs.elevationMap, x1 + 1, z1, scale);
                                blocks.setBlockState(i1, h2 - minY, k1, Blocks.TORCH.getDefaultState());
                            }

                            if (nx != null && !nx.toString().contains("ROAD") && z1 % 8 == 0)
                            {
                                h2 = bicubicInterpolator.interpolate(dorfs.elevationMap, x1 - 1, z1, scale);
                                blocks.setBlockState(i1, h2 - minY, k1, Blocks.TORCH.getDefaultState());
                            }

                            if (pz != null && !pz.toString().contains("ROAD") && x1 % 8 == 0)
                            {
                                h2 = bicubicInterpolator.interpolate(dorfs.elevationMap, x1, z1 + 1, scale);
                                blocks.setBlockState(i1, h2 - minY, k1, Blocks.TORCH.getDefaultState());
                            }

                            if (nz != null && !nz.toString().contains("ROAD") && x1 % 8 == 0)
                            {
                                h2 = bicubicInterpolator.interpolate(dorfs.elevationMap, x1, z1 - 1, scale);
                                blocks.setBlockState(i1, h2 - minY, k1, Blocks.TORCH.getDefaultState());
                            }
                        }
                    }
                }
            }
        }
    }

    public static Block getSurfaceBlockForSite(SiteTerrain site, int num)
    {
        switch (site)
        {
        case BUILDINGS:
            return num == 0 ? Blocks.BRICK_BLOCK : null;
        case WALLS:
            return Blocks.STONEBRICK;
        case FARMYELLOW:
            return num == 0 ? Blocks.SAND : null;
        case FARMORANGE:
            return num == 0 ? Blocks.DIRT : null;
        case FARMLIMEGREEN:
            return num == 0 ? Blocks.CLAY : null;
        case FARMORANGELIGHT:
            return num == 0 ? Blocks.HARDENED_CLAY : null;
        case FARMGREEN:
            return num == 0 ? Blocks.STAINED_HARDENED_CLAY : null;
        default:
            return null;
        }
    }
}
