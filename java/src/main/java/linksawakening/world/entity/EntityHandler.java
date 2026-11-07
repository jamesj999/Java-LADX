package linksawakening.world.entity;

import java.util.function.IntSupplier;

/** Slot-preserving ROM type behavior across initialization and ordered frame stages. */
@FunctionalInterface
interface EntityHandler {
  void initialize(RoomEntity entity, IntSupplier randomByteSupplier);

  default RoomEntity advance(RoomEntity entity, EntityFrameContext context) {
    return entity;
  }

  default boolean tick(EntityTickStage stage, EntityFrameExecution state) {
    return true;
  }
}
