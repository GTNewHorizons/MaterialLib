package com.ruling_0.materiallib.api;

import net.minecraftforge.fluids.Fluid;

/// Chooses the [FluidBlockKind] of the block placed for one material's fluid.
///
/// Set through [FluidShapeBuilder#blockKind] or the [ShapeFluid] constructor, and invoked once per placeable material,
/// before its block exists. Only the shape owner's chooser runs.
@FunctionalInterface
public interface FluidBlockKindChooser {

    /// The rule used when no chooser is set: gaseous fluids are [FluidBlockKind#GAS], fluids at least as hot as lava
    /// are [FluidBlockKind#LAVA], and the rest are [FluidBlockKind#WATER].
    FluidBlockKindChooser DEFAULT = (material, fluid) -> FluidBlockKind.of(fluid.isGaseous(), fluid.getTemperature());

    FluidBlockKind choose(Material material, Fluid fluid);
}
