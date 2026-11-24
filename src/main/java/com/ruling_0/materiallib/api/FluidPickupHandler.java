package com.ruling_0.materiallib.api;

import net.minecraft.block.BlockDispenser;
import net.minecraft.dispenser.IBehaviorDispenseItem;
import net.minecraft.dispenser.IBlockSource;
import net.minecraft.init.Items;
import net.minecraft.item.ItemStack;
import net.minecraft.util.EnumFacing;
import net.minecraft.util.MovingObjectPosition;

import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.event.entity.player.FillBucketEvent;

import cpw.mods.fml.common.eventhandler.Event;
import cpw.mods.fml.common.eventhandler.EventPriority;
import cpw.mods.fml.common.eventhandler.SubscribeEvent;

/// Lets vanilla buckets pick up placeable material fluids into placing containers, from a hand or a dispenser.
///
/// No other use of a vanilla bucket picks up a [ShapeFluidBlock], so vanilla never takes a water or lava kind block as
/// plain water or lava. The bucket handler runs last, so another mod's handler that fills the bucket first wins.
public final class FluidPickupHandler {

    private FluidPickupHandler() {}

    /// Registers the bucket handler and wraps the dispenser behavior of [Items#bucket].
    public static void register() {
        MinecraftForge.EVENT_BUS.register(new FluidPickupHandler());
        IBehaviorDispenseItem vanilla = (IBehaviorDispenseItem) BlockDispenser.dispenseBehaviorRegistry
            .getObject(Items.bucket);
        IBehaviorDispenseItem guarded = (source, stack) -> facesFluidBlock(source) ?
            FluidContainerDispenseBehavior.INSTANCE.dispense(source, stack) : vanilla.dispense(source, stack);
        BlockDispenser.dispenseBehaviorRegistry.putObject(Items.bucket, guarded);
    }

    private static boolean facesFluidBlock(IBlockSource source) {
        EnumFacing facing = BlockDispenser.func_149937_b(source.getBlockMetadata());
        return source.getWorld().getBlock(source.getXInt() + facing.getFrontOffsetX(),
            source.getYInt() + facing.getFrontOffsetY(),
            source.getZInt() + facing.getFrontOffsetZ()) instanceof ShapeFluidBlock;
    }

    @SubscribeEvent(priority = EventPriority.LOWEST)
    public void onFillBucket(FillBucketEvent event) {
        MovingObjectPosition target = event.target;
        if (event.getResult() != Event.Result.DEFAULT || event.current == null || target == null ||
            target.typeOfHit != MovingObjectPosition.MovingObjectType.BLOCK) return;
        if (!(event.world.getBlock(target.blockX, target.blockY, target.blockZ) instanceof ShapeFluidBlock)) return;
        ItemStack filled = FluidPlacement.fillFrom(event.entityPlayer, event.world, target.blockX, target.blockY,
            target.blockZ, event.current);
        if (filled == null) {
            event.setCanceled(true);
            return;
        }
        event.result = filled;
        event.setResult(Event.Result.ALLOW);
    }
}
