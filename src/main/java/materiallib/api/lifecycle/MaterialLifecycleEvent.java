package materiallib.api.lifecycle;

import java.util.Collection;

public interface MaterialLifecycleEvent {

    default void validatePrerequisites(Collection<MaterialLifecycleEvent> dispatchedEvents) {

    }

    /**
     * When true, this event acts like a systemd target. That is: when it is fired, a significant state transition has
     * occurred in the program and consumers are free to do whatever they need to do within this new stage (pre init,
     * init, post init, etc).
     * When this event is not a target, it means some specific event has occurred. Consumers should filter non-target
     * events (i.e. some specific material has been registered vs all material registration has finished).
     * If this is true, then this event is a target and its handlers will be automatically freed when this event
     * is invoked.
     * If this is false, then this event is an instance event and does not act like a goal. Its handlers will not be
     * automatically cleared, and must be freed manually.
     */
    default boolean isTarget() {
        return true;
    }
}
