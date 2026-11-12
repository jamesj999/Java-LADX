package linksawakening.world.entity.handler;

import static linksawakening.world.entity.EntityRomConstants.ENTITY_STAR;

import linksawakening.world.entity.EntityHandlerRegistration;
import linksawakening.world.entity.EntityStatus;
import linksawakening.world.entity.EntityTickStage;
import linksawakening.world.entity.RoomEntityBackgroundInteraction;

/** ROM handler stages for Star. */
final class StarHandler {
    static void install(EntityHandlerRegistration registry) {
        registry.registerTick(ENTITY_STAR, EntityTickStage.PHASE_3_STAR, state -> {
            if (state.status == EntityStatus.ACTIVE && !state.wasInitializing && state.handlerLinkCollisionEnabled) {

                RoomEntityBackgroundInteraction starBackgroundInteraction =
                        state.background().interaction();
                if (starBackgroundInteraction == null && state.backgroundCollision != null) {
                    starBackgroundInteraction = RoomEntityBackgroundInteraction.fromBoolean(state.backgroundCollision);
                }
                state.updated = state.starMotion()
                        .advance(
                                state.entity,
                                state.frame,
                                state.randomByteSupplier,
                                starBackgroundInteraction,
                                state.slot().ignoreHits(state.entity.slot()));
                state.preserveStarPresentation = true;
            }
            return true;
        });
    }
}
