package dorfgen.conversion;

import java.awt.Color;
import java.awt.image.BufferedImage;
import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;

import javax.imageio.ImageIO;
import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;

import org.w3c.dom.Document;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

import com.google.common.collect.Lists;

// import com.sun.org.apache.xerces.internal.dom.DeferredElementImpl;

import MappedXML.MappedTruncate;
import dorfgen.Dorfgen;
import dorfgen.conversion.DorfMap.ConstructionType;
import dorfgen.conversion.DorfMap.Region;
import dorfgen.conversion.DorfMap.RegionType;
import dorfgen.conversion.DorfMap.Site;
import dorfgen.conversion.DorfMap.SiteType;
import dorfgen.conversion.DorfMap.Structure;
import dorfgen.conversion.DorfMap.StructureType;
import dorfgen.conversion.DorfMap.WorldConstruction;
import net.minecraftforge.common.BiomeDictionary.Type;

public class FileLoader
{

    public File   resourceDir = null;
    final DorfMap map;

    public String elevation              = "";
    public String elevationWater         = "";
    public String biome                  = "";
    public String temperature            = "";
    public String evil                   = "";
    public String rain                   = "";
    public String drainage               = "";
    public String volcanism              = "";
    public String vegitation             = "";
    public String structs                = "";
    public String legends                = "";
    public String legendsPlus            = "";
    public String constructionFineCoords = "";
    public String siteInfo               = "";

    public HashMap<Integer, BufferedImage> sites = new HashMap<>();

    public FileLoader(final File folder)
    {
        this.resourceDir = folder;
        this.map = new DorfMap(folder);
        final String name = folder.getName();
        final File biomes = new File(folder, "biome_mappings.csv");
        boolean noRegion = true;
        for (final File f : folder.listFiles())
        {
            String s = f.getName();
            if (f.isDirectory() && s.contains("site_maps")) for (final File f1 : f.listFiles())
            {
                s = f1.getName();
                if (s.contains("-site_map-"))
                {
                    final String[] args = s.split("-");
                    final String s1 = args[args.length - 1].replace(".png", "").replace(".bmp", "");
                    final Integer id = Integer.parseInt(s1);
                    final BufferedImage site = this.getImage(f1.getAbsolutePath());
                    if (site != null) this.sites.put(id, site);
                    else Dorfgen.LOGGER.info("Site " + id + " did not read correctly. " + s + " " + name);
                }
            }
            else if (f.isDirectory() && s.contains("region_maps"))
            {
                noRegion = false;
                for (final File f1 : f.listFiles())
                {
                    s = f1.getAbsolutePath();
                    if (s.contains("-el.")) this.elevation = s;
                    else if (s.contains("-elw.")) this.elevationWater = s;
                    else if (s.contains("-bm.")) this.biome = s;
                    else if (s.contains("-rain.")) this.rain = s;
                    else if (s.contains("-drn.")) this.rain = s;
                    else if (s.contains("-tmp.")) this.temperature = s;
                    else if (s.contains("-vol.")) this.volcanism = s;
                    else if (s.contains("-veg.")) this.vegitation = s;
                    else if (s.contains("-evil.")) this.evil = s;
                    else if (s.contains("-str.")) this.structs = s;
                }
            }
            else if (!f.isDirectory())
            {
                s = f.getAbsolutePath();
                if (s.contains("-legends") && !s.contains("plus")) this.legends = s;
                else if (s.contains("-legends_plus")) this.legendsPlus = s;
                else if (s.contains("constructs.txt")) this.constructionFineCoords = s;
                else if (s.contains("sites.txt")) this.siteInfo = s;
            }
        }
        if (noRegion)
        {
            Dorfgen.LOGGER.warn("No Region maps found for " + name);
            return;
        }
        Dorfgen.LOGGER.info("Loading Region " + name);

        if (!this.legends.isEmpty())
        {
            if (!this.legends.contains("trunc"))
            {
                MappedTruncate.ReadTruncateAndOutput(this.legends, this.legends.replace(".xml", "_trunc.xml"),
                        "<artifacts>", "\n</df_world>", true);
                this.legends = this.legends.replace(".xml", "_trunc.xml");
            }
            this.loadLegends(this.legends);
        }
        List<String> names = Collections.emptyList();
        if (!this.legendsPlus.isEmpty())
        {
            if (!this.legendsPlus.contains("trunc"))
            {
                MappedTruncate.ReadTruncateAndOutput(this.legendsPlus, this.legendsPlus.replace(".xml", "_trunc.xml"),
                        "<artifacts>", "\n</df_world>", true);
                this.legendsPlus = this.legendsPlus.replace(".xml", "_trunc.xml");
            }
            names = this.loadLegendsPlus(this.legendsPlus);
        }
        if (!this.constructionFineCoords.isEmpty()) this.loadFineConstructLocations(this.constructionFineCoords);
        if (!this.siteInfo.isEmpty()) this.loadSiteInfo(this.siteInfo);

        this.map.name = names.size() > 0 ? names.get(0) : name;
        this.map.altName = names.size() > 1 ? names.get(1) : name;

        this.map.images.biomeMap = this.getImage(this.biome);
        this.map.images.elevationMap = this.getImage(this.elevation);
        this.map.images.elevationWaterMap = this.getImage(this.elevationWater);
        this.map.images.temperatureMap = this.getImage(this.temperature);
        this.map.images.vegitationMap = this.getImage(this.vegitation);
        this.map.images.structuresMap = this.getImage(this.structs);
        this.map.images.drainageMap = this.getImage(this.drainage);
        this.map.images.rainMap = this.getImage(this.rain);
        this.map.images.volcanismMap = this.getImage(this.volcanism);

        this.map.init();
        this.map.structureGen = new SiteStructureGenerator(this.map);
        this.map.structureGen.init();
        this.loadBiomes(biomes);
        Dorfgen.instance.addDorfMap(this.map);
    }

