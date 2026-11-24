package com.ruling_0.materiallib.api;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.util.List;
import java.util.Map;
import java.util.Set;

import net.minecraft.util.ResourceLocation;

import org.junit.jupiter.api.Test;

/// Pins what [PaletteRef] accepts, where a material's palette png is looked up, and how it derives a sprite name from a
/// resolved texture path.
class PaletteRefTest {

    /// The modid and name take the identifier rules: non-null, non-empty, free of ':' and whitespace.
    @Test
    void anInvalidModidOrNameIsRejected() {
        assertThrows(IllegalArgumentException.class, () -> new PaletteRef(null, "Iron"));
        assertThrows(IllegalArgumentException.class, () -> new PaletteRef("", "Iron"));
        assertThrows(IllegalArgumentException.class, () -> new PaletteRef("test:mod", "Iron"));
        assertThrows(IllegalArgumentException.class, () -> new PaletteRef("testmod", null));
        assertThrows(IllegalArgumentException.class, () -> new PaletteRef("testmod", ""));
        assertThrows(IllegalArgumentException.class, () -> new PaletteRef("testmod", "hot iron"));
    }

    /// A material's palette png is named after the material with the name's case kept. The owner's png wins, and a
    /// png shipped only under another declaring mod's modid is used when the owner ships none.
    @Test
    void aMaterialsPaletteIsThePngNamedAfterItUnderTheFirstDeclaringModidShippingOne() {
        MaterialRegistry registry = new MaterialRegistry();
        Material owner = new Material(registry, "amod", "StainlessSteel", Map.of(), Set.of(), List.of());
        registry.register(owner);
        registry.register(new Material(registry, "bmod", "StainlessSteel", Map.of(), Set.of(), List.of()));
        registry.resolve();
        ResourceLocation ownerPng = new ResourceLocation("amod", "textures/palettes/StainlessSteel.png");
        ResourceLocation otherPng = new ResourceLocation("bmod", "textures/palettes/StainlessSteel.png");

        assertEquals(ownerPng, PaletteRef.find(owner, Set.of(ownerPng, otherPng)::contains).png());
        assertEquals(otherPng, PaletteRef.find(owner, Set.of(otherPng)::contains).png());
        assertNull(PaletteRef.find(owner, location -> false));
    }

    /// The resolved path's ':' becomes a '/', leaving the baked name the one colon a ResourceLocation allows.
    @Test
    void theBakedNameFoldsTheResolvedPathIntoOneResourceLocation() {
        PaletteRef palette = new PaletteRef("testmod", "Iron");

        assertEquals("materiallib:mlbaked/examplemod/materials/dull/gear/testmod/Iron",
            palette.bakedIconName("examplemod:materials/dull/gear"));
    }
}
