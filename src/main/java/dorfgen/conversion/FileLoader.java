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
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.logging.Level;

import javax.imageio.ImageIO;
import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;

import org.w3c.dom.Document;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

import com.google.common.collect.Lists;

// import com.sun.org.apache.xerces.internal.dom.DeferredElementImpl;

import MappedXML.MappedTruncate;
import dorfgen.WorldGenerator;
import dorfgen.conversion.DorfMap.ConstructionType;
import dorfgen.conversion.DorfMap.Region;
import dorfgen.conversion.DorfMap.RegionType;
import dorfgen.conversion.DorfMap.Site;
import dorfgen.conversion.DorfMap.SiteType;
import dorfgen.conversion.DorfMap.Structure;
import dorfgen.conversion.DorfMap.StructureType;
import dorfgen.conversion.DorfMap.WorldConstruction;
import net.minecraftforge.common.BiomeDictionary.Type;
import net.minecraftforge.fml.relauncher.ReflectionHelper;

public class FileLoader
{

    public File                            resourceDir            = null;
    final DorfMap                          map;

    public String                          elevation              = "";
    public String                          elevationWater         = "";
    public String                          biome                  = "";
    public String                          temperature            = "";
    public String                          evil                   = "";
    public String                          rain                   = "";
    public String                          drainage               = "";
    public String                          volcanism              = "";
    public String                          vegitation             = "";
    public String                          structs                = "";
    public String                          legends                = "";
    public String                          legendsPlus            = "";
    public String                          constructionFineCoords = "";
    public String                          siteInfo               = "";

    public HashMap<Integer, BufferedImage> sites                  = new HashMap<Integer, BufferedImage>();

    public FileLoader(File folder)
    {
        resourceDir = folder;
        String name = folder.getName();
        if (WorldGenerator.instance.defaultRegion.isEmpty()) WorldGenerator.instance.defaultRegion = name;
        this.map = new DorfMap(name);
        this.map.resourceDir = resourceDir;
        File biomes = new File(folder, "biome_mappings.csv");
        boolean noRegion = true;
        for (File f : folder.listFiles())
        {
            String s = f.getName();
            if (f.isDirectory() && s.contains("site_maps"))
            {
                for (File f1 : f.listFiles())
                {
                    s = f1.getName();
                    if (s.contains("-site_map-"))
                    {
                        String[] args = s.split("-");
                        String s1 = args[args.length - 1].replace(".png", "").replace(".bmp", "");
                        Integer id = Integer.parseInt(s1);
                        BufferedImage site = getImage(f1.getAbsolutePath());
                        if (site != null) sites.put(id, site);
                        else WorldGenerator.log("Site " + id + " did not read correctly. " + s + " " + map.name);
                    }
                }
            }
            else if (f.isDirectory() && s.contains("region_maps"))
            {
                noRegion = false;
                for (File f1 : f.listFiles())
                {
                    s = f1.getAbsolutePath();
                    if (s.contains("-el."))
                    {
                        elevation = s;
                    }
                    else if (s.contains("-elw."))
                    {
                        elevationWater = s;
                    }
                    else if (s.contains("-bm."))
                    {
                        biome = s;
                    }
                    else if (s.contains("-rain."))
                    {
                        rain = s;
                    }
                    else if (s.contains("-drn."))
                    {
                        rain = s;
                    }
                    else if (s.contains("-tmp."))
                    {
                        temperature = s;
                    }
                    else if (s.contains("-vol."))
                    {
                        volcanism = s;
                    }
                    else if (s.contains("-veg."))
                    {
                        vegitation = s;
                    }
                    else if (s.contains("-evil."))
                    {
                        evil = s;
                    }
                    else if (s.contains("-str."))
                    {
                        structs = s;
                    }
                }
            }
            else if (!f.isDirectory())
            {
                s = f.getAbsolutePath();
                if (s.contains("-legends") && !s.contains("plus"))
                {
                    legends = s;
                }
                else if (s.contains("-legends_plus"))
                {
                    legendsPlus = s;
                }
                else if (s.contains("constructs.txt"))
                {
                    constructionFineCoords = s;
                }
                else if (s.contains("sites.txt"))
                {
                    siteInfo = s;
                }
            }
        }
        if (noRegion)
        {
            WorldGenerator.log(Level.SEVERE, "No Region maps found for " + name);
            return;
        }
        WorldGenerator.log("Loading Region " + name);

        if (!legends.contains("trunc"))
        {
            MappedTruncate.ReadTruncateAndOutput(legends, legends.replace(".xml", "_trunc.xml"),
                    "<artifacts>", "\n</df_world>", true);
            legends = legends.replace(".xml", "_trunc.xml");
        }
        if (!legendsPlus.contains("trunc"))
        {
            MappedTruncate.ReadTruncateAndOutput(legendsPlus, legendsPlus.replace(".xml", "_trunc.xml"),
                    "<artifacts>", "\n</df_world>", true);
            legendsPlus = legendsPlus.replace(".xml", "_trunc.xml");
        }
        loadLegends(legends);
        loadLegendsPlus(legendsPlus);
        if (!constructionFineCoords.isEmpty()) loadFineConstructLocations(constructionFineCoords);
        if (!siteInfo.isEmpty()) loadSiteInfo(siteInfo);

        map.images.biomeMap = getImage(biome);
        map.images.elevationMap = getImage(elevation);
        map.images.elevationWaterMap = getImage(elevationWater);
        map.images.temperatureMap = getImage(temperature);
        map.images.vegitationMap = getImage(vegitation);
        map.images.structuresMap = getImage(structs);
        map.images.drainageMap = getImage(drainage);
        map.images.rainMap = getImage(rain);
        map.images.volcanismMap = getImage(volcanism);

        map.init();
        map.structureGen = new SiteStructureGenerator(map);
        map.structureGen.init();
        loadBiomes(biomes);
        WorldGenerator.setMap(name, map);
    }

