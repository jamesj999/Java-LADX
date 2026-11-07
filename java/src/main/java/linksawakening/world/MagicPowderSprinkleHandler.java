package linksawakening.world;



/** ROM handler stages for MagicPowderSprinkle. */
final class MagicPowderSprinkleHandler {
  static void installPhase1(EntityHandlerRegistry registry) {
    registry.registerTick(
        RoomEntityRuntime.ENTITY_MAGIC_POWDER_SPRINKLE,
        EntityTickStage.PHASE_1_MAGIC_POWDER_SPRINKLE,
        state -> {
          if (state.status == EntityStatus.ACTIVE) {
            state.runtime.projectiles.advanceMagicPowderSprinkleEntity(
                state.index,
                state.entity,
                state.frame,
                state.projectileLinkState.motionState(),
                state.handlerInteractive && !state.creditsGameplay,
                state.projectileTimersInteractive);
            return false;
          }
          return true;
        });
  }
}
