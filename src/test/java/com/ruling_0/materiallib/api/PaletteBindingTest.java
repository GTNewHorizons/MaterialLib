package com.ruling_0.materiallib.api;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Predicate;

import net.minecraft.client.renderer.texture.IIconRegister;
import net.minecraft.util.IIcon;

import org.junit.jupiter.api.Test;

/// Pins which texture source [ShapeIcons] offers to the palette baker, the art it hands it, and the shape and color
/// of the stack a material ends up with whether or not it has a palette png. The baker is injected, so the whole
/// classification runs headless.
class PaletteBindingTest {

    private static final int TINT = 0xFFFFCC00;

    /// Accepts every path outside the resource-pack override root, so a test can run with that source missing.
    private static final Predicate<String> NO_OVERRIDE = path -> !path.startsWith(ShapeIcons.OVERRIDE_ROOT);

    /// Accepts no numbered layer sibling, bounding the layer probe of a test that otherwise accepts every path.
    private static final Predicate<String> NO_LAYERS = path -> !path.contains(ShapeIcons.LAYER_SUFFIX);

    private final MaterialRegistry registry = new MaterialRegistry();
    private final RecordingRegister register = new RecordingRegister();
    private final RecordingBinder binder = new RecordingBinder();
    private final TextureSet setA = TextureSet.of("testmod", "setA");

    /// Texture-set art bakes: the baker sees the whole discovered stack and its single icon replaces that stack,
    /// leaving no overlay layer and nothing for a tint to multiply.
    @Test
    void textureSetArtBakesIntoASingleUntintedLayer() {
        Material material = declareMaterial("Testiron");
        registry.resolve();
        String base = setA.iconPath("gear");
        String layer = base + ShapeIcons.LAYER_SUFFIX + 1;
        String overlay = base + ShapeIcons.OVERLAY_SUFFIX;
        int index = material.getIndex();

        ShapeIcons icons = new ShapeIcons(true, Set.of(base, layer, overlay)::contains, binder);
        icons.bind(register, new Material[] { material }, "gear");

        assertTrue(icons.isPaletteBaked(index));
        assertEquals(1, icons.layerCount(index));
        assertSame(register.registered.get(PaletteRef.of(material).bakedIconName(base)), icons.get(index));
        assertFalse(icons.isOverlayLayer(index, 0));
        assertEquals(0xFFFFFFFF, icons.layerColor(material, 0));

        assertEquals(1, binder.calls.size());
        assertEquals(new BakeCall(true, new ShapeIcons.StackPaths(base, List.of(layer), overlay), material),
            binder.calls.get(0));
    }

    /// A stack whose textures differ in size or timing bakes one icon per pass. The shared overlay stays the last
    /// layer and reports as the overlay, and every layer draws white.
    @Test
    void aStackBakedInPassesKeepsItsLayersAndOverlay() {
        Material material = declareMaterial("Testiron");
        registry.resolve();
        String base = setA.iconPath("gear");
        String layer = base + ShapeIcons.LAYER_SUFFIX + 1;
        String overlay = base + ShapeIcons.OVERLAY_SUFFIX;
        int index = material.getIndex();
        binder.passes = true;

        ShapeIcons icons = new ShapeIcons(true, Set.of(base, layer, overlay)::contains, binder);
        icons.bind(register, new Material[] { material }, "gear");

        assertTrue(icons.isPaletteBaked(index));
        assertEquals(3, icons.layerCount(index));
        assertTrue(icons.isOverlayLayer(index, 2));
        assertFalse(icons.isOverlayLayer(index, 1));
        assertSame(register.registered.get(overlay), icons.layer(index, 2));
        for (int l = 0; l < 3; l++) {
            assertEquals(0xFFFFFFFF, icons.layerColor(material, l));
        }
    }

    /// A resource-pack override outranks a palette and never reaches the baker, so its art draws as authored.
    @Test
    void aResourcePackOverrideOutranksThePalette() {
        Material material = declareMaterial("Testiron");
        registry.resolve();
        int index = material.getIndex();

        ShapeIcons icons = new ShapeIcons(true, NO_LAYERS, binder);
        icons.bind(register, new Material[] { material }, "gear");

        assertTrue(icons.isOverride(index));
        assertFalse(icons.isPaletteBaked(index));
        assertTrue(binder.calls.isEmpty());
        assertSame(register.registered.get(ShapeIcons.overridePath(material, "gear")), icons.get(index));
    }

