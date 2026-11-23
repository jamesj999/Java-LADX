package linksawakening.world.entity.handler;

import static linksawakening.world.entity.EntityRomConstants.ENTITY_LIFTABLE_ROCK;

import linksawakening.world.entity.EntityFrameInputLatch;
import linksawakening.world.entity.EntityHandlerRegistry;
import linksawakening.world.entity.EntityLiftThrowController;
import linksawakening.world.entity.EntitySlotExecution;
import linksawakening.world.entity.EntityStatus;
import linksawakening.world.entity.EntityTickStage;

/** ROM handler stages for LiftableRock. */
final class LiftableRockHandler {
    static void install(EntityHandlerRegistry registry) {
        registry.registerTick(
                ENTITY_LIFTABLE_ROCK,
                EntityTickStage.PHASE_1_BEFORE_PICKUP_SEQUENCE,
                13,
                frame -> phase1LiftableRock(frame.slotFrame(), frame.inputs(), frame.liftThrow()));
    }

    private static boolean phase1LiftableRock(
            EntitySlotExecution slotState, EntityFrameInputLatch frameInput, EntityLiftThrowController liftThrow) {
        if (slotState.status() == EntityStatus.ACTIVE
                && liftThrow.liftableRockSmashActive(slotState.entity().slot())) {
            liftThrow.advanceLiftableRockSmash(slotState.index(), slotState.entity(), frameInput.frame());
            return false;
        }
        return true;
    }
}
