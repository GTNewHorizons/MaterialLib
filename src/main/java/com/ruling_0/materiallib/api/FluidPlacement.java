package com.ruling_0.materiallib.api;

import java.util.concurrent.ConcurrentHashMap;

import net.minecraft.block.Block;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.item.ItemStack;
import net.minecraft.world.World;

import net.minecraftforge.fluids.FluidContainerRegistry;
import net.minecraftforge.fluids.FluidStack;

import com.ruling_0.materiallib.MaterialLib;

/// Helpers for placing material fluids as blocks and picking them back up.
///
/// Placeability is one boolean per material and fluid shape, merged like any other property.
final class FluidPlacement {

    private static final ConcurrentHashMap<String, Property<Boolean>> PLACEABLE = new ConcurrentHashMap<>();

    private FluidPlacement() {}

    static Property<Boolean> placeableProperty(String fluidShapeName) {
        return PLACEABLE.computeIfAbsent(
            fluidShapeName,
            name -> Property.of(MaterialLib.MODID, "placeable." + name, false));
    }

    static boolean isPlaceable(Material material, Shape fluid) {
        return material.getProperty(placeableProperty(fluid.getName()));
    }

    static void requireFluidShape(Shape fluid) {
        if (!(fluid instanceof ShapeFluid)) throw new IllegalArgumentException(fluid + " is not a fluid shape");
    }

    /// Places the fluid of `filled` at the given position when the player may edit there; see [#place].
    static boolean placeFrom(EntityPlayer player, World world, int x, int y, int z, int side, ItemStack filled) {
        return player.canPlayerEdit(x, y, z, side, filled) && place(world, x, y, z, filled);
    }

    /// Places the fluid of `filled` at the given position when `filled` holds a placeable material fluid and the
    /// position holds no solid block. Returns whether it placed.
    static boolean place(World world, int x, int y, int z, ItemStack filled) {
        FluidStack fluid = FluidContainerRegistry.getFluidForFilledItem(filled);
        if (fluid == null || !(fluid.getFluid().getBlock() instanceof ShapeFluidBlock block)) return false;
        net.minecraft.block.material.Material target = world.getBlock(x, y, z).getMaterial();
        if (target.isSolid()) return false;
        if (!world.isRemote && !target.isLiquid()) world.func_147480_a(x, y, z, true);
        return world.setBlock(x, y, z, block, 0, 3);
    }

    /// Picks up the placeable material fluid source at the given position when the player may mine and edit there; see
    /// [#fill].
    static ItemStack fillFrom(EntityPlayer player, World world, int x, int y, int z, ItemStack empty) {
        if (!world.canMineBlock(player, x, y, z) || !player.canPlayerEdit(x, y, z, 0, empty)) return null;
        return fill(world, x, y, z, empty);
    }

    /// Picks up the placeable material fluid source at the given position into one item of `empty`. Returns the filled
    /// container, or null when nothing was picked up.
    static ItemStack fill(World world, int x, int y, int z, ItemStack empty) {
        Block block = world.getBlock(x, y, z);
        if (!(block instanceof ShapeFluidBlock fluidBlock) || world.getBlockMetadata(x, y, z) != 0) return null;
        ItemStack copy = empty.copy();
        copy.stackSize = 1;
        FluidStack source = new FluidStack(fluidBlock.getFluid(), FluidContainerRegistry.BUCKET_VOLUME);
        ItemStack filled = FluidContainerRegistry.fillFluidContainer(source, copy);
        if (filled == null || !(filled.getItem() instanceof ShapeFluidInContainer container)) return null;
        if (!container.placesFluid()) return null;
        world.setBlockToAir(x, y, z);
        return filled;
    }

    /// Swaps one of `held` for `result` as a vanilla bucket does, and returns the stack for the player's hand. A null
    /// `result` stands for a container consumed by use.
    static ItemStack exchange(ItemStack held, ItemStack result, EntityPlayer player) {
        if (player.capabilities.isCreativeMode) return held;
        if (--held.stackSize <= 0) return result != null ? result : held;
        if (result != null && !player.inventory.addItemStackToInventory(result)) {
            player.dropPlayerItemWithRandomChoice(result, false);
        }
        return held;
    }
}
