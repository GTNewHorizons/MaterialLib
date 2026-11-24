package com.ruling_0.materiallib.api;

import net.minecraft.block.material.MapColor;
import net.minecraft.block.material.MaterialLiquid;
import net.minecraft.client.renderer.texture.IIconRegister;
import net.minecraft.util.IIcon;
import net.minecraft.world.IBlockAccess;

import net.minecraftforge.fluids.BlockFluidClassic;
import net.minecraftforge.fluids.Fluid;

import cpw.mods.fml.relauncher.Side;
import cpw.mods.fml.relauncher.SideOnly;

/// The world block of one placeable material fluid.
///
/// The block takes its Minecraft material from the [FluidBlockKind] the shape chose, and draws the fluid's icons and
/// tint. A [ShapeFluid] subclass supplies its own subclass of this block from [ShapeFluid#createBlock].
public class ShapeFluidBlock extends BlockFluidClassic {

    private static final MaterialLiquid GAS = new MaterialLiquid(MapColor.airColor);

    protected ShapeFluidBlock(ShapeFluid shape, Material material, Fluid fluid, FluidBlockKind kind) {
        super(fluid, minecraftMaterial(kind));
        setBlockName(fluid.getUnlocalizedName());
    }

    private static net.minecraft.block.material.Material minecraftMaterial(FluidBlockKind kind) {
        return switch (kind) {
            case WATER -> net.minecraft.block.material.Material.water;
            case LAVA -> net.minecraft.block.material.Material.lava;
            case GAS -> GAS;
        };
    }

    @Override
    @SideOnly(Side.CLIENT)
    public IIcon getIcon(int side, int meta) {
        return side <= 1 ? getFluid().getStillIcon() : getFluid().getFlowingIcon();
    }

    @Override
    @SideOnly(Side.CLIENT)
    public void registerBlockIcons(IIconRegister register) {}

    @Override
    @SideOnly(Side.CLIENT)
    public int colorMultiplier(IBlockAccess world, int x, int y, int z) {
        return getFluid().getColor();
    }

    @Override
    public String getLocalizedName() { return getFluid().getLocalizedName(); }
}
