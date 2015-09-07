package dorfgen.conversion;

import java.awt.Color;
import java.awt.image.BufferedImage;
import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.UUID;

import javax.imageio.ImageIO;
import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;

import org.w3c.dom.Document;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import org.xml.sax.SAXException;

//import com.sun.org.apache.xerces.internal.dom.DeferredElementImpl;

import MappedXML.MappedTruncate;
import dorfgen.WorldGenerator;
import dorfgen.conversion.DorfMap.Region;
import dorfgen.conversion.DorfMap.RegionType;
import dorfgen.conversion.DorfMap.Site;
import dorfgen.conversion.DorfMap.SiteType;
import dorfgen.conversion.DorfMap.Structure;
import dorfgen.conversion.DorfMap.StructureType;
import net.minecraft.world.biome.BiomeGenBase;

public class FileLoader {

	public static String IMAGELOCATION = "";
	public static String elevation = "";
	public static String elevationWater = "";
	public static String biome = "";
	public static String temperature = "";
	public static String evil = "";
	public static String rain = "";
	public static String volcanism = "";
	public static String vegitation = "";
	public static String biomes = "";
	public static String legends = "";
	public static String legendsPlus = "";

	public FileLoader() {
		File temp = new File(biome.replace("biome", "")); 
		File temp1;
		if(!temp.exists())
		{
			temp.mkdirs();
		}
		
		for(File f:temp.listFiles())
		{
			if(!f.isDirectory())
			{
				String s = f.getName();
				if(s.contains("-el."))
				{
					elevation = biomes.replace("biomes.csv", s);
				}
				else if(s.contains("-elw."))
				{
					elevationWater = biomes.replace("biomes.csv", s);
				}
				else if(s.contains("-bm."))
				{
					biome = biomes.replace("biomes.csv", s);
				}
				else if(s.contains("-rain."))
				{
					rain = biomes.replace("biomes.csv", s);
				}
				else if(s.contains("-tmp."))
				{
					temperature = biomes.replace("biomes.csv", s);
				}
				else if(s.contains("-vol."))
				{
					volcanism = biomes.replace("biomes.csv", s);
				}
				else if(s.contains("-veg."))
				{
					vegitation = biomes.replace("biomes.csv", s);
				}
				else if(s.contains("-evil."))
				{
					evil = biomes.replace("biomes.csv", s);
				}
				else if(s.contains("-legends."))
				{
					legends = biomes.replace("biomes.csv", s);
				}
				else if(s.contains("-legends_plus."))
				{
					legendsPlus = biomes.replace("biomes.csv", s);
				}
			}
		}
		MappedTruncate.ReadTruncateAndOutput(legends, legends.replace(".xml", "_trunc.xml"), "</sites>", "\n</df_world>");
		MappedTruncate.ReadTruncateAndOutput(legendsPlus, legendsPlus.replace(".xml", "_trunc.xml"), "</sites>", "\n</df_world>");

		legends = legends.replace(".xml", "_trunc.xml");
		legendsPlus = legendsPlus.replace(".xml", "_trunc.xml");
		
		loadLegends(legends);
		loadLegendsPlus(legendsPlus);
		
		WorldGenerator.instance.biomeMap = getImage(biome);
		WorldGenerator.instance.elevationMap = getImage(elevation);
		WorldGenerator.instance.elevationWaterMap = getImage(elevationWater);
		WorldGenerator.instance.temperatureMap = getImage(temperature);
		WorldGenerator.instance.vegitationMap = getImage(vegitation);

		loadBiomes(biomes);
	}

	BufferedImage getImage(String file) {
		BufferedImage ret = null;
		try {
			InputStream res = new FileInputStream(file);
			ret = ImageIO.read(res);
		} catch (Exception e) {
			System.err.println("Cannot find "+file);
		}

		return ret;
	}

