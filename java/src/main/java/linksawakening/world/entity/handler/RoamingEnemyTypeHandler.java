package linksawakening.world.entity.handler;

import static linksawakening.world.entity.EntityRomConstants.ENTITY_IRON_MASK;
import static linksawakening.world.entity.EntityTypeRules.isRoamingEnemyType;

import linksawakening.world.entity.EntityHandlerRegistration;
import linksawakening.world.entity.EntityStatus;
import linksawakening.world.entity.EntityTickStage;
import linksawakening.world.entity.motion.RoamingEnemyMotion;
import linksawakening.world.entity.motion.UnmaskedIronMaskMotion;

/** ROM handler stages for RoamingEnemyType. */
final class RoamingEnemyTypeHandler {
    static void install(EntityHandlerRegistration registry) {
        for (int type = 0; type < 256; type++) {
            if ((isRoamingEnemyType(type))) {
                registry.registerTick(type, EntityTickStage.PHASE_3_ROAMING_ENEMY_TYPE, state -> {
                    if (state.status == EntityStatus.ACTIVE && !state.wasInitializing) {
                        if (state.entity.type() == ENTITY_IRON_MASK
                                && state.slot().ironMaskPrivateState2(state.entity.slot()) != 0) {
                            UnmaskedIronMaskMotion.Update unmaskedUpdate = state.unmaskedIronMaskMotion()
                                    .advance(
                                            state.entity,
                                            state.frame,
                                            state.randomByteSupplier,
                                            state.background().interaction(),
                                            state.backgroundCollision,
                                            state.slot().ignoreHits(state.entity.slot()),
                                            state.frame);
                            state.updated = unmaskedUpdate.entity();
                            state.slot()
                                    .setTransitionCountdown(state.entity.slot(), unmaskedUpdate.transitionCountdown());
                        } else {
                            RoamingEnemyMotion.Update roamingUpdate =
                                    state.background().interaction() == null
                                            ? state.roamingEnemyMotion()
                                                    .advance(
                                                            state.entity,
                                                            state.linkEntityX,
                                                            state.linkEntityY,
                                                            state.randomByteSupplier,
                                                            state.backgroundCollision,
                                                            state.creditsGameplay)
                                            : state.roamingEnemyMotion()
                                                    .advanceWithInteraction(
                                                            state.entity,
                                                            state.linkEntityX,
                                                            state.linkEntityY,
                                                            state.randomByteSupplier,
                                                            state.background().interaction(),
                                                            state.slot().ignoreHits(state.entity.slot()),
                                                            state.frame,
                                                            state.creditsGameplay);
                            state.updated = roamingUpdate.entity();
                            if (roamingUpdate.launchRequest() != null) {
                                state.emit(roamingUpdate.launchRequest());
                                state.enemySpawns().spawnEnemyProjectile(state.entity, roamingUpdate.launchRequest());
                            }
                        }
                    }
                    return true;
                });
            }
        }
    }
}
