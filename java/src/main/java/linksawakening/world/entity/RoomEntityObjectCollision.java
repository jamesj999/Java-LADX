package linksawakening.world.entity;

import java.util.Objects;

/**
 * Source-backed static room-object query for handlers that do not use the
 * ordinary one-axis background movement callback.
 */
@FunctionalInterface
public interface RoomEntityObjectCollision {
    RoomEntityObjectCollision NONE = entity -> false;

    static RoomEntityObjectCollision orNone(RoomEntityObjectCollision value) {
        return Objects.requireNonNullElse(value, NONE);
    }

    boolean collides(RoomEntity entity);
}
