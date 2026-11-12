package linksawakening.world.entity.handler;

import static linksawakening.world.entity.EntityRomConstants.ENTITY_FISH;

import linksawakening.vfx.TransientVfxType;
import linksawakening.world.entity.EntityFrameExecution;
import linksawakening.world.entity.EntityHandlerRegistration;
import linksawakening.world.entity.EntityStatus;
import linksawakening.world.entity.EntityTickStage;
import linksawakening.world.entity.event.EntityCombatEvent;
import linksawakening.world.entity.event.TransientVfxRequest;
import linksawakening.world.entity.motion.*;
import linksawakening.world.entity.motion.FishMotion;

/** ROM handler stages for Fish. */
final class FishHandler {
    static void install(EntityHandlerRegistration registry) {
        registry.registerTick(ENTITY_FISH, EntityTickStage.PHASE_3_AFTER_LASER, 3, FishHandler::phase3Fish);
    }

    private static boolean phase3Fish(EntityFrameExecution state) {
        var slotState = state.slotFrame();
        var frameInput = state.inputs();

        if (slotState.status() == EntityStatus.ACTIVE && !slotState.wasInitializing()) {
            FishMotion.Update fishUpdate = state.motion(FishMotion.class, FishMotion::new)
                    .advance(
                            slotState.entity(),
                            state.slot().transitionCountdown(slotState.entity().slot()),
                            frameInput.randomByteSupplier(),
                            frameInput.backgroundCollision());
            slotState.updated(fishUpdate.entity());
            state.slot().setTransitionCountdown(slotState.entity().slot(), fishUpdate.transitionCountdown());
            state.slot().setPhysicsFlags(slotState.entity().slot(), fishUpdate.physicsFlags());
            if (fishUpdate.waterSplash()) {
                state.emit(new TransientVfxRequest(
                        TransientVfxType.WATER_SPLASH,
                        slotState.updated().x(),
                        slotState.updated().y()));
                state.emit(new EntityCombatEvent(
                        slotState.updated().slot(),
                        slotState.updated().type(),
                        0,
                        false,
                        EntityCombatEvent.SoundChannel.JINGLE,
                        0x0E));
            }
        }
        return true;
    }
}
