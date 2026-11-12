package linksawakening.world.entity.handler;

import static linksawakening.world.entity.EntityRomConstants.ENTITY_LEEVER;
import static linksawakening.world.entity.EntitySnapshots.withVariant;

import linksawakening.world.entity.EntityFrameExecution;
import linksawakening.world.entity.EntityHandlerRegistration;
import linksawakening.world.entity.EntityTickStage;

/** ROM handler stages for Leever. */
final class LeeverHandler {
    static void install(EntityHandlerRegistration registry) {
        registry.registerTick(
                ENTITY_LEEVER, EntityTickStage.PHASE_2_AFTER_RECOIL_APPLICATION, 5, LeeverHandler::phase2Leever);
    }

    private static boolean phase2Leever(EntityFrameExecution state) {
        var slotState = state.slotFrame();

        if (slotState.wasInitializing()) {
            // EntityInitLeever calls SetEntitySpriteVariant($FF) before
            // the first active handler dispatch.
            slotState.updated(withVariant(slotState.entity(), -1));
        }
        return true;
    }
}
