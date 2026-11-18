package linksawakening.world.entity.handler;

import static linksawakening.world.entity.EntityRomConstants.ENTITY_HOOKSHOT_BRIDGE;

import linksawakening.world.entity.EntityBackgroundMechanics;
import linksawakening.world.entity.EntityHandlerRegistry;
import linksawakening.world.entity.EntitySlotExecution;
import linksawakening.world.entity.EntityStatus;
import linksawakening.world.entity.EntityTickStage;

/** ROM handler stages for HookshotBridge. */
final class HookshotBridgeHandler {
    static void install(EntityHandlerRegistry registry) {
        registry.registerTick(
                ENTITY_HOOKSHOT_BRIDGE,
                EntityTickStage.PHASE_1_BEFORE_PICKUP_SEQUENCE,
                3,
                frame -> phase1HookshotBridge(frame.slotFrame(), frame.background()));
    }

    private static boolean phase1HookshotBridge(EntitySlotExecution slotState, EntityBackgroundMechanics background) {
        if (slotState.status() == EntityStatus.ACTIVE) {
            background.advanceHookshotBridgeEntity(slotState.index(), slotState.entity());
            return false;
        }
        return true;
    }
}
