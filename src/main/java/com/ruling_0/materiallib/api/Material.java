package com.ruling_0.materiallib.api;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Predicate;
import java.util.function.UnaryOperator;

import net.minecraft.util.StatCollector;

import com.ruling_0.materiallib.MaterialLib;

import it.unimi.dsi.fastutil.objects.Reference2ObjectLinkedOpenHashMap;
import it.unimi.dsi.fastutil.objects.ReferenceArraySet;
import it.unimi.dsi.fastutil.objects.ReferenceLinkedOpenHashSet;
import it.unimi.dsi.fastutil.objects.ReferenceOpenHashSet;

/// A named material, such as iron, that generates a set of [Shape]s and carries [Property] values.
///
/// Materials are created through [MaterialLibAPI#newMaterial] inside a [MaterialRegistrationEvent] handler and
/// become read-only once the registry resolves at the end of MaterialLib's preInit. A material may belong to
/// any number of [Family]s. Membership, properties, and the effective shape set are only available after
/// resolution.
///
/// Two mods may declare a material with the same name without coordinating. Every [MaterialBuilder#build] of one name
/// returns the same material. At resolve, the declarations unify into that material, carrying the union of their
/// shapes, families, tooltip lines, and properties and keeping the owner's value for a property both declare. The owner
/// is the mod recorded in the persisted owner store when it declares the name this session, else the
/// alphabetically-first declaring modid. It supplies the modid, key, texture set, and lang key. [#getModId], [#getKey]
/// and [#getLocalizedName] are only available once the registry has elected it.
///
/// A shape override ([MaterialBuilder#addShapeOverride(Shape, net.minecraft.item.Item)]) serves one shape of the
/// material with a foreign item instead of one MaterialLib mints. A [MaterialEdit] override wins over every
/// declaration. Otherwise the owner's override applies. A non-owning declaration's override applies only to a shape
/// that declaration generates and the owner's declaration does not. Among several, the alphabetically-first modid
/// wins. An override for a shape the unified material does not generate is ignored.
public final class Material {

    private static final Comparator<Family> FAMILY_KEY_ORDER = Comparator.comparing(Family::getKey);

    private final MaterialRegistry registry;
    private final String name;
    private final List<MaterialDeclaration> declarations = new ArrayList<>(1);
    private final Map<Property<?>, Object> properties = new Reference2ObjectLinkedOpenHashMap<>();
    private final Set<Shape> ownShapes = new ReferenceLinkedOpenHashSet<>();
    // Plain (unordered) sets: removedShapes is only membership-tested, and families is sorted into
    // sortedFamilies before any iteration order can be observed.
    private final Set<Shape> removedShapes = new ReferenceOpenHashSet<>(4);
    private final Set<Family> families = new ReferenceOpenHashSet<>(4);
    private final List<String> tooltipLines = new ArrayList<>(2);
    private final Map<Shape, ShapeOverride> editedOverrides = new Reference2ObjectLinkedOpenHashMap<>();

    private Family[] sortedFamilies;
    private Set<Family> familiesView;
    private Set<Shape> shapes;
    private UnaryOperator<Shape> canonicalShape = UnaryOperator.identity();
    private Map<Property<?>, Object> propertiesView;
    private int index = -1;
    private String modid;
    private String key;

    Material(MaterialRegistry registry, String name) {
        this.registry = registry;
        this.name = name;
    }

    /// The owning mod's modid. Only available once the registry has elected the owner.
    public String getModId() {
        requireOwner();
        return modid;
    }

    /// The material's registry name, used in translation keys and oredict entries. Conventionally starts with a
    /// capital letter.
    public String getName() { return name; }

    /// The translation of the material's [StandardProperties#DISPLAY_NAME_KEY] (by default
    /// `material.<modid>.<name>`), or the registry name when that key has no translation.
    public String getLocalizedName() {
        String key = ShapeNaming.materialNameKey(this);
        return StatCollector.canTranslate(key) ? StatCollector.translateToLocal(key) : name;
    }

    /// The registry key, `modid:name`, with the owner's modid. Only available once the registry has elected the owner.
    public String getKey() {
        requireOwner();
        return key;
    }

    /// The material's global index, used as the item damage in every item and block shape. Only available after the
    /// registry has resolved.
    public int getIndex() {
        registry.requireResolved("query the index of ", name);
        return index;
    }

    /// The families this material belongs to, iterated in alphabetical (case-sensitive) `modid:name` key order
    /// -- the same order used to resolve property values. Empty for a standalone material. Only available after
    /// the registry has resolved.
    public Set<Family> getFamilies() {
        registry.requireResolved("query the families of ", name);
        return familiesView;
    }

