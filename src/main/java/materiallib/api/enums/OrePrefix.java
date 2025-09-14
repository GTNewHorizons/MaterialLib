package materiallib.api.enums;

import static materiallib.api.misc.MaterialLibValues.M;

import net.minecraft.item.ItemStack;
import net.minecraftforge.oredict.OreDictionary;

import com.google.common.collect.ImmutableList;
import it.unimi.dsi.fastutil.Pair;
import it.unimi.dsi.fastutil.objects.Object2ObjectLinkedOpenHashMap;
import materiallib.api.data.ItemId;

public enum OrePrefix {

    oreBlackgranite("Black Granite Ores", -1),
    oreRedgranite("Red Granite Ores", -1),
    oreMarble("Marble Ores", -1),
    oreBasalt("Basalt Ores", -1),
    oreNetherrack("Netherrack Ores", -1),
    oreNether("Nether Ores", -1),
    oreDense("Dense Ores", -1),
    /** Prefix of TFC */
    oreRich("Rich Ores", -1),
    /** Prefix of TFC */
    oreNormal("Normal Ores", -1),
    /** Prefix of Railcraft. */
    oreSmall("Small Ores", -1),
    /** Prefix of Railcraft. */
    orePoor("Poor Ores", -1),
    oreEndstone("Endstone Ores", -1),
    oreEnd("End Ores", -1),
    ore("Ores", -1),
    crushedCentrifuged("Centrifuged Ores", -1),
    crushedPurified("Purified Ores", -1),
    crushed("Crushed Ores", -1),
    rawOre("Raw Ore", -1),

