package dorfgen.conversion;

import java.awt.Color;
import java.awt.image.BufferedImage;
import java.io.File;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Set;

import dorfgen.Dorfgen;
import dorfgen.util.ISigmoid;
import dorfgen.util.Interpolator.BicubicInterpolator;
import net.minecraft.util.Direction;
import net.minecraft.util.math.BlockPos;

public class DorfMap
{
    public static class ImgHolder
    {
        public BufferedImage elevationMap;
        public BufferedImage elevationWaterMap;
        public BufferedImage biomeMap;
        public BufferedImage evilMap;
        public BufferedImage temperatureMap;
        public BufferedImage rainMap;
        public BufferedImage drainageMap;
        public BufferedImage vegitationMap;
        public BufferedImage structuresMap;
        public BufferedImage volcanismMap;
    }

    public static boolean inBounds(final int x, final int z, final int[][] map)
    {
        if (x < 0 || z < 0 || x >= map.length || z >= map[0].length) return false;
        return true;
    }

    private static final Color RIVER      = new Color(0, 128, 128);
    static final int           WATERSHIFT = -24;

    public int[][]                                  biomeMap             = new int[0][0];
    public int[][]                                  elevationMap         = new int[0][0];
    public int[][]                                  waterMap             = new int[0][0];
    public int[][]                                  riverMap             = new int[0][0];
    public int[][]                                  evilMap              = new int[0][0];
    public int[][]                                  rainMap              = new int[0][0];
    public int[][]                                  drainageMap          = new int[0][0];
    public int[][]                                  temperatureMap       = new int[0][0];
    public int[][]                                  volcanismMap         = new int[0][0];
    public int[][]                                  vegitationMap        = new int[0][0];
    public int[][]                                  structureMap         = new int[0][0];
    /** Coords are embark tile resolution and are x + 8192 * z */
    public HashMap<Integer, Set<Site>>              sitesByCoord         = new HashMap<>();
    public HashMap<Integer, Site>                   sitesById            = new HashMap<>();
    public HashMap<String, Site>                    sitesByName          = new HashMap<>();
    public HashMap<Integer, Region>                 regionsById          = new HashMap<>();
    /** Coords are world tile resolution and are x + 2048 * z */
    public HashMap<Integer, Region>                 regionsByCoord       = new HashMap<>();
    public HashMap<Integer, Region>                 ugRegionsById        = new HashMap<>();
    /** Coords are world tile resolution and are x + 2048 * z */
    public HashMap<Integer, Region>                 ugRegionsByCoord     = new HashMap<>();
    public HashMap<Integer, WorldConstruction>      constructionsById    = new HashMap<>();
    /** Coords are world tile resolution and are x + 2048 * z */
    public HashMap<Integer, Set<WorldConstruction>> constructionsByCoord = new HashMap<>();

    public BicubicInterpolator    biomeInterpolator  = new BicubicInterpolator();
    public BicubicInterpolator    heightInterpolator = new BicubicInterpolator();
    public BicubicInterpolator    miscInterpolator   = new BicubicInterpolator();
    public ImgHolder              images             = new ImgHolder();
    public SiteStructureGenerator structureGen;
    public int                    scale              = Dorfgen.scale;
    public int                    cubicHeightScale   = Dorfgen.cubicHeightScale;
    public boolean                finite             = Dorfgen.finite;
    public int                    yMin               = 0;
    public BlockPos               spawn              = Dorfgen.spawn;
    public BlockPos               shift              = Dorfgen.shift;
    public String                 spawnSite          = Dorfgen.spawnSite;
    public int                    seaLevel           = 73;
    public String                 name               = "";
    public String                 altName            = "";
    public final File             mainDir;
    public boolean                randomSpawn;

    public ISigmoid  sigmoid   = new ISigmoid()
                               {
                               };
    public BiomeList biomeList = new BiomeList();

    void addSiteByCoord(final int x, final int z, final Site site)
    {
        final int coord = x + 8192 * z;
        Set<Site> sites = this.sitesByCoord.get(coord);
        if (sites == null)
        {
            sites = new HashSet<>();
            this.sitesByCoord.put(coord, sites);
        }
        sites.add(site);
    }

