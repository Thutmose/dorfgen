package dorfgen.worldgen.cubic;

import cubicchunks.util.IntRange;
import cubicchunks.world.ICubicWorld;
import cubicchunks.world.type.ICubicWorldType;
import cubicchunks.worldgen.generator.ICubeGenerator;
import dorfgen.WorldGenerator;
import dorfgen.worldgen.common.BiomeProviderFinite;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiCreateWorld;
import net.minecraft.world.DimensionType;
import net.minecraft.world.World;
import net.minecraft.world.WorldServer;
import net.minecraft.world.WorldType;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;

public class WorldTypeCubic extends WorldType implements ICubicWorldType
{
    public static DimensionType TYPE = DimensionType.register("DORFCUBIC", "", 0, WorldProviderCubic.class, true);

    public WorldTypeCubic(String name)
    {
        super(name);
    }

    @Override
    public net.minecraft.world.biome.BiomeProvider getBiomeProvider(World world)
    {
        return new BiomeProviderFinite(world);
    }

    /** Get the height to render the clouds for this world type
     * 
     * @return The height to render clouds at */
    @Override
    public float getCloudHeight()
    {
        return WorldGenerator.instance.dorfs.sigmoid.elevationSigmoid(128);
    }

    @Override
    public double getHorizon(World world)
    {
        return WorldGenerator.instance.dorfs.sigmoid.elevationSigmoid(63);
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

    @Override
    public boolean isCustomizable()
    {
        return true;
    }

    @SideOnly(Side.CLIENT)
    public void onCustomizeButton(Minecraft mc, GuiCreateWorld guiCreateWorld)
    {
        mc.displayGuiScreen(
                new dorfgen.client.GuiCustomizeWorld(guiCreateWorld, guiCreateWorld.chunkProviderSettingsJson, true));
    }
}
