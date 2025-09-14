package materiallib.api.lifecycle.events;

import materiallib.api.lifecycle.MaterialLifecycleEvent;
import materiallib.api.material.IMaterial;
import materiallib.api.repository.IMaterialRepository;

public class RegisterRecipesEvent implements MaterialLifecycleEvent {

    public final IMaterialRepository<?> repository;
    public final IMaterial material;

    public RegisterRecipesEvent(IMaterialRepository<?> repository, IMaterial material) {
        this.repository = repository;
        this.material = material;
    }

    @Override
    public boolean isTarget() {
        return false;
    }
}
