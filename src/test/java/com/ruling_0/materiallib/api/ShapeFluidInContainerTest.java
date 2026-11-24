package com.ruling_0.materiallib.api;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

class ShapeFluidInContainerTest {

    @Test
    void aNonFluidShapeIsRejectedAtConstruction() {
        assertThrows(IllegalArgumentException.class, () -> new TestContainer(new TestShape("testmod", "plate"), null));
    }

    @Test
    void emptyIconPathDerivesFromTheShapeUnlessOverridden() {
        Shape fluid = new ShapeFluid("testmod", "molten");

        assertEquals("testmod:materials/cell_empty", new TestContainer(fluid, null).emptyIconPath());
        assertEquals("gregtech:items/cell_base", new TestContainer(fluid, "gregtech:items/cell_base").emptyIconPath());
    }

    @Test
    void onlyAOneBucketContainerPlacesItsFluid() {
        Shape fluid = new ShapeFluid("testmod", "molten");
        TestContainer bucket = new TestContainer(fluid, null, 1000);
        TestContainer ingotCell = new TestContainer(fluid, null, 144);
        assertFalse(bucket.placesFluid());

        bucket.setProperty(StandardProperties.PLACES_FLUID, true);
        ingotCell.setProperty(StandardProperties.PLACES_FLUID, true);

        assertTrue(bucket.placesFluid());
        assertFalse(ingotCell.placesFluid());
    }

    private static final class TestContainer extends ShapeFluidInContainer {

        TestContainer(Shape fluidShape, String emptyIconOverride) {
            this(fluidShape, emptyIconOverride, 1000);
        }

        TestContainer(Shape fluidShape, String emptyIconOverride, int volume) {
            super("testmod", "cell", fluidShape, (EmptyContainer) null, volume, emptyIconOverride, "cell");
        }
    }
}
