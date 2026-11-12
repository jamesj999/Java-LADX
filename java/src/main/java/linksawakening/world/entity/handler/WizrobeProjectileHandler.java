package linksawakening.world.entity.handler;

import static linksawakening.world.entity.EntityRomConstants.ENTITY_WIZROBE_PROJECTILE;
import static linksawakening.world.entity.EntitySnapshots.withFlipAttribute;

import linksawakening.world.entity.EnemyProjectileCollision;
import linksawakening.world.entity.EntityFrameExecution;
import linksawakening.world.entity.EntityHandlerRegistration;
import linksawakening.world.entity.EntityStatus;
import linksawakening.world.entity.EntityTickStage;
import linksawakening.world.entity.event.EntityProjectileEvent;

/** ROM handler stages for WizrobeProjectile. */
final class WizrobeProjectileHandler {
    static void install(EntityHandlerRegistration registry) {
        registry.registerTick(
                ENTITY_WIZROBE_PROJECTILE,
                EntityTickStage.PHASE_2_WIZROBE_PROJECTILE,
                WizrobeProjectileHandler::phase2WizrobeProjectile);
        registry.registerTick(
                ENTITY_WIZROBE_PROJECTILE,
                EntityTickStage.PHASE_3_WIZROBE_PROJECTILE,
                WizrobeProjectileHandler::phase3WizrobeProjectile);
    }

    private static boolean phase2WizrobeProjectile(EntityFrameExecution state) {
        state.wizrobeProjectileMotion().initialize(state.entity.slot(), state.randomByteSupplier);
        state.preserveWizrobeProjectilePresentation = true;

        return true;
    }

    private static boolean phase3WizrobeProjectile(EntityFrameExecution state) {
        if (state.status == EntityStatus.ACTIVE && !state.wasInitializing) {
            var collisionEvent = EnemyProjectileCollision.check(
                    state.entity,
                    state.wizrobeProjectileMotion().direction(state.entity.slot()),
                    state.projectileLinkState);
            if (collisionEvent.isPresent()) {
                state.projectileEvents.add(collisionEvent.orElseThrow());
            }
            state.updated = state.wizrobeProjectileMotion().advance(state.entity);
            state.updated = withFlipAttribute(
                    state.updated, state.wizrobeProjectileMotion().flipAttribute(state.frame));
            state.preserveWizrobeProjectilePresentation = true;
            boolean objectCollisionHit = state.objectCollision != null && state.objectCollision.collides(state.updated);
            if (objectCollisionHit
                    || collisionEvent.map(EntityProjectileEvent::remove).orElse(false)) {
                state.lifecycle().disableEntityWithoutPersistence(state.entity.slot());
                return false;
            }
        }
        return true;
    }
}
