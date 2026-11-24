package com.ruling_0.materiallib.api;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;

import java.awt.Transparency;
import java.awt.color.ColorSpace;
import java.awt.image.BufferedImage;
import java.awt.image.ComponentColorModel;
import java.awt.image.DataBuffer;
import java.awt.image.IndexColorModel;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;

import javax.imageio.ImageIO;

import org.junit.jupiter.api.Test;

/// Pins how [PaletteArt] reads images into ARGB pixels and hands pixels back as an image.
class PaletteArtTest {

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

            assertArrayEquals(image.getRGB(0, 0, 2, 2, null, 0, 2), PaletteArt.argb(image));
        }
    }

    /// A gray png reads as its stored samples, not as `getRGB`'s conversion out of linear gray. A 16-bit sample keeps
    /// its high byte.
    @Test
    void grayPngsReadTheirStoredSamples() throws IOException {
        BufferedImage gray = new BufferedImage(2, 1, BufferedImage.TYPE_BYTE_GRAY);
        gray.getRaster().setPixels(0, 0, 2, 1, new int[] { 0x80, 0x12 });
        BufferedImage wideGray = new BufferedImage(2, 1, BufferedImage.TYPE_USHORT_GRAY);
        wideGray.getRaster().setPixels(0, 0, 2, 1, new int[] { 0x80FF, 0x1200 });
        ComponentColorModel grayAlphaModel = new ComponentColorModel(ColorSpace.getInstance(ColorSpace.CS_GRAY), true,
            false, Transparency.TRANSLUCENT, DataBuffer.TYPE_BYTE);
        BufferedImage grayAlpha = new BufferedImage(grayAlphaModel,
            grayAlphaModel.createCompatibleWritableRaster(2, 1), false, null);
        grayAlpha.getRaster().setPixels(0, 0, 2, 1, new int[] { 0x80, 0x40, 0x12, 0xFF });

        assertArrayEquals(new int[] { 0xFF808080, 0xFF121212 }, PaletteArt.argb(throughPng(gray)));
        assertArrayEquals(new int[] { 0xFF808080, 0xFF121212 }, PaletteArt.argb(throughPng(wideGray)));
        assertArrayEquals(new int[] { 0x40808080, 0xFF121212 }, PaletteArt.argb(throughPng(grayAlpha)));
    }

    /// The image handed to `loadSprite` is a plain ARGB image over the baked pixels.
    @Test
    void theBakedImageIsArgbOverItsPixels() {
        int[] pixels = { 0x80FF0000, 0xFF00FF00 };

        BufferedImage image = PaletteArt.image(pixels, 1, 2);

        assertEquals(BufferedImage.TYPE_INT_ARGB, image.getType());
        assertArrayEquals(pixels, image.getRGB(0, 0, 1, 2, null, 0, 1));
    }

    private static BufferedImage throughPng(BufferedImage image) throws IOException {
        ByteArrayOutputStream png = new ByteArrayOutputStream();
        ImageIO.write(image, "png", png);
        return ImageIO.read(new ByteArrayInputStream(png.toByteArray()));
    }
}
