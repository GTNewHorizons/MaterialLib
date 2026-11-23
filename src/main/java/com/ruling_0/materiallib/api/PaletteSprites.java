package com.ruling_0.materiallib.api;

import java.awt.image.BufferedImage;
import java.io.IOException;
import java.io.InputStream;
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

/// Reads the palette pngs behind [PaletteRef]s and puts sprites baked against them on an atlas through [#bake].
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

    /// The atlas sprite baking `stack`'s art through `material`'s palette, or null when `register` is not a texture
    /// atlas or the material has no readable palette png. A sprite already on the atlas under
    /// [PaletteRef#bakedIconName] is reused; a new one is inserted with `setTextureEntry`, which keeps the atlas from
    /// reading a file under that name.
    static IIcon bake(IIconRegister register, boolean isItem, ShapeIcons.StackPaths stack, Material material) {
        if (!(register instanceof TextureMap map)) return null;
        PaletteRef palette = PaletteRef.of(material);
        PaletteBaker.Palette colors = palette(palette);
        if (colors == null) return null;
        String name = palette.bakedIconName(stack.base());
        TextureAtlasSprite bound = map.getTextureExtry(name);
        if (bound != null) return bound;
        PaletteAtlasSprite sprite = new PaletteAtlasSprite(name, isItem, stack.base(), stack.layers(), stack.overlay(),
            colors);
        map.setTextureEntry(name, sprite);
        return sprite;
    }

    /// The art read for the sprites baking during the current stitch.
    static PaletteArt art() {
        return art;
    }

    /// The palette read from `palette`'s png, or null when the png is missing, unreadable or holds no palette.
    static PaletteBaker.Palette palette(PaletteRef palette) {
        return PALETTES.computeIfAbsent(palette, PaletteSprites::read).orElse(null);
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
