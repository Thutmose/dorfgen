package dorfgen.conversion;

import java.awt.image.BufferedImage;
import java.io.File;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Set;

import dorfgen.WorldGenerator;
import dorfgen.conversion.Interpolator.BicubicInterpolator;
import dorfgen.conversion.Interpolator.CachedBicubicInterpolator;
import net.minecraft.init.Biomes;
import net.minecraft.util.EnumFacing;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.biome.Biome;

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

    public static boolean inBounds(int x, int z, int[][] map)
    {
        if (x < 0 || z < 0 || x >= map.length || z >= map[0].length) return false;
        return true;
    }

    public int[][]                                      biomeMap             = new int[0][0];
    public int[][]                                      elevationMap         = new int[0][0];
    public int[][]                                      waterMap             = new int[0][0];
    public int[][]                                      riverMap             = new int[0][0];
    public int[][]                                      evilMap              = new int[0][0];
    public int[][]                                      rainMap              = new int[0][0];
    public int[][]                                      drainageMap          = new int[0][0];
    public int[][]                                      temperatureMap       = new int[0][0];
    public int[][]                                      volcanismMap         = new int[0][0];
    public int[][]                                      vegitationMap        = new int[0][0];
    public int[][]                                      structureMap         = new int[0][0];
    /** Coords are embark tile resolution and are x + 8192 * z */
    public HashMap<Integer, HashSet<Site>>              sitesByCoord         = new HashMap<Integer, HashSet<Site>>();
    public HashMap<Integer, Site>                       sitesById            = new HashMap<Integer, Site>();
    public HashMap<Integer, Region>                     regionsById          = new HashMap<Integer, Region>();
    /** Coords are world tile resolution and are x + 2048 * z */
    public HashMap<Integer, Region>                     regionsByCoord       = new HashMap<Integer, Region>();
    public HashMap<Integer, Region>                     ugRegionsById        = new HashMap<Integer, Region>();
    /** Coords are world tile resolution and are x + 2048 * z */
    public HashMap<Integer, Region>                     ugRegionsByCoord     = new HashMap<Integer, Region>();
    public HashMap<Integer, WorldConstruction>          constructionsById    = new HashMap<Integer, WorldConstruction>();
    /** Coords are world tile resolution and are x + 2048 * z */
    public HashMap<Integer, HashSet<WorldConstruction>> constructionsByCoord = new HashMap<Integer, HashSet<WorldConstruction>>();
    static int                                          waterShift           = -24;

    public BicubicInterpolator                          biomeInterpolator    = new BicubicInterpolator();
    public CachedBicubicInterpolator                    heightInterpolator   = new CachedBicubicInterpolator();
    public CachedBicubicInterpolator                    miscInterpolator     = new CachedBicubicInterpolator();
    public ImgHolder                                    images               = new ImgHolder();
    public SiteStructureGenerator                       structureGen;
    public int                                          scale                = WorldGenerator.scale;
    public int                                          cubicHeightScale     = WorldGenerator.cubicHeightScale;
    public boolean                                      finite               = WorldGenerator.finite;
    public int                                          yMin                 = 0;
    public BlockPos                                     spawn                = WorldGenerator.spawn;
    public BlockPos                                     shift                = WorldGenerator.shift;
    public String                                       spawnSite            = WorldGenerator.spawnSite;
    public int                                          seaLevel             = 73;
    public final String                                 name;
    public boolean                                      randomSpawn;

    public ISigmoid                                     sigmoid              = new ISigmoid()
                                                                             {
                                                                             };
    public File                                         resourceDir;

    void addSiteByCoord(int x, int z, Site site)
    {
        int coord = x + 8192 * z;
        HashSet<Site> sites = sitesByCoord.get(coord);
        if (sites == null)
        {
            sites = new HashSet<Site>();
            sitesByCoord.put(coord, sites);
        }
        sites.add(site);
    }

    public DorfMap(String name)
    {
        this.name = name;
    }

    public void setScale(int scale)
    {
        this.scale = scale;
    }

    public int shiftX(int xAbs)
    {
        return xAbs + shift.getX();
    }

    public int shiftZ(int zAbs)
    {
        return zAbs + shift.getZ();
    }

    public int unShiftX(int xAbs)
    {
        return xAbs - shift.getX();
    }

    public int unShiftZ(int zAbs)
    {
        return zAbs - shift.getZ();
    }

    public void init()
    {
        populateBiomeMap();
        populateElevationMap();
        populateWaterMap();
        populateTemperatureMap();
        populateVegitationMap();
        populateDrainageMap();
        populateRainMap();
        populateStructureMap();

        postProcessRegions();
        if (biomeMap.length > 0)
        {
            postProcessBiomeMap();
        }
    }

    public void populateBiomeMap()
    {
        BufferedImage img = images.biomeMap;
        if (img == null) return;
        biomeMap = new int[img.getWidth()][img.getHeight()];
        for (int y = 0; y < img.getHeight(); y++)
        {
            for (int x = 0; x < img.getWidth(); x++)
            {
                int rgb = images.biomeMap.getRGB(x, y);
                biomeMap[x][y] = BiomeList.GetBiomeIndex(rgb);
            }
        }
        images.biomeMap = null;
    }

    public void setElevationSigmoid(ISigmoid sigmoid)
    {
        this.sigmoid = sigmoid;
        populateElevationMap();
    }

    private int elevationSigmoid(int preHeight)
    {
        return sigmoid.elevationSigmoid(preHeight);
    }

    public void populateElevationMap()
    {
        BufferedImage img = images.elevationMap;
        if (img == null) return;
        elevationMap = new int[img.getWidth()][img.getHeight()];
        int max = Integer.MIN_VALUE;
        int min = Integer.MAX_VALUE;
        for (int y = 0; y < img.getHeight(); y++)
        {
            for (int x = 0; x < img.getWidth(); x++)
            {
                int rgb = images.elevationMap.getRGB(x, y);
                int r = (rgb >> 16) & 0xFF, b = (rgb >> 0) & 0xFF;
                int h = b;
                if (r == 0)
                {
                    h = b + waterShift;
                }
                h = Math.max(0, h);
                elevationMap[x][y] = elevationSigmoid(h);
                if (elevationMap[x][y] > max) max = elevationMap[x][y];
                if (elevationMap[x][y] < min) min = elevationMap[x][y];

                if (biomeMap.length > 0)
                    if (h < 145 && biomeMap[x][y] == Biome.getIdForBiome(Biomes.MUTATED_EXTREME_HILLS))
                    {
                    biomeMap[x][y] = Biome.getIdForBiome(Biomes.EXTREME_HILLS);
                    }
            }
        }
        System.out.println(max + " " + min);
        // Don't clear the elevation map, it is needed again if sigmoid changes.
        // images.elevationMap = null;
    }

    public void populateWaterMap()
    {
        BufferedImage img = images.elevationWaterMap;
        if (img == null) return;
        waterMap = new int[img.getWidth()][img.getHeight()];
        riverMap = new int[img.getWidth()][img.getHeight()];
        for (int y = 0; y < img.getHeight(); y++)
        {
            for (int x = 0; x < img.getWidth(); x++)
            {
                int rgb = images.elevationWaterMap.getRGB(x, y);
                int r = (rgb >> 16) & 0xFF, g = (rgb >> 8) & 0xFF, b = (rgb >> 0) & 0xFF;
                riverMap[x][y] = r != 0 || g != b ? -1 : b;
                waterMap[x][y] = r != 0 || g != 0 ? -1 : b;
            }
        }
        joinRivers();
        images.elevationWaterMap = null;
    }

    public void populateTemperatureMap()
    {
        BufferedImage img = images.temperatureMap;
        if (img == null) return;
        temperatureMap = new int[img.getWidth()][img.getHeight()];
        for (int y = 0; y < img.getHeight(); y++)
        {
            for (int x = 0; x < img.getWidth(); x++)
            {
                int rgb = img.getRGB(x, y);
                temperatureMap[x][y] = rgb & 255;
            }
        }
        images.temperatureMap = null;
    }

    public void populateVegitationMap()
    {
        BufferedImage img = images.vegitationMap;
        if (img == null) return;
        vegitationMap = new int[img.getWidth()][img.getHeight()];
        for (int y = 0; y < img.getHeight(); y++)
        {
            for (int x = 0; x < img.getWidth(); x++)
            {
                int rgb = img.getRGB(x, y);
                vegitationMap[x][y] = rgb & 255;
            }
        }
        images.vegitationMap = null;
    }

    public void populateDrainageMap()
    {
        BufferedImage img = images.drainageMap;
        if (img == null) return;
        drainageMap = new int[img.getWidth()][img.getHeight()];
        for (int y = 0; y < img.getHeight(); y++)
        {
            for (int x = 0; x < img.getWidth(); x++)
            {
                int rgb = img.getRGB(x, y);
                drainageMap[x][y] = rgb & 255;
            }
        }
        images.drainageMap = null;
    }

    public void populateRainMap()
    {
        BufferedImage img = images.rainMap;
        if (img == null) return;
        rainMap = new int[img.getWidth()][img.getHeight()];
        for (int y = 0; y < img.getHeight(); y++)
        {
            for (int x = 0; x < img.getWidth(); x++)
            {
                int rgb = img.getRGB(x, y);
                rainMap[x][y] = rgb & 255;
            }
        }
        images.rainMap = null;
    }

    public void populateStructureMap()
    {
        BufferedImage img = images.structuresMap;
        if (img == null) return;
        structureMap = new int[img.getWidth()][img.getHeight()];
        for (int y = 0; y < img.getHeight(); y++)
        {
            for (int x = 0; x < img.getWidth(); x++)
            {
                int rgb = img.getRGB(x, y);
                structureMap[x][y] = rgb;
            }
        }
        images.structuresMap = null;
    }

    private void joinRivers()
    {
        for (int y = 0; y < riverMap[0].length; y++)
        {
            for (int x = 0; x < riverMap.length; x++)
            {
                int r = riverMap[x][y];
                if (r > 0)
                {
                    int num = countLarger(0, waterMap, x, y, 1);
                    int num2 = countLarger(0, waterMap, x, y, 2);
                    if (num == 0 && num2 > 0)
                    {
                        int[] dir = getDirToWater(x, y);
                        riverMap[x + dir[0]][y + dir[1]] = r;
                        riverMap[x + 2 * dir[0]][y + 2 * dir[1]] = r;
                    }
                }
            }
        }
        for (int y = 0; y < waterMap[0].length; y++)
        {
            for (int x = 0; x < waterMap.length; x++)
            {
                int b = waterMap[x][y];
                if (b > 0)
                {
                    int num = countLarger(0, waterMap, x, y, 1);
                    int num2 = countLarger(0, waterMap, x, y, 2);
                    if (num == 0 && num2 > 0)
                    {
                        for (int i = 1; i < 3; i++)
                            for (EnumFacing dir : EnumFacing.HORIZONTALS)
                            {
                                waterMap[x + dir.getFrontOffsetX() * i][y + dir.getFrontOffsetZ() * i] = b;
                            }
                    }
                }
            }
        }

    }

    private int[] getDirToWater(int x, int y)
    {
        int[] ret = new int[2];
        if (waterMap[x + 2][y] > 0) ret[0] = 1;
        else if (waterMap[x - 2][y] > 0) ret[0] = -1;
        else if (waterMap[x][y + 2] > 0) ret[1] = 1;
        else if (waterMap[x][y - 2] > 0) ret[1] = -1;

        return ret;
    }

    public int countNear(int toCheck, int[][] image, int pixelX, int pixelY, int distance)
    {
        int ret = 0;
        for (int i = -distance; i <= distance; i++)
        {
            for (int j = -distance; j <= distance; j++)
            {
                if (i == 0 && j == 0) continue;
                int x = pixelX + i, y = pixelY + j;
                if (x >= 0 && x < image.length && y >= 0 && y < image[0].length)
                {
                    if (image[x][y] == toCheck)
                    {
                        ret++;
                    }
                }
            }
        }
        return ret;
    }

    public int countLarger(int toCheck, int[][] image, int pixelX, int pixelY, int distance)
    {
        int ret = 0;
        for (int i = -distance; i <= distance; i++)
        {
            for (int j = -distance; j <= distance; j++)
            {
                if (i == 0 && j == 0) continue;
                int x = pixelX + i, y = pixelY + j;
                if (x >= 0 && x < image.length && y >= 0 && y < image[0].length)
                {
                    if (image[x][y] > toCheck)
                    {
                        ret++;
                    }
                }
            }
        }
        return ret;
    }

    public void postProcessBiomeMap()
    {
        boolean hasThermalMap = temperatureMap.length > 0;
        for (int x = 0; x < biomeMap.length; x++)
            for (int z = 0; z < biomeMap[0].length; z++)
            {
                int biome = biomeMap[x][z];
                if (biome == Biome.getIdForBiome(Biomes.RIVER)) continue;
                int temperature = hasThermalMap ? temperatureMap[x][z] : 128;
                int drainage = drainageMap.length > 0 ? drainageMap[x][z] : 100;
                int rain = rainMap.length > 0 ? rainMap[x][z] : 100;
                int evil = evilMap.length > 0 ? evilMap[x][z] : 100;
                Region region = getRegionForCoords(x * scale, z * scale);
                int newBiome = BiomeList.getBiomeFromValues(biome, temperature, drainage, rain, evil, region);
                biomeMap[x][z] = newBiome;
            }
    }

    public Region getRegionForCoords(int x, int z)
    {
        x = shiftX(x) / (scale * 16);
        z = shiftZ(z) / (scale * 16);
        int key = x + 2048 * z;
        return regionsByCoord.get(key);
    }

    public Region getUgRegionForCoords(int x, int depth, int z)
    {
        x = shiftX(x) / (scale * 16);
        z = shiftZ(z) / (scale * 16);
        int key = x + 2048 * z + depth * 4194304;
        return ugRegionsByCoord.get(key);
    }

    public HashSet<Site> getSiteForCoords(int x, int z)
    {
        x = shiftX(x);
        z = shiftZ(z);
        int kx = x / (scale);
        int kz = z / (scale);
        int key = kx + 8192 * kz;

        HashSet<Site> ret = sitesByCoord.get(key);

        if (ret != null)
        {
            for (Site s : ret)
            {
                if (s.isInSite(x, z)) return ret;
            }
        }

        return null;
    }

    public HashSet<WorldConstruction> getConstructionsForCoords(int x, int z)
    {
        x = shiftX(x) / (scale * 16);
        z = shiftZ(z) / (scale * 16);
        int key = x + 2048 * z;
        return constructionsByCoord.get(key);
    }

    public void postProcessRegions()
    {
        for (Region region : regionsById.values())
        {
            for (int i : region.coords)
            {
                if (!regionsByCoord.containsKey(i))
                {
                    regionsByCoord.put(i, region);
                }
                else
                {
                    System.err.println("Existing region for " + (i & 2047) + " " + (i / 2048));
                }
            }
        }
        for (Region region : ugRegionsById.values())
        {
            for (int i : region.coords)
            {
                if (!ugRegionsByCoord.containsKey(i))
                {
                    ugRegionsByCoord.put(i, region);
                }
                else
                {
                    System.err.println("Existing region for " + (i & 2047) + " " + (i / 2048));
                }
            }
        }
    }

    public static enum SiteType
    {
        CAVE("cave"), FORTRESS("fortress"), TOWN("town"), HIPPYHUTS("forest retreat"), DARKFORTRESS(
                "dark fortress"), HAMLET("hamlet"), VAULT("vault"), DARKPITS("dark pits"), HILLOCKS("hillocks"), TOMB(
                        "tomb"), TOWER("tower"), MOUNTAINHALLS(
                                "mountain halls"), CAMP("camp"), LAIR("lair"), SHRINE("shrine"), LABYRINTH("labyrinth");

        public final String name;

        SiteType(String name_)
        {
            name = name_;
        }

        public static SiteType getSite(String name)
        {
            for (SiteType t : SiteType.values())
            {
                if (t.name.equalsIgnoreCase(name)) { return t; }
            }
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
        public final Set<Structure> structures = new HashSet<DorfMap.Structure>();
        // public BufferedImage map;

        public Site(DorfMap map, String name_, int id_, SiteType type_, int x_, int z_)
        {
            this.map = map;
            name = name_;
            id = id_;
            type = type_;
            x = x_;
            z = z_;
            if (type == null) { throw new NullPointerException(); }
        }

        public int[] getSiteMid()
        {
            int[] mid = new int[2];
            mid[0] = (map.unShiftX(corners[1][0] * map.scale) + map.unShiftX(corners[0][0] * map.scale)) / 2;
            mid[1] = (map.unShiftZ(corners[0][1] * map.scale) + map.unShiftZ(corners[1][1] * map.scale)) / 2;
            return mid;
        }

        public void setSiteLocation(int x1, int z1, int x2, int z2)
        {
            corners[0][0] = x1;
            corners[0][1] = z1;
            corners[1][0] = x2;
            corners[1][1] = z2;
            x = (x1 + x2) / 2;
            z = (z1 + z2) / 2;
            for (int x = x1; x <= x2; x++)
                for (int z = z1; z <= z2; z++)
                    map.addSiteByCoord(x, z, this);
        }

        @Override
        public String toString()
        {
            return name + " " + type + " " + id + " " + map.unShiftX(corners[0][0] * map.scale) + ","
                    + map.unShiftZ(corners[0][1] * map.scale) + "->" + map.unShiftX(corners[1][0] * map.scale) + ","
                    + map.unShiftZ(corners[1][1] * map.scale);
        }

        @Override
        public int hashCode()
        {
            return id;
        }

        @Override
        public boolean equals(Object o)
        {
            if (o instanceof Site) { return ((Site) o).id == id; }
            return super.equals(o);
        }

        public boolean isInSite(int x, int z)
        {
            int width = rgbmap != null ? map.scale / 2 : 0;
            if (x < corners[0][0] * map.scale + width || z < corners[0][1] * map.scale + width) { return false; }
            if (rgbmap != null)
            {
                // System.out.println("test");
                // Equals as it starts at 0
                if (x >= (corners[0][0] * map.scale + rgbmap.length * map.scale / SiteStructureGenerator.SITETOBLOCK
                        + map.scale / 2)
                        || z >= (corners[0][1] * map.scale
                                + rgbmap[0].length * map.scale / SiteStructureGenerator.SITETOBLOCK + map.scale / 2))
                    return false;
            }
            return true;
        }
    }

    public static enum StructureType
    {
        MARKET("market"), UNDERWORLDSPIRE("underworld spire"), TEMPLE("temple");

        public final String name;

        StructureType(String name_)
        {
            name = name_;
        }
    }

    public static class Structure
    {
        final DorfMap       map;
        final String        name;
        final String        name2;
        final int           id;
        final StructureType type;

        public Structure(DorfMap map, String name_, String name2_, int id_, StructureType type_)
        {
            if (name_ == null)
            {
                name_ = "";
            }
            if (name2_ == null)
            {
                name2_ = "";
            }
            this.map = map;
            name = name_;
            name2 = name2_;
            id = id_;
            type = type_;
        }

        @Override
        public boolean equals(Object o)
        {
            if (o instanceof Structure) { return ((Structure) o).id == id; }
            return super.equals(o);
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
        public final HashSet<Integer>          coords   = new HashSet<Integer>();
        public final HashMap<Integer, Integer> biomeMap = new HashMap<Integer, Integer>();

        public Region(DorfMap map, int id_, String name_, RegionType type_)
        {
            id = id_;
            name = name_;
            type = type_;
            depth = 0;
            this.map = map;
        }

        public Region(DorfMap map, int id_, String name_, int depth_, RegionType type_)
        {
            this.map = map;
            id = id_;
            name = name_;
            type = type_;
            depth = depth_;
        }

        public boolean isInRegion(int x, int z)
        {
            x = map.shiftX(x) / (map.scale * 16);
            z = map.shiftZ(z) / (map.scale * 16);
            int key = x + 2048 * z + depth * 4194304;
            return coords.contains(key);
        }

        @Override
        public boolean equals(Object o)
        {
            if (o instanceof Region) { return ((Region) o).id == id; }
            return super.equals(o);
        }

        @Override
        public String toString()
        {
            return id + " " + name + " " + type;
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
        public final HashSet<Integer>          worldCoords  = new HashSet<Integer>();
        /** Key: x,z coordinate, Value: depth, -1 for surface */
        public final HashMap<Integer, Integer> embarkCoords = new HashMap<Integer, Integer>();

        public WorldConstruction(DorfMap map, int id_, String name_, ConstructionType type_)
        {
            this.map = map;
            id = id_;
            name = name_;
            type = type_;
        }

        public boolean isInRegion(int x, int z)
        {
            x = map.shiftX(x) / (map.scale * 16);
            z = map.shiftZ(z) / (map.scale * 16);
            int key = x + 2048 * z;
            return worldCoords.contains(key);
        }

        public int getYValue(int x, int surfaceY, int z)
        {
            x = map.shiftX(x) / (map.scale);
            z = map.shiftZ(z) / (map.scale);
            int key = x + 8192 * z;
            if (!embarkCoords.containsKey(key)) return Integer.MIN_VALUE;
            Integer i = embarkCoords.get(key);
            if (i == -1) return surfaceY;
            return map.sigmoid.elevationSigmoid(i);

        }

        public boolean isInConstruct(int x, int y, int z)
        {
            x = map.shiftX(x) / (map.scale);
            z = map.shiftZ(z) / (map.scale);
            int key = x + 8192 * z;
            return embarkCoords.containsKey(key);
        }

        @Override
        public boolean equals(Object o)
        {
            if (o instanceof WorldConstruction) { return ((WorldConstruction) o).id == id; }
            return super.equals(o);
        }

        @Override
        public String toString()
        {
            return id + " " + name + " " + type;
        }
    }
}
