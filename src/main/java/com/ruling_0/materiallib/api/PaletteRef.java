package com.ruling_0.materiallib.api;

import java.util.function.Predicate;

import net.minecraft.util.ResourceLocation;

import com.ruling_0.materiallib.MaterialLib;

/// A material's palette png, `assets/<modid>/textures/palettes/<name>.png`, under the modid of one of the material's
/// declarations ([#find]). The README describes the palette format.
record PaletteRef(String modid, String name) {

    /// The palette of `material`: the first png accepted by `exists`, looked up under the owner's modid and then under
    /// each unification alternative's modid in [ShapeIcons#resolvePath]'s order. Null when no png exists.
    static PaletteRef find(Material material, Predicate<ResourceLocation> exists) {
        PaletteRef owner = new PaletteRef(material.getModId(), material.getName());
        if (exists.test(owner.png())) return owner;
        for (Material alternative : material.getAlternatives()) {
            PaletteRef palette = new PaletteRef(alternative.getModIdIgnoreCanonical(), material.getName());
            if (exists.test(palette.png())) return palette;
        }
        return null;
    }

    /// The location of the palette png.
    ResourceLocation png() {
        return new ResourceLocation(modid, "textures/palettes/" + name + ".png");
    }

    /// The atlas name of the sprite baked from the art at `resolvedBasePath` through this palette. `mlbaked/` should
    /// not match a file on disk, so the name always reaches the baking loader.
    String bakedIconName(String resolvedBasePath) {
        return MaterialLib.MODID + ":mlbaked/" + resolvedBasePath.replace(':', '/') + "/" + modid + "/" + name;
    }
}
