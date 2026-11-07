package linksawakening.world.entity.handler;

import linksawakening.world.EntityTickStage;
import linksawakening.world.RoomEntityRuntime;
import linksawakening.world.EntityStatus;
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
