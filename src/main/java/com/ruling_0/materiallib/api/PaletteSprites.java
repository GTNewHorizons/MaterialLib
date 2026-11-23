package com.ruling_0.materiallib.api;

import java.awt.image.BufferedImage;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;

import javax.imageio.ImageIO;

import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.texture.IIconRegister;
import net.minecraft.client.renderer.texture.TextureAtlasSprite;
import net.minecraft.client.renderer.texture.TextureMap;
import net.minecraft.util.IIcon;

import net.minecraftforge.client.event.TextureStitchEvent;

import com.gtnewhorizon.gtnhlib.util.ResourceUtil;
import com.ruling_0.materiallib.MaterialLib;

import cpw.mods.fml.common.eventhandler.SubscribeEvent;
import cpw.mods.fml.relauncher.Side;
import cpw.mods.fml.relauncher.SideOnly;

/// Reads the palette pngs behind [PaletteRef]s and puts sprites baked against them on an atlas through [#bake]: one
/// flattened sprite per material and shape, or one per pass when the shape's textures differ in size or animation
/// timing ([PaletteArt#layout]).
///
/// A palette png is read the first time a shape of its material binds on an atlas, and dropped when that atlas
/// finishes stitching, as is the art its sprites baked from. Shapes bind in `registerIcons`, which Forge runs before
/// `TextureStitchEvent.Pre`, so both are dropped on `TextureStitchEvent.Post`.
@SideOnly(Side.CLIENT)
public final class PaletteSprites {

    private static final Map<PaletteRef, Optional<PaletteBaker.Palette>> PALETTES = new ConcurrentHashMap<>();

    private static PaletteArt art = new PaletteArt();

    @SubscribeEvent
    public void onTextureStitched(TextureStitchEvent.Post event) {
        PALETTES.clear();
        art = new PaletteArt();
    }

    /// The icons `stack`'s art binds for `material` in draw order, or null when `register` is not a texture atlas or
    /// the material has no readable palette. A flattened stack binds one sprite. A stack drawn in passes binds a sprite
    /// for its base and each numbered layer, then the overlay registered as on the tint path. A sprite already on the
    /// atlas under [PaletteRef#bakedIconName] is reused; a new one is inserted with `setTextureEntry`, which keeps the
    /// atlas from reading a file under that name.
    static IIcon[] bake(IIconRegister register, boolean isItem, ShapeIcons.StackPaths stack, Material material) {
        if (!(register instanceof TextureMap map)) return null;
        PaletteRef palette = PaletteRef.of(material);
        PaletteBaker.Palette colors = palette(palette);
        if (colors == null) return null;
        PaletteArt.Layout layout = art.layout(Minecraft.getMinecraft().getResourceManager(), isItem, stack);
        if (layout.flattened()) {
            return new IIcon[] {
                sprite(map, palette, colors, isItem, stack.base(), layout.layers(), layout.overlay()) };
        }
        List<IIcon> icons = new ArrayList<>();
        icons.add(sprite(map, palette, colors, isItem, stack.base(), List.of(), null));
        for (String layer : layout.layers()) {
            icons.add(sprite(map, palette, colors, isItem, layer, List.of(), null));
        }
        if (stack.overlay() != null) icons.add(register.registerIcon(stack.overlay()));
        return icons.toArray(new IIcon[0]);
    }

    /// The art read for the sprites baking during the current stitch.
    static PaletteArt art() {
        return art;
    }

    /// The palette read from `palette`'s png, or null when the png is missing, unreadable or holds no palette.
    static PaletteBaker.Palette palette(PaletteRef palette) {
        return PALETTES.computeIfAbsent(palette, PaletteSprites::read).orElse(null);
    }

    private static IIcon sprite(TextureMap map, PaletteRef palette, PaletteBaker.Palette colors, boolean isItem,
                                String path, List<String> layers, String overlay) {
        String name = palette.bakedIconName(path);
        TextureAtlasSprite bound = map.getTextureExtry(name);
        if (bound != null) return bound;
        PaletteAtlasSprite sprite = new PaletteAtlasSprite(name, isItem, path, layers, overlay, colors);
        map.setTextureEntry(name, sprite);
        return sprite;
    }

    private static Optional<PaletteBaker.Palette> read(PaletteRef palette) {
        if (!ResourceUtil.resourceExists(palette.png())) return Optional.empty();
        try (InputStream stream = Minecraft.getMinecraft().getResourceManager().getResource(palette.png())
            .getInputStream()) {
            BufferedImage image = ImageIO.read(stream);
            if (image == null) throw new IOException("no image reader accepted the file");
            PaletteBaker.Palette read = PaletteBaker.palette(PaletteBaker.argb(image), image.getWidth(),
                image.getHeight());
            if (read != null) return Optional.of(read);
            MaterialLib.LOG.warn("Palette {} needs an index column and a color column with at least one entry",
                palette.png());
        }
        catch (IOException e) {
            MaterialLib.LOG.warn("Could not read palette {}", palette.png(), e);
        }
        return Optional.empty();
    }
}
