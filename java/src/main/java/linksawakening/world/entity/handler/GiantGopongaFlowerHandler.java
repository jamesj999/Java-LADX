package linksawakening.world.entity.handler;

import static linksawakening.world.entity.EntityRomConstants.ENTITY_GIANT_GOPONGA_FLOWER;

import linksawakening.world.entity.EntityFrameExecution;
import linksawakening.world.entity.EntityHandlerRegistration;
import linksawakening.world.entity.EntityLinkCollisionRules;
import linksawakening.world.entity.EntityStatus;
import linksawakening.world.entity.EntityTickStage;
import linksawakening.world.entity.motion.*;
import linksawakening.world.entity.motion.GiantGopongaMotion;

/** ROM handler stages for GiantGopongaFlower. */
final class GiantGopongaFlowerHandler {
    static void install(EntityHandlerRegistration registry) {
        registry.registerTick(
                ENTITY_GIANT_GOPONGA_FLOWER,
                EntityTickStage.PHASE_4_BEFORE_FOLLOWER,
                7,
                GiantGopongaFlowerHandler::phase4GiantGopongaFlower);
    }

    private static boolean phase4GiantGopongaFlower(EntityFrameExecution state) {
        var slotState = state.slotFrame();
        var frameInput = state.inputs();
        var timerGates = state.gates();
        var presentationFlags = state.presentation();

        if (slotState.status() == EntityStatus.ACTIVE && !slotState.wasInitializing()) {
            if (GiantGopongaMotion.overlapsInteractiveLink(
                    slotState.entity(),
                    frameInput.linkEntityX(),
                    frameInput.linkEntityY(),
                    timerGates.handlerLinkCollisionEnabled())) {
                state.background().requestLinkPush(slotState.entity(), EntityLinkCollisionRules.STANDARD_PUSH);
            }
            GiantGopongaMotion.Update giantUpdate = state.motion(GiantGopongaMotion.class, GiantGopongaMotion::new)
                    .advance(
                            slotState.entity(),
                            frameInput.frame(),
                            state.slot().transitionCountdown(slotState.entity().slot()),
                            frameInput.linkEntityX(),
                            frameInput.linkEntityY());
            slotState.updated(giantUpdate.entity());
            state.slot().setTransitionCountdown(slotState.entity().slot(), giantUpdate.transitionCountdown());
            if (giantUpdate.projectileSpawn() != null) {
                state.enemySpawns().spawnGopongaProjectile(slotState.entity(), giantUpdate.projectileSpawn());
            }
            presentationFlags.preserveGiantGopongaPresentation(true);
        }
        return true;
    }
}
