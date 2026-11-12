package linksawakening.world.entity.handler;

import static linksawakening.world.entity.EntityRomConstants.ENTITY_POLS_VOICE;
import static linksawakening.world.entity.EntitySnapshots.withStatus;

import linksawakening.world.entity.EntityFrameExecution;
import linksawakening.world.entity.EntityHandlerRegistration;
import linksawakening.world.entity.EntityStatus;
import linksawakening.world.entity.EntityTickStage;
import linksawakening.world.entity.event.EntityCombatEvent;
import linksawakening.world.entity.motion.PolsVoiceMotion;

/** ROM handler stages for PolsVoice. */
final class PolsVoiceHandler {
    static void install(EntityHandlerRegistration registry) {
        registry.registerTick(ENTITY_POLS_VOICE, EntityTickStage.PHASE_2_POLS_VOICE, PolsVoiceHandler::phase2PolsVoice);
        registry.registerTick(ENTITY_POLS_VOICE, EntityTickStage.PHASE_4_POLS_VOICE, PolsVoiceHandler::phase4PolsVoice);
    }

    private static boolean phase2PolsVoice(EntityFrameExecution state) {
        if (state.status == EntityStatus.ACTIVE
                && !state.wasInitializing
                && state.enemyInspection().polsVoiceBalladOcarinaTrigger()) {
            // The handler checks this before rendering, recoil, movement,
            // or the ordinary collision path. Its private countdown $1F
            // is the shared EntityDeathHandler's visible death timer.
            state.lifecycle().setDyingCountdown(state.entity.slot(), 0x1F);
            state.slot().setPhysicsFlags(state.entity.slot(), 0x04);
            state.status = EntityStatus.DYING;
            state.updated = withStatus(state.entity, EntityStatus.DYING);
            state.emit(new EntityCombatEvent(
                    state.entity.slot(), state.entity.type(), 0, false, EntityCombatEvent.SoundChannel.NOISE, 0x13));
        }
        return true;
    }

    private static boolean phase4PolsVoice(EntityFrameExecution state) {
        if (state.status == EntityStatus.ACTIVE && !state.wasInitializing) {
            // PolsVoiceEntityHandler deliberately writes the temporary
            // ignore-hits byte after moving, so the background helper sees
            // $01, then clears it with B (zero) before the default enemy
            // collision pass. The shared Java movement probe is the
            // handler's background-interaction seam, so bracket it here.
            state.slot().setIgnoreHits(state.entity.slot(), 0x01);
            try {
                PolsVoiceMotion.Update polsVoiceUpdate = state.polsVoiceMotion()
                        .advance(
                                state.entity,
                                state.linkEntityX,
                                state.linkEntityY,
                                state.randomByteSupplier,
                                state.backgroundCollision,
                                state.slot().transitionCountdown(state.entity.slot()));
                state.updated = polsVoiceUpdate.entity();
                state.slot().setTransitionCountdown(state.entity.slot(), polsVoiceUpdate.transitionCountdown());
            } finally {
                state.slot().setIgnoreHits(state.entity.slot(), 0);
            }
            state.preservePolsVoicePresentation = true;
        }
        return true;
    }
}
