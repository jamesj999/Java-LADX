package linksawakening.world;

import linksawakening.vfx.TransientVfxType;
import linksawakening.world.entity.event.TransientVfxRequest;

/** ROM handler stages for Fish. */
final class FishHandler {
  static void installPhase3(EntityHandlerRegistry registry) {
    registry.registerTick(
        RoomEntityRuntime.ENTITY_FISH,
        EntityTickStage.PHASE_3_FISH,
        state -> {
          if (state.status == EntityStatus.ACTIVE && !state.wasInitializing) {
            FishMotion.Update fishUpdate =
                state.runtime.fishMotion.advance(
                    state.entity,
                    state.runtime.enemyTransitionCountdown[state.entity.slot()],
                    state.randomByteSupplier,
                    state.backgroundCollision);
            state.updated = fishUpdate.entity();
            state.runtime.enemyTransitionCountdown[state.entity.slot()] =
                fishUpdate.transitionCountdown();
            state.runtime.enemyPhysicsFlags[state.entity.slot()] = fishUpdate.physicsFlags();
            if (fishUpdate.waterSplash()) {
              state.emit(
                  new TransientVfxRequest(
                      TransientVfxType.WATER_SPLASH, state.updated.x(), state.updated.y()));
              state.emit(
                  new EntityCombatEvent(
                      state.updated.slot(),
                      state.updated.type(),
                      0,
                      false,
                      EntityCombatEvent.SoundChannel.JINGLE,
                      0x0E));
            }
          }
          return true;
        });
  }
}
