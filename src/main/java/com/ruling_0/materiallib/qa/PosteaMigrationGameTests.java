package com.ruling_0.materiallib.qa;

import java.io.File;
import java.io.FileInputStream;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.Map;
import java.util.UUID;

import net.minecraft.block.Block;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.entity.player.EntityPlayerMP;
import net.minecraft.init.Blocks;
import net.minecraft.inventory.IInventory;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.CompressedStreamTools;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.server.MinecraftServer;
import net.minecraft.world.World;
import net.minecraft.world.WorldSavedData;
import net.minecraft.world.WorldServer;
import net.minecraft.world.storage.IPlayerFileData;

import net.minecraftforge.common.util.FakePlayer;
import net.minecraftforge.common.util.FakePlayerFactory;

import com.gtnewhorizons.horizonqa.api.GameTestHelper;
import com.gtnewhorizons.horizonqa.api.annotation.GameTest;
import com.gtnewhorizons.horizonqa.api.annotation.GameTestHolder;
import com.gtnewhorizons.postea.api.IDExtenderCompat;
import com.gtnewhorizons.postea.utility.VersionStamps;
import com.mojang.authlib.GameProfile;
import com.ruling_0.materiallib.MaterialLib;
import com.ruling_0.materiallib.api.StackResolver;
import com.ruling_0.materiallib.api.WorldMaterialIds;

import codechicken.enderstorage.api.EnderStorageManager;
import codechicken.enderstorage.storage.item.EnderItemStorage;
import openblocks.common.PlayerInventoryStore;

/// In-world checks that [com.ruling_0.materiallib.PosteaMigration] reaches chunks, player data and mod-private
/// storage.
///
/// `qa/postea-migration/run.sh` seeds each storage with a witness stack saved under an older material id list
/// version. The chunk and Forestry tests seed their own storages on the first boot instead. Each test reads the
/// storage through the owning mod's load path and asserts the stack holds the witness material at the current list
/// version. A test with no seed is skipped. The harness requires a "verified" log line from every test.
@GameTestHolder(
                value = MaterialLib.MODID,
                requiredMods = { "postea", "EnderStorage", "OpenBlocks", "Backpack", "gregtech", "Baubles",
                    "cosmeticarmorreworked", "Forestry" })
public class PosteaMigrationGameTests {

    private static final String BATCH = "materiallib.postea";
    private static final String WITNESS_MATERIAL = System.getProperty("materiallib.qa.witnessMaterial", "Tin");
    private static final String WITNESS_SHAPE = System.getProperty("materiallib.qa.witnessShape", "ingot");
    private static final String WITNESS_BLOCK_SHAPE = System.getProperty("materiallib.qa.witnessBlockShape", "frameGt");
    /// A material whose witness-shape pair is served by a shape override. The seeded EnderStorage slot 1 holds it.
    private static final String OVERRIDE_MATERIAL = System.getProperty("materiallib.qa.overrideMaterial", "Iron");
    /// The registry name of the item overriding that pair.
    private static final String OVERRIDE_ITEM = System.getProperty("materiallib.qa.overrideItem",
        "minecraft:iron_ingot");

    /// The player whose Baubles and Cosmetic Armor files the harness seeds.
    private static final GameProfile FILES_PLAYER = new GameProfile(
        UUID.fromString("11111111-2222-3333-4444-666666666666"),
        "qa-postea-files");
    /// The player of the seeded player tag, who also owns the Forestry post box and trade station.
    private static final GameProfile PLAYER = new GameProfile(
        UUID.fromString("11111111-2222-3333-4444-555555555555"),
        "qa-postea");
    private static final String TRADER = "qapostea";
    /// A stamp the harness seeds on the player tag for a transformer that is never registered. Saves must carry it.
    private static final String ABSENT_STAMP_KEY = "postea_qa:absent";
    private static final int ABSENT_STAMP_VERSION = 7;

    /// Where the chunk test keeps its witness block and chest across boots: inside the always-loaded spawn area,
    /// below the test grid.
    private static final int CHUNK_X = 40, CHUNK_Y = 5, CHUNK_Z = 40;

    /// Returns the stack a seeded slot should hold: the witness material's shape stack at the current list version.
    private static ItemStack witness(GameTestHelper helper) {
        ItemStack expected = StackResolver.getStack(WITNESS_MATERIAL, WITNESS_SHAPE, 1);
        if (expected == null) {
            helper.fail("Witness " + WITNESS_MATERIAL + ":" + WITNESS_SHAPE + " does not resolve");
        }
        return expected;
    }