    BufferedImage getImage(final String file)
    {
        if (file.isEmpty()) return null;
        BufferedImage ret = null;
        try
        {
            final InputStream res = new FileInputStream(file);
            ret = ImageIO.read(res);
        }
        catch (final Exception e)
        {
            Dorfgen.LOGGER.info("Cannot Find File: " + file, new Exception(e));
        }

        return ret;
    }

    public void loadLegends(final String file)
    {
        try
        {
            final DocumentBuilderFactory dbFactory = DocumentBuilderFactory.newInstance();
            final DocumentBuilder dBuilder = dbFactory.newDocumentBuilder();
            final Document doc = dBuilder.parse(new FileInputStream(file));
            doc.getDocumentElement().normalize();

            final NodeList siteList = doc.getElementsByTagName("site");

            for (int i = 0; i < siteList.getLength(); i++)
            {
                final Node siteNode = siteList.item(i);
                int id = -1;
                String typeName = null;
                String name = null;
                String coords = null;
                for (int j = 0; j < siteNode.getChildNodes().getLength(); j++)
                {
                    final Node node = siteNode.getChildNodes().item(j);
                    final String nodeName = node.getNodeName();
                    if (nodeName.equals("id")) id = Integer.parseInt(node.getFirstChild().getNodeValue());
                    if (nodeName.equals("name")) name = node.getFirstChild().getNodeValue();
                    if (nodeName.equals("type")) typeName = node.getFirstChild().getNodeValue();
                    if (nodeName.equals("coords")) coords = node.getFirstChild().getNodeValue();
                }
                if (id == -1) continue;
                final SiteType type = SiteType.getSite(typeName);
                final String[] args = coords.split(",");
                int x = Integer.parseInt(args[0]);
                int z = Integer.parseInt(args[1]);
                final Site site = new Site(this.map, name, id, type, x, z);
                if (this.sites.containsKey(id))
                {
                    final BufferedImage image = this.sites.get(id);
                    site.rgbmap = new int[image.getWidth()][image.getHeight()];
                    for (x = 0; x < image.getWidth(); x++)
                        for (z = 0; z < image.getHeight(); z++)
                            site.rgbmap[x][z] = image.getRGB(x, z);
                    this.sites.remove(id);
                }
                this.map.sitesById.put(id, site);
                this.map.sitesByName.put(site.name, site);
            }

            NodeList regionList = doc.getElementsByTagName("region");
            for (int i = 0; i < regionList.getLength(); i++)
            {
                final Node regionNode = regionList.item(i);
                int id = -1;
                String typeName = null;
                String name = null;
                for (int j = 0; j < regionNode.getChildNodes().getLength(); j++)
                {
                    final Node node = regionNode.getChildNodes().item(j);
                    final String nodeName = node.getNodeName();
                    if (nodeName.equals("id")) id = Integer.parseInt(node.getFirstChild().getNodeValue());
                    if (nodeName.equals("name")) name = node.getFirstChild().getNodeValue();
                    if (nodeName.equals("type")) typeName = node.getFirstChild().getNodeValue();
                }
                final Region region = new Region(this.map, id, name, RegionType.valueOf(typeName.toUpperCase()));
                this.map.regionsById.put(id, region);
            }

            regionList = doc.getElementsByTagName("underground_region");
            for (int i = 0; i < regionList.getLength(); i++)
            {
                final Node regionNode = regionList.item(i);
                int id = -1;
                String typeName = null;
                String name = null;
                int depth = 0;
                for (int j = 0; j < regionNode.getChildNodes().getLength(); j++)
                {
                    final Node node = regionNode.getChildNodes().item(j);
                    final String nodeName = node.getNodeName();
                    if (nodeName.equals("id")) id = Integer.parseInt(node.getFirstChild().getNodeValue());
                    if (nodeName.equals("name")) name = node.getFirstChild().getNodeValue();
                    if (nodeName.equals("type")) typeName = node.getFirstChild().getNodeValue();
                    if (nodeName.equals("depth")) depth = Integer.parseInt(node.getFirstChild().getNodeValue());
                }

                final Region region = new Region(this.map, id, name, depth, RegionType.valueOf(typeName.toUpperCase()));
                this.map.ugRegionsById.put(id, region);
            }
        }
        catch (final Exception e)
        {
            e.printStackTrace();
        }
    }

