package linksawakening.world;



/** ROM handler stages for WingedOctorok. */
final class WingedOctorokHandler {
  static void install(EntityHandlerRegistry registry) {
    registry.registerTick(
        RoomEntityRuntime.ENTITY_WINGED_OCTOROK,
        EntityTickStage.PHASE_3_WINGED_OCTOROK,
        state -> {
          if (state.status == EntityStatus.ACTIVE
              && !state.wasInitializing

              && state.handlerLinkCollisionEnabled) {
            WingedOctorokMotion.Update wingedUpdate =
                state.runtime.wingedOctorokMotion.advance(
                    state.entity,
                    state.frame,
                    state.linkEntityX,
                    state.linkEntityY,
                    state.randomByteSupplier,
                    state.backgroundCollision,
                    state.runtime.enemyTransitionCountdown[state.entity.slot()],
                    state.runtime.enemyIgnoreHitsCountdown[state.entity.slot()],
                    state.runtime.actionButtonAHeld,
                    state.runtime.actionButtonBHeld,
                    state.runtime.linkItemA,
                    state.runtime.linkItemB);
            state.updated = wingedUpdate.entity();
            state.runtime.enemyTransitionCountdown[state.entity.slot()] =
                wingedUpdate.transitionCountdown();
            state.preserveWingedOctorokPresentation = true;
            state.runtime.wingedStateTwoThisFrame[state.entity.slot()] =
                wingedUpdate.skippedDefaultCollision();
            if (wingedUpdate.rockSpawn() != null) {
              state.enemySpawns().spawnWingedOctorokRock(
                  state.updated, wingedUpdate.rockSpawn().direction());
            }
            if (wingedUpdate.jumped()) {
              state.runtime.wingedSwordAttackThisFrame[state.entity.slot()] = true;
              state.emit(
                  new EntityCombatEvent(
                      state.entity.slot(),
                      state.entity.type(),
                      0,
                      false,
                      EntityCombatEvent.SoundChannel.JINGLE,
                      0x24));
            }
          }
          return true;
        });
  }
}
