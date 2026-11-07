package linksawakening.world;

import linksawakening.world.entity.event.DialogRequest;

/** ROM handler stages for Cucco. */
final class CuccoHandler {
  static void installPhase3(EntityHandlerRegistry registry) {
    registry.registerTick(
        RoomEntityRuntime.ENTITY_CUCCO,
        EntityTickStage.PHASE_3_CUCCO,
        state -> {
          if (state.status == EntityStatus.ACTIVE
              && !state.wasInitializing

              && state.handlerLinkCollisionEnabled) {
            CuccoMotion.Update cuccoUpdate =
                state.runtime.cuccoMotion.advance(
                    state.entity,
                    state.frame,
                    state.runtime.enemyTransitionCountdown[state.entity.slot()],
                    state.runtime.enemyFlashCountdown[state.entity.slot()],
                    state.linkEntityX,
                    state.linkEntityY,
                    state.linkZ,
                    state.handlerLinkCollisionEnabled,
                    state.runtime.liftThrow.cuccoPowerBraceletHeld(),
                    state.runtime.linkAttackStepAnimationCountdown != 0,
                    state.randomByteSupplier,
                    state.backgroundCollision,
                    state.runtime.indoorRoom,
                    state.runtime.followingNpcState.marinFollowing(),
                    state.runtime.transitionSequenceCounter);
            state.updated = cuccoUpdate.entity();
            state.runtime.enemyTransitionCountdown[state.entity.slot()] =
                cuccoUpdate.transitionCountdown();
            state.runtime.enemyFlashCountdown[state.entity.slot()] = cuccoUpdate.flashCountdown();
            state.runtime.enemyHealth[state.entity.slot()] = CuccoMotion.HEALTH_OVERRIDE;
            state.preserveCuccoPresentation = true;
            if (cuccoUpdate.dialogLowId() >= 0) {
              state.runtime.pendingDialogRequests.add(
                  new DialogRequest(cuccoUpdate.dialogTableId(), cuccoUpdate.dialogLowId()));
            }
            if (cuccoUpdate.unloaded()) {
              state.runtime.lifecycle.disableEntityWithoutPersistence(state.entity.slot());
              return false;
            }
            if (cuccoUpdate.liftRequested()
                && state.runtime.beginLift(state.entity.slot(), state.romLinkDirection)) {
              state.runtime.pendingEntityEvents.add(
                  new EntityCombatEvent(
                      state.entity.slot(),
                      state.entity.type(),
                      0,
                      false,
                      EntityCombatEvent.SoundChannel.WAVE,
                      CuccoMotion.LIFT_WAVE_SFX));
              state.runtime.slots[state.index] =
                  state.runtime.liftThrow.advanceLiftedEntity(
                      state.runtime.liftThrow.renderLiftedEntity(
                          state.runtime.slots[state.index], state.frame),
                      state.linkEntityX,
                      state.linkEntityY,
                      state.linkZ,
                      state.romLinkDirection);
              return false;
            }
            if (cuccoUpdate.spawnRequest() != null
                && state.runtime.entityEnemySpawns.spawnCuccoAngry(cuccoUpdate.spawnRequest())) {
              state.runtime.pendingEntityEvents.add(
                  new EntityCombatEvent(
                      state.entity.slot(),
                      state.entity.type(),
                      0,
                      false,
                      EntityCombatEvent.SoundChannel.WAVE,
                      CuccoMotion.CUCCO_HURT_WAVE_SFX));
            }
            if (cuccoUpdate.boomerangSound()) {
              state.runtime.projectiles.playBoomerangSoundIfDue(state.updated);
            }
          }
          return true;
        });
  }
}
