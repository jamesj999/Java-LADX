package linksawakening.world.entity.handler;

import static linksawakening.world.entity.EntityRomConstants.ENTITY_SPIKE_TRAP;

import linksawakening.world.entity.EntityFrameExecution;
import linksawakening.world.entity.EntityHandlerRegistration;
import linksawakening.world.entity.EntityStatus;
import linksawakening.world.entity.EntityTickStage;
import linksawakening.world.entity.event.EntityCombatEvent;
import linksawakening.world.entity.motion.*;
import linksawakening.world.entity.motion.SpikeTrapMotion;

/** ROM handler stages for SpikeTrap. */
final class SpikeTrapHandler {
    static void install(EntityHandlerRegistration registry) {
        registry.registerTick(
                ENTITY_SPIKE_TRAP, EntityTickStage.PHASE_3_BEFORE_LASER, 7, SpikeTrapHandler::phase3SpikeTrap);
    }

    private static boolean phase3SpikeTrap(EntityFrameExecution state) {
        var slotState = state.slotFrame();
        var frameInput = state.inputs();

        if (slotState.status() == EntityStatus.ACTIVE && !slotState.wasInitializing()) {
            SpikeTrapMotion.Update spikeTrapUpdate = state.motion(SpikeTrapMotion.class, SpikeTrapMotion::new)
                    .advance(
                            slotState.entity(),
                            frameInput.linkEntityX(),
                            frameInput.linkEntityY(),
                            frameInput.backgroundCollision());
            slotState.updated(spikeTrapUpdate.entity());
            if (spikeTrapUpdate.soundChannel() != EntityCombatEvent.SoundChannel.NONE) {
                state.emit(new EntityCombatEvent(
                        slotState.entity().slot(),
                        slotState.entity().type(),
                        0,
                        false,
                        spikeTrapUpdate.soundChannel(),
                        spikeTrapUpdate.soundId()));
            }
        }
        return true;
    }
}