    /// A per-material icon path outranks a palette, never reaches the baker, and keeps the material's tint.
    @Test
    void aPerMaterialIconPathOutranksThePalette() {
        Material material = declareMaterial("Testiron");
        registry.resolve();
        String perMaterial = "testmod:custom/iron_gear";
        int index = material.getIndex();

        ShapeIcons icons = new ShapeIcons(true, NO_OVERRIDE.and(NO_LAYERS), binder);
        icons.bind(register, new Material[] { material }, List.of("gear"), ignored -> perMaterial);

        assertFalse(icons.isPaletteBaked(index));
        assertTrue(binder.calls.isEmpty());
        assertSame(register.registered.get(perMaterial), icons.get(index));
        assertEquals(TINT, icons.layerColor(material, 0));
    }

    /// A material without a palette png, which the baker declines, keeps the full tinted stack.
    @Test
    void aMaterialWithoutAPalettePngKeepsTheTintedStack() {
        Material material = declareMaterial("Testiron");
        registry.resolve();
        String base = setA.iconPath("gear");
        String layer = base + ShapeIcons.LAYER_SUFFIX + 1;
        String overlay = base + ShapeIcons.OVERLAY_SUFFIX;
        int index = material.getIndex();
        binder.bakes = false;

        ShapeIcons icons = new ShapeIcons(true, Set.of(base, layer, overlay)::contains, binder);
        icons.bind(register, new Material[] { material }, "gear");

        assertFalse(icons.isPaletteBaked(index));
        assertEquals(3, icons.layerCount(index));
        assertSame(register.registered.get(base), icons.layer(index, 0));
        assertSame(register.registered.get(layer), icons.layer(index, 1));
        assertSame(register.registered.get(overlay), icons.layer(index, 2));
        assertTrue(icons.isOverlayLayer(index, 2));
        assertEquals(TINT, icons.layerColor(material, 0));
    }

    /// A palette-disabled instance never offers art to a baker.
    @Test
    void aPaletteDisabledInstanceKeepsTheTintedStack() {
        Material material = declareMaterial("Testiron");
        registry.resolve();
        String base = setA.iconPath("gear");
        int index = material.getIndex();

        ShapeIcons icons = new ShapeIcons(true, Set.of(base)::contains);
        icons.bind(register, new Material[] { material }, "gear");

        assertFalse(icons.isPaletteBaked(index));
        assertSame(register.registered.get(base), icons.get(index));
        assertEquals(TINT, icons.layerColor(material, 0));
    }

    /// The deferred placeholder bind drops the baked mark along with the stacks it replaces.
    @Test
    void bindingThePlaceholderClearsTheBakedMark() {
        Material material = declareMaterial("Testiron");
        registry.resolve();
        String base = setA.iconPath("gear");
        int index = material.getIndex();

        ShapeIcons icons = new ShapeIcons(true, Set.of(base)::contains, binder);
        icons.bind(register, new Material[] { material }, "gear");
        assertTrue(icons.isPaletteBaked(index));

        icons.bindPlaceholder(register);

        assertFalse(icons.isPaletteBaked(index));
        assertSame(register.registered.get(ShapeIcons.EMPTY_ICON), icons.get(index));
    }

    private Material declareMaterial(String name) {
        return declare(name, Map.of(StandardProperties.TEXTURE_SET, setA, StandardProperties.TINT, TINT));
    }

    private Material declare(String name, Map<Property<?>, Object> properties) {
        Material material = new Material(registry, "testmod", name, properties, Set.of(), List.of());
        registry.register(material);
        return material;
    }

    private record BakeCall(boolean isItem, ShapeIcons.StackPaths stack, Material material) {}

    /// A [ShapeIcons.PaletteBinder] recording what it was handed, standing in for the client atlas. While `bakes`
    /// it hands back the icons a real bake would put on the atlas under [PaletteRef#bakedIconName], one flattened
    /// icon or, while `passes`, one per pass with the plain overlay last; otherwise it declines the way a missing
    /// palette png does.
    private static final class RecordingBinder implements ShapeIcons.PaletteBinder {

        private final List<BakeCall> calls = new ArrayList<>();
        private boolean bakes = true;
        private boolean passes;

        @Override
        public IIcon[] bake(IIconRegister register, boolean isItem, ShapeIcons.StackPaths stack, Material material) {
            calls.add(new BakeCall(isItem, stack, material));
            if (!bakes) return null;
            PaletteRef palette = PaletteRef.of(material);
            if (!passes) return new IIcon[] { register.registerIcon(palette.bakedIconName(stack.base())) };
            List<IIcon> icons = new ArrayList<>();
            icons.add(register.registerIcon(palette.bakedIconName(stack.base())));
            for (String layer : stack.layers()) {
                icons.add(register.registerIcon(palette.bakedIconName(layer)));
            }
            if (stack.overlay() != null) icons.add(register.registerIcon(stack.overlay()));
            return icons.toArray(new IIcon[0]);
        }
    }
}
