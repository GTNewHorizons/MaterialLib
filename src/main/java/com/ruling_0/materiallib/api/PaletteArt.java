package com.ruling_0.materiallib.api;

import java.awt.image.BufferedImage;
import java.io.IOException;
import java.io.InputStream;
import java.util.HashMap;
import java.util.Map;

import javax.imageio.ImageIO;

import net.minecraft.client.resources.IResource;
import net.minecraft.client.resources.IResourceManager;
import net.minecraft.client.resources.data.AnimationMetadataSection;
import net.minecraft.util.ResourceLocation;

import cpw.mods.fml.relauncher.Side;
import cpw.mods.fml.relauncher.SideOnly;

/// The textures [PaletteAtlasSprite]s bake from, each decoded and ranked once.
///
/// Every material baking a shape draws on that shape's base texture, layers and overlay, so the sprites of one
/// stitch share a single read of each file instead of decoding it once per material. The arrays handed out are
/// shared and must not be written to.
@SideOnly(Side.CLIENT)
final class PaletteArt {

    /// A texture's pixels, the palette ranks of its shades, and its animation metadata, if any.
    record Texture(int width, int height, int[] argb, PaletteBaker.IndexedImage indexed,
                   AnimationMetadataSection animation) {}

    private final Map<ResourceLocation, Texture> textures = new HashMap<>();
    private final Map<ResourceLocation, IOException> failures = new HashMap<>();

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

    private static Texture read(IResource resource) throws IOException {
        BufferedImage image;
        try (InputStream stream = resource.getInputStream()) {
            image = ImageIO.read(stream);
        }
        if (image == null) throw new IOException("no image reader accepted the file");
        int width = image.getWidth();
        int height = image.getHeight();
        int[] argb = image.getRGB(0, 0, width, height, null, 0, width);
        return new Texture(width, height, argb, PaletteBaker.index(argb, width, height),
            (AnimationMetadataSection) resource.getMetadata("animation"));
    }
}
