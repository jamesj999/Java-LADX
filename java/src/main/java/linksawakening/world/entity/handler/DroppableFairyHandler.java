package linksawakening.world.entity.handler;

import static linksawakening.world.entity.EntityRomConstants.ENTITY_DROPPABLE_FAIRY;

import linksawakening.world.entity.EntityFrameExecution;
import linksawakening.world.entity.EntityHandlerRegistration;
import linksawakening.world.entity.EntityStatus;
import linksawakening.world.entity.EntityTickStage;
import linksawakening.world.entity.motion.FairyMotion;

/** ROM handler stages for DroppableFairy. */
final class DroppableFairyHandler {
    static void install(EntityHandlerRegistration registry) {
        registry.registerTick(
                ENTITY_DROPPABLE_FAIRY,
                EntityTickStage.PHASE_3_DROPPABLE_FAIRY,
                DroppableFairyHandler::phase3DroppableFairy);
    }

    private static boolean phase3DroppableFairy(EntityFrameExecution state) {
        if (state.status == EntityStatus.ACTIVE && !state.wasInitializing) {
            FairyMotion.Update fairyUpdate = state.fairyMotion()
                    .advance(
                            state.entity,
                            state.frame,
                            state.linkEntityX,
                            state.linkEntityY,
                            state.randomByteSupplier,
                            state.slot().transitionCountdown(state.entity.slot()));
            state.updated = fairyUpdate.entity();
            state.slot().setTransitionCountdown(state.entity.slot(), fairyUpdate.transitionCountdown());
        }
        return true;
    }
}
