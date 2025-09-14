package materiallib.api.interfaces;

import java.util.concurrent.atomic.AtomicInteger;

public interface ObjectWithId {

    AtomicInteger NEXT_ID = new AtomicInteger(0);

    int getId();
}
