package linksawakening.world;



/** ROM handler stages for SpikeTrap. */
final class SpikeTrapHandler {
  static void installPhase3(EntityHandlerRegistry registry) {
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
              state.runtime.pendingEntityEvents.add(
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
