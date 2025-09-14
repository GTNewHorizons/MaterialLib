package materiallib.mixin;

import java.util.List;
import java.util.Set;

import org.jetbrains.annotations.NotNull;
import org.spongepowered.asm.mixin.Mixin;

import com.gtnewhorizon.gtnhmixins.ILateMixinLoader;
import com.gtnewhorizon.gtnhmixins.LateMixin;
import com.gtnewhorizon.gtnhmixins.builders.IMixins;

@LateMixin
public class LateMixinPlugin implements ILateMixinLoader {

    @Override
    public String getMixinConfig() {
        return "mixins.materiallib.late.json";
    }

    @Override
    public @NotNull List<String> getMixins(Set<String> loadedMods) {
        return IMixins.getLateMixins(Mixins.class, loadedMods);
    }
}
