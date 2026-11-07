package linksawakening.world.entity.handler;

import linksawakening.world.entity.EntityTickStage;
import linksawakening.world.entity.RoomEntityRuntime;
import linksawakening.world.entity.EntityStatus;
import linksawakening.world.entity.EntityHandlerRegistration;

/** ROM handler stages for SwordBeam. */
final class SwordBeamHandler {
  static void install(EntityHandlerRegistration registry) {
    registry.registerTick(
        RoomEntityRuntime.ENTITY_SWORD_BEAM,
        EntityTickStage.PHASE_1_SWORD_BEAM,
        state -> {
          if (state.status == EntityStatus.ACTIVE) {
            state.projectiles().advanceSwordBeamEntity(
                state.index,
                state.entity,
                state.frame,
                state.linkEntityX,
                state.linkEntityY,
                state.romLinkDirection,
                state.handlerInteractive);
            return false;
          }
          return true;
        });
  }
}