    public DorfMap(final File mainDir)
    {
        this.mainDir = mainDir;
    }

    public void setScale(final int scale)
    {
        this.scale = scale;
    }

    public int shiftX(final int xAbs)
    {
        return xAbs + this.shift.getX();
    }

    public int shiftZ(final int zAbs)
    {
        return zAbs + this.shift.getZ();
    }

    public int unShiftX(final int xAbs)
    {
        return xAbs - this.shift.getX();
    }

    public int unShiftZ(final int zAbs)
    {
        return zAbs - this.shift.getZ();
    }

    public void init()
    {
        this.populateBiomeMap();
        this.populateWaterMap();
        this.populateElevationMap();
        this.populateTemperatureMap();
        this.populateVegitationMap();
        this.populateDrainageMap();
        this.populateRainMap();
        this.populateStructureMap();

        this.postProcessRegions();
    }

    public void populateBiomeMap()
    {
        final BufferedImage img = this.images.biomeMap;
        if (img == null) return;
        this.biomeMap = new int[img.getWidth()][img.getHeight()];
        for (int y = 0; y < img.getHeight(); y++)
            for (int x = 0; x < img.getWidth(); x++)
                this.biomeMap[x][y] = this.images.biomeMap.getRGB(x, y);
        this.images.biomeMap = null;
    }

    public void setElevationSigmoid(final ISigmoid sigmoid)
    {
        this.sigmoid = sigmoid;
        this.populateWaterMap();
        this.populateElevationMap();
    }

    private int elevationSigmoid(final int preHeight)
    {
        return this.sigmoid.elevationSigmoid(preHeight);
    }

    public void populateElevationMap()
    {
        final BufferedImage img = this.images.elevationMap;
        if (img == null) return;
        this.elevationMap = new int[img.getWidth()][img.getHeight()];
        int max = Integer.MIN_VALUE;
        int min = Integer.MAX_VALUE;
        for (int y = 0; y < img.getHeight(); y++)
            for (int x = 0; x < img.getWidth(); x++)
            {
                final int rgb = this.images.elevationMap.getRGB(x, y);
                final int r = rgb >> 16 & 0xFF, b = rgb >> 0 & 0xFF;
                int h = b;
                if (r == 0) h = b + DorfMap.WATERSHIFT;
                h = Math.max(0, h);
                this.elevationMap[x][y] = this.elevationSigmoid(h);
                if (this.elevationMap[x][y] > max) max = this.elevationMap[x][y];
                if (this.elevationMap[x][y] < min) min = this.elevationMap[x][y];
            }
        Dorfgen.LOGGER.info("Max Alt: " + max + ", Min Alt: " + min + " for " + this.name);
    }

    public void populateWaterMap()
    {
        final BufferedImage img = this.images.elevationWaterMap;
        if (img == null) return;
        this.waterMap = new int[img.getWidth()][img.getHeight()];
        this.riverMap = new int[img.getWidth()][img.getHeight()];
        for (int y = 0; y < img.getHeight(); y++)
            for (int x = 0; x < img.getWidth(); x++)
            {
                final int rgb = this.images.elevationWaterMap.getRGB(x, y);
                final int r = rgb >> 16 & 0xFF, g = rgb >> 8 & 0xFF, b = rgb >> 0 & 0xFF;
                this.riverMap[x][y] = r != 0 || g != b ? -1 : b + DorfMap.WATERSHIFT;
                this.waterMap[x][y] = r != 0 || g != 0 ? -1 : b;
                if (this.riverMap[x][y] != -1) this.riverMap[x][y] = this.elevationSigmoid(this.riverMap[x][y]);
                if (this.waterMap[x][y] != -1) this.waterMap[x][y] = this.elevationSigmoid(this.waterMap[x][y]);
            }
        this.joinRivers();
    }