    /** Introduced by Mekanism */
    shard("Crystallised Shards", -1),
    clump("Clumps", -1),
    reduced("Reduced Gravels", -1),
    crystalline("Crystallised Metals", -1),
    cleanGravel("Clean Gravels", -1),
    dirtyGravel("Dirty Gravels", -1),
    /** A hot Ingot, which has to be cooled down by a Vacuum Freezer. */
    ingotHot("Hot Ingots", M),
    /** A regular Ingot. Introduced by Eloraam */
    ingot("Ingots", M),
    /** A regular Gem worth one small Dust. Introduced by TerraFirmaCraft */
    gemChipped("Chipped Gemstones", M / 4),
    /** A regular Gem worth two small Dusts. Introduced by TerraFirmaCraft */
    gemFlawed("Flawed Gemstones", M / 2),
    /** A regular Gem worth two Dusts. Introduced by TerraFirmaCraft */
    gemFlawless("Flawless Gemstones", M * 2),
    /** A regular Gem worth four Dusts. Introduced by TerraFirmaCraft */
    gemExquisite("Exquisite Gemstones", M * 4),
    /** A regular Gem worth one Dust. Introduced by Eloraam */
    gem("Gemstones", M),
    /** 1/9th of a Dust. */
    dustTiny("Tiny Dusts", M / 9),
    /** 1/4th of a Dust. */
    dustSmall("Small Dusts", M / 4),
    /** Dust with impurities. 1 Unit of Main Material and 1/9 - 1/4 Unit of secondary Material */
    dustImpure("Impure Dusts", M),
    dustRefined("Refined Dusts", M),
    dustPure("Purified Dusts", M),
    /** Pure Dust worth of one Ingot or Gem. Introduced by Alblaka. */
    dust("Dusts", M),
    /** A Nugget. Introduced by Eloraam */
    nugget("Nuggets", M / 9),
    /** Special Alloys have this prefix. */
    plateAlloy("Alloy Plates", -1),
    plateSteamcraft("Steamcraft Plates", -1),
    /** 9 Plates combined in one Item. */
    plateDense("Dense Plates", M * 9),
    plateSuperdense("Superdense Plates", M * 64),
    plateQuintuple("5x Plates", M * 5),
    plateQuadruple("4x Plates", M * 4),
    plateTriple("3x Plates", M * 3),
    plateDouble("2x Plates", M * 2),
    plate("Plates", M),
    /** Casing made of 1/2 Ingot/Dust */
    itemCasing("Casings", M / 2),
    /** Foil made of 1/4 Ingot/Dust. */
    foil("Foils", M / 4),
    /** Stick made of an Ingot. */
    stickLong("Long Sticks/Rods", M),
    /** Stick made of half an Ingot. Introduced by Eloraam */
    stick("Sticks/Rods", M / 2),
    /** consisting out of one Nugget. */
    round("Rounds", M / 9),
    /** consisting out of 1/8 Ingot or 1/4 Stick. */
    bolt("Bolts", M / 8),
    /** contain dusts */
    comb("Combs", M),
    /** consisting out of a Bolt. */
    screw("Screws", M / 9),
    /** consisting out of 1/2 Stick. */
    ring("Rings", M / 4),
    /** consisting out of 1 Fine Wire. */
    springSmall("Small Springs", M / 4),
    /** consisting out of 2 Sticks. */
    spring("Springs", M),
    /** consisting out of 1/8 Ingot or 1/4 Wire. */
    wireFine("Fine Wires", M / 8),
    /** consisting out of 4 Plates, 1 Ring and 1 Screw. */
    rotor("Rotors", M * 4 + M / 4),
    gearGtSmall("Small Gears", M),
    /** Introduced by me because BuildCraft has ruined the gear Prefix... */
    gearGt("Gears", M * 4),
    /** 3/4 of a Plate or Gem used to shape a Lense. Normally only used on Transparent Materials. */
    lens("Lenses", (M * 3) / 4),
    /** Hot Cell full of Plasma, which can be used in the Plasma Generator. */
    cellPlasma("Cells of Plasma", M),
    /** Hot Cell full of molten stuff, which can be used in the Plasma Generator. */
    cellMolten("Cells of Molten stuff", M),
    cell("Cells", M),
    /** A vanilla Iron Bucket filled with the Material. */
    bucket("Buckets", M),
    /** An Iguana Tweaks Clay Bucket filled with the Material. */
    bucketClay("Clay Buckets", M),
    /** Glass Bottle containing a Fluid. */
    bottle("Bottles", -1),
    capsule("Capsules", M),
    crystal("Crystals", M),
    bulletGtSmall("Small Bullets", M / 9),
    bulletGtMedium("Medium Bullets", M / 6),
    bulletGtLarge("Large Bullets", M / 3),
    /** consisting out of 2 Ingots. */
    toolHeadFile("File Heads", M * 2),
    /** consisting out of 6 Ingots. */
    toolHeadHammer("Hammer Heads", M * 6),
    /** consisting out of 2 Ingots. */
    toolHeadSaw("Saw Blades", M * 2),
    /** consisting out of 4 Ingots. */
    toolHeadBuzzSaw("Buzzsaw Blades", M * 4),
    /** consisting out of 1 Ingots. */
    toolHeadScrewdriver("Screwdriver Tips", M),
    /** consisting out of 4 Ingots. */
    toolHeadDrill("Drill Tips", M * 4),
    /** consisting out of 2 Ingots. */
    toolHeadChainsaw("Chainsaw Tips", M * 2),
    /** consisting out of 4 Ingots. */
    toolHeadWrench("Wrench Tips", M * 4),
    /** consisting out of 6 Ingots. */
    turbineBlade("Turbine Blades", M * 6),
    /** vanilly Sword */
    toolSword("Swords", M * 2),
    /** vanilly Pickaxe */
    toolPickaxe("Pickaxes", M * 3),
    /** vanilly Shovel */
    toolShovel("Shovels", M),
    /** vanilly Axe */
    toolAxe("Axes", M * 3),
    /** vanilly Hoe */
    toolHoe("Hoes", M * 2),
    /** vanilly Shears */
    toolShears("Shears", M * 2),
    /**
     * toolPot, toolSkillet, toolSaucepan, toolBakeware, toolCuttingboard, toolMortarandpestle, toolMixingbowl,
     * toolJuicer
     */
    tool("Tools", -1),
    compressedCobblestone("9^X Compressed Cobblestones", -1),
    compressedStone("9^X Compressed Stones", -1),
    compressedDirt("9^X Compressed Dirt", -1),
    compressedGravel("9^X Compressed Gravel", -1),
    compressedSand("9^X Compressed Sand", -1),
    /** Compressed Material, worth 1 Unit. Introduced by Galacticraft */
    compressed("Compressed Materials", M * 3),
    glass("Glasses", -1),
    paneGlass("Glass Panes", -1),
    blockGlass("Glass Blocks", -1),
    blockWool("Wool Blocks", -1),
    /** IGNORE */
    block_("Random Blocks", -1),
    /** Storage Block consisting out of 9 Ingots/Gems/Dusts. Introduced by CovertJaguar */
    block("Storage Blocks", M * 9),
    /** Special Prefix used mainly for the Crafting Handler. */
    craftingTool("Crafting Tools", -1),
    /** Special Prefix used mainly for the Crafting Handler. */
    crafting("Crafting Ingredients", -1),
    /** Special Prefix used mainly for the Crafting Handler. */
    craft("Crafting Stuff?", -1),
    /** Prefix used for Logs. Usually as "logWood". Introduced by Eloraam */
    log("Logs", -1),
    /** Prefix used for Slabs. Usually as "slabWood" or "slabStone". Introduced by SirSengir */
    slab("Slabs", -1),
    /** Prefix used for Stairs. Usually as "stairWood" or "stairStone". Introduced by SirSengir */
    stair("Stairs", -1),
    /** Prefix used for Fences. Usually as "fenceWood". Introduced by Forge */
    fence("Fences", -1),
    /** Prefix for Planks. Usually "plankWood". Introduced by Eloraam */
    plank("Planks", -1),
    /** Prefix for Saplings. */
    treeSapling("Saplings", -1),
    /** Prefix for Leaves. */
    treeLeaves("Leaves", -1),
    /** Prefix for Tree Parts. */
    tree("Tree Parts", -1),
    /** Cobblestone Prefix for all Cobblestones. */
    stoneCobble("Cobblestones", -1),
    /** Smoothstone Prefix. */
    stoneSmooth("Smoothstones", -1),
    /** Mossy Stone Bricks. */
    stoneMossyBricks("mossy Stone Bricks", -1),
    /** Mossy Cobble. */
    stoneMossy("Mossy Stones", -1),
    /** Stone Bricks. */
    stoneBricks("Stone Bricks", -1),
    /** Cracked Bricks. */
    stoneCracked("Cracked Stones", -1),
    /** Chiseled Stone. */
    stoneChiseled("Chiseled Stones", -1),
    /** Prefix to determine which kind of Rock this is. */
    stone("Stones", -1),
    cobblestone("Cobblestones", -1),
    /** Prefix to determine which kind of Rock this is. */
    rock("Rocks", -1),
    record("Records", -1),
    rubble("Rubbles", -1),
    scraps("Scraps", -1),
    scrap("Scraps", -1),
    /** IGNORE */
    item_("Items", -1),
    /** Random Item. Introduced by Alblaka */
    item("Items", -1),
    /** Used for Books of any kind. */
    book("Books", -1),
    /** Used for Papers of any kind. */
    paper("Papers", -1),
    /** Used for the 16 dyes. Introduced by Eloraam */
    dye("Dyes", -1),
    /** Used for the 16 colors of Stained Clay. Introduced by Forge */
    stainedClay("Stained Clays", -1),
    /** vanilly Helmet */
    armorHelmet("Helmets", M * 5),
    /** vanilly Chestplate */
    armorChestplate("Chestplates", M * 8),
    /** vanilly Pants */
    armorLeggings("Leggings", M * 7),
    /** vanilly Boots */
    armorBoots("Boots", M * 4),
    armor("Armor Parts", -1),
    frameGt("Frame Boxes", M * 2),
    pipeTiny("Tiny Pipes", M / 2),
    pipeSmall("Small Pipes", M),
    pipeMedium("Medium Pipes", M * 3),
    pipeLarge("Large pipes", M * 6),
    pipeHuge("Huge Pipes", M * 12),
    pipeQuadruple("Quadruple Pipes", M * 12),
    pipeNonuple("Nonuple Pipes", M * 9),
    pipeRestrictiveTiny("Tiny Restrictive Pipes", M / 2),
    pipeRestrictiveSmall("Small Restrictive Pipes", M),
    pipeRestrictiveMedium("Medium Restrictive Pipes", M * 3),
    pipeRestrictiveLarge("Large Restrictive Pipes", M * 6),
    pipeRestrictiveHuge("Huge Restrictive Pipes", M * 12),
    pipe("Pipes", -1),
    wireGt16("16x Wires", M * 8),
    wireGt12("12x Wires", M * 6),
    wireGt08("8x Wires", M * 4),
    wireGt04("4x Wires", M * 2),
    wireGt02("2x Wires", M),
    wireGt01("1x Wires", M / 2),
    cableGt16("16x Cables", M * 8),
    cableGt12("12x Cables", M * 6),
    cableGt08("8x Cables", M * 4),
    cableGt04("4x Cables", M * 2),
    cableGt02("2x Cables", M),
    cableGt01("1x Cables", M / 2),

