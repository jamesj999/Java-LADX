package linksawakening.world;



/** ROM handler stages for SwordBeam. */
final class SwordBeamHandler {
  static void install(EntityHandlerRegistry registry) {
    registry.registerTick(
        RoomEntityRuntime.ENTITY_SWORD_BEAM,
        EntityTickStage.PHASE_1_SWORD_BEAM,
        state -> {
          if (state.status == EntityStatus.ACTIVE) {
            state.runtime.projectiles.advanceSwordBeamEntity(
                state.index,
                state.entity,
                state.frame,
                state.linkEntityX,
                state.linkEntityY,
                state.romLinkDirection,
                state.handlerInteractive);
            return false;
          }
          return true;
        });
  }
}
