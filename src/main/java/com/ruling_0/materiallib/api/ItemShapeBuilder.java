package com.ruling_0.materiallib.api;

import java.util.Objects;

/// Builds and registers a simple item [Shape] backed by a [ShapeItem]. Obtained from
/// [MaterialLibAPI#newItemShape] and finished with [#build], which must be called inside the owning mod's
/// [MaterialRegistrationEvent] handler. Mods needing custom item behavior subclass [ShapeItem] instead and
/// register through [MaterialLibAPI#registerItemShape].
public final class ItemShapeBuilder extends ShapeBuilder<ItemShapeBuilder> {

    private String[] oreDicts;
    private String iconName;

    ItemShapeBuilder(String modid, String name) {
        super(modid, name, "Item shape");
    }

    /// Sets the oredict prefixes; the material name is appended to each (e.g. `gear` -> `gearIron`). Pass several
    /// to register the item under each, e.g. `oreDict("gear", "cog")` gives both `gearIron` and `cogIron`.
    /// Defaults to the shape name. At least one prefix is required.
    public ItemShapeBuilder oreDict(String... prefixes) {
        this.oreDicts = prefixes;
        return this;
    }

    /// Sets the name this shape's textures are filed under inside each texture set, defaulting to the shape name,
    /// so several shapes can share one art file (e.g. every wire gauge drawing `wire.png`). Under shape
    /// unification the owning declaration's icon name wins; see [ShapeUnification].
    public ItemShapeBuilder iconName(String iconName) {
        this.iconName = Objects.requireNonNull(iconName, "iconName must not be null");
        return this;
    }

    @Override
    ServedShape create() {
        ShapeItem shape = new ShapeItem(modid, name, prefixesOrName(oreDicts));
        if (iconName != null) {
            shape.setIconName(iconName);
        }
        return shape;
    }
}
