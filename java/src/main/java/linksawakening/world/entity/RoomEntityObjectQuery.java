package linksawakening.world.entity;

/** Resolves the ROM {@code GetObjectUnderEntity} sample for a host entity. */
@FunctionalInterface
public interface RoomEntityObjectQuery {
    RoomEntityObjectQuery NONE = entity -> RoomEntityObjectSample.UNSAMPLED;

    static RoomEntityObjectQuery orNone(RoomEntityObjectQuery query) {
        return java.util.Objects.requireNonNullElse(query, NONE);
    }

    /** Normalizes legacy query implementations that use null to signal no sample. */
    default RoomEntityObjectSample sampleOrAbsent(RoomEntity entity) {
        return java.util.Objects.requireNonNullElse(sample(entity), RoomEntityObjectSample.UNSAMPLED);
    }

    RoomEntityObjectSample sample(RoomEntity entity);
}
