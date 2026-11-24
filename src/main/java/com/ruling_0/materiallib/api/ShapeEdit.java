package com.ruling_0.materiallib.api;

import java.util.Objects;

import com.ruling_0.materiallib.MaterialLib;

/// Queued cross-mod changes to a [Shape] identified by name, obtained from [MaterialLibAPI#editShape].
/// Queuing, ordering, and skip behavior are as in [MaterialEdit].
///
/// Unlike a material, a shape is addressed by name alone. Shape names unify across mods into one owner (see
/// [ShapeUnification]), and an edit applies to that owner. The modid identifies the editor, not the target.
public final class ShapeEdit {

    private final ShapeRegistry registry;
    private final String modid;
    private final String name;

    ShapeEdit(ShapeRegistry registry, String modid, String name) {
        this.registry = registry;
        this.modid = Names.validate("shape modid", modid);
        this.name = Names.validate("shape name", name);
    }

    /// Sets a property value on the shape.
    public <T> ShapeEdit setProperty(Property<T> property, T value) {
        ShapeProperties.requireSettable(property, value);
        registry.enqueueShapeOp(
            modid,
            name,
            "set " + property + " on shape",
            shape -> shape.properties().set(shape, property, value));
        return this;
    }

    /// Clears the shape's value for a property, letting the property default show again.
    public ShapeEdit removeProperty(Property<?> property) {
        Objects.requireNonNull(property, "property must not be null");
        registry.enqueueShapeOp(
            modid,
            name,
            "remove " + property + " from shape",
            shape -> shape.properties().remove(shape, property));
        return this;
    }

    /// Attaches a callback that runs when a player uses one of the shape's items on a block. See [ItemUseCallback]
    /// for the callback contract. Callbacks run in the order attached. A pair served by an override item from
    /// [MaterialBuilder#addShapeOverride] does not run them. Skipped with a warning for a non-item shape.
    public ShapeEdit onItemUse(ItemUseCallback callback) {
        Objects.requireNonNull(callback, "callback must not be null");
        registry.enqueueShapeOp(modid, name, "attach item use callback to shape", shape -> {
            ShapeItem item = asItem(shape);
            if (item != null) item.addUseCallback(callback);
        });
        return this;
    }

    /// Attaches a callback that runs every tick while one of the shape's items lies dropped in the world. See
    /// [EntityItemCallback] for the callback contract. Ordering, overridden pairs and non-item shapes are as in
    /// [#onItemUse].
    public ShapeEdit onEntityItemUpdate(EntityItemCallback callback) {
        Objects.requireNonNull(callback, "callback must not be null");
        registry.enqueueShapeOp(modid, name, "attach dropped item callback to shape", shape -> {
            ShapeItem item = asItem(shape);
            if (item != null) item.addEntityItemCallback(callback);
        });
        return this;
    }

    /// Attaches a callback that runs while an entity touches the placed block of one of the shape's fluids. See
    /// [FluidContactCallback] for the callback contract. Callbacks run in the order attached. Skipped with a warning
    /// for a non-fluid shape.
    public ShapeEdit onFluidContact(FluidContactCallback callback) {
        Objects.requireNonNull(callback, "callback must not be null");
        registry.enqueueShapeOp(modid, name, "attach fluid contact callback to shape", shape -> {
            ShapeFluid fluid = asFluid(shape);
            if (fluid != null) fluid.addContactCallback(callback);
        });
        return this;
    }

    /// Attaches a callback that runs when the placed block of one of the shape's fluids ticks. See
    /// [FluidTickCallback] for the callback contract. Ordering and non-fluid shapes are as in [#onFluidContact].
    public ShapeEdit onFluidTick(FluidTickCallback callback) {
        Objects.requireNonNull(callback, "callback must not be null");
        registry.enqueueShapeOp(modid, name, "attach fluid tick callback to shape", shape -> {
            ShapeFluid fluid = asFluid(shape);
            if (fluid != null) fluid.addTickCallback(callback);
        });
        return this;
    }

    /// Attaches a callback that runs when a block next to the placed block of one of the shape's fluids changes. See
    /// [FluidNeighborCallback] for the callback contract. Ordering and non-fluid shapes are as in [#onFluidContact].
    public ShapeEdit onFluidNeighborChange(FluidNeighborCallback callback) {
        Objects.requireNonNull(callback, "callback must not be null");
        registry.enqueueShapeOp(modid, name, "attach fluid neighbor callback to shape", shape -> {
            ShapeFluid fluid = asFluid(shape);
            if (fluid != null) fluid.addNeighborCallback(callback);
        });
        return this;
    }

    private ShapeFluid asFluid(ServedShape shape) {
        if (shape instanceof ShapeFluid fluid) return fluid;
        MaterialLib.LOG.warn("Skipping fluid callback from {}: shape {} is not a fluid shape", modid, name);
        return null;
    }

    private ShapeItem asItem(ServedShape shape) {
        if (shape instanceof ShapeItem item) return item;
        MaterialLib.LOG.warn("Skipping item callback from {}: shape {} is not an item shape", modid, name);
        return null;
    }
}
