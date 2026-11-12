package linksawakening.world.entity.handler;

import static linksawakening.world.entity.EntitySnapshots.withDefinition;

import linksawakening.world.entity.EntityStatus;
import linksawakening.world.entity.event.EntityCombatEvent;
import linksawakening.world.entity.motion.*;
import linksawakening.world.entity.motion.MoldormMotion;

/** Reused, reentrancy-safe scratch for MoldormHandler.phase2Moldorm; values are assigned in source order. */
final class MoldormHandlerFrameStep {
    private linksawakening.world.entity.EntityFrameExecution state;
    private linksawakening.world.entity.EntitySlotExecution slotState;
    private linksawakening.world.entity.EntityFrameInputs frameInput;
    private linksawakening.world.entity.EntityTimerGates timerGates;
    private linksawakening.world.entity.EntityPresentationFlags presentationFlags;

    MoldormHandlerFrameStep() {}

    boolean run() {
        if (!prepareMotion()) return completedResult;
        if (!applyMotionAndPresentation()) return completedResult;

        return true;
    }

    private int slot;
    private linksawakening.world.entity.RoomEntityBackgroundInteraction moldormBackgroundInteraction;
    private int transitionForMotion;
    private linksawakening.world.entity.motion.MoldormMotion.Update moldormUpdate;

    private boolean completedResult;

    private boolean prepareMotion() {

        slotState = state.slotFrame();
        frameInput = state.inputs();
        timerGates = state.gates();
        presentationFlags = state.presentation();

        if (!(slotState.status() == EntityStatus.ACTIVE && !slotState.wasInitializing())) {
            completedResult = true;
            return false;
        }

        slot = slotState.entity().slot();
        if (!timerGates.handlerInteractive()
                || timerGates.countdownBlocksInteraction()
                || slotState.moldormIntroOpenedThisFrame()) {
            if (state.sprites() != null) {
                state.replaceEntity(withDefinition(
                        slotState.entity(),
                        state.sprites()
                                .forMoldormState(
                                        state.motion(MoldormMotion.class, MoldormMotion::new)
                                                .headSpriteVariant(slot),
                                        slotState.entity().x(),
                                        (slotState.entity().y()
                                                        - slotState.entity().z())
                                                & 0xFF,
                                        state.motion(MoldormMotion.class, MoldormMotion::new)
                                                .currentTailSegments(slot),
                                        0,
                                        frameInput.frame()),
                        0));
            }
            if (state.roomInspection().roomTransitionActive())
                state.motion(MoldormMotion.class, MoldormMotion::new).resetHistory(slot);
            {
                completedResult = false;
                return false;
            }
        }
        moldormBackgroundInteraction = state.background().interaction();
        if (moldormBackgroundInteraction == null && frameInput.backgroundCollision() != null) {
            moldormBackgroundInteraction = frameInput.hinoxBackgroundInteraction();
        }
        transitionForMotion = state.slot().transitionCountdown(slot);
        if (transitionForMotion != 0) {
            // Common countdowns have already ticked before dispatch;
            // the direct motion API performs the same decrement.
            transitionForMotion = (transitionForMotion + 1) & 0xFF;
        }
        return true;
    }

    private boolean applyMotionAndPresentation() {

        moldormUpdate = state.motion(MoldormMotion.class, MoldormMotion::new)
                .advance(
                        slotState.entity(),
                        frameInput.frame(),
                        transitionForMotion,
                        state.slot().ignoreHits(slot),
                        state.slot().flashCountdown(slot),
                        state.slot().health(slot),
                        frameInput.randomByteSupplier(),
                        moldormBackgroundInteraction);
        slotState.updated(moldormUpdate.entity());
        state.slot().setTransitionCountdown(slot, moldormUpdate.transitionCountdown());
        if (moldormUpdate.noiseRequested()) {
            state.emit(new EntityCombatEvent(
                    slot, slotState.entity().type(), 0, false, EntityCombatEvent.SoundChannel.NOISE, 0x1B));
        }
        if (state.sprites() != null) {
            MoldormMotion.Presentation presentation = moldormUpdate.presentation();
            slotState.updated(withDefinition(
                    slotState.updated(),
                    state.sprites()
                            .forMoldormPresentation(
                                    presentation.headSpriteVariant(),
                                    presentation.headX(),
                                    presentation.headY(),
                                    slotState.updated().x(),
                                    (slotState.updated().y()
                                                    - slotState.updated().z())
                                            & 0xFF,
                                    presentation.segments(),
                                    0,
                                    frameInput.frame()),
                    0));
        }
        state.slot().setPhysicsFlags(slot, 0x02);
        presentationFlags.preserveMoldormPresentation(true);
        return true;
    }

    private boolean inUse;

    boolean run(linksawakening.world.entity.EntityFrameExecution state) {
        if (inUse) {
            return new MoldormHandlerFrameStep().run(state);
        }
        inUse = true;
        slotState = null;
        frameInput = null;
        timerGates = null;
        presentationFlags = null;
        slot = 0;
        moldormBackgroundInteraction = null;
        transitionForMotion = 0;
        moldormUpdate = null;
        completedResult = false;
        this.state = state;
        try {
            return run();
        } finally {
            inUse = false;
            this.state = null;
            this.slotState = null;
            this.frameInput = null;
            this.timerGates = null;
            this.presentationFlags = null;
            this.moldormBackgroundInteraction = null;
            this.moldormUpdate = null;
        }
    }
}
