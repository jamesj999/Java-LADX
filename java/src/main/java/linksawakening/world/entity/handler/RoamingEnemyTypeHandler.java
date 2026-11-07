package linksawakening.world.entity.handler;

import linksawakening.world.entity.EntityTickStage;
import linksawakening.world.entity.EntityStatus;
import linksawakening.world.entity.EntityHandlerRegistration;

import linksawakening.world.entity.motion.RoamingEnemyMotion;
import linksawakening.world.entity.motion.UnmaskedIronMaskMotion;

import static linksawakening.world.entity.EntityTypeRules.isRoamingEnemyType;

/** ROM handler stages for RoamingEnemyType. */
final class RoamingEnemyTypeHandler {
  static void install(EntityHandlerRegistration registry) {
    for (int type = 0; type < 256; type++) {
      if ((isRoamingEnemyType(type))) {
        registry.registerTick(
            type,
            EntityTickStage.PHASE_3_ROAMING_ENEMY_TYPE,
            state -> {
              if (state.status == EntityStatus.ACTIVE && !state.wasInitializing) {
                if (state.entity.type() == state.runtime.ENTITY_IRON_MASK
                    && state.runtime.ironMaskPrivateState2[state.entity.slot()] != 0) {
                  UnmaskedIronMaskMotion.Update unmaskedUpdate =
                      state.runtime.unmaskedIronMaskMotion.advance(
                          state.entity,
                          state.frame,
                          state.randomByteSupplier,
                          state.runtime.backgroundInteraction,
                          state.backgroundCollision,
                          state.slot().ignoreHits(state.entity.slot()),
                          state.frame);
                  state.updated = unmaskedUpdate.entity();
                  state.slot().setTransitionCountdown(state.entity.slot(), unmaskedUpdate.transitionCountdown());
                } else {
                  RoamingEnemyMotion.Update roamingUpdate =
                      state.runtime.backgroundInteraction == null
                          ? state.runtime.roamingEnemyMotion.advance(
                              state.entity,
                              state.linkEntityX,
                              state.linkEntityY,
                              state.randomByteSupplier,
                              state.backgroundCollision,
                              state.creditsGameplay)
                          : state.runtime.roamingEnemyMotion.advanceWithInteraction(
                              state.entity,
                              state.linkEntityX,
                              state.linkEntityY,
                              state.randomByteSupplier,
                              state.runtime.backgroundInteraction,
                              state.slot().ignoreHits(state.entity.slot()),
                              state.frame,
                              state.creditsGameplay);
                  state.updated = roamingUpdate.entity();
                  if (roamingUpdate.launchRequest() != null) {
                    state.runtime.events.projectileLaunchRequests.add(roamingUpdate.launchRequest());
                    state.enemySpawns().spawnEnemyProjectile(
                        state.entity, roamingUpdate.launchRequest());
                  }
                }
              }
              return true;
            });
      }
    }
  }
}
