package linksawakening.world.entity.handler;

import static linksawakening.world.entity.EntityRomConstants.ENTITY_SPIKED_BEETLE;

import linksawakening.world.entity.EntityFrameExecution;
import linksawakening.world.entity.EntityHandlerRegistration;
import linksawakening.world.entity.EntityStatus;
import linksawakening.world.entity.EntityTickStage;
import linksawakening.world.entity.motion.SpikedBeetleMotion;

/** ROM handler stages for SpikedBeetle. */
final class SpikedBeetleHandler {
    static void install(EntityHandlerRegistration registry) {
        registry.registerTick(
                ENTITY_SPIKED_BEETLE, EntityTickStage.PHASE_4_SPIKED_BEETLE, SpikedBeetleHandler::phase4SpikedBeetle);
    }

    private static boolean phase4SpikedBeetle(EntityFrameExecution state) {
        if (state.status == EntityStatus.ACTIVE && !state.wasInitializing) {
            SpikedBeetleMotion.Update spikedBeetleUpdate = state.spikedBeetleMotion()
                    .advance(
                            state.entity,
                            state.frame,
                            state.linkEntityX,
                            state.linkEntityY,
                            state.randomByteSupplier,
                            state.backgroundCollision,
                            state.slot().transitionCountdown(state.entity.slot()),
                            state.slot().ignoreHits(state.entity.slot()));
            state.updated = spikedBeetleUpdate.entity();
            state.slot().setTransitionCountdown(state.entity.slot(), spikedBeetleUpdate.transitionCountdown());
            state.slot().setOptions1Override(state.entity.slot(), spikedBeetleUpdate.options1());
            state.slot().setHitboxFlags(state.entity.slot(), spikedBeetleUpdate.hitboxFlags());
            state.preserveSpikedBeetlePresentation = true;
        }
        return true;
    }
}
