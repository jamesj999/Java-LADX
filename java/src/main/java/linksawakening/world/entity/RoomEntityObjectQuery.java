package linksawakening.world.entity;

import java.util.Objects;

/** Resolves the ROM {@code GetObjectUnderEntity} sample for a host entity. */
@FunctionalInterface
public interface RoomEntityObjectQuery {
    RoomEntityObjectQuery NONE = entity -> RoomEntityObjectSample.UNSAMPLED;

    static RoomEntityObjectQuery orNone(RoomEntityObjectQuery query) {
        return Objects.requireNonNullElse(query, NONE);
    }

    /** Normalize a query reporting no sample to the explicit absence value. */
    default RoomEntityObjectSample sampleOrAbsent(RoomEntity entity) {
        return Objects.requireNonNullElse(sample(entity), RoomEntityObjectSample.UNSAMPLED);
    }

    RoomEntityObjectSample sample(RoomEntity entity);
}
