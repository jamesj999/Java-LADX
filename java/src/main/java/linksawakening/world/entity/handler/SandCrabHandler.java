package linksawakening.world.entity.handler;

import linksawakening.world.RoomEntityBackgroundInteraction;
import linksawakening.world.EntityTickStage;
import linksawakening.world.RoomEntityRuntime;
import linksawakening.world.EntityStatus;
import linksawakening.world.entity.EntityHandlerRegistration;







import linksawakening.world.entity.motion.SandCrabMotion;



/** ROM handler stages for SandCrab. */
final class SandCrabHandler {
  static void install(EntityHandlerRegistration registry) {
    registry.registerTick(
        RoomEntityRuntime.ENTITY_SAND_CRAB,
        EntityTickStage.PHASE_4_SAND_CRAB,
        state -> {
          if (state.status == EntityStatus.ACTIVE
              && !state.wasInitializing

              && !state.creditsGameplay
              && state.handlerLinkCollisionEnabled) {
            int slot = state.entity.slot();
            RoomEntityBackgroundInteraction sandCrabBackgroundInteraction =
                state.runtime.backgroundInteraction;
            if (sandCrabBackgroundInteraction == null && state.backgroundCollision != null) {
              sandCrabBackgroundInteraction =
                  RoomEntityBackgroundInteraction.fromBoolean(state.backgroundCollision);
            }
            SandCrabMotion.Update sandCrabUpdate =
                state.runtime.sandCrabMotion.advance(
                    state.updated,
                    state.frame,
                    state.runtime.enemyTransitionCountdown[slot],
                    state.randomByteSupplier,
                    sandCrabBackgroundInteraction);
            state.updated = sandCrabUpdate.entity();
            state.runtime.enemyTransitionCountdown[slot] = sandCrabUpdate.transitionCountdown();
            state.applyGenericGroundInteraction = sandCrabUpdate.appliesBackgroundInteraction();
            state.preserveSandCrabPresentation = true;
          }
          return true;
        });
  }
}
