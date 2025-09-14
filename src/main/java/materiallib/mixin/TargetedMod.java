package materiallib.mixin;

import org.jetbrains.annotations.NotNull;

import com.gtnewhorizon.gtnhmixins.builders.ITargetMod;
import com.gtnewhorizon.gtnhmixins.builders.TargetModBuilder;

@SuppressWarnings("unused")
public enum TargetedMod implements ITargetMod {

    ADVANCED_SOLAR_PANELS(null, "AdvancedSolarPanel"),
    ANGELICA("com.gtnewhorizons.angelica.loading.AngelicaTweaker", "angelica"),
    ARCHAICFIX("org.embeddedt.archaicfix.ArchaicCore", "archaicfix"),
    AUTOMAGY(null, "Automagy"),
    BAUBLES(null, "Baubles"),
    BETTERHUD(null, "hud"),
    BIBLIOCRAFT(null, "BiblioCraft"),
    BOP(null, "BiomesOPlenty"),
    BUGTORCH("jss.bugtorch.mixinplugin.BugTorchEarlyMixins", "bugtorch"),
    BUKKIT("Bukkit", null),
    COFH_CORE("cofh.asm.LoadingPlugin", "CoFHCore"),
    DAMAGE_INDICATORS(null, "DamageIndicatorsMod"),
    EXTRATIC(null, "ExtraTiC"),
    EXTRA_UTILITIES(null, "ExtraUtilities"),
    FASTCRAFT("fastcraft.Tweaker"),
    GALACTICRAFT_CORE("micdoodle8.mods.galacticraft.core.asm.GCLoadingPlugin", "GalacticraftCore"),
    GT5U(null, "gregtech_nh"),
    GT6("gregtech.asm.GT_ASM", "gregapi"),
    GTNHLIB("com.gtnewhorizon.gtnhlib.core.GTNHLibCore", "gtnhlib"),
    HARVESTCRAFT(null, "harvestcraft"),
    HARVESTTHENETHER(null, "harvestthenether"),
    HUNGER_OVERHAUL(null, "HungerOverhaul"),
    IC2("ic2.core.coremod.IC2core", "IC2"),
    IMMERSIVE_ENGINENEERING(null, "ImmersiveEngineering"),
    JOURNEYMAP(null, "journeymap"),
    LOTR("lotr.common.coremod.LOTRLoadingPlugin", "lotr"),
    LWJGL3IFY("me.eigenraven.lwjgl3ify.core.Lwjgl3ifyCoremod", "lwjgl3ify"),
    MINECHEM(null, "minechem"),
    MINEFACTORY_RELOADED(null, "MineFactoryReloaded"),
    MRTJPCORE(null, "MrTJPCoreMod"),
    NOTENOUGHITEMS("codechicken.nei.asm.NEICorePlugin", "NotEnoughItems"),
    OPTIFINE("optifine.OptiFineForgeTweaker", "Optifine"),
    PORTAL_GUN(null, "PortalGun"),
    PROJECTE(null, "ProjectE"),
    RAILCRAFT(null, "Railcraft"),
    THAUMCRAFT(null, "Thaumcraft"),
    THERMALDYNAMICS(null, "ThermalDynamics"),
    THERMALEXPANSION(null, "ThermalExpansion"),
    TINKERSCONSTRUCT(null, "TConstruct"),
    TRAVELLERSGEAR(null, "TravellersGear"),
    VANILLA(null),
    VOXELMAP("com.thevoxelbox.voxelmap.litemod.VoxelMapTransformer", "voxelmap"),
    WITCHERY(null, "witchery"),
    XAEROWORLDMAP(null, "XaeroWorldMap"),
    ZTONES(null, "Ztones");

    private final TargetModBuilder builder;

    TargetedMod(String coreModClass) {
        this(coreModClass, null);
    }

    TargetedMod(String coreModClass, String modId) {
        this.builder = new TargetModBuilder().setModId(modId).setCoreModClass(coreModClass);
    }

    @Override
    public @NotNull TargetModBuilder getBuilder() {
        return builder;
    }
}
