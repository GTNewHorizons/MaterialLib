package com.ruling_0.materiallib.api;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.Random;
import java.util.Set;
import java.util.function.Predicate;

import net.minecraft.block.Block;
import net.minecraft.client.renderer.texture.IIconRegister;
import net.minecraft.entity.Entity;
import net.minecraft.util.IIcon;
import net.minecraft.world.World;

import net.minecraftforge.fluids.Fluid;
import net.minecraftforge.fluids.FluidRegistry;
import net.minecraftforge.fluids.FluidStack;

import com.ruling_0.materiallib.MaterialLib;

import cpw.mods.fml.common.registry.GameRegistry;
import cpw.mods.fml.relauncher.Side;
import cpw.mods.fml.relauncher.SideOnly;
import it.unimi.dsi.fastutil.ints.Int2ObjectMap;
import it.unimi.dsi.fastutil.ints.Int2ObjectOpenHashMap;
import it.unimi.dsi.fastutil.objects.ReferenceOpenHashSet;

/// The Forge fluid backing a fluid [Shape]: one registered [Fluid] per material that generates the shape, named by
/// this shape's [FluidNamer] (by default `<shape>.<material>` lowercased, e.g. `test.testiron`).
///
/// A fluid shape is not a [BackedShape], as fluids are registered by name and not numeric ID. Materials declare it
/// through [MaterialBuilder#generateShape]. At resolve, the registry registers one fluid per material and configures
/// newly registered fluids through this shape's [FluidConfigurer]. On the client, each fluid's still and flowing
/// icons bind from a resource-pack override, then from this shape's [IconPather] when it names an existing file
/// (see [FluidShapeBuilder#iconPath]), and otherwise from the material's texture sets -- the same chain [ShapeIcons]
/// walks for an item or block icon.
///
/// A bare fluid has no item form, so its material tooltip is carried by its container item (see
/// [ShapeFluidInContainer]). Each fluid takes its display name from the shape's format and its color from the
/// material's [StandardProperties#FLUID_TINT], or [StandardProperties#TINT] when unset, so tank and GUI renderers
/// show the right name and tint. A fluid whose icon came from the resource-pack override location is untinted; see
/// [ShapeItem#hasOverrideIcon].
///
/// A placeable pair (see [MaterialBuilder#setPlaceable]) also gets a [ShapeFluidBlock] named `fluid.<fluidName>` in
/// MaterialLib's domain.
public class ShapeFluid implements ServedShape {

    private static final List<String> NO_OREDICTS = List.of();
    private static final FluidConfigurer NO_OP_CONFIGURER = (material, fluid) -> {};
    private static final FluidBlockConfigurer NO_OP_BLOCK_CONFIGURER = (material, block) -> {};

    private final String modid;
    private final String name;
    private final FluidNamer namer;
    private final FluidConfigurer configurer;
    private final IconPather iconPather;
    private final FluidBlockKindChooser kindChooser;
    private final FluidBlockConfigurer blockConfigurer;

    private final List<FluidContactCallback> contactCallbacks = new ArrayList<>();
    private final List<FluidTickCallback> tickCallbacks = new ArrayList<>();
    private final List<FluidNeighborCallback> neighborCallbacks = new ArrayList<>();

    private final ServedMaterials served = new ServedMaterials();
    private final ShapeProperties props = new ShapeProperties();

    private final Int2ObjectMap<Fluid> fluidsByIndex = new Int2ObjectOpenHashMap<>();
    private final Int2ObjectMap<ShapeFluidBlock> blocksByIndex = new Int2ObjectOpenHashMap<>();
    private final Set<Material> warnedMissingIcon = new ReferenceOpenHashSet<>();
    private final Set<Material> overrideIcons = new ReferenceOpenHashSet<>();

    /// Creates a fluid shape. Identifiers must be non-empty and free of ':' and whitespace. The fluid's display name
    /// comes from the shape's [StandardProperties#LOCALIZATION_FORMAT_KEY].
    public ShapeFluid(String modid, String name) {
        this(modid, name, null, null);
    }

    /// As [#ShapeFluid(String, String)], additionally setting this shape's [FluidNamer] and [FluidConfigurer]. A
    /// null namer defaults to [FluidNamer#DEFAULT]; a null configurer performs no extra configuration.
    public ShapeFluid(String modid, String name, FluidNamer namer, FluidConfigurer configurer) {
        this(modid, name, namer, configurer, null);
    }

