package linksawakening.world.entity;

import java.util.Objects;

/** Rich entity/background probe corresponding to the ROM collision helper. */
@FunctionalInterface
public interface RoomEntityBackgroundInteraction {
    /** No room probe: preserves the absent-interaction path without allocating a result. */
    RoomEntityBackgroundInteraction NONE = (entity, direction, x, y) -> EntityBackgroundCollisionResult.NONE;

    /** Compatibility boundary for callers predating explicit room defaults. */
    static RoomEntityBackgroundInteraction orNone(RoomEntityBackgroundInteraction value) {
        return Objects.requireNonNullElse(value, NONE);
    }

    static RoomEntityBackgroundInteraction orBoolean(
            RoomEntityBackgroundInteraction rich, RoomEntityBackgroundCollision basic) {
        RoomEntityBackgroundInteraction selected = orNone(rich);
        return selected == NONE ? fromBoolean(basic) : selected;
    }

    EntityBackgroundCollisionResult probe(RoomEntity entity, int direction, int nextX, int nextY);

    /**
     * Rich probe variant carrying the ROM's current ignore-hits countdown.
     * Existing four-argument callers retain their original behavior.
     */
    default EntityBackgroundCollisionResult probe(
            RoomEntity entity, int direction, int nextX, int nextY, int ignoreHitsCountdown) {
        return probe(entity, direction, nextX, nextY);
    }

    /** Rich probe variant carrying the current ROM frame for stateful physics. */
    default EntityBackgroundCollisionResult probe(
            RoomEntity entity, int direction, int nextX, int nextY, int ignoreHitsCountdown, int frameCounter) {
        return probe(entity, direction, nextX, nextY, ignoreHitsCountdown);
    }

    static RoomEntityBackgroundInteraction fromBoolean(RoomEntityBackgroundCollision backgroundCollision) {
        backgroundCollision = RoomEntityBackgroundCollision.orOpen(backgroundCollision);
        if (backgroundCollision == RoomEntityBackgroundCollision.OPEN) return NONE;
        RoomEntityBackgroundCollision collision = backgroundCollision;
        return (entity, direction, nextX, nextY) -> EntityBackgroundCollisionResult.fromBoolean(
                collision.blocks(entity, direction, nextX, nextY), direction, nextX, nextY);
    }
}
