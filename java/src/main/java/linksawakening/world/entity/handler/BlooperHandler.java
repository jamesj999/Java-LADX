package linksawakening.world.entity.handler;

import static linksawakening.world.entity.EntityRomConstants.ENTITY_BLOOPER;

import java.util.Objects;
import linksawakening.world.entity.EntityHandlerRegistration;
import linksawakening.world.entity.EntityStatus;
import linksawakening.world.entity.EntityTickStage;
import linksawakening.world.entity.RoomEntityBackgroundInteraction;
import linksawakening.world.entity.RoomEntityGroundInteraction;
import linksawakening.world.entity.motion.BlooperMotion;

/** ROM handler stages for Blooper. */
final class BlooperHandler {
    static void install(EntityHandlerRegistration registry) {
        registry.registerTick(ENTITY_BLOOPER, EntityTickStage.PHASE_3_BLOOPER, state -> {
            if (state.status == EntityStatus.ACTIVE && !state.wasInitializing && state.handlerLinkCollisionEnabled) {

                RoomEntityBackgroundInteraction blooperBackgroundInteraction =
                        state.background().interaction();
                if (blooperBackgroundInteraction == null && state.backgroundCollision != null) {
                    blooperBackgroundInteraction =
                            RoomEntityBackgroundInteraction.fromBoolean(state.backgroundCollision);
                }
                BlooperMotion.Movement movement = state.blooperMotion()
                        .move(
                                state.entity,
                                state.frame,
                                blooperBackgroundInteraction,
                                state.slot().ignoreHits(state.entity.slot()));
                RoomEntityGroundInteraction.Result groundResult = Objects.requireNonNull(
                        state.background()
                                .groundInteraction()
                                .apply(
                                        movement.entity(),
                                        state.frame,
                                        state.background().entityGroundStatus(state.entity.slot()),
                                        state.background().verticalSpeedZ(movement.entity()),
                                        state.background().groundInteractionSideScrolling()),
                        "Room entity ground interaction returned null");
                BlooperMotion.Update blooperUpdate = state.blooperMotion()
                        .finish(movement, groundResult, state.frame, state.linkEntityX, state.linkEntityY);
                state.updated = blooperUpdate.entity();
                state.preAppliedGroundResult = blooperUpdate.groundResult();
                state.preserveBlooperPresentation = true;
            }
            return true;
        });
    }
}
