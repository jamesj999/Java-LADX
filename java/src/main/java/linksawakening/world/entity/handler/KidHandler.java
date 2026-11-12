package linksawakening.world.entity.handler;

import static linksawakening.world.entity.EntityRomConstants.ENTITY_KID_71;
import static linksawakening.world.entity.EntityRomConstants.ENTITY_KID_72;

import linksawakening.world.entity.EntityFrameExecution;
import linksawakening.world.entity.EntityHandlerRegistration;
import linksawakening.world.entity.EntityStatus;
import linksawakening.world.entity.EntityTickStage;

/** ROM handler stages for Kid. */
final class KidHandler {
    static void install(EntityHandlerRegistration registry) {
        registry.registerTick(
                ENTITY_KID_71, EntityTickStage.PHASE_1_BEFORE_PICKUP_SEQUENCE, 19, KidHandler::phase1Kid72);
        registry.registerTick(
                ENTITY_KID_72, EntityTickStage.PHASE_1_BEFORE_PICKUP_SEQUENCE, 19, KidHandler::phase1Kid72);
    }

    private static boolean phase1Kid72(EntityFrameExecution state) {
        var slotState = state.slotFrame();
        var frameInput = state.inputs();

        if (slotState.status() == EntityStatus.ACTIVE) {
            state.story()
                    .advanceVillageKid(
                            slotState.entity(), frameInput.frame(), frameInput.linkEntityX(), frameInput.linkEntityY());
            return false;
        }
        return true;
    }
}
