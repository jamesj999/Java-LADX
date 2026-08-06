package linksawakening.world;

/** Resolves the ROM {@code GetObjectUnderEntity} sample for a host entity. */
@FunctionalInterface
public interface RoomEntityObjectQuery {
    RoomEntityObjectSample sample(RoomEntity entity);
}
