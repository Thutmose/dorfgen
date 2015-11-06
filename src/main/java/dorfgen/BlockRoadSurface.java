package dorfgen;

import java.util.Random;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import cpw.mods.fml.relauncher.Side;
import cpw.mods.fml.relauncher.SideOnly;
import net.minecraft.block.Block;
import net.minecraft.block.BlockFalling;
import net.minecraft.block.BlockGrass;
import net.minecraft.block.material.Material;
import net.minecraft.client.renderer.texture.IIconRegister;
import net.minecraft.creativetab.CreativeTabs;
import net.minecraft.init.Blocks;
import net.minecraft.item.Item;
import net.minecraft.util.IIcon;
import net.minecraft.world.ColorizerGrass;
import net.minecraft.world.IBlockAccess;
import net.minecraft.world.World;

public class BlockRoadSurface extends BlockFalling
{
	public static BlockRoadSurface uggrass;

	@SideOnly(Side.CLIENT)
	private IIcon topIcon;

	protected BlockRoadSurface()
	{
		super(Material.sand);
		this.setTickRandomly(true);
		this.setCreativeTab(CreativeTabs.tabBlock);
		this.setBlockTextureName("gravel");
		this.setHardness(0.6F).setStepSound(soundTypeGravel).setBlockName("gravel");
		this.setTickRandomly(true);
		uggrass = this;
	}

	/** Gets the block's texture. Args: side, meta */
	@SideOnly(Side.CLIENT)
	public IIcon getIcon(int p_149691_1_, int p_149691_2_)
	{
		return this.blockIcon;
	}

	/** Ticks the block if it's been scheduled */
	public void updateTick(World world, int x, int y, int z, Random p_149674_5_)
	{
		Block b;
		if ((b = world.getBlock(x, y + 1, z)) == Blocks.snow_layer)
		{
			world.setBlock(x, y + 1, z, Blocks.air);
		}
		super.updateTick(world, x, y, z, p_149674_5_);
	}

	public Item getItemDropped(int p_149650_1_, Random p_149650_2_, int p_149650_3_)
	{
		return Blocks.gravel.getItemDropped(0, p_149650_2_, p_149650_3_);
	}

	public boolean func_149851_a(World p_149851_1_, int p_149851_2_, int p_149851_3_, int p_149851_4_,
			boolean p_149851_5_)
	{
		return true;
	}

	public boolean func_149852_a(World p_149852_1_, Random p_149852_2_, int p_149852_3_, int p_149852_4_,
			int p_149852_5_)
	{
		return true;
	}

	@SideOnly(Side.CLIENT)
	public IIcon getIcon(IBlockAccess p_149673_1_, int p_149673_2_, int p_149673_3_, int p_149673_4_, int p_149673_5_)
	{

		return Blocks.gravel.getBlockTextureFromSide(p_149673_5_);

	}

	@SideOnly(Side.CLIENT)
	public void registerBlockIcons(IIconRegister p_149651_1_)
	{
		this.topIcon = blockIcon = p_149651_1_.registerIcon(this.getTextureName());
	}
	/** Returns a integer with hex for 0xrrggbb with this color multiplied
	 * against the blocks color. Note only called when first determining what to
	 * render. */
	@Override
	@SideOnly(Side.CLIENT)
	public int colorMultiplier(IBlockAccess p_149720_1_, int p_149720_2_, int p_149720_3_, int p_149720_4_)
	{
		int l = 200;
		int i1 = 200;
		int j1 = 200;
		
		return (l / 1 & 255) << 16 | (i1 / 1 & 255) << 8 | j1 / 1 & 255;
	}
}
