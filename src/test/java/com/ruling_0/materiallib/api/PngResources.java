package com.ruling_0.materiallib.api;

import java.awt.image.BufferedImage;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import javax.imageio.ImageIO;

import net.minecraft.client.resources.IResource;
import net.minecraft.client.resources.IResourceManager;
import net.minecraft.client.resources.data.AnimationFrame;
import net.minecraft.client.resources.data.AnimationMetadataSection;
import net.minecraft.client.resources.data.IMetadataSection;
import net.minecraft.util.ResourceLocation;

/// Serves pngs and their animation metadata from memory in place of the client's resource packs.
final class PngResources implements IResourceManager {

    private final Map<ResourceLocation, byte[]> pngs = new HashMap<>();
    private final Map<ResourceLocation, AnimationMetadataSection> animations = new HashMap<>();

    void put(ResourceLocation location, int width, int height, int[] argb,
             AnimationMetadataSection animation) throws IOException {
        BufferedImage image = new BufferedImage(width, height, BufferedImage.TYPE_INT_ARGB);
        image.setRGB(0, 0, width, height, argb, 0, width);
        ByteArrayOutputStream bytes = new ByteArrayOutputStream();
        ImageIO.write(image, "png", bytes);
        pngs.put(location, bytes.toByteArray());
        if (animation != null) animations.put(location, animation);
    }

    boolean has(ResourceLocation location) {
        return pngs.containsKey(location);
    }

    @Override
    public Set<String> getResourceDomains() { return Set.of("testmod"); }

    @Override
    public IResource getResource(ResourceLocation location) throws IOException {
        byte[] png = pngs.get(location);
        if (png == null) throw new FileNotFoundException(location.toString());
        AnimationMetadataSection animation = animations.get(location);
        return new IResource() {

            @Override
            public InputStream getInputStream() { return new ByteArrayInputStream(png); }

            @Override
            public boolean hasMetadata() {
                return animation != null;
            }

            @Override
            public IMetadataSection getMetadata(String name) {
                return animation;
            }
        };
    }

    @Override
    public List<IResource> getAllResources(ResourceLocation location) throws IOException {
        return List.of(getResource(location));
    }

    /// Parsed `.mcmeta` animation metadata with `frametime` and, when `frames` is non-empty, that frame list.
    static AnimationMetadataSection animation(int frametime, AnimationFrame... frames) {
        return new AnimationMetadataSection(List.of(frames), -1, -1, frametime);
    }

    /// The complete item texture location that GTNHLib derives from a `domain:path` texture key.
    static ResourceLocation itemPng(String key) {
        int colon = key.indexOf(':');
        return new ResourceLocation(key.substring(0, colon), "textures/items/" + key.substring(colon + 1) + ".png");
    }
}
