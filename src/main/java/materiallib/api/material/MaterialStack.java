package materiallib.api.material;

import static materiallib.api.misc.MaterialLibValues.INGOTS;
import static materiallib.api.misc.MaterialLibValues.M;

import net.minecraft.item.ItemStack;
import net.minecraftforge.fluids.FluidStack;

import lombok.EqualsAndHashCode;
import lombok.ToString;
import materiallib.api.enums.FluidState;
import materiallib.api.enums.OrePrefix;

@EqualsAndHashCode
public class MaterialStack {

    public IMaterial material;
    /** 1 unit = {@link materiallib.api.misc.MaterialLibValues#M} */
    public long amount;

    public MaterialStack(IMaterial material, long amount) {
        this.material = material;
        this.amount = amount;
    }

    public static MaterialStack liters(IMaterial material, int liters) {
        return new MaterialStack(material, liters * M / INGOTS);
    }

    public static MaterialStack items(IMaterial material, int itemCount) {
        return new MaterialStack(material, itemCount * M);
    }

    public MaterialStack copy(long aAmount) {
        return new MaterialStack(material, aAmount);
    }

    public MaterialStack copy() {
        return new MaterialStack(material, amount);
    }

    @Override
    public String toString() {
        return String.format("MaterialStack{material=%s, amount=%d M (%d items, %d L)}", material, amount, getItemCount(OrePrefix.dust), getFluidLitres());
    }

    public ItemStack toItemStack(OrePrefix prefix) {
        return material.getItem(prefix, getItemCount(prefix));
    }

    public FluidStack toFluidStack(FluidState fluid) {
        return material.getFluid(fluid, getFluidLitres());
    }

    public int getItemCount(OrePrefix prefix) {
        return (int) (amount / prefix.materialAmount);
    }

    public int getFluidLitres() {
        return (int) (amount * INGOTS / M);
    }
}
