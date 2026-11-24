package com.ruling_0.materiallib.api;

import java.util.Arrays;

/// Recolors shape art through a palette and flattens layers. The README describes the palette format.
final class PaletteBaker {

    private static final int LEVELS = 256;

    private PaletteBaker() {}

    /// Art reduced to gray levels: `levels` holds each pixel's [#level], or -1 where the pixel is fully transparent,
    /// and `alphas` holds each pixel's alpha.
    record IndexedImage(int width, int height, int[] levels, int[] alphas) {}

    /// A palette png read into a color for every gray level. Built through [PaletteBaker#palette].
    static final class Palette {

        private final int[] colors;
        private final boolean[] indexed;

        private Palette(int[] colors, boolean[] indexed) {
            this.colors = colors;
            this.indexed = indexed;
        }

        /// The `0xRRGGBB` color for pixels at gray level `level`.
        int color(int level) {
            return colors[level];
        }

        /// Whether some row's index pixel has gray level `level`.
        boolean indexes(int level) {
            return indexed[level];
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

    /// The palette in a png's first two columns, or null when the png is narrower than two columns or holds no entries.
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
        return new Palette(colors, indexed);
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
}
