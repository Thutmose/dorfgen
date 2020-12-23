package dorfgen.world.gen;

import net.minecraft.nbt.CompoundNBT;
import net.minecraft.world.gen.GenerationSettings;

public class DorfSettings extends GenerationSettings
{
    private GeneratorInfo info = new GeneratorInfo();

    public DorfSettings()
    {
    }

    public DorfSettings(final CompoundNBT tag)
    {
        this.info = new GeneratorInfo(tag);
    }

    public void setInfo(final GeneratorInfo info)
    {
        this.info = info;
    }

    public GeneratorInfo getInfo()
    {
        return this.info;
    }
}
