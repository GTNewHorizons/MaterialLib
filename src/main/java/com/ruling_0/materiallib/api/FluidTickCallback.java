package com.ruling_0.materiallib.api;

import java.util.Random;

import net.minecraft.world.World;

/// A callback that runs when the placed block of a fluid [Shape] ticks, attached through [ShapeEdit#onFluidTick]. It
/// sees both random ticks and scheduled flow ticks.
@FunctionalInterface
public interface FluidTickCallback {

    /// Handles one tick of the fluid block of `material` at (`x`, `y`, `z`). Returns true when it acted, which skips
    /// later callbacks and the fluid's own handling of the tick.
    boolean onTick(Material material, World world, int x, int y, int z, Random random);
}
