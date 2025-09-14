package materiallib.api.items;

import java.util.List;
import java.util.Set;
import java.util.function.BiConsumer;

import javax.annotation.OverridingMethodsMustInvokeSuper;

import net.minecraft.creativetab.CreativeTabs;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;

import net.minecraftforge.oredict.OreDictionary;

import org.jetbrains.annotations.NotNull;

import cpw.mods.fml.common.registry.GameRegistry;
import it.unimi.dsi.fastutil.objects.Object2IntMap;
import it.unimi.dsi.fastutil.objects.Object2IntOpenHashMap;
import materiallib.api.enums.OrePrefix;
import materiallib.api.lifecycle.MaterialLifecycleEvent;
import materiallib.api.lifecycle.MaterialLifecycleHandler;
import materiallib.api.lifecycle.MaterialLifecycles;
import materiallib.api.lifecycle.events.RegisterObjectsEvent;
import materiallib.api.lifecycle.events.RegisterOreDictEvent;
import materiallib.api.material.IMaterial;
import materiallib.api.material.MaterialItem;
import materiallib.api.repository.IMaterialRepository;
import materiallib.api.util.DataUtils;

public abstract class MaterialMetaItem<TMat extends IMaterial> extends Item implements MaterialItem,
    MaterialLifecycleHandler {

    public final String name;

    @NotNull
    protected final List<OrePrefix> prefixes;
    @NotNull
    protected final Object2IntMap<OrePrefix> prefixesReversed;
    @NotNull
    protected final IMaterialRepository<TMat> repository;
    protected final int matIdOffset;

    public MaterialMetaItem(String name, List<OrePrefix> prefixes, @NotNull IMaterialRepository<TMat> repository, int matIdOffset) {
        if (prefixes.size() > 32) throw new IllegalArgumentException("MaterialMetaItems can only accept up to 32 ore prefixes");

        this.name = name;
        this.prefixes = prefixes;
        this.repository = repository;
        this.matIdOffset = matIdOffset;
        this.prefixesReversed = DataUtils.mapToMap(prefixes, new Object2IntOpenHashMap<>(), (p, i) -> p, (p, i) -> i);
        this.prefixesReversed.defaultReturnValue(-1);

        setUnlocalizedName(name);
        setHasSubtypes(true);

        MaterialLifecycles.register(this);
    }

    @Override
    @OverridingMethodsMustInvokeSuper
    public void getRelevantEvents(Set<Class<? extends MaterialLifecycleEvent>> set) {
        set.add(RegisterObjectsEvent.class);
        set.add(RegisterOreDictEvent.class);
    }

    @Override
    @OverridingMethodsMustInvokeSuper
    public void onLifecycleEvent(MaterialLifecycleEvent event) {
        if (event instanceof RegisterObjectsEvent) {
            this.register();
        }

        if (event instanceof RegisterOreDictEvent) {
            this.registerOreDict();
        }
    }

    @OverridingMethodsMustInvokeSuper
    protected void register() {
        GameRegistry.registerItem(this, this.name);
    }

    protected void registerOreDict() {
        forEachItem((prefix, mat, stack) -> {
            OreDictionary.registerOre(mat.getOredictName(prefix), stack);
        });
    }

    protected interface ItemConsumer<TMat extends IMaterial> {
        void accept(OrePrefix prefix, TMat mat, ItemStack stack);
    }

    protected void forEachItem(ItemConsumer<TMat> fn) {
        for (int matId = matIdOffset; matId < matIdOffset + 1000; matId++) {
            TMat mat = repository.getMaterial(matId);

            if (mat == null) continue;

            for (OrePrefix prefix : prefixes) {
                ItemStack stack = this.getStack(mat, prefix);

                if (stack == null) continue;

                fn.accept(prefix, mat, stack);
            }
        }
    }

    @Override
    public void getSubItems(Item p_150895_1_, CreativeTabs p_150895_2_, List<ItemStack> stacks) {
        forEachItem((prefix, mat, stack) -> {
            stacks.add(stack);
        });
    }

    @Override
    public TMat getMaterial(ItemStack stack) {
        int id = stack.getItemDamage() % 1000 + matIdOffset;

        return repository.getMaterial(id);
    }

    @Override
    public OrePrefix getPrefix(ItemStack stack) {
        int prefix = stack.getItemDamage() / 1000;

        return DataUtils.getIndexSafe(prefixes, prefix);
    }

    @Override
    public ItemStack getStack(IMaterial material, OrePrefix prefix) {
        if (material.getRepository() != this.repository) return null;

        int id = material.getID() - this.matIdOffset;

        if (id < 0 || id >= 1000) return null;

        int prefixIndex = this.prefixesReversed.getInt(prefix);

        if (prefixIndex == -1) return null;

        return new ItemStack(this, 1, prefixIndex * 1000 + id);
    }
}
