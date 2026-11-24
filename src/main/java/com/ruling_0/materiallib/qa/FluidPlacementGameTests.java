package com.ruling_0.materiallib.qa;

import net.minecraft.block.Block;
import net.minecraft.block.material.Material;
import net.minecraft.entity.item.EntityItem;
import net.minecraft.entity.passive.EntityPig;
import net.minecraft.init.Blocks;
import net.minecraft.init.Items;
import net.minecraft.item.ItemStack;
import net.minecraft.tileentity.TileEntityDispenser;
import net.minecraft.util.MovingObjectPosition;
import net.minecraft.util.Vec3;
import net.minecraft.world.World;

import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.common.util.FakePlayer;
import net.minecraftforge.event.entity.player.FillBucketEvent;
import net.minecraftforge.fluids.FluidContainerRegistry;

import com.gtnewhorizons.horizonqa.api.GameTestHelper;
import com.gtnewhorizons.horizonqa.api.TestPos;
import com.gtnewhorizons.horizonqa.api.annotation.GameTest;
import com.gtnewhorizons.horizonqa.api.annotation.GameTestHolder;
import com.ruling_0.materiallib.Config;
import com.ruling_0.materiallib.MaterialLib;
import com.ruling_0.materiallib.api.ShapeFluidBlock;
import com.ruling_0.materiallib.api.StackResolver;

import cpw.mods.fml.common.eventhandler.Event;
import cpw.mods.fml.common.registry.GameRegistry;

/// In-world checks of placeable material fluids, run on the fluids registered by
/// [com.ruling_0.materiallib.examples.ExampleContent].
///
/// Placement and pickup go through the container items' right-click and a posted [FillBucketEvent], with a fake player
/// aiming at a block. Every test is skipped unless `registerExamples` is on, and logs a "verified" line when it passes.
@GameTestHolder(MaterialLib.MODID)
public class FluidPlacementGameTests {

    private static final String BATCH = "materiallib.fluids";
    private static final int CELL_SIZE = 5;
    private static final float LOOK_DOWN = 90, LOOK_EAST = -90;
    private static final int FACING_NORTH = 2;

    /// Skips the test unless `registerExamples` is on, and clears the test cell of fluids and dropped items when the
    /// test ends.
    private static void setUp(GameTestHelper helper) {
        helper.assumeTrue(Config.registerExamples, "registerExamples is off");
        helper.afterTest(() -> removeLeftovers(helper));
    }

    private static void verified(GameTestHelper helper, String check) {
        MaterialLib.LOG.info("[{}] verified {}", BATCH, check);
        helper.succeed();
    }

    /// A survival fake player with an empty inventory.
    private static FakePlayer player(GameTestHelper helper, String name) {
        FakePlayer player = helper.spawnFakePlayer(name);
        player.inventory.clearInventory(null, -1);
        player.capabilities.isCreativeMode = false;
        return player;
    }

    private static void removeLeftovers(GameTestHelper helper) {
        for (EntityItem item : helper.getEntities(EntityItem.class, 0, 0, 0, CELL_SIZE, CELL_SIZE, CELL_SIZE)) {
            item.setDead();
        }
        for (int x = 0; x < CELL_SIZE; x++) {
            for (int y = 0; y < CELL_SIZE; y++) {
                for (int z = 0; z < CELL_SIZE; z++) {
                    TestPos pos = helper.absolute(x, y, z);
                    if (helper.getWorld().getBlock(pos.x(), pos.y(), pos.z()) instanceof ShapeFluidBlock) {
                        helper.destroyBlock(x, y, z);
                    }
                }
            }
        }
    }

    private static ItemStack stack(GameTestHelper helper, String material, String shape, int amount) {
        ItemStack stack = StackResolver.getStack(material, shape, amount);
        helper.assertNotNull(stack, material + ":" + shape + " does not resolve");
        return stack;
    }

    private static Block fluidBlock(ItemStack filled) {
        return FluidContainerRegistry.getFluidForFilledItem(filled).getFluid().getBlock();
    }

