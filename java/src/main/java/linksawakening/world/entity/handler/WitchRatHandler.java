package linksawakening.world.entity.handler;

import static linksawakening.world.entity.EntityRomConstants.ENTITY_WITCH_RAT;

import linksawakening.world.entity.EntityFrameExecution;
import linksawakening.world.entity.EntityHandlerRegistration;
import linksawakening.world.entity.EntityStatus;
import linksawakening.world.entity.EntityTickStage;
import linksawakening.world.entity.RoomEntityBackgroundInteraction;
import linksawakening.world.entity.motion.WitchRatMotion;

/** ROM handler stages for WitchRat. */
final class WitchRatHandler {
    static void install(EntityHandlerRegistration registry) {
        registry.registerTick(
                ENTITY_WITCH_RAT, EntityTickStage.PHASE_4_BEFORE_FOLLOWER, 18, WitchRatHandler::phase4WitchRat);
    }

    private static boolean phase4WitchRat(EntityFrameExecution state) {
        if (state.status == EntityStatus.ACTIVE && !state.wasInitializing && state.handlerLinkCollisionEnabled) {

            int slot = state.entity.slot();
            RoomEntityBackgroundInteraction witchRatBackgroundInteraction =
                    state.background().interaction();
            if (witchRatBackgroundInteraction == null && state.backgroundCollision != null) {
                witchRatBackgroundInteraction = RoomEntityBackgroundInteraction.fromBoolean(state.backgroundCollision);
            }
            WitchRatMotion.Update witchRatUpdate = state.witchRatMotion()
                    .advance(
                            state.updated,
                            state.frame,
                            state.slot().transitionCountdown(slot),
                            state.randomByteSupplier,
                            witchRatBackgroundInteraction);
            state.updated = witchRatUpdate.entity();
            state.slot().setTransitionCountdown(slot, witchRatUpdate.transitionCountdown());
            state.applyGenericGroundInteraction = witchRatUpdate.appliesBackgroundInteraction();
            state.preserveWitchRatPresentation = true;
        }
        return true;
    }
}
