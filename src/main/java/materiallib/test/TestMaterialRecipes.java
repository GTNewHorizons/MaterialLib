package materiallib.test;

import java.util.Arrays;
import java.util.HashSet;
import java.util.Objects;
import java.util.Set;
import java.util.function.BiConsumer;
import java.util.function.Consumer;

import net.minecraft.item.crafting.CraftingManager;

import it.unimi.dsi.fastutil.objects.ReferenceOpenHashSet;
import materiallib.api.annotations.LoadedEagerly;
import materiallib.api.enums.OrePrefix;
import materiallib.api.lifecycle.MaterialLifecycleEvent;
import materiallib.api.lifecycle.MaterialLifecycleHandler;
import materiallib.api.lifecycle.MaterialLifecycles;
import materiallib.api.lifecycle.events.RegisterRecipesEvent;
import materiallib.api.material.IMaterial;
import materiallib.api.recipe.IMaterialRecipe;
import materiallib.api.repository.IMaterialRepository;
import materiallib.api.tags.Tag;

@LoadedEagerly
public enum TestMaterialRecipes implements IMaterialRecipe {

    CraftGear(new RecipeBuilder()
        .addRequirement(OrePrefix.ingot)
        .addRequirement(OrePrefix.gearGt)
        .recipe((repo, material) -> {
            CraftingManager.getInstance().addRecipe(
                material.getItem(OrePrefix.gearGt, 1),
                " I ",
                "I I",
                " I ",
                'I', material.getItem(OrePrefix.ingot, 1)
            );
        }))

    ;

    private final ReferenceOpenHashSet<OrePrefix> requiredPrefixes = new ReferenceOpenHashSet<>();
    private final ReferenceOpenHashSet<Tag> requirements = new ReferenceOpenHashSet<>();
    private final ReferenceOpenHashSet<Tag> blockers = new ReferenceOpenHashSet<>();

    private final BiConsumer<IMaterialRepository<?>, IMaterial> recipe;

    TestMaterialRecipes(RecipeBuilder builder) {
        this.recipe = Objects.requireNonNull(builder.recipe);

        requiredPrefixes.addAll(builder.requiredPrefixes);
        requirements.addAll(builder.requirements);
        blockers.addAll(builder.blockers);
    }

    @Override
    public IMaterialRepository<?> getRepository() {
        return TestRepository.INSTANCE;
    }

    @Override
    public String getName() {
        return name();
    }

    @Override
    public void addRequirement(Tag tag) {
        requirements.add(tag);
    }

    @Override
    public void removeRequirement(Tag tag) {
        requirements.remove(tag);
    }

    @Override
    public void addBlocker(Tag tag) {
        blockers.add(tag);
    }

    @Override
    public void removeBlocker(Tag tag) {
        blockers.remove(tag);
    }

    private void generate(IMaterialRepository<?> repo, IMaterial material) {
        for (Tag tag : requirements) {
            if (!material.hasTag(tag)) return;
        }

        for (Tag tag : blockers) {
            if (material.hasTag(tag)) return;
        }

        for (OrePrefix prefix : requiredPrefixes) {
            if (!material.hasItem(prefix)) return;
        }

        recipe.accept(repo, material);
    }

    static {
        MaterialLifecycles.register(new MaterialLifecycleHandler() {

            @Override
            public void getRelevantEvents(Set<Class<? extends MaterialLifecycleEvent>> set) {
                set.add(RegisterRecipesEvent.class);
            }

            @Override
            public void onLifecycleEvent(MaterialLifecycleEvent event) {
                if (event instanceof RegisterRecipesEvent recipesEvent) {
                    for (TestMaterialRecipes recipe : values()) {
                        recipe.generate(recipesEvent.repository, recipesEvent.material);
                    }
                }
            }
        });
    }

    private static class RecipeBuilder {
        private BiConsumer<IMaterialRepository<?>, IMaterial> recipe;

        private final Set<OrePrefix> requiredPrefixes = new HashSet<>();
        private final Set<Tag> requirements = new HashSet<>();
        private final Set<Tag> blockers = new HashSet<>();

        public RecipeBuilder recipe(BiConsumer<IMaterialRepository<?>, IMaterial> recipe) {
            this.recipe = recipe;
            return this;
        }

        public RecipeBuilder recipe(Consumer<IMaterial> recipe) {
            this.recipe = (repo, material) -> recipe.accept(material);
            return this;
        }

        public RecipeBuilder addRequirement(OrePrefix prefix) {
            this.requiredPrefixes.add(prefix);
            return this;
        }

        public RecipeBuilder addRequirement(Tag tag) {
            this.requirements.add(tag);
            return this;
        }

        public RecipeBuilder addBlocker(Tag tag) {
            this.blockers.add(tag);
            return this;
        }

        public RecipeBuilder addRequirement(String tag) {
            this.requirements.add(Tag.getTag(tag));
            return this;
        }

        public RecipeBuilder addBlocker(String tag) {
            this.blockers.add(Tag.getTag(tag));
            return this;
        }
    }
}
