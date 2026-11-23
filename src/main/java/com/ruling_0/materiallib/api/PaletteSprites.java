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

/// Reads palette pngs and puts the sprites baked from them on texture atlases through [#bake].
///
/// Palette pngs and decoded textures are read on first use during a stitch and dropped on `TextureStitchEvent.Post`,
/// not `Pre`, because shapes bind in `registerIcons`, which Forge runs before `TextureStitchEvent.Pre`.
@SideOnly(Side.CLIENT)
public final class PaletteSprites {

    private static final Map<PaletteRef, Optional<PaletteBaker.Palette>> PALETTES = new ConcurrentHashMap<>();

    private static PaletteArt art = new PaletteArt();

    @SubscribeEvent
    public void onTextureStitched(TextureStitchEvent.Post event) {
        PALETTES.clear();
        art = new PaletteArt();
    }

    /// The icons that `material` binds for `stack`'s art, in draw order, or null when `register` is not a texture atlas
    /// or the material has no readable palette. A flattened stack binds one sprite. A stack drawn in passes binds a
    /// sprite for its base and each numbered layer, then the overlay registered as on the tint path
    /// ([PaletteArt#layout]). A sprite already on the atlas under its [PaletteRef#bakedIconName] is reused.
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

    /// The textures decoded for the sprites baking in the current stitch.
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
