package linksawakening.world.entity.handler;

import static linksawakening.world.entity.EntityRomConstants.ENTITY_PAIRODD_PROJECTILE;

import linksawakening.vfx.TransientVfxType;
import linksawakening.world.entity.EnemyProjectileCollision;
import linksawakening.world.entity.EntityFrameExecution;
import linksawakening.world.entity.EntityHandlerRegistration;
import linksawakening.world.entity.EntityStatus;
import linksawakening.world.entity.EntityTickStage;
import linksawakening.world.entity.event.EntityProjectileEvent;
import linksawakening.world.entity.event.TransientVfxRequest;

/** ROM handler stages for PairoddProjectile. */
final class PairoddProjectileHandler {
    static void install(EntityHandlerRegistration registry) {
        registry.registerTick(
                ENTITY_PAIRODD_PROJECTILE,
                EntityTickStage.PHASE_3_PAIRODD_PROJECTILE,
                PairoddProjectileHandler::phase3PairoddProjectile);
    }

    private static boolean phase3PairoddProjectile(EntityFrameExecution state) {
        if (state.status == EntityStatus.ACTIVE && !state.wasInitializing) {
            state.updated = state.pairoddProjectileMotion().advance(state.entity, state.frame);
            boolean objectCollisionHit = state.objectCollision != null && state.objectCollision.collides(state.updated);
            boolean removePairoddProjectile = false;
            boolean swordPokeVfxQueued = false;
            var collisionEvent = EnemyProjectileCollision.check(
                    state.updated,
                    state.pairoddProjectileMotion().direction(state.entity.slot()),
                    state.projectileLinkState);
            if (collisionEvent.isPresent()) {
                EntityProjectileEvent event = collisionEvent.orElseThrow();
                state.projectileEvents.add(event);
                if (event.swordPokeVfx()) {
                    state.emit(new TransientVfxRequest(
                            TransientVfxType.SWORD_POKE, event.swordPokeX(), event.swordPokeY()));
                    swordPokeVfxQueued = true;
                }
                if (event.remove()) {
                    removePairoddProjectile = true;
                }
            }
            var swordCollisionEvent = EnemyProjectileCollision.checkSwordCollision(
                    state.updated,
                    state.swordCollisionActive,
                    state.swordX,
                    state.swordWidth,
                    state.swordY,
                    state.swordHeight);
            if (swordCollisionEvent.isPresent()) {
                EntityProjectileEvent event = swordCollisionEvent.orElseThrow();
                state.projectileEvents.add(event);
                if (event.swordPokeVfx() && !swordPokeVfxQueued) {
                    state.emit(new TransientVfxRequest(
                            TransientVfxType.SWORD_POKE, event.swordPokeX(), event.swordPokeY()));
                    swordPokeVfxQueued = true;
                }
                removePairoddProjectile = true;
            }
            if (objectCollisionHit) {
                if (!swordPokeVfxQueued) {
                    state.emit(new TransientVfxRequest(
                            TransientVfxType.SWORD_POKE,
                            state.updated.x(),
                            (state.updated.y() - state.updated.z()) & 0xFF));
                }
                removePairoddProjectile = true;
            }
            if (removePairoddProjectile) {
                state.lifecycle().disableEntityWithoutPersistence(state.entity.slot());
                return false;
            }
        }
        return true;
    }
}
