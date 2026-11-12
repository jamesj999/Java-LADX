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
import linksawakening.world.entity.motion.BossIntroMotion;
import linksawakening.world.entity.motion.HinoxMotion;

/** ROM handler stages for Hinox. */
final class HinoxHandler {
    static void install(EntityHandlerRegistration registry) {
        registry.registerTick(ENTITY_HINOX, EntityTickStage.PHASE_4_BEFORE_FOLLOWER, 2, HinoxHandler::phase4Hinox);
    }

    private static boolean phase4Hinox(EntityFrameExecution state) {
        if (state.status == EntityStatus.ACTIVE
                && !state.wasInitializing
                && state.handlerLinkCollisionEnabled
                && state.background().entityTimersInteractive()
                && state.lifecycle().transitionSequenceCounter() == 0x04
                && !state.creditsGameplay) {
            int slot = state.entity.slot();
            BossIntroMotion.Update bossIntro = state.bossIntroMotion()
                    .advance(
                            state.slotProperties().options1(state.entity.slot()),
                            state.entity.type(),
                            state.roomInspection().entityMapId(),
                            state.lifecycle().transitionSequenceCounter());
            if (bossIntro.musicTrack() >= 0) {
                state.requestMusic(bossIntro.musicTrack());
            }
            if (bossIntro.dialogLowId() >= 0) {
                state.emit(new DialogRequest(bossIntro.dialogTableId(), bossIntro.dialogLowId()));
            }
            HinoxMotion.Update hinoxUpdate = state.hinoxMotion()
                    .advance(
                            state.entity,
                            state.slot().transitionCountdown(slot),
                            state.frame,
                            state.linkEntityX,
                            state.linkEntityY,
                            state.linkZ,
                            state.projectileLinkState.motionState(),
                            state.randomByteSupplier,
                            state.hinoxBackgroundInteraction,
                            state.hinoxMotion().collisionFlags(slot),
                            state.slot().flashCountdown(slot));
            state.updated = hinoxUpdate.entity();
            state.slot().setTransitionCountdown(slot, hinoxUpdate.transitionCountdown());
            state.preserveHinoxPresentation = true;
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
                        TransientVfxType.PEGASUS_DUST, state.updated.x(), (state.updated.y() + 0x0A) & 0xFF));
            }
            if (hinoxUpdate.jingleId() >= 0) {
                state.emit(new EntityCombatEvent(
                        slot,
                        state.entity.type(),
                        0,
                        false,
                        EntityCombatEvent.SoundChannel.JINGLE,
                        hinoxUpdate.jingleId()));
            }
            if (hinoxUpdate.waveId() >= 0) {
                state.emit(new EntityCombatEvent(
                        slot,
                        state.entity.type(),
                        0,
                        false,
                        EntityCombatEvent.SoundChannel.WAVE,
                        hinoxUpdate.waveId()));
            }
        }
        return true;
    }
}