    public void populateTemperatureMap()
    {
        final BufferedImage img = this.images.temperatureMap;
        if (img == null) return;
        this.temperatureMap = new int[img.getWidth()][img.getHeight()];
        for (int y = 0; y < img.getHeight(); y++)
            for (int x = 0; x < img.getWidth(); x++)
            {
                final int rgb = img.getRGB(x, y);
                this.temperatureMap[x][y] = rgb & 255;
            }
        this.images.temperatureMap = null;
    }

    public void populateVegitationMap()
    {
        final BufferedImage img = this.images.vegitationMap;
        if (img == null) return;
        this.vegitationMap = new int[img.getWidth()][img.getHeight()];
        for (int y = 0; y < img.getHeight(); y++)
            for (int x = 0; x < img.getWidth(); x++)
            {
                final int rgb = img.getRGB(x, y);
                this.vegitationMap[x][y] = rgb & 255;
            }
        this.images.vegitationMap = null;
    }

    public void populateDrainageMap()
    {
        final BufferedImage img = this.images.drainageMap;
        if (img == null) return;
        this.drainageMap = new int[img.getWidth()][img.getHeight()];
        for (int y = 0; y < img.getHeight(); y++)
            for (int x = 0; x < img.getWidth(); x++)
            {
                final int rgb = img.getRGB(x, y);
                this.drainageMap[x][y] = rgb & 255;
            }
        this.images.drainageMap = null;
    }

    public void populateRainMap()
    {
        final BufferedImage img = this.images.rainMap;
        if (img == null) return;
        this.rainMap = new int[img.getWidth()][img.getHeight()];
        for (int y = 0; y < img.getHeight(); y++)
            for (int x = 0; x < img.getWidth(); x++)
            {
                final int rgb = img.getRGB(x, y);
                this.rainMap[x][y] = rgb & 255;
            }
        this.images.rainMap = null;
    }

    public void populateStructureMap()
    {
        final BufferedImage img = this.images.structuresMap;
        if (img == null) return;
        this.structureMap = new int[img.getWidth()][img.getHeight()];
        for (int y = 0; y < img.getHeight(); y++)
            for (int x = 0; x < img.getWidth(); x++)
            {
                final int rgb = img.getRGB(x, y);
                this.structureMap[x][y] = rgb;
            }
        this.images.structuresMap = null;
    }

    private void joinRivers()
    {
        final boolean biomes = this.biomeMap.length > 0;
        for (int y = 0; y < this.riverMap[0].length; y++)
            for (int x = 0; x < this.riverMap.length; x++)
            {
                final int r = this.riverMap[x][y];
                if (r > 0)
                {
                    final int num = this.countLarger(0, this.waterMap, x, y, 1);
                    final int num2 = this.countLarger(0, this.waterMap, x, y, 2);
                    if (num == 0 && num2 > 0)
                    {
                        final int[] dir = this.getDirToWater(x, y);
                        this.riverMap[x + dir[0]][y + dir[1]] = r;
                        this.riverMap[x + 2 * dir[0]][y + 2 * dir[1]] = r;
                    }
                }
            }
        for (int y = 0; y < this.waterMap[0].length; y++)
            for (int x = 0; x < this.waterMap.length; x++)
            {
                final int b = this.waterMap[x][y];
                if (biomes && this.riverMap[x][y] > 0)
                {
                    this.biomeMap[x][y] = DorfMap.RIVER.getRGB();
                    // Also set the ones above as river to fix annoying
                    // interpolation issues.
                    if (x + 1 < this.biomeMap.length) this.biomeMap[x + 1][y] = DorfMap.RIVER.getRGB();
                    if (y + 1 < this.biomeMap[x].length) this.biomeMap[x][y + 1] = DorfMap.RIVER.getRGB();
                }
                if (b > 0)
                {
                    final int num = this.countLarger(0, this.waterMap, x, y, 1);
                    final int num2 = this.countLarger(0, this.waterMap, x, y, 2);
                    if (num == 0 && num2 > 0) for (int i = 1; i < 3; i++)
                        for (final Direction dir : Direction.Plane.HORIZONTAL)
                            this.waterMap[x + dir.getXOffset() * i][y + dir.getZOffset() * i] = b;
                }
            }

    }

