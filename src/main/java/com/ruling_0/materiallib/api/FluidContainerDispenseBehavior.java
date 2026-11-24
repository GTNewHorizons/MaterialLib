package com.ruling_0.materiallib.api;

import net.minecraft.block.BlockDispenser;
import net.minecraft.dispenser.BehaviorDefaultDispenseItem;
import net.minecraft.dispenser.IBlockSource;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.tileentity.TileEntityDispenser;
import net.minecraft.util.EnumFacing;
import net.minecraft.world.World;

/// Lets dispensers use fluid containers as a player does.
///
/// A filled placing container places its fluid in front of the dispenser. An empty container picks up the placeable
/// material fluid source there, into the container a player's use would fill. A container with nothing to do is
/// dropped as an item, as a vanilla bucket is. Like vanilla's dispenser bucket, a dispenser skips the player's edit and
/// mining permission checks.
final class FluidContainerDispenseBehavior extends BehaviorDefaultDispenseItem {

    static final FluidContainerDispenseBehavior INSTANCE = new FluidContainerDispenseBehavior();

    private final BehaviorDefaultDispenseItem drop = new BehaviorDefaultDispenseItem();

    private FluidContainerDispenseBehavior() {}

    static void registerFor(Item item) {
        BlockDispenser.dispenseBehaviorRegistry.putObject(item, INSTANCE);
    }

    @Override
    protected ItemStack dispenseStack(IBlockSource source, ItemStack stack) {
        EnumFacing facing = BlockDispenser.func_149937_b(source.getBlockMetadata());
        World world = source.getWorld();
        int x = source.getXInt() + facing.getFrontOffsetX();
        int y = source.getYInt() + facing.getFrontOffsetY();
        int z = source.getZInt() + facing.getFrontOffsetZ();
        if (stack.getItem() instanceof ShapeFluidInContainer container) {
            if (!container.placesFluid() || !FluidPlacement.place(world, x, y, z, stack)) {
                return super.dispenseStack(source, stack);
            }
            return exchange(source, stack, container.emptyStack());
        }
        ItemStack filled = FluidPlacement.fill(world, x, y, z, stack);
        return filled == null ? super.dispenseStack(source, stack) : exchange(source, stack, filled);
    }

    /// Swaps one of `used` for `result` as the vanilla bucket's dispenser behavior does, and returns the stack for the
    /// dispenser slot. A null `result` stands for a container consumed by use.
    private ItemStack exchange(IBlockSource source, ItemStack used, ItemStack result) {
        if (--used.stackSize <= 0) return result != null ? result : used;
        TileEntityDispenser dispenser = (TileEntityDispenser) source.getBlockTileEntity();
        if (result != null && dispenser.func_146019_a(result) < 0) drop.dispense(source, result);
        return used;
    }
}
