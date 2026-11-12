package linksawakening.world.entity.handler;

import static linksawakening.world.entity.EntityRomConstants.ENTITY_BOOMERANG;

import linksawakening.world.entity.BoomerangFrame;
import linksawakening.world.entity.EntityFrameExecution;
import linksawakening.world.entity.EntityHandlerRegistration;
import linksawakening.world.entity.EntityStatus;
import linksawakening.world.entity.EntityTickStage;

/** ROM handler stages for Boomerang. */
final class BoomerangHandler {
    static void install(EntityHandlerRegistration registry) {
        registry.registerTick(
                ENTITY_BOOMERANG, EntityTickStage.PHASE_1_BEFORE_PICKUP_SEQUENCE, 6, BoomerangHandler::phase1Boomerang);
    }

    private static boolean phase1Boomerang(EntityFrameExecution state) {
        var slotState = state.slotFrame();
        var frameInput = state.inputs();
        var timerGates = state.gates();

        if (slotState.status() == EntityStatus.ACTIVE) {
            state.projectiles()
                    .advanceBoomerangEntity(new BoomerangFrame(
                            slotState.index(),
                            slotState.entity(),
                            frameInput.frame(),
                            frameInput.linkEntityX(),
                            frameInput.linkEntityY(),
                            frameInput.linkZ(),
                            frameInput.projectileLinkState().motionState(),
                            timerGates.handlerInteractive(),
                            timerGates.projectileTimersInteractive()));
            return false;
        }
        return true;
    }
}
