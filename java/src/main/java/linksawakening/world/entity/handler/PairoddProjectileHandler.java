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
import linksawakening.world.entity.motion.*;

/** ROM handler stages for PairoddProjectile. */
final class PairoddProjectileHandler {
    static void install(EntityHandlerRegistration registry) {
        registry.registerTick(
                ENTITY_PAIRODD_PROJECTILE,
                EntityTickStage.PHASE_3_BEFORE_LASER,
                14,
                PairoddProjectileHandler::phase3PairoddProjectile);
    }

    private static boolean phase3PairoddProjectile(EntityFrameExecution state) {
        var slotState = state.slotFrame();
        var frameInput = state.inputs();

        if (slotState.status() == EntityStatus.ACTIVE && !slotState.wasInitializing()) {
            slotState.updated(state.motion(PairoddProjectileMotion.class, PairoddProjectileMotion::new)
                    .advance(slotState.entity(), frameInput.frame()));
            boolean objectCollisionHit = frameInput.objectCollision() != null
                    && frameInput.objectCollision().collides(slotState.updated());
            boolean removePairoddProjectile = false;
            boolean swordPokeVfxQueued = false;
            var collisionEvent = EnemyProjectileCollision.check(
                    slotState.updated(),
                    state.motion(PairoddProjectileMotion.class, PairoddProjectileMotion::new)
                            .direction(slotState.entity().slot()),
                    frameInput.projectileLinkState());
            if (collisionEvent.isPresent()) {
                EntityProjectileEvent event = collisionEvent.orElseThrow();
                frameInput.projectileEvents().add(event);
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
                    slotState.updated(),
                    frameInput.swordCollisionActive(),
                    frameInput.swordX(),
                    frameInput.swordWidth(),
                    frameInput.swordY(),
                    frameInput.swordHeight());
            if (swordCollisionEvent.isPresent()) {
                EntityProjectileEvent event = swordCollisionEvent.orElseThrow();
                frameInput.projectileEvents().add(event);
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
                            slotState.updated().x(),
                            (slotState.updated().y() - slotState.updated().z()) & 0xFF));
                }
                removePairoddProjectile = true;
            }
            if (removePairoddProjectile) {
                state.lifecycle()
                        .disableEntityWithoutPersistence(slotState.entity().slot());
                return false;
            }
        }
        return true;
    }
}