    BufferedImage getImage(String file)
    {
        if (file.isEmpty()) return null;
        BufferedImage ret = null;
        try
        {
            InputStream res = new FileInputStream(file);
            ret = ImageIO.read(res);
        }
        catch (Exception e)
        {
            WorldGenerator.log("Cannot Find File: " + file, new Exception(e));
        }

        return ret;
    }

    public void loadLegends(String file)
    {
        try
        {
            DocumentBuilderFactory dbFactory = DocumentBuilderFactory.newInstance();
            DocumentBuilder dBuilder = dbFactory.newDocumentBuilder();
            Document doc = dBuilder.parse(new FileInputStream(file));
            doc.getDocumentElement().normalize();

            NodeList siteList = doc.getElementsByTagName("site");

            for (int i = 0; i < siteList.getLength(); i++)
            {
                Node siteNode = siteList.item(i);
                int id = -1;
                String typeName = null;
                String name = null;
                String coords = null;
                for (int j = 0; j < siteNode.getChildNodes().getLength(); j++)
                {
                    Node node = siteNode.getChildNodes().item(j);
                    String nodeName = node.getNodeName();
                    if (nodeName.equals("id"))
                    {
                        id = Integer.parseInt(node.getFirstChild().getNodeValue());
                    }
                    if (nodeName.equals("name"))
                    {
                        name = node.getFirstChild().getNodeValue();
                    }
                    if (nodeName.equals("type"))
                    {
                        typeName = node.getFirstChild().getNodeValue();
                    }
                    if (nodeName.equals("coords"))
                    {
                        coords = node.getFirstChild().getNodeValue();
                    }
                }
                if (id == -1) continue;
                SiteType type = SiteType.getSite(typeName);
                String[] args = coords.split(",");
                int x = Integer.parseInt(args[0]);
                int z = Integer.parseInt(args[1]);
                Site site = new Site(map, name, id, type, x, z);
                if (sites.containsKey(id))
                {
                    BufferedImage image = sites.get(id);
                    site.rgbmap = new int[image.getWidth()][image.getHeight()];
                    for (x = 0; x < image.getWidth(); x++)
                    {
                        for (z = 0; z < image.getHeight(); z++)
                        {
                            site.rgbmap[x][z] = image.getRGB(x, z);
                        }
                    }
                    sites.remove(id);
                }
                map.sitesById.put(id, site);
            }

            NodeList regionList = doc.getElementsByTagName("region");
            for (int i = 0; i < regionList.getLength(); i++)
            {
                Node regionNode = regionList.item(i);
                int id = -1;
                String typeName = null;
                String name = null;
                for (int j = 0; j < regionNode.getChildNodes().getLength(); j++)
                {
                    Node node = regionNode.getChildNodes().item(j);
                    String nodeName = node.getNodeName();
                    if (nodeName.equals("id"))
                    {
                        id = Integer.parseInt(node.getFirstChild().getNodeValue());
                    }
                    if (nodeName.equals("name"))
                    {
                        name = node.getFirstChild().getNodeValue();
                    }
                    if (nodeName.equals("type"))
                    {
                        typeName = node.getFirstChild().getNodeValue();
                    }
                }
                Region region = new Region(map, id, name, RegionType.valueOf(typeName.toUpperCase()));
                map.regionsById.put(id, region);
            }

            regionList = doc.getElementsByTagName("underground_region");
            for (int i = 0; i < regionList.getLength(); i++)
            {
                Node regionNode = regionList.item(i);
                int id = -1;
                String typeName = null;
                String name = null;
                int depth = 0;
                for (int j = 0; j < regionNode.getChildNodes().getLength(); j++)
                {
                    Node node = regionNode.getChildNodes().item(j);
                    String nodeName = node.getNodeName();
                    if (nodeName.equals("id"))
                    {
                        id = Integer.parseInt(node.getFirstChild().getNodeValue());
                    }
                    if (nodeName.equals("name"))
                    {
                        name = node.getFirstChild().getNodeValue();
                    }
                    if (nodeName.equals("type"))
                    {
                        typeName = node.getFirstChild().getNodeValue();
                    }
                    if (nodeName.equals("depth"))
                    {
                        depth = Integer.parseInt(node.getFirstChild().getNodeValue());
                    }
                }

                Region region = new Region(map, id, name, depth, RegionType.valueOf(typeName.toUpperCase()));
                map.ugRegionsById.put(id, region);
            }
        }
        catch (Exception e)
        {
            e.printStackTrace();
        }
    }

