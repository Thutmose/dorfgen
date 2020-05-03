package dorfgen.world.feature;

import java.util.Set;

import dorfgen.conversion.DorfMap;
import dorfgen.conversion.DorfMap.Site;
import dorfgen.conversion.SiteMapColours;
import dorfgen.conversion.SiteStructureGenerator;
import dorfgen.conversion.SiteStructureGenerator.SiteStructures;
import dorfgen.conversion.SiteStructureGenerator.StructureSpace;
import dorfgen.conversion.SiteTerrain;
import dorfgen.util.Interpolator.BicubicInterpolator;
import net.minecraft.block.Block;
import net.minecraft.block.BlockState;
import net.minecraft.block.Blocks;
import net.minecraft.util.math.BlockPos.Mutable;
import net.minecraft.util.math.ChunkPos;
import net.minecraft.world.chunk.IChunk;

public class SiteMaker
{

    public BicubicInterpolator   bicubicInterpolator = new BicubicInterpolator();
    final DorfMap                dorfs;
    final SiteStructureGenerator structureGen;
    private int                  scale               = 0;

    public SiteMaker(final DorfMap map, final SiteStructureGenerator gen)
    {
        this.dorfs = map;
        this.structureGen = gen;
        this.setScale(map.scale);
    }

    public void setScale(final int scale)
    {
        this.scale = scale;
    }

    private SiteMapColours getSiteMapColour(final Site s, final int x, final int z)
    {
        final int offset = this.scale / 2;
        int rgb;

        if (s.rgbmap == null || !s.isInSite(x, z)) return null;

        final int shiftX = (x - s.corners[0][0] * this.scale - offset) * SiteStructureGenerator.SITETOBLOCK
                / this.scale;
        final int shiftZ = (z - s.corners[0][1] * this.scale - offset) * SiteStructureGenerator.SITETOBLOCK
                / this.scale;
        if (shiftX >= s.rgbmap.length || shiftZ >= s.rgbmap[0].length) return null;
        if (shiftX < 0 || shiftZ < 0) return null;
        rgb = s.rgbmap[shiftX][shiftZ];
        final SiteMapColours siteCol = SiteMapColours.getMatch(rgb);

        return siteCol;
    }

