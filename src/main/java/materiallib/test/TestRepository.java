package materiallib.test;

import materiallib.api.annotations.LoadedEagerly;
import materiallib.api.recipe.IMaterialRecipe;
import materiallib.api.repository.StandardMaterialRepository;

@LoadedEagerly
public class TestRepository extends StandardMaterialRepository<TestMaterial> {

    public static final TestRepository INSTANCE = new TestRepository();

    private TestRepository() {
        super("test");
    }

    @Override
    public IMaterialRecipe getRecipe(String name) {
        return TestMaterialRecipes.valueOf(name);
    }
}
