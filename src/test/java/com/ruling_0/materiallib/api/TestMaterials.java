package com.ruling_0.materiallib.api;

import java.util.List;
import java.util.Map;
import java.util.Set;

/// Material fixtures for tests: a material resolved in a registry of its own, or one declared straight into a registry
/// with a property set [MaterialBuilder] refuses, such as one without a texture set.
final class TestMaterials {

    private TestMaterials() {}

    /// A material named `name` declared by `modid` alone, in a registry of its own that has resolved. Its index is 0.
    static Material resolved(String modid, String name) {
        return resolvedAll(modid, name)[0];
    }

    /// One material per name, all declared by `modid` in one registry that has resolved, so each has its own index.
    static Material[] resolvedAll(String modid, String... names) {
        MaterialRegistry registry = new MaterialRegistry();
        Material[] materials = new Material[names.length];
        for (int i = 0; i < names.length; i++) {
            materials[i] = registry.newMaterial(modid, names[i], TextureSet.of(modid, "plain")).build();
        }
        registry.resolve();
        return materials;
    }

    /// Declares `name` for `modid` with exactly `properties` and no shapes, families, tooltip or overrides.
    static Material declare(MaterialRegistry registry, String modid, String name, Map<Property<?>, Object> properties) {
        return registry.declare(name, new MaterialDeclaration(modid, properties, Set.of(), Set.of(), List.of(),
            List.of(), Map.of()));
    }
}
