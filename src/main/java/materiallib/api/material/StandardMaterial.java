package materiallib.api.material;

import net.minecraft.item.ItemStack;
import net.minecraft.util.StatCollector;
import net.minecraftforge.fluids.FluidStack;

import org.jetbrains.annotations.Nullable;

import it.unimi.dsi.fastutil.objects.ReferenceOpenHashSet;
import materiallib.api.enums.FluidState;
import materiallib.api.enums.OrePrefix;
import materiallib.api.material.metadata.MaterialMetadata;
import materiallib.api.oredict.OredictUnificator;
import materiallib.api.repository.IMaterialRepository;
import materiallib.api.tags.Tag;
import materiallib.api.util.MCUtils;

public class StandardMaterial implements IMaterial {

    public final IMaterialRepository<? extends StandardMaterial> repository;
    public final int id;
    public final MaterialIdentifier name;

    protected final ReferenceOpenHashSet<Tag> tags = new ReferenceOpenHashSet<>();

    public StandardMaterial(IMaterialRepository<? extends StandardMaterial> repository, int id, String name) {
        this.repository = repository;
        this.id = id;
        this.name = new MaterialIdentifier(repository.getName(), name);
    }

    @Override
    public IMaterialRepository<?> getRepository() {
        return repository;
    }

    @Override
    public int getID() {
        return id;
    }

    @Override
    public MaterialIdentifier getName() {
        return name;
    }

    @Override
    public String getLocalizedName() {
        return MCUtils.translate("material.name." + name.describe());
    }

    @Override
    public String getLocalizedName(OrePrefix prefix) {
        String override = "material.prefix-override." + name.describe() + "." + prefix;

        if (StatCollector.canTranslate(override)) {
            return MCUtils.translate(override);
        }

        String localized = getLocalizedName();

        return MCUtils.translate("material.prefix.name." + prefix, localized);
    }

    @Override
    public String getLocalizedName(FluidState fluidType) {
        String override = "material.fluidstate-override." + name.describe() + "." + fluidType;

        if (StatCollector.canTranslate(override)) {
            return MCUtils.translate(override);
        }

        String localized = getLocalizedName();

        return MCUtils.translate("material.fluidstate.name." + fluidType, localized);
    }

    @Override
    public ItemStack getItem(OrePrefix prefix, int amount) {
        return OredictUnificator.get(prefix, this, amount);
    }

    @Override
    public boolean hasItem(OrePrefix prefix) {
        return OredictUnificator.ITEM_DATA_BY_NAME.containsKey(getOredictName(prefix));
    }

    @Override
    public FluidStack getFluid(FluidState fluidType, int amount) {
        return null;
    }

    @Override
    public <T> @Nullable T getMeta(MaterialMetadata<T> key) {
        return null;
    }

    @Override
    public void addTag(Tag tag) {
        tags.add(tag);
    }

    @Override
    public void removeTag(Tag tag) {
        tags.remove(tag);
    }

    @Override
    public boolean hasTag(Tag tag) {
        return tags.contains(tag);
    }
}
