package com.ruling_0.materiallib.api;

import java.util.Arrays;

import net.minecraft.client.resources.data.AnimationMetadataSection;

import cpw.mods.fml.relauncher.Side;
import cpw.mods.fml.relauncher.SideOnly;

/// Reduces a texture's strip and `.mcmeta` to the frame sequence vanilla's `TextureAtlasSprite` plays, so the palette
/// path can tell whether the textures of one shape animate in lockstep.
@SideOnly(Side.CLIENT)
final class PaletteAnimation {

    private PaletteAnimation() {}

    /// The frames a texture shows: the strip frame at each step in `order`, held for that step's ticks in `times`.
    record Track(int frameCount, int[] order, int[] times) {

        private static final Track STATIC = new Track(1, new int[] { 0 }, new int[] { 1 });

        /// The track of a `width` x `height` png with `animation`, or null when vanilla would refuse it: a non-square
        /// png without animation, a strip shorter than one frame, or a frame list naming a frame past the strip. Frames
        /// are square at the png's width.
        static Track of(int width, int height, AnimationMetadataSection animation) {
            if (width <= 0) return null;
            if (animation == null) return height == width ? STATIC : null;
            int frameCount = height / width;
            if (frameCount == 0) return null;
            boolean listed = animation.getFrameCount() > 0;
            int steps = listed ? animation.getFrameCount() : frameCount;
            int[] order = new int[steps];
            int[] times = new int[steps];
            for (int step = 0; step < steps; step++) {
                order[step] = listed ? animation.getFrameIndex(step) : step;
                if (order[step] < 0 || order[step] >= frameCount) return null;
                times[step] = Math.max(1, listed ? animation.getFrameTimeSingle(step) : animation.getFrameTime());
            }
            return new Track(frameCount, order, times);
        }
    }

    /// Whether `a` and `b` show the same frames for the same ticks.
    static boolean sameTimeline(Track a, Track b) {
        return a.frameCount() == b.frameCount() && Arrays.equals(a.order(), b.order()) &&
            Arrays.equals(a.times(), b.times());
    }
}
