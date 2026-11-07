package linksawakening.world;

import java.util.function.IntSupplier;

/** Slot-preserving adapter to a ROM entity handler's initialization phase. */
@FunctionalInterface
interface EntityHandler {
    void initialize(RoomEntity entity, IntSupplier randomByteSupplier);
    default RoomEntity advance(RoomEntity entity, EntityFrameContext context) { return entity; }
    default boolean tick(int stage, EntityFrameExecution state) { return true; }
}
