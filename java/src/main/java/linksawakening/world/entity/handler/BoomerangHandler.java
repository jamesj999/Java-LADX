package linksawakening.world.entity.handler;

import static linksawakening.world.entity.EntityRomConstants.ENTITY_BOOMERANG;


import linksawakening.world.entity.EntityTickStage;
import linksawakening.world.entity.EntityStatus;
import linksawakening.world.entity.EntityHandlerRegistration;

/** ROM handler stages for Boomerang. */
final class BoomerangHandler {
  static void install(EntityHandlerRegistration registry) {
    registry.registerTick(
        ENTITY_BOOMERANG,
        EntityTickStage.PHASE_1_BOOMERANG,
        state -> {
          if (state.status == EntityStatus.ACTIVE) {
            state.projectiles().advanceBoomerangEntity(
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
