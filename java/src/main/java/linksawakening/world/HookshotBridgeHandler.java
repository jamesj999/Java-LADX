package linksawakening.world;



/** ROM handler stages for HookshotBridge. */
final class HookshotBridgeHandler {
  static void installPhase1(EntityHandlerRegistry registry) {
    registry.registerTick(
        RoomEntityRuntime.ENTITY_HOOKSHOT_BRIDGE,
        EntityTickStage.PHASE_1_HOOKSHOT_BRIDGE,
        state -> {
          if (state.status == EntityStatus.ACTIVE) {
            state.runtime.backgroundMechanics.advanceHookshotBridgeEntity(
                state.index, state.entity);
            return false;
          }
          return true;
        });
  }
}
