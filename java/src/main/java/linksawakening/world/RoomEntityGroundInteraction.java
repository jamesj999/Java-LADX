package linksawakening.world;

/**
 * Source-shaped room-ground pass executed after an active entity handler has
 * produced its frame update. The session owns ROM object physics; the entity
 * runtime owns the ordering relative to entity-family motion.
 */
@FunctionalInterface
interface RoomEntityGroundInteraction {
    Result apply(RoomEntity entity, int frameCounter, int previousGroundStatus,
                 int speedZ, boolean sideScrolling);

    record Result(RoomEntity entity, int groundStatus, boolean unloaded,
                  boolean waterSplash) {
        static Result unchanged(RoomEntity entity, int groundStatus) {
            return new Result(entity, groundStatus, false, false);
        }

        static Result unloaded(RoomEntity entity, int groundStatus, boolean waterSplash) {
            return new Result(entity, groundStatus, true, waterSplash);
        }
    }
}
