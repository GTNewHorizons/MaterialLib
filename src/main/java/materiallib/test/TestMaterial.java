package materiallib.test;

import org.jetbrains.annotations.Nullable;

import materiallib.api.material.StandardMaterial;
import materiallib.api.material.metadata.MaterialColorMetaKey;
import materiallib.api.material.metadata.MaterialMetadata;
import materiallib.api.util.Color;

public class TestMaterial extends StandardMaterial {

    public TestMaterial(int id, String name) {
        super(TestRepository.INSTANCE, id, name);
    }

    @Override
    public <T> @Nullable T getMeta(MaterialMetadata<T> key) {
        if (key == MaterialColorMetaKey.INSTANCE) return key.cast(new Color(255, 125, 125));

        return null;
    }
}