    /*
     * Electric Components. usual Materials for this are: Primitive (Tier 1) Basic (Tier 2) as used by UE as well : IC2
     * Circuit and RE-Battery Good (Tier 3) Advanced (Tier 4) as used by UE as well : Advanced Circuit, Advanced Battery
     * and Lithium Battery Data (Tier 5) : Data Storage Circuit Elite (Tier 6) as used by UE as well : Energy Crystal
     * and Data Control Circuit Master (Tier 7) : Energy Flow Circuit and Lapotron Crystal Ultimate (Tier 8) : Data Orb
     * and Lapotronic Energy Orb Infinite (Cheaty)
     */
    batterySingleuse("Single Use Batteries", -1),
    battery("Reusable Batteries", -1),
    circuit("Circuits", -1),
    /** Introduced by Buildcraft */
    chipset("Chipsets", -1),
    /** A whole Computer. "computerMaster" = ComputerCube */
    computer("Computers", -1),

    // random known prefixes without special abilities.
    skull("Skulls", -1),
    plating("Platings", -1),
    dinosaur("Dinosaurs", -1),
    travelgear("Travel Gear", -1),
    bauble("Baubles", -1),
    cluster("Clusters", -1),
    grafter("Grafters", -1),
    scoop("Scoops", -1),
    frame("Frames", -1),
    tome("Tomes", -1),
    junk("Junk", -1),
    bee("Bees", -1),
    rod("Rods", -1),
    dirt("Dirts", -1),
    sand("Sands", -1),
    grass("Grasses", -1),
    gravel("Gravels", -1),
    mushroom("Mushrooms", -1),
    /** Introduced by Eloraam */
    wood("Woods", -1),
    drop("Drops", -1),
    fuel("Fuels", -1),
    panel("Panels", -1),
    brick("Bricks", -1),
    chunk("Chunks", -1),
    wire("Wires", -1),
    seed("Seeds", -1),
    reed("Reeds", -1),
    sheetDouble("2x Sheets", -1),
    sheet("Sheets", -1),
    crop("Crops", -1),
    plant("Plants", -1),
    coin("Coins", -1),
    lumar("Lumars", -1),
    ground("Grounded Stuff", -1),
    cable("Cables", -1),
    component("Components", -1),
    wax("Waxes", -1),
    wall("Walls", -1),
    tube("Tubes", -1),
    list("Lists", -1),
    food("Foods", -1),
    /** Introduced by SirSengir */
    gear("Gears", -1),
    coral("Corals", -1),
    flower("Flowers", -1),
    storage("Storages", -1),
    material("Materials", -1),
    plasma("Plasmas", -1),
    element("Elements", -1),
    molecule("Molecules", -1),
    wafer("Wafers", -1),
    orb("Orbs", -1),
    handle("Handles", -1),
    blade("Blades", -1),
    head("Heads", -1),
    motor("Motors", -1),
    bit("Bits", -1),
    shears("Shears", -1),
    turbine("Turbines", -1),
    fertilizer("Fertilizers", -1),
    chest("Chests", -1),
    raw("Raw Things", -1),
    stainedGlass("Stained Glasses", -1),
    mystic("Mystic Stuff", -1),
    mana("Mana Stuff", -1),
    rune("Runes", -1),
    petal("Petals", -1),
    pearl("Pearls", -1),
    powder("Powders", -1),
    soulsand("Soulsands", -1),
    obsidian("Obsidians", -1),
    glowstone("Glowstones", -1),
    beans("Beans", -1),
    br("br", -1),
    essence("Essences", -1),
    alloy("Alloys", -1),
    cooking("Cooked Things", -1),
    elven("Elven Stuff", -1),
    reactor("Reactors", -1),
    mffs("MFFS", -1),
    projred("Project Red", -1),
    ganys("Ganys Stuff", -1),
    liquid("Liquids", -1),
    bars("Bars", -1),
    bar("Bars", -1),
    /** Reverse Head consisting out of 6 Ingots. */
    toolHeadMallet("Mallet Heads", M * 6),
    /** Reverse Stick made of half an Ingot. Introduced by Eloraam */
    handleMallet("Mallet Handle", M / 2),

