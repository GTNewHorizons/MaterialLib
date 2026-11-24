package com.ruling_0.materiallib.api;

import static com.ruling_0.materiallib.api.PngResources.animation;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.IOException;
import java.util.List;
import java.util.Map;
import java.util.Set;

import net.minecraft.client.renderer.texture.TextureMap;
import net.minecraft.util.ResourceLocation;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

/// Pins what [PaletteSprites#bake] binds on a real item atlas, with textures and palette pngs served from memory.
class PaletteSpritesTest {

    private static final int WHITE = 0xFFFFFFFF;

    private final TextureSet set = TextureSet.of("testmod", "anim");
    private final String base = set.iconPath("ingot");
    private final String layer = base + ShapeIcons.LAYER_SUFFIX + 1;
    private final String overlay = base + ShapeIcons.OVERLAY_SUFFIX;
    private final PngResources resources = new PngResources();

    @BeforeEach
    void dropTheLastStitch() {
        new PaletteSprites().onTextureStitched(null);
    }

    /// A stack drawn in passes binds one baked sprite per pass. An overlay that the layout leaves out is neither put
    /// on the atlas nor reported as the overlay layer.
    @Test
    void anOverlayLeftOutOfAStackDrawnInPassesIsNotBound() throws IOException {
        MaterialRegistry registry = new MaterialRegistry();
        Material material = new Material(registry, "testmod", "Testiron", Map.of(StandardProperties.TEXTURE_SET, set),
            Set.of(), List.of());
        registry.register(material);
        registry.resolve();
        resources.put(new ResourceLocation("testmod", "textures/palettes/Testiron.png"), 2, 1,
            new int[] { WHITE, 0xFFAA0000 }, null);
        resources.put(PngResources.itemPng(base), 1, 2, new int[] { WHITE, WHITE }, animation(20));
        resources.put(PngResources.itemPng(layer), 1, 2, new int[] { WHITE, WHITE }, animation(10));
        resources.put(PngResources.itemPng(overlay), 1, 2, new int[] { WHITE, WHITE }, null);
        TextureMap atlas = new TextureMap(1, "textures/items");
        int index = material.getIndex();

        ShapeIcons icons = new ShapeIcons(true, Set.of(base, layer, overlay)::contains,
            (register, isItem, stack, bound) -> PaletteSprites.bake(register, resources, resources::has, isItem, stack,
                bound));
        icons.bind(atlas, new Material[] { material }, "ingot");

        assertTrue(icons.isPaletteBaked(index));
        assertEquals(2, icons.layerCount(index));
        assertFalse(icons.isOverlayLayer(index, 1));
        assertNull(atlas.getTextureExtry(overlay));
    }
}
