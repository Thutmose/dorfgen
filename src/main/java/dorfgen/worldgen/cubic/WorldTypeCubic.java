package dorfgen.worldgen.cubic;

import cubicchunks.util.IntRange;
import cubicchunks.world.ICubicWorld;
import cubicchunks.world.type.ICubicWorldType;
import cubicchunks.worldgen.generator.ICubeGenerator;
import dorfgen.worldgen.vanilla.WorldTypeFinite;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiCreateWorld;
import net.minecraft.world.WorldServer;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;

public class WorldTypeCubic extends WorldTypeFinite implements ICubicWorldType
{
    public WorldTypeCubic(String name)
    {
        super(name);
    }

    @Override
    public ICubeGenerator createCubeGenerator(ICubicWorld world)
    {
        return new CubeGeneratorFinite(world);
    }

    @Override
    public IntRange calculateGenerationHeightRange(WorldServer world)
    {
        return new IntRange(-2048, 2048);
    }

    @SideOnly(Side.CLIENT)
    public void onCustomizeButton(Minecraft mc, GuiCreateWorld guiCreateWorld)
    {
        mc.displayGuiScreen(
                new dorfgen.client.GuiCustomizeWorld(guiCreateWorld, guiCreateWorld.chunkProviderSettingsJson, true));
    }
}