    public void loadLegendsPlus(String file)
    {
        try
        {
            DocumentBuilderFactory dbFactory = DocumentBuilderFactory.newInstance();
            DocumentBuilder dBuilder = dbFactory.newDocumentBuilder();
            Document doc = dBuilder.parse(new FileInputStream(file));
            doc.getDocumentElement().normalize();

            NodeList siteList = doc.getElementsByTagName("site");

            for (int i = 0; i < siteList.getLength(); i++)
            {
                Node siteNode = siteList.item(i);
                int id = -1;
                HashSet<Structure> toAdd = new HashSet<Structure>();
                for (int j = 0; j < siteNode.getChildNodes().getLength(); j++)
                {
                    Node node = siteNode.getChildNodes().item(j);
                    String nodeName = node.getNodeName();
                    if (nodeName.equals("id"))
                    {
                        id = Integer.parseInt(node.getFirstChild().getNodeValue());
                    }
                    if (nodeName.equals("structures"))
                    {
                        NodeList structures = node.getChildNodes();
                        for (int k = 0; k < structures.getLength(); k++)
                        {
                            Node structure = structures.item(k);
                            if (structure.getNodeName().equals("structure"))
                            {
                                NodeList structureList = structure.getChildNodes();
                                int structId = -1;
                                StructureType structType = null;
                                String name = "";
                                String name2 = "";

                                for (int l = 0; l < structureList.getLength(); l++)
                                {
                                    String subName = structureList.item(l).getNodeName();
                                    if (subName.equals("id"))
                                    {
                                        structId = Integer
                                                .parseInt(structureList.item(l).getFirstChild().getNodeValue());
                                    }
                                    if (subName.equals("type"))
                                    {
                                        String typeName = structureList.item(l).getFirstChild().getNodeValue();
                                        for (StructureType t : StructureType.values())
                                        {
                                            if (t.name.equals(typeName))
                                            {
                                                structType = t;
                                                break;
                                            }
                                        }
                                    }
                                    if (subName.equals("name"))
                                    {
                                        name = structureList.item(l).getFirstChild().getNodeValue();
                                    }
                                    if (subName.equals("name2"))
                                    {
                                        name2 = structureList.item(l).getFirstChild().getNodeValue();
                                    }
                                }
                                if (structId == -1) continue;
                                toAdd.add(new Structure(map, name, name2, structId, structType));
                            }
                        }
                    }
                }
                if (id == -1) continue;
                Site site = map.sitesById.get(id);
                if (site != null)
                {
                    site.structures.addAll(toAdd);
                }
                else
                {
                    WorldGenerator.log("No Site found for id: " + id + " " + map.name, new Exception());
                }

            }

            NodeList regionList = doc.getElementsByTagName("region");
            for (int i = 0; i < regionList.getLength(); i++)
            {
                Node regionNode = regionList.item(i);
                int id = -1;
                String coords = "";
                String[] toAdd = null;
                for (int j = 0; j < regionNode.getChildNodes().getLength(); j++)
                {
                    Node node = regionNode.getChildNodes().item(j);
                    String nodeName = node.getNodeName();
                    if (nodeName.equals("id"))
                    {
                        id = Integer.parseInt(node.getFirstChild().getNodeValue());
                    }
                    if (nodeName.equals("coords"))
                    {
                        coords = node.getFirstChild().getNodeValue();
                    }
                }
                toAdd = coords.split("\\|");
                // System.out.println(toAdd.length+": "+coords.length()+":
                // "+coords);
                if (id != -1 && toAdd != null)
                {
                    Region region = map.regionsById.get(id);
                    if (region != null)
                    {
                        region.coords.clear();
                        for (String s : toAdd)
                        {
                            int x = Integer.parseInt(s.split(",")[0]);
                            int z = Integer.parseInt(s.split(",")[1]);
                            region.coords.add(x + 2048 * z + region.depth * 4194304);
                        }
                    }
                    else
                    {
                        WorldGenerator.log("No Region found for id: " + id + " " + map.name, new Exception());
                    }
                }
            }

            regionList = doc.getElementsByTagName("underground_region");
            for (int i = 0; i < regionList.getLength(); i++)
            {
                Node regionNode = regionList.item(i);
                int id = -1;
                String coords = "";
                String[] toAdd = null;
                for (int j = 0; j < regionNode.getChildNodes().getLength(); j++)
                {
                    Node node = regionNode.getChildNodes().item(j);
                    String nodeName = node.getNodeName();
                    if (nodeName.equals("id"))
                    {
                        id = Integer.parseInt(node.getFirstChild().getNodeValue());
                    }
                    if (nodeName.equals("coords"))
                    {
                        coords = node.getFirstChild().getNodeValue();
                    }
                }
                toAdd = coords.split("\\|");
                // System.out.println(toAdd.length+": "+coords.length()+":
                // "+coords);
                if (id != -1 && toAdd != null)
                {
                    Region region = map.ugRegionsById.get(id);
                    if (region != null)
                    {
                        region.coords.clear();
                        for (String s : toAdd)
                        {
                            int x = Integer.parseInt(s.split(",")[0]);
                            int z = Integer.parseInt(s.split(",")[1]);
                            region.coords.add(x + 2048 * z + region.depth * 4194304);
                        }
                    }
                    else
                    {
                        WorldGenerator.log("No Region found for id: " + id + " " + map.name, new Exception());
                    }
                }
            }

            regionList = doc.getElementsByTagName("world_construction");
            for (int i = 0; i < regionList.getLength(); i++)
            {
                Node regionNode = regionList.item(i);
                int id = -1;
                String name = "";
                String type = "";
                String coords = "";
                String[] toAdd = null;
                for (int j = 0; j < regionNode.getChildNodes().getLength(); j++)
                {
                    Node node = regionNode.getChildNodes().item(j);
                    String nodeName = node.getNodeName();
                    if (nodeName.equals("id"))
                    {
                        id = Integer.parseInt(node.getFirstChild().getNodeValue());
                    }
                    if (nodeName.equals("coords"))
                    {
                        coords = node.getFirstChild().getNodeValue();
                    }
                    if (nodeName.equals("name"))
                    {
                        name = node.getFirstChild().getNodeValue();
                    }
                    if (nodeName.equals("type"))
                    {
                        type = node.getFirstChild().getNodeValue();
                    }
                }
                toAdd = coords.split("\\|");
                // System.out.println(toAdd.length+": "+coords.length()+":
                // "+coords);
                if (id != -1 && toAdd != null)
                {
                    ConstructionType cType = ConstructionType.valueOf(type.toUpperCase());
                    if (cType != null)
                    {
                        WorldConstruction construct = new WorldConstruction(map, id, name, cType);
                        construct.worldCoords.clear();
                        for (String s : toAdd)
                        {
                            int x = Integer.parseInt(s.split(",")[0]);
                            int z = Integer.parseInt(s.split(",")[1]);
                            int index = x + 2048 * z;
                            construct.worldCoords.add(index);
                            HashSet<WorldConstruction> constructs = map.constructionsByCoord.get(index);
                            if (constructs == null)
                            {
                                constructs = new HashSet<>();
                                map.constructionsByCoord.put(index, constructs);
                            }
                            constructs.add(construct);
                        }
                        map.constructionsById.put(id, construct);
                    }
                    else
                    {
                        WorldGenerator.log(Level.WARNING, "Unknown Construction Type: " + type + " " + map.name,
                                new Exception());
                    }
                }
            }
        }
        catch (Exception e)
        {
            e.printStackTrace();
        }
    }

