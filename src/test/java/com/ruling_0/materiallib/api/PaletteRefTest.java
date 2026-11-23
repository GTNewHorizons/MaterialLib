package com.ruling_0.materiallib.api;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.util.List;
import java.util.Map;
import java.util.Set;

import net.minecraft.util.ResourceLocation;

import org.junit.jupiter.api.Test;

/// Pins what [PaletteRef] accepts, where a material's palette png lives, and how it derives a sprite name from a
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

    /// A material's palette png sits under its own modid, named after the material with the name's case kept.
    @Test
    void aMaterialsPaletteIsThePngNamedAfterIt() {
        MaterialRegistry registry = new MaterialRegistry();
        Material material = new Material(registry, "testmod", "StainlessSteel", Map.of(), Set.of(), List.of());
        registry.register(material);

        PaletteRef palette = PaletteRef.of(material);

        assertEquals(new PaletteRef("testmod", "StainlessSteel"), palette);
        assertEquals(new ResourceLocation("testmod", "textures/palettes/StainlessSteel.png"), palette.png());
    }

    /// The resolved path's ':' becomes a '/', leaving the baked name the one colon a ResourceLocation allows.
    @Test
    void theBakedNameFoldsTheResolvedPathIntoOneResourceLocation() {
        PaletteRef palette = new PaletteRef("testmod", "Iron");

        assertEquals("materiallib:mlbaked/examplemod/materials/dull/gear/testmod/Iron",
            palette.bakedIconName("examplemod:materials/dull/gear"));
    }
}
