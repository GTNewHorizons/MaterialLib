package com.ruling_0.materiallib.api;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.Collection;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Consumer;
import java.util.function.Supplier;

import com.ruling_0.materiallib.MaterialLib;

import it.unimi.dsi.fastutil.objects.Object2ObjectLinkedOpenHashMap;
import it.unimi.dsi.fastutil.objects.ObjectArrayList;
import it.unimi.dsi.fastutil.objects.Reference2ObjectLinkedOpenHashMap;
import it.unimi.dsi.fastutil.objects.ReferenceLinkedOpenHashSet;

/// The broker holding every registered [Material] and [Family].
///
/// The registry has two phases. During registration (inside [MaterialRegistrationEvent] handlers), mods register
/// materials and families through the builders and queue cross-mod changes through [MaterialEdit] and [FamilyEdit]; key
/// lookups return the registered objects, but their membership, shapes, and properties cannot be read yet, and neither
/// can the bulk collection views. Once every handler has returned, [#resolve] elects each material's owner and folds
/// its declarations into it, applies all queued edits in call order, derives family membership and per-material shape
/// sets, and freezes the registry. From then on everything is readable and nothing can be registered or edited.
///
/// Indices are assigned deterministically at resolve: the post-unification material names sorted ascending take
/// indices 0..n-1, so identical registered sets derive identical assignments.
///
/// The game uses the single [#instance].
public final class MaterialRegistry {

    private static final MaterialRegistry INSTANCE = new MaterialRegistry();

    private final Map<String, Material> materials = new Object2ObjectLinkedOpenHashMap<>();
    private final Map<String, Family> families = new Object2ObjectLinkedOpenHashMap<>();
    private final PendingOps pendingOps = new PendingOps();
    private boolean resolved;
    private Collection<Material> materialsView;
    private Collection<Family> familiesView;
    private Material[] materialsByIndex;
    private Map<String, Integer> assignedIndices = new LinkedHashMap<>();
    private String contentHash;
    private Map<String, String> persistedOwners = new LinkedHashMap<>();
    private Map<String, String> assignedOwners = new LinkedHashMap<>();

    MaterialRegistry() {}

    /// The registry instance the game runs on, also reachable through [MaterialLibAPI].
    public static MaterialRegistry instance() {
        return INSTANCE;
    }

    public MaterialBuilder newMaterial(String modid, String name, TextureSet textureSet) {
        return new MaterialBuilder(this, modid, name, textureSet);
    }

    public FamilyBuilder newFamily(String modid, String name) {
        return new FamilyBuilder(this, modid, name);
    }

    public MaterialEdit editMaterial(String modid, String name) {
        return new MaterialEdit(this, modid, name);
    }

    public FamilyEdit editFamily(String modid, String name) {
        return new FamilyEdit(this, modid, name);
    }

    /// The material named `name`, or null unless `modid` declared it. Usable during registration, though the
    /// returned material is only readable after the registry resolves. Every declaring mod's key returns the same
    /// material.
    public Material getMaterial(String modid, String name) {
        Material material = materials.get(name);
        return material != null && material.isDeclaredBy(modid) ? material : null;
    }

    /// The family with the given key, or null if none exists. Usable during registration, though the returned
    /// family is only readable after the registry resolves.
    public Family getFamily(String modid, String name) {
        return families.get(Names.key(modid, name));
    }

    /// The material assigned the given global index (see [Material#getIndex]), or null if none has it. Only
    /// available after the registry has resolved.
    public Material getMaterialByIndex(int index) {
        requireResolved("look up a material by index", "");
        return index >= 0 && index < materialsByIndex.length ? materialsByIndex[index] : null;
    }

    /// All registered materials. Only available after the registry has resolved.
    public Collection<Material> getMaterials() {
        requireResolved("list registered materials", "");
        return materialsView;
    }

    /// All registered families. Only available after the registry has resolved.
    public Collection<Family> getFamilies() {
        requireResolved("list registered families", "");
        return familiesView;
    }

    public boolean isResolved() { return resolved; }

    /// Ends registration and freezes the registry, once the registration event has returned; other mods must not
    /// call it.
    public void resolve() {
        requireRegistration("resolve the registry");
        unifyMaterials();
        pendingOps.drain();

        assignMaterialIndices();

        Map<Family, Set<Material>> membership = new Reference2ObjectLinkedOpenHashMap<>();
        for (Family family : families.values()) {
            membership.put(family, new ReferenceLinkedOpenHashSet<>());
        }
        for (Material material : materials.values()) {
            material.resolveFamilies();
            for (Family family : material.getSortedFamiliesInternal()) {
                membership.get(family).add(material);
            }
        }
        for (Map.Entry<Family, Set<Material>> entry : membership.entrySet()) {
            entry.getKey().resolveMembers(entry.getValue());
        }
        for (Material material : materials.values()) {
            logPropertyCollisions(material);
            material.resolveShapes();
        }
        materialsView = Collections.unmodifiableCollection(materials.values());
        familiesView = Collections.unmodifiableCollection(families.values());

        resolved = true;
        MaterialLib.LOG.info("Resolved {} materials and {} families", materials.size(), families.size());
    }

