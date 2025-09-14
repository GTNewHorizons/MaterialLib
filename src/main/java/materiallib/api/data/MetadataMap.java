package materiallib.api.data;

import java.util.AbstractMap;
import java.util.AbstractSet;
import java.util.HashSet;
import java.util.Iterator;
import java.util.NoSuchElementException;
import java.util.Set;

import org.jetbrains.annotations.NotNull;

import com.google.common.collect.Iterators;
import com.google.common.collect.PeekingIterator;
import it.unimi.dsi.fastutil.objects.Object2ObjectOpenHashMap;
import materiallib.api.material.metadata.MaterialMetadata;

public class MetadataMap extends AbstractMap<materiallib.api.material.metadata.MaterialMetadata<?>, Object> {

    private final Object2ObjectOpenHashMap<MaterialMetadata<?>, Object> values = new Object2ObjectOpenHashMap<>();
    private final Object2ObjectOpenHashMap<MaterialMetadata<?>, Object> derived = new Object2ObjectOpenHashMap<>();

    @Override
    public final Object get(Object key) {
        Object value = values.get(key);

        if (value != null) return value;

        return derived.get(key);
    }

    @SuppressWarnings("unchecked")
    public final <T> T getMeta(MaterialMetadata<T> key) {
        return (T) this.get(key);
    }

    @SuppressWarnings("unchecked")
    public final <T> T getMetaOrDefault(MaterialMetadata<T> key) {
        T value = (T) this.get(key);

        if (value != null) return value;

        return key.getDefaultValue();
    }

    @Override
    public Object put(MaterialMetadata<?> key, Object value) {
        return values.put(key, value);
    }

    @SuppressWarnings("unchecked")
    public final <T> T putMeta(MaterialMetadata<T> key, T value) {
        T old = (T) values.put(key, value);

        MetadataMap derived = key.getDerivedValues(value);

        if (derived != null) {
            this.derived.putAll(derived);
        }

        return old;
    }

    @Override
    public @NotNull Set<Entry<MaterialMetadata<?>, Object>> entrySet() {
        HashSet<MaterialMetadata<?>> keys = new HashSet<>();
        keys.addAll(values.keySet());
        keys.addAll(derived.keySet());

        return new AbstractSet<>() {

            @Override
            public @NotNull Iterator<Entry<MaterialMetadata<?>, Object>> iterator() {
                HashSet<MaterialMetadata<?>> visited = new HashSet<>(MetadataMap.this.values.keySet());

                Iterator<Entry<MaterialMetadata<?>, Object>> valueIter = MetadataMap.this.values.entrySet().iterator();

                PeekingIterator<Entry<MaterialMetadata<?>, Object>> derivedIter = Iterators.peekingIterator(
                    MetadataMap.this.derived.entrySet().iterator());

                return new Iterator<>() {

                    @Override
                    public boolean hasNext() {
                        if (valueIter.hasNext()) return true;

                        while (derivedIter.hasNext()) {
                            Entry<MaterialMetadata<?>, Object> e = derivedIter.peek();

                            if (!visited.contains(e.getKey())) return true;

                            derivedIter.next();
                        }

                        return false;
                    }

                    @Override
                    public Entry<MaterialMetadata<?>, Object> next() {
                        if (valueIter.hasNext()) {
                            return valueIter.next();
                        }

                        while (derivedIter.hasNext()) {
                            var e = derivedIter.next();

                            if (visited.add(e.getKey())) {
                                return e;
                            }
                        }

                        throw new NoSuchElementException();
                    }
                };
            }

            @Override
            public int size() {
                return keys.size();
            }

            public boolean isEmpty() {
                return keys.isEmpty();
            }

            public void clear() {
                values.clear();
                derived.clear();
            }
        };
    }
}
