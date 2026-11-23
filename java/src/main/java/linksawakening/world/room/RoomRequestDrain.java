package linksawakening.world.room;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/** Room-owned staging; queues are cleared before the first consumer effect. */
final class RoomRequestDrain<T> {
    private final List<T> staging = new ArrayList<>(16);
    private final List<T> view = Collections.unmodifiableList(staging);

    void drain(List<? extends T> pending, RoomFrameDrain<? super T> target) {
        drain(List.of(), pending, target);
    }

    void drain(List<? extends T> first, List<? extends T> pending, RoomFrameDrain<? super T> target) {
        if (pending.isEmpty()) {
            target.accept(first);
            return;
        }
        if (!staging.isEmpty()) throw new IllegalStateException("Reentrant frame drain");
        for (int i = 0; i < first.size(); i++) staging.add(first.get(i));
        for (int i = 0; i < pending.size(); i++) staging.add(pending.get(i));
        pending.clear();
        try {
            target.accept(view);
        } finally {
            staging.clear();
        }
    }
}
