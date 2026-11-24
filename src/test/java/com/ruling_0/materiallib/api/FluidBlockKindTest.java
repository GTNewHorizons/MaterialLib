package com.ruling_0.materiallib.api;

import static org.junit.jupiter.api.Assertions.assertEquals;

import org.junit.jupiter.api.Test;

class FluidBlockKindTest {

    @Test
    void gasesNeverBurnAndOnlyLavaHotLiquidsDo() {
        assertEquals(FluidBlockKind.GAS, FluidBlockKind.of(true, 5000));
        assertEquals(FluidBlockKind.LAVA, FluidBlockKind.of(false, 1300));
        assertEquals(FluidBlockKind.WATER, FluidBlockKind.of(false, 1299));
    }
}
