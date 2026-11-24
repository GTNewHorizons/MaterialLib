package com.ruling_0.materiallib.api;

import java.awt.image.BufferedImage;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Predicate;

import javax.imageio.ImageIO;

import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.texture.IIconRegister;
import net.minecraft.client.renderer.texture.TextureAtlasSprite;
import net.minecraft.client.renderer.texture.TextureMap;
import net.minecraft.client.resources.IResourceManager;
import net.minecraft.util.IIcon;
import net.minecraft.util.ResourceLocation;

import net.minecraftforge.client.event.TextureStitchEvent;

import com.gtnewhorizon.gtnhlib.util.ResourceUtil;
import com.ruling_0.materiallib.MaterialLib;

import cpw.mods.fml.common.eventhandler.SubscribeEvent;
import cpw.mods.fml.relauncher.Side;
import cpw.mods.fml.relauncher.SideOnly;

/// Reads palette pngs and puts the sprites baked from them on texture atlases through [#bake].
///
/// Palette pngs and decoded textures are read on first use during a stitch and dropped on `TextureStitchEvent.Post`,
/// not `Pre`, because shapes bind in `registerIcons`, which Forge runs before `TextureStitchEvent.Pre`.
@SideOnly(Side.CLIENT)
public final class PaletteSprites {

    /// A material's palette and the png it was read from.
    private record MaterialPalette(PaletteRef ref, PaletteBaker.Palette colors) {}

    private static final Map<Material, Optional<MaterialPalette>> PALETTES = new ConcurrentHashMap<>();

    private static PaletteArt art = new PaletteArt();

    @SubscribeEvent
    public void onTextureStitched(TextureStitchEvent.Post event) {
        PALETTES.clear();
        art = new PaletteArt();
    }

    /// The icons that `material` binds for `stack`'s art, in draw order, or null when `register` is not a texture atlas
    /// or the material has no readable palette. A flattened stack binds one sprite. A stack drawn in passes binds a
    /// sprite for its base and each numbered layer, then the overlay registered as on the tint path. Layers and an
    /// overlay that [PaletteArt#layout] leaves out are not bound. A sprite already on the atlas under its
    /// [PaletteRef#bakedIconName] is reused.
    static ShapeIcons.PaletteIcons bake(IIconRegister register, boolean isItem, ShapeIcons.StackPaths stack,
                                        Material material) {
        return bake(register, Minecraft.getMinecraft().getResourceManager(), ResourceUtil::resourceExists, isItem,
            stack, material);
    }

    /// As [#bake(IIconRegister, boolean, ShapeIcons.StackPaths, Material)], reading textures and palette pngs through
    /// `manager`, with `exists` deciding which palette pngs exist.
    static ShapeIcons.PaletteIcons bake(IIconRegister register, IResourceManager manager,
                                        Predicate<ResourceLocation> exists, boolean isItem,
                                        ShapeIcons.StackPaths stack, Material material) {
        if (!(register instanceof TextureMap map)) return null;
        MaterialPalette palette = PALETTES.computeIfAbsent(material, m -> read(m, manager, exists)).orElse(null);
        if (palette == null) return null;
        PaletteArt.Layout layout = art.layout(manager, isItem, stack);
        if (layout.flattened()) {
            IIcon sprite = sprite(map, palette, isItem, stack.base(), layout.layers(), layout.overlay());
            return new ShapeIcons.PaletteIcons(new IIcon[] { sprite }, false);
        }
        List<IIcon> icons = new ArrayList<>();
        icons.add(sprite(map, palette, isItem, stack.base(), List.of(), null));
        for (String layer : layout.layers()) {
            icons.add(sprite(map, palette, isItem, layer, List.of(), null));
        }
        String overlay = layout.overlay();
        if (overlay != null) icons.add(register.registerIcon(overlay));
        return new ShapeIcons.PaletteIcons(icons.toArray(new IIcon[0]), overlay != null);
    }

    /// The textures decoded for the sprites baking in the current stitch.
    static PaletteArt art() {
        return art;
    }

    private static IIcon sprite(TextureMap map, MaterialPalette palette, boolean isItem, String path,
                                List<String> layers, String overlay) {
        String name = palette.ref().bakedIconName(path);
        TextureAtlasSprite bound = map.getTextureExtry(name);
        if (bound != null) return bound;
        PaletteAtlasSprite sprite = new PaletteAtlasSprite(name, isItem, path, layers, overlay, palette.colors());
        map.setTextureEntry(name, sprite);
        return sprite;
    }

    private static Optional<MaterialPalette> read(Material material, IResourceManager manager,
                                                  Predicate<ResourceLocation> exists) {
        PaletteRef palette = PaletteRef.find(material, exists);
        if (palette == null) return Optional.empty();
        try (InputStream stream = manager.getResource(palette.png()).getInputStream()) {
            BufferedImage image = ImageIO.read(stream);
            if (image == null) throw new IOException("no image reader accepted the file");
            PaletteBaker.Palette read = PaletteBaker.palette(PaletteArt.argb(image), image.getWidth(),
                image.getHeight());
            if (read != null) return Optional.of(new MaterialPalette(palette, read));
            MaterialLib.LOG.warn("Palette {} needs an index column and a color column with at least one entry",
                palette.png());
        }
        catch (IOException e) {
            MaterialLib.LOG.warn("Could not read palette {}", palette.png(), e);
        }
        return Optional.empty();
    }
}
