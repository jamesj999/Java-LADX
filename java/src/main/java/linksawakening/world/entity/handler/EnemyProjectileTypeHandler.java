package linksawakening.world.entity.handler;

import static linksawakening.world.entity.EntityRomConstants.*;

import linksawakening.world.entity.EnemyProjectileCollision;
import linksawakening.world.entity.EntityFrameExecution;
import linksawakening.world.entity.EntityHandlerRegistration;
import linksawakening.world.entity.EntityStatus;
import linksawakening.world.entity.EntityTickStage;
import linksawakening.world.entity.event.EntityCombatEvent;
import linksawakening.world.entity.event.EntityProjectileEvent;
import linksawakening.world.entity.motion.*;
import linksawakening.world.entity.motion.EnemyProjectileMotion;

/** ROM handler stages for EnemyProjectileType. */
final class EnemyProjectileTypeHandler {
    static void install(EntityHandlerRegistration registry) {
        registry.registerTick(
                ENTITY_OCTOROK_ROCK,
                EntityTickStage.PHASE_3_AFTER_LASER,
                0,
                EnemyProjectileTypeHandler::phase3EnemyProjectileType);
        registry.registerTick(
                ENTITY_MOBLIN_ARROW,
                EntityTickStage.PHASE_3_AFTER_LASER,
                0,
                EnemyProjectileTypeHandler::phase3EnemyProjectileType);
    }

    private static boolean phase3EnemyProjectileType(EntityFrameExecution state) {
        var slotState = state.slotFrame();
        var frameInput = state.inputs();
        var timerGates = state.gates();

        if (slotState.status() == EntityStatus.ACTIVE && !slotState.wasInitializing()) {
            boolean preexistingCollision = state.motion(EnemyProjectileMotion.class, EnemyProjectileMotion::new)
                    .pendingLinkCollision(slotState.entity().slot());
            boolean hasLinkCollision = state.motion(EnemyProjectileMotion.class, EnemyProjectileMotion::new)
                            .transitionCountdown(slotState.entity().slot())
                    == 0;
            if (hasLinkCollision) {
                var collisionEvent = EnemyProjectileCollision.check(
                        slotState.entity(),
                        state.motion(EnemyProjectileMotion.class, EnemyProjectileMotion::new)
                                .direction(slotState.entity().slot()),
                        frameInput.projectileLinkState());
                if (collisionEvent.isPresent()) {
                    EntityProjectileEvent event = collisionEvent.orElseThrow();
                    frameInput.projectileEvents().add(event);
                    if (event.remove()) {
                        state.lifecycle()
                                .disableEntityWithoutPersistence(
                                        slotState.entity().slot());
                        return false;
                    }
                    preexistingCollision |= event.startsWallTransition();
                }
            }
            EnemyProjectileMotion.Update projectileUpdate = state.motion(
                            EnemyProjectileMotion.class, EnemyProjectileMotion::new)
                    .advance(
                            slotState.entity(),
                            frameInput.backgroundCollision(),
                            preexistingCollision,
                            frameInput.frame(),
                            state.background()
                                    .entityGroundStatus(slotState.entity().slot()),
                            state.background().groundInteractionSideScrolling(),
                            timerGates.handlerInteractive(),
                            timerGates.projectileTimersInteractive());
            if (projectileUpdate.unloaded()) {
                state.lifecycle()
                        .disableEntityWithoutPersistence(slotState.entity().slot());
                return false;
            }
            if (projectileUpdate.collidedWithWall()) {
                // $6B0C skips the poke jingle when Link collision supplied
                // $FF (including shield blocks, which already play a ting).
                if (!preexistingCollision) {
                    state.emit(new EntityCombatEvent(
                            slotState.entity().slot(),
                            slotState.entity().type(),
                            0,
                            false,
                            EntityCombatEvent.SoundChannel.JINGLE,
                            0x07));
                }
                state.projectiles().setSwordMoblinAlertingSoundCounter(0x04);
            }
            slotState.updated(projectileUpdate.entity());
        }
        return true;
    }
}
