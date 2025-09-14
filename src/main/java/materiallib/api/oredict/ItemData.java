package materiallib.api.oredict;

import net.minecraft.item.ItemStack;

import it.unimi.dsi.fastutil.objects.ObjectOpenCustomHashSet;
import materiallib.api.enums.OrePrefix;
import materiallib.api.material.IMaterial;
import materiallib.api.repository.IMaterialRepository;

public class ItemData {

    public final OrePrefix prefix;
    public final String materialName;
    public ItemStack primary;

    public final ObjectOpenCustomHashSet<ItemStack> alternatives = new ObjectOpenCustomHashSet<>(OredictUnificator.HASH_NBT_SENSITIVE);

    public ItemData(OrePrefix prefix, String materialName) {
        this.prefix = prefix;
        this.materialName = materialName;
    }

    public void add(ItemStack stack) {
        alternatives.add(stack.copy());

        if (primary == null) {
            primary = stack.copy();
        }
    }

    public void setPrimary(ItemStack stack) {
        alternatives.add(stack.copy());

        primary = stack.copy();
    }

    public IMaterial getMaterial() {
        for (var repo : IMaterialRepository.REPOSITORIES.values()) {
            IMaterial mat = repo.getMaterial(materialName);

            if (mat != null) return mat;
        }

        return null;
    }
}
