package com.ruling_0.materiallib.api;

import java.util.function.Predicate;

import net.minecraft.util.ResourceLocation;

import com.ruling_0.materiallib.MaterialLib;

/// A material's palette: the png `assets/<modid>/textures/palettes/<name>.png`, named after the material and found
/// under the modid of one of its declarations ([#find]).
///
/// A material whose palette png exists has its shape art baked rather than tinted. When the texture atlas stitches, the
/// base texture and its `_LAYER<n>`s are recolored through the palette, and the `_OVERLAY` is drawn uncolored above
/// them ([PaletteSprites#bake]). [StandardProperties#TINT] and its siblings still apply to fluids and external
/// consumers, but not to the material's baked shape art. A material with no palette png keeps the tint path.
///
/// A palette png pairs an index pixel in its first column with a color in its second, one row per entry, running top to
/// bottom. Baking replaces every art pixel whose gray level, its brightest channel, matches a row's index pixel with
/// that row's color. Noise in the two dimmer channels does not change a level. A level that no row indexes takes the
/// last entry. Alpha is kept from the base art and layers.
///
/// The first row indexing a level wins it. A row whose index pixel is fully transparent indexes nothing but is still an
/// entry, so it can serve as the last entry. Trailing rows whose color is fully transparent are not entries, and
/// columns past the second are ignored.
///
/// Only texture set art bakes. Overrides under `mloverrides/`, [IconPather] paths, and fluid still/flowing textures
/// are used without coloring. A palette png that cannot be read, or that holds no entries, leaves the material on the
/// tint path. A layer that cannot be read, or that vanilla would refuse as a sprite, is left out.
///
/// A resource pack recolors a material by shipping or replacing its palette png.
public record PaletteRef(String modid, String name) {

    public PaletteRef {
        Names.validate("palette modid", modid);
        Names.validate("palette name", name);
    }

    /// The palette of `material`: the first png accepted by `exists`, looked up under the owner's modid and then under
    /// each unification alternative's modid in [ShapeIcons#resolvePath]'s order. Null when no png exists.
    public static PaletteRef find(Material material, Predicate<ResourceLocation> exists) {
        PaletteRef owner = new PaletteRef(material.getModId(), material.getName());
        if (exists.test(owner.png())) return owner;
        for (Material alternative : material.getAlternatives()) {
            PaletteRef palette = new PaletteRef(alternative.getModIdIgnoreCanonical(), material.getName());
            if (exists.test(palette.png())) return palette;
        }
        return null;
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
