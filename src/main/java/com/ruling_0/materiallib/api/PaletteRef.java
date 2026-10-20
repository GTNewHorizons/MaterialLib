package com.ruling_0.materiallib.api;

import net.minecraft.util.ResourceLocation;

import com.ruling_0.materiallib.MaterialLib;

/// A material's palette: the png `assets/<modid>/textures/palettes/<name>.png`, named after the material's modid and
/// name ([#of]).
///
/// A material whose palette png exists has its shape art baked rather than tinted. The base texture and its
/// `_LAYER<n>`s are recolored through the palette when the texture atlas stitches, then flattened with the uncolored
/// `_OVERLAY` into a single sprite. [StandardProperties#TINT] and its siblings stay settable and readable -- fluids
/// and external consumers still apply them -- but they do not color the material's baked shape art. A material with
/// no palette png keeps the tint path.
///
/// A palette png holds its entries in its first column, running top to bottom. Trailing fully-transparent pixels
/// are ignored; interior transparent entries are not. No maximum height is assumed. Baking sorts the base texture's
/// distinct shades (by HSV value, the brightest channel, lightest first) and maps them onto the entries in that
/// order, clamping to the last entry where the art carries more shades than the palette. Alpha is kept from the base
/// art and layers.
///
/// Only texture set art bakes. Overrides under `mloverrides/`, [IconPather] paths, and fluid still/flowing textures
/// are used without coloring. A palette png that cannot be read, or whose first column is fully transparent, leaves
/// the material on the tint path. A layer that cannot be read, or whose size differs from the base, is left out of
/// the sprite.
///
/// A resource pack recolors a material by shipping or replacing its palette png.
public record PaletteRef(String modid, String name) {

    public PaletteRef {
        Names.validate("palette modid", modid);
        Names.validate("palette name", name);
    }

    /// The palette `material` draws through, named after its modid and name.
    public static PaletteRef of(Material material) {
        return new PaletteRef(material.getModId(), material.getName());
    }

    /// The location of the palette png.
    public ResourceLocation png() {
        return new ResourceLocation(modid, "textures/palettes/" + name + ".png");
    }

    /// The atlas name of the sprite baked from the art at `resolvedBasePath` through this palette; materials
    /// sharing both share the sprite. `mlbaked/` should not match a file on disk, so the name always reaches the
    /// baking loader.
    public String bakedIconName(String resolvedBasePath) {
        return MaterialLib.MODID + ":mlbaked/" + resolvedBasePath.replace(':', '/') + "/" + modid + "/" + name;
    }
}
