package linksawakening.world;



/** ROM handler stages for Pairodd. */
final class PairoddHandler {
  static void install(EntityHandlerRegistry registry) {
    registry.registerTick(
        RoomEntityRuntime.ENTITY_PAIRODD,
        EntityTickStage.PHASE_3_PAIRODD,
        state -> {
          if (state.status == EntityStatus.ACTIVE && !state.wasInitializing) {
            PairoddMotion.Update pairoddUpdate =
                state.runtime.pairoddMotion.advance(
                    state.entity,
                    state.frame,
                    state.linkEntityX,
                    state.linkEntityY,
                    state.randomByteSupplier,
                    state.runtime.enemyFlashCountdown[state.entity.slot()] > 0);
            state.updated = pairoddUpdate.entity();
            if (pairoddUpdate.spawnProjectile()) {
              state.enemySpawns().spawnPairoddProjectile(
                  state.entity, state.linkEntityX, state.linkEntityY);
            }
            if (pairoddUpdate.teleportJingle()) {
              state.emit(
                  new EntityCombatEvent(
                      state.entity.slot(),
                      state.entity.type(),
                      0,
                      false,
                      EntityCombatEvent.SoundChannel.JINGLE,
                      0x3C));
            }
          }
          return true;
        });
  }
}