    /// As [#ShapeFluid(String, String, FluidNamer, FluidConfigurer)], additionally setting this shape's
    /// [IconPather], or null for none; see [#resolveIconPath].
    ShapeFluid(String modid, String name, FluidNamer namer, FluidConfigurer configurer, IconPather iconPather) {
        this(modid, name, namer, configurer, iconPather, null, null);
    }

    /// As [#ShapeFluid(String, String, FluidNamer, FluidConfigurer, IconPather)], additionally setting the
    /// [FluidBlockKindChooser], where null means [FluidBlockKindChooser#DEFAULT], and the [FluidBlockConfigurer],
    /// where null performs no extra configuration.
    ShapeFluid(String modid, String name, FluidNamer namer, FluidConfigurer configurer, IconPather iconPather,
               FluidBlockKindChooser kindChooser, FluidBlockConfigurer blockConfigurer) {
        this.modid = Names.validate("fluid shape modid", modid);
        this.name = Names.validate("fluid shape name", name);
        this.namer = namer != null ? namer : FluidNamer.DEFAULT;
        this.configurer = configurer != null ? configurer : NO_OP_CONFIGURER;
        this.iconPather = iconPather;
        this.kindChooser = kindChooser != null ? kindChooser : FluidBlockKindChooser.DEFAULT;
        this.blockConfigurer = blockConfigurer != null ? blockConfigurer : NO_OP_BLOCK_CONFIGURER;
    }

    @Override
    public String getModId() { return modid; }

    @Override
    public String getName() { return name; }

    @Override
    public List<String> getOreDicts() { return NO_OREDICTS; }

    @Override
    public String toString() {
        return "ShapeFluid[" + Names.key(modid, name) + "]";
    }

    @Override
    public void bindServedMaterials(Material[] materials) {
        served.bind(this, materials);
    }

    @Override
    public Material[] getServedMaterials() { return served.get(); }

    @Override
    public boolean serves(Material material) {
        return served.contains(material);
    }

    @Override
    public ShapeProperties properties() {
        return props;
    }

    /// The Forge fluid name for a material in this shape, as produced by this shape's [FluidNamer].
    String fluidName(Material material) {
        return namer.name(this, material);
    }

    /// The first served material this shape and `candidate` name differently, or null when both name every served
    /// material identically.
    Material firstNameDivergence(ShapeFluid candidate) {
        for (Material material : served.get()) {
            if (!fluidName(material).equals(candidate.fluidName(material))) {
                return material;
            }
        }
        return null;
    }

    /// Logs where `candidate`, a fluid shape unified onto this one, would have named, configured or chosen block
    /// kinds for this shape's fluids differently, or configured their blocks differently.
    void logCandidateDivergence(ShapeFluid candidate) {
        try {
            Material diverging = firstNameDivergence(candidate);
            if (diverging != null) {
                MaterialLib.LOG.error(
                    "Fluid shapes {} and {} share a name but name the fluid of {} differently ({} vs {}); " +
                        "registering only the owner's names, so stored fluid stacks depend on which mod owns the shape",
                    Names.key(modid, name),
                    Names.key(candidate.modid, candidate.name),
                    diverging.getKey(),
                    fluidName(diverging),
                    candidate.fluidName(diverging));
            }
        }
        catch (RuntimeException e) {
            MaterialLib.LOG.error("Fluid namer of {} failed while comparing its names against {}", candidate, this, e);
        }
        if (candidate.configurer != NO_OP_CONFIGURER && candidate.configurer != configurer) {
            MaterialLib.LOG.warn(
                "Fluid shapes {} and {} share a name but set different fluid configurers; only the owner's runs",
                Names.key(modid, name),
                Names.key(candidate.modid, candidate.name));
        }
        if (candidate.kindChooser != FluidBlockKindChooser.DEFAULT && candidate.kindChooser != kindChooser) {
            MaterialLib.LOG.warn(
                "Fluid shapes {} and {} share a name but set different block kind choosers; only the owner's runs",
                Names.key(modid, name),
                Names.key(candidate.modid, candidate.name));
        }
        if (candidate.blockConfigurer != NO_OP_BLOCK_CONFIGURER && candidate.blockConfigurer != blockConfigurer) {
            MaterialLib.LOG.warn(
                "Fluid shapes {} and {} share a name but set different block configurers; only the owner's runs",
                Names.key(modid, name),
                Names.key(candidate.modid, candidate.name));
        }
    }

