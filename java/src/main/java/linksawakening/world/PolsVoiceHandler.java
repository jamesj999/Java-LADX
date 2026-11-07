package linksawakening.world;

import static linksawakening.world.EntitySnapshots.withStatus;

/** ROM handler stages for PolsVoice. */
final class PolsVoiceHandler {
  static void installPhase2(EntityHandlerRegistry registry) {
    registry.registerTick(
        RoomEntityRuntime.ENTITY_POLS_VOICE,
        EntityTickStage.PHASE_2_POLS_VOICE,
        state -> {
          if (state.status == EntityStatus.ACTIVE
              && !state.wasInitializing

              && state.runtime.enemyInspection.polsVoiceBalladOcarinaTrigger()) {
            // The handler checks this before rendering, recoil, movement,
            // or the ordinary collision path. Its private countdown $1F
            // is the shared EntityDeathHandler's visible death timer.
            state.runtime.dyingCountdown[state.entity.slot()] = 0x1F;
            state.runtime.enemyPhysicsFlags[state.entity.slot()] = 0x04;
            state.status = EntityStatus.DYING;
            state.updated = withStatus(state.entity, EntityStatus.DYING);
            state.runtime.pendingEntityEvents.add(
                new EntityCombatEvent(
                    state.entity.slot(),
                    state.entity.type(),
                    0,
                    false,
                    EntityCombatEvent.SoundChannel.NOISE,
                    0x13));
          }
          return true;
        });
  }

  static void installPhase4(EntityHandlerRegistry registry) {
    registry.registerTick(
        RoomEntityRuntime.ENTITY_POLS_VOICE,
        EntityTickStage.PHASE_4_POLS_VOICE,
        state -> {
          if (state.status == EntityStatus.ACTIVE && !state.wasInitializing) {
            // PolsVoiceEntityHandler deliberately writes the temporary
            // ignore-hits byte after moving, so the background helper sees
            // $01, then clears it with B (zero) before the default enemy
            // collision pass. The shared Java movement probe is the
            // handler's background-interaction seam, so bracket it here.
            state.runtime.enemyIgnoreHitsCountdown[state.entity.slot()] = 0x01;
            try {
              PolsVoiceMotion.Update polsVoiceUpdate =
                  state.runtime.polsVoiceMotion.advance(
                      state.entity,
                      state.linkEntityX,
                      state.linkEntityY,
                      state.randomByteSupplier,
                      state.backgroundCollision,
                      state.runtime.enemyTransitionCountdown[state.entity.slot()]);
              state.updated = polsVoiceUpdate.entity();
              state.runtime.enemyTransitionCountdown[state.entity.slot()] =
                  polsVoiceUpdate.transitionCountdown();
            } finally {
              state.runtime.enemyIgnoreHitsCountdown[state.entity.slot()] = 0;
            }
            state.preservePolsVoicePresentation = true;
          }
          return true;
        });
  }
}