    private int[] getDirToWater(final int x, final int y)
    {
        final int[] ret = new int[2];
        if (this.waterMap[x + 2][y] > 0) ret[0] = 1;
        else if (this.waterMap[x - 2][y] > 0) ret[0] = -1;
        else if (this.waterMap[x][y + 2] > 0) ret[1] = 1;
        else if (this.waterMap[x][y - 2] > 0) ret[1] = -1;

        return ret;
    }

    public int countNear(final int toCheck, final int[][] image, final int pixelX, final int pixelY, final int distance)
    {
        int ret = 0;
        for (int i = -distance; i <= distance; i++)
            for (int j = -distance; j <= distance; j++)
            {
                if (i == 0 && j == 0) continue;
                final int x = pixelX + i, y = pixelY + j;
                if (x >= 0 && x < image.length && y >= 0 && y < image[0].length) if (image[x][y] == toCheck) ret++;
            }
        return ret;
    }

    public int countLarger(final int toCheck, final int[][] image, final int pixelX, final int pixelY,
            final int distance)
    {
        int ret = 0;
        for (int i = -distance; i <= distance; i++)
            for (int j = -distance; j <= distance; j++)
            {
                if (i == 0 && j == 0) continue;
                final int x = pixelX + i, y = pixelY + j;
                if (x >= 0 && x < image.length && y >= 0 && y < image[0].length) if (image[x][y] > toCheck) ret++;
            }
        return ret;
    }

    public Region getRegionForCoords(int x, int z)
    {
        x = this.shiftX(x) / (this.scale * 16);
        z = this.shiftZ(z) / (this.scale * 16);
        final int key = x + 2048 * z;
        return this.regionsByCoord.get(key);
    }

    public Region getUgRegionForCoords(int x, final int depth, int z)
    {
        x = this.shiftX(x) / (this.scale * 16);
        z = this.shiftZ(z) / (this.scale * 16);
        final int key = x + 2048 * z + depth * 4194304;
        return this.ugRegionsByCoord.get(key);
    }

    public Set<Site> getSiteForCoords(int x, int z)
    {
        x = this.shiftX(x);
        z = this.shiftZ(z);
        final int kx = x / this.scale;
        final int kz = z / this.scale;
        final int key = kx + 8192 * kz;
        final Set<Site> ret = this.sitesByCoord.get(key);
        if (ret != null) for (final Site s : ret)
            if (s.isInSite(x, z)) return ret;

        return Collections.emptySet();
    }

    public Set<WorldConstruction> getConstructionsForCoords(int x, int z)
    {
        x = this.shiftX(x) / (this.scale * 16);
        z = this.shiftZ(z) / (this.scale * 16);
        final int key = x + 2048 * z;
        return this.constructionsByCoord.getOrDefault(key, Collections.emptySet());
    }

    public void postProcessRegions()
    {
        for (final Region region : this.regionsById.values())
            for (final int i : region.coords)
                if (!this.regionsByCoord.containsKey(i)) this.regionsByCoord.put(i, region);
                else System.err.println("Existing region for " + (i & 2047) + " " + i / 2048);
        for (final Region region : this.ugRegionsById.values())
            for (final int i : region.coords)
                if (!this.ugRegionsByCoord.containsKey(i)) this.ugRegionsByCoord.put(i, region);
                else System.err.println("Existing region for " + (i & 2047) + " " + i / 2048);
    }

    public static enum SiteType
    {
        CAVE("cave"), FORTRESS("fortress"), TOWN("town"), HIPPYHUTS("forest retreat"), DARKFORTRESS(
                "dark fortress"), HAMLET("hamlet"), VAULT("vault"), DARKPITS("dark pits"), HILLOCKS("hillocks"), TOMB(
                        "tomb"), TOWER("tower"), MOUNTAINHALLS("mountain halls"), CAMP("camp"), LAIR("lair"), SHRINE(
                                "shrine"), LABYRINTH("labyrinth");