    // Cracked fluids
    cellHydroCracked1("Cells", M),
    cellHydroCracked2("Cells", M),
    cellHydroCracked3("Cells", M),
    cellSteamCracked1("Cells", M),
    cellSteamCracked2("Cells", M),
    cellSteamCracked3("Cells", M),

    componentCircuit("Circuit Parts", -1),

    apiaryUpgrade("Industrial Apiary Upgrade", -1),
    beeComb("Bee Combs", -1),
    nanite("Nanites", -1),
    // migrated from GT++
    milled("Milled Ores", -1),
    // migrated from bartworks
    blockCasing("A Casing block for a Multiblock-Machine", M * 9),
    blockCasingAdvanced("An Advanced Casing block for a Multiblock-Machine", M * 9),
    capsuleMolten("Capsule of Molten stuff", M),
    // subatomic particles
    particle("A Subatomic Particle", -1),
    // Beamline Masks
    mask("A Photolithographic Mask", -1),
    wrapCircuit("A Circuit Wrap", -1);

    public final String description;

    /**
     * Used to determine the amount of Material this Prefix contains. Multiple of {@link materiallib.api.misc.MaterialLibValues#M}.
     * 0 = Null. Negative = Not Applicable (prefix is constructed of a material but doesn't have a well-defined amount).
     */
    public final long materialAmount;

