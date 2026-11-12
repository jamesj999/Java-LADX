package linksawakening.world.entity.handler;

import static linksawakening.world.entity.EntityRomConstants.ENTITY_STALFOS_AGGRESSIVE;

import linksawakening.world.entity.EntityHandlerRegistration;
import linksawakening.world.entity.EntityStatus;
import linksawakening.world.entity.EntityTickStage;

/** ROM handler stages for StalfosAggressive. */
final class StalfosAggressiveHandler {
    static void install(EntityHandlerRegistration registry) {
        registry.registerTick(ENTITY_STALFOS_AGGRESSIVE, EntityTickStage.PHASE_3_STALFOS_AGGRESSIVE, state -> {
            if (state.status == EntityStatus.ACTIVE && !state.wasInitializing) {
                state.updated = state.stalfosAggressiveMotion()
                        .advance(
                                state.entity,
                                state.frame,
                                state.linkEntityX,
                                state.linkEntityY,
                                state.randomByteSupplier,
                                state.backgroundCollision);
            }
            return true;
        });
    }
}
