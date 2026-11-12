package linksawakening.world.entity.handler;

import static linksawakening.world.entity.EntityRomConstants.ENTITY_ARMOS_KNIGHT;
import static linksawakening.world.entity.EntityRomConstants.ENTITY_LIFTABLE_ROCK;

import linksawakening.vfx.TransientVfxType;
import linksawakening.world.entity.EntityFrameExecution;
import linksawakening.world.entity.EntityHandlerRegistration;
import linksawakening.world.entity.EntityStatus;
import linksawakening.world.entity.EntityTickStage;
import linksawakening.world.entity.event.DialogRequest;
import linksawakening.world.entity.event.EntityCombatEvent;
import linksawakening.world.entity.event.LinkFallenPoseRequest;
import linksawakening.world.entity.event.LinkMotionBlockRequest;
import linksawakening.world.entity.event.ScreenShakeRequest;
import linksawakening.world.entity.event.TransientVfxRequest;
import linksawakening.world.entity.motion.ArmosKnightMotion;
import linksawakening.world.entity.motion.BossIntroMotion;

/** ROM handler stages for ArmosKnight. */
final class ArmosKnightHandler {
    static void install(EntityHandlerRegistration registry) {
        registry.registerTick(
                ENTITY_ARMOS_KNIGHT, EntityTickStage.PHASE_4_BEFORE_FOLLOWER, 1, ArmosKnightHandler::phase4ArmosKnight);
    }

    private static boolean phase4ArmosKnight(EntityFrameExecution state) {
        if (state.status == EntityStatus.ACTIVE && !state.wasInitializing) {
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
            state.background()
                    .requestArmosLinkPush(
                            state.entity,
                            state.linkEntityX,
                            state.linkEntityY,
                            state.projectileLinkState.motionState());
            ArmosKnightMotion.Update armosKnightUpdate = state.armosKnightMotion()
                    .advance(
                            state.entity,
                            state.linkEntityX,
                            state.linkEntityY,
                            state.linkZ,
                            state.slot().transitionCountdown(state.entity.slot()),
                            state.slot().physicsFlags(state.entity.slot()),
                            state.slot().hitboxFlags(state.entity.slot()),
                            state.slotProperties().options1(state.entity.slot()),
                            state.slot().health(state.entity.slot()));
            state.updated = armosKnightUpdate.entity();
            state.slot().setTransitionCountdown(state.entity.slot(), armosKnightUpdate.transitionCountdown());
            state.slot().setPhysicsFlags(state.entity.slot(), armosKnightUpdate.physicsFlags());
            state.slot().setHitboxFlags(state.entity.slot(), armosKnightUpdate.hitboxFlags());
            state.slot().setOptions1Override(state.entity.slot(), armosKnightUpdate.options1());
            state.preserveArmosKnightPresentation = true;
            if (armosKnightUpdate.linkMotionBlocked()) {
                state.emit(new LinkMotionBlockRequest(state.entity.slot()));
                state.emit(new LinkFallenPoseRequest(state.entity.slot()));
            }
            if (armosKnightUpdate.screenShakeRequest() != null) {
                ArmosKnightMotion.ScreenShakeRequest shake = armosKnightUpdate.screenShakeRequest();
                state.emit(new ScreenShakeRequest(state.entity.slot(), shake.countdown(), shake.phase()));
            }
            if (armosKnightUpdate.rubbleRequest() != null) {
                ArmosKnightMotion.RubbleRequest rubble = armosKnightUpdate.rubbleRequest();
                int rubbleSlot = state.playerSpawns().spawnLiftableRockRubble(rubble.x(), rubble.y());
                if (rubbleSlot >= 0) {
                    state.emit(new TransientVfxRequest(TransientVfxType.POOF, rubble.x(), rubble.y()));
                    state.emit(new EntityCombatEvent(
                            rubbleSlot, ENTITY_LIFTABLE_ROCK, 0, false, EntityCombatEvent.SoundChannel.NOISE, 0x29));
                }
            }
            if (armosKnightUpdate.jingleId() >= 0) {
                state.emit(new EntityCombatEvent(
                        state.entity.slot(),
                        state.entity.type(),
                        0,
                        false,
                        EntityCombatEvent.SoundChannel.JINGLE,
                        armosKnightUpdate.jingleId()));
            }
        }
        return true;
    }
}
