package com.ruling_0.materiallib.api;

import net.minecraft.block.Block;
import net.minecraft.world.World;

/// A callback that runs when a block next to the placed block of a fluid [Shape] changes, attached through
/// [ShapeEdit#onFluidNeighborChange].
@FunctionalInterface
public interface FluidNeighborCallback {

    /// Handles `neighbor` changing beside the fluid block of `material` at (`x`, `y`, `z`). Returns true when it
    /// acted, which skips later callbacks and the fluid's own handling of the change.
    boolean onNeighborChange(Material material, World world, int x, int y, int z, Block neighbor);
}
