package linksawakening.world.entity.handler;

import static linksawakening.world.entity.EntityRomConstants.ENTITY_GOPONGA_FLOWER;
import static linksawakening.world.entity.EntitySnapshots.withVariant;

import linksawakening.world.entity.EntityFrameExecution;
import linksawakening.world.entity.EntityHandlerRegistration;
import linksawakening.world.entity.EntityLinkCollisionRules;
import linksawakening.world.entity.EntityStatus;
import linksawakening.world.entity.EntityTickStage;
import linksawakening.world.entity.motion.GopongaFlowerMotion;

/** ROM handler stages for GopongaFlower. */
final class GopongaFlowerHandler {
    static void install(EntityHandlerRegistration registry) {
        registry.registerTick(
                ENTITY_GOPONGA_FLOWER,
                EntityTickStage.PHASE_4_BEFORE_FOLLOWER,
                6,
                GopongaFlowerHandler::phase4GopongaFlower);
    }

    private static boolean phase4GopongaFlower(EntityFrameExecution state) {
        if (state.status == EntityStatus.ACTIVE && !state.wasInitializing) {
            if (GopongaFlowerMotion.overlapsInteractiveLink(
                    state.entity, state.linkEntityX, state.linkEntityY, state.handlerLinkCollisionEnabled)) {
                state.background().requestLinkPush(state.entity, EntityLinkCollisionRules.STANDARD_PUSH);
            }
            state.updated = withVariant(state.updated, GopongaFlowerMotion.frameVariant(state.frame));
            state.preserveGopongaFlowerPresentation = true;
        }
        return true;
    }
}