    private static WorldServer overworld() {
        return MinecraftServer.getServer()
            .worldServerForDimension(0);
    }

    private static void unseeded(GameTestHelper helper, String storage) {
        helper.assumeTrue(false, "no seed for " + storage);
    }

    private static void verified(GameTestHelper helper, String... storages) {
        for (String storage : storages) MaterialLib.LOG.info("[{}] verified {}", BATCH, storage);
        helper.succeed();
    }

    private static void assertWitnessStack(GameTestHelper helper, ItemStack actual, String storage) {
        helper.assertTrue(actual != null, storage + " slot is empty");
        ItemStack expected = witness(helper);
        helper.assertEquals(expected.getItem(), actual.getItem(), storage + " item");
        helper.assertEquals(expected.getItemDamage(), actual.getItemDamage(), storage + " damage");
    }

    /// Checks that EnderStorage's transform, deferred to the first server world load, reached the seeded frequency.
    @GameTest(batch = BATCH)
    public static void seededEnderChestHoldsTheWitnessMaterial(GameTestHelper helper) {
        EnderItemStorage storage = (EnderItemStorage) EnderStorageManager.instance(false)
            .getStorage("global", 0, "item");
        ItemStack actual = storage.getStackInSlot(0);
        if (actual == null) {
            unseeded(helper, "enderstorage:global");
            return;
        }
        assertWitnessStack(helper, actual, "enderstorage:global");
        ItemStack overridden = storage.getStackInSlot(1);
        helper.assertTrue(overridden != null, "seeded override slot 1 is empty");
        ItemStack expected = StackResolver.getStack(OVERRIDE_MATERIAL, WITNESS_SHAPE, 1);
        helper.assertTrue(expected != null, "override witness " + OVERRIDE_MATERIAL + " does not resolve");
        helper.assertEquals(
            OVERRIDE_ITEM,
            Item.itemRegistry.getNameForObject(expected.getItem()),
            "override witness " + OVERRIDE_MATERIAL + " item");
        helper.assertEquals(expected.getItem(), overridden.getItem(), "enderstorage:global override item");
        helper.assertEquals(expected.getItemDamage(), overridden.getItemDamage(),
            "enderstorage:global override damage");
        verified(helper, "enderstorage:global");
    }

    /// Checks that a seeded OpenBlocks inventory dump reads back with the witness material. Dumps are never rewritten,
    /// and the transform runs on every read.
    @GameTest(batch = BATCH)
    public static void seededInventoryDumpRestoresTheWitnessMaterial(GameTestHelper helper) {
        PlayerInventoryStore.LoadedInventories loaded = PlayerInventoryStore.instance
            .loadInventories(overworld(), "qa-postea-death-0");
        if (loaded == null || loaded.mainInventory == null) {
            unseeded(helper, "openblocks:inventory");
            return;
        }
        assertWitnessStack(helper, loaded.mainInventory.getStackInSlot(0), "openblocks:inventory");
        verified(helper, "openblocks:inventory");
    }

    /// Checks the seeded backpack file, loaded through Backpack's `SaveFileHandler` by reflection since Backpack is
    /// not a compile dependency.
    @GameTest(batch = BATCH)
    public static void seededBackpackFileHoldsTheWitnessMaterial(GameTestHelper helper) throws Exception {
        Object handler = Class.forName("de.eydamos.backpack.Backpack")
            .getField("saveFileHandler")
            .get(null);
        NBTTagCompound data = (NBTTagCompound) handler.getClass()
            .getMethod("loadBackpack", String.class)
            .invoke(handler, "11111111-2222-3333-4444-555555555555");
        if (data == null || !data.hasKey("Items")) {
            unseeded(helper, "backpack");
            return;
        }
        NBTTagCompound tag = data.getTagList("Items", 10)
            .getCompoundTagAt(0);
        ItemStack expected = witness(helper);
        helper.assertEquals(
            Item.getIdFromItem(expected.getItem()),
            IDExtenderCompat.getItemStackID(tag),
            "backpack item id");
        helper.assertEquals(expected.getItemDamage(), tag.getShort("Damage"), "backpack damage");
        verified(helper, "backpack");
    }

