package linksawakening.world.entity.handler;

import static linksawakening.world.entity.EntityRomConstants.ENTITY_GOPONGA_FLOWER_PROJECTILE;

import linksawakening.world.entity.EntityFrameExecution;
import linksawakening.world.entity.EntityHandlerRegistration;
import linksawakening.world.entity.EntityStatus;
import linksawakening.world.entity.EntityTickStage;
import linksawakening.world.entity.motion.*;
import linksawakening.world.entity.motion.GopongaProjectileMotion;

/** ROM handler stages for GopongaFlowerProjectile. */
final class GopongaFlowerProjectileHandler {
    static void install(EntityHandlerRegistration registry) {
        registry.registerTick(
                ENTITY_GOPONGA_FLOWER_PROJECTILE,
                EntityTickStage.PHASE_3_AFTER_LASER,
                1,
                GopongaFlowerProjectileHandler::phase3GopongaFlowerProjectile);
    }

    private static boolean phase3GopongaFlowerProjectile(EntityFrameExecution state) {
        var slotState = state.slotFrame();
        var frameInput = state.inputs();
        var timerGates = state.gates();
        var presentationFlags = state.presentation();

        if (slotState.status() == EntityStatus.ACTIVE && !slotState.wasInitializing()) {
            GopongaProjectileMotion.Update projectileUpdate = state.motion(
                            GopongaProjectileMotion.class, GopongaProjectileMotion::new)
                    .advance(
                            slotState.entity(),
                            frameInput.frame(),
                            state.slot().transitionCountdown(slotState.entity().slot()),
                            state.slot().ignoreHits(slotState.entity().slot()),
                            timerGates.handlerLinkCollisionEnabled());
            state.slot().setHealth(slotState.entity().slot(), GopongaProjectileMotion.HEALTH_OVERRIDE);
            state.slot().setTransitionCountdown(slotState.entity().slot(), projectileUpdate.transitionCountdown());
            state.slot().setIgnoreHits(slotState.entity().slot(), projectileUpdate.ignoreHitsCountdown());
            if (projectileUpdate.unloadRequested()) {
                state.lifecycle()
                        .disableEntityWithoutPersistence(slotState.entity().slot());
                return false;
            }
            slotState.updated(projectileUpdate.entity());
            presentationFlags.preserveGopongaProjectilePresentation(true);
        }
        return true;
    }
}
