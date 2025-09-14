package materiallib.api.items;

import java.util.List;

import javax.annotation.OverridingMethodsMustInvokeSuper;

import net.minecraft.client.renderer.ItemRenderer;
import net.minecraft.item.ItemStack;

import net.minecraftforge.client.MinecraftForgeClient;

import org.jetbrains.annotations.NotNull;

import cpw.mods.fml.common.FMLCommonHandler;
import cpw.mods.fml.relauncher.Side;
import cpw.mods.fml.relauncher.SideOnly;
import it.unimi.dsi.fastutil.ints.Int2ObjectLinkedOpenHashMap;
import materiallib.api.enums.OrePrefix;
import materiallib.api.material.IMaterial;
import materiallib.api.rendering.IItemTexture;
import materiallib.api.rendering.ItemWithTextures;
import materiallib.api.repository.IMaterialRepository;
import materiallib.client.rendering.TexturedItemRenderer;

public abstract class StandardMaterialMetaItem<TMat extends IMaterial> extends MaterialMetaItem<TMat> implements
    ItemWithTextures {

    public StandardMaterialMetaItem(String name, List<OrePrefix> prefixes, @NotNull IMaterialRepository<TMat> repository, int matIdOffset) {
        super(name, prefixes, repository, matIdOffset);
    }

    @Override
    protected void register() {
        super.register();

        if (FMLCommonHandler.instance().getSide().isClient()) {
            registerRenderer();
        }
    }

    protected final Int2ObjectLinkedOpenHashMap<IItemTexture[]> cache = new Int2ObjectLinkedOpenHashMap<>();

    @SideOnly(Side.CLIENT)
    @OverridingMethodsMustInvokeSuper
    protected void registerRenderer() {
        MinecraftForgeClient.registerItemRenderer(this, TexturedItemRenderer.INSTANCE);
    }

    @Override
    public String getItemStackDisplayName(ItemStack stack) {
        OrePrefix prefix = getPrefix(stack);
        IMaterial material = getMaterial(stack);

        if (prefix == null || material == null) return super.getItemStackDisplayName(stack);

        return material.getLocalizedName(prefix);
    }

    @Override
    public IItemTexture[] getTextures(ItemStack stack) {
        IItemTexture[] cached = cache.getAndMoveToFirst(stack.getItemDamage());

        if (cached != null) return cached;

        TMat mat = getMaterial(stack);
        OrePrefix prefix = getPrefix(stack);

        if (mat == null || prefix == null) return new IItemTexture[0];

        IItemTexture[] textures = getTextures(mat, prefix);

        cache.putAndMoveToFirst(stack.getItemDamage(), textures);

        return textures;
    }

    protected abstract IItemTexture[] getTextures(IMaterial material, OrePrefix prefix);
}
