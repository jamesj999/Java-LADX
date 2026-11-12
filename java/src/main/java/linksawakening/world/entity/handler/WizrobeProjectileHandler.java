package linksawakening.world.entity.handler;

import static linksawakening.world.entity.EntityRomConstants.ENTITY_WIZROBE_PROJECTILE;
import static linksawakening.world.entity.EntitySnapshots.withFlipAttribute;

import linksawakening.world.entity.EnemyProjectileCollision;
import linksawakening.world.entity.EntityFrameExecution;
import linksawakening.world.entity.EntityHandlerRegistration;
import linksawakening.world.entity.EntityStatus;
import linksawakening.world.entity.EntityTickStage;
import linksawakening.world.entity.event.EntityProjectileEvent;
import linksawakening.world.entity.motion.*;

/** ROM handler stages for WizrobeProjectile. */
final class WizrobeProjectileHandler {
    static void install(EntityHandlerRegistration registry) {
        registry.registerTick(
                ENTITY_WIZROBE_PROJECTILE,
                EntityTickStage.PHASE_2_AFTER_INITIALIZE,
                7,
                WizrobeProjectileHandler::phase2WizrobeProjectile);
        registry.registerTick(
                ENTITY_WIZROBE_PROJECTILE,
                EntityTickStage.PHASE_3_BEFORE_LASER,
                15,
                WizrobeProjectileHandler::phase3WizrobeProjectile);
    }

    private static boolean phase2WizrobeProjectile(EntityFrameExecution state) {
        var slotState = state.slotFrame();
        var frameInput = state.inputs();
        var presentationFlags = state.presentation();

        state.motion(WizrobeProjectileMotion.class, WizrobeProjectileMotion::new)
                .initialize(slotState.entity().slot(), frameInput.randomByteSupplier());
        presentationFlags.preserveWizrobeProjectilePresentation(true);

        return true;
    }

    private static boolean phase3WizrobeProjectile(EntityFrameExecution state) {
        var slotState = state.slotFrame();
        var frameInput = state.inputs();
        var presentationFlags = state.presentation();

        if (slotState.status() == EntityStatus.ACTIVE && !slotState.wasInitializing()) {
            var collisionEvent = EnemyProjectileCollision.check(
                    slotState.entity(),
                    state.motion(WizrobeProjectileMotion.class, WizrobeProjectileMotion::new)
                            .direction(slotState.entity().slot()),
                    frameInput.projectileLinkState());
            if (collisionEvent.isPresent()) {
                frameInput.projectileEvents().add(collisionEvent.orElseThrow());
            }
            slotState.updated(state.motion(WizrobeProjectileMotion.class, WizrobeProjectileMotion::new)
                    .advance(slotState.entity()));
            slotState.updated(withFlipAttribute(
                    slotState.updated(),
                    state.motion(WizrobeProjectileMotion.class, WizrobeProjectileMotion::new)
                            .flipAttribute(frameInput.frame())));
            presentationFlags.preserveWizrobeProjectilePresentation(true);
            boolean objectCollisionHit = frameInput.objectCollision() != null
                    && frameInput.objectCollision().collides(slotState.updated());
            if (objectCollisionHit
                    || collisionEvent.map(EntityProjectileEvent::remove).orElse(false)) {
                state.lifecycle()
                        .disableEntityWithoutPersistence(slotState.entity().slot());
                return false;
            }
        }
        return true;
    }
}
