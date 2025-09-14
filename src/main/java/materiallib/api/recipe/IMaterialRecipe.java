package materiallib.api.recipe;

import javax.annotation.Nullable;

import materiallib.api.repository.IMaterialRepository;
import materiallib.api.tags.Tag;

public interface IMaterialRecipe {

    @Nullable
    IMaterialRepository<?> getRepository();

    String getName();

    void addRequirement(Tag tag);
    void removeRequirement(Tag tag);

    void addBlocker(Tag tag);
    void removeBlocker(Tag tag);

}