    /// Registers one Forge fluid per served material, validating and reserving each material's fluid name against
    /// `usedFluidNames`, shared across every fluid shape resolving this session.
    void registerFluids(Set<String> usedFluidNames) {
        fluidsByIndex.clear();
        for (Material material : served.get()) {
            String fluidName = FluidNaming.validate(fluidName(material), this, material, usedFluidNames);
            fluidsByIndex.put(material.getIndex(), registerOrReuse(fluidName, material));
        }
    }

    /// Registers a newly created [MaterialFluid] under `fluidName` and configures it, or -- if that name is already
    /// registered by another mod -- reuses the existing fluid unmodified.
    private Fluid registerOrReuse(String fluidName, Material material) {
        Fluid created = new MaterialFluid(fluidName, material);
        if (FluidRegistry.registerFluid(created)) {
            configurer.configure(material, created);
            return created;
        }
        MaterialLib.LOG.warn(
            "Fluid {} of {} is already registered elsewhere; its tint, name, and icons will not apply",
            fluidName,
            material.getKey());
        return FluidRegistry.getFluid(fluidName);
    }

    /// Registers a [ShapeFluidBlock] for each served material whose fluid is placeable ([FluidPlacement#isPlaceable]),
    /// skipping a fluid this shape did not create.
    void registerBlocks() {
        blocksByIndex.clear();
        FluidFlow flow = getProperty(StandardProperties.FLUID_FLOW);
        for (Material material : served.get()) {
            if (!FluidPlacement.isPlaceable(material, this)) continue;
            Fluid fluid = fluidsByIndex.get(material.getIndex());
            if (!(fluid instanceof MaterialFluid)) {
                MaterialLib.LOG.warn(
                    "Fluid {} of {} is registered elsewhere; it will not be placeable",
                    fluid.getName(),
                    material.getKey());
                continue;
            }
            ShapeFluidBlock block = createBlock(material, fluid, blockKind(material, fluid));
            block.setDensity(flow.blockDensity(fluid.getDensity()));
            block.setQuantaPerBlock(flow.distance());
            blockConfigurer.configure(material, block);
            GameRegistry.registerBlock(block, null, blockName(material));
            blocksByIndex.put(material.getIndex(), block);
        }
    }

    void addContactCallback(FluidContactCallback callback) {
        contactCallbacks.add(callback);
    }

    void addTickCallback(FluidTickCallback callback) {
        tickCallbacks.add(callback);
    }

    void addNeighborCallback(FluidNeighborCallback callback) {
        neighborCallbacks.add(callback);
    }

    /// Runs the attached [FluidContactCallback]s in attachment order until one acts, and returns whether one did.
    boolean runContactCallbacks(Material material, World world, int x, int y, int z, Entity entity) {
        return dispatch(contactCallbacks, callback -> callback.onContact(material, world, x, y, z, entity));
    }

    /// Runs the attached [FluidTickCallback]s in attachment order until one acts, and returns whether one did.
    boolean runTickCallbacks(Material material, World world, int x, int y, int z, Random random) {
        return dispatch(tickCallbacks, callback -> callback.onTick(material, world, x, y, z, random));
    }

    /// Runs the attached [FluidNeighborCallback]s in attachment order until one acts, and returns whether one did.
    boolean runNeighborCallbacks(Material material, World world, int x, int y, int z, Block neighbor) {
        return dispatch(neighborCallbacks, callback -> callback.onNeighborChange(material, world, x, y, z, neighbor));
    }

    private static <C> boolean dispatch(List<C> callbacks, Predicate<C> run) {
        for (C callback : callbacks) {
            if (run.test(callback)) return true;
        }
        return false;
    }

    /// The registry name of `material`'s fluid block.
    String blockName(Material material) {
        return "fluid." + fluidName(material);
    }

    /// The kind of block placed for `material`'s `fluid`, as chosen by this shape's [FluidBlockKindChooser].
    FluidBlockKind blockKind(Material material, Fluid fluid) {
        return Objects.requireNonNull(
            kindChooser.choose(material, fluid),
            "block kind chooser of " + this + " returned null for " + material.getKey());
    }

    /// Creates the block for `material`'s `fluid`. Subclasses override this to supply their own [ShapeFluidBlock].
    protected ShapeFluidBlock createBlock(Material material, Fluid fluid, FluidBlockKind kind) {
        return new ShapeFluidBlock(this, material, fluid, kind);
    }

