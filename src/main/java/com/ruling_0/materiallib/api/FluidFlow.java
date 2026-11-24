package com.ruling_0.materiallib.api;

/// How a placed fluid spreads: which way it flows and how far from its source. Flow speed follows the fluid's
/// viscosity.
///
/// [Direction#DENSITY] keeps Forge's rule, where positive density flows down and negative density flows up. The
/// distance is Forge's quanta per block, from 1 to 16. A distance of 1 does not spread.
public record FluidFlow(Direction direction, int distance) {

    public static final FluidFlow DEFAULT = new FluidFlow(Direction.DENSITY, 8);

    public enum Direction {
        DENSITY,
        DOWN,
        UP
    }

    public FluidFlow {
        if (direction == null) throw new IllegalArgumentException("direction must not be null");
        if (distance < 1 || distance > 16) throw new IllegalArgumentException("distance must be 1 to 16: " + distance);
    }

    /// The block density for a fluid of density `fluidDensity`. [Direction#DOWN] and [Direction#UP] force the sign and
    /// keep the magnitude, with a minimum of 1.
    public int blockDensity(int fluidDensity) {
        return switch (direction) {
            case DENSITY -> fluidDensity;
            case DOWN -> Math.max(1, Math.abs(fluidDensity));
            case UP -> -Math.max(1, Math.abs(fluidDensity));
        };
    }
}
