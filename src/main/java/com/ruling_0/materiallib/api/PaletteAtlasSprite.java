package com.ruling_0.materiallib.api;

import java.awt.image.BufferedImage;
import java.io.IOException;
import java.util.List;

import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.texture.TextureAtlasSprite;
import net.minecraft.client.resources.IResourceManager;
import net.minecraft.client.resources.data.AnimationMetadataSection;
import net.minecraft.util.ResourceLocation;

import com.ruling_0.materiallib.MaterialLib;

import cpw.mods.fml.relauncher.Side;
import cpw.mods.fml.relauncher.SideOnly;

/// An atlas sprite baked through a [PaletteRef], from one texture or from a flattened stack of a base, its numbered
/// layers and an uncolored overlay.
///
/// Baking runs in Forge's custom-loader hook in place of the atlas reading a file, so the sprite name only has to be
/// unique on the atlas. Animation follows the base texture's `.mcmeta`, and layers are drawn frame for frame.
///
/// A base texture that cannot be read, or a bake that throws, resolves as the missing texture.
@SideOnly(Side.CLIENT)
final class PaletteAtlasSprite extends TextureAtlasSprite {

    /// An image and its animation metadata, if any.
    record Art(BufferedImage image, AnimationMetadataSection animation) {}

    private final boolean isItem;
    private final String basePath;
    private final List<String> layerPaths;
    private final String overlayPath;
    private final PaletteBaker.Palette palette;

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

    /// The flattened strip with the base texture's animation, or null when the art cannot be read. Textures are read
    /// through `art`.
    Art bake(IResourceManager manager, PaletteArt art) {
        try {
            PaletteArt.Texture base = art.get(manager, PaletteArt.location(isItem, basePath));
            int[] pixels = PaletteBaker.bake(base.indexed(), palette);
            int unmatched = PaletteBaker.unmatched(base.indexed(), palette);
            for (String layerPath : layerPaths) {
                PaletteArt.Texture layer = art.get(manager, PaletteArt.location(isItem, layerPath));
                PaletteBaker.compositeOver(pixels, PaletteBaker.bake(layer.indexed(), palette));
                unmatched += PaletteBaker.unmatched(layer.indexed(), palette);
            }
            if (unmatched > 0) {
                MaterialLib.LOG.debug("{} gray levels of {}'s art match no index row of its palette", unmatched,
                    getIconName());
            }
            if (overlayPath != null) {
                PaletteBaker.compositeOver(pixels, art.get(manager, PaletteArt.location(isItem, overlayPath)).argb());
            }
            return new Art(PaletteArt.image(pixels, base.width(), base.height()), base.animation());
        }
        catch (IOException e) {
            MaterialLib.LOG.error("Could not read the art of {}", getIconName(), e);
            return null;
        }
    }

    /// The level array `loadSprite` takes for `composited`: the art in slot 0 and one null slot per mipmap level
    /// above it, similar to vanilla. `TextureUtil.generateMipmapData` fills one slot per level.
    static BufferedImage[] frameLadder(BufferedImage composited, int mipmapLevels) {
        BufferedImage[] levels = new BufferedImage[1 + mipmapLevels];
        levels[0] = composited;
        return levels;
    }
}
