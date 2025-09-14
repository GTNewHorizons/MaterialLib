package materiallib.api.data;

import static materiallib.api.misc.MaterialLibValues.WILDCARD;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

import net.minecraft.init.Items;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraftforge.common.util.Constants;

import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.ToString;

@EqualsAndHashCode
@ToString
@Getter
public class ItemId {

    public final Item item;
    public final int meta;
    public final NBTTagCompound tag;

    public ItemId(Item item, int meta, NBTTagCompound tag) {
        this.item = item;
        this.meta = meta;
        this.tag = tag;
    }

    public static ItemId create(NBTTagCompound tag) {
        return new ItemId(
            Item.getItemById(tag.getShort("item")),
            tag.getShort("meta"),
            tag.hasKey("tag", Constants.NBT.TAG_COMPOUND) ? tag.getCompoundTag("tag") : null);
    }

    /**
     * This method copies NBT, as it is mutable.
     */
    public static ItemId create(ItemStack itemStack) {
        NBTTagCompound nbt = itemStack.getTagCompound();
        if (nbt != null) {
            nbt = (NBTTagCompound) nbt.copy();
        }

        return new ItemId(itemStack.getItem(), Items.feather.getDamage(itemStack), nbt);
    }

    /**
     * This method copies NBT, as it is mutable.
     */
    public static ItemId createWithStackSize(ItemStack itemStack) {
        NBTTagCompound nbt = itemStack.getTagCompound();
        if (nbt != null) {
            nbt = (NBTTagCompound) nbt.copy();
        }

        return new ItemId(
            itemStack.getItem(),
            Items.feather.getDamage(itemStack),
            nbt);
    }

    /**
     * This method copies NBT, as it is mutable.
     */
    public static ItemId create(Item item, int metaData, @Nullable NBTTagCompound nbt) {
        if (nbt != null) {
            nbt = (NBTTagCompound) nbt.copy();
        }
        return new ItemId(item, metaData, nbt);
    }

    /**
     * This method stores metadata as wildcard and NBT as null.
     */
    public static ItemId createAsWildcard(ItemStack itemStack) {
        return new ItemId(itemStack.getItem(), WILDCARD, null);
    }

    public static ItemId createAsWildcardWithNBT(ItemStack itemStack) {
        NBTTagCompound nbt = itemStack.getTagCompound();
        if (nbt != null) {
            nbt = (NBTTagCompound) nbt.copy();
        }
        return new ItemId(itemStack.getItem(), WILDCARD, nbt);
    }

    /**
     * This method stores NBT as null.
     */
    public static ItemId createWithoutNBT(ItemStack itemStack) {
        return new ItemId(itemStack.getItem(), Items.feather.getDamage(itemStack), null);
    }

    /**
     * This method does not copy NBT in order to save time. Make sure not to mutate it!
     */
    public static ItemId createNoCopy(ItemStack itemStack) {
        return new ItemId(
            itemStack.getItem(),
            Items.feather.getDamage(itemStack),
            itemStack.getTagCompound());
    }

    /**
     * This method does not copy NBT in order to save time. Make sure not to mutate it!
     */
    public static ItemId createNoCopyWithStackSize(ItemStack itemStack) {
        return new ItemId(
            itemStack.getItem(),
            Items.feather.getDamage(itemStack),
            itemStack.getTagCompound());
    }

    /**
     * This method does not copy NBT in order to save time. Make sure not to mutate it!
     */
    public static ItemId createNoCopy(Item item, int metaData, @Nullable NBTTagCompound nbt) {
        return new ItemId(item, metaData, nbt);
    }

    public NBTTagCompound writeToNBT() {
        NBTTagCompound tag = new NBTTagCompound();
        tag.setShort("item", (short) Item.getIdFromItem(getItem()));
        tag.setShort("meta", (short) getMeta());
        if (getTag() != null) tag.setTag("tag", getTag());
        tag.setInteger("stackSize", 1);
        return tag;
    }

    @Nonnull
    public ItemStack getItemStack() {
        return getItemStack(1);
    }

    @Nonnull
    public ItemStack getItemStack(int stackSize) {
        ItemStack itemStack = new ItemStack(getItem(), stackSize, getMeta());
        itemStack.setTagCompound(getTag() == null ? null : (NBTTagCompound) getTag().copy());
        return itemStack;
    }
}
