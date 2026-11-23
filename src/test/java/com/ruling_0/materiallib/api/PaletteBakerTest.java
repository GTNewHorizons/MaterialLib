package com.ruling_0.materiallib.api;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

import java.awt.image.BufferedImage;
import java.awt.image.IndexColorModel;

import com.ruling_0.materiallib.api.PaletteBaker.IndexedImage;

import org.junit.jupiter.api.Test;

/// Pins the recoloring [PaletteBaker] performs: art and index pixels matching by gray level, the last entry taking
/// every unlisted level, which alpha a baked pixel keeps, how palette rows are read, the blend that flattens layers,
/// and the pixels it reads from and hands back as images.
class PaletteBakerTest {

    private static final int RED = 0xFFAA0000;
    private static final int GREEN = 0xFF00BB00;
    private static final int BLUE = 0xFF0000CC;

    /// A two-column palette png's pixels, one {index, color} pair per row.
    private static PaletteBaker.Palette palette(int[]... rows) {
        int[] argb = new int[rows.length * 2];
        for (int row = 0; row < rows.length; row++) {
            argb[row * 2] = rows[row][0];
            argb[row * 2 + 1] = rows[row][1];
        }
        return PaletteBaker.palette(argb, 2, rows.length);
    }

    /// Each pixel takes the color of the row indexing its gray level, and a level no row indexes takes the last
    /// entry.
    @Test
    void pixelsTakeTheColorOfTheirIndexRowAndUnlistedLevelsTheLastEntry() {
        IndexedImage art = PaletteBaker.index(new int[] { 0xFFFFFFFF, 0xFF808080, 0xFF202020, 0xFF7F7F7F }, 2, 2);
        PaletteBaker.Palette palette = palette(new int[] { 0xFF202020, RED }, new int[] { 0xFFFFFFFF, GREEN },
            new int[] { 0xFF808080, BLUE });

        assertArrayEquals(new int[] { GREEN, BLUE, RED, BLUE }, PaletteBaker.bake(art, palette));
    }

    /// Art and index pixels reduce to their brightest channel, so a channel off by a unit still finds its row, and
    /// hue never separates two pixels of one level.
    @Test
    void pixelsMatchByTheirBrightestChannel() {
        IndexedImage art = PaletteBaker.index(new int[] { 0xFF909190, 0xFF919191, 0xFF0000FF }, 3, 1);
        PaletteBaker.Palette palette = palette(new int[] { 0xFF919091, RED }, new int[] { 0xFFFFFFFF, BLUE },
            new int[] { 0xFF000000, GREEN });

        assertArrayEquals(new int[] { RED, RED, BLUE }, PaletteBaker.bake(art, palette));
    }

    /// The palette decides color and the art decides alpha, so partially transparent art bakes just as transparent
    /// and a fully transparent pixel stays clear. An index pixel's alpha takes no part in matching.
    @Test
    void matchingIgnoresAlphaAndThePixelKeepsTheArtAlpha() {
        IndexedImage art = PaletteBaker.index(new int[] { 0x80FFFFFF, 0x00FFFFFF }, 2, 1);
        PaletteBaker.Palette palette = palette(new int[] { 0x40FFFFFF, 0x11AA0000 }, new int[] { 0xFF000000, GREEN });

        assertArrayEquals(new int[] { 0x80AA0000, 0 }, PaletteBaker.bake(art, palette));
    }

    /// The first row wins a repeated level, a transparent index pixel matches nothing but can still be the default,
    /// and trailing rows with a transparent color are not entries.
    @Test
    void paletteRowsReadTopToBottom() {
        PaletteBaker.Palette palette = palette(new int[] { 0xFFFFFFFF, RED }, new int[] { 0xFFFFFFFE, GREEN },
            new int[] { 0, BLUE }, new int[] { 0xFF808080, 0 });

        assertEquals(RED & 0xFFFFFF, palette.color(255));
        assertEquals(BLUE & 0xFFFFFF, palette.color(0));
        assertEquals(BLUE & 0xFFFFFF, palette.color(128));
    }

    /// A png without both columns, or whose color column is empty, has no palette and leaves its material tinted.
    @Test
    void aPngNarrowerThanTwoColumnsOrWithoutEntriesHasNoPalette() {
        assertNull(PaletteBaker.palette(new int[] { 0xFFFFFFFF, 0xFF000000 }, 1, 2));
        assertNull(PaletteBaker.palette(new int[] { 0xFFFFFFFF, 0, 0xFF000000, 0 }, 2, 2));
    }

    /// Layers flatten in the order they are drawn: a later opaque pixel replaces what is under it, a clear one
    /// leaves it alone.
    @Test
    void layersCompositeInDrawOrder() {
        int[] flattened = { BLUE, BLUE, BLUE };

        PaletteBaker.compositeOver(flattened, new int[] { GREEN, GREEN, 0 });
        PaletteBaker.compositeOver(flattened, new int[] { RED, 0, 0 });

        assertArrayEquals(new int[] { RED, GREEN, BLUE }, flattened);
    }

    /// A partially transparent layer mixes with what is under it, and over a partially transparent destination the
    /// flattened pixel ends up more opaque than either.
    @Test
    void aPartiallyTransparentLayerBlendsIntoWhatIsBeneathIt() {
        int[] flattened = { 0xFF0000FF, 0x80FF0000 };

        PaletteBaker.compositeOver(flattened, new int[] { 0x80FF0000, 0x800000FF });

        assertArrayEquals(new int[] { 0xFF80007F, 0xC05500AA }, flattened);
    }

    /// Each layout read straight from the raster yields what `getRGB` would, alpha included.
    @Test
    void decodedLayoutsReadTheSameAsGetRgb() {
        int[] pixels = { 0x80FF0000, 0xFF00FF00, 0x000000FF, 0xFF123456 };
        BufferedImage rgba = new BufferedImage(2, 2, BufferedImage.TYPE_4BYTE_ABGR);
        BufferedImage rgb = new BufferedImage(2, 2, BufferedImage.TYPE_3BYTE_BGR);
        byte[] reds = { (byte) 0xFF, 0x00, 0x00, 0x12 };
        byte[] greens = { 0x00, (byte) 0xFF, 0x00, 0x34 };
        byte[] blues = { 0x00, 0x00, (byte) 0xFF, 0x56 };
        byte[] alphas = { (byte) 0x80, (byte) 0xFF, 0x00, (byte) 0xFF };
        BufferedImage indexed = new BufferedImage(2, 2, BufferedImage.TYPE_BYTE_INDEXED,
            new IndexColorModel(8, 4, reds, greens, blues, alphas));
        for (BufferedImage image : new BufferedImage[] { rgba, rgb, indexed }) {
            image.setRGB(0, 0, 2, 2, pixels, 0, 2);

            assertArrayEquals(image.getRGB(0, 0, 2, 2, null, 0, 2), PaletteBaker.argb(image));
        }
    }

    /// The image handed to `loadSprite` is a plain ARGB image over the baked pixels.
    @Test
    void theBakedImageIsArgbOverItsPixels() {
        int[] pixels = { 0x80FF0000, 0xFF00FF00 };

        BufferedImage image = PaletteBaker.image(pixels, 1, 2);

        assertEquals(BufferedImage.TYPE_INT_ARGB, image.getType());
        assertArrayEquals(pixels, image.getRGB(0, 0, 1, 2, null, 0, 1));
    }
}
