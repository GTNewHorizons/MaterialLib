package materiallib.api.material;

import net.minecraft.item.ItemStack;
import net.minecraftforge.fluids.FluidStack;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import materiallib.api.enums.FluidState;
import materiallib.api.enums.OrePrefix;
import materiallib.api.material.metadata.MaterialMetadata;
import materiallib.api.repository.IMaterialRepository;
import materiallib.api.tags.Tag;

public interface IMaterial {
    IMaterialRepository<?> getRepository();

    int getID();

    MaterialIdentifier getName();

    String getLocalizedName();
    String getLocalizedName(OrePrefix prefix);

    String getLocalizedName(FluidState fluidType);

    default String getOredictName(OrePrefix prefix) {
        return prefix + getName().materialName;
    }

    ItemStack getItem(OrePrefix prefix, int amount);

    default boolean hasItem(OrePrefix prefix) {
        return getItem(prefix, 1) != null;
    }

    default ItemStack getItem(OrePrefix prefix, long amount) {
        return getItem(prefix, (int) amount);
    }

    FluidStack getFluid(FluidState fluidType, int amount);

    default FluidStack getFluid(FluidState fluidType, long amount) {
        return getFluid(fluidType, (int) amount);
    }

    @Nullable
    <T> T getMeta(MaterialMetadata<T> key);

    void addTag(Tag tag);
    void removeTag(Tag tag);
    boolean hasTag(Tag tag);

    /**
     * Gets metadata that must be valid. This should be used when missing the given metadata is a logic error and cannot
     * be recovered from.
     */
    @NotNull
    default <T> T getRequiredMeta(MaterialMetadata<T> key) {
        T value = getMeta(key);

        if (value == null) {
            throw new IllegalStateException("Required material metadata was null. Material=" + this + ", Metadata key=" + key);
        }

        return value;
    }

    default <T> T getMetaOrDefault(MaterialMetadata<T> key) {
        T value = getMeta(key);

        return value == null ? key.getDefaultValue() : value;
    }
}
