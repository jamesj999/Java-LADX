package linksawakening.world.entity.handler;

import static linksawakening.world.entity.EntityRomConstants.*;
import static linksawakening.world.entity.EntityRomConstants.ENTITY_ARMOS_KNIGHT;
import static linksawakening.world.entity.EntityRomConstants.ENTITY_LIFTABLE_ROCK;

import java.util.function.IntSupplier;
import linksawakening.vfx.TransientVfxType;
import linksawakening.world.entity.*;
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
import linksawakening.world.entity.motion.*;
import linksawakening.world.entity.motion.ArmosKnightFrame;
import linksawakening.world.entity.motion.ArmosKnightMotion;
import linksawakening.world.entity.motion.BossIntroMotion;

/** ROM handler stages for ArmosKnight. */
final class ArmosKnightHandler {
    static void install(EntityHandlerRegistration registry) {
        registry.registerInitializer(ENTITY_ARMOS_KNIGHT, (entity, random) -> initialize(registry, entity, random));
        registry.registerTick(
                ENTITY_ARMOS_KNIGHT, EntityTickStage.PHASE_4_BEFORE_FOLLOWER, 1, ArmosKnightHandler::phase4ArmosKnight);
    }

    private static boolean phase4ArmosKnight(EntityFrameExecution state) {
        var slotState = state.slotFrame();
        var frameInput = state.inputs();
        var presentationFlags = state.presentation();

        if (slotState.status() == EntityStatus.ACTIVE && !slotState.wasInitializing()) {
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
            state.background()
                    .requestArmosLinkPush(
                            slotState.entity(),
                            frameInput.linkEntityX(),
                            frameInput.linkEntityY(),
                            frameInput.projectileLinkState().motionState());
            ArmosKnightMotion.Update armosKnightUpdate = state.motion(ArmosKnightMotion.class, ArmosKnightMotion::new)
                    .advance(new ArmosKnightFrame(
                            slotState.entity(),
                            frameInput.linkEntityX(),
                            frameInput.linkEntityY(),
                            frameInput.linkZ(),
                            state.slot().transitionCountdown(slotState.entity().slot()),
                            state.slot().physicsFlags(slotState.entity().slot()),
                            state.slot().hitboxFlags(slotState.entity().slot()),
                            state.slotProperties().options1(slotState.entity().slot()),
                            state.slot().health(slotState.entity().slot())));
            slotState.updated(armosKnightUpdate.entity());
            state.slot().setTransitionCountdown(slotState.entity().slot(), armosKnightUpdate.transitionCountdown());
            state.slot().setPhysicsFlags(slotState.entity().slot(), armosKnightUpdate.physicsFlags());
            state.slot().setHitboxFlags(slotState.entity().slot(), armosKnightUpdate.hitboxFlags());
            state.slot().setOptions1Override(slotState.entity().slot(), armosKnightUpdate.options1());
            presentationFlags.preserveArmosKnightPresentation(true);
            if (armosKnightUpdate.linkMotionBlocked()) {
                state.emit(new LinkMotionBlockRequest(slotState.entity().slot()));
                state.emit(new LinkFallenPoseRequest(slotState.entity().slot()));
            }
            if (armosKnightUpdate.screenShakeRequest() != null) {
                ArmosKnightMotion.ScreenShakeRequest shake = armosKnightUpdate.screenShakeRequest();
                state.emit(new ScreenShakeRequest(slotState.entity().slot(), shake.countdown(), shake.phase()));
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
                        slotState.entity().slot(),
                        slotState.entity().type(),
                        0,
                        false,
                        EntityCombatEvent.SoundChannel.JINGLE,
                        armosKnightUpdate.jingleId()));
            }
        }
        return true;
    }

    private static void initialize(EntityHandlerRegistration registry, RoomEntity entity, IntSupplier random) {
        registry.motion(ArmosKnightMotion.class, ArmosKnightMotion::new).initialize(entity.slot());
    }
}