    public void loadFineConstructLocations(String file)
    {
        ArrayList<String> rows = new ArrayList<String>();
        BufferedReader br = null;
        String line = "";

        try
        {
            InputStream res = new FileInputStream(file);
            br = new BufferedReader(new InputStreamReader(res));
            while ((line = br.readLine()) != null)
            {
                rows.add(line);
            }

            for (String entry : rows)
            {
                String[] args = entry.split(":");
                int id = Integer.parseInt(args[0]);
                WorldConstruction construct = map.constructionsById.get(id);
                if (construct != null)
                {
                    for (int i = 1; i < args.length; i++)
                    {
                        String[] coordString = args[i].split(",");
                        int x = Integer.parseInt(coordString[0]);
                        int y = Integer.parseInt(coordString[1]);
                        int z = Integer.parseInt(coordString[2]);
                        int index = x + 8192 * z;
                        construct.embarkCoords.put(index, y);
                    }
                }
                else
                {
                    WorldGenerator.log(Level.WARNING, "Cannot Find Construction for id:" + id + " " + map.name,
                            new NullPointerException());
                }
            }
        }
        catch (Exception e)
        {

        }
    }

    public void loadSiteInfo(String file)
    {
        ArrayList<String> rows = new ArrayList<String>();
        BufferedReader br = null;
        String line = "";

        try
        {
            InputStream res = new FileInputStream(file);
            br = new BufferedReader(new InputStreamReader(res));
            int n = 0;
            while ((line = br.readLine()) != null)
            {
                rows.add(line);
            }

            for (String entry : rows)
            {
                String[] args = entry.split(":");
                int id = Integer.parseInt(args[0]);

                Site site = map.sitesById.get(id);

                if (site != null)
                {
                    String[] corner1 = args[1].split("->")[0].split(",");
                    int x1 = Integer.parseInt(corner1[0]);
                    int y1 = Integer.parseInt(corner1[1]);
                    String[] corner2 = args[1].split("->")[1].split(",");
                    int x2 = Integer.parseInt(corner2[0]);
                    int y2 = Integer.parseInt(corner2[1]);
                    site.setSiteLocation(x1, y1, x2, y2);
                    n++;
                }
                else
                {
                    WorldGenerator.log(Level.WARNING, "Cannot Find Site for id:" + id + " " + map.name, new NullPointerException());
                }
            }
            WorldGenerator.log("Imported locations for " + n + " Sites for " + map.name);
        }
        catch (Exception e)
        {
            WorldGenerator.log("Error with file " + file + " " + map.name, e);
        }
    }