    /// The shapes this material generates: its own shapes plus its families', minus any removed for this
    /// material specifically. Once shapes unify, each is the canonical shape of its name. Only available after the
    /// registry has resolved.
    public Set<Shape> getShapes() {
        registry.requireResolved("query the shapes of ", name);
        return shapes;
    }

    /// Whether this material generates `shape`, or the shape it unified onto. Agrees with
    /// [MaterialLibAPI#findStack] returning a stack. Only available after the registry has resolved.
    public boolean hasShape(Shape shape) {
        return getShapes().contains(canonicalShape.apply(shape));
    }

    /// Replaces each generated shape with its canonical shape once shape names unify.
    void unifyShapes(UnaryOperator<Shape> canonicalOf) {
        canonicalShape = canonicalOf;
        Set<Shape> unified = new ReferenceLinkedOpenHashSet<>(shapes.size());
        for (Shape shape : shapes) {
            unified.add(canonicalOf.apply(shape));
        }
        shapes = Collections.unmodifiableSet(unified);
    }

    /// Resolves a property for this material: its own value, else the value of the alphabetically-first family
    /// (by case-sensitive `modid:name` key) that sets it, else the property's default. Only available after the
    /// registry has resolved.
    @SuppressWarnings("unchecked")
    public <T> T getProperty(Property<T> property) {
        registry.requireResolved("query properties of ", name);
        Object value = properties.get(property);
        if (value != null) return (T) value;
        for (Family family : sortedFamilies) {
            Object inherited = family.getOwnPropertiesInternal().get(property);
            if (inherited != null) return (T) inherited;
        }
        return property.getDefaultValue();
    }

    /// True if this material or any of its families sets the property explicitly (the property default does not
    /// count).
    public boolean hasProperty(Property<?> property) {
        registry.requireResolved("query properties of ", name);
        if (properties.containsKey(property)) return true;
        for (Family family : sortedFamilies) {
            if (family.getOwnPropertiesInternal().containsKey(property)) return true;
        }
        return false;
    }

    /// Properties set directly on this material, excluding family-level and default values.
    public Map<Property<?>, Object> getOwnProperties() {
        registry.requireResolved("query properties of ", name);
        return propertiesView;
    }

    public boolean hasCustomTooltip() {
        registry.requireResolved("query the tooltip of ", name);
        return !tooltipLines.isEmpty();
    }

    /// The added tooltip lines of this material, as an unmodifiable view.
    public List<String> getTooltip() {
        registry.requireResolved("query the tooltip of ", name);
        return Collections.unmodifiableList(tooltipLines);
    }

    void setPropertyValue(Property<?> property, Object value) {
        requireMutable();
        properties.put(property, value);
    }

    void removePropertyValue(Property<?> property) {
        requireMutable();
        properties.remove(property);
    }

    void addShape(Shape shape) {
        requireMutable();
        ownShapes.add(shape);
        removedShapes.remove(shape);
    }

    void removeShape(Shape shape) {
        requireMutable();
        ownShapes.remove(shape);
        removedShapes.add(shape);
    }

    void addTooltip(String... lines) {
        requireMutable();
        tooltipLines.addAll(Arrays.asList(lines));
    }

    void setEditedOverride(Shape shape, ShapeOverride override) {
        requireMutable();
        editedOverrides.put(shape, override);
    }

    /// The override serving `canonicalShape` of this unified material, or null when MaterialLib mints the pair. See
    /// the class doc for the precedence. `canonical` maps a declared shape onto its election winner.
    ShapeOverride chooseOverride(Shape canonicalShape, UnaryOperator<Shape> canonical, Predicate<String> modLoaded) {
        if (!namesOverrides()) return null;
        ShapeOverride edited = null;
        for (Map.Entry<Shape, ShapeOverride> entry : editedOverrides.entrySet()) {
            if (canonical.apply(entry.getKey()) == canonicalShape && entry.getValue().isAvailable(modLoaded)) {
                edited = entry.getValue();
            }
        }
        if (edited != null) return edited;

        MaterialDeclaration owner = declarations.get(0);
        ShapeOverride owned = owner.overrideFor(canonicalShape, canonical);
        if (owned != null && owned.isAvailable(modLoaded)) return owned;
        if (owner.generates(canonicalShape, canonical, registry)) return null;

        for (MaterialDeclaration other : declarations.subList(1, declarations.size())) {
            ShapeOverride declared = other.overrideFor(canonicalShape, canonical);
            if (declared != null && declared.isAvailable(modLoaded) &&
                other.generates(canonicalShape, canonical, registry)) {
                return declared;
            }
        }
        return null;
    }

