package materiallib.api.material;

public interface IMaterialHandle<Mat extends IMaterial> {

    void setMaterial(Mat material);

    Mat getMaterial();
}
