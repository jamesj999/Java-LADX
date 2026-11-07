package linksawakening.world.entity.handler;

import linksawakening.world.entity.EntityTickStage;
import linksawakening.world.entity.RoomEntityRuntime;
import linksawakening.world.entity.EntityStatus;
import linksawakening.world.entity.EntityHandlerRegistration;

import linksawakening.world.entity.motion.GopongaProjectileMotion;

/** ROM handler stages for GopongaFlowerProjectile. */
final class GopongaFlowerProjectileHandler {
  static void install(EntityHandlerRegistration registry) {
    registry.registerTick(
        RoomEntityRuntime.ENTITY_GOPONGA_FLOWER_PROJECTILE,
        EntityTickStage.PHASE_3_GOPONGA_FLOWER_PROJECTILE,
        state -> {
          if (state.status == EntityStatus.ACTIVE && !state.wasInitializing) {
            GopongaProjectileMotion.Update projectileUpdate =
                state.runtime.gopongaProjectileMotion.advance(
                    state.entity,
                    state.frame,
                    state.slot().transitionCountdown(state.entity.slot()),
                    state.slot().ignoreHits(state.entity.slot()),
                    state.handlerLinkCollisionEnabled);
            state.runtime.enemyHealth[state.entity.slot()] =
                GopongaProjectileMotion.HEALTH_OVERRIDE;
            state.slot().setTransitionCountdown(state.entity.slot(), projectileUpdate.transitionCountdown());
            state.slot().setIgnoreHits(state.entity.slot(), projectileUpdate.ignoreHitsCountdown());
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
