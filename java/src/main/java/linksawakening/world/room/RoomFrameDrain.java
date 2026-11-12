package linksawakening.world.room;

import java.util.List;

/** Synchronous callback over a read-only batch valid only for the duration of accept.
 * Snapshot-returning consume APIs remain available when values must be retained.
 */
@FunctionalInterface
public interface RoomFrameDrain<T> {
    void accept(List<? extends T> requests);
}