    public void buildSites(final IChunk blocks, final Mutable pos, final int minY, final int maxY)
    {
        final int width = this.scale / SiteStructureGenerator.SITETOBLOCK;
        if (this.dorfs.structureMap.length == 0 || width == 0) return;
        final ChunkPos cpos = blocks.getPos();
        final int x = this.dorfs.shiftX(cpos.getXStart());
        final int z = this.dorfs.shiftZ(cpos.getZStart());
        int x1, z1, h;

        for (int dx = 0; dx < 16; dx++)
            for (int dz = 0; dz < 16; dz++)
            {
                x1 = x + dx;
                z1 = z + dz;

                final Set<Site> sites = this.dorfs.getSiteForCoords(this.dorfs.unShiftX(x1), this.dorfs.unShiftZ(z1));

                if (sites == null) continue;
                for (final Site s : sites)
                {
                    final SiteMapColours siteCol = this.getSiteMapColour(s, x1, z1);
                    if (siteCol == null) continue;

                    h = this.bicubicInterpolator.interpolate(this.dorfs.elevationMap, x1, z1, this.scale);
                    final int j = h - 1;

                    // if (Math.abs(j - minY) > 8) return;
                    // if (Math.abs(maxY - j) > 8) return;

                    final SiteStructures structs = this.structureGen.getStructuresForSite(s);
                    final StructureSpace struct = structs.getStructure(x1, z1, this.scale);
                    if (struct != null) // itself.
                        continue;

                    final BlockState[] repBlocks = SiteMapColours.getSurfaceBlocks(siteCol);

                    BlockState surface = repBlocks[1];
                    final BlockState above = repBlocks[2];

                    final boolean wall = siteCol == SiteMapColours.TOWNWALL;
                    final boolean roof = siteCol.toString().contains("ROOF");
                    final boolean farm = siteCol.toString().contains("FARM");
                    if (farm)
                    {
                        // TODO set biome to plains here!
                    }

                    if (surface == null && siteCol.toString().contains("ROOF")) surface = Blocks.BRICKS
                            .getDefaultState();

                    if (surface == null) continue;
                    blocks.setBlockState(pos.setPos(dx, j - minY, dz), surface, false);
                    blocks.setBlockState(pos.setPos(dx, j - 1 - minY, dz), repBlocks[0], false);
                    if (above != null) blocks.setBlockState(pos.setPos(dx, j + 1, dz), above, false);
                    final boolean tower = siteCol.toString().contains("TOWER");
                    if (wall || roof)
                    {
                        int j1 = j;
                        final int num = tower ? 10 : 3;
                        while (j1 < h + 1)
                        {
                            j1 = j1 + 1;
                            blocks.setBlockState(pos.setPos(dx, j1 - minY, dz), Blocks.AIR.getDefaultState(), false);
                            blocks.setBlockState(pos.setPos(dx, h + num - minY, dz), surface, false);
                        }
                        j1 = j;
                        if (wall) while (j1 < h + num)
                        {
                            j1 = j1 + 1;
                            blocks.setBlockState(pos.setPos(dx, j1 - minY, dz), surface, false);
                        }
                    }

                    if (siteCol.toString().contains("ROAD")) if (dx > 0 && dx < 15 && dz > 0 && dz < 15)
                    {
                        h = this.bicubicInterpolator.interpolate(this.dorfs.elevationMap, x1, z1, this.scale);
                        int h2;

                        SiteMapColours px, nx, pz, nz;
                        px = this.getSiteMapColour(s, x1 + 1, z1);
                        nx = this.getSiteMapColour(s, x1 - 1, z1);
                        pz = this.getSiteMapColour(s, x1, z1 + 1);
                        nz = this.getSiteMapColour(s, x1, z1 - 1);

                        if (px != null && !px.toString().contains("ROAD") && z1 % 8 == 0)
                        {
                            h2 = this.bicubicInterpolator.interpolate(this.dorfs.elevationMap, x1 + 1, z1, this.scale);
                            blocks.setBlockState(pos.setPos(dx, h2 - minY, dz), Blocks.TORCH.getDefaultState(), false);
                        }

                        if (nx != null && !nx.toString().contains("ROAD") && z1 % 8 == 0)
                        {
                            h2 = this.bicubicInterpolator.interpolate(this.dorfs.elevationMap, x1 - 1, z1, this.scale);
                            blocks.setBlockState(pos.setPos(dx, h2 - minY, dz), Blocks.TORCH.getDefaultState(), false);
                        }

                        if (pz != null && !pz.toString().contains("ROAD") && x1 % 8 == 0)
                        {
                            h2 = this.bicubicInterpolator.interpolate(this.dorfs.elevationMap, x1, z1 + 1, this.scale);
                            blocks.setBlockState(pos.setPos(dx, h2 - minY, dz), Blocks.TORCH.getDefaultState(), false);
                        }

                        if (nz != null && !nz.toString().contains("ROAD") && x1 % 8 == 0)
                        {
                            h2 = this.bicubicInterpolator.interpolate(this.dorfs.elevationMap, x1, z1 - 1, this.scale);
                            blocks.setBlockState(pos.setPos(dx, h2 - minY, dz), Blocks.TORCH.getDefaultState(), false);
                        }
                    }
                }
            }
    }

    public static Block getSurfaceBlockForSite(final SiteTerrain site, final int num)
    {
        switch (site)
        {
        case BUILDINGS:
            return num == 0 ? Blocks.BRICKS : null;
        case WALLS:
            return Blocks.STONE_BRICKS;
        case FARMYELLOW:
            return num == 0 ? Blocks.SAND : null;
        case FARMORANGE:
            return num == 0 ? Blocks.DIRT : null;
        case FARMLIMEGREEN:
            return num == 0 ? Blocks.CLAY : null;
        case FARMORANGELIGHT:
            return num == 0 ? Blocks.TERRACOTTA : null;
        case FARMGREEN:
            return num == 0 ? Blocks.GREEN_TERRACOTTA : null;
        default:
            return null;
        }
    }
}