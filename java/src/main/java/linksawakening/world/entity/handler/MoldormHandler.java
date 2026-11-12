package linksawakening.world.entity.handler;

import static linksawakening.world.entity.EntityRomConstants.ENTITY_MOLDORM;
import static linksawakening.world.entity.EntityRomConstants.ENTITY_ROLLING_BONES;
import static linksawakening.world.entity.EntitySnapshots.withDefinition;

import linksawakening.world.entity.EntityFrameExecution;
import linksawakening.world.entity.EntityHandlerRegistration;
import linksawakening.world.entity.EntityStatus;
import linksawakening.world.entity.EntityTickStage;
import linksawakening.world.entity.RoomEntityBackgroundInteraction;
import linksawakening.world.entity.event.DialogRequest;
import linksawakening.world.entity.event.EntityCombatEvent;
import linksawakening.world.entity.event.LinkMotionBlockRequest;
import linksawakening.world.entity.motion.*;
import linksawakening.world.entity.motion.BossIntroMotion;
import linksawakening.world.entity.motion.MoldormMotion;

/** ROM handler stages for Moldorm. */
final class MoldormHandler {
    static void install(EntityHandlerRegistration registry) {
        registry.registerTick(ENTITY_MOLDORM, EntityTickStage.PHASE_1_BOSS_INTRO, 0, MoldormHandler::phase1Moldorm);
        registry.registerTick(ENTITY_MOLDORM, EntityTickStage.PHASE_1_BOSS_DOORWAY, 0, MoldormHandler::phase1Moldorm2);
        registry.registerTick(
                ENTITY_ROLLING_BONES, EntityTickStage.PHASE_1_BOSS_DOORWAY, 0, MoldormHandler::phase1Moldorm2);
        registry.registerTick(
                ENTITY_MOLDORM, EntityTickStage.PHASE_2_AFTER_RECOIL_APPLICATION, 1, MoldormHandler::phase2Moldorm);
        registry.registerTick(ENTITY_MOLDORM, EntityTickStage.PHASE_2_DYING, 3, MoldormHandler::phase2Moldorm2);
        registry.registerTick(
                ENTITY_MOLDORM, EntityTickStage.PHASE_2_AFTER_INITIALIZE, 5, MoldormHandler::phase2Moldorm3);
    }

    private static boolean phase1Moldorm(EntityFrameExecution state) {
        var slotState = state.slotFrame();

        if (!slotState.wasInitializing()
                && !state.roomInspection().roomTransitionActive()
                && !state.link().inventoryAppearing()) {
            BossIntroMotion.Update intro = state.motion(BossIntroMotion.class, BossIntroMotion::new)
                    .advance(
                            state.slotProperties().options1(slotState.entity().slot()),
                            slotState.entity().type(),
                            state.roomInspection().entityMapId(),
                            state.lifecycle().transitionSequenceCounter());
            if (intro.musicTrack() >= 0) state.requestMusic(intro.musicTrack());
            if (intro.dialogLowId() >= 0) {
                state.emit(new DialogRequest(intro.dialogTableId(), intro.dialogLowId()));
                slotState.moldormIntroOpenedThisFrame(true);
            }
        }
        return true;
    }

    private static boolean phase1Moldorm2(EntityFrameExecution state) {
        var slotState = state.slotFrame();
        var frameInput = state.inputs();

        if (!slotState.wasInitializing()
                && !state.roomInspection().roomTransitionActive()
                && !state.roomInspection().freeMovementMode()) {
            int dx = 0;
            int dy = 0;
            if (((frameInput.linkEntityX() - 0x11) & 0xFF) >= 0x7E) {
                dx = frameInput.linkEntityX() < 0x50 ? 1 : -1;
            } else if (((frameInput.linkEntityY() - 0x16) & 0xFF) >= 0x5E) {
                dy = -1;
            }
            if (dx != 0 || dy != 0) {
                state.emit(new LinkMotionBlockRequest(slotState.entity().slot(), dx, dy, 1));
                frameInput.linkEntityX((frameInput.linkEntityX() + dx) & 0xFF);
                frameInput.linkEntityY((frameInput.linkEntityY() + dy) & 0xFF);
                state.lifecycle().delayInteractions(2);
            }
        }
        return true;
    }

    private static boolean phase2Moldorm(EntityFrameExecution state) {
        var slotState = state.slotFrame();
        var frameInput = state.inputs();
        var timerGates = state.gates();
        var presentationFlags = state.presentation();

        if (slotState.status() == EntityStatus.ACTIVE && !slotState.wasInitializing()) {
            int slot = slotState.entity().slot();
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
                return false;
            }
            RoomEntityBackgroundInteraction moldormBackgroundInteraction =
                    state.background().interaction();
            if (moldormBackgroundInteraction == null && frameInput.backgroundCollision() != null) {
                moldormBackgroundInteraction =
                        RoomEntityBackgroundInteraction.fromBoolean(frameInput.backgroundCollision());
            }
            int transitionForMotion = state.slot().transitionCountdown(slot);
            if (transitionForMotion != 0) {
                // Common countdowns have already ticked before dispatch;
                // the direct motion API performs the same decrement.
                transitionForMotion = (transitionForMotion + 1) & 0xFF;
            }
            MoldormMotion.Update moldormUpdate = state.motion(MoldormMotion.class, MoldormMotion::new)
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
        }
        return true;
    }

    private static boolean phase2Moldorm2(EntityFrameExecution state) {
        var slotState = state.slotFrame();
        var frameInput = state.inputs();

        state.bossRewards().advanceMoldormDestruction(slotState.entity(), frameInput.frame());
        return false;
    }

    private static boolean phase2Moldorm3(EntityFrameExecution state) {
        var slotState = state.slotFrame();

        state.motion(MoldormMotion.class, MoldormMotion::new).initialize(slotState.entity());

        return true;
    }
}
