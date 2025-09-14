package materiallib.api.repository;

import javax.annotation.Nullable;

import it.unimi.dsi.fastutil.ints.Int2ObjectOpenHashMap;
import it.unimi.dsi.fastutil.objects.Object2ObjectOpenHashMap;
import materiallib.api.material.IMaterial;
import materiallib.api.material.IMaterialHandle;
import materiallib.api.recipe.IMaterialRecipe;

public class StandardMaterialRepository<Mat extends IMaterial> implements IMaterialRepository<Mat> {

    private final String repoName;

    protected final Int2ObjectOpenHashMap<Mat> materialsById = new Int2ObjectOpenHashMap<>();
    protected final Object2ObjectOpenHashMap<String, Mat> materialsByName = new Object2ObjectOpenHashMap<>();

    public StandardMaterialRepository(String repoName) {
        this.repoName = repoName;
        IMaterialRepository.registerRepository(this);
    }

    @Override
    public String getName() {
        return repoName;
    }

    @Override
    public Mat getMaterial(int materialId) {
        return materialsById.get(materialId);
    }

    @Override
    public Mat getMaterial(String materialName) {
        return materialsByName.get(materialName);
    }

    @Override
    public Iterable<Mat> getMaterials() {
        return materialsByName.values();
    }

    @Override
    @Nullable
    public IMaterialRecipe getRecipe(String name) {
        return null;
    }

    public void register(Mat mat) {
        materialsById.put(mat.getID(), mat);
        materialsByName.put(mat.getName().materialName, mat);
    }

    public <T extends Enum<T> & IMaterialHandle<Mat>> void registerFromList(Class<T> list) {
        for (IMaterialHandle<Mat> handle : list.getEnumConstants()) {
            register(handle.getMaterial());
        }
    }
}