    /// Checks the seeded linked input bus channel, read by reflection since the bus table's inventory type is private.
    @GameTest(batch = BATCH)
    public static void seededLinkedInputBusHoldsTheWitnessMaterial(GameTestHelper helper) throws Exception {
        Class<?> worldSave = Class.forName("ggfab.mte.MTELinkedInputBus$WorldSave");
        WorldSavedData save = overworld().loadItemData(
            worldSave.asSubclass(WorldSavedData.class),
            "LinkedInputBusses");
        if (save == null) {
            unseeded(helper, "gregtech:linkedInputBusses");
            return;
        }
        Field dataField = worldSave.getDeclaredField("data");
        dataField.setAccessible(true);
        Object shared = ((Map<?, ?>) dataField.get(save)).get("qa");
        if (shared == null) {
            unseeded(helper, "gregtech:linkedInputBusses");
            return;
        }
        Field stacksField = shared.getClass()
            .getDeclaredField("stacks");
        stacksField.setAccessible(true);
        assertWitnessStack(helper, ((ItemStack[]) stacksField.get(shared))[0], "gregtech:linkedInputBusses");
        verified(helper, "gregtech:linkedInputBusses");
    }

    /// Checks a witness shape block and chest stack saved in a chunk, seeding them itself. The first run places both
    /// near spawn, then reports itself skipped. Later runs read both back from the saved chunk and assert they hold
    /// the witness material at the current list version.
    @GameTest(batch = BATCH)
    public static void worldChunkKeepsTheWitnessMaterial(GameTestHelper helper) {
        WorldServer world = overworld();
        ItemStack blockStack = StackResolver.getStack(WITNESS_MATERIAL, WITNESS_BLOCK_SHAPE, 1);
        if (blockStack == null) {
            helper.fail("Witness " + WITNESS_MATERIAL + ":" + WITNESS_BLOCK_SHAPE + " does not resolve");
        }
        Block block = Block.getBlockFromItem(blockStack.getItem());
        if (world.getBlock(CHUNK_X, CHUNK_Y, CHUNK_Z) != block) {
            world.setBlock(CHUNK_X, CHUNK_Y, CHUNK_Z, block, blockStack.getItemDamage(), 2);
            world.setBlock(CHUNK_X + 1, CHUNK_Y, CHUNK_Z, Blocks.chest, 0, 2);
            IInventory chest = (IInventory) world.getTileEntity(CHUNK_X + 1, CHUNK_Y, CHUNK_Z);
            chest.setInventorySlotContents(0, witness(helper));
            MaterialLib.LOG
                .info("[{}] seeded chunk witnesses at {},{},{}", BATCH, CHUNK_X, CHUNK_Y, CHUNK_Z);
            helper.assumeTrue(false, "seeded the chunk witnesses; the next boot verifies them");
            return;
        }
        helper.assertEquals(
            blockStack.getItemDamage(),
            world.getBlockMetadata(CHUNK_X, CHUNK_Y, CHUNK_Z),
            "chunk block metadata");
        IInventory chest = (IInventory) world.getTileEntity(CHUNK_X + 1, CHUNK_Y, CHUNK_Z);
        helper.assertTrue(chest != null, "witness chest is missing");
        assertWitnessStack(helper, chest.getStackInSlot(0), "chunk chest stack");
        verified(helper, "chunk");
    }

    /// Loads the seeded player tag into a fake player through `Entity.readFromNBT`, the hook real player files take,
    /// and checks the witness stack. Then respawns the player into a fresh one, as a death does, writes that one out
    /// and checks its version stamps.
    @GameTest(batch = BATCH)
    public static void seededPlayerDataHoldsTheWitnessMaterial(GameTestHelper helper) throws Exception {
        File file = new File(
            overworld().getSaveHandler()
                .getWorldDirectory(),
            "qa-postea-player.dat");
        if (!file.isFile()) {
            unseeded(helper, "player");
            return;
        }
        NBTTagCompound tag;
        try (FileInputStream stream = new FileInputStream(file)) {
            tag = CompressedStreamTools.readCompressed(stream);
        }
        EntityPlayerMP player = FakePlayerFactory.get(overworld(), PLAYER);
        player.readFromNBT(tag);
        assertWitnessStack(helper, player.inventory.getStackInSlot(0), "player inventory");
        EntityPlayerMP respawned = new FakePlayer(overworld(), player.getGameProfile());
        respawned.clonePlayer(player, false);
        NBTTagCompound out = new NBTTagCompound();
        respawned.writeToNBT(out);
        NBTTagCompound stamps = VersionStamps.read(out);
        helper.assertEquals(
            WorldMaterialIds.currentListVersion(),
            VersionStamps.stored(stamps, "materiallib:idList"),
            "respawned player stamp");
        helper.assertEquals(
            ABSENT_STAMP_VERSION,
            VersionStamps.stored(stamps, ABSENT_STAMP_KEY),
            "respawned player stamp of an unregistered transformer");
        verified(helper, "player");
    }

