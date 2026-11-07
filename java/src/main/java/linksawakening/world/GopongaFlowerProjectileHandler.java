package linksawakening.world;

import linksawakening.world.entity.motion.GopongaProjectileMotion;



/** ROM handler stages for GopongaFlowerProjectile. */
final class GopongaFlowerProjectileHandler {
  static void install(EntityHandlerRegistry registry) {
    registry.registerTick(
        RoomEntityRuntime.ENTITY_GOPONGA_FLOWER_PROJECTILE,
        EntityTickStage.PHASE_3_GOPONGA_FLOWER_PROJECTILE,
        state -> {
          if (state.status == EntityStatus.ACTIVE && !state.wasInitializing) {
            GopongaProjectileMotion.Update projectileUpdate =
                state.runtime.gopongaProjectileMotion.advance(
                    state.entity,
                    state.frame,
                    state.runtime.enemyTransitionCountdown[state.entity.slot()],
                    state.runtime.enemyIgnoreHitsCountdown[state.entity.slot()],
                    state.handlerLinkCollisionEnabled);
            state.runtime.enemyHealth[state.entity.slot()] =
                GopongaProjectileMotion.HEALTH_OVERRIDE;
            state.runtime.enemyTransitionCountdown[state.entity.slot()] =
                projectileUpdate.transitionCountdown();
            state.runtime.enemyIgnoreHitsCountdown[state.entity.slot()] =
                projectileUpdate.ignoreHitsCountdown();
            if (projectileUpdate.unloadRequested()) {
              state.lifecycle().disableEntityWithoutPersistence(state.entity.slot());
              return false;
            }
            state.updated = projectileUpdate.entity();
            state.preserveGopongaProjectilePresentation = true;
          }
          return true;
        });
  }
}
