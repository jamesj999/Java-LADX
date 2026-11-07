package linksawakening.world.entity.handler;

import linksawakening.world.entity.EntityTickStage;
import linksawakening.world.entity.RoomEntityRuntime;
import linksawakening.world.entity.EntityStatus;
import linksawakening.world.entity.EntityHandlerRegistration;

/** ROM handler stages for LiftableRock. */
final class LiftableRockHandler {
  static void install(EntityHandlerRegistration registry) {
    registry.registerTick(
        RoomEntityRuntime.ENTITY_LIFTABLE_ROCK,
        EntityTickStage.PHASE_1_LIFTABLE_ROCK,
        state -> {
          if (state.status == EntityStatus.ACTIVE

              && state.runtime.liftableRockSmashActive[state.entity.slot()]) {
            state.runtime.liftThrow.advanceLiftableRockSmash(
                state.index, state.entity, state.frame);
            return false;
          }
          return true;
        });
  }
}