    public List<String> loadLegendsPlus(final String file)
    {
        final List<String> names = new ArrayList<>();
        try
        {
            final DocumentBuilderFactory dbFactory = DocumentBuilderFactory.newInstance();
            final DocumentBuilder dBuilder = dbFactory.newDocumentBuilder();
            final Document doc = dBuilder.parse(new FileInputStream(file));
            doc.getDocumentElement().normalize();

            final NodeList siteList = doc.getElementsByTagName("site");

            names.add(doc.getElementsByTagName("name").item(0).getFirstChild().getNodeValue());
            names.add(doc.getElementsByTagName("altname").item(0).getFirstChild().getNodeValue());

            for (int i = 0; i < siteList.getLength(); i++)
            {
                final Node siteNode = siteList.item(i);
                int id = -1;
                final HashSet<Structure> toAdd = new HashSet<>();
                for (int j = 0; j < siteNode.getChildNodes().getLength(); j++)
                {
                    final Node node = siteNode.getChildNodes().item(j);
                    final String nodeName = node.getNodeName();
                    if (nodeName.equals("id")) id = Integer.parseInt(node.getFirstChild().getNodeValue());
                    if (nodeName.equals("structures"))
                    {
                        final NodeList structures = node.getChildNodes();
                        for (int k = 0; k < structures.getLength(); k++)
                        {
                            final Node structure = structures.item(k);
                            if (structure.getNodeName().equals("structure"))
                            {
                                final NodeList structureList = structure.getChildNodes();
                                int structId = -1;
                                StructureType structType = null;
                                String name = "";
                                String name2 = "";

                                for (int l = 0; l < structureList.getLength(); l++)
                                {
                                    final String subName = structureList.item(l).getNodeName();
                                    if (subName.equals("id")) structId = Integer.parseInt(structureList.item(l)
                                            .getFirstChild().getNodeValue());
                                    if (subName.equals("type"))
                                    {
                                        final String typeName = structureList.item(l).getFirstChild().getNodeValue();
                                        for (final StructureType t : StructureType.values())
                                            if (t.name.equals(typeName))
                                            {
                                                structType = t;
                                                break;
                                            }
                                    }
                                    if (subName.equals("name")) name = structureList.item(l).getFirstChild()
                                            .getNodeValue();
                                    if (subName.equals("name2")) name2 = structureList.item(l).getFirstChild()
                                            .getNodeValue();
                                }
                                if (structId == -1) continue;
                                toAdd.add(new Structure(this.map, name, name2, structId, structType));
                            }
                        }
                    }
                }
                if (id == -1) continue;
                final Site site = this.map.sitesById.get(id);
                if (site != null) site.structures.addAll(toAdd);
                else Dorfgen.LOGGER.info("No Site found for id: " + id + " " + this.map.name, new Exception());

            }

            NodeList regionList = doc.getElementsByTagName("region");
            for (int i = 0; i < regionList.getLength(); i++)
            {
                final Node regionNode = regionList.item(i);
                int id = -1;
                String coords = "";
                String[] toAdd = null;
                for (int j = 0; j < regionNode.getChildNodes().getLength(); j++)
                {
                    final Node node = regionNode.getChildNodes().item(j);
                    final String nodeName = node.getNodeName();
                    if (nodeName.equals("id")) id = Integer.parseInt(node.getFirstChild().getNodeValue());
                    if (nodeName.equals("coords")) coords = node.getFirstChild().getNodeValue();
                }
                toAdd = coords.split("\\|");
                if (id != -1 && toAdd != null)
                {
                    final Region region = this.map.regionsById.get(id);
                    if (region != null)
                    {
                        region.coords.clear();
                        for (final String s : toAdd)
                        {
                            final int x = Integer.parseInt(s.split(",")[0]);
                            final int z = Integer.parseInt(s.split(",")[1]);
                            region.coords.add(x + 2048 * z + region.depth * 4194304);
                        }
                    }
                    else Dorfgen.LOGGER.info("No Region found for id: " + id + " " + this.map.name, new Exception());
                }
            }

            regionList = doc.getElementsByTagName("underground_region");
            for (int i = 0; i < regionList.getLength(); i++)
            {
                final Node regionNode = regionList.item(i);
                int id = -1;
                String coords = "";
                String[] toAdd = null;
                for (int j = 0; j < regionNode.getChildNodes().getLength(); j++)
                {
                    final Node node = regionNode.getChildNodes().item(j);
                    final String nodeName = node.getNodeName();
                    if (nodeName.equals("id")) id = Integer.parseInt(node.getFirstChild().getNodeValue());
                    if (nodeName.equals("coords")) coords = node.getFirstChild().getNodeValue();
                }
                toAdd = coords.split("\\|");
                if (id != -1 && toAdd != null)
                {
                    final Region region = this.map.ugRegionsById.get(id);
                    if (region != null)
                    {
                        region.coords.clear();
                        for (final String s : toAdd)
                        {
                            final int x = Integer.parseInt(s.split(",")[0]);
                            final int z = Integer.parseInt(s.split(",")[1]);
                            region.coords.add(x + 2048 * z + region.depth * 4194304);
                        }
                    }
                    else Dorfgen.LOGGER.info("No Region found for id: " + id + " " + this.map.name, new Exception());
                }
            }

            regionList = doc.getElementsByTagName("world_construction");
            for (int i = 0; i < regionList.getLength(); i++)
            {
                final Node regionNode = regionList.item(i);
                int id = -1;
                String name = "";
                String type = "";
                String coords = "";
                String[] toAdd = null;
                for (int j = 0; j < regionNode.getChildNodes().getLength(); j++)
                {
                    final Node node = regionNode.getChildNodes().item(j);
                    final String nodeName = node.getNodeName();
                    if (nodeName.equals("id")) id = Integer.parseInt(node.getFirstChild().getNodeValue());
                    if (nodeName.equals("coords")) coords = node.getFirstChild().getNodeValue();
                    if (nodeName.equals("name")) name = node.getFirstChild().getNodeValue();
                    if (nodeName.equals("type")) type = node.getFirstChild().getNodeValue();
                }
                toAdd = coords.split("\\|");

                if (id != -1 && toAdd != null)
                {
                    final ConstructionType cType = ConstructionType.valueOf(type.toUpperCase());
                    if (cType != null)
                    {
                        final WorldConstruction construct = new WorldConstruction(this.map, id, name, cType);
                        construct.worldCoords.clear();
                        for (final String s : toAdd)
                        {
                            final int x = Integer.parseInt(s.split(",")[0]);
                            final int z = Integer.parseInt(s.split(",")[1]);
                            final int index = x + 2048 * z;
                            construct.worldCoords.add(index);
                            Set<WorldConstruction> constructs = this.map.constructionsByCoord.get(index);
                            if (constructs == null)
                            {
                                constructs = new HashSet<>();
                                this.map.constructionsByCoord.put(index, constructs);
                            }
                            constructs.add(construct);
                        }
                        this.map.constructionsById.put(id, construct);
                    }
                    else Dorfgen.LOGGER.warn("Unknown Construction Type: " + type + " " + this.map.name,
                            new Exception());
                }
            }
        }
        catch (final Exception e)
        {
            e.printStackTrace();
        }
        return names;
    }