        public final String name;

        SiteType(final String name_)
        {
            this.name = name_;
        }

        public static SiteType getSite(final String name)
        {
            for (final SiteType t : SiteType.values())
                if (t.name.equalsIgnoreCase(name)) return t;
            return null;
        }

        public boolean isVillage()
        {
            return this == TOWN || this == HAMLET || this == HILLOCKS || this == HIPPYHUTS;
        }
    }

    public static class Site
    {
        public final String         name;
        public final int            id;
        public final SiteType       type;
        public final DorfMap        map;
        public int                  x;
        public int                  z;
        /** Corners in embark tile coordinates */
        public final int[][]        corners    = new int[2][2];
        public int[][]              rgbmap;
        public final Set<Structure> structures = new HashSet<>();
        // public BufferedImage map;

        public Site(final DorfMap map, final String name_, final int id_, final SiteType type_, final int x_,
                final int z_)
        {
            this.map = map;
            this.name = name_;
            this.id = id_;
            this.type = type_;
            this.x = x_;
            this.z = z_;
            if (this.type == null) throw new NullPointerException();
        }

        public int[] getSiteMid()
        {
            final int[] mid = new int[2];
            mid[0] = (this.map.unShiftX(this.corners[1][0] * this.map.scale) + this.map.unShiftX(this.corners[0][0]
                    * this.map.scale)) / 2;
            mid[1] = (this.map.unShiftZ(this.corners[0][1] * this.map.scale) + this.map.unShiftZ(this.corners[1][1]
                    * this.map.scale)) / 2;
            return mid;
        }

        public void setSiteLocation(final int x1, final int z1, final int x2, final int z2)
        {
            this.corners[0][0] = x1;
            this.corners[0][1] = z1;
            this.corners[1][0] = x2;
            this.corners[1][1] = z2;
            this.x = (x1 + x2) / 2;
            this.z = (z1 + z2) / 2;
            for (int x = x1; x <= x2; x++)
                for (int z = z1; z <= z2; z++)
                    this.map.addSiteByCoord(x, z, this);
        }

        @Override
        public String toString()
        {
            return this.name + " " + this.type + " " + this.id + " " + this.map.unShiftX(this.corners[0][0]
                    * this.map.scale) + "," + this.map.unShiftZ(this.corners[0][1] * this.map.scale) + "->" + this.map
                            .unShiftX(this.corners[1][0] * this.map.scale) + "," + this.map.unShiftZ(this.corners[1][1]
                                    * this.map.scale);
        }

        @Override
        public int hashCode()
        {
            return this.id;
        }

        @Override
        public boolean equals(final Object o)
        {
            if (o instanceof Site) return ((Site) o).id == this.id;
            return super.equals(o);
        }

        public boolean isInSite(final int x, final int z)
        {
            final int width = this.rgbmap != null ? this.map.scale / 2 : 0;
            if (x < this.corners[0][0] * this.map.scale + width || z < this.corners[0][1] * this.map.scale + width)
                return false;
            if (this.rgbmap != null) // System.out.println("test");
                // Equals as it starts at 0
                if (x >= this.corners[0][0] * this.map.scale + this.rgbmap.length * this.map.scale
                        / SiteStructureGenerator.SITETOBLOCK + this.map.scale / 2 || z >= this.corners[0][1]
                                * this.map.scale + this.rgbmap[0].length * this.map.scale
                                        / SiteStructureGenerator.SITETOBLOCK + this.map.scale / 2) return false;
            return true;
        }
    }

    public static enum StructureType
    {
        MARKET("market"), UNDERWORLDSPIRE("underworld spire"), TEMPLE("temple");

        public final String name;

        StructureType(final String name_)
        {
            this.name = name_;
        }
    }

    public static class Structure
    {
        final DorfMap       map;
        final String        name;
        final String        name2;
        final int           id;
        final StructureType type;

        public Structure(final DorfMap map, String name_, String name2_, final int id_, final StructureType type_)
        {
            if (name_ == null) name_ = "";
            if (name2_ == null) name2_ = "";
            this.map = map;
            this.name = name_;
            this.name2 = name2_;
            this.id = id_;
            this.type = type_;
        }

