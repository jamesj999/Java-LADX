package linksawakening.world.entity.handler;

import static linksawakening.world.entity.EntityRomConstants.ENTITY_LIFTABLE_ROCK;

import linksawakening.world.entity.EntityFrameExecution;
import linksawakening.world.entity.EntityHandlerRegistration;
import linksawakening.world.entity.EntityStatus;
import linksawakening.world.entity.EntityTickStage;

/** ROM handler stages for LiftableRock. */
final class LiftableRockHandler {
    static void install(EntityHandlerRegistration registry) {
        registry.registerTick(
                ENTITY_LIFTABLE_ROCK,
                EntityTickStage.PHASE_1_BEFORE_PICKUP_SEQUENCE,
                13,
                LiftableRockHandler::phase1LiftableRock);
    }

    private static boolean phase1LiftableRock(EntityFrameExecution state) {
        var slotState = state.slotFrame();
        var frameInput = state.inputs();

        if (slotState.status() == EntityStatus.ACTIVE
                && state.liftThrow().liftableRockSmashActive(slotState.entity().slot())) {

            state.liftThrow().advanceLiftableRockSmash(slotState.index(), slotState.entity(), frameInput.frame());
            return false;
        }
        return true;
    }
}