    public void loadFineConstructLocations(final String file)
    {
        final ArrayList<String> rows = new ArrayList<>();
        BufferedReader br = null;
        String line = "";

        try
        {
            final InputStream res = new FileInputStream(file);
            br = new BufferedReader(new InputStreamReader(res));
            while ((line = br.readLine()) != null)
                rows.add(line);

            for (final String entry : rows)
            {
                final String[] args = entry.split(":");
                final int id = Integer.parseInt(args[0]);
                final WorldConstruction construct = this.map.constructionsById.get(id);
                if (construct != null) for (int i = 1; i < args.length; i++)
                {
                    final String[] coordString = args[i].split(",");
                    final int x = Integer.parseInt(coordString[0]);
                    final int y = Integer.parseInt(coordString[1]);
                    final int z = Integer.parseInt(coordString[2]);
                    final int index = x + 8192 * z;
                    construct.embarkCoords.put(index, y);
                }
                else Dorfgen.LOGGER.warn("Cannot Find Construction for id:" + id + " " + this.map.name,
                        new NullPointerException());
            }
        }
        catch (final Exception e)
        {

        }
    }

    public void loadSiteInfo(final String file)
    {
        final ArrayList<String> rows = new ArrayList<>();
        BufferedReader br = null;
        String line = "";

        try
        {
            final InputStream res = new FileInputStream(file);
            br = new BufferedReader(new InputStreamReader(res));
            int n = 0;
            while ((line = br.readLine()) != null)
                rows.add(line);

            for (final String entry : rows)
            {
                final String[] args = entry.split(":");
                final int id = Integer.parseInt(args[0]);
                final Site site = this.map.sitesById.get(id);
                if (site != null)
                {
                    final String[] corner1 = args[1].split("->")[0].split(",");
                    final int x1 = Integer.parseInt(corner1[0]);
                    final int y1 = Integer.parseInt(corner1[1]);
                    final String[] corner2 = args[1].split("->")[1].split(",");
                    final int x2 = Integer.parseInt(corner2[0]);
                    final int y2 = Integer.parseInt(corner2[1]);
                    site.setSiteLocation(x1, y1, x2, y2);
                    n++;
                }
                else Dorfgen.LOGGER.error("Cannot Find Site for id:" + id + " " + this.map.name,
                        new NullPointerException());
            }
            Dorfgen.LOGGER.info("Imported locations for " + n + " Sites for " + this.map.name);
        }
        catch (final Exception e)
        {
            Dorfgen.LOGGER.error("Error with file " + file + " " + this.map.name, e);
        }
    }

