package com.ruling_0.materiallib.api;

import static com.ruling_0.materiallib.api.PngResources.animation;
import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import net.minecraft.client.resources.data.AnimationFrame;

import org.junit.jupiter.api.Test;

/// Pins how [PaletteAnimation] reads a `.mcmeta` frame list: listed frames play in their own order for their own
/// times, and a list spelling out the default order is the same timeline as no list at all.
class PaletteAnimationTest {

    @Test
    void anExplicitFrameListIsFollowedWithItsOwnTimes() {
        PaletteAnimation.Track track = PaletteAnimation.Track
            .of(1, 2, animation(2, new AnimationFrame(1, 4), new AnimationFrame(0)));

        assertArrayEquals(new int[] { 1, 0 }, track.order());
        assertArrayEquals(new int[] { 4, 2 }, track.times());
    }

    @Test
    void anExplicitListOfTheDefaultOrderSharesItsTimeline() {
        PaletteAnimation.Track listed = PaletteAnimation.Track
            .of(1, 2, animation(20, new AnimationFrame(0), new AnimationFrame(1)));
        PaletteAnimation.Track unlisted = PaletteAnimation.Track.of(1, 2, animation(20));

        assertTrue(PaletteAnimation.sameTimeline(listed, unlisted));
    }
}
