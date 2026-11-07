package linksawakening.world.entity.handler;

import linksawakening.world.EntityTickStage;
import linksawakening.world.RoomEntityRuntime;
import linksawakening.world.EntityStatus;
import linksawakening.world.entity.EntityHandlerRegistration;







import linksawakening.world.entity.motion.PiranhaMotion;



/** ROM handler stages for PiranhaPlant. */
final class PiranhaPlantHandler {
  static void install(EntityHandlerRegistration registry) {
    registry.registerTick(
        RoomEntityRuntime.ENTITY_PIRANHA_PLANT,
        EntityTickStage.PHASE_4_PIRANHA_PLANT,
        state -> {
          if (state.status == EntityStatus.ACTIVE
              && !state.wasInitializing

              && !state.creditsGameplay
              && state.runtime.transitionSequenceCounter == 0x04
              && state.handlerLinkCollisionEnabled) {
            int slot = state.entity.slot();
            PiranhaMotion.Update piranhaUpdate =
                state.runtime.piranhaMotion.advance(
                    state.updated,
                    state.runtime.enemyTransitionCountdown[slot],
                    state.linkEntityX,
                    state.linkEntityY);
            state.updated = piranhaUpdate.entity();
            state.runtime.enemyTransitionCountdown[slot] = piranhaUpdate.transitionCountdown();
            state.preservePiranhaPresentation = true;
          }
          return true;
        });
  }
}
