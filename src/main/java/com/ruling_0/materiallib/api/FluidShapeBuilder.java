package com.ruling_0.materiallib.api;

import java.util.Objects;

/// Builds and registers a simple fluid [Shape] backed by a [ShapeFluid]. Obtained from [MaterialLibAPI#newFluidShape]
/// and finished with [#build], which must be called inside the owning mod's [MaterialRegistrationEvent] handler.
/// Mods needing custom fluid behavior subclass [ShapeFluid] instead and register through
/// [MaterialLibAPI#registerFluidShape].
public final class FluidShapeBuilder extends ShapeBuilder<FluidShapeBuilder> {

    private FluidNamer namer;
    private FluidConfigurer configurer;
    private IconPather iconPather;

    FluidShapeBuilder(String modid, String name) {
        super(modid, name, "Fluid shape");
    }

    /// Sets the function computing each served material's Forge fluid name, in place of the default
    /// `<shapeName>.<materialName>` lowercased; see [FluidNamer].
    public FluidShapeBuilder fluidName(FluidNamer namer) {
        this.namer = Objects.requireNonNull(namer, "namer must not be null");
        return this;
    }

    /// Sets the callback configuring each newly registered material fluid (temperature, gaseous, luminosity,
    /// density, viscosity, ...); see [FluidConfigurer].
    public FluidShapeBuilder configureFluid(FluidConfigurer configurer) {
        this.configurer = Objects.requireNonNull(configurer, "configurer must not be null");
        return this;
    }

    /// Sets the per-material icon path override, tried ahead of the material's texture set; see [IconPather].
    public FluidShapeBuilder iconPath(IconPather pather) {
        this.iconPather = Objects.requireNonNull(pather, "pather must not be null");
        return this;
    }

    /// As [#iconPath(IconPather)], for a single icon path shared by every served material.
    public FluidShapeBuilder iconPath(String path) {
        Objects.requireNonNull(path, "path must not be null");
        return iconPath((shape, material) -> path);
    }

    @Override
    ServedShape create() {
        return new ShapeFluid(modid, name, namer, configurer, iconPather);
    }
}
