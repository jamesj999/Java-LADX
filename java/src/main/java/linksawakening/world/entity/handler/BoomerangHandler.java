package linksawakening.world.entity.handler;

import linksawakening.world.EntityTickStage;
import linksawakening.world.RoomEntityRuntime;
import linksawakening.world.EntityStatus;
import linksawakening.world.entity.EntityHandlerRegistration;









/** ROM handler stages for Boomerang. */
final class BoomerangHandler {
  static void install(EntityHandlerRegistration registry) {
    registry.registerTick(
        RoomEntityRuntime.ENTITY_BOOMERANG,
        EntityTickStage.PHASE_1_BOOMERANG,
        state -> {
          if (state.status == EntityStatus.ACTIVE) {
            state.runtime.projectiles.advanceBoomerangEntity(
                state.index,
                state.entity,
                state.frame,
                state.linkEntityX,
                state.linkEntityY,
                state.linkZ,
                state.projectileLinkState.motionState(),
                state.handlerInteractive,
                state.projectileTimersInteractive);
            return false;
          }
          return true;
        });
  }
}