    /// Right-clicks `held` with the player's eye at the test-local position (`x`, `y`, `z`), looking along `yaw` and
    /// `pitch`. Returns the stack left in hand.
    private static ItemStack use(GameTestHelper helper, FakePlayer player, ItemStack held, double x, double y, double z,
                                 float yaw, float pitch) {
        player.setLocationAndAngles(helper.getOriginX() + x, helper.getOriginY() + y - player.getEyeHeight(),
            helper.getOriginZ() + z, yaw, pitch);
        player.inventory.mainInventory[player.inventory.currentItem] = held;
        return held.getItem().onItemRightClick(held, helper.getWorld(), player);
    }

    /// Right-clicks `held` looking down at the floor block at test-local (`x`, 0, `z`), so a placed fluid lands at
    /// (`x`, 1, `z`).
    private static ItemStack placeOnFloor(GameTestHelper helper, FakePlayer player, ItemStack held, int x, int z) {
        return use(helper, player, held, x + 0.5, 3.5, z + 0.5, 0, LOOK_DOWN);
    }

    /// Posts a vanilla bucket's [FillBucketEvent] at the test-local position, carrying `earlier` as the result an
    /// earlier
    /// handler set.
    private static FillBucketEvent fillVanillaBucket(GameTestHelper helper, FakePlayer player, int x, int y, int z,
                                                     Event.Result earlier) {
        int ax = helper.getOriginX() + x, ay = helper.getOriginY() + y, az = helper.getOriginZ() + z;
        MovingObjectPosition target = new MovingObjectPosition(ax, ay, az, 1,
            Vec3.createVectorHelper(ax + 0.5, ay + 1, az + 0.5));
        FillBucketEvent event = new FillBucketEvent(player, new ItemStack(Items.bucket), helper.getWorld(), target);
        event.setResult(earlier);
        MinecraftForge.EVENT_BUS.post(event);
        return event;
    }

    @GameTest(batch = BATCH)
    public static void aBucketPlacesASourceAndReturnsTheEmptyBucket(GameTestHelper helper) {
        setUp(helper);
        FakePlayer player = player(helper, "qa-fluids-place");
        Block iron = fluidBlock(stack(helper, "TestIron", "testBucket", 1));
        helper.setBlock(1, 0, 1, Blocks.stone);
        ItemStack left = placeOnFloor(helper, player, stack(helper, "TestIron", "testBucket", 1), 1, 1);
        helper.assertBlockPresent(iron, 0, 1, 1, 1);
        helper.assertItemEqual(new ItemStack(Items.bucket), left, "hand after placing");

        player.capabilities.isCreativeMode = true;
        helper.setBlock(3, 0, 1, Blocks.stone);
        left = placeOnFloor(helper, player, stack(helper, "TestIron", "testBucket", 1), 3, 1);
        helper.assertBlockPresent(iron, 0, 3, 1, 1);
        helper.assertItemEqual(stack(helper, "TestIron", "testBucket", 1), left, "creative hand after placing");
        verified(helper, "bucket placement");
    }

    @GameTest(batch = BATCH)
    public static void placingFromAStackKeepsTheRestAndGivesTheEmpty(GameTestHelper helper) {
        setUp(helper);
        FakePlayer player = player(helper, "qa-fluids-stack");
        helper.setBlock(1, 0, 1, Blocks.stone);
        ItemStack left = placeOnFloor(helper, player, stack(helper, "TestIron", "testBucket", 3), 1, 1);
        helper.assertItemEqual(stack(helper, "TestIron", "testBucket", 2), left, "hand after placing from a stack");
        helper.assertTrue(player.inventory.hasItem(Items.bucket), "the emptied bucket is not in the inventory");
        verified(helper, "stack placement");
    }