    /// Elects each material's owner through an [OwnerElection] and folds its declarations into it.
    private void unifyMaterials() {
        assignedOwners = new LinkedHashMap<>(persistedOwners);
        for (Material material : materials.values()) {
            String name = material.getName();
            String ownerModid = OwnerElection.choose("Material", name, material.declarations(),
                MaterialDeclaration::modid, persistedOwners.get(name), "declared");
            assignedOwners.put(name, ownerModid);
            material.unify(ownerModid);
            for (MaterialDeclaration other : material.declarations().subList(1, material.declarations().size())) {
                MaterialLib.LOG.info("Unified material {}:{} onto owner {}", other.modid(), name, ownerModid);
            }
        }
    }

    /// The index assignment after resolve: every registered material name mapped to its index, dense over
    /// 0..n-1 in ascending name order.
    Map<String, Integer> getAssignedIndices() {
        requireResolved("read assigned material indices", "");
        return Collections.unmodifiableMap(assignedIndices);
    }

    /// The SHA-256 hex fingerprint of the index assignment: the material names joined with `\n` in index
    /// order, hashed as UTF-8. Instances with equal hashes agree on every index.
    String getContentHash() {
        requireResolved("read the material list hash", "");
        return contentHash;
    }

    /// Sets the persisted material owners to honor at resolve, loaded from the instance-global store. Must be
    /// set before resolve.
    void setPersistedOwners(Map<String, String> owners) {
        requireRegistration("set persisted material owners");
        this.persistedOwners = new LinkedHashMap<>(owners);
    }

    /// The full owner assignment after resolve: every persisted entry (including names with no declaration this
    /// session) plus the owners chosen this session.
    Map<String, String> getAssignedOwners() {
        requireResolved("read assigned material owners", "");
        return Collections.unmodifiableMap(assignedOwners);
    }

    /// The full index assignment as a CSV table for debugging, with the columns [MaterialCsv] lists. Only
    /// available after the registry has resolved.
    public String dumpCsv() {
        return MaterialCsv.dump(this, ShapeRegistry.instance()::describeOverrides);
    }

    /// Assigns each material its global index and fingerprints the result as [#getContentHash].
    private void assignMaterialIndices() {
        List<String> names = new ObjectArrayList<>(materials.keySet());
        Collections.sort(names);
        if (names.size() - 1 > Short.MAX_VALUE) {
            throw new IllegalStateException(
                "Material index " + (names.size() - 1) + " exceeds the item damage limit of " + Short.MAX_VALUE +
                    "; too many materials are registered.");
        }

        assignedIndices = new LinkedHashMap<>();
        materialsByIndex = new Material[names.size()];
        for (int index = 0; index < names.size(); index++) {
            String name = names.get(index);
            assignedIndices.put(name, index);
            Material material = materials.get(name);
            material.resolveIndex(index);
            materialsByIndex[index] = material;
        }
        contentHash = contentHash(names);
    }

    /// The SHA-256 hex digest of `namesInIndexOrder` joined with `\n`, encoded as UTF-8.
    static String contentHash(List<String> namesInIndexOrder) {
        MessageDigest digest;
        try {
            digest = MessageDigest.getInstance("SHA-256");
        }
        catch (NoSuchAlgorithmException e) {
            throw new IllegalStateException("SHA-256 is unavailable", e);
        }
        byte[] bytes = digest.digest(String.join("\n", namesInIndexOrder).getBytes(StandardCharsets.UTF_8));
        StringBuilder hex = new StringBuilder(bytes.length * 2);
        for (byte b : bytes) {
            hex.append(Character.forDigit((b >> 4) & 0xF, 16)).append(Character.forDigit(b & 0xF, 16));
        }
        return hex.toString();
    }

