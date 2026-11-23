package com.ruling_0.materiallib.api;

import static org.junit.jupiter.api.Assertions.assertEquals;

import org.junit.jupiter.api.Test;

/// Pins the pack-facing lang keys the standard tint properties and the numbered icon layers read, and the coded
/// value a read falls back to with no lang entry present.
class MaterialTintsTest {

    private final MaterialRegistry registry = new MaterialRegistry();
    private final TextureSet texture = TextureSet.of("testmod", "shiny");

    @Test
    void aTintReadsTheLangKeyNamedAfterItsMaterialAndProperty() {
        Material material = registry.newMaterial("testmod", "TestIron", texture).setTint(0xFFFFCC00).build();
        registry.resolve();

        assertEquals("color.resource.materiallib.TestIron.tint",
            MaterialTints.resourceFor(material, StandardProperties.TINT).getLangKey());
        assertEquals(0xFFFFCC00, MaterialTints.color(material, StandardProperties.TINT));
    }

    @Test
    void aLayerTintReadsTheLangKeyNumberedAfterItsLayer() {
        Material material = registry.newMaterial("testmod", "TestBronze", texture)
            .setLayerTints(0xFF00FF00, 0xFF0000FF).build();
        registry.resolve();

        assertEquals("color.resource.materiallib.TestBronze.tint.1",
            MaterialTints.layerResourceFor(material, 1).getLangKey());
        assertEquals(0xFF00FF00, MaterialTints.layerColor(material, 1));
        assertEquals(0xFF0000FF, MaterialTints.layerColor(material, 2));
        assertEquals(0xFFFFFFFF, MaterialTints.layerColor(material, 3));
    }
}
