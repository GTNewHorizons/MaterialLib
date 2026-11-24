package com.ruling_0.materiallib.api;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import org.junit.jupiter.api.Test;

class FluidFlowTest {

    @Test
    void densityFlowFollowsTheFluidAndFixedDirectionsKeepItsMagnitude() {
        assertEquals(-1000, FluidFlow.DEFAULT.blockDensity(-1000));
        assertEquals(1000, new FluidFlow(FluidFlow.Direction.DOWN, 8).blockDensity(-1000));
        assertEquals(-3000, new FluidFlow(FluidFlow.Direction.UP, 8).blockDensity(3000));
        assertEquals(1, new FluidFlow(FluidFlow.Direction.DOWN, 8).blockDensity(0));
        assertEquals(-1, new FluidFlow(FluidFlow.Direction.UP, 8).blockDensity(0));
    }

    @Test
    void distanceRunsFromOneToSixteen() {
        assertThrows(IllegalArgumentException.class, () -> new FluidFlow(FluidFlow.Direction.DOWN, 0));
        assertThrows(IllegalArgumentException.class, () -> new FluidFlow(FluidFlow.Direction.DOWN, 17));
    }
}