    /// Logs each property whose resolved value is ambiguous for a material: the material does not set it, and
    /// two or more of its families set conflicting values. The alphabetically-first family still wins.
    private void logPropertyCollisions(Material material) {
        Family[] sorted = material.getSortedFamiliesInternal();
        if (sorted.length < 2) return;
        Map<Property<?>, Family> firstSetters = new Reference2ObjectLinkedOpenHashMap<>();
        for (Family family : sorted) {
            for (Map.Entry<Property<?>, Object> entry : family.getOwnPropertiesInternal().entrySet()) {
                Property<?> property = entry.getKey();
                if (material.getOwnPropertiesInternal().containsKey(property)) continue;
                Family first = firstSetters.putIfAbsent(property, family);
                if (first != null && !entry.getValue().equals(first.getOwnPropertiesInternal().get(property))) {
                    MaterialLib.LOG.warn(
                        "Material {} takes {} = {} from family {}; family {} sets conflicting value {}",
                        material.getKey(),
                        property,
                        first.getOwnPropertiesInternal().get(property),
                        first.getKey(),
                        family.getKey(),
                        entry.getValue());
                }
            }
        }
    }

    /// Adds `declaration` to the material named `name`, creating that material on its first declaration, and returns
    /// it. Throws [IllegalStateException] when the declaring mod already declared the name.
    Material declare(String name, MaterialDeclaration declaration) {
        requireRegistration("register material " + Names.key(declaration.modid(), name));
        Material material = materials.computeIfAbsent(name, ignored -> new Material(this, name));
        material.addDeclaration(declaration);
        return material;
    }

    void register(Family family) {
        requireRegistration("register family " + family.getKey());
        Family existing = families.putIfAbsent(family.getKey(), family);
        if (existing != null) {
            throw new IllegalStateException("Family " + family.getKey() + " is already registered");
        }
    }

    void enqueueMaterialOp(String modid, String name, String description, Consumer<Material> op) {
        enqueueOp(
            () -> getMaterial(modid, name),
            "Skipping edit \"{} {}\": no such material is registered",
            modid,
            name,
            description,
            op);
    }

    void enqueueFamilyOp(String modid, String name, String description, Consumer<Family> op) {
        enqueueOp(
            () -> getFamily(modid, name),
            "Skipping edit \"{} {}\": no such family is registered",
            modid,
            name,
            description,
            op);
    }

    private <T> void enqueueOp(Supplier<T> lookup, String missingWarning, String modid, String name,
                               String description, Consumer<T> op) {
        String key = Names.key(modid, name);
        enqueue(description + " " + key, () -> {
            T target = lookup.get();
            if (target == null) {
                MaterialLib.LOG.warn(missingWarning, description, key);
                return;
            }
            op.accept(target);
        });
    }

    void enqueueAddToFamily(String materialModid, String materialName, String familyModid, String familyName) {
        String familyKey = Names.key(familyModid, familyName);
        enqueueMaterialOp(materialModid, materialName, "add to family " + familyKey + " material",
            material -> addToFamily(material, familyKey));
    }

    void enqueueAddToFamily(Material material, String familyModid, String familyName) {
        String familyKey = Names.key(familyModid, familyName);
        enqueue("add to family " + familyKey + " material " + material.getName(),
            () -> addToFamily(material, familyKey));
    }

    private void addToFamily(Material material, String familyKey) {
        Family family = familyForEdit(familyKey, "addition", material);
        if (family == null) return;
        material.addFamilyInternal(family);
    }

    void enqueueRemoveFromFamily(String materialModid, String materialName, String familyModid, String familyName) {
        String familyKey = Names.key(familyModid, familyName);
        enqueueMaterialOp(materialModid, materialName, "remove from family " + familyKey + " material", material -> {
            Family family = familyForEdit(familyKey, "removal", material);
            if (family == null) return;
            if (!material.isMemberOfInternal(family)) {
                MaterialLib.LOG.warn(
                    "Skipping family removal for material {}: it is not a member of {} at this point in the edit order",
                    material.getKey(),
                    familyKey);
                return;
            }
            material.removeFamilyInternal(family);
        });
    }

    /// The family for a queued family membership edit, or null after logging a skip warning when `familyKey` is
    /// not registered.
    private Family familyForEdit(String familyKey, String action, Material material) {
        Family family = families.get(familyKey);
        if (family == null) {
            MaterialLib.LOG.warn(
                "Skipping family {} for material {}: no such family {} is registered",
                action,
                material.getKey(),
                familyKey);
        }
        return family;
    }

    private void enqueue(String description, Runnable action) {
        requireRegistration(description);
        pendingOps.add(description, action);
    }

    void requireResolved(String action, String target) {
        if (!resolved) {
            throw new IllegalStateException(
                "Cannot " + action + target + ": the material registry has not resolved yet. " +
                    "Registry contents are readable once MaterialLib's preInit has resolved the registry; " +
                    "mods depending on materiallib read from their preInit onwards");
        }
    }

    private void requireRegistration(String what) {
        if (resolved) {
            throw new IllegalStateException(
                "Cannot " + what + ": the material registry has already resolved. " +
                    "Materials, families, and edits register inside a MaterialRegistrationEvent handler " +
                    "subscribed during construction");
        }
    }
}
