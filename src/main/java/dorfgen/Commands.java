package dorfgen;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;

import javax.annotation.Nullable;

import com.google.common.collect.Lists;

import dorfgen.conversion.DorfMap;
import dorfgen.conversion.DorfMap.Region;
import dorfgen.conversion.DorfMap.Site;
import dorfgen.conversion.DorfMap.WorldConstruction;
import dorfgen.worldgen.common.BiomeProviderFinite;
import dorfgen.worldgen.common.CachedInterpolator;
import dorfgen.worldgen.common.IDorfgenProvider;
import net.minecraft.command.CommandBase;
import net.minecraft.command.CommandException;
import net.minecraft.command.ICommandSender;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.server.MinecraftServer;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.text.ITextComponent;
import net.minecraft.util.text.TextComponentString;

public class Commands extends CommandBase
{
    private List<String> aliases;
    private DorfMap      dorfs;

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
        IDorfgenProvider gen = WorldGenerator.getProvider(sender.getEntityWorld());
        if (gen == null) { throw new CommandException("This command only works on Dorfgen Worlds!"); }
        int scale = ((BiomeProviderFinite) sender.getEntityWorld().getBiomeProvider()).scale;
        dorfs = ((BiomeProviderFinite) sender.getEntityWorld().getBiomeProvider()).map;
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
                telesite = dorfs.sitesById.get(id);
            }
            catch (NumberFormatException e)
            {
                ArrayList<Site> sites = new ArrayList<Site>(dorfs.sitesById.values());
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
                int[] mid = telesite.getSiteMid();
                int x = dorfs.shiftX(mid[0]);
                int z = dorfs.shiftZ(mid[1]);
                int y = new CachedInterpolator().interpolate(dorfs.elevationMap, x, z, scale);
                entity.sendMessage(new TextComponentString("Teleported to " + telesite));
                entity.setPositionAndUpdate(mid[0], y, mid[1]);
            }

            WorldConstruction teleConstruct = null;
            try
            {
                int id = Integer.parseInt(name);
                teleConstruct = dorfs.constructionsById.get(id);
            }
            catch (NumberFormatException e)
            {
                ArrayList<WorldConstruction> sites = new ArrayList<WorldConstruction>(dorfs.constructionsById.values());
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
                int y = dorfs.elevationMap[x / scale][z / scale];
                entity.sendMessage(new TextComponentString("Teleported to " + teleConstruct));
                entity.setPositionAndUpdate(dorfs.shiftX(x), y, dorfs.shiftZ(z));
            }
        }
        else if (args.length > 0 && args[0].equalsIgnoreCase("info") && sender instanceof EntityPlayer)
        {
            EntityPlayer entity = (EntityPlayer) sender;
            BlockPos pos = entity.getPosition();
            Region region = dorfs.getRegionForCoords(pos.getX(), pos.getZ());
            HashSet<Site> sites = dorfs.getSiteForCoords(pos.getX(), pos.getZ());
            HashSet<WorldConstruction> constructs = dorfs.getConstructionsForCoords(pos.getX(), pos.getZ());
            List<ITextComponent> messages = Lists.newArrayList();
            messages.add(new TextComponentString("Region: " + region));
            if (region != null)
            {
                int x = dorfs.shiftX(pos.getX());
                int z = dorfs.shiftZ(pos.getZ());

                CachedInterpolator interpolator = new CachedInterpolator();
                String mess = "";

                if (DorfMap.inBounds(x / scale, z / scale, dorfs.temperatureMap))
                {
                    int temp = interpolator.interpolate(dorfs.temperatureMap, x, z, scale);
                    mess = mess + "Temp: " + temp + " ";
                }
                if (DorfMap.inBounds(x / scale, z / scale, dorfs.elevationMap))
                {
                    int height = interpolator.interpolate(dorfs.elevationMap, x, z, scale);
                    mess = mess + "Height: " + height + " ";
                }
                if (DorfMap.inBounds(x / scale, z / scale, dorfs.rainMap))
                {
                    int rain = interpolator.interpolate(dorfs.rainMap, x, z, scale);
                    mess = mess + "Rain: " + rain + " ";
                }
                if (DorfMap.inBounds(x / scale, z / scale, dorfs.vegitationMap))
                {
                    int veg = interpolator.interpolate(dorfs.vegitationMap, x, z, scale);
                    mess = mess + "Veg:" + veg + " ";
                }
                messages.add(new TextComponentString(mess));
            }

            if (sites != null) for (Site site : sites)
            {
                messages.add(new TextComponentString("Site: " + site));
            }
            else
            {
                Site nearest = null;
                int kx = dorfs.shiftX(pos.getX()) / (scale);
                int kz = dorfs.shiftZ(pos.getZ()) / (scale);
                BlockPos k = new BlockPos(kx, 0, kz);
                double dist = Integer.MAX_VALUE;
                for (Site site : dorfs.sitesById.values())
                {
                    BlockPos s = new BlockPos(site.x, 0, site.z);
                    if (s.distanceSq(k) < dist)
                    {
                        dist = s.distanceSq(k);
                        nearest = site;
                    }
                }
                messages.add(new TextComponentString("Nearest Site: " + nearest));
            }
            if (constructs != null) for (WorldConstruction cons : constructs)
            {
                messages.add(new TextComponentString("Construct: " + cons));
            }
            for (ITextComponent message : messages)
                sender.sendMessage(message);
        }
        else if (args.length > 0 && args[0].equalsIgnoreCase("river"))
        {
            int x1 = sender.getPosition().getX();
            int z1 = sender.getPosition().getZ();
            int r = dorfs.riverMap[dorfs.shiftX(x1) / scale][dorfs.shiftZ(z1) / scale];
            sender.sendMessage(new TextComponentString(
                    (gen.getRiverMaker().isInRiver(dorfs.shiftX(x1), dorfs.shiftZ(z1), gen.getRiverMaker().getWidth())
                            + " " + r)));
        }
        else if (args.length > 0 && args[0].equalsIgnoreCase("road"))
        {
            int x1 = sender.getPosition().getX();
            int z1 = sender.getPosition().getZ();
            int h;
            if (x1 / scale >= dorfs.waterMap.length || z1 / scale >= dorfs.waterMap[0].length)
            {
                h = 1;
            }
            else h = gen.getRoadMaker().bicubicInterpolator.interpolate(dorfs.elevationMap, dorfs.shiftX(x1),
                    dorfs.shiftZ(z1), scale);
            int dh = -20;
            HashSet<WorldConstruction> constructs = dorfs.getConstructionsForCoords(x1, z1);
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
                        if (dh != -1) dh = dorfs.sigmoid.elevationSigmoid(dh);
                        else dh = h;
                        break;
                    }
                }
            }

            System.out.println(gen.getRoadMaker().hasRoad(x1, h, z1) + " " + h + " " + x1 + " " + z1 + " " + dh);
        }
    }

    @Override
    public List<String> getTabCompletions(MinecraftServer server, ICommandSender sender, String[] args,
            @Nullable BlockPos targetPos)
    {
        IDorfgenProvider gen = WorldGenerator.getProvider(sender.getEntityWorld());
        if (gen == null)
        {
            super.getTabCompletions(server, sender, args, targetPos);
        }
        dorfs = ((BiomeProviderFinite) sender.getEntityWorld().getBiomeProvider()).map;

        if (args[0].equalsIgnoreCase("tp"))
        {
            Collection<Site> sites = dorfs.sitesById.values();
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