    private void loadBiomes(final File biomes)
    {
        final ArrayList<ArrayList<String>> rows = new ArrayList<>();
        BufferedReader br = null;
        String line = "";
        final String cvsSplitBy = ",";

        // try
        // {
        // InputStream res = new FileInputStream(biomes);
        // br = new BufferedReader(new InputStreamReader(res));
        // int n = 0;
        // while ((line = br.readLine()) != null)
        // {
        //
        // String[] row = line.split(cvsSplitBy);
        // rows.add(new ArrayList<String>());
        // for (int i = 0; i < row.length; i++)
        // {
        // rows.get(n).add(row[i]);
        // }
        // n++;
        // }
        //
        // }
        // catch (FileNotFoundException e)
        {

            FileWriter fwriter;
            PrintWriter out;
            try
            {
                fwriter = new FileWriter(biomes);
                out = new PrintWriter(fwriter);

                final String defaultPath = "/assets/dorfgen/biome_mappings.csv";
                final ArrayList<String> lines = new ArrayList<>();
                InputStream res;
                try
                {
                    res = this.getClass().getResourceAsStream(defaultPath);

                    br = new BufferedReader(new InputStreamReader(res));
                    int n = 0;

                    while ((line = br.readLine()) != null)
                    {
                        lines.add(line);
                        final String[] row = line.split(cvsSplitBy);
                        rows.add(new ArrayList<String>());
                        for (final String element : row)
                            rows.get(n).add(element);
                        n++;
                    }
                }
                catch (final Exception e1)
                {
                    e1.printStackTrace();
                }
                for (final String s : lines)
                    out.println(s);

                out.close();
                fwriter.close();

            }
            catch (final IOException e2)
            {
            }

        }
        // catch (NullPointerException e)
        // {
        // e.printStackTrace();
        // }
        // catch (IOException e)
        // {
        // e.printStackTrace();
        // }
        // finally
        // {
        // if (br != null)
        // {
        // try
        // {
        // br.close();
        // }
        // catch (IOException e)
        // {
        // e.printStackTrace();
        // }
        // }
        // }

        for (int i = 1; i < rows.size(); i++)
        {
            final ArrayList<String> row = rows.get(i);
            int r;
            int g;
            int b;
            final List<Type> types = Lists.newArrayList();
            r = Integer.parseInt(row.get(0));
            g = Integer.parseInt(row.get(1));
            b = Integer.parseInt(row.get(2));
            final String typeString = row.get(3);
            final String[] args = typeString.split(" ");
            for (final String s : args)
            {
                final Type type = FileLoader.getBiomeType(s);
                if (type != null) types.add(type);
                else Dorfgen.LOGGER.warn("No Biome type by name: " + s + " " + this.map.name);
            }

            // first row is blank, with colour of black.
            if (types.isEmpty())
            {
                Dorfgen.LOGGER.warn("Error in row " + i + " " + row + " " + this.map.name);
                continue;
            }
            final Color c = new Color(r, g, b);
            this.map.biomeList.biomes.put(c.getRGB(), new BiomeConversion(c, types.toArray(new Type[0])));
        }

    }

    public static Type getBiomeType(final String name)
    {
        return Type.getType(name.toUpperCase(Locale.ROOT));
    }
}