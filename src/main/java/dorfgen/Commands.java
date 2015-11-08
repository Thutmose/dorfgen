package dorfgen;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;

import dorfgen.conversion.DorfMap;
import dorfgen.conversion.DorfMap.Site;
import net.minecraft.command.ICommand;
import net.minecraft.command.ICommandSender;
import net.minecraft.entity.Entity;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.util.ChatComponentText;

public class Commands implements ICommand
{
	private List aliases;

	public Commands()
	{
		this.aliases = new ArrayList();
		this.aliases.add("dorfgen");
		this.aliases.add("dg");
	}

	@Override
	public int compareTo(Object arg0)
	{
		// TODO Auto-generated method stub
		return 0;
	}

	@Override
	public String getCommandName()
	{
		return "dorfgen";
	}

	@Override
	public String getCommandUsage(ICommandSender sender)
	{
		// TODO Auto-generated method stub
		return "dorfgen <text>";
	}

	@Override
	public List getCommandAliases()
	{
		return aliases;
	}

	@Override
	public void processCommand(ICommandSender sender, String[] args)
	{
		if(args.length > 1 && args[0].equalsIgnoreCase("tp") && sender instanceof EntityPlayer)
		{
			String name = args[1];
			
			if(args[1].contains("\""))
			{
				for(int i = 2; i<args.length; i++)
				{
					name += args[i];
				}
			}
			
			EntityPlayer entity = (EntityPlayer) sender;
			Site telesite = null;
			try
			{
				int id = Integer.parseInt(name);
				telesite = WorldGenerator.instance.dorfs.sitesById.get(id);
			}
			catch (NumberFormatException e)
			{
				ArrayList<Site> sites = new ArrayList(WorldGenerator.instance.dorfs.sitesById.values());
				for(Site s: sites)
				{
					if(s.name.replace(" ", "").equalsIgnoreCase(name.replace("\"", "").replace(" ", "")))
					{
						telesite = s;
						break;
					}
				}
			}
			if(telesite!=null)
			{
				int x = telesite.x * WorldGenerator.scale + WorldGenerator.scale;
				int z = telesite.z * WorldGenerator.scale + WorldGenerator.scale;
				
				int y = WorldGenerator.instance.dorfs.elevationMap[(x - WorldGenerator.instance.shift.posX) / WorldGenerator.scale]
						[(z - WorldGenerator.instance.shift.posZ) / WorldGenerator.scale];
				entity.addChatMessage(new ChatComponentText("Teleported to "+telesite));
				entity.setPositionAndUpdate(x, y, z);
			}
		}
	}

	@Override
	public boolean canCommandSenderUseCommand(ICommandSender sender)
	{
		// TODO Auto-generated method stub
		return true;
	}

	@Override
	public List addTabCompletionOptions(ICommandSender sender, String[] args)
	{
		// TODO Auto-generated method stub
		
		if(args[0].equalsIgnoreCase("tp"))
		{
			Collection<Site> sites = DorfMap.sitesById.values();
			ArrayList<String> names = new ArrayList();
			Collections.sort(names);
			for(Site site: sites)
			{
				if(site.name.split(" ").length>1)
				{
					names.add("\""+site.name+"\"");
				}
				else
				{
					names.add(site.name);
				}
			}
			List ret = new ArrayList();
			if(args.length == 2)
			{
				String text = args[1];
				for(String name: names)
				{
					if(name.contains(text))
					{
						ret.add(name);
					}
				}
			}
			return ret;
		}
		return null;
	}

	@Override
	public boolean isUsernameIndex(String[] p_82358_1_, int p_82358_2_)
	{
		// TODO Auto-generated method stub
		return false;
	}

}
