package dorfgen;

import java.util.Random;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;
import net.minecraft.block.Block;
import net.minecraft.block.BlockFalling;
import net.minecraft.block.BlockGrass;
import net.minecraft.block.material.Material;
import net.minecraft.block.state.IBlockState;
import net.minecraft.creativetab.CreativeTabs;
import net.minecraft.init.Blocks;
import net.minecraft.item.Item;
import net.minecraft.util.BlockPos;
import net.minecraft.world.ColorizerGrass;
import net.minecraft.world.IBlockAccess;
import net.minecraft.world.World;

public class BlockRoadSurface extends BlockFalling
{
	public static BlockRoadSurface uggrass;


	protected BlockRoadSurface()
	{
		super(Material.sand);
		this.setTickRandomly(true);
		this.setCreativeTab(CreativeTabs.tabBlock);
		this.setHardness(0.6F).setStepSound(soundTypeGravel).setUnlocalizedName("roadgravel");
		this.setTickRandomly(true);
		uggrass = this;
	}

	@Override
	/** Ticks the block if it's been scheduled */
	public void updateTick(World world, BlockPos pos, IBlockState state, Random rand)
	{
		if (world.getBlockState(pos.up()).getBlock() == Blocks.snow_layer)
		{
			world.setBlockToAir(pos.up());
		}
		super.updateTick(world, pos, state, rand);
	}

    /**
     * Get the Item that this Block should drop when harvested.
     *  
     * @param fortune the level of the Fortune enchantment on the player's tool
     */
    public Item getItemDropped(IBlockState state, Random rand, int fortune)
    {
        return Item.getItemFromBlock(Blocks.gravel);
    }

	/** Returns a integer with hex for 0xrrggbb with this color multiplied
	 * against the blocks color. Note only called when first determining what to
	 * render. */
	@Override
	@SideOnly(Side.CLIENT)
	public int colorMultiplier(IBlockAccess p_149720_1_, BlockPos pos, int renderPass)
	{
		int l = 200;
		int i1 = 200;
		int j1 = 200;
		return (l / 1 & 255) << 16 | (i1 / 1 & 255) << 8 | j1 / 1 & 255;
	}
}