    @GameTest(batch = BATCH)
    public static void aVanillaBucketPicksUpOnlyASource(GameTestHelper helper) {
        setUp(helper);
        FakePlayer player = player(helper, "qa-fluids-pickup");
        Block iron = fluidBlock(stack(helper, "TestIron", "testBucket", 1));
        helper.setBlock(1, 1, 1, iron, 1);
        FillBucketEvent flowing = fillVanillaBucket(helper, player, 1, 1, 1, Event.Result.DEFAULT);
        helper.assertEquals(Event.Result.DEFAULT, flowing.getResult(), "bucket event at a flowing block");

        helper.setBlock(3, 1, 1, iron, 0);
        FillBucketEvent source = fillVanillaBucket(helper, player, 3, 1, 1, Event.Result.DEFAULT);
        helper.assertEquals(Event.Result.ALLOW, source.getResult(), "bucket event at a source block");
        helper.assertItemEqual(stack(helper, "TestIron", "testBucket", 1), source.result, "filled bucket");
        helper.assertBlockPresent(Blocks.air, 3, 1, 1);
        verified(helper, "bucket pickup");
    }

    /// The block of TestGold's testGas: water kind, with no vanilla bucket form.
    private static Block waterKindGas(GameTestHelper helper) {
        Block gold = fluidBlock(stack(helper, "TestGold", "testCanister", 1));
        helper.assertSame(Material.water, gold.getMaterial(), "TestGold's testGas block material");
        return gold;
    }

    @GameTest(batch = BATCH)
    public static void aVanillaBucketLeavesAWaterKindSourceAlone(GameTestHelper helper) {
        setUp(helper);
        FakePlayer player = player(helper, "qa-fluids-vanilla");
        Block gold = waterKindGas(helper);
        helper.setBlock(1, 1, 1, gold, 0);
        ItemStack left = use(helper, player, new ItemStack(Items.bucket), 1.5, 3.5, 1.5, 0, LOOK_DOWN);
        helper.assertBlockPresent(gold, 0, 1, 1, 1);
        helper.assertItemEqual(new ItemStack(Items.bucket), left, "hand after using a bucket on the source");
        FillBucketEvent denied = fillVanillaBucket(helper, player, 1, 1, 1, Event.Result.DENY);
        helper.assertTrue(denied.isCanceled(), "a bucket event denied by an earlier handler was not canceled");
        verified(helper, "vanilla bucket at a water-kind source");
    }

    /// Fires `held` once from a dispenser at test-local (1, 1, 2), facing (1, 1, 1). Returns the dispenser.
    private static TileEntityDispenser dispense(GameTestHelper helper, ItemStack held) {
        World world = helper.getWorld();
        TestPos pos = helper.absolute(1, 1, 2);
        helper.setBlock(1, 1, 2, Blocks.dispenser);
        world.setBlockMetadataWithNotify(pos.x(), pos.y(), pos.z(), FACING_NORTH, 2);
        TileEntityDispenser dispenser = (TileEntityDispenser) world.getTileEntity(pos.x(), pos.y(), pos.z());
        for (int slot = 0; slot < dispenser.getSizeInventory(); slot++) dispenser.setInventorySlotContents(slot, null);
        dispenser.setInventorySlotContents(0, held);
        Blocks.dispenser.updateTick(world, pos.x(), pos.y(), pos.z(), world.rand);
        return dispenser;
    }

    @GameTest(batch = BATCH)
    public static void aDispenserPlacesFromAStackAndKeepsTheEmpty(GameTestHelper helper) {
        setUp(helper);
        Block iron = fluidBlock(stack(helper, "TestIron", "testBucket", 1));
        TileEntityDispenser dispenser = dispense(helper, stack(helper, "TestIron", "testBucket", 2));
        helper.assertBlockPresent(iron, 0, 1, 1, 1);
        helper.assertItemEqual(stack(helper, "TestIron", "testBucket", 1), dispenser.getStackInSlot(0),
            "dispenser slot after placing from a stack");
        helper.assertItemEqual(new ItemStack(Items.bucket), dispenser.getStackInSlot(1), "emptied bucket");
        verified(helper, "dispenser placement");
    }

