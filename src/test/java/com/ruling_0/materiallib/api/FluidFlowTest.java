package com.ruling_0.materiallib.api;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import net.minecraftforge.fluids.Fluid;

import org.junit.jupiter.api.Test;

class FluidFlowTest {

    @Test
    void densityFlowFollowsTheFluidAndFixedDirectionsKeepItsMagnitude() {
        assertEquals(-1000, FluidFlow.DEFAULT.blockDensity(-1000));
        assertEquals(1000, new FluidFlow(FluidFlow.Direction.DOWN, 8).blockDensity(-1000));
        assertEquals(-3000, new FluidFlow(FluidFlow.Direction.UP, 8).blockDensity(3000));
        assertEquals(1, new FluidFlow(FluidFlow.Direction.DOWN, 8).blockDensity(0));
        assertThrows(IllegalArgumentException.class, () -> new FluidFlow(FluidFlow.Direction.DOWN, 17));
    }

    @Test
    void gasesNeverBurnAndOnlyLavaHotLiquidsDo() {
        assertEquals(FluidBlockKind.GAS, FluidBlockKind.of(true, 5000));
        assertEquals(FluidBlockKind.LAVA, FluidBlockKind.of(false, 1300));
        assertEquals(FluidBlockKind.WATER, FluidBlockKind.of(false, 1299));
    }

    @Test
    void theDefaultChooserReadsTheFluidsGasAndTemperature() {
        Fluid lava = new Fluid("hotliquid").setTemperature(1300);
        assertEquals(FluidBlockKind.LAVA, FluidBlockKindChooser.DEFAULT.choose(null, lava));
        assertEquals(FluidBlockKind.GAS, FluidBlockKindChooser.DEFAULT.choose(null, lava.setGaseous(true)));
    }
}
