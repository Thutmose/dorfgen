package dorfgen.worldgen.structures.scattered;

import java.util.List;
import java.util.Random;

import com.google.common.collect.Lists;

import dorfgen.conversion.DorfMap;
import net.minecraft.util.Rotation;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;
import net.minecraft.world.gen.ChunkGeneratorOverworld;
import net.minecraft.world.gen.structure.WoodlandMansion.Start;
import net.minecraft.world.gen.structure.WoodlandMansionPieces;

public class WoodlandMansionStart extends Start
{
    DorfMap map;

    public WoodlandMansionStart(World world, DorfMap map, Random rand, int x, int z)
    {
        super(world, null, rand, x, z);
        this.map = map;
    }

    @Override
    public void create(World world, ChunkGeneratorOverworld unused, Random rand, int x, int z)
    {
        int avg = 0;
        int num = 0;
        for (int dx = -32; dx <= 32; dx += 8)
            for (int dz = -32; dz <= 32; dz += 8)
            {
                int x0 = x * 16 + dx;
                int z0 = z * 16 + dz;
                if (!DorfMap.inBounds(map.shiftX(x0) / map.scale, map.shiftZ(z0) / map.scale, map.elevationMap))
                    continue;
                avg += map.heightInterpolator.interpolate(map.elevationMap, map.shiftX(x0), map.shiftZ(z0), map.scale);
                num++;
            }
        if (num != 0) avg /= num;
        BlockPos blockpos = new BlockPos(x * 16 + 8, avg + 1, z * 16 + 8);
        Rotation rotation = Rotation.values()[rand.nextInt(Rotation.values().length)];
        List<WoodlandMansionPieces.MansionTemplate> list = Lists.<WoodlandMansionPieces.MansionTemplate> newLinkedList();
        WoodlandMansionPieces.generateMansion(world.getSaveHandler().getStructureTemplateManager(), blockpos, rotation,
                list, rand);
        this.components.addAll(list);
        this.updateBoundingBox();
    }

    /** currently only defined for Villages, returns true if Village has more
     * than 2 non-road components */
    @Override
    public boolean isSizeableStructure()
    {
        return true;
    }

}