    OrePrefix(String description, long materialAmount) {
        this.materialAmount = materialAmount;
        this.description = description;
    }

    public static String stripPrefix(String aOre) {
        for (OrePrefix tPrefix : values()) {
            if (aOre.startsWith(tPrefix.toString())) {
                return aOre.replaceFirst(tPrefix.toString(), "");
            }
        }
        return aOre;
    }

    public static Pair<OrePrefix, String> detectPrefix(String oredictName) {
        for (OrePrefix prefix : values()) {
            if (oredictName.startsWith(prefix.name())) {
                return Pair.of(
                    prefix,
                    oredictName.substring(
                        prefix.name()
                            .length()));
            }
        }

        return null;
    }

    private static final ThreadLocal<Object2ObjectLinkedOpenHashMap<ItemId, ImmutableList<Pair<OrePrefix, String>>>> PREFIX_CACHE = ThreadLocal
        .withInitial(Object2ObjectLinkedOpenHashMap::new);

    public static ImmutableList<Pair<OrePrefix, String>> detectPrefix(ItemStack stack) {
        Object2ObjectLinkedOpenHashMap<ItemId, ImmutableList<Pair<OrePrefix, String>>> cache = PREFIX_CACHE.get();

        ItemId itemId = materiallib.api.data.ItemId.create(stack);

        var cacheResult = cache.getAndMoveToFirst(itemId);

        if (cacheResult != null) return cacheResult;

        ImmutableList.Builder<Pair<OrePrefix, String>> result = ImmutableList.builder();

        for (int id : OreDictionary.getOreIDs(stack)) {
            Pair<OrePrefix, String> p = detectPrefix(OreDictionary.getOreName(id));

            if (p != null) {
                result.add(p);
            }
        }

        ImmutableList<Pair<OrePrefix, String>> prefixes = result.build();

        cache.putAndMoveToFirst(itemId, prefixes);

        while (cache.size() > 1024) {
            cache.removeLast();
        }

        return prefixes;
    }
}
