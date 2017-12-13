package dorfgen.worldgen.structures.village;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

import dorfgen.conversion.DorfMap;
import net.minecraft.world.gen.structure.StructureComponent;
import net.minecraft.world.gen.structure.StructureVillagePieces.Village;

public class VillageWrappedList<T extends Object> extends ArrayList<StructureComponent>
        implements List<StructureComponent>
{
    private static final long serialVersionUID = 2219213015963071838L;
    private DorfMap           map;

    public VillageWrappedList(DorfMap map)
    {
        super();
        this.map = map;
    }

    @Override
    public boolean add(StructureComponent e)
    {
        if (!(e instanceof VillageWrapper))
        {
            e = new VillageWrapper((Village) e, map);
        }
        return super.add(e);
    }

    @Override
    public boolean addAll(Collection<? extends StructureComponent> c)
    {
        for (StructureComponent comp : c)
        {
            add(comp);
        }
        return true;
    }
}
