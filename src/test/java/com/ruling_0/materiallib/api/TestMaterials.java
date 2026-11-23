package com.ruling_0.materiallib.api;

import java.util.List;
import java.util.Map;
import java.util.Set;

/// Material fixtures for tests: a material resolved in a registry of its own, or one declared straight into a registry
/// with a property set [MaterialBuilder] refuses, such as one without a texture set.
final class TestMaterials {

    private TestMaterials() {}

    /// A material named `name` declared by `modid` alone, in a registry of its own that has resolved.
    static Material resolved(String modid, String name) {
        MaterialRegistry registry = new MaterialRegistry();
        Material material = registry.newMaterial(modid, name, TextureSet.of(modid, "plain")).build();
        registry.resolve();
        return material;
    }

    /// Declares `name` for `modid` with exactly `properties` and no shapes, families, tooltip or overrides.
    static Material declare(MaterialRegistry registry, String modid, String name, Map<Property<?>, Object> properties) {
        return registry.declare(name, new MaterialDeclaration(modid, properties, Set.of(), Set.of(), List.of(),
            List.of(), Map.of()));
    }
}
