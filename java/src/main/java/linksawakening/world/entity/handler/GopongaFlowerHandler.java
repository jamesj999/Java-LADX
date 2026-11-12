package linksawakening.world.entity.handler;

import static linksawakening.world.entity.EntityRomConstants.ENTITY_GOPONGA_FLOWER;
import static linksawakening.world.entity.EntitySnapshots.withVariant;

import linksawakening.world.entity.EntityFrameExecution;
import linksawakening.world.entity.EntityHandlerRegistration;
import linksawakening.world.entity.EntityLinkCollisionRules;
import linksawakening.world.entity.EntityStatus;
import linksawakening.world.entity.EntityTickStage;
import linksawakening.world.entity.motion.GopongaFlowerMotion;

/** ROM handler stages for GopongaFlower. */
final class GopongaFlowerHandler {
    static void install(EntityHandlerRegistration registry) {
        registry.registerTick(
                ENTITY_GOPONGA_FLOWER,
                EntityTickStage.PHASE_4_BEFORE_FOLLOWER,
                6,
                GopongaFlowerHandler::phase4GopongaFlower);
    }

    private static boolean phase4GopongaFlower(EntityFrameExecution state) {
        var slotState = state.slotFrame();
        var frameInput = state.inputs();
        var timerGates = state.gates();
        var presentationFlags = state.presentation();

        if (slotState.status() == EntityStatus.ACTIVE && !slotState.wasInitializing()) {
            if (GopongaFlowerMotion.overlapsInteractiveLink(
                    slotState.entity(),
                    frameInput.linkEntityX(),
                    frameInput.linkEntityY(),
                    timerGates.handlerLinkCollisionEnabled())) {
                state.background().requestLinkPush(slotState.entity(), EntityLinkCollisionRules.STANDARD_PUSH);
            }
            slotState.updated(withVariant(slotState.updated(), GopongaFlowerMotion.frameVariant(frameInput.frame())));
            presentationFlags.preserveGopongaFlowerPresentation(true);
        }
        return true;
    }
}
