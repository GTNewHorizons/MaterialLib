package materiallib.test;

import materiallib.api.annotations.LoadedEagerly;
import materiallib.api.material.IMaterialHandle;

@LoadedEagerly
public enum TestMaterialList implements IMaterialHandle<TestMaterial> {
    Test1
    ;

    private TestMaterial material;

    @Override
    public void setMaterial(TestMaterial material) {
        this.material = material;
    }

    @Override
    public TestMaterial getMaterial() {
        return material;
    }

    static {
        init();
    }

    private static void init() {
        Test1.setMaterial(new TestMaterial(0, "Test"));

        TestRepository.INSTANCE.registerFromList(TestMaterialList.class);
    }
}
