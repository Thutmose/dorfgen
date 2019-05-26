package dorfgen.worldgen.cubic;

import dorfgen.worldgen.vanilla.WorldTypeFinite;
import io.github.opencubicchunks.cubicchunks.api.util.IntRange;
import io.github.opencubicchunks.cubicchunks.api.world.ICubicWorldType;
import io.github.opencubicchunks.cubicchunks.api.worldgen.ICubeGenerator;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiCreateWorld;
import net.minecraft.world.World;
import net.minecraft.world.WorldServer;
import net.minecraft.world.gen.IChunkGenerator;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;

public class WorldTypeCubic extends WorldTypeFinite implements ICubicWorldType
{
    public WorldTypeCubic(String name)
    {
        super(name);
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

    @Override
    public ICubeGenerator createCubeGenerator(World world)
    {
        if (world instanceof WorldServer)
        {
            WorldServer worlds = (WorldServer) world;
            IChunkGenerator gen = worlds.getChunkProvider().chunkGenerator;
            return new CubeGeneratorFinite(gen, world);
        }
        return null;
    }

    @Override
    public boolean hasCubicGeneratorForWorld(World world)
    {
        return world.getWorldType() instanceof WorldTypeFinite && world instanceof WorldServer;
    }
}
