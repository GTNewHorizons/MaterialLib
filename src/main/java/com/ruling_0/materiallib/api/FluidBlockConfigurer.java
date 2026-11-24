package com.ruling_0.materiallib.api;

/// Configures the [ShapeFluidBlock] registered for one placeable material in one fluid [Shape].
///
/// Set through [FluidShapeBuilder#configureBlock] and invoked once per created block, after the shape's [FluidFlow] is
/// applied. Only the shape owner's configurer runs. Typical uses are Forge's block setters, such as `setTickRate` and
/// `setMaxScaledLight`.
@FunctionalInterface
public interface FluidBlockConfigurer {

    void configure(Material material, ShapeFluidBlock block);
}
