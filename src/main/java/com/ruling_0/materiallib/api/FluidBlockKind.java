package com.ruling_0.materiallib.api;

/// How a placed fluid behaves, through the vanilla block material each kind picks.
///
/// Vanilla keys world behavior on that material as well as entity behavior. A [#WATER] kind block hydrates farmland and
/// reacts with lava as water does, and vanilla treats a [#GAS] kind block as neither water nor lava.
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
