package linksawakening.world;

import java.util.Objects;

/** Rich entity/background probe corresponding to the ROM collision helper. */
@FunctionalInterface
public interface RoomEntityBackgroundInteraction {
    EntityBackgroundCollisionResult probe(RoomEntity entity, int direction,
                                           int nextX, int nextY);

    /**
     * Rich probe variant carrying the ROM's current ignore-hits countdown.
     * Existing four-argument callers retain their original behavior.
     */
    default EntityBackgroundCollisionResult probe(RoomEntity entity, int direction,
                                                    int nextX, int nextY,
                                                    int ignoreHitsCountdown) {
        return probe(entity, direction, nextX, nextY);
    }

    /** Rich probe variant carrying the current ROM frame for stateful physics. */
    default EntityBackgroundCollisionResult probe(RoomEntity entity, int direction,
                                                    int nextX, int nextY,
                                                    int ignoreHitsCountdown,
                                                    int frameCounter) {
        return probe(entity, direction, nextX, nextY, ignoreHitsCountdown);
    }

    static RoomEntityBackgroundInteraction fromBoolean(
            RoomEntityBackgroundCollision backgroundCollision) {
        Objects.requireNonNull(backgroundCollision, "backgroundCollision");
        return (entity, direction, nextX, nextY) ->
            EntityBackgroundCollisionResult.fromBoolean(
                backgroundCollision.blocks(entity, direction, nextX, nextY),
                direction, nextX, nextY);
    }
}
