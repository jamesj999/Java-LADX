package linksawakening.world.entity.handler;

import linksawakening.world.entity.EntityTickStage;
import linksawakening.world.entity.RoomEntityRuntime;
import linksawakening.world.entity.EntityStatus;
import linksawakening.world.entity.EntityHandlerRegistration;

/** ROM handler stages for HookshotBridge. */
final class HookshotBridgeHandler {
  static void install(EntityHandlerRegistration registry) {
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
