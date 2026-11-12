package linksawakening.world.entity.handler;

import static linksawakening.world.entity.EntityRomConstants.*;
import static linksawakening.world.entity.EntityRomConstants.ENTITY_POLS_VOICE;
import static linksawakening.world.entity.EntitySnapshots.withStatus;

import java.util.function.IntSupplier;
import linksawakening.world.entity.*;
import linksawakening.world.entity.EntityFrameExecution;
import linksawakening.world.entity.EntityHandlerRegistration;
import linksawakening.world.entity.EntityStatus;
import linksawakening.world.entity.EntityTickStage;
import linksawakening.world.entity.event.EntityCombatEvent;
import linksawakening.world.entity.motion.*;
import linksawakening.world.entity.motion.PolsVoiceMotion;

/** ROM handler stages for PolsVoice. */
final class PolsVoiceHandler {
    static void install(EntityHandlerRegistration registry) {
        registry.registerInitializer(ENTITY_POLS_VOICE, (entity, random) -> initialize(registry, entity, random));
        registry.registerTick(
                ENTITY_POLS_VOICE,
                EntityTickStage.PHASE_2_BEFORE_RECOIL_APPLICATION,
                8,
                PolsVoiceHandler::phase2PolsVoice);
        registry.registerTick(
                ENTITY_POLS_VOICE, EntityTickStage.PHASE_4_BEFORE_FOLLOWER, 21, PolsVoiceHandler::phase4PolsVoice);
    }

    private static boolean phase2PolsVoice(EntityFrameExecution state) {
        var slotState = state.slotFrame();

        if (slotState.status() == EntityStatus.ACTIVE
                && !slotState.wasInitializing()
                && state.enemyInspection().polsVoiceBalladOcarinaTrigger()) {
            // The handler checks this before rendering, recoil, movement,
            // or the ordinary collision path. Its private countdown $1F
            // is the shared EntityDeathHandler's visible death timer.
            state.lifecycle().setDyingCountdown(slotState.entity().slot(), 0x1F);
            state.slot().setPhysicsFlags(slotState.entity().slot(), 0x04);
            slotState.status(EntityStatus.DYING);
            slotState.updated(withStatus(slotState.entity(), EntityStatus.DYING));
            state.emit(new EntityCombatEvent(
                    slotState.entity().slot(),
                    slotState.entity().type(),
                    0,
                    false,
                    EntityCombatEvent.SoundChannel.NOISE,
                    0x13));
        }
        return true;
    }

    private static boolean phase4PolsVoice(EntityFrameExecution state) {
        var slotState = state.slotFrame();
        var frameInput = state.inputs();
        var presentationFlags = state.presentation();

        if (slotState.status() == EntityStatus.ACTIVE && !slotState.wasInitializing()) {
            // PolsVoiceEntityHandler deliberately writes the temporary
            // ignore-hits byte after moving, so the background helper sees
            // $01, then clears it with B (zero) before the default enemy
            // collision pass. The shared Java movement probe is the
            // handler's background-interaction seam, so bracket it here.
            state.slot().setIgnoreHits(slotState.entity().slot(), 0x01);
            try {
                PolsVoiceMotion.Update polsVoiceUpdate = state.motion(PolsVoiceMotion.class, PolsVoiceMotion::new)
                        .advance(
                                slotState.entity(),
                                frameInput.linkEntityX(),
                                frameInput.linkEntityY(),
                                frameInput.randomByteSupplier(),
                                frameInput.backgroundCollision(),
                                state.slot()
                                        .transitionCountdown(slotState.entity().slot()));
                slotState.updated(polsVoiceUpdate.entity());
                state.slot().setTransitionCountdown(slotState.entity().slot(), polsVoiceUpdate.transitionCountdown());
            } finally {
                state.slot().setIgnoreHits(slotState.entity().slot(), 0);
            }
            presentationFlags.preservePolsVoicePresentation(true);
        }
        return true;
    }

    private static void initialize(EntityHandlerRegistration registry, RoomEntity entity, IntSupplier random) {
        registry.motion(PolsVoiceMotion.class, PolsVoiceMotion::new).initialize(entity.slot());
    }
}
