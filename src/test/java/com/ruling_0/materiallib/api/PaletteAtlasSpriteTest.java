package com.ruling_0.materiallib.api;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;

import java.awt.image.BufferedImage;
import java.io.IOException;
import java.util.List;

import net.minecraft.client.resources.data.AnimationMetadataSection;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

/// Pins how [PaletteAtlasSprite] flattens a layer stack: layers are strips of the base's exact size, baked and
/// drawn frame for frame, and any other size is left out. Also pins the level array it hands `loadSprite`: the art
/// followed by one null slot per client mipmap level.
class PaletteAtlasSpriteTest {

    private static final int WHITE = 0xFFFFFFFF;
    private static final int GRAY = 0xFF808080;
    private static final int DARK = 0xFF404040;
    private static final int RED = 0xFFAA0000;
    private static final int GREEN = 0xFF00BB00;
    private static final int BLUE = 0xFF0000CC;

    private static final String BASE = "testmod:materials/anim/ingot";
    private static final String LAYER = BASE + ShapeIcons.LAYER_SUFFIX + 1;
    private static final String OVERLAY = BASE + ShapeIcons.OVERLAY_SUFFIX;
    private static final PaletteBaker.Palette PALETTE = PaletteBaker.palette(
        new int[] { WHITE, RED, GRAY, GREEN, DARK, RED }, 2, 3);

    /// Two 1x1 frames stacked into a 1x2 strip.
    private static final AnimationMetadataSection TWO_FRAMES = PngResources.animation(20);
    private static final AnimationMetadataSection LAYER_ONLY = PngResources.animation(5);

    private final PngResources resources = new PngResources();

    @BeforeEach
    void serveBase() throws IOException {
        resources.put(PngResources.itemPng(BASE), 1, 2, new int[] { WHITE, GRAY }, TWO_FRAMES);
    }

    /// The base's two shades take the palette's two entries. A numbered layer marking only the second frame is
    /// baked through its own single rank and drawn on that frame alone; an overlay marking only the first frame is
    /// drawn as authored on that frame alone. The layer's own animation metadata is ignored.
    @Test
    void layersBakeFrameForFrameWithTheBaseStrip() throws IOException {
        resources.put(PngResources.itemPng(LAYER), 1, 2, new int[] { 0, DARK }, LAYER_ONLY);
        resources.put(PngResources.itemPng(OVERLAY), 1, 2, new int[] { BLUE, 0 }, null);
        PaletteAtlasSprite sprite = new PaletteAtlasSprite("materiallib:mlbaked/anim", true, BASE, List.of(LAYER),
            OVERLAY, PALETTE);

        PaletteAtlasSprite.Art art = sprite.bake(resources, new PaletteArt());

        assertSame(TWO_FRAMES, art.animation());
        assertArrayEquals(new int[] { BLUE, RED }, art.image().getRGB(0, 0, 1, 2, null, 0, 1));
    }

    /// A layer authored at frame size against a two-frame base is left out.
    @Test
    void aLayerThatIsNotTheBaseStripIsLeftOut() throws IOException {
        resources.put(PngResources.itemPng(LAYER), 1, 1, new int[] { DARK }, null);
        PaletteAtlasSprite sprite = new PaletteAtlasSprite("materiallib:mlbaked/anim", true, BASE, List.of(LAYER), null,
            PALETTE);

        PaletteAtlasSprite.Art art = sprite.bake(resources, new PaletteArt());

        assertArrayEquals(new int[] { RED, GREEN }, art.image().getRGB(0, 0, 1, 2, null, 0, 1));
    }

    /// One slot per mipmap level sits above the art -- four of them at the default client setting -- and every one
    /// of them is left for the atlas to fill.
    @Test
    void theFrameLadderHoldsTheArtAndOneNullSlotPerMipmapLevel() {
        for (int mipmapLevels : new int[] { 0, 1, 4 }) {
            BufferedImage art = new BufferedImage(16, 16, BufferedImage.TYPE_INT_ARGB);
            BufferedImage[] levels = PaletteAtlasSprite.frameLadder(art, mipmapLevels);

            assertEquals(1 + mipmapLevels, levels.length, "ladder length at " + mipmapLevels + " mipmap levels");
            assertSame(art, levels[0]);
            for (int level = 1; level < levels.length; level++) {
                assertNull(levels[level], "level " + level + " must be left for the atlas to generate");
            }
        }
    }
}