        @Override
        public boolean equals(final Object o)
        {
            if (o instanceof Structure) return ((Structure) o).id == this.id;
            return super.equals(o);
        }

        @Override
        public int hashCode()
        {
            return this.id;
        }
    }

    public static enum RegionType
    {
        OCEAN, TUNDRA, GLACIER, FOREST, HILLS, GRASSLAND, WETLAND, MOUNTAINS, DESERT, LAKE, CAVERN, MAGMA, UNDERWORLD;
    }

    public static class Region
    {
        final DorfMap                          map;
        public final int                       id;
        public final String                    name;
        public final RegionType                type;
        final int                              depth;
        public final HashSet<Integer>          coords   = new HashSet<>();
        public final HashMap<Integer, Integer> biomeMap = new HashMap<>();

        public Region(final DorfMap map, final int id_, final String name_, final RegionType type_)
        {
            this.id = id_;
            this.name = name_;
            this.type = type_;
            this.depth = 0;
            this.map = map;
        }

        public Region(final DorfMap map, final int id_, final String name_, final int depth_, final RegionType type_)
        {
            this.map = map;
            this.id = id_;
            this.name = name_;
            this.type = type_;
            this.depth = depth_;
        }

        public boolean isInRegion(int x, int z)
        {
            x = this.map.shiftX(x) / (this.map.scale * 16);
            z = this.map.shiftZ(z) / (this.map.scale * 16);
            final int key = x + 2048 * z + this.depth * 4194304;
            return this.coords.contains(key);
        }

        @Override
        public boolean equals(final Object o)
        {
            if (o instanceof Region) return ((Region) o).id == this.id;
            return super.equals(o);
        }

        @Override
        public int hashCode()
        {
            return this.id;
        }

        @Override
        public String toString()
        {
            return this.id + " " + this.name + " " + this.type;
        }
    }

    public static enum ConstructionType
    {
        ROAD, BRIDGE, TUNNEL;
    }

    public static class WorldConstruction
    {
        final DorfMap                          map;
        public final int                       id;
        public final String                    name;
        public final ConstructionType          type;
        public final HashSet<Integer>          worldCoords  = new HashSet<>();
        /** Key: x,z coordinate, Value: depth, -1 for surface */
        public final HashMap<Integer, Integer> embarkCoords = new HashMap<>();

        public WorldConstruction(final DorfMap map, final int id_, final String name_, final ConstructionType type_)
        {
            this.map = map;
            this.id = id_;
            this.name = name_;
            this.type = type_;
        }

        public boolean isInRegion(int x, int z)
        {
            x = this.map.shiftX(x) / (this.map.scale * 16);
            z = this.map.shiftZ(z) / (this.map.scale * 16);
            final int key = x + 2048 * z;
            return this.worldCoords.contains(key);
        }

        public int getYValue(int x, final int surfaceY, int z)
        {
            x = this.map.shiftX(x) / this.map.scale;
            z = this.map.shiftZ(z) / this.map.scale;
            final int key = x + 8192 * z;
            if (!this.embarkCoords.containsKey(key)) return Integer.MIN_VALUE;
            final Integer i = this.embarkCoords.get(key);
            if (i == -1) return surfaceY;
            return this.map.sigmoid.elevationSigmoid(i);

        }

        public boolean isInConstruct(int x, final int y, int z)
        {
            x = this.map.shiftX(x) / this.map.scale;
            z = this.map.shiftZ(z) / this.map.scale;
            final int key = x + 8192 * z;
            return this.embarkCoords.containsKey(key);
        }

        @Override
        public boolean equals(final Object o)
        {
            if (o instanceof WorldConstruction) return ((WorldConstruction) o).id == this.id;
            return super.equals(o);
        }

        @Override
        public int hashCode()
        {
            return this.id;
        }

        @Override
        public String toString()
        {
            return this.id + " " + this.name + " " + this.type;
        }
    }
}