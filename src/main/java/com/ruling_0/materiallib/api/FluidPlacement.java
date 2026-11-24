package com.ruling_0.materiallib.api;

import java.util.concurrent.ConcurrentHashMap;

import com.ruling_0.materiallib.MaterialLib;

/// Helpers for placing material fluids as blocks and picking them back up.
///
/// Placeability is one boolean per material and fluid shape, merged like any other property.
final class FluidPlacement {

    private static final ConcurrentHashMap<String, Property<Boolean>> PLACEABLE = new ConcurrentHashMap<>();

    private FluidPlacement() {}

    static Property<Boolean> placeableProperty(String fluidShapeName) {
        return PLACEABLE.computeIfAbsent(
            fluidShapeName,
            name -> Property.of(MaterialLib.MODID, "placeable." + name, false));
    }

    static boolean isPlaceable(Material material, Shape fluid) {
        return material.getProperty(placeableProperty(fluid.getName()));
    }

    static void requireFluidShape(Shape fluid) {
        if (!(fluid instanceof ShapeFluid)) throw new IllegalArgumentException(fluid + " is not a fluid shape");
    }
}
