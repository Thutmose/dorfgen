package dorfgen.world.gen;

import dorfgen.client.CustomizeWorld;
import dorfgen.util.ISigmoid;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screen.CreateWorldScreen;
import net.minecraft.world.World;
import net.minecraft.world.WorldType;
import net.minecraft.world.gen.ChunkGenerator;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;

public class DorfWorldType extends WorldType
{

    public DorfWorldType(final String name)
    {
        super(name);
    }

    @Override
    public ChunkGenerator<?> createChunkGenerator(final World world)
    {
        final DorfSettings settings = new DorfSettings(world.getWorldInfo().getGeneratorOptions());
        settings.getInfo().create(true);
        final DorfBiomeProvider provider = new DorfBiomeProvider(settings);
        return new DorfChunkGenerator(world, provider, settings);
    }

    @Override
    @OnlyIn(Dist.CLIENT)
    public void onCustomizeButton(final Minecraft mc, final CreateWorldScreen gui)
    {
        mc.displayGuiScreen(new CustomizeWorld(gui, true));
    }

    @Override
    public boolean hasCustomOptions()
    {
        return true;
    }

    @Override
    public float getCloudHeight()
    {
        // TODO figure out what map we actually are for this...
        return new ISigmoid()
        {
        }.elevationSigmoid(128);
    }
}
