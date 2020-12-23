package dorfgen.client;

import java.util.List;
import java.util.function.Predicate;

import com.google.common.collect.Lists;

import dorfgen.world.gen.GeneratorInfo;
import net.minecraft.client.gui.screen.CreateWorldScreen;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.gui.widget.TextFieldWidget;
import net.minecraft.client.gui.widget.button.Button;
import net.minecraft.nbt.CompoundNBT;
import net.minecraft.util.text.TranslationTextComponent;

public class CustomizeWorld extends Screen
{
    private final CreateWorldScreen createWorldGui;
    private GeneratorInfo           generatorInfo = new GeneratorInfo();

    private TextFieldWidget region;
    private TextFieldWidget scaleh;
    private TextFieldWidget scalev;
    private TextFieldWidget dx;
    private TextFieldWidget dz;
    private TextFieldWidget sx;
    private TextFieldWidget sy;
    private TextFieldWidget sz;
    private TextFieldWidget spawn;

    boolean hasSigmoid;
    boolean autoOrigin = true;

    public CustomizeWorld(final CreateWorldScreen parent, final boolean hasSigmoid)
    {
        super(new TranslationTextComponent("createWorld.customize.dorfgen.title"));
        if (parent.chunkProviderSettingsJson.contains("scaleh")) this.generatorInfo = new GeneratorInfo(
                parent.chunkProviderSettingsJson);
        this.createWorldGui = parent;
        this.hasSigmoid = hasSigmoid;
    }

    @Override
    protected void init()
    {
        super.init();
        final int x = this.width / 2 - 50;
        int y = this.height / 2 - 50;
        final int dy = 20;
        final int tx = 22;
        final int tsy = 11;

        this.region = new TextFieldWidget(this.font, x + tx, y - dy + 0 * tsy, 100, 10, "");
        this.scaleh = new TextFieldWidget(this.font, x + tx, y - dy + 1 * tsy, 100, 10, "");
        this.scalev = new TextFieldWidget(this.font, x + tx, y - dy + 2 * tsy, 100, 10, "");

        this.dx = new TextFieldWidget(this.font, x + tx, y - dy + 3 * tsy, 100, 10, "");
        this.dz = new TextFieldWidget(this.font, x + tx, y - dy + 4 * tsy, 100, 10, "");

        this.sx = new TextFieldWidget(this.font, x + tx, y - dy + 5 * tsy, 100, 10, "");
        this.sy = new TextFieldWidget(this.font, x + tx, y - dy + 6 * tsy, 100, 10, "");
        this.sz = new TextFieldWidget(this.font, x + tx, y - dy + 7 * tsy, 100, 10, "");

        this.spawn = new TextFieldWidget(this.font, x + tx, y - dy + 8 * tsy, 100, 10, "");

        this.region.setText(this.generatorInfo.region);
        this.scaleh.setText(this.generatorInfo.scaleh + "");
        this.scalev.setText(this.hasSigmoid ? this.generatorInfo.scalev + ""
                : this.generatorInfo.scalev != 1 ? "" : 1 + "");

        this.dx.setText(this.generatorInfo.dx + "");
        this.dz.setText(this.generatorInfo.dz + "");
        this.sx.setText(this.generatorInfo.sx + "");
        this.sy.setText(this.generatorInfo.sy + "");
        this.sz.setText(this.generatorInfo.sz + "");
        this.spawn.setText(this.generatorInfo.spawn);

        this.addButton(this.region);
        this.addButton(this.scaleh);
        this.addButton(this.scalev);
        this.addButton(this.dx);
        this.addButton(this.dz);
        this.addButton(this.sx);
        this.addButton(this.sy);
        this.addButton(this.sz);
        this.addButton(this.spawn);

        final Predicate<String> intValid = input ->
        {
            try
            {
                Integer.parseInt(input);
                return true;
            }
            catch (final NumberFormatException e)
            {
                return input.isEmpty();
            }
        };
        final Predicate<String> sigMoidValid = input ->
        {
            try
            {
                final int num = Integer.parseInt(input);
                return CustomizeWorld.this.hasSigmoid ? true : num == 1;
            }
            catch (final NumberFormatException e)
            {
                return input.isEmpty();
            }
        };

        this.dx.setValidator(intValid);
        this.dz.setValidator(intValid);
        this.sx.setValidator(intValid);
        this.sy.setValidator(intValid);
        this.sz.setValidator(intValid);
        this.scaleh.setValidator(intValid);
        this.scalev.setValidator(sigMoidValid);

        y += 100;

        this.addButton(new Button(x - 100, y + 0 * dy, 120, dy, "Rivers: " + this.generatorInfo.rivers, b -> this
                .setRivers(b)));
        this.addButton(new Button(x - 100, y + 1 * dy, 120, dy, "Sites: " + this.generatorInfo.sites, b -> this
                .setSites(b)));
        this.addButton(new Button(x - 100, y + 2 * dy, 120, dy, "Constructs: " + this.generatorInfo.constructs,
                b -> this.setConstructs(b)));

        this.addButton(new Button(x + 20, y + 0 * dy, 120, dy, "Random Spawn: " + this.generatorInfo.random, b -> this
                .setSpawnRandom(b)));
        this.addButton(new Button(x + 20, y + 1 * dy, 120, dy, "Villages: " + this.generatorInfo.villages, b -> this
                .setVillages(b)));
        this.addButton(new Button(x + 20, y + 2 * dy, 120, dy, "Done", b -> this.done(b)));
    }

