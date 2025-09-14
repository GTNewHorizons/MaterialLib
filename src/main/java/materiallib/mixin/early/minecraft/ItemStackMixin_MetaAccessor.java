package materiallib.mixin.early.minecraft;

import net.minecraft.item.ItemStack;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;

import materiallib.api.interfaces.ItemMetaAccessor;

@Mixin(ItemStack.class)
public class ItemStackMixin_MetaAccessor implements ItemMetaAccessor {

    @Shadow
    int itemDamage;

    @Override
    public int ml$getMeta() {
        return itemDamage;
    }
}