    /// Loads a player through the server's player file handler, as a login does, which also reads the seeded Baubles
    /// and Cosmetic Armor files, and checks the witness in both. Then saves the player as a logout does.
    @GameTest(batch = BATCH)
    public static void seededPlayerFilesHoldTheWitnessMaterial(GameTestHelper helper) throws Exception {
        File playerDirectory = new File(
            overworld().getSaveHandler()
                .getWorldDirectory(),
            "playerdata");
        if (!new File(playerDirectory, FILES_PLAYER.getName() + ".baub").isFile()) {
            unseeded(helper, "baubles");
            return;
        }
        EntityPlayerMP player = new FakePlayer(overworld(), FILES_PLAYER);
        IPlayerFileData files = overworld().getSaveHandler()
            .getSaveHandler();
        files.readPlayerData(player);
        IInventory baubles = (IInventory) Class.forName("baubles.common.lib.PlayerHandler")
            .getMethod("getPlayerBaubles", EntityPlayer.class)
            .invoke(null, player);
        Object cosmeticArmor = Class.forName("lain.mods.cos.CosmeticArmorReworked")
            .getField("invMan")
            .get(null);
        IInventory cosmetics = (IInventory) cosmeticArmor.getClass()
            .getMethod("getCosArmorInventory", UUID.class)
            .invoke(cosmeticArmor, FILES_PLAYER.getId());
        assertWitnessStack(helper, baubles.getStackInSlot(0), "baubles");
        assertWitnessStack(helper, cosmetics.getStackInSlot(0), "cosarmor");
        files.writePlayerData(player);
        verified(helper, "baubles", "cosarmor");
    }

    /// Checks a Forestry post box and trade station, seeding them itself. The first run stores the witness in both and
    /// reports itself skipped. Later runs read both back through Forestry's post registry.
    @GameTest(batch = BATCH)
    public static void forestryMailKeepsTheWitnessMaterial(GameTestHelper helper) throws Exception {
        World world = overworld();
        Object registry = Class.forName("forestry.api.mail.PostManager")
            .getField("postRegistry")
            .get(null);
        Class<?> registryType = Class.forName("forestry.api.mail.IPostRegistry");
        Class<?> addressType = Class.forName("forestry.api.mail.IMailAddress");
        Class<?> postRegistry = Class.forName("forestry.mail.PostRegistry");
        Object boxAddress = registryType.getMethod("getMailAddress", GameProfile.class)
            .invoke(registry, PLAYER);
        Object tradeAddress = registryType.getMethod("getMailAddress", String.class)
            .invoke(registry, TRADER);
        IInventory box = (IInventory) postRegistry.getMethod("getPOBox", World.class, addressType)
            .invoke(null, world, boxAddress);
        if (box == null) {
            box = (IInventory) postRegistry.getMethod("getOrCreatePOBox", World.class, addressType)
                .invoke(null, world, boxAddress);
            Method createTrade = registryType
                .getMethod("getOrCreateTradeStation", World.class, GameProfile.class, addressType);
            IInventory trade = (IInventory) createTrade.invoke(registry, world, PLAYER, tradeAddress);
            box.setInventorySlotContents(0, witness(helper));
            trade.setInventorySlotContents(0, witness(helper));
            ((WorldSavedData) box).markDirty();
            ((WorldSavedData) trade).markDirty();
            MaterialLib.LOG.info("[{}] seeded forestry witnesses", BATCH);
            helper.assumeTrue(false, "seeded the Forestry witnesses; the next boot verifies them");
            return;
        }
        IInventory trade = (IInventory) registryType.getMethod("getTradeStation", World.class, addressType)
            .invoke(registry, world, tradeAddress);
        helper.assertTrue(trade != null, "seeded trade station is missing");
        assertWitnessStack(helper, box.getStackInSlot(0), "forestry:POBox");
        assertWitnessStack(helper, trade.getStackInSlot(0), "forestry:TradePO");
        verified(helper, "forestry:POBox", "forestry:TradePO");
    }
}
