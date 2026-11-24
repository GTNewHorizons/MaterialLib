package com.ruling_0.materiallib.api;

import java.awt.color.ColorSpace;
import java.awt.image.BufferedImage;
import java.awt.image.ColorModel;
import java.awt.image.DataBuffer;
import java.awt.image.DataBufferByte;
import java.awt.image.DataBufferInt;
import java.awt.image.DirectColorModel;
import java.awt.image.IndexColorModel;
import java.awt.image.Raster;
import java.awt.image.WritableRaster;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import javax.imageio.ImageIO;

import net.minecraft.client.resources.IResource;
import net.minecraft.client.resources.IResourceManager;
import net.minecraft.client.resources.data.AnimationMetadataSection;
import net.minecraft.util.ResourceLocation;

import com.gtnewhorizon.gtnhlib.util.ResourceUtil;
import com.ruling_0.materiallib.MaterialLib;

import cpw.mods.fml.relauncher.Side;
import cpw.mods.fml.relauncher.SideOnly;

/// Shape textures decoded once per stitch and shared by every [PaletteAtlasSprite] that bakes from them, together with
/// the [Layout] of each shape's stack. Safe for concurrent use. Callers must not write to the arrays handed out.
///
/// A stack flattens into one sprite only when every texture in it has the base's size and animation timeline. Any other
/// stack draws in passes, like the tint path.
@SideOnly(Side.CLIENT)
final class PaletteArt {

    /// A texture's pixels, their HSV values, and its animation metadata, if any.
    record Texture(int width, int height, int[] argb, PaletteBaker.IndexedImage indexed,
                   AnimationMetadataSection animation) {}

    /// Whether a stack bakes into one sprite, the numbered layer paths kept, in draw order, and the overlay path when
    /// the overlay is usable, else null.
    record Layout(boolean flattened, List<String> layers, String overlay) {}

    /// A texture acceptable to vanilla as a sprite, with animation frames.
    private record Usable(Texture texture, PaletteAnimation.Track track) {

        boolean linesUpWith(Usable base) {
            return texture.width() == base.texture.width() && texture.height() == base.texture.height() &&
                PaletteAnimation.sameTimeline(track, base.track);
        }
    }

    private final Map<ResourceLocation, Texture> textures = new ConcurrentHashMap<>();
    private final Map<ResourceLocation, IOException> failures = new ConcurrentHashMap<>();
    private final Map<String, Layout> layouts = new ConcurrentHashMap<>();

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

    /// How `stack` binds on the item atlas when `isItem`, the block atlas otherwise. A numbered layer or overlay that
    /// cannot be read, or that vanilla would refuse as a sprite, is left out. Such a base yields a flattened layout
    /// with no layers and no overlay.
    Layout layout(IResourceManager manager, boolean isItem, ShapeIcons.StackPaths stack) {
        String key = (isItem ? "items:" : "blocks:") + stack.base();
        return layouts.computeIfAbsent(key, ignored -> findLayout(manager, isItem, stack));
    }

    /// The complete location of the texture at icon path `path` on the item atlas when `isItem`, the block atlas
    /// otherwise.
    static ResourceLocation location(boolean isItem, String path) {
        if (isItem) return ResourceUtil.getCompleteItemTextureResourceLocation(path);
        else return ResourceUtil.getCompleteBlockTextureResourceLocation(path);
    }

    private Layout findLayout(IResourceManager manager, boolean isItem, ShapeIcons.StackPaths stack) {
        Usable base = usable(manager, isItem, stack.base());
        if (base == null) return new Layout(true, List.of(), null);
        boolean flattened = true;
        List<String> layers = new ArrayList<>();
        for (String path : stack.layers()) {
            Usable layer = usable(manager, isItem, path);
            if (layer == null) {
                MaterialLib.LOG.warn(
                    "Leaving {} out of {}: unreadable, or neither a square texture nor an animation strip", path,
                    stack.base());
                continue;
            }
            layers.add(path);
            flattened &= layer.linesUpWith(base);
        }
        Usable overlay = stack.overlay() != null ? usable(manager, isItem, stack.overlay()) : null;
        if (overlay != null) flattened &= overlay.linesUpWith(base);
        return new Layout(flattened, List.copyOf(layers), overlay != null ? stack.overlay() : null);
    }

