package linksawakening.world.entity.handler;

import linksawakening.world.entity.RoomEntityBackgroundInteraction;
import linksawakening.world.entity.EntityTickStage;
import linksawakening.world.entity.RoomEntityRuntime;
import linksawakening.world.entity.EntityStatus;
import linksawakening.world.entity.EntityHandlerRegistration;

import linksawakening.world.entity.motion.WitchRatMotion;

/** ROM handler stages for WitchRat. */
final class WitchRatHandler {
  static void install(EntityHandlerRegistration registry) {
    registry.registerTick(
        RoomEntityRuntime.ENTITY_WITCH_RAT,
        EntityTickStage.PHASE_4_WITCH_RAT,
        state -> {
          if (state.status == EntityStatus.ACTIVE
              && !state.wasInitializing

              && state.handlerLinkCollisionEnabled) {
            int slot = state.entity.slot();
            RoomEntityBackgroundInteraction witchRatBackgroundInteraction =
                state.runtime.backgroundInteraction;
            if (witchRatBackgroundInteraction == null && state.backgroundCollision != null) {
              witchRatBackgroundInteraction =
                  RoomEntityBackgroundInteraction.fromBoolean(state.backgroundCollision);
            }
            WitchRatMotion.Update witchRatUpdate =
                state.runtime.witchRatMotion.advance(
                    state.updated,
                    state.frame,
                    state.runtime.enemyTransitionCountdown[slot],
                    state.randomByteSupplier,
                    witchRatBackgroundInteraction);
            state.updated = witchRatUpdate.entity();
            state.runtime.enemyTransitionCountdown[slot] = witchRatUpdate.transitionCountdown();
            state.applyGenericGroundInteraction = witchRatUpdate.appliesBackgroundInteraction();
            state.preserveWitchRatPresentation = true;
          }
          return true;
        });
  }
}
