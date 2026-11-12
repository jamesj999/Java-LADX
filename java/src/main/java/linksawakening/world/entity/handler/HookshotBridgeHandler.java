package linksawakening.world.entity.handler;

import static linksawakening.world.entity.EntityRomConstants.ENTITY_HOOKSHOT_BRIDGE;

import linksawakening.world.entity.EntityHandlerRegistration;
import linksawakening.world.entity.EntityStatus;
import linksawakening.world.entity.EntityTickStage;

/** ROM handler stages for HookshotBridge. */
final class HookshotBridgeHandler {
    static void install(EntityHandlerRegistration registry) {
        registry.registerTick(ENTITY_HOOKSHOT_BRIDGE, EntityTickStage.PHASE_1_HOOKSHOT_BRIDGE, state -> {
            if (state.status == EntityStatus.ACTIVE) {
                state.background().advanceHookshotBridgeEntity(state.index, state.entity);
                return false;
            }
            return true;
        });
    }
}
