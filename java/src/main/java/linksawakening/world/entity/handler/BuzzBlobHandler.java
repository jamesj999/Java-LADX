package linksawakening.world.entity.handler;

import linksawakening.world.entity.RoomEntityBackgroundInteraction;
import linksawakening.world.entity.EntityTickStage;
import linksawakening.world.entity.RoomEntityRuntime;
import linksawakening.world.entity.EntityStatus;
import linksawakening.world.entity.EntityHandlerRegistration;







import linksawakening.world.entity.motion.BuzzBlobMotion;



/** ROM handler stages for BuzzBlob. */
final class BuzzBlobHandler {
  static void install(EntityHandlerRegistration registry) {
    registry.registerTick(
        RoomEntityRuntime.ENTITY_BUZZ_BLOB,
        EntityTickStage.PHASE_4_BUZZ_BLOB,
        state -> {
          if (state.status == EntityStatus.ACTIVE
              && !state.wasInitializing

              && !state.creditsGameplay
              && state.handlerLinkCollisionEnabled) {
            int slot = state.entity.slot();
            RoomEntityBackgroundInteraction buzzBlobBackgroundInteraction =
                state.runtime.backgroundInteraction;
            if (buzzBlobBackgroundInteraction == null && state.backgroundCollision != null) {
              buzzBlobBackgroundInteraction =
                  RoomEntityBackgroundInteraction.fromBoolean(state.backgroundCollision);
            }
            BuzzBlobMotion.Update buzzBlobUpdate =
                state.runtime.buzzBlobMotion.advance(
                    state.updated,
                    state.frame,
                    state.runtime.enemyTransitionCountdown[slot],
                    state.randomByteSupplier,
                    buzzBlobBackgroundInteraction);
            state.updated = buzzBlobUpdate.entity();
            state.runtime.enemyTransitionCountdown[slot] = buzzBlobUpdate.transitionCountdown();
            state.applyGenericGroundInteraction = buzzBlobUpdate.appliesBackgroundInteraction();
            state.preserveBuzzBlobPresentation = true;
          }
          return true;
        });
  }
}
