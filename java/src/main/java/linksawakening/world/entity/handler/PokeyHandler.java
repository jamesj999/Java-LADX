package linksawakening.world.entity.handler;

import static linksawakening.world.entity.EntityRomConstants.ENTITY_POKEY;
import static linksawakening.world.entity.EntitySnapshots.withDefinition;

import linksawakening.vfx.TransientVfxType;
import linksawakening.world.entity.EntityFrameExecution;
import linksawakening.world.entity.EntityHandlerRegistration;
import linksawakening.world.entity.EntityStatus;
import linksawakening.world.entity.EntityTickStage;
import linksawakening.world.entity.event.EntityCombatEvent;
import linksawakening.world.entity.event.TransientVfxRequest;
import linksawakening.world.entity.motion.*;
import linksawakening.world.entity.motion.PokeyMotion;

/** ROM handler stages for Pokey. */
final class PokeyHandler {
    static void install(EntityHandlerRegistration registry) {
        registry.registerTick(ENTITY_POKEY, EntityTickStage.PHASE_4_BEFORE_FOLLOWER, 8, PokeyHandler::phase4Pokey);
    }

    private static boolean phase4Pokey(EntityFrameExecution state) {
        var slotState = state.slotFrame();
        var frameInput = state.inputs();
        var timerGates = state.gates();
        var presentationFlags = state.presentation();

        if (slotState.status() == EntityStatus.ACTIVE && !slotState.wasInitializing()) {
            int slot = slotState.entity().slot();
            if (!timerGates.handlerLinkCollisionEnabled()) {
                // Both Pokey branches return immediately after their
                // pre-handler display pass when Link is non-interactive.
            } else if (state.motion(PokeyMotion.class, PokeyMotion::new).isSegment(slot)) {
                PokeyMotion.Update segmentUpdate = state.motion(PokeyMotion.class, PokeyMotion::new)
                        .advanceSegment(
                                slotState.updated(),
                                state.slot().pokeyInertia(slot),
                                state.slot().transitionCountdown(slot),
                                frameInput.backgroundCollision());
                slotState.updated(segmentUpdate.entity());
                state.slot().setPokeyInertia(slot, segmentUpdate.inertia());
                state.slot().setTransitionCountdown(slot, segmentUpdate.transitionCountdown());
                if (segmentUpdate.unloadRequested()) {
                    state.emit(new TransientVfxRequest(
                            TransientVfxType.POOF,
                            slotState.updated().x(),
                            (slotState.updated().y() - slotState.updated().z()) & 0xFF));
                    state.emit(new EntityCombatEvent(
                            slot, slotState.entity().type(), 0, false, EntityCombatEvent.SoundChannel.JINGLE, 0x2F));
                    state.lifecycle().disableEntityWithoutPersistence(slot);
                    return false;
                }
                if (segmentUpdate.bumpJingle()) {
                    state.emit(new EntityCombatEvent(
                            slot, slotState.entity().type(), 0, false, EntityCombatEvent.SoundChannel.JINGLE, 0x09));
                }
            } else {
                if (state.slot().pokeyInertia(slot) < 0x02) {
                    // PokeyEntityHandler replaces the normal group-$00
                    // health with two hit points until its body is fully
                    // separated.
                    state.slot().setHealth(slot, 0x02);
                }
                PokeyMotion.Update pokeyUpdate = state.motion(PokeyMotion.class, PokeyMotion::new)
                        .advanceMain(
                                slotState.updated(),
                                frameInput.frame(),
                                state.slot().pokeyInertia(slot),
                                state.slot().transitionCountdown(slot),
                                state.slot().flashCountdown(slot),
                                frameInput.linkEntityX(),
                                frameInput.linkEntityY(),
                                frameInput.randomByteSupplier(),
                                frameInput.backgroundCollision());
                slotState.updated(pokeyUpdate.entity());
                state.slot().setPokeyInertia(slot, pokeyUpdate.inertia());
                state.slot().setTransitionCountdown(slot, pokeyUpdate.transitionCountdown());
                state.slot().setFlashCountdown(slot, pokeyUpdate.flashCountdown());
                if (pokeyUpdate.segmentSpawn() != null) {
                    state.enemySpawns().spawnPokeySegment(pokeyUpdate.segmentSpawn());
                }
                if (state.sprites() != null) {
                    slotState.updated(withDefinition(
                            slotState.updated(),
                            state.sprites().forPokeyState(pokeyUpdate.renderInertia()),
                            slotState.updated().spriteVariant()));
                }
            }
            presentationFlags.preservePokeyPresentation(true);
        }
        return true;
    }
}
