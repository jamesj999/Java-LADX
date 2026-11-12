package linksawakening.world.entity.handler;

import static linksawakening.world.entity.EntityRomConstants.*;

import linksawakening.vfx.TransientVfxType;
import linksawakening.world.entity.*;
import linksawakening.world.entity.EntityStatus;
import linksawakening.world.entity.event.DialogRequest;
import linksawakening.world.entity.event.EntityCombatEvent;
import linksawakening.world.entity.event.HinoxLinkEffectRequest;
import linksawakening.world.entity.event.LinkFallenPoseRequest;
import linksawakening.world.entity.event.LinkMotionBlockRequest;
import linksawakening.world.entity.event.TransientVfxRequest;
import linksawakening.world.entity.motion.*;
import linksawakening.world.entity.motion.BossIntroMotion;
import linksawakening.world.entity.motion.HinoxFrame;
import linksawakening.world.entity.motion.HinoxMotion;

/** Reused, reentrancy-safe scratch for HinoxHandler.phase4Hinox; values are assigned in source order. */
final class HinoxHandlerFrameStep {
    private linksawakening.world.entity.EntityFrameExecution state;
    private linksawakening.world.entity.EntitySlotExecution slotState;
    private linksawakening.world.entity.EntityFrameInputs frameInput;
    private linksawakening.world.entity.EntityTimerGates timerGates;
    private linksawakening.world.entity.EntityPresentationFlags presentationFlags;

    HinoxHandlerFrameStep() {}

    boolean run() {
        if (!advanceMotion()) return completedResult;
        if (!applyLinkAndBombEffects()) return completedResult;
        if (!emitPresentationEffects()) return completedResult;

        return true;
    }

    private int slot;
    private linksawakening.world.entity.motion.BossIntroMotion.Update bossIntro;
    private linksawakening.world.entity.motion.HinoxMotion.Update hinoxUpdate;

    private boolean completedResult;

    private boolean advanceMotion() {

        slotState = state.slotFrame();
        frameInput = state.inputs();
        timerGates = state.gates();
        presentationFlags = state.presentation();

        if (!(slotState.status() == EntityStatus.ACTIVE
                && !slotState.wasInitializing()
                && timerGates.handlerLinkCollisionEnabled()
                && state.background().entityTimersInteractive()
                && state.lifecycle().transitionSequenceCounter() == 4
                && !frameInput.creditsGameplay())) {
            completedResult = true;
            return false;
        }

        slot = slotState.entity().slot();
        bossIntro = state.motion(BossIntroMotion.class, BossIntroMotion::new)
                .advance(
                        state.slotProperties().options1(slotState.entity().slot()),
                        slotState.entity().type(),
                        state.roomInspection().entityMapId(),
                        state.lifecycle().transitionSequenceCounter());
        if (bossIntro.musicTrack() >= 0) {
            state.requestMusic(bossIntro.musicTrack());
        }
        if (bossIntro.dialogLowId() >= 0) {
            state.emit(new DialogRequest(bossIntro.dialogTableId(), bossIntro.dialogLowId()));
        }
        hinoxUpdate = state.motion(HinoxMotion.class, HinoxMotion::new)
                .advance(new HinoxFrame(
                        slotState.entity(),
                        state.slot().transitionCountdown(slot),
                        frameInput.frame(),
                        frameInput.linkEntityX(),
                        frameInput.linkEntityY(),
                        frameInput.linkZ(),
                        frameInput.projectileLinkState().motionState(),
                        frameInput.randomByteSupplier(),
                        frameInput.hinoxBackgroundInteraction(),
                        state.motion(HinoxMotion.class, HinoxMotion::new).collisionFlags(slot),
                        state.slot().flashCountdown(slot)));
        slotState.updated(hinoxUpdate.entity());
        state.slot().setTransitionCountdown(slot, hinoxUpdate.transitionCountdown());
        presentationFlags.preserveHinoxPresentation(true);
        if (hinoxUpdate.linkMotionBlocked()) {
            state.emit(new LinkMotionBlockRequest(slot, 0, 0, 1));
            state.emit(new LinkFallenPoseRequest(slot));
        }
        return true;
    }

    private boolean applyLinkAndBombEffects() {

        if (hinoxUpdate.heldLinkPositionWritten()) {
            state.emit(new HinoxLinkEffectRequest(
                    slot,
                    hinoxUpdate.heldLinkX(),
                    hinoxUpdate.heldLinkY(),
                    hinoxUpdate.heldLinkZ(),
                    hinoxUpdate.linkSpeedX(),
                    hinoxUpdate.linkSpeedY(),
                    hinoxUpdate.linkVelocityZ(),
                    hinoxUpdate.linkAirborneState(),
                    hinoxUpdate.linkDamage(),
                    hinoxUpdate.linkMotionBlocked(),
                    hinoxUpdate.applyHeldLinkPose()));
        }
        if (hinoxUpdate.bombSpawn() != null) {
            HinoxMotion.BombSpawn spawn = hinoxUpdate.bombSpawn();
            state.enemySpawns()
                    .spawnEnemyBomb(
                            spawn.x(),
                            spawn.y(),
                            spawn.z(),
                            spawn.transitionCountdown(),
                            spawn.speedX(),
                            spawn.speedY(),
                            spawn.speedZ());
        }
        if (!hinoxUpdate.heldLinkPositionWritten()
                && !hinoxUpdate.linkMotionBlocked()
                && (hinoxUpdate.linkSpeedX() != 0
                        || hinoxUpdate.linkSpeedY() != 0
                        || hinoxUpdate.linkVelocityZ() != 0
                        || hinoxUpdate.linkDamage() != 0)) {
            state.emit(new HinoxLinkEffectRequest(
                    slot,
                    hinoxUpdate.heldLinkX(),
                    hinoxUpdate.heldLinkY(),
                    hinoxUpdate.heldLinkZ(),
                    hinoxUpdate.linkSpeedX(),
                    hinoxUpdate.linkSpeedY(),
                    hinoxUpdate.linkVelocityZ(),
                    hinoxUpdate.linkAirborneState(),
                    hinoxUpdate.linkDamage(),
                    hinoxUpdate.linkMotionBlocked(),
                    hinoxUpdate.applyHeldLinkPose()));
        }
        return true;
    }

    private boolean emitPresentationEffects() {

        if (hinoxUpdate.dustRequested()) {
            state.emit(new TransientVfxRequest(
                    TransientVfxType.PEGASUS_DUST,
                    slotState.updated().x(),
                    (slotState.updated().y() + 0x0A) & 0xFF));
        }
        if (hinoxUpdate.jingleId() >= 0) {
            state.emit(new EntityCombatEvent(
                    slot,
                    slotState.entity().type(),
                    0,
                    false,
                    EntityCombatEvent.SoundChannel.JINGLE,
                    hinoxUpdate.jingleId()));
        }
        if (hinoxUpdate.waveId() >= 0) {
            state.emit(new EntityCombatEvent(
                    slot,
                    slotState.entity().type(),
                    0,
                    false,
                    EntityCombatEvent.SoundChannel.WAVE,
                    hinoxUpdate.waveId()));
        }
        return true;
    }

    private boolean inUse;

    boolean run(linksawakening.world.entity.EntityFrameExecution state) {
        if (inUse) {
            return new HinoxHandlerFrameStep().run(state);
        }
        inUse = true;
        slotState = null;
        frameInput = null;
        timerGates = null;
        presentationFlags = null;
        slot = 0;
        bossIntro = null;
        hinoxUpdate = null;
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
            this.bossIntro = null;
            this.hinoxUpdate = null;
        }
    }
}
