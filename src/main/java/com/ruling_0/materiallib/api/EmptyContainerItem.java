package com.ruling_0.materiallib.api;

import net.minecraft.client.renderer.texture.IIconRegister;
import net.minecraft.creativetab.CreativeTabs;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.util.MovingObjectPosition;
import net.minecraft.world.World;

import cpw.mods.fml.relauncher.Side;
import cpw.mods.fml.relauncher.SideOnly;

/// The item backing an empty container registration: a plain single-icon item carrying no material in its damage
/// value.
final class EmptyContainerItem extends Item {

    private final String iconPath;

    EmptyContainerItem(String ownerModid, String name, String iconPath) {
        this.iconPath = iconPath;
        setCreativeTab(CreativeTabs.tabMaterials);
        setUnlocalizedName(ownerModid + "." + name);
    }

    /// Picks up the placeable material fluid source the player looks at; see [FluidPlacement#fillFrom].
    @Override
    public ItemStack onItemRightClick(ItemStack stack, World world, EntityPlayer player) {
        MovingObjectPosition hit = getMovingObjectPositionFromPlayer(world, player, true);
        if (hit == null || hit.typeOfHit != MovingObjectPosition.MovingObjectType.BLOCK) return stack;
        ItemStack filled = FluidPlacement.fillFrom(player, world, hit.blockX, hit.blockY, hit.blockZ, hit.sideHit,
            stack);
        return filled == null ? stack : FluidPlacement.exchange(stack, filled, player);
    }

    @Override
    @SideOnly(Side.CLIENT)
    public void registerIcons(IIconRegister register) {
        itemIcon = register.registerIcon(iconPath);
    }
}
