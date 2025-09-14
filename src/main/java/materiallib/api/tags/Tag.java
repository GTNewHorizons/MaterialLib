package materiallib.api.tags;

import java.util.HashMap;
import java.util.Map;

import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.ToString;

@EqualsAndHashCode
@ToString
public class Tag {

    @Getter
    private final String name;

    private Tag(String name) {
        this.name = name;
    }

    private static final Map<String, Tag> TAGS = new HashMap<>();

    public static Tag getTag(String name) {
        return TAGS.computeIfAbsent(name, Tag::new);
    }
}
