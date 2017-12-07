package dorfgen.worldgen.vanilla;

import dorfgen.worldgen.common.BiomeProviderFinite;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiCreateWorld;
import net.minecraft.world.World;
import net.minecraft.world.WorldType;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;

public class WorldTypeFinite extends WorldType
{

    public WorldTypeFinite(String name)
    {
        super(name);
    }

    @Override
    public net.minecraft.world.biome.BiomeProvider getBiomeProvider(World world)
    {
        // new Exception().printStackTrace();
        return new BiomeProviderFinite(world);
    }

    @Override
    public net.minecraft.world.gen.IChunkGenerator getChunkGenerator(World world, String generatorOptions)
    {
        // new Exception().printStackTrace();
        return new ChunkGeneratorFinite(world, world.getSeed(), world.getWorldInfo().isMapFeaturesEnabled(),
                generatorOptions);
    }

    /** Get the height to render the clouds for this world type
     * 
     * @return The height to render clouds at */
    public float getCloudHeight()
    {
        return 200.0F;
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
                new dorfgen.client.GuiCustomizeWorld(guiCreateWorld, guiCreateWorld.chunkProviderSettingsJson, false));
    }
}
