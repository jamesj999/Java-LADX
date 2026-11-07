package linksawakening.world.entity.handler;

import linksawakening.world.entity.EntityTickStage;
import linksawakening.world.entity.RoomEntityRuntime;
import linksawakening.world.entity.EntityStatus;
import linksawakening.world.entity.EntityHandlerRegistration;
import linksawakening.world.entity.event.EntityCombatEvent;

import linksawakening.world.entity.motion.WingedOctorokMotion;

/** ROM handler stages for WingedOctorok. */
final class WingedOctorokHandler {
  static void install(EntityHandlerRegistration registry) {
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
                    state.slot().transitionCountdown(state.entity.slot()),
                    state.runtime.enemyIgnoreHitsCountdown[state.entity.slot()],
                    state.runtime.linkState.actionButtonAHeld,
                    state.runtime.linkState.actionButtonBHeld,
                    state.runtime.linkState.linkItemA,
                    state.runtime.linkState.linkItemB);
            state.updated = wingedUpdate.entity();
            state.slot().setTransitionCountdown(state.entity.slot(), wingedUpdate.transitionCountdown());
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
