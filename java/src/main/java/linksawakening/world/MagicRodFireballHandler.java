package linksawakening.world;



/** ROM handler stages for MagicRodFireball. */
final class MagicRodFireballHandler {
  static void install(EntityHandlerRegistry registry) {
    registry.registerTick(
        RoomEntityRuntime.ENTITY_MAGIC_ROD_FIREBALL,
        EntityTickStage.PHASE_1_MAGIC_ROD_FIREBALL,
        state -> {
          if (state.status == EntityStatus.ACTIVE) {
            state.runtime.projectiles.advanceMagicRodFireballEntity(
                state.index,
                state.entity,
                state.frame,
                state.handlerInteractive,
                state.projectileTimersInteractive);
            return false;
          }
          return true;
        });
  }
}
