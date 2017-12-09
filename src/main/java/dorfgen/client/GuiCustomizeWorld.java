package dorfgen.client;

import java.io.IOException;
import java.util.List;

import com.google.common.collect.Lists;

import dorfgen.worldgen.common.GeneratorInfo;
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
    private GuiTextField         dx;
    private GuiTextField         dz;
    private GuiTextField         sx;
    private GuiTextField         sy;
    private GuiTextField         sz;
    private GuiTextField         spawn;
    private List<GuiTextField>   text          = Lists.newArrayList();
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
        int dy = 100;
        int tx = +22;
        int tsy = 11;
        region = new GuiTextField(0, fontRenderer, x + tx, y - dy + 0 * tsy, 100, 10);
        scaleh = new GuiTextField(1, fontRenderer, x + tx, y - dy + 1 * tsy, 100, 10);
        scalev = new GuiTextField(2, fontRenderer, x + tx, y - dy + 2 * tsy, 100, 10);

        dx = new GuiTextField(3, fontRenderer, x + tx, y - dy + 3 * tsy, 100, 10);
        dz = new GuiTextField(4, fontRenderer, x + tx, y - dy + 4 * tsy, 100, 10);

        sx = new GuiTextField(5, fontRenderer, x + tx, y - dy + 5 * tsy, 100, 10);
        sy = new GuiTextField(6, fontRenderer, x + tx, y - dy + 6 * tsy, 100, 10);
        sz = new GuiTextField(7, fontRenderer, x + tx, y - dy + 7 * tsy, 100, 10);

        spawn = new GuiTextField(8, fontRenderer, x + tx, y - dy + 8 * tsy, 100, 10);

        region.setText(generatorInfo.region);
        scaleh.setText(generatorInfo.scaleh + "");
        scalev.setText(hasSigmoid ? generatorInfo.scalev + "" : generatorInfo.scalev != 1 ? "" : 1 + "");
        dx.setText(generatorInfo.dx + "");
        dz.setText(generatorInfo.dz + "");
        sx.setText(generatorInfo.sx + "");
        sy.setText(generatorInfo.sy + "");
        sz.setText(generatorInfo.sz + "");
        spawn.setText(generatorInfo.spawn);

        text.add(region);
        text.add(scaleh);
        text.add(scalev);
        text.add(dx);
        text.add(dz);
        text.add(sx);
        text.add(sy);
        text.add(sz);
        text.add(spawn);

        final com.google.common.base.Predicate<String> intValid = new com.google.common.base.Predicate<String>()
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
        com.google.common.base.Predicate<String> sigMoidValid = new com.google.common.base.Predicate<String>()
        {
            @Override
            public boolean apply(String input)
            {
                try
                {
                    int num = Integer.parseInt(input);
                    return hasSigmoid ? true : num == 1;
                }
                catch (NumberFormatException e)
                {
                    return input.isEmpty();
                }
            }
        };

        dx.setValidator(intValid);
        dz.setValidator(intValid);
        sx.setValidator(intValid);
        sy.setValidator(intValid);
        sz.setValidator(intValid);
        scaleh.setValidator(intValid);
        scalev.setValidator(sigMoidValid);
        dy = 15;

        addButton(new GuiButton(0, x - 100, y + 0 * dy, 120, 15, "Rivers: " + generatorInfo.rivers));
        addButton(new GuiButton(1, x - 100, y + 1 * dy, 120, 15, "Sites: " + generatorInfo.sites));
        addButton(new GuiButton(2, x - 100, y + 2 * dy, 120, 15, "Constructs: " + generatorInfo.constructs));

        addButton(new GuiButton(3, x + 20, y + 0 * dy, 120, 15, "Random Spawn: " + generatorInfo.random));
        addButton(new GuiButton(4, x + 20, y + 1 * dy, 120, 15, "Villages: " + generatorInfo.villages));
        addButton(new GuiButton(5, x + 20, y + 2 * dy, 120, 15, "Done"));
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
            generatorInfo.random = !generatorInfo.random;
            button.displayString = "Random Spawn: " + generatorInfo.random;
        }
        else if (button.id == 4)
        {
            generatorInfo.villages = !generatorInfo.villages;
            button.displayString = "Villages: " + generatorInfo.villages;
        }
        else if (button.id == 5)
        {
            if (!scalev.getText().isEmpty())
            {
                generatorInfo.scalev = Integer.parseInt(scalev.getText());
            }
            if (!scaleh.getText().isEmpty())
            {
                generatorInfo.scaleh = Integer.parseInt(scaleh.getText());
            }
            if (!dx.getText().isEmpty())
            {
                generatorInfo.dx = Integer.parseInt(dx.getText());
            }
            if (!dz.getText().isEmpty())
            {
                generatorInfo.dz = Integer.parseInt(dz.getText());
            }
            if (!sx.getText().isEmpty())
            {
                generatorInfo.sx = Integer.parseInt(sx.getText());
            }
            if (!sy.getText().isEmpty())
            {
                generatorInfo.sy = Integer.parseInt(sy.getText());
            }
            if (!sz.getText().isEmpty())
            {
                generatorInfo.sz = Integer.parseInt(sz.getText());
            }
            if (!region.getText().isEmpty())
            {
                generatorInfo.region = region.getText();
            }
            if (!spawn.getText().isEmpty())
            {
                generatorInfo.spawn = spawn.getText();
            }
            this.createWorldGui.chunkProviderSettingsJson = this.getPreset();
            System.out.println(this.getPreset());
            this.mc.displayGuiScreen(this.createWorldGui);
        }

        createWorldGui.chunkProviderSettingsJson = getPreset();
    }

    @Override
    public void drawScreen(int mouseX, int mouseY, float partialTicks)
    {
        this.drawDefaultBackground();
        super.drawScreen(mouseX, mouseY, partialTicks);
        int colour = 0xFFFFFFFF;
        int x = width / 2 - 50;
        int y = height / 2;
        int dy = 100;
        int tx = 20;
        int tsy = 11;

        List<String> words = Lists.newArrayList();
        words.add("Region Name:");
        words.add("Horizontal Scale:");
        words.add("Vertical Scale:");
        words.add("Origin X:");
        words.add("Origin Z:");
        words.add("Spawn X:");
        words.add("Spawn Y:");
        words.add("Spawn Z:");
        words.add("Spawn Site:");

        for (int i = 0; i < words.size(); i++)
        {
            String word = words.get(i);
            drawString(fontRenderer, word, x + tx - fontRenderer.getStringWidth(word), y - dy + i * tsy, colour);
        }

        for (GuiTextField box : text)
            box.drawTextBox();
    }

    @Override
    protected void keyTyped(char typedChar, int keyCode) throws IOException
    {
        super.keyTyped(typedChar, keyCode);
        for (GuiTextField box : text)
            box.textboxKeyTyped(typedChar, keyCode);
    }

    @Override
    protected void mouseClicked(int mouseX, int mouseY, int mouseButton) throws IOException
    {
        super.mouseClicked(mouseX, mouseY, mouseButton);
        for (GuiTextField box : text)
            box.mouseClicked(mouseX, mouseY, mouseButton);
    }

}
