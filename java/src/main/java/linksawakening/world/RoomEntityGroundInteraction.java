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

    record PitTransition(int targetX, int targetY) {
        public PitTransition {
            targetX &= 0xFF;
            targetY &= 0xFF;
        }
    }

    record Result(RoomEntity entity, int groundStatus, PitTransition pitTransition,
                  boolean unloaded, boolean waterSplash) {
        static Result unchanged(RoomEntity entity, int groundStatus) {
            return new Result(entity, groundStatus, null, false, false);
        }

        static Result unloaded(RoomEntity entity, int groundStatus, boolean waterSplash) {
            return new Result(entity, groundStatus, null, true, waterSplash);
        }

        static Result pit(RoomEntity entity, int groundStatus, int targetX, int targetY) {
            return new Result(entity, groundStatus,
                new PitTransition(targetX, targetY), false, false);
        }
    }
}
