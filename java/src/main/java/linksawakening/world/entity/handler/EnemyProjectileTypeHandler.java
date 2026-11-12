package linksawakening.world.entity.handler;

import static linksawakening.world.entity.EntityRomConstants.*;

import linksawakening.world.entity.EnemyProjectileCollision;
import linksawakening.world.entity.EntityFrameExecution;
import linksawakening.world.entity.EntityHandlerRegistration;
import linksawakening.world.entity.EntityStatus;
import linksawakening.world.entity.EntityTickStage;
import linksawakening.world.entity.event.EntityCombatEvent;
import linksawakening.world.entity.event.EntityProjectileEvent;
import linksawakening.world.entity.motion.EnemyProjectileMotion;

/** ROM handler stages for EnemyProjectileType. */
final class EnemyProjectileTypeHandler {
    static void install(EntityHandlerRegistration registry) {
        registry.registerTick(
                ENTITY_OCTOROK_ROCK,
                EntityTickStage.PHASE_3_ENEMY_PROJECTILE_TYPE,
                EnemyProjectileTypeHandler::phase3EnemyProjectileType);
        registry.registerTick(
                ENTITY_MOBLIN_ARROW,
                EntityTickStage.PHASE_3_ENEMY_PROJECTILE_TYPE,
                EnemyProjectileTypeHandler::phase3EnemyProjectileType);
    }

    private static boolean phase3EnemyProjectileType(EntityFrameExecution state) {
        if (state.status == EntityStatus.ACTIVE && !state.wasInitializing) {
            boolean preexistingCollision = state.enemyProjectileMotion().pendingLinkCollision(state.entity.slot());
            boolean hasLinkCollision = state.enemyProjectileMotion().transitionCountdown(state.entity.slot()) == 0;
            if (hasLinkCollision) {
                var collisionEvent = EnemyProjectileCollision.check(
                        state.entity,
                        state.enemyProjectileMotion().direction(state.entity.slot()),
                        state.projectileLinkState);
                if (collisionEvent.isPresent()) {
                    EntityProjectileEvent event = collisionEvent.orElseThrow();
                    state.projectileEvents.add(event);
                    if (event.remove()) {
                        state.lifecycle().disableEntityWithoutPersistence(state.entity.slot());
                        return false;
                    }
                    preexistingCollision |= event.startsWallTransition();
                }
            }
            EnemyProjectileMotion.Update projectileUpdate = state.enemyProjectileMotion()
                    .advance(
                            state.entity,
                            state.backgroundCollision,
                            preexistingCollision,
                            state.frame,
                            state.background().entityGroundStatus(state.entity.slot()),
                            state.background().groundInteractionSideScrolling(),
                            state.handlerInteractive,
                            state.projectileTimersInteractive);
            if (projectileUpdate.unloaded()) {
                state.lifecycle().disableEntityWithoutPersistence(state.entity.slot());
                return false;
            }
            if (projectileUpdate.collidedWithWall()) {
                // $6B0C skips the poke jingle when Link collision supplied
                // $FF (including shield blocks, which already play a ting).
                if (!preexistingCollision) {
                    state.emit(new EntityCombatEvent(
                            state.entity.slot(),
                            state.entity.type(),
                            0,
                            false,
                            EntityCombatEvent.SoundChannel.JINGLE,
                            0x07));
                }
                state.projectiles().setSwordMoblinAlertingSoundCounter(0x04);
            }
            state.updated = projectileUpdate.entity();
        }
        return true;
    }
}
