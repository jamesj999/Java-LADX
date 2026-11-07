package linksawakening.world;

import linksawakening.world.entity.event.DialogRequest;

/** ROM handler stages for Dog. */
final class DogHandler {
  static void install(EntityHandlerRegistry registry) {
    registry.registerTick(
        RoomEntityRuntime.ENTITY_DOG,
        EntityTickStage.PHASE_1_DOG,
        state -> {
          if (state.status == EntityStatus.ACTIVE) {
            // DogEntityHandler writes this before ReturnIfNonInteractive_19.
            state.runtime.enemyHealth[state.entity.slot()] = DogMotion.HANDLER_HEALTH;
          }
          return true;
        });
    registry.registerTick(
        RoomEntityRuntime.ENTITY_DOG,
        EntityTickStage.PHASE_4_DOG,
        state -> {
          if (state.status == EntityStatus.ACTIVE
              && !state.wasInitializing

              && !state.creditsGameplay
              && state.handlerLinkCollisionEnabled) {
            int slot = state.entity.slot();
            RoomEntityBackgroundInteraction dogBackgroundInteraction =
                state.runtime.backgroundInteraction;
            if (dogBackgroundInteraction == null && state.backgroundCollision != null) {
              dogBackgroundInteraction =
                  RoomEntityBackgroundInteraction.fromBoolean(state.backgroundCollision);
            }
            DogMotion.Update dogUpdate =
                state.runtime.dogMotion.advance(
                    state.updated,
                    state.frame,
                    state.runtime.enemyTransitionCountdown[slot],
                    state.runtime.enemyPhysicsFlags[slot],
                    state.linkEntityX,
                    state.linkEntityY,
                    state.romLinkDirection,
                    state.runtime.actionButtonAHeld,
                    state.runtime.dialogActive,
                    state.randomByteSupplier,
                    dogBackgroundInteraction);
            state.updated = dogUpdate.entity();
            state.runtime.enemyTransitionCountdown[slot] = dogUpdate.transitionCountdown();
            state.runtime.enemyPhysicsFlags[slot] = dogUpdate.physicsFlags();
            state.applyGenericGroundInteraction = dogUpdate.appliesBackgroundInteraction();
            if (dogUpdate.dialogRequested()) {
              state.emit(
                  new DialogRequest(0, dogUpdate.dialogLowId()));
            }
            state.preserveDogPresentation = true;
          }
          return true;
        });
  }
}
