package com.ruling_0.materiallib.api;

import java.awt.image.BufferedImage;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.imageio.ImageIO;

import net.minecraft.client.resources.IResource;
import net.minecraft.client.resources.IResourceManager;
import net.minecraft.client.resources.data.AnimationMetadataSection;
import net.minecraft.util.ResourceLocation;

import com.gtnewhorizon.gtnhlib.util.ResourceUtil;
import com.ruling_0.materiallib.MaterialLib;

import cpw.mods.fml.relauncher.Side;
import cpw.mods.fml.relauncher.SideOnly;

/// The textures [PaletteAtlasSprite]s bake from, each decoded and reduced to gray levels once, and the [Layout] each
/// shape's stack binds with.
///
/// Every material baking a shape draws on that shape's base texture, layers and overlay, so the sprites of one stitch
/// share a single read of each file instead of decoding it once per material. The arrays handed out are shared and must
/// not be written to.
///
/// A stack flattens into one sprite only when every texture in it has the base's size and animation timeline, so one
/// strip under the base's `.mcmeta` shows all of them exactly. Any other stack draws in passes like the tint path, each
/// texture animating on its own.
@SideOnly(Side.CLIENT)
final class PaletteArt {

    /// A texture's pixels, their gray levels, and its animation metadata, if any.
    record Texture(int width, int height, int[] argb, PaletteBaker.IndexedImage indexed,
                   AnimationMetadataSection animation) {}

    /// Whether a stack bakes into one sprite, the numbered layer paths it keeps in draw order, and the overlay path
    /// when the overlay is usable, else null.
    record Layout(boolean flattened, List<String> layers, String overlay) {}

    /// A readable texture vanilla would accept as a sprite, and the frames it shows.
    private record Usable(Texture texture, PaletteAnimation.Track track) {

        boolean linesUpWith(Usable base) {
            return texture.width() == base.texture.width() && texture.height() == base.texture.height() &&
                PaletteAnimation.sameTimeline(track, base.track);
        }
    }

    private final Map<ResourceLocation, Texture> textures = new HashMap<>();
    private final Map<ResourceLocation, IOException> failures = new HashMap<>();
    private final Map<String, Layout> layouts = new HashMap<>();

    /// The texture at `location`, read from `manager` on first request. A texture that could not be read throws
    /// the same exception on every request.
    Texture get(IResourceManager manager, ResourceLocation location) throws IOException {
        Texture texture = textures.get(location);
        if (texture != null) return texture;
        IOException failure = failures.get(location);
        if (failure != null) throw failure;
        try {
            texture = read(manager.getResource(location));
        }
        catch (IOException e) {
            failures.put(location, e);
            throw e;
        }
        textures.put(location, texture);
        return texture;
    }

    /// How `stack` binds on the item atlas when `isItem`, the block atlas otherwise. A numbered layer that cannot be
    /// read or that vanilla would refuse as a sprite is left out with a warning; such an overlay takes no part. A base
    /// of either kind keeps the stack flattened, so its sprite resolves as the missing texture.
    Layout layout(IResourceManager manager, boolean isItem, ShapeIcons.StackPaths stack) {
        String key = (isItem ? "items:" : "blocks:") + stack.base();
        Layout layout = layouts.get(key);
        if (layout == null) {
            layout = findLayout(manager, isItem, stack);
            layouts.put(key, layout);
        }
        return layout;
    }

    /// The complete location of the texture at icon path `path` on the item atlas when `isItem`, the block atlas
    /// otherwise.
    static ResourceLocation location(boolean isItem, String path) {
        if (isItem) return ResourceUtil.getCompleteItemTextureResourceLocation(path);
        else return ResourceUtil.getCompleteBlockTextureResourceLocation(path);
    }

    private Layout findLayout(IResourceManager manager, boolean isItem, ShapeIcons.StackPaths stack) {
        Usable base = usable(manager, isItem, stack.base());
        if (base == null) return new Layout(true, List.of(), null);
        boolean flattened = true;
        List<String> layers = new ArrayList<>();
        for (String path : stack.layers()) {
            Usable layer = usable(manager, isItem, path);
            if (layer == null) {
                MaterialLib.LOG.warn(
                    "Leaving {} out of {}: unreadable, or neither a square texture nor an animation strip", path,
                    stack.base());
                continue;
            }
            layers.add(path);
            flattened &= layer.linesUpWith(base);
        }
        Usable overlay = stack.overlay() != null ? usable(manager, isItem, stack.overlay()) : null;
        if (overlay != null) flattened &= overlay.linesUpWith(base);
        return new Layout(flattened, List.copyOf(layers), overlay != null ? stack.overlay() : null);
    }

    private Usable usable(IResourceManager manager, boolean isItem, String path) {
        Texture texture;
        try {
            texture = get(manager, location(isItem, path));
        }
        catch (IOException e) {
            return null;
        }
        PaletteAnimation.Track track = PaletteAnimation.Track.of(texture.width(), texture.height(),
            texture.animation());
        return track != null ? new Usable(texture, track) : null;
    }

    private static Texture read(IResource resource) throws IOException {
        BufferedImage image;
        try (InputStream stream = resource.getInputStream()) {
            image = ImageIO.read(stream);
        }
        if (image == null) throw new IOException("no image reader accepted the file");
        int width = image.getWidth();
        int height = image.getHeight();
        int[] argb = PaletteBaker.argb(image);
        return new Texture(width, height, argb, PaletteBaker.index(argb, width, height),
            (AnimationMetadataSection) resource.getMetadata("animation"));
    }
}
