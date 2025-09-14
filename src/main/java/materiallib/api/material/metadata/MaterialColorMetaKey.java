package materiallib.api.material.metadata;

import materiallib.api.util.ImmutableColor;

public class MaterialColorMetaKey implements MaterialMetadata<ImmutableColor> {

    public static final MaterialColorMetaKey INSTANCE = new MaterialColorMetaKey();

    private MaterialColorMetaKey() { }

    @Override
    public String getName() {
        return "material-color";
    }
}
