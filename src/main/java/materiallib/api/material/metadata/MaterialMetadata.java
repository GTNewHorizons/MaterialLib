package materiallib.api.material.metadata;

import materiallib.api.data.MetadataMap;

public interface MaterialMetadata<T> {

    String getName();

    @SuppressWarnings("unchecked")
    default T cast(Object obj) {
        return (T) obj;
    }

    default T getDefaultValue() {
        return null;
    }

    /**
     * If this metadata field should update another field, use this method to return those values. The returned values
     * are treated as default values and will be hidden if their values are set separately.
     */
    default MetadataMap getDerivedValues(T value) {
        return null;
    }
}
