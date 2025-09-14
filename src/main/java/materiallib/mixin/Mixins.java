package materiallib.mixin;

import org.jetbrains.annotations.NotNull;

import com.gtnewhorizon.gtnhmixins.builders.IMixins;
import com.gtnewhorizon.gtnhmixins.builders.MixinBuilder;

@SuppressWarnings("unused")
public enum Mixins implements IMixins {
    FastItemFluidIds(
        new MixinBuilder("Fast mechanism for unique item/fluid ids")
            .addCommonMixins("minecraft.ItemMixin_Ids", "minecraft.FluidMixin_Ids")
            .setPhase(Phase.EARLY)),

    ItemStackMetaAccessor(
        new MixinBuilder("Getter for ItemStack metadata")
            .addCommonMixins("minecraft.ItemStackMixin_MetaAccessor")
            .setPhase(Phase.EARLY)),

    NBTTagListIterable(
        new MixinBuilder("Implement Iterable for NBTTagList")
            .addCommonMixins("minecraft.NBTTagListMixin_Iterable")
            .setPhase(Phase.EARLY)),
    ;

    private final MixinBuilder builder;

    Mixins(MixinBuilder builder) {
        this.builder = builder;
    }

    @Override
    public @NotNull MixinBuilder getBuilder() {
        return builder;
    }
}
