package com.ruling_0.materiallib.api;

import java.awt.image.BufferedImage;
import java.io.IOException;
import java.util.List;

import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.texture.TextureAtlasSprite;
import net.minecraft.client.resources.IResourceManager;
import net.minecraft.client.resources.data.AnimationMetadataSection;
import net.minecraft.util.ResourceLocation;

import com.gtnewhorizon.gtnhlib.util.ResourceUtil;
import com.ruling_0.materiallib.MaterialLib;

import cpw.mods.fml.relauncher.Side;
import cpw.mods.fml.relauncher.SideOnly;

/// One atlas sprite baked from a shape's art through a [PaletteRef]: the base texture and its numbered layers
/// recolored through the palette, the overlay kept without coloring, all flattened in draw order into one
/// sprite.
///
/// Baking happens inside Forge's custom-loader hook, which replaces the atlas's own read of a file named after
/// the sprite. The name therefore only has to be unique on the atlas, and the art comes from the paths passed to
/// the constructor. Animation follows the base texture: every layer is a strip of the base's exact size, drawn
/// frame for frame, and only the base's animation metadata is read.
///
/// A base texture that cannot be read, or a bake that throws, resolves as the missing texture. A layer that cannot
/// be read, or of any other size, is left out. Each of these failures is logged.
@SideOnly(Side.CLIENT)
final class PaletteAtlasSprite extends TextureAtlasSprite {

    /// An image and its animation metadata, if any.
    record Art(BufferedImage image, AnimationMetadataSection animation) {}

    private final boolean isItem;
    private final String basePath;
    private final List<String> layerPaths;
    private final String overlayPath;
    private final PaletteBaker.Palette palette;
    private boolean warnedLayer;

    /// A sprite named `name` baking the art at `basePath`, its numbered layers at `layerPaths` and, when
    /// `overlayPath` is non-null, its overlay, through `palette`. Every path names an item texture when
    /// `isItem`, a block texture otherwise.
    PaletteAtlasSprite(String name, boolean isItem, String basePath, List<String> layerPaths, String overlayPath,
                       PaletteBaker.Palette palette) {
        super(name);
        this.isItem = isItem;
        this.basePath = basePath;
        this.layerPaths = layerPaths;
        this.overlayPath = overlayPath;
        this.palette = palette;
    }

    @Override
    public boolean hasCustomLoader(IResourceManager manager, ResourceLocation location) {
        return true;
    }

    @Override
    public boolean load(IResourceManager manager, ResourceLocation location) {
        try {
            Art art = bake(manager, PaletteSprites.art());
            if (art == null) return true;
            loadSprite(frameLadder(art.image(), Minecraft.getMinecraft().gameSettings.mipmapLevels), art.animation(),
                !isItem && Minecraft.getMinecraft().gameSettings.anisotropicFiltering > 1);
            return false;
        }
        catch (RuntimeException e) {
            MaterialLib.LOG.error("Could not bake {}", getIconName(), e);
            return true;
        }
    }

    /// The flattened strip with the base texture's animation, or null when the base texture cannot be read. Art is
    /// read through `art`, which shares each texture among every sprite baking it.
    Art bake(IResourceManager manager, PaletteArt art) {
        PaletteArt.Texture base;
        try {
            base = art.get(manager, texturePath(basePath));
        }
        catch (IOException e) {
            MaterialLib.LOG.error("Could not read base texture {} of {}", basePath, getIconName(), e);
            return null;
        }
        int width = base.width();
        int height = base.height();
        int[] pixels = PaletteBaker.bake(base.indexed(), palette);
        int unmatched = PaletteBaker.unmatched(base.indexed(), palette);
        for (String layerPath : layerPaths) {
            PaletteArt.Texture layer = layer(manager, art, layerPath, width, height);
            if (layer == null) continue;
            PaletteBaker.compositeOver(pixels, PaletteBaker.bake(layer.indexed(), palette));
            unmatched += PaletteBaker.unmatched(layer.indexed(), palette);
        }
        if (unmatched > 0) {
            MaterialLib.LOG.debug("{} gray levels of {}'s art match no index row of its palette", unmatched,
                getIconName());
        }
        if (overlayPath != null) {
            PaletteArt.Texture overlay = layer(manager, art, overlayPath, width, height);
            if (overlay != null) PaletteBaker.compositeOver(pixels, overlay.argb());
        }
        return new Art(PaletteBaker.image(pixels, width, height), base.animation());
    }

    /// The level array `loadSprite` takes for `composited`: the art in slot 0 and one null slot per mipmap level
    /// above it, similar to vanilla. `TextureUtil.generateMipmapData` fills one slot per level.
    static BufferedImage[] frameLadder(BufferedImage composited, int mipmapLevels) {
        BufferedImage[] levels = new BufferedImage[1 + mipmapLevels];
        levels[0] = composited;
        return levels;
    }

    /// The art at `path`, or null when that art is unreadable or is not `width` x `height`.
    private PaletteArt.Texture layer(IResourceManager manager, PaletteArt art, String path, int width, int height) {
        PaletteArt.Texture layer;
        try {
            layer = art.get(manager, texturePath(path));
        }
        catch (IOException e) {
            warnLayer("Could not read layer {} of {}", path, getIconName(), e);
            return null;
        }
        if (layer.width() != width || layer.height() != height) {
            warnLayer("{} is {}x{} but the base of {} is {}x{}", path, layer.width(), layer.height(), getIconName(),
                width, height);
            return null;
        }
        return layer;
    }

    private ResourceLocation texturePath(String path) {
        if (isItem) return ResourceUtil.getCompleteItemTextureResourceLocation(path);
        else return ResourceUtil.getCompleteBlockTextureResourceLocation(path);
    }

    private void warnLayer(String message, Object... args) {
        if (warnedLayer) return;
        warnedLayer = true;
        MaterialLib.LOG.warn(message, args);
    }
}
