package materiallib.api.lifecycle;

import java.util.Set;

public interface MaterialLifecycleHandler {

    void getRelevantEvents(Set<Class<? extends MaterialLifecycleEvent>> set);

    default void beforeLifecycleEvent(MaterialLifecycleEvent event) {

    }

    default void onLifecycleEvent(MaterialLifecycleEvent event) {

    }

    default void afterLifecycleEvent(MaterialLifecycleEvent event) {

    }
}
