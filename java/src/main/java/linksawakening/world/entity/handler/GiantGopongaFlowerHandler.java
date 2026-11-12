package linksawakening.world.entity.handler;

import static linksawakening.world.entity.EntityRomConstants.ENTITY_GIANT_GOPONGA_FLOWER;

import linksawakening.world.entity.EntityHandlerRegistration;
import linksawakening.world.entity.EntityLinkCollisionRules;
import linksawakening.world.entity.EntityStatus;
import linksawakening.world.entity.EntityTickStage;
import linksawakening.world.entity.motion.GiantGopongaMotion;

/** ROM handler stages for GiantGopongaFlower. */
final class GiantGopongaFlowerHandler {
    static void install(EntityHandlerRegistration registry) {
        registry.registerTick(ENTITY_GIANT_GOPONGA_FLOWER, EntityTickStage.PHASE_4_GIANT_GOPONGA_FLOWER, state -> {
            if (state.status == EntityStatus.ACTIVE && !state.wasInitializing) {
                if (GiantGopongaMotion.overlapsInteractiveLink(
                        state.entity, state.linkEntityX, state.linkEntityY, state.handlerLinkCollisionEnabled)) {
                    state.background().requestLinkPush(state.entity, EntityLinkCollisionRules.STANDARD_PUSH);
                }
                GiantGopongaMotion.Update giantUpdate = state.giantGopongaMotion()
                        .advance(
                                state.entity,
                                state.frame,
                                state.slot().transitionCountdown(state.entity.slot()),
                                state.linkEntityX,
                                state.linkEntityY);
                state.updated = giantUpdate.entity();
                state.slot().setTransitionCountdown(state.entity.slot(), giantUpdate.transitionCountdown());
                if (giantUpdate.projectileSpawn() != null) {
                    state.enemySpawns().spawnGopongaProjectile(state.entity, giantUpdate.projectileSpawn());
                }
                state.preserveGiantGopongaPresentation = true;
            }
            return true;
        });
    }
}
