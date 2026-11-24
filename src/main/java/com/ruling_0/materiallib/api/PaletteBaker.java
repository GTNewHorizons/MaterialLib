package com.ruling_0.materiallib.api;

import java.util.Arrays;

/// Recolors shape art through a palette and flattens layers. The README describes the palette format.
final class PaletteBaker {

    private static final int HSV_VALUES = 256;

    private PaletteBaker() {}

    /// Art reduced to HSV values: `hsvValues` holds each pixel's [#hsvValue], or -1 where the pixel is fully
    /// transparent, and `alphas` holds each pixel's alpha.
    record IndexedImage(int width, int height, int[] hsvValues, int[] alphas) {}

    /// A palette png read into a color for every HSV value. Built through [PaletteBaker#palette].
    static final class Palette {

        private final int[] colors;
        private final boolean[] indexed;

        private Palette(int[] colors, boolean[] indexed) {
            this.colors = colors;
            this.indexed = indexed;
        }

        /// The `0xRRGGBB` color for pixels with HSV value `hsvValue`.
        int color(int hsvValue) {
            return colors[hsvValue];
        }

        /// Whether some row's index pixel has HSV value `hsvValue`.
        boolean indexes(int hsvValue) {
            return indexed[hsvValue];
        }
    }

    /// Reduces every pixel of `argb` to its HSV value.
    static IndexedImage index(int[] argb, int width, int height) {
        int[] hsvValues = new int[argb.length];
        int[] alphas = new int[argb.length];
        for (int i = 0; i < argb.length; i++) {
            int alpha = argb[i] >>> 24;
            alphas[i] = alpha;
            hsvValues[i] = alpha == 0 ? -1 : hsvValue(argb[i]);
        }
        return new IndexedImage(width, height, hsvValues, alphas);
    }

    /// The palette in a png's first two columns, or null when the png is narrower than two columns or holds no entries.
    static Palette palette(int[] argb, int width, int height) {
        if (width < 2) return null;
        int entries = height;
        while (entries > 0 && (argb[(entries - 1) * width + 1] >>> 24) == 0) {
            entries--;
        }
        if (entries == 0) return null;
        int[] colors = new int[HSV_VALUES];
        Arrays.fill(colors, argb[(entries - 1) * width + 1] & 0xFFFFFF);
        boolean[] indexed = new boolean[HSV_VALUES];
        for (int row = 0; row < entries; row++) {
            int index = argb[row * width];
            if ((index >>> 24) == 0) continue;
            int hsvValue = hsvValue(index);
            if (indexed[hsvValue]) continue;
            indexed[hsvValue] = true;
            colors[hsvValue] = argb[row * width + 1] & 0xFFFFFF;
        }
        return new Palette(colors, indexed);
    }

    /// Recolors `art` through `palette`. Each pixel keeps the art's alpha, and a fully transparent pixel stays fully
    /// transparent.
    static int[] bake(IndexedImage art, Palette palette) {
        int[] hsvValues = art.hsvValues();
        int[] alphas = art.alphas();
        int[] baked = new int[hsvValues.length];
        for (int i = 0; i < baked.length; i++) {
            int hsvValue = hsvValues[i];
            if (hsvValue < 0) continue;
            baked[i] = (alphas[i] << 24) | palette.color(hsvValue);
        }
        return baked;
    }

    /// The number of distinct HSV values in `art` that no row of `palette` indexes.
    static int unmatched(IndexedImage art, Palette palette) {
        boolean[] seen = new boolean[HSV_VALUES];
        int unmatched = 0;
        for (int hsvValue : art.hsvValues()) {
            if (hsvValue < 0 || seen[hsvValue]) continue;
            seen[hsvValue] = true;
            if (!palette.indexes(hsvValue)) unmatched++;
        }
        return unmatched;
    }

    /// The HSV value of `argb`: its brightest channel.
    static int hsvValue(int argb) {
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
