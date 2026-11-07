package linksawakening.world.entity.handler;

import linksawakening.world.entity.EntityTickStage;
import linksawakening.world.entity.RoomEntityRuntime;
import linksawakening.world.entity.EntityStatus;
import linksawakening.world.entity.EntityHandlerRegistration;

/** ROM handler stages for MagicPowderSprinkle. */
final class MagicPowderSprinkleHandler {
  static void install(EntityHandlerRegistration registry) {
    registry.registerTick(
        RoomEntityRuntime.ENTITY_MAGIC_POWDER_SPRINKLE,
        EntityTickStage.PHASE_1_MAGIC_POWDER_SPRINKLE,
        state -> {
          if (state.status == EntityStatus.ACTIVE) {
            state.projectiles().advanceMagicPowderSprinkleEntity(
                state.index,
                state.entity,
                state.frame,
                state.projectileLinkState.motionState(),
                state.handlerInteractive && !state.creditsGameplay,
                state.projectileTimersInteractive);
            return false;
          }
          return true;
        });
  }
}
