package linksawakening.world.entity.handler;

import linksawakening.world.entity.EntityTickStage;
import linksawakening.world.entity.RoomEntityRuntime;
import linksawakening.world.entity.EntityStatus;
import linksawakening.world.entity.EntityHandlerRegistration;







import linksawakening.world.entity.motion.SpikedBeetleMotion;



/** ROM handler stages for SpikedBeetle. */
final class SpikedBeetleHandler {
  static void install(EntityHandlerRegistration registry) {
    registry.registerTick(
        RoomEntityRuntime.ENTITY_SPIKED_BEETLE,
        EntityTickStage.PHASE_4_SPIKED_BEETLE,
        state -> {
          if (state.status == EntityStatus.ACTIVE && !state.wasInitializing) {
            SpikedBeetleMotion.Update spikedBeetleUpdate =
                state.runtime.spikedBeetleMotion.advance(
                    state.entity,
                    state.frame,
                    state.linkEntityX,
                    state.linkEntityY,
                    state.randomByteSupplier,
                    state.backgroundCollision,
                    state.runtime.enemyTransitionCountdown[state.entity.slot()],
                    state.runtime.enemyIgnoreHitsCountdown[state.entity.slot()]);
            state.updated = spikedBeetleUpdate.entity();
            state.runtime.enemyTransitionCountdown[state.entity.slot()] =
                spikedBeetleUpdate.transitionCountdown();
            state.runtime.entityOptions1Override[state.entity.slot()] =
                spikedBeetleUpdate.options1();
            state.runtime.enemyHitboxFlags[state.entity.slot()] = spikedBeetleUpdate.hitboxFlags();
            state.preserveSpikedBeetlePresentation = true;
          }
          return true;
        });
  }
}
