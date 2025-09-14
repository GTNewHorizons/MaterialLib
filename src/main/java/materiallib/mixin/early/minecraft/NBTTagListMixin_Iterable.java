package materiallib.mixin.early.minecraft;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

import net.minecraft.nbt.NBTBase;
import net.minecraft.nbt.NBTTagList;

import org.jetbrains.annotations.NotNull;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;

@Mixin(NBTTagList.class)
public class NBTTagListMixin_Iterable implements Iterable<NBTBase> {

    @Shadow
    private List<NBTBase> tagList = new ArrayList<>();

    @Override
    public @NotNull Iterator<NBTBase> iterator() {
        return tagList.iterator();
    }
}
