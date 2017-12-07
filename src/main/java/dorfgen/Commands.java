package dorfgen;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;

import javax.annotation.Nullable;

import cubicchunks.server.CubeProviderServer;
import dorfgen.conversion.DorfMap;
import dorfgen.conversion.DorfMap.Region;
import dorfgen.conversion.DorfMap.Site;
import dorfgen.conversion.DorfMap.WorldConstruction;
import dorfgen.worldgen.common.BiomeProviderFinite;
import dorfgen.worldgen.cubic.CubeGeneratorFinite;
import net.minecraft.command.CommandBase;
import net.minecraft.command.CommandException;
import net.minecraft.command.ICommandSender;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.server.MinecraftServer;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.text.TextComponentString;

public class Commands extends CommandBase
{
    private List<String> aliases;

    public Commands()
    {
        this.aliases = new ArrayList<String>();
        this.aliases.add("dorfgen");
        this.aliases.add("dg");
    }

    @Override
    public String getName()
    {
        return "dorfgen";
    }

    @Override
    public String getUsage(ICommandSender sender)
    {
        // TODO Auto-generated method stub
        return "dorfgen <text>";
    }

    @Override
    public List<String> getAliases()
    {
        return aliases;
    }

    @Override
    public void execute(MinecraftServer server, ICommandSender sender, String[] args) throws CommandException
    {

        int scale = ((BiomeProviderFinite) sender.getEntityWorld().getBiomeProvider()).scale;
        // TODO command to say which building player is in in the site.
        if (args.length > 1 && args[0].equalsIgnoreCase("tp") && sender instanceof EntityPlayer)
        {
            String name = args[1];

            if (args[1].contains("\""))
            {
                for (int i = 2; i < args.length; i++)
                {
                    name += args[i];
                }
            }

            EntityPlayer entity = (EntityPlayer) sender;
            Site telesite = null;
            try
            {
                int id = Integer.parseInt(name);
                telesite = DorfMap.sitesById.get(id);
            }
            catch (NumberFormatException e)
            {
                ArrayList<Site> sites = new ArrayList<Site>(DorfMap.sitesById.values());
                for (Site s : sites)
                {
                    if (s.name.replace(" ", "").equalsIgnoreCase(name.replace("\"", "").replace(" ", "")))
                    {
                        telesite = s;
                        break;
                    }
                }
            }
            if (telesite != null)
            {
                int x = telesite.x * scale + scale;
                int z = telesite.z * scale + scale;
                int y = WorldGenerator.instance.dorfs.elevationMap[(x - WorldGenerator.shift.getX())
                        / scale][(z - WorldGenerator.shift.getZ()) / scale];
                entity.sendMessage(new TextComponentString("Teleported to " + telesite));
                entity.setPositionAndUpdate(x, y, z);
            }

            WorldConstruction teleConstruct = null;
            try
            {
                int id = Integer.parseInt(name);
                teleConstruct = DorfMap.constructionsById.get(id);
            }
            catch (NumberFormatException e)
            {
                ArrayList<WorldConstruction> sites = new ArrayList<WorldConstruction>(
                        DorfMap.constructionsById.values());
                for (WorldConstruction s : sites)
                {
                    if (s.name.replace(" ", "").equalsIgnoreCase(name.replace("\"", "").replace(" ", "")))
                    {
                        teleConstruct = s;
                        break;
                    }
                }
            }
            if (teleConstruct != null)
            {
                HashSet<Integer> coords = teleConstruct.worldCoords;
                int i = coords.iterator().next();
                int x = (i & (2047)) * scale * 16 + scale;
                int z = (i / (2048)) * scale * 16 + scale;
                int y = WorldGenerator.instance.dorfs.elevationMap[(x - WorldGenerator.shift.getX())
                        / scale][(z - WorldGenerator.shift.getZ()) / scale];
                entity.sendMessage(new TextComponentString("Teleported to " + teleConstruct));
                entity.setPositionAndUpdate(x, y, z);
            }
        }
        else if (args.length > 0 && args[0].equalsIgnoreCase("info") && sender instanceof EntityPlayer)
        {
            EntityPlayer entity = (EntityPlayer) sender;
            BlockPos pos = entity.getPosition();
            Region region = WorldGenerator.instance.dorfs.getRegionForCoords(pos.getX(), pos.getZ());
            HashSet<Site> sites = WorldGenerator.instance.dorfs.getSiteForCoords(pos.getX(), pos.getZ());
            HashSet<WorldConstruction> constructs = WorldGenerator.instance.dorfs.getConstructionsForCoords(pos.getX(),
                    pos.getZ());
            String message = "Region: " + region.toString();
            if (sites != null) for (Site site : sites)
            {
                message += ", Site: " + site;
            }
            if (constructs != null) for (WorldConstruction cons : constructs)
            {
                message += ", Construct: " + cons;
            }
            entity.sendMessage(new TextComponentString(message));
        }
        else if (args.length > 0 && args[0].equalsIgnoreCase("river"))
        {
            CubeGeneratorFinite gen = (CubeGeneratorFinite) ((CubeProviderServer) sender.getEntityWorld()
                    .getChunkProvider()).getCubeGenerator();
            int x1 = sender.getPosition().getX();
            int z1 = sender.getPosition().getZ();
            int h;
            int x = x1 / scale + x1 % scale;
            int z = z1 / scale + z1 % scale;
            if (x >= WorldGenerator.instance.dorfs.waterMap.length
                    || z >= WorldGenerator.instance.dorfs.waterMap[0].length)
            {
                h = 1;
            }
            else h = gen.roadMaker.bicubicInterpolator.interpolate(WorldGenerator.instance.dorfs.elevationMap, x1, z1,
                    scale);

            System.out.println(gen.riverMaker.isInRiver(x1, z1) + " " + h + " " + x1 + " " + z1 + " " + scale);
        }
        else if (args.length > 0 && args[0].equalsIgnoreCase("road"))
        {
            CubeGeneratorFinite gen = (CubeGeneratorFinite) ((CubeProviderServer) sender.getEntityWorld()
                    .getChunkProvider()).getCubeGenerator();
            int x1 = sender.getPosition().getX();
            int z1 = sender.getPosition().getZ();
            int h;
            int x = x1 / scale + x1 % scale;
            int z = z1 / scale + z1 % scale;
            if (x >= WorldGenerator.instance.dorfs.waterMap.length
                    || z >= WorldGenerator.instance.dorfs.waterMap[0].length)
            {
                h = 1;
            }
            else h = gen.roadMaker.bicubicInterpolator.interpolate(WorldGenerator.instance.dorfs.elevationMap, x1, z1,
                    scale);
            int dh = -20;
            HashSet<WorldConstruction> constructs = WorldGenerator.instance.dorfs.getConstructionsForCoords(x1, z1);
            if (constructs != null)
            {
                int x2 = x1 / (scale * 16);
                int z2 = z1 / (scale * 16);
                int key = x2 + 2048 * z2;
                Iterator<WorldConstruction> iter = constructs.iterator();
                while (iter.hasNext())
                {
                    WorldConstruction cons = iter.next();
                    System.out.println(cons);
                    if (cons.isInConstruct(x1, h, z1))
                    {
                        x2 = x1 / (scale);
                        z2 = z1 / (scale);
                        key = x2 + 8192 * z2;
                        dh = cons.embarkCoords.get(key);
                        if (dh != -1) dh = WorldGenerator.instance.dorfs.sigmoid.elevationSigmoid(dh);
                        else dh = h;
                        break;
                    }
                }
            }

            System.out.println(gen.roadMaker.hasRoad(x1, h, z1) + " " + h + " " + x1 + " " + z1 + " " + dh);
        }
    }

    @Override
    public List<String> getTabCompletions(MinecraftServer server, ICommandSender sender, String[] args,
            @Nullable BlockPos targetPos)
    {
        // TODO Auto-generated method stub

        if (args[0].equalsIgnoreCase("tp"))
        {
            Collection<Site> sites = DorfMap.sitesById.values();
            ArrayList<String> names = new ArrayList<String>();
            Collections.sort(names);
            for (Site site : sites)
            {
                if (site.name.split(" ").length > 1)
                {
                    names.add("\"" + site.name + "\"");
                }
                else
                {
                    names.add(site.name);
                }
            }
            List<String> ret = new ArrayList<String>();
            if (args.length == 2)
            {
                String text = args[1];
                for (String name : names)
                {
                    if (name.startsWith(text))
                    {
                        ret.add(name);
                    }
                }
            }
            return ret;
        }
        return super.getTabCompletions(server, sender, args, targetPos);
    }

    @Override
    public boolean isUsernameIndex(String[] p_82358_1_, int p_82358_2_)
    {
        // TODO Auto-generated method stub
        return false;
    }

    @Override
    public int getRequiredPermissionLevel()
    {
        return 4;
    }
}
