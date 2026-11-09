package com.ruling_0.materiallib.api;

import java.util.Map;

import it.unimi.dsi.fastutil.objects.Reference2ObjectLinkedOpenHashMap;

/// The part shared by the simple shape builders: the owner and name, the shape's properties, and registering the
/// shape once through [#build]. Each builder is obtained from its `MaterialLibAPI.new*Shape` factory, and [#build]
/// must be called inside the owning mod's [MaterialRegistrationEvent] handler.
public abstract class ShapeBuilder<B extends ShapeBuilder<B>> {

    final String modid;
    final String name;
    private final String kind;
    private final Map<Property<?>, Object> properties = new Reference2ObjectLinkedOpenHashMap<>();
    private boolean built;

    ShapeBuilder(String modid, String name, String kind) {
        this.modid = modid;
        this.name = name;
        this.kind = kind;
    }

    /// Sets the shape's [StandardProperties#DISPLAY_NAME_FORMAT_KEY], the translation key of the format applied to the
    /// material name. For example, a key translating to `%s Gear` gives `Iron Gear`. Without a translation, the
    /// name is the material name followed by the capitalized shape name. A lang file may override individual
    /// names; see [ShapeNaming].
    public B displayNameKey(String key) {
        return property(StandardProperties.DISPLAY_NAME_FORMAT_KEY, key);
    }

    /// Sets a property value on the shape. Values are read back through [Shape#getProperty] and may be altered
    /// by another mod through [MaterialLibAPI#editShape].
    public <T> B property(Property<T> property, T value) {
        ShapeProperties.requireSettable(property, value);
        properties.put(property, value);
        return self();
    }

    /// Registers the shape and returns the shape to generate; see [ShapeRegistry#register]. Fails if called twice,
    /// or if the builder is missing a required setting.
    public final Shape build() {
        if (built) {
            throw new IllegalStateException(label() + " was already built");
        }
        validate();
        built = true;
        ServedShape shape = create();
        shape.properties().setAll(shape, properties);
        return ShapeRegistry.instance().register(shape);
    }

    /// Throws an [IllegalStateException] when the builder cannot build its shape.
    void validate() {}

    /// Creates the shape, before its properties are applied.
    abstract ServedShape create();

    /// The builder kind and shape key, for error messages.
    final String label() {
        return kind + " " + Names.key(modid, name);
    }

    /// The given oredict prefixes, or the shape name when none were set.
    final String[] prefixesOrName(String[] prefixes) {
        return prefixes != null ? prefixes : new String[] { name };
    }

    @SuppressWarnings("unchecked")
    private B self() {
        return (B) this;
    }
}
