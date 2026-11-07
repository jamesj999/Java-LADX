package linksawakening.world.entity.handler;

import linksawakening.world.EntityTickStage;
import linksawakening.world.RoomEntityRuntime;
import linksawakening.world.EntityStatus;
import linksawakening.world.entity.EntityHandlerRegistration;
import linksawakening.world.EntityLinkCollisionRules;







import linksawakening.world.entity.motion.GiantGopongaMotion;



/** ROM handler stages for GiantGopongaFlower. */
final class GiantGopongaFlowerHandler {
  static void install(EntityHandlerRegistration registry) {
    registry.registerTick(
        RoomEntityRuntime.ENTITY_GIANT_GOPONGA_FLOWER,
        EntityTickStage.PHASE_4_GIANT_GOPONGA_FLOWER,
        state -> {
          if (state.status == EntityStatus.ACTIVE && !state.wasInitializing) {
            if (GiantGopongaMotion.overlapsInteractiveLink(
                state.entity,
                state.linkEntityX,
                state.linkEntityY,
                state.handlerLinkCollisionEnabled)) {
              state.runtime.backgroundMechanics.requestLinkPush(
                  state.entity, EntityLinkCollisionRules.STANDARD_PUSH);
            }
            GiantGopongaMotion.Update giantUpdate =
                state.runtime.giantGopongaMotion.advance(
                    state.entity,
                    state.frame,
                    state.runtime.enemyTransitionCountdown[state.entity.slot()],
                    state.linkEntityX,
                    state.linkEntityY);
            state.updated = giantUpdate.entity();
            state.runtime.enemyTransitionCountdown[state.entity.slot()] =
                giantUpdate.transitionCountdown();
            if (giantUpdate.projectileSpawn() != null) {
              state.enemySpawns().spawnGopongaProjectile(
                  state.entity, giantUpdate.projectileSpawn());
            }
            state.preserveGiantGopongaPresentation = true;
          }
          return true;
        });
  }
}
