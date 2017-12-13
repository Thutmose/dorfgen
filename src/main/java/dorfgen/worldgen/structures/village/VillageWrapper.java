package dorfgen.worldgen.structures.village;

import java.lang.reflect.Method;
import java.util.Map;
import java.util.Random;
import java.util.logging.Level;

import com.google.common.collect.Maps;

import dorfgen.WorldGenerator;
import dorfgen.conversion.DorfMap;
import dorfgen.worldgen.structures.scattered.StructureLair;
import dorfgen.worldgen.structures.scattered.WoodlandMansionStart;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.world.World;
import net.minecraft.world.gen.structure.MapGenStructureIO;
import net.minecraft.world.gen.structure.StructureBoundingBox;
import net.minecraft.world.gen.structure.StructureComponent;
import net.minecraft.world.gen.structure.StructureVillagePieces.Village;
import net.minecraft.world.gen.structure.StructureVillagePieces.Well;
import net.minecraftforge.fml.relauncher.ReflectionHelper;

public class VillageWrapper extends Village
{
    private static final Map<String, Class<?>> classMap   = Maps.newHashMap();
    private static final Method                WRITETONBT = ReflectionHelper.findMethod(Village.class,
            "writeStructureToNBT", "func_143012_a", NBTTagCompound.class);

    public static void postInitVillageWrapper()
    {
        Map<String, Class<? extends StructureComponent>> componentNameToClassMap = MapGenStructureIO.componentNameToClassMap;
        MapGenStructureIO.registerStructure(WoodlandMansionStart.class, "dorfgen:WLM");
        MapGenStructureIO.registerStructureComponent(StructureLair.class, "dorfgen:lair");

        for (String s : componentNameToClassMap.keySet())
        {
            Class<? extends StructureComponent> old = componentNameToClassMap.get(s);
            if (old.getName().contains("net.minecraft.world.gen.structure.StructureVillagePieces$"))
            {
                classMap.put(s, old);
            }
        }
        for (String s : classMap.keySet())
        {
            MapGenStructureIO.registerStructureComponent(VillageWrapper.class, s);
        }
        WorldGenerator
                .log(MapGenStructureIO.getStructureComponentName(new VillageWrapper()) + " " + componentNameToClassMap);
    }

    Village wrapped;
    DorfMap map;

    public VillageWrapper()
    {
    }

    public VillageWrapper(Village wrapped, DorfMap map)
    {
        this.map = map;
        setWrapped(wrapped);
        this.structureType = wrapped.structureType;
        this.villagersSpawned = wrapped.villagersSpawned;
        this.componentType = wrapped.getComponentType();
        this.isZombieInfested = wrapped.isZombieInfested;
        // this.startPiece = wrapped.startPiece;
        this.boundingBox = wrapped.getBoundingBox();
        this.setCoordBaseMode(wrapped.getCoordBaseMode());

    }

    private void setWrapped(Village wrapped)
    {

        this.wrapped = wrapped;
    }

    @Override
    public boolean addComponentParts(World worldIn, Random randomIn, StructureBoundingBox structureBoundingBoxIn)
    {
        if (map == null) return wrapped.addComponentParts(worldIn, randomIn, structureBoundingBoxIn);
        int vanilla = super.getAverageGroundLevel(worldIn, structureBoundingBoxIn);
        int ours = getAverageGroundLevel(worldIn, wrapped.getBoundingBox());
        int diff = ours - vanilla - 1;
        boolean shift = !(wrapped instanceof Well);
        int sea = worldIn.getSeaLevel();
        worldIn.setSeaLevel(ours - 1);
        if (!shift)
        {
            diff = -1;
        }
        if (diff != 0)
        {
            wrapped.getBoundingBox().maxY -= diff;
        }
        if (wrapped != null)
        {
            boolean made = wrapped.addComponentParts(worldIn, randomIn, structureBoundingBoxIn);
            if (wrapped.averageGroundLvl != -1) averageGroundLvl = wrapped.averageGroundLvl;
            if (diff != 0) wrapped.getBoundingBox().maxY += diff;
            worldIn.setSeaLevel(sea);
            return made;
        }
        return false;
    }

    @Override
    public int getAverageGroundLevel(World worldIn, StructureBoundingBox structurebb)
    {
        if (map == null) return wrapped.getAverageGroundLevel(worldIn, structurebb);
        int x, z;
        int avg = 0;
        int num = 0;
        StructureBoundingBox box = structurebb;
        for (x = box.minX; x <= box.maxX; x++)
            for (z = box.minZ; z <= box.maxZ; z++)
            {
                if (!DorfMap.inBounds(map.shiftX(x) / map.scale, map.shiftZ(z) / map.scale, map.elevationMap)) continue;
                avg += map.heightInterpolator.interpolate(map.elevationMap, map.shiftX(x), map.shiftZ(z), map.scale);
                num++;
            }
        if (num == 0) { return wrapped.getAverageGroundLevel(worldIn, structurebb); }
        return (avg / num);
    }

    @SuppressWarnings("unchecked")
    @Override
    public void readStructureBaseNBT(World worldIn, NBTTagCompound tagCompound)
    {
        if (wrapped == null)
        {
            try
            {
                if (tagCompound.hasKey("id"))
                {
                    Class<? extends StructureComponent> oclass = (Class<? extends StructureComponent>) classMap
                            .get(tagCompound.getString("id"));

                    if (oclass != null)
                    {
                        setWrapped((Village) oclass.newInstance());
                    }
                }
                else
                {
                    Class<? extends Village> oclass;
                    oclass = (Class<? extends Village>) Class.forName(tagCompound.getString("wrapId"));
                    setWrapped((Village) oclass.newInstance());
                }
            }
            catch (Exception e)
            {
                e.printStackTrace();
            }
        }
        map = WorldGenerator.getDorfMap(tagCompound.getString("dorfMap"));
        wrapped.readStructureBaseNBT(worldIn, tagCompound);
        this.averageGroundLvl = wrapped.averageGroundLvl;
    }

    @Override
    protected void writeStructureToNBT(NBTTagCompound tagCompound)
    {
        if (map != null) tagCompound.setString("dorfMap", map.name);
        try
        {
            WRITETONBT.invoke(wrapped, tagCompound);
        }
        catch (Exception e)
        {
            WorldGenerator.log(Level.SEVERE, "Error saving wrapped piece.", e);
        }
        if (!tagCompound.hasKey("id"))
        {
            tagCompound.setString("wrapId", wrapped.getClass().getName());
        }
    }

}
