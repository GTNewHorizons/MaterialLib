package materiallib.api.material;

import java.util.Objects;

public class MaterialIdentifier {

    public final String repositoryName;
    public final String materialName;

    public MaterialIdentifier(String repositoryName, String materialName) {
        this.repositoryName = repositoryName;
        this.materialName = materialName;
    }

    public MaterialIdentifier(String id) {
        String[] parts = id.split(":");

        this.repositoryName = parts[0];
        this.materialName = parts[1];
    }

    @Override
    public final boolean equals(Object o) {
        if (!(o instanceof MaterialIdentifier that)) return false;

        return Objects.equals(repositoryName, that.repositoryName) && Objects.equals(materialName, that.materialName);
    }

    @Override
    public int hashCode() {
        int result = Objects.hashCode(repositoryName);
        result = 31 * result + Objects.hashCode(materialName);
        return result;
    }

    public String describe() {
        return repositoryName + "." + materialName;
    }

    @Override
    public String toString() {
        return "MaterialIdentifier{"
            + "repositoryName='"
            + repositoryName
            + '\''
            + ", materialName='"
            + materialName
            + '\''
            + '}';
    }
}
