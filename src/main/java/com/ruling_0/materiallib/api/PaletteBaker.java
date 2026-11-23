package com.ruling_0.materiallib.api;

import java.awt.image.BufferedImage;
import java.awt.image.ColorModel;
import java.awt.image.DataBuffer;
import java.awt.image.DataBufferByte;
import java.awt.image.DataBufferInt;
import java.awt.image.DirectColorModel;
import java.awt.image.IndexColorModel;
import java.awt.image.Raster;
import java.awt.image.WritableRaster;
import java.util.Arrays;

/// Handles the pixel-level arithmetic behind [PaletteRef].
///
/// A palette png pairs an index pixel with a color in each row, and baking replaces every art pixel whose gray level
/// matches a row's index pixel with that row's color. A gray level is the brightest channel, so art with a unit or two
/// of channel noise still finds its row. A level no row indexes takes the last entry. The art keeps its own alpha.
final class PaletteBaker {

    private static final int LEVELS = 256;

    private PaletteBaker() {}

    /// Art reduced to gray levels. `levels` holds each pixel's [#level], or -1 where the pixel is fully transparent and
    /// takes no color from the palette; `alphas` holds each pixel's own alpha, which survives baking untouched.
    record IndexedImage(int width, int height, int[] levels, int[] alphas) {}

    /// A palette png read into a color for every gray level. Built through [PaletteBaker#palette].
    static final class Palette {

        private final int[] colors;
        private final boolean[] indexed;
        private final int size;

        private Palette(int[] colors, boolean[] indexed, int size) {
            this.colors = colors;
            this.indexed = indexed;
            this.size = size;
        }

        /// The `0xRRGGBB` color gray level `level` bakes to.
        int color(int level) {
            return colors[level];
        }

        /// Whether some row's index pixel has gray level `level`.
        boolean indexes(int level) {
            return indexed[level];
        }

        /// The number of entries.
        int size() {
            return size;
        }
    }

    /// Reduces every pixel of `argb` to its gray level.
    static IndexedImage index(int[] argb, int width, int height) {
        int[] levels = new int[argb.length];
        int[] alphas = new int[argb.length];
        for (int i = 0; i < argb.length; i++) {
            int alpha = argb[i] >>> 24;
            alphas[i] = alpha;
            levels[i] = alpha == 0 ? -1 : level(argb[i]);
        }
        return new IndexedImage(width, height, levels, alphas);
    }

    /// The palette in a png's first two columns, rows running top to bottom, or null when the png is narrower than two
    /// columns or its color column holds no entries. Trailing rows with a fully transparent color are not entries. A
    /// row whose index pixel is fully transparent indexes no level but still counts as an entry. The first row indexing
    /// a level wins it.
    static Palette palette(int[] argb, int width, int height) {
        if (width < 2) return null;
        int entries = height;
        while (entries > 0 && (argb[(entries - 1) * width + 1] >>> 24) == 0) {
            entries--;
        }
        if (entries == 0) return null;
        int[] colors = new int[LEVELS];
        Arrays.fill(colors, argb[(entries - 1) * width + 1] & 0xFFFFFF);
        boolean[] indexed = new boolean[LEVELS];
        for (int row = 0; row < entries; row++) {
            int index = argb[row * width];
            if ((index >>> 24) == 0) continue;
            int level = level(index);
            if (indexed[level]) continue;
            indexed[level] = true;
            colors[level] = argb[row * width + 1] & 0xFFFFFF;
        }
        return new Palette(colors, indexed, entries);
    }

    /// Recolors `art` through `palette`. Each pixel keeps the art's alpha, and a fully transparent pixel stays fully
    /// transparent.
    static int[] bake(IndexedImage art, Palette palette) {
        int[] levels = art.levels();
        int[] alphas = art.alphas();
        int[] baked = new int[levels.length];
        for (int i = 0; i < baked.length; i++) {
            int level = levels[i];
            if (level < 0) continue;
            baked[i] = (alphas[i] << 24) | palette.color(level);
        }
        return baked;
    }

    /// The number of distinct gray levels in `art` that no row of `palette` indexes.
    static int unmatched(IndexedImage art, Palette palette) {
        boolean[] seen = new boolean[LEVELS];
        int unmatched = 0;
        for (int level : art.levels()) {
            if (level < 0 || seen[level]) continue;
            seen[level] = true;
            if (!palette.indexes(level)) unmatched++;
        }
        return unmatched;
    }

