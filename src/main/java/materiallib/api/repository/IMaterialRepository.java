package materiallib.api.repository;

import java.util.HashMap;

import javax.annotation.Nullable;

import net.minecraft.item.ItemStack;
import net.minecraftforge.fluids.FluidStack;

import materiallib.api.enums.FluidState;
import materiallib.api.enums.OrePrefix;
import materiallib.api.material.IMaterial;
import materiallib.api.recipe.IMaterialRecipe;

public interface IMaterialRepository<Mat extends IMaterial> {

    String getName();

    Mat getMaterial(int materialId);
    Mat getMaterial(String materialName);

    default ItemStack getStack(OrePrefix prefix, String materialName, int amount) {
        return getMaterial(materialName).getItem(prefix, amount);
    }

    default FluidStack getFluid(FluidState fluidType, String materialName, int amount) {
        return getMaterial(materialName).getFluid(fluidType, amount);
    }

    @Nullable
    IMaterialRecipe getRecipe(String name);

    Iterable<Mat> getMaterials();

    HashMap<String, IMaterialRepository<?>> REPOSITORIES = new HashMap<>();

    static void registerRepository(IMaterialRepository<?> repo) {
        REPOSITORIES.put(repo.getName(), repo);
    }

    static IMaterialRepository<?> getRepository(String name) {
        return REPOSITORIES.get(name);
    }
}