    /// The block registered for `material` in this shape, or null when the pair is not placeable.
    ShapeFluidBlock block(Material material) {
        return blocksByIndex.get(material.getIndex());
    }

    /// The fluid registered for `material` in this shape, or null when the material does not generate it.
    Fluid fluid(Material material) {
        return fluidsByIndex.get(material.getIndex());
    }

    /// The fluid stack of `material` in this shape, with the given volume in millibuckets. The material must
    /// generate this shape.
    FluidStack fluidStack(Material material, int amount) {
        Fluid fluid = fluidsByIndex.get(material.getIndex());
        if (fluid == null) {
            throw new IllegalArgumentException(
                "Material " + material.getKey() + " does not generate fluid shape " + this);
        }
        return new FluidStack(fluid, amount);
    }

    /// Binds each material's still and flowing fluid icon from [#resolveIconPath]. Fluid textures live on the
    /// block atlas, so this runs from a blocks texture-stitch on the client (see [ShapeFluidIcons]).
    @SideOnly(Side.CLIENT)
    void registerIcons(IIconRegister register) {
        for (Material material : served.get()) {
            Fluid fluid = fluidsByIndex.get(material.getIndex());
            if (!(fluid instanceof MaterialFluid)) continue;
            fluid.setIcons(register.registerIcon(resolveIconPath(material)));
        }
    }

    /// Binds the placeholder to every material's fluid; see [MaterialLibClient#deferIconBinding].
    @SideOnly(Side.CLIENT)
    void registerPlaceholderIcons(IIconRegister register) {
        IIcon empty = register.registerIcon(ShapeIcons.EMPTY_ICON);
        for (Material material : served.get()) {
            Fluid fluid = fluidsByIndex.get(material.getIndex());
            if (fluid instanceof MaterialFluid) fluid.setIcons(empty);
        }
    }

    /// The icon path to register for `material`'s fluid, or the [ShapeIcons#EMPTY_ICON] placeholder when no source
    /// carries one; see [ShapeIcons#resolvePath] for the order. Never null.
    String resolveIconPath(Material material) {
        return resolveIconPath(material, ShapeFluid::blockTextureExists);
    }

    /// As [#resolveIconPath(Material)], with `exists` deciding whether an icon path names a file on the block
    /// atlas.
    String resolveIconPath(Material material, Predicate<String> exists) {
        String path = ShapeIcons.resolvePath(material, List.of(name), this::patherPath, exists);
        if (path != null && path.startsWith(ShapeIcons.OVERRIDE_ROOT)) overrideIcons.add(material);
        else overrideIcons.remove(material);
        if (path != null) return path;
        if (warnedMissingIcon.add(material)) {
            MaterialLib.LOG.warn(
                "Fluid shape {} of {} resolved no icon; it will render the transparent placeholder",
                this,
                material.getKey());
        }
        return ShapeIcons.EMPTY_ICON;
    }

    /// Whether `material`'s fluid icon resolved from the resource-pack override location; see
    /// [ShapeItem#hasOverrideIcon].
    boolean hasOverrideIcon(Material material) {
        return overrideIcons.contains(material);
    }

    private String patherPath(Material material) {
        return iconPather != null ? iconPather.iconPath(this, material) : null;
    }

    private static boolean blockTextureExists(String path) {
        return ShapeIcons.textureExists(path, false);
    }

    /// A material's fluid, serving the display name and fill tint the class doc describes through overrides.
    /// 1.7.10 fluids expose no color setter, only an overridable [Fluid#getColor].
    private final class MaterialFluid extends Fluid {

        private final Material material;

        MaterialFluid(String fluidName, Material material) {
            super(fluidName);
            this.material = material;
            setUnlocalizedName(Names.key(modid, name));
        }

        @Override
        public String getLocalizedName() { return ShapeText.displayName(ShapeFluid.this, material); }

        @Override
        public int getColor() {
            if (hasOverrideIcon(material)) return 0xFFFFFF;
            return tintOf(material) & 0xFFFFFF;
        }
    }

    /// The ARGB fill tint for `material`'s fluid: [StandardProperties#FLUID_TINT] when set, or
    /// [StandardProperties#TINT] otherwise.
    static int tintOf(Material material) {
        Property<Integer> tint = material.getProperty(StandardProperties.FLUID_TINT) != null ?
            StandardProperties.FLUID_TINT : StandardProperties.TINT;
        return MaterialTints.color(material, tint);
    }
}
