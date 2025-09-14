package materiallib.api.lifecycle;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

public class MaterialLifecycles {

    private static final Map<Class<? extends MaterialLifecycleHandler>, List<MaterialLifecycleHandler>> DELEGATES = new HashMap<>();

    private static final HashSet<MaterialLifecycleEvent> dispatchedEvents = new HashSet<>();

    public static void register(MaterialLifecycleHandler obj) {
        HashSet<Class<? extends MaterialLifecycleEvent>> relevantEvents = new HashSet<>();

        obj.getRelevantEvents(relevantEvents);

        for (var clazz : relevantEvents) {
            if (dispatchedEvents.stream().anyMatch(clazz::isInstance)) {
                throw new IllegalStateException("Cannot register a MaterialLifecycleHandler with a relevant event that has already been dispatched: this indicates a logic error or a poorly defined initialization point.");
            }

            //noinspection unchecked
            DELEGATES.computeIfAbsent(
                    (Class<? extends MaterialLifecycleHandler>) clazz,
                    ignored -> new ArrayList<>())
                .add(obj);
        }
    }

    public static void invoke(MaterialLifecycleEvent event) {
        event.validatePrerequisites(dispatchedEvents);
        if (event.isTarget()) dispatchedEvents.add(event);

        List<MaterialLifecycleHandler> handlers;

        if (event.isTarget()) {
            handlers = DELEGATES.remove(event.getClass());
        } else {
            handlers = DELEGATES.get(event.getClass());
        }

        if (handlers == null) return;

        handlers.forEach(d -> d.beforeLifecycleEvent(event));
        handlers.forEach(d -> d.onLifecycleEvent(event));
        handlers.forEach(d -> d.afterLifecycleEvent(event));
    }

    public static void finish(Class<? extends MaterialLifecycleHandler> clazz) {
        DELEGATES.remove(clazz);
    }
}