    /// Whether some declaration or edit of this unified material names an override.
    private boolean namesOverrides() {
        if (!editedOverrides.isEmpty()) return true;
        for (MaterialDeclaration declaration : declarations) {
            if (!declaration.overrides().isEmpty()) return true;
        }
        return false;
    }

    /// Every shape named by an override in some declaration or edit of this unified material.
    Set<Shape> overriddenShapeDeclarations() {
        if (!namesOverrides()) return Set.of();
        Set<Shape> declared = new ReferenceLinkedOpenHashSet<>(editedOverrides.keySet());
        for (MaterialDeclaration declaration : declarations) {
            declared.addAll(declaration.overrides().keySet());
        }
        return declared;
    }

    void clearTooltip() {
        requireMutable();
        tooltipLines.clear();
    }

    /// Adds one mod's declaration of this material's name. Throws [IllegalStateException] when that mod already
    /// declared it.
    void addDeclaration(MaterialDeclaration declaration) {
        requireMutable();
        if (isDeclaredBy(declaration.modid())) {
            throw new IllegalStateException(
                "Material " + Names.key(declaration.modid(), name) + " is already registered");
        }
        declarations.add(declaration);
    }

    boolean isDeclaredBy(String modid) {
        for (MaterialDeclaration declaration : declarations) {
            if (declaration.modid().equals(modid)) return true;
        }
        return false;
    }

    /// Every declaration of this material, in declaration order until it unifies, then owner first and the rest by
    /// modid.
    List<MaterialDeclaration> declarations() {
        return Collections.unmodifiableList(declarations);
    }

    /// Makes `ownerModid` the owner and folds every declaration into this material, owner first, then by modid. A
    /// property keeps the first value set, and shapes and tooltip lines accumulate.
    void unify(String ownerModid) {
        declarations.sort(Comparator
            .comparing((MaterialDeclaration declaration) -> !declaration.modid().equals(ownerModid))
            .thenComparing(MaterialDeclaration::modid));
        for (MaterialDeclaration declaration : declarations) {
            for (Map.Entry<Property<?>, Object> entry : declaration.properties().entrySet()) {
                Object existing = properties.putIfAbsent(entry.getKey(), entry.getValue());
                if (existing != null && !existing.equals(entry.getValue())) {
                    MaterialLib.LOG.warn(
                        "Material {} keeps {} = {}; {} declares conflicting value {}",
                        name,
                        entry.getKey(),
                        existing,
                        declaration.modid(),
                        entry.getValue());
                }
            }
            ownShapes.addAll(declaration.shapes());
            tooltipLines.addAll(declaration.tooltipLines());
        }
        modid = ownerModid;
        key = Names.key(ownerModid, name);
    }

    void addFamilyInternal(Family family) {
        requireMutable();
        families.add(family);
    }

    void removeFamilyInternal(Family family) {
        requireMutable();
        families.remove(family);
    }

    boolean isMemberOfInternal(Family family) {
        return families.contains(family);
    }

    Map<Property<?>, Object> getOwnPropertiesInternal() { return properties; }

    Family[] getSortedFamiliesInternal() { return sortedFamilies; }

    void resolveIndex(int index) {
        this.index = index;
    }

    void resolveFamilies() {
        sortedFamilies = families.toArray(new Family[0]);
        Arrays.sort(sortedFamilies, FAMILY_KEY_ORDER);
        familiesView = Collections.unmodifiableSet(new ReferenceArraySet<>(sortedFamilies));
        propertiesView = Collections.unmodifiableMap(properties);
    }

    void resolveShapes() {
        Set<Shape> effective = new ReferenceLinkedOpenHashSet<>(ownShapes);
        for (Family family : sortedFamilies) {
            for (Shape shape : family.getShapesInternal()) {
                if (!removedShapes.contains(shape)) {
                    effective.add(shape);
                }
            }
        }
        shapes = Collections.unmodifiableSet(effective);
    }

    private void requireMutable() {
        if (registry.isResolved()) {
            throw new IllegalStateException("Material " + name + " mutated after the registry resolved");
        }
    }

    private void requireOwner() {
        if (modid == null) {
            throw new IllegalStateException("Material " + name +
                " has no owner until the material registry resolves. Mods depending on materiallib read its modid and key from their preInit onwards");
        }
    }

    /// `Material[modid:name]`, or `Material[name]` before the owner is elected.
    @Override
    public String toString() {
        return "Material[" + (key != null ? key : name) + "]";
    }
}