    @GameTest(batch = BATCH)
    public static void aDispenserPicksUpIntoTheContainersAPlayerFills(GameTestHelper helper) {
        setUp(helper);
        helper.setBlock(1, 1, 1, fluidBlock(stack(helper, "TestIron", "testBucket", 1)), 0);
        TileEntityDispenser dispenser = dispense(helper, new ItemStack(Items.bucket));
        helper.assertBlockPresent(Blocks.air, 1, 1, 1);
        helper.assertItemEqual(stack(helper, "TestIron", "testBucket", 1), dispenser.getStackInSlot(0),
            "dispensed vanilla bucket");

        helper.setBlock(1, 1, 1, fluidBlock(stack(helper, "TestIron", "testCanister", 1)), 0);
        dispenser = dispense(helper, new ItemStack(GameRegistry.findItem(MaterialLib.MODID, "testCanisterEmpty")));
        helper.assertBlockPresent(Blocks.air, 1, 1, 1);
        helper.assertItemEqual(stack(helper, "TestIron", "testCanister", 1), dispenser.getStackInSlot(0),
            "dispensed empty canister");
        verified(helper, "dispenser pickup");
    }

    /// A dispenser never takes a water-kind block as water: a bucket with no form of the fluid is dropped instead.
    @GameTest(batch = BATCH)
    public static void aDispenserDropsABucketAtAWaterKindSource(GameTestHelper helper) {
        setUp(helper);
        Block gold = waterKindGas(helper);
        helper.setBlock(1, 1, 1, gold, 0);
        TileEntityDispenser dispenser = dispense(helper, new ItemStack(Items.bucket));
        helper.assertBlockPresent(gold, 0, 1, 1, 1);
        helper.assertNull(dispenser.getStackInSlot(0), "dispenser slot after dispensing at the source");
        boolean dropped = false;
        for (EntityItem item : helper.getEntities(EntityItem.class, 0, 0, 0, CELL_SIZE, CELL_SIZE, CELL_SIZE)) {
            dropped |= item.getEntityItem().getItem() == Items.bucket;
        }
        helper.assertTrue(dropped, "the dispenser dropped no empty bucket");
        verified(helper, "dispensed bucket at a water-kind source");
    }

    @GameTest(batch = BATCH)
    public static void aNonPlaceablePairPlacesNothing(GameTestHelper helper) {
        setUp(helper);
        FakePlayer player = player(helper, "qa-fluids-unplaceable");
        helper.setBlock(1, 0, 1, Blocks.stone);
        ItemStack left = placeOnFloor(helper, player, stack(helper, "TestPalette", "testBucket", 1), 1, 1);
        helper.assertItemEqual(stack(helper, "TestPalette", "testBucket", 1), left, "hand after a refused placement");
        helper.assertBlockPresent(Blocks.air, 1, 1, 1);
        verified(helper, "non-placeable pair");
    }

    /// Cobweb stands in for a solid block: it has no collision box, so the player's aim passes through it to the floor
    /// and targets the cobweb itself.
    @GameTest(batch = BATCH)
    public static void placementReplacesGrassButNotCobweb(GameTestHelper helper) {
        setUp(helper);
        FakePlayer player = player(helper, "qa-fluids-replace");
        Block iron = fluidBlock(stack(helper, "TestIron", "testBucket", 1));
        helper.setBlock(1, 0, 1, Blocks.grass);
        helper.setBlock(1, 1, 1, Blocks.tallgrass, 1);
        placeOnFloor(helper, player, stack(helper, "TestIron", "testBucket", 1), 1, 1);
        helper.assertBlockPresent(iron, 0, 1, 1, 1);

        helper.setBlock(3, 0, 1, Blocks.stone);
        helper.setBlock(3, 1, 1, Blocks.web);
        ItemStack left = placeOnFloor(helper, player, stack(helper, "TestIron", "testBucket", 1), 3, 1);
        helper.assertBlockPresent(Blocks.web, 3, 1, 1);
        helper.assertItemEqual(stack(helper, "TestIron", "testBucket", 1), left, "hand after a refused placement");
        verified(helper, "replaceable target");
    }