    private Usable usable(IResourceManager manager, boolean isItem, String path) {
        Texture texture;
        try {
            texture = get(manager, location(isItem, path));
        }
        catch (IOException e) {
            return null;
        }
        PaletteAnimation.Track track = PaletteAnimation.Track.of(texture.width(), texture.height(),
            texture.animation());
        return track != null ? new Usable(texture, track) : null;
    }

    private static Texture read(IResource resource) throws IOException {
        BufferedImage image;
        try (InputStream stream = resource.getInputStream()) {
            image = ImageIO.read(stream);
        }
        if (image == null) throw new IOException("no image reader accepted the file");
        int width = image.getWidth();
        int height = image.getHeight();
        int[] argb = argb(image);
        return new Texture(width, height, argb, PaletteBaker.index(argb, width, height),
            (AnimationMetadataSection) resource.getMetadata("animation"));
    }

    /// The pixels of `image` as ARGB, row by row. Gray samples are read as stored, keeping the high byte of a 16-bit
    /// sample.
    static int[] argb(BufferedImage image) {
        int width = image.getWidth();
        int height = image.getHeight();
        int count = width * height;
        ColorModel model = image.getColorModel();
        if (model.getColorSpace().getType() == ColorSpace.TYPE_GRAY && model.getComponentSize(0) >= 8) {
            return gray(image.getRaster(), model.getComponentSize(0) - 8, count);
        }
        DataBuffer buffer = image.getRaster().getDataBuffer();
        if (image.getRaster().getParent() == null && buffer instanceof DataBufferByte bytes &&
            bytes.getNumBanks() == 1 && bytes.getOffset() == 0) {
            byte[] data = bytes.getData();
            int[] argb = new int[count];
            switch (image.getType()) {
                case BufferedImage.TYPE_4BYTE_ABGR -> {
                    if (data.length != count * 4) break;
                    for (int i = 0, j = 0; i < count; i++, j += 4) {
                        argb[i] = (data[j] & 0xFF) << 24 | (data[j + 3] & 0xFF) << 16 | (data[j + 2] & 0xFF) << 8 |
                            data[j + 1] & 0xFF;
                    }
                    return argb;
                }
                case BufferedImage.TYPE_3BYTE_BGR -> {
                    if (data.length != count * 3) break;
                    for (int i = 0, j = 0; i < count; i++, j += 3) {
                        argb[i] = 0xFF000000 | (data[j + 2] & 0xFF) << 16 | (data[j + 1] & 0xFF) << 8 | data[j] & 0xFF;
                    }
                    return argb;
                }
                case BufferedImage.TYPE_BYTE_INDEXED -> {
                    if (data.length != count) break;
                    int[] colors = new int[256];
                    ((IndexColorModel) image.getColorModel()).getRGBs(colors);
                    for (int i = 0; i < count; i++) {
                        argb[i] = colors[data[i] & 0xFF];
                    }
                    return argb;
                }
                default -> {}
            }
        }
        return image.getRGB(0, 0, width, height, null, 0, width);
    }

    private static int[] gray(Raster raster, int shift, int count) {
        int bands = raster.getNumBands();
        int[] samples = raster.getPixels(0, 0, raster.getWidth(), raster.getHeight(), (int[]) null);
        int[] argb = new int[count];
        for (int i = 0, j = 0; i < count; i++, j += bands) {
            int gray = samples[j] >> shift;
            int alpha = bands > 1 ? samples[j + 1] >> shift : 0xFF;
            argb[i] = alpha << 24 | gray * 0x010101;
        }
        return argb;
    }

    /// An ARGB image drawn straight from `pixels`, row by row, without copying them.
    static BufferedImage image(int[] pixels, int width, int height) {
        DirectColorModel model = (DirectColorModel) ColorModel.getRGBdefault();
        WritableRaster raster = Raster.createPackedRaster(new DataBufferInt(pixels, pixels.length), width, height,
            width, model.getMasks(), null);
        return new BufferedImage(model, raster, false, null);
    }
}
