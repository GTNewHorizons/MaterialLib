package com.ruling_0.materiallib.api;

import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.UnaryOperator;

import it.unimi.dsi.fastutil.objects.Reference2ObjectLinkedOpenHashMap;
import it.unimi.dsi.fastutil.objects.ReferenceLinkedOpenHashSet;

/// One mod's contribution to a shared [Material], as its [MaterialBuilder] declared it. Declarations stay apart after
/// the material unifies. Which [ShapeOverride] survives depends on which declaration generates the shape. Icons fall
/// back to the other declarations' texture sets.
record MaterialDeclaration(String modid, Map<Property<?>, Object> properties, Set<Shape> shapes,
                           Set<Shape> removedShapes, List<String[]> familyKeys, List<String> tooltipLines,
                           Map<Shape, ShapeOverride> overrides) {

    MaterialDeclaration {
        properties = new Reference2ObjectLinkedOpenHashMap<>(properties);
        shapes = new ReferenceLinkedOpenHashSet<>(shapes);
        removedShapes = new ReferenceLinkedOpenHashSet<>(removedShapes);
        familyKeys = List.copyOf(familyKeys);
        tooltipLines = List.copyOf(tooltipLines);
        overrides = new Reference2ObjectLinkedOpenHashMap<>(overrides);
    }

    /// The value this declaration sets for `property`, or null.
    @SuppressWarnings("unchecked")
    <T> T property(Property<T> property) {
        return (T) properties.get(property);
    }

    /// Whether this declaration's own shapes or families generate `canonicalShape`, without it removing the shape.
    /// `canonical` maps a declared shape onto its election winner.
    boolean generates(Shape canonicalShape, UnaryOperator<Shape> canonical, MaterialRegistry registry) {
        for (Shape removed : removedShapes) {
            if (canonical.apply(removed) == canonicalShape) return false;
        }
        for (Shape shape : shapes) {
            if (canonical.apply(shape) == canonicalShape) return true;
        }
        for (String[] familyKey : familyKeys) {
            Family family = registry.getFamily(familyKey[0], familyKey[1]);
            if (family == null) continue;
            for (Shape shape : family.getShapesInternal()) {
                if (canonical.apply(shape) == canonicalShape) return true;
            }
        }
        return false;
    }

    /// The override this declaration names for `canonicalShape`, or null.
    ShapeOverride overrideFor(Shape canonicalShape, UnaryOperator<Shape> canonical) {
        for (Map.Entry<Shape, ShapeOverride> entry : overrides.entrySet()) {
            if (canonical.apply(entry.getKey()) == canonicalShape) return entry.getValue();
        }
        return null;
    }
}
