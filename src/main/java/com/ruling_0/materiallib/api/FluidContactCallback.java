package com.ruling_0.materiallib.api;

import net.minecraft.entity.Entity;
import net.minecraft.world.World;

/// A callback that runs while an entity touches the placed block of a fluid [Shape], attached through
/// [ShapeEdit#onFluidContact]. It runs in both client and server worlds.
@FunctionalInterface
public interface FluidContactCallback {

    /// Handles `entity` touching the fluid block of `material` at (`x`, `y`, `z`). Returns true when it acted, which
    /// skips later callbacks.
    boolean onContact(Material material, World world, int x, int y, int z, Entity entity);
}
