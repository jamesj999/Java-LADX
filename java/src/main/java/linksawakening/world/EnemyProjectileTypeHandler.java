package linksawakening.world;

import linksawakening.world.entity.motion.EnemyProjectileMotion;

import static linksawakening.world.EntityTypeRules.isEnemyProjectileType;

/** ROM handler stages for EnemyProjectileType. */
final class EnemyProjectileTypeHandler {
  static void install(EntityHandlerRegistry registry) {
    for (int type = 0; type < 256; type++) {
      if ((isEnemyProjectileType(type))) {
        registry.registerTick(
            type,
            EntityTickStage.PHASE_3_ENEMY_PROJECTILE_TYPE,
            state -> {
              if (state.status == EntityStatus.ACTIVE && !state.wasInitializing) {
                boolean preexistingCollision =
                    state.runtime.enemyProjectileMotion.pendingLinkCollision(state.entity.slot());
                boolean hasLinkCollision =
                    state.runtime.enemyProjectileMotion.transitionCountdown(state.entity.slot())
                        == 0;
                if (hasLinkCollision) {
                  var collisionEvent =
                      EnemyProjectileCollision.check(
                          state.entity,
                          state.runtime.enemyProjectileMotion.direction(state.entity.slot()),
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
                EnemyProjectileMotion.Update projectileUpdate =
                    state.runtime.enemyProjectileMotion.advance(
                        state.entity,
                        state.backgroundCollision,
                        preexistingCollision,
                        state.frame,
                        state.runtime.entityGroundStatus[state.entity.slot()],
                        state.runtime.groundInteractionSideScrolling,
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
                    state.emit(
                        new EntityCombatEvent(
                            state.entity.slot(),
                            state.entity.type(),
                            0,
                            false,
                            EntityCombatEvent.SoundChannel.JINGLE,
                            0x07));
                  }
                  state.runtime.swordMoblinAlertingSoundCounter = 0x04;
                }
                state.updated = projectileUpdate.entity();
              }
              return true;
            });
      }
    }
  }
}
