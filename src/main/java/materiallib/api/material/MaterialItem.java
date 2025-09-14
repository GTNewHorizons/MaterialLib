package materiallib.api.material;

import net.minecraft.item.ItemStack;

import materiallib.api.enums.OrePrefix;

public interface MaterialItem {

    IMaterial getMaterial(ItemStack stack);

    OrePrefix getPrefix(ItemStack stack);

    ItemStack getStack(IMaterial material, OrePrefix prefix);
}
