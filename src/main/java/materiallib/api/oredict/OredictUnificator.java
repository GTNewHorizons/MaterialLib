package materiallib.api.oredict;

import java.util.HashMap;
import java.util.Map;
import java.util.Objects;

import net.minecraft.init.Items;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.fluids.Fluid;
import net.minecraftforge.fluids.FluidStack;
import net.minecraftforge.oredict.OreDictionary;

import com.gtnewhorizon.gtnhlib.eventbus.EventBusSubscriber;
import com.gtnewhorizon.gtnhlib.hash.Fnv1a32;
import cpw.mods.fml.common.eventhandler.SubscribeEvent;
import it.unimi.dsi.fastutil.Hash;
import it.unimi.dsi.fastutil.ints.Int2ObjectOpenHashMap;
import it.unimi.dsi.fastutil.longs.Long2ObjectOpenHashMap;
import it.unimi.dsi.fastutil.objects.Object2ObjectOpenCustomHashMap;
import it.unimi.dsi.fastutil.objects.Object2ObjectOpenHashMap;
import materiallib.api.annotations.LoadedEagerly;
import materiallib.api.enums.OrePrefix;
import materiallib.api.interfaces.ObjectWithId;
import materiallib.api.material.IMaterial;
import materiallib.api.util.MCUtils;

@LoadedEagerly
public class OredictUnificator {

    public static final Hash.Strategy<ItemStack> HASH_NBT_SENSITIVE = new Hash.Strategy<>() {

        @Override
        public int hashCode(ItemStack o) {
            int hash = Fnv1a32.initialState();
            hash = Fnv1a32.hashStep(hash, MCUtils.stackHash(o));
            hash = Fnv1a32.hashStep(hash, Objects.hash(o.getTagCompound()));
            return hash;
        }

        @Override
        public boolean equals(ItemStack a, ItemStack b) {
            if (a == b) return true;
            if (a == null || b == null) return false;

            if (MCUtils.stackHash(a) != MCUtils.stackHash(b)) return false;

            return Objects.equals(a.getTagCompound(), b.getTagCompound());
        }
    };

    public static final Object2ObjectOpenCustomHashMap<ItemStack, ItemData> ITEM_DATA = new Object2ObjectOpenCustomHashMap<>(HASH_NBT_SENSITIVE);
    public static final Map<String, ItemData> ITEM_DATA_BY_NAME = new HashMap<>();

    static {
        MinecraftForge.EVENT_BUS.register(new Handler());
    }

    public static class Handler {
        @SubscribeEvent
        public void registerOre(OreDictionary.OreRegisterEvent event) {
            OredictUnificator.registerOre(event);
        }
    }

    public static void registerOre(OreDictionary.OreRegisterEvent event) {
        var prefix = OrePrefix.detectPrefix(event.Name);

        if (prefix == null) return;

        ItemData itemData = ITEM_DATA_BY_NAME.get(event.Name);

        if (itemData == null) {
            ITEM_DATA_BY_NAME.put(event.Name, itemData = new ItemData(prefix.left(), prefix.right()));

            itemData.setPrimary(event.Ore);
        } else {
            itemData.add(event.Ore);
        }

        ITEM_DATA.put(event.Ore.copy(), itemData);
    }

    public static void setPrimary(String oredictName, ItemStack stack) {
        ItemData itemData = ITEM_DATA_BY_NAME.get(oredictName);

        itemData.setPrimary(stack);

        ITEM_DATA.put(stack.copy(), itemData);
    }

    public static ItemData getItemData(ItemStack stack) {
        return ITEM_DATA.get(stack);
    }

    public static ItemStack unificate(ItemStack stack) {
        ItemData itemData = ITEM_DATA.get(stack);

        if (itemData == null) return stack;

        return MCUtils.copyWithAmount(itemData.primary, stack.stackSize);
    }

    public static ItemStack get(OrePrefix prefix, IMaterial material, int amount) {
        ItemData itemData = ITEM_DATA_BY_NAME.get(material.getOredictName(prefix));

        if (itemData == null || itemData.primary == null) return null;

        return MCUtils.copyWithAmount(itemData.primary, amount);
    }
}
