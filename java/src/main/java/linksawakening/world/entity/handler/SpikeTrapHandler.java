package linksawakening.world.entity.handler;

import linksawakening.world.entity.EntityTickStage;
import linksawakening.world.entity.RoomEntityRuntime;
import linksawakening.world.entity.EntityStatus;
import linksawakening.world.entity.EntityHandlerRegistration;
import linksawakening.world.entity.event.EntityCombatEvent;

import linksawakening.world.entity.motion.SpikeTrapMotion;

/** ROM handler stages for SpikeTrap. */
final class SpikeTrapHandler {
  static void install(EntityHandlerRegistration registry) {
    registry.registerTick(
        RoomEntityRuntime.ENTITY_SPIKE_TRAP,
        EntityTickStage.PHASE_3_SPIKE_TRAP,
        state -> {
          if (state.status == EntityStatus.ACTIVE && !state.wasInitializing) {
            SpikeTrapMotion.Update spikeTrapUpdate =
                state.runtime.spikeTrapMotion.advance(
                    state.entity, state.linkEntityX, state.linkEntityY, state.backgroundCollision);
            state.updated = spikeTrapUpdate.entity();
            if (spikeTrapUpdate.soundChannel() != EntityCombatEvent.SoundChannel.NONE) {
              state.emit(
                  new EntityCombatEvent(
                      state.entity.slot(),
                      state.entity.type(),
                      0,
                      false,
                      spikeTrapUpdate.soundChannel(),
                      spikeTrapUpdate.soundId()));
            }
          }
          return true;
        });
  }
}
