package linksawakening.world.entity.handler;

import static linksawakening.world.entity.EntityRomConstants.ENTITY_GOPONGA_FLOWER;
import static linksawakening.world.entity.EntitySnapshots.withVariant;

import linksawakening.world.entity.EntityBackgroundMechanics;
import linksawakening.world.entity.EntityFrameInputLatch;
import linksawakening.world.entity.EntityHandlerRegistry;
import linksawakening.world.entity.EntityLinkCollisionRules;
import linksawakening.world.entity.EntityPresentationFlags;
import linksawakening.world.entity.EntitySlotExecution;
import linksawakening.world.entity.EntityStatus;
import linksawakening.world.entity.EntityTickStage;
import linksawakening.world.entity.EntityTimerGates;
import linksawakening.world.entity.motion.GopongaFlowerMotion;

/** ROM handler stages for GopongaFlower. */
final class GopongaFlowerHandler {
    static void install(EntityHandlerRegistry registry) {
        registry.registerTick(
                ENTITY_GOPONGA_FLOWER,
                EntityTickStage.PHASE_4_BEFORE_FOLLOWER,
                6,
                frame -> phase4GopongaFlower(
                        frame.slotFrame(), frame.inputs(), frame.gates(), frame.presentation(), frame.background()));
    }

    private static boolean phase4GopongaFlower(
            EntitySlotExecution slotState,
            EntityFrameInputLatch frameInput,
            EntityTimerGates timerGates,
            EntityPresentationFlags presentationFlags,
            EntityBackgroundMechanics background) {
        if (slotState.status() == EntityStatus.ACTIVE && !slotState.wasInitializing()) {
            if (GopongaFlowerMotion.overlapsInteractiveLink(
                    slotState.entity(),
                    frameInput.linkEntityX(),
                    frameInput.linkEntityY(),
                    timerGates.handlerLinkCollisionEnabled())) {
                background.requestLinkPush(slotState.entity(), EntityLinkCollisionRules.STANDARD_PUSH);
            }
            slotState.updated(withVariant(slotState.updated(), GopongaFlowerMotion.frameVariant(frameInput.frame())));
            presentationFlags.preserveGopongaFlowerPresentation(true);
        }
        return true;
    }
}
