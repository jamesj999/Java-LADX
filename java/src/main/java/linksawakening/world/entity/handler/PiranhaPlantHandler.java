package linksawakening.world.entity.handler;

import static linksawakening.world.entity.EntityRomConstants.ENTITY_PIRANHA_PLANT;


import linksawakening.world.entity.EntityTickStage;
import linksawakening.world.entity.EntityStatus;
import linksawakening.world.entity.EntityHandlerRegistration;

import linksawakening.world.entity.motion.PiranhaMotion;

/** ROM handler stages for PiranhaPlant. */
final class PiranhaPlantHandler {
  static void install(EntityHandlerRegistration registry) {
    registry.registerTick(
        ENTITY_PIRANHA_PLANT,
        EntityTickStage.PHASE_4_PIRANHA_PLANT,
        state -> {
          if (state.status == EntityStatus.ACTIVE
              && !state.wasInitializing

              && !state.creditsGameplay
              && state.lifecycle().transitionSequenceCounter() == 0x04
              && state.handlerLinkCollisionEnabled) {
            int slot = state.entity.slot();
            PiranhaMotion.Update piranhaUpdate =
                state.piranhaMotion().advance(
                    state.updated,
                    state.slot().transitionCountdown(slot),
                    state.linkEntityX,
                    state.linkEntityY);
            state.updated = piranhaUpdate.entity();
            state.slot().setTransitionCountdown(slot, piranhaUpdate.transitionCountdown());
            state.preservePiranhaPresentation = true;
          }
          return true;
        });
  }
}
