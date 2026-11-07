package linksawakening.world.entity.handler;

import linksawakening.world.entity.EntityTickStage;
import linksawakening.world.entity.EntityStatus;
import linksawakening.world.entity.EntityHandlerRegistration;

import linksawakening.world.entity.RoomEntityRuntime.CommonDroppableUpdate;

import static linksawakening.world.entity.EntitySnapshots.withStatus;
import static linksawakening.world.entity.EntitySnapshots.withVariant;
import static linksawakening.world.entity.EntityTypeRules.isCommonDroppableType;

/** ROM handler stages for CommonDroppableType. */
final class CommonDroppableTypeHandler {
  static void install(EntityHandlerRegistration registry) {
    for (int type = 0; type < 256; type++) {
      if ((isCommonDroppableType(type))) {
        registry.registerTick(
            type,
            EntityTickStage.PHASE_2_COMMON_DROPPABLE_TYPE,
            state -> {
              if (state.wasInitializing

                  && state.runtime.droppablePrivateState3[state.entity.slot()] != 0) {
                // The init handler has already set the hidden options. The
                // corresponding active handler will be entered on the next
                // entity tick, so suppress this first display-list pass too.
                state.updated = withVariant(state.entity, -1);
                state.preserveDroppablePresentation = true;
              }
              return true;
            });
      }
    }
    for (int type = 0; type < 256; type++) {
      if ((isCommonDroppableType(type))) {
        registry.registerTick(
            type,
            EntityTickStage.PHASE_2_COMMON_DROPPABLE_TYPE_2,
            state -> {
              if (state.status == EntityStatus.ACTIVE && !state.wasInitializing) {
                CommonDroppableUpdate droppableUpdate =
                    state.pickups().advanceCommonDroppable(
                        state.entity,
                        state.linkEntityX,
                        state.linkEntityY,
                        state.background().objectQuery() == null
                            ? null
                            : state.background().objectQuery().sample(state.entity));
                state.updated = droppableUpdate.entity();
                if (droppableUpdate.hidden()) {
                  // DroppableRevealOrReturnIfNeeded returns past the caller's
                  // remaining handler. In particular, no rendering, pickup,
                  // movement, or background interaction may run while hidden.
                  state.replaceEntity(withStatus(withVariant(state.updated, -1), state.status));
                  return false;
                }
              }
              return true;
            });
      }
    }
  }
}
