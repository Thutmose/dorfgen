package dorfgen.world.gen;

import dorfgen.Dorfgen;
import dorfgen.conversion.DorfMap;
import dorfgen.util.ISigmoid;
import net.minecraft.nbt.CompoundNBT;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.IWorld;

public class GeneratorInfo
{
    public static GeneratorInfo getDefault()
    {
        final GeneratorInfo info = new GeneratorInfo();
        return info;
    }

    public String region = Dorfgen.instance.getDorfs((IWorld) null).name;
    // Mapping of embark tiles -> blocks
    public int scaleh = 51;
    // Mapping of vertical scale -> blocks
    public int scalev = 8;
    // Shift of the world map, in blocks.
    public int dx = 0;
    public int dz = 0;
    // Spawn site of the world, in Block coords
    public int sx = 0;
    public int sy = 0;
    public int sz = 0;
    // Site to spawn at, if random spawn is false.
    public String spawn = "";
    // Should a random site be picked for spawn.
    public boolean random = true;
    // Are rivers made
    public boolean rivers = true;
    // Are sites made
    public boolean sites = true;
    // Are constructions made
    public boolean constructs = true;
    // Do vanilla villages spawn.
    public boolean villages = false;

    private DorfMap map = null;

    public GeneratorInfo()
    {
    }

    public GeneratorInfo(final CompoundNBT tag)
    {
        this.region = tag.getString("region");

        this.scaleh = tag.getInt("scaleh");
        this.scalev = tag.getInt("scalev");

        this.dx = tag.getInt("dx");
        this.dz = tag.getInt("dz");

        this.sx = tag.getInt("sx");
        this.sy = tag.getInt("sy");
        this.sz = tag.getInt("sz");

        this.spawn = tag.getString("spawn");

        this.random = tag.getBoolean("random");
        this.rivers = tag.getBoolean("rivers");
        this.sites = tag.getBoolean("sites");
        this.constructs = tag.getBoolean("constructs");
        this.villages = tag.getBoolean("villages");
    }

    public CompoundNBT getTag()
    {
        final CompoundNBT tag = new CompoundNBT();
        tag.putString("region", this.region);

        tag.putInt("scaleh", this.scaleh);
        tag.putInt("scalev", this.scalev);

        tag.putInt("dx", this.dx);
        tag.putInt("dz", this.dz);

        tag.putInt("sx", this.sx);
        tag.putInt("sy", this.sy);
        tag.putInt("sz", this.sz);

        tag.putString("spawn", this.spawn);

        tag.putBoolean("random", this.random);
        tag.putBoolean("rivers", this.rivers);
        tag.putBoolean("sites", this.sites);
        tag.putBoolean("constructs", this.constructs);
        tag.putBoolean("villages", this.villages);
        return tag;
    }

    public DorfMap create(final boolean vanilla)
    {
        if (this.map == null) this.map = Dorfgen.instance.getDorfs(this.region);

        if (vanilla)
        {
            if (this.scalev == 1) this.map.setElevationSigmoid(new ISigmoid()
            {
                @Override
                public int elevationSigmoid(final int preHeight)
                {
                    return preHeight;
                }
            });
            else this.map.setElevationSigmoid(new ISigmoid()
            {
            });
        }
        else this.map.setElevationSigmoid(new ISigmoid()
        {
            @Override
            public int elevationSigmoid(final int preHeight)
            {
                return preHeight * GeneratorInfo.this.scalev;
            }
        });
        Dorfgen.LOGGER.info("Creating with scale: {}, from old {}", this.scaleh, this.map.getScale());
        if (this.scaleh == 0) Dorfgen.LOGGER.info("Error setting map scale! {}, {}", this, this.hashCode());
        else this.map.setScale(this.scaleh);
        this.map.spawn = new BlockPos(this.sx, this.sy, this.sz);
        this.map.shift = new BlockPos(this.dx, 0, this.dz);
        this.map.randomSpawn = this.random;
        this.map.spawnSite = this.spawn;
        return this.map;
    }

    @Override
    public String toString()
    {
        return this.getTag().toString();
    }
}