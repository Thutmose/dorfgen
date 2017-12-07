package dorfgen.client;

import java.io.IOException;

import dorfgen.worldgen.cubic.GeneratorInfo;
import net.minecraft.client.gui.GuiButton;
import net.minecraft.client.gui.GuiCreateWorld;
import net.minecraft.client.gui.GuiScreen;
import net.minecraft.client.gui.GuiTextField;

public class GuiCustomizeWorld extends GuiScreen
{
    private final GuiCreateWorld createWorldGui;
    private GeneratorInfo        generatorInfo = GeneratorInfo.getDefault();
    private GuiTextField         region;
    private GuiTextField         scaleh;
    private GuiTextField         scalev;
    private boolean              hasSigmoid;

    public GuiCustomizeWorld(GuiCreateWorld createWorldGuiIn, String preset, boolean hasSigmoid)
    {
        this.createWorldGui = createWorldGuiIn;
        this.setPreset(preset);
        this.hasSigmoid = hasSigmoid;
    }

    @Override
    public void initGui()
    {
        super.initGui();
        int x = width / 2 - 50;
        int y = height / 2;
        region = new GuiTextField(0, fontRenderer, x, y - 50, 100, 10);
        scaleh = new GuiTextField(1, fontRenderer, x, y - 40, 100, 10);
        scalev = new GuiTextField(2, fontRenderer, x, y - 30, 100, 10);

        region.setText(generatorInfo.region);
        scaleh.setText(generatorInfo.scaleh + "");
        scalev.setVisible(hasSigmoid);
        scalev.setText(generatorInfo.scalev + "");

        com.google.common.base.Predicate<String> intValid = new com.google.common.base.Predicate<String>()
        {
            @Override
            public boolean apply(String input)
            {
                try
                {
                    Integer.parseInt(input);
                    return true;
                }
                catch (NumberFormatException e)
                {
                    return input.isEmpty();
                }
            }
        };
        scaleh.setValidator(intValid);
        scalev.setValidator(intValid);
        addButton(new GuiButton(0, x, y + 00, "Rivers: " + generatorInfo.rivers));
        addButton(new GuiButton(1, x, y + 25, "Sites: " + generatorInfo.sites));
        addButton(new GuiButton(2, x, y + 50, "Constructs: " + generatorInfo.constructs));
        addButton(new GuiButton(3, x, y + 75, "Done"));
    }

    /** Gets the superflat preset in the text format described on the Superflat
     * article on the Minecraft Wiki */
    public String getPreset()
    {
        return this.generatorInfo.toString();
    }

    /** Sets the superflat preset. Invalid or null values will result in the
     * default superflat preset being used. */
    public void setPreset(String preset)
    {
        this.generatorInfo = GeneratorInfo.fromJson(preset);
    }

    /** Called by the controls from the buttonList when activated. (Mouse
     * pressed for buttons) */
    protected void actionPerformed(GuiButton button) throws IOException
    {
        if (button.id == 0)
        {
            generatorInfo.rivers = !generatorInfo.rivers;
            button.displayString = "Rivers: " + generatorInfo.rivers;
        }
        else if (button.id == 1)
        {
            generatorInfo.sites = !generatorInfo.sites;
            button.displayString = "Sites: " + generatorInfo.sites;
        }
        else if (button.id == 2)
        {
            generatorInfo.constructs = !generatorInfo.constructs;
            button.displayString = "Constructs: " + generatorInfo.constructs;
        }
        else if (button.id == 3)
        {
            if (!scalev.getText().isEmpty())
            {
                generatorInfo.scalev = Integer.parseInt(scalev.getText());
            }
            if (!scaleh.getText().isEmpty())
            {
                generatorInfo.scaleh = Integer.parseInt(scaleh.getText());
            }
            if (!region.getText().isEmpty())
            {
                generatorInfo.region = region.getText();
            }
            this.createWorldGui.chunkProviderSettingsJson = this.getPreset();
            this.mc.displayGuiScreen(this.createWorldGui);
        }

        createWorldGui.chunkProviderSettingsJson = getPreset();
    }

    @Override
    public void drawScreen(int mouseX, int mouseY, float partialTicks)
    {
        this.drawDefaultBackground();
        super.drawScreen(mouseX, mouseY, partialTicks);
        region.drawTextBox();
        scaleh.drawTextBox();
        scalev.drawTextBox();
    }

    @Override
    protected void keyTyped(char typedChar, int keyCode) throws IOException
    {
        super.keyTyped(typedChar, keyCode);
        region.textboxKeyTyped(typedChar, keyCode);
        scaleh.textboxKeyTyped(typedChar, keyCode);
        scalev.textboxKeyTyped(typedChar, keyCode);
    }

    @Override
    protected void mouseClicked(int mouseX, int mouseY, int mouseButton) throws IOException
    {
        super.mouseClicked(mouseX, mouseY, mouseButton);
        region.mouseClicked(mouseX, mouseY, mouseButton);
        scaleh.mouseClicked(mouseX, mouseY, mouseButton);
        scalev.mouseClicked(mouseX, mouseY, mouseButton);
    }

}