    /// The canister places sideways against a stone block, so the gas has air both above and below it.
    @GameTest(batch = BATCH)
    public static void testGasRisesAndTheCanisterPicksItBackUp(GameTestHelper helper) {
        setUp(helper);
        FakePlayer player = player(helper, "qa-fluids-gas");
        Block gas = fluidBlock(stack(helper, "TestIron", "testCanister", 1));
        helper.setBlock(3, 2, 1, Blocks.stone);
        ItemStack left = use(helper, player, stack(helper, "TestIron", "testCanister", 1), 0.5, 2.5, 1.5, LOOK_EAST, 0);
        helper.assertBlockPresent(gas, 0, 2, 2, 1);
        helper.assertItemEqual(new ItemStack(GameRegistry.findItem(MaterialLib.MODID, "testCanisterEmpty")), left,
            "hand after placing holds the canister's own empty, not a bucket");

        World world = helper.getWorld();
        gas.updateTick(world, helper.getOriginX() + 2, helper.getOriginY() + 2, helper.getOriginZ() + 1, world.rand);
        helper.assertBlockPresent(gas, 2, 3, 1);
        helper.assertBlockPresent(Blocks.air, 2, 1, 1);

        ItemStack filled = use(helper, player, left, 0.5, 2.5, 1.5, LOOK_EAST, 0);
        helper.assertItemEqual(stack(helper, "TestIron", "testCanister", 1), filled, "canister after pickup");
        helper.assertBlockPresent(Blocks.air, 2, 2, 1);
        verified(helper, "gas flow and canister pickup");
    }

    /// TestGold's testGas gilds an iron block beneath it from a tick callback.
    @GameTest(batch = BATCH)
    public static void aTickCallbackThatActsLeavesTheFluidFlowing(GameTestHelper helper) {
        setUp(helper);
        Block gas = waterKindGas(helper);
        helper.setBlock(1, 1, 1, Blocks.iron_block);
        helper.setBlock(1, 2, 1, gas, 0);
        World world = helper.getWorld();
        gas.updateTick(world, helper.getOriginX() + 1, helper.getOriginY() + 2, helper.getOriginZ() + 1, world.rand);
        helper.assertBlockPresent(Blocks.gold_block, 1, 1, 1);
        helper.assertBlockPresent(gas, 1, 3, 1);
        helper.succeed();
    }

    /// Moves the pig through vanilla entity movement, the path that reaches the contact callback in play.
    @GameTest(batch = BATCH)
    public static void touchingTestGoldHurts(GameTestHelper helper) {
        setUp(helper);
        helper.setBlock(1, 1, 1, fluidBlock(stack(helper, "TestGold", "testBucket", 1)), 0);
        EntityPig pig = helper.spawnEntity(new EntityPig(helper.getWorld()), 1.5, 1, 1.5);
        helper.afterTest(pig::setDead);
        float health = pig.getHealth();
        pig.moveEntity(0, -0.01, 0);
        helper.assertTrue(pig.getHealth() < health, "a pig moving in TestGold's fluid took no damage");
        verified(helper, "contact callback");
    }

    @GameTest(batch = BATCH)
    public static void placeableBlocksAreRegisteredUnderTheirFluidNames(GameTestHelper helper) {
        setUp(helper);
        helper.assertSame(fluidBlock(stack(helper, "TestIron", "testBucket", 1)),
            Block.blockRegistry.getObject("materiallib:fluid.test.testiron"),
            "block registered as materiallib:fluid.test.testiron");
        helper.assertFalse(Block.blockRegistry.containsKey("materiallib:fluid.test.testpalette"),
            "non-placeable TestPalette has a registered fluid block");
        verified(helper, "block registry names");
    }
}
