package com.ruling_0.materiallib.api;

import static com.ruling_0.materiallib.api.PngResources.animation;
import static org.junit.jupiter.api.Assertions.assertEquals;

import java.io.IOException;
import java.util.Arrays;
import java.util.List;

import net.minecraft.client.resources.data.AnimationFrame;
import net.minecraft.client.resources.data.AnimationMetadataSection;

import org.junit.jupiter.api.Test;

/// Pins when [PaletteArt#layout] flattens a palette-baked stack into one sprite: only when every usable texture has
/// the base's size and timeline. Otherwise the stack draws in passes. Art vanilla would refuse is left out without
/// keeping the rest of the stack from flattening.
class PaletteLayoutTest {

    private static final String BASE = "testmod:materials/anim/ingot";
    private static final String LAYER = BASE + ShapeIcons.LAYER_SUFFIX + 1;
    private static final String LAYER2 = BASE + ShapeIcons.LAYER_SUFFIX + 2;
    private static final String OVERLAY = BASE + ShapeIcons.OVERLAY_SUFFIX;

    private final PngResources resources = new PngResources();

    @Test
    void artSharingTheBaseTimelineFlattens() throws IOException {
        serve(BASE, 1, 2, animation(20));
        serve(LAYER, 1, 2, animation(20));
        serve(OVERLAY, 1, 2, animation(20));

        assertEquals(new PaletteArt.Layout(true, List.of(LAYER), OVERLAY), layout(List.of(LAYER), OVERLAY));
    }

    @Test
    void anOverlayOnItsOwnTimelineDrawsInPasses() throws IOException {
        serve(BASE, 1, 2, animation(20));
        serve(LAYER, 1, 2, animation(20));
        serve(OVERLAY, 1, 2, animation(10));

        assertEquals(new PaletteArt.Layout(false, List.of(LAYER), OVERLAY), layout(List.of(LAYER), OVERLAY));
    }

    @Test
    void aLayerOfAnotherSizeDrawsInPasses() throws IOException {
        serve(BASE, 1, 1, null);
        serve(LAYER, 2, 2, null);

        assertEquals(new PaletteArt.Layout(false, List.of(LAYER), null), layout(List.of(LAYER), null));
    }

    /// A strip without metadata, a frame list naming a frame past its strip, and an unusable overlay are each left
    /// out, and the base still flattens with what remains.
    @Test
    void unusableArtIsLeftOutWithoutBlockingFlattening() throws IOException {
        serve(BASE, 1, 1, null);
        serve(LAYER, 1, 2, null);
        serve(LAYER2, 1, 2, animation(20, new AnimationFrame(0), new AnimationFrame(2)));
        serve(OVERLAY, 1, 2, null);

        assertEquals(new PaletteArt.Layout(true, List.of(), null), layout(List.of(LAYER, LAYER2), OVERLAY));
    }

    private PaletteArt.Layout layout(List<String> layers, String overlay) {
        return new PaletteArt().layout(resources, true, new ShapeIcons.StackPaths(BASE, layers, overlay));
    }

    private void serve(String key, int width, int height, AnimationMetadataSection animation) throws IOException {
        int[] white = new int[width * height];
        Arrays.fill(white, 0xFFFFFFFF);
        resources.put(PngResources.itemPng(key), width, height, white, animation);
    }
}
