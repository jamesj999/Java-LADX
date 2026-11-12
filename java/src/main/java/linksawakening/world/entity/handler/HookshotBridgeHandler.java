package linksawakening.world.entity.handler;

import static linksawakening.world.entity.EntityRomConstants.ENTITY_HOOKSHOT_BRIDGE;

import linksawakening.world.entity.EntityFrameExecution;
import linksawakening.world.entity.EntityHandlerRegistration;
import linksawakening.world.entity.EntityStatus;
import linksawakening.world.entity.EntityTickStage;

/** ROM handler stages for HookshotBridge. */
final class HookshotBridgeHandler {
    static void install(EntityHandlerRegistration registry) {
        registry.registerTick(
                ENTITY_HOOKSHOT_BRIDGE,
                EntityTickStage.PHASE_1_BEFORE_PICKUP_SEQUENCE,
                3,
                HookshotBridgeHandler::phase1HookshotBridge);
    }

    private static boolean phase1HookshotBridge(EntityFrameExecution state) {
        var slotState = state.slotFrame();

        if (slotState.status() == EntityStatus.ACTIVE) {
            state.background().advanceHookshotBridgeEntity(slotState.index(), slotState.entity());
            return false;
        }
        return true;
    }
}
