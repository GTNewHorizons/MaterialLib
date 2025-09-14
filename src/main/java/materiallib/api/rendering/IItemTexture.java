package materiallib.api.rendering;

import net.minecraft.item.ItemStack;
import net.minecraftforge.client.IItemRenderer.ItemRenderType;

public interface IItemTexture {

    void render(ItemRenderType type, ItemStack stack);
}
