package com.ruling_0.materiallib.api;

import net.minecraft.item.ItemStack;
import net.minecraft.util.MovingObjectPosition;

import net.minecraftforge.event.entity.player.FillBucketEvent;

import cpw.mods.fml.common.eventhandler.Event;
import cpw.mods.fml.common.eventhandler.SubscribeEvent;

/// Lets vanilla buckets pick up placeable material fluids.
public final class FluidPickupHandler {

    @SubscribeEvent
    public void onFillBucket(FillBucketEvent event) {
        MovingObjectPosition target = event.target;
        if (target.typeOfHit != MovingObjectPosition.MovingObjectType.BLOCK) return;
        ItemStack filled = FluidPlacement.fillFrom(event.entityPlayer, event.world, target.blockX, target.blockY,
            target.blockZ, event.current);
        if (filled == null) return;
        event.result = filled;
        event.setResult(Event.Result.ALLOW);
    }
}
