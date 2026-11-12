package linksawakening.world.entity.handler;

import static linksawakening.world.entity.EntityRomConstants.ENTITY_SPIKED_BEETLE;

import linksawakening.world.entity.EntityFrameExecution;
import linksawakening.world.entity.EntityHandlerRegistration;
import linksawakening.world.entity.EntityStatus;
import linksawakening.world.entity.EntityTickStage;
import linksawakening.world.entity.motion.*;
import linksawakening.world.entity.motion.SpikedBeetleMotion;

/** ROM handler stages for SpikedBeetle. */
final class SpikedBeetleHandler {
    static void install(EntityHandlerRegistration registry) {
        registry.registerTick(
                ENTITY_SPIKED_BEETLE,
                EntityTickStage.PHASE_4_BEFORE_FOLLOWER,
                22,
                SpikedBeetleHandler::phase4SpikedBeetle);
    }

    private static boolean phase4SpikedBeetle(EntityFrameExecution state) {
        var slotState = state.slotFrame();
        var frameInput = state.inputs();
        var presentationFlags = state.presentation();

        if (slotState.status() == EntityStatus.ACTIVE && !slotState.wasInitializing()) {
            SpikedBeetleMotion.Update spikedBeetleUpdate = state.motion(
                            SpikedBeetleMotion.class, SpikedBeetleMotion::new)
                    .advance(
                            slotState.entity(),
                            frameInput.frame(),
                            frameInput.linkEntityX(),
                            frameInput.linkEntityY(),
                            frameInput.randomByteSupplier(),
                            frameInput.backgroundCollision(),
                            state.slot().transitionCountdown(slotState.entity().slot()),
                            state.slot().ignoreHits(slotState.entity().slot()));
            slotState.updated(spikedBeetleUpdate.entity());
            state.slot().setTransitionCountdown(slotState.entity().slot(), spikedBeetleUpdate.transitionCountdown());
            state.slot().setOptions1Override(slotState.entity().slot(), spikedBeetleUpdate.options1());
            state.slot().setHitboxFlags(slotState.entity().slot(), spikedBeetleUpdate.hitboxFlags());
            presentationFlags.preserveSpikedBeetlePresentation(true);
        }
        return true;
    }
}