    void setRivers(final Button b)
    {
        this.generatorInfo.rivers = !this.generatorInfo.rivers;
        b.setMessage("Rivers: " + this.generatorInfo.rivers);
    }

    void setSites(final Button b)
    {
        this.generatorInfo.sites = !this.generatorInfo.sites;
        b.setMessage("Sites: " + this.generatorInfo.sites);
    }

    void setConstructs(final Button b)
    {

        this.generatorInfo.constructs = !this.generatorInfo.constructs;
        b.setMessage("Constructs: " + this.generatorInfo.constructs);
    }

    void setSpawnRandom(final Button b)
    {
        this.generatorInfo.random = !this.generatorInfo.random;
        b.setMessage("Random Spawn: " + this.generatorInfo.random);
    }

    void setVillages(final Button b)
    {
        this.generatorInfo.villages = !this.generatorInfo.villages;
        b.setMessage("Villages: " + this.generatorInfo.villages);
    }

    void done(final Button b)
    {
        if (!this.scalev.getText().isEmpty()) this.generatorInfo.scalev = Integer.parseInt(this.scalev.getText());
        else this.generatorInfo.scalev = new GeneratorInfo().scalev;
        if (!this.scaleh.getText().isEmpty()) this.generatorInfo.scaleh = Integer.parseInt(this.scaleh.getText());
        if (!this.dx.getText().isEmpty()) this.generatorInfo.dx = Integer.parseInt(this.dx.getText());
        if (!this.dz.getText().isEmpty()) this.generatorInfo.dz = Integer.parseInt(this.dz.getText());
        if (!this.sx.getText().isEmpty()) this.generatorInfo.sx = Integer.parseInt(this.sx.getText());
        if (!this.sy.getText().isEmpty()) this.generatorInfo.sy = Integer.parseInt(this.sy.getText());
        if (!this.sz.getText().isEmpty()) this.generatorInfo.sz = Integer.parseInt(this.sz.getText());
        if (!this.region.getText().isEmpty()) this.generatorInfo.region = this.region.getText();
        if (!this.spawn.getText().isEmpty()) this.generatorInfo.spawn = this.spawn.getText();
        this.createWorldGui.chunkProviderSettingsJson = this.getGeneratorOptions();
        this.minecraft.displayGuiScreen(this.createWorldGui);
    }

    /**
     * Gets the NBT data for the generator (which has the same use as the
     * preset)
     */
    public CompoundNBT getGeneratorOptions()
    {
        final CompoundNBT tag = this.generatorInfo.getTag();
        System.out.println(tag);
        return tag;
    }

    @Override
    public void render(final int x0, final int y0, final float partialTicks)
    {
        this.renderBackground();

        final int colour = 0xFFFFFFFF;
        final int x = this.width / 2 - 50;
        final int y = this.height / 2 - 50;
        final int dy = 20;
        final int tx = 20;
        final int tsy = 11;

        final List<String> words = Lists.newArrayList();
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
            final String word = words.get(i);
            this.drawString(this.font, word, x + tx - this.font.getStringWidth(word), y - dy + i * tsy, colour);
        }

        this.drawCenteredString(this.font, this.title.getFormattedText(), this.width / 2, 20, 16777215);
        super.render(x0, y0, partialTicks);
    }

    /**
     * Sets the generator config based on the given NBT.
     */
    public void setGeneratorOptions(final CompoundNBT nbt)
    {
        this.generatorInfo = new GeneratorInfo(nbt);
    }
}