	public static void loadLegends(String file)
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
				for(int j = 0; j < siteNode.getChildNodes().getLength(); j++)
				{
					Node node = siteNode.getChildNodes().item(j);
					String nodeName = node.getNodeName();
					if(nodeName.equals("id"))
					{
						id = Integer.parseInt(node.getFirstChild().getNodeValue());
					}
					if(nodeName.equals("name"))
					{
						name = node.getFirstChild().getNodeValue();
					}
					if(nodeName.equals("type"))
					{
						typeName = node.getFirstChild().getNodeValue();
					}
					if(nodeName.equals("coords"))
					{
						coords = node.getFirstChild().getNodeValue();
					}
				}
				if(id == -1)
					continue;
				SiteType type = SiteType.getSite(typeName);
				String[] args = coords.split(",");
				int x = Integer.parseInt(args[0]);
				int z = Integer.parseInt(args[1]);
				Site site = new Site(name, id, type, x, z);
				DorfMap.sitesByCoord.put(x + 2048 * z, site);
				DorfMap.sitesById.put(id, site);
			}
			
			NodeList regionList = doc.getElementsByTagName("region");
			for (int i = 0; i < regionList.getLength(); i++)
			{
				Node regionNode = regionList.item(i);
				int id = -1;
				String typeName = null;
				String name = null;
				String coords = null;
				for(int j = 0; j < regionNode.getChildNodes().getLength(); j++)
				{
					Node node = regionNode.getChildNodes().item(j);
					String nodeName = node.getNodeName();
					if(nodeName.equals("id"))
					{
						id = Integer.parseInt(node.getFirstChild().getNodeValue());
					}
					if(nodeName.equals("name"))
					{
						name = node.getFirstChild().getNodeValue();
					}
					if(nodeName.equals("type"))
					{
						typeName = node.getFirstChild().getNodeValue();
					}
				}
				Region region = new Region(id, name, RegionType.valueOf(typeName.toUpperCase()));
				DorfMap.regionsById.put(id, region);
			}
			
			regionList = doc.getElementsByTagName("underground_region");
			for (int i = 0; i < regionList.getLength(); i++)
			{
				Node regionNode = regionList.item(i);
				int id = -1;
				String typeName = null;
				String name = null;
				String coords = null;
				int depth = 0;
				for(int j = 0; j < regionNode.getChildNodes().getLength(); j++)
				{
					Node node = regionNode.getChildNodes().item(j);
					String nodeName = node.getNodeName();
					if(nodeName.equals("id"))
					{
						id = Integer.parseInt(node.getFirstChild().getNodeValue());
					}
					if(nodeName.equals("name"))
					{
						name = node.getFirstChild().getNodeValue();
					}
					if(nodeName.equals("type"))
					{
						typeName = node.getFirstChild().getNodeValue();
					}
					if(nodeName.equals("depth"))
					{
						depth = Integer.parseInt(node.getFirstChild().getNodeValue());
					}
				}
				
				Region region = new Region(id, name, depth, RegionType.valueOf(typeName.toUpperCase()));
				DorfMap.ugRegionsById.put(id, region);
			}
		}
		catch (Exception e)
		{
			e.printStackTrace();
		}
	}
	
	public static void loadLegendsPlus(String file)
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
				HashSet<Structure> toAdd = new HashSet();
				for(int j = 0; j < siteNode.getChildNodes().getLength(); j++)
				{
					Node node = siteNode.getChildNodes().item(j);
					String nodeName = node.getNodeName();
					if(nodeName.equals("id"))
					{
						id = Integer.parseInt(node.getFirstChild().getNodeValue());
					}
					if(nodeName.equals("structures"))
					{
						NodeList structures = node.getChildNodes();
						for(int k = 0; k< structures.getLength(); k++)
						{
							Node structure = structures.item(k);
							if(structure.getNodeName().equals("structure"))
							{
								NodeList structureList = structure.getChildNodes();
								int structId = -1;
								StructureType structType = null;
								String name = "";
								String name2 = "";
								
								for(int l = 0; l<structureList.getLength(); l++)
								{
									String subName = structureList.item(l).getNodeName();
									if(subName.equals("id"))
									{
										structId = Integer.parseInt(structureList.item(l).getFirstChild().getNodeValue());
									}
									if(subName.equals("type"))
									{
										String typeName = structureList.item(l).getFirstChild().getNodeValue();
										for(StructureType t: StructureType.values())
										{
											if(t.name.equals(typeName))
											{
												structType = t;
												break;
											}
										}
									}
									if(subName.equals("name"))
									{
										name = structureList.item(l).getFirstChild().getNodeValue();
									}
									if(subName.equals("name2"))
									{
										name2 = structureList.item(l).getFirstChild().getNodeValue();
									}
								}
								if(structId == -1)
									continue;
								toAdd.add(new Structure(name, name2, structId, structType));
							}
						}
					}
				}
				if(id == -1)
					continue;
				Site site = DorfMap.sitesById.get(id);
				if(site!=null)
				{
					site.structures.addAll(toAdd);
				}
				else
				{
					new Exception().printStackTrace();
				}
				
			}
			
			NodeList regionList = doc.getElementsByTagName("region");
			for (int i = 0; i < regionList.getLength(); i++)
			{
				Node regionNode = regionList.item(i);
				int id = -1;
				String coords = "";
				String[] toAdd = null;
				for(int j = 0; j < regionNode.getChildNodes().getLength(); j++)
				{
					Node node = regionNode.getChildNodes().item(j);
					String nodeName = node.getNodeName();
					if(nodeName.equals("id"))
					{
						id = Integer.parseInt(node.getFirstChild().getNodeValue());
					}
					if(nodeName.equals("coords"))
					{
						coords = node.getFirstChild().getNodeValue();
					}
				}
				toAdd = coords.split("\\|");
				//System.out.println(toAdd.length+": "+coords.length()+": "+coords);
				if(id!=-1 && toAdd != null)
				{
					Region region = DorfMap.regionsById.get(id);
					if(region!=null)
					{
						region.coords.clear();
						for(String s: toAdd)
						{
							int x = Integer.parseInt(s.split(",")[0]);
							int z = Integer.parseInt(s.split(",")[1]);
							region.coords.add(x + 2048*z + region.depth * 4194304);
						}
					}
					else
					{
						new Exception().printStackTrace();
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
				for(int j = 0; j < regionNode.getChildNodes().getLength(); j++)
				{
					Node node = regionNode.getChildNodes().item(j);
					String nodeName = node.getNodeName();
					if(nodeName.equals("id"))
					{
						id = Integer.parseInt(node.getFirstChild().getNodeValue());
					}
					if(nodeName.equals("coords"))
					{
						coords = node.getFirstChild().getNodeValue();
					}
				}
				toAdd = coords.split("\\|");
				//System.out.println(toAdd.length+": "+coords.length()+": "+coords);
				if(id!=-1 && toAdd != null)
				{
					Region region = DorfMap.ugRegionsById.get(id);
					if(region!=null)
					{
						region.coords.clear();
						for(String s: toAdd)
						{
							int x = Integer.parseInt(s.split(",")[0]);
							int z = Integer.parseInt(s.split(",")[1]);
							region.coords.add(x + 2048*z + region.depth * 4194304);
						}
					}
					else
					{
						new Exception().printStackTrace();
					}
				}
			}
		}
		catch (Exception e)
		{
			e.printStackTrace();
		}
	}
	
	private void loadBiomes(String file) {
		ArrayList<ArrayList<String>> rows = new ArrayList<ArrayList<String>>();
		BufferedReader br = null;
		String line = "";
		String cvsSplitBy = ",";

		try {
			InputStream res = new FileInputStream(file);
			br = new BufferedReader(new InputStreamReader(res));
			int n = 0;
			while ((line = br.readLine()) != null) {

				String[] row = line.split(cvsSplitBy);
				rows.add(new ArrayList<String>());
				for (int i = 0; i < row.length; i++) {
					rows.get(n).add(row[i]);
				}
				n++;
			}

		} catch (FileNotFoundException e) {

			FileWriter fwriter;
			PrintWriter out;
			try {
				fwriter = new FileWriter(file);
				out = new PrintWriter(fwriter);
				
				String defaultPath = "/assets/dorfgen/biomes.csv";
				ArrayList<String> lines = new ArrayList<String>();
				InputStream res;
				try {
					res = getClass().getResourceAsStream(defaultPath);

					br = new BufferedReader(new InputStreamReader(res));
					int n = 0;

					while ((line = br.readLine()) != null) {
						lines.add(line);
						String[] row = line.split(cvsSplitBy);
						rows.add(new ArrayList<String>());
						for (int i = 0; i < row.length; i++) {
							rows.get(n).add(row[i]);
						}
						n++;
					}
				} catch (Exception e1) {
					e1.printStackTrace();
				}
				for (String s : lines) {
					
					out.println(s);
				}

				out.close();
				fwriter.close();

			} catch (IOException e2) {
			}

		} catch (NullPointerException e) {
			e.printStackTrace();
		} catch (IOException e) {
			e.printStackTrace();
		} finally {
			if (br != null) {
				try {
					br.close();
				} catch (IOException e) {
					e.printStackTrace();
				}
			}
		}

		for (int i = 1; i < rows.size(); i++) {
			ArrayList<String> row = rows.get(i);
			int r;
			int g;
			int b;
			int biomeId = -1;
			String biomeName = "";
			r = Integer.parseInt(row.get(0));
			g = Integer.parseInt(row.get(1));
			b = Integer.parseInt(row.get(2));

			try {
				biomeId = Integer.parseInt(row.get(3));
			} catch (Exception e) {
			}
			if (row.size() > 4)
				biomeName = row.get(4);

			if (biomeId < 0) {
				for (BiomeGenBase biome : BiomeGenBase.getBiomeGenArray()) {
					if (biome != null
							&& biome.biomeName.replace(" ", "")
									.equalsIgnoreCase(
											biomeName.replace(" ", ""))) {
						biomeId = biome.biomeID;
						break;
					}
				}
			}
			if (biomeId < 0) {
				System.out.println("Error in row " + i + " " + row);
				continue;
			}
			Color c = new Color(r, g, b);
			BiomeList.biomes.put(c.getRGB(), new BiomeConversion(c, biomeId));
		}

	}

}