    /// The gray level of `argb`: its brightest channel.
    static int level(int argb) {
        return Math.max(Math.max((argb >> 16) & 0xFF, (argb >> 8) & 0xFF), argb & 0xFF);
    }

    /// Draws `src` over `dst` in place, leaving `dst` holding the flattened result. Both arrays cover the same
    /// pixels in the same order.
    static void compositeOver(int[] dst, int[] src) {
        /*
         * Straight-alpha source-over. The destination survives in proportion to what the source lets through,
         * carried = da * (255 - sa) / 255, and each channel is the alpha-weighted sum of source and destination
         * divided by the resulting alpha. Both divisions round half up by adding half the divisor first, so
         * successive layers do not drift darker. A fully opaque or fully clear source reaches the same result
         * through the general formula; the shortcuts only skip the arithmetic.
         */
        for (int i = 0; i < dst.length; i++) {
            int srcAlpha = src[i] >>> 24;
            if (srcAlpha == 0) continue;
            int dstAlpha = dst[i] >>> 24;
            if (srcAlpha == 255 || dstAlpha == 0) {
                dst[i] = src[i];
                continue;
            }
            int carried = (dstAlpha * (255 - srcAlpha) + 127) / 255;
            int alpha = srcAlpha + carried;
            int half = alpha / 2;
            int red = (((src[i] >> 16) & 0xFF) * srcAlpha + ((dst[i] >> 16) & 0xFF) * carried + half) / alpha;
            int green = (((src[i] >> 8) & 0xFF) * srcAlpha + ((dst[i] >> 8) & 0xFF) * carried + half) / alpha;
            int blue = ((src[i] & 0xFF) * srcAlpha + (dst[i] & 0xFF) * carried + half) / alpha;
            dst[i] = (alpha << 24) | (red << 16) | (green << 8) | blue;
        }
    }

    /// The pixels of `image` as ARGB, row by row. The layouts ImageIO decodes 8-bit PNGs into (RGBA, RGB and
    /// indexed color) are read straight from the raster; any other layout goes through `getRGB`, which converts one
    /// pixel at a time.
    static int[] argb(BufferedImage image) {
        int width = image.getWidth();
        int height = image.getHeight();
        int count = width * height;
        DataBuffer buffer = image.getRaster().getDataBuffer();
        if (image.getRaster().getParent() == null && buffer instanceof DataBufferByte bytes &&
            bytes.getNumBanks() == 1 && bytes.getOffset() == 0) {
            byte[] data = bytes.getData();
            int[] argb = new int[count];
            switch (image.getType()) {
                case BufferedImage.TYPE_4BYTE_ABGR -> {
                    if (data.length != count * 4) break;
                    for (int i = 0, j = 0; i < count; i++, j += 4) {
                        argb[i] = (data[j] & 0xFF) << 24 | (data[j + 3] & 0xFF) << 16 | (data[j + 2] & 0xFF) << 8 |
                            data[j + 1] & 0xFF;
                    }
                    return argb;
                }
                case BufferedImage.TYPE_3BYTE_BGR -> {
                    if (data.length != count * 3) break;
                    for (int i = 0, j = 0; i < count; i++, j += 3) {
                        argb[i] = 0xFF000000 | (data[j + 2] & 0xFF) << 16 | (data[j + 1] & 0xFF) << 8 | data[j] & 0xFF;
                    }
                    return argb;
                }
                case BufferedImage.TYPE_BYTE_INDEXED -> {
                    if (data.length != count) break;
                    int[] colors = new int[256];
                    ((IndexColorModel) image.getColorModel()).getRGBs(colors);
                    for (int i = 0; i < count; i++) {
                        argb[i] = colors[data[i] & 0xFF];
                    }
                    return argb;
                }
                default -> {}
            }
        }
        return image.getRGB(0, 0, width, height, null, 0, width);
    }

    /// An ARGB image drawn straight from `pixels`, row by row, without copying them.
    static BufferedImage image(int[] pixels, int width, int height) {
        DirectColorModel model = (DirectColorModel) ColorModel.getRGBdefault();
        WritableRaster raster = Raster.createPackedRaster(new DataBufferInt(pixels, pixels.length), width, height,
            width, model.getMasks(), null);
        return new BufferedImage(model, raster, false, null);
    }
}
