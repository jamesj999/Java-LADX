package linksawakening.world;

/**
 * Source-shaped room-ground pass executed after an active entity handler has
 * produced its frame update. The session owns ROM object physics; the entity
 * runtime owns the ordering relative to entity-family motion.
 */
@FunctionalInterface
interface RoomEntityGroundInteraction {
    RoomEntity apply(RoomEntity entity, int frameCounter);
}
