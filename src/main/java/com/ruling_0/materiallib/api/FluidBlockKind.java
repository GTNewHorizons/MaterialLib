package com.ruling_0.materiallib.api;

/// What a placed fluid does to entities.
public enum FluidBlockKind {

    /// Entities swim, are pushed by the current and drown, and burning entities are put out.
    WATER,
    /// Entities swim slowly and catch fire.
    LAVA,
    /// Entities neither swim nor burn.
    GAS;

    static final int LAVA_TEMPERATURE = 1300;

    static FluidBlockKind of(boolean gaseous, int temperature) {
        if (gaseous) return GAS;
        return temperature >= LAVA_TEMPERATURE ? LAVA : WATER;
    }
}
