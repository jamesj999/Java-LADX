package linksawakening.world.entity.handler;

import static linksawakening.world.entity.EntityRomConstants.ENTITY_HINOX;

import linksawakening.vfx.TransientVfxType;
import linksawakening.world.entity.EntityFrameExecution;
import linksawakening.world.entity.EntityHandlerRegistration;
import linksawakening.world.entity.EntityStatus;
import linksawakening.world.entity.EntityTickStage;
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

/** ROM handler stages for Hinox. */
final class HinoxHandler {
    static void install(EntityHandlerRegistration registry) {
        registry.registerTick(ENTITY_HINOX, EntityTickStage.PHASE_4_BEFORE_FOLLOWER, 2, HinoxHandler::phase4Hinox);
    }

    private static boolean phase4Hinox(EntityFrameExecution state) {
        var slotState = state.slotFrame();
        var frameInput = state.inputs();
        var timerGates = state.gates();
        var presentationFlags = state.presentation();

        if (slotState.status() == EntityStatus.ACTIVE
                && !slotState.wasInitializing()
                && timerGates.handlerLinkCollisionEnabled()
                && state.background().entityTimersInteractive()
                && state.lifecycle().transitionSequenceCounter() == 0x04
                && !frameInput.creditsGameplay()) {
            int slot = slotState.entity().slot();
            BossIntroMotion.Update bossIntro = state.motion(BossIntroMotion.class, BossIntroMotion::new)
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
            HinoxMotion.Update hinoxUpdate = state.motion(HinoxMotion.class, HinoxMotion::new)
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
        }
        return true;
    }
}