    private void loadBiomes(File biomes)
    {
        ArrayList<ArrayList<String>> rows = new ArrayList<ArrayList<String>>();
        BufferedReader br = null;
        String line = "";
        String cvsSplitBy = ",";

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

                String defaultPath = "/assets/dorfgen/biome_mappings.csv";
                ArrayList<String> lines = new ArrayList<String>();
                InputStream res;
                try
                {
                    res = getClass().getResourceAsStream(defaultPath);

                    br = new BufferedReader(new InputStreamReader(res));
                    int n = 0;

                    while ((line = br.readLine()) != null)
                    {
                        lines.add(line);
                        String[] row = line.split(cvsSplitBy);
                        rows.add(new ArrayList<String>());
                        for (int i = 0; i < row.length; i++)
                        {
                            rows.get(n).add(row[i]);
                        }
                        n++;
                    }
                }
                catch (Exception e1)
                {
                    e1.printStackTrace();
                }
                for (String s : lines)
                {

                    out.println(s);
                }

                out.close();
                fwriter.close();

            }
            catch (IOException e2)
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
            ArrayList<String> row = rows.get(i);
            int r;
            int g;
            int b;
            List<Type> types = Lists.newArrayList();
            r = Integer.parseInt(row.get(0));
            g = Integer.parseInt(row.get(1));
            b = Integer.parseInt(row.get(2));
            String typeString = row.get(3);
            String[] args = typeString.split(" ");
            for (String s : args)
            {
                Type type = getBiomeType(s);
                if (type != null) types.add(type);
                else WorldGenerator.log(Level.WARNING, "No Biome type by name: " + s + " " + map.name);
            }

            // first row is blank, with colour of black.
            if (types.isEmpty())
            {
                WorldGenerator.log(Level.WARNING, "Error in row " + i + " " + row + " " + map.name);
                continue;
            }
            Color c = new Color(r, g, b);
            map.biomeList.biomes.put(c.getRGB(), new BiomeConversion(c, types.toArray(new Type[0])));
        }

    }

    private static final Map<String, Type> byName = ReflectionHelper.getPrivateValue(Type.class, null, "byName");

    public static Type getBiomeType(String name)
    {
        return byName.get(name.toUpperCase(Locale.ENGLISH));
    }
}
