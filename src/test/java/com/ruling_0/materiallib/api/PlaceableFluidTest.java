package com.ruling_0.materiallib.api;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

class PlaceableFluidTest {

    private final MaterialRegistry registry = new MaterialRegistry();
    private final TextureSet texture = TextureSet.of("amod", "shiny");

    @Test
    void placeabilityMergesPerFluidShapeWithTheOwnerWinningConflicts() {
        ShapeFluid molten = new ShapeFluid("amod", "molten");
        ShapeFluid liquid = new ShapeFluid("amod", "liquid");
        registry.newMaterial("amod", "TestIron", texture).setPlaceable(molten, false).build();
        registry.newMaterial("bmod", "TestIron", texture).setPlaceable(molten, true).setPlaceable(liquid, true).build();
        registry.resolve();

        Material iron = registry.getMaterial("amod", "TestIron");
        assertFalse(FluidPlacement.isPlaceable(iron, molten));
        assertTrue(FluidPlacement.isPlaceable(iron, liquid));
        assertThrows(
            IllegalArgumentException.class,
            () -> registry.newMaterial("amod", "TestGold", texture).setPlaceable(new TestShape("amod", "plate"), true));
    }

    @Test
    void aFamilyMarksItsMembersPlaceable() {
        ShapeFluid molten = new ShapeFluid("amod", "molten");
        Family metals = registry.newFamily("amod", "Metals").setPlaceable(molten, true).build();
        registry.newMaterial("amod", "TestIron", texture).addToFamily(metals).build();
        registry.resolve();

        assertTrue(FluidPlacement.isPlaceable(registry.getMaterial("amod", "TestIron"), molten));
    }

    @Test
    void anEditOverridesTheDeclaredPlaceability() {
        ShapeFluid molten = new ShapeFluid("amod", "molten");
        registry.newMaterial("amod", "TestIron", texture).setPlaceable(molten, true).build();
        registry.editMaterial("amod", "TestIron").setPlaceable(molten, false);
        registry.resolve();

        assertFalse(FluidPlacement.isPlaceable(registry.getMaterial("amod", "TestIron"), molten));
    }
}
