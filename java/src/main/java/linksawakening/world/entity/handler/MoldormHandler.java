package linksawakening.world.entity.handler;

import static linksawakening.world.entity.EntityRomConstants.ENTITY_MOLDORM;
import static linksawakening.world.entity.EntityRomConstants.ENTITY_ROLLING_BONES;

import java.util.function.Consumer;
import linksawakening.world.entity.EntityBossRewards;
import linksawakening.world.entity.EntityFrameExecution;
import linksawakening.world.entity.EntityFrameInputLatch;
import linksawakening.world.entity.EntityHandlerRegistry;
import linksawakening.world.entity.EntityPresentationFlags;
import linksawakening.world.entity.EntityRoomInspection;
import linksawakening.world.entity.EntitySlotExecution;
import linksawakening.world.entity.EntitySlotView;
import linksawakening.world.entity.EntitySpriteViews;
import linksawakening.world.entity.EntityStatus;
import linksawakening.world.entity.EntityTickStage;
import linksawakening.world.entity.EntityTimerGates;
import linksawakening.world.entity.RoomEntity;
import linksawakening.world.entity.RoomEntityBackgroundInteraction;
import linksawakening.world.entity.event.DialogRequest;
import linksawakening.world.entity.event.EntityCombatEvent;
import linksawakening.world.entity.event.LinkMotionBlockRequest;
import linksawakening.world.entity.motion.BossIntroMotion;
import linksawakening.world.entity.motion.MoldormMotion;

/** ROM handler stages for Moldorm. */
final class MoldormHandler {
    static void install(EntityHandlerRegistry registry) {
        registry.registerTick(ENTITY_MOLDORM, EntityTickStage.PHASE_1_BOSS_INTRO, 0, MoldormHandler::phase1Moldorm);
        registry.registerTick(ENTITY_MOLDORM, EntityTickStage.PHASE_1_BOSS_DOORWAY, 0, MoldormHandler::phase1Moldorm2);
        registry.registerTick(
                ENTITY_ROLLING_BONES, EntityTickStage.PHASE_1_BOSS_DOORWAY, 0, MoldormHandler::phase1Moldorm2);
        registry.registerTick(
                ENTITY_MOLDORM, EntityTickStage.PHASE_2_AFTER_RECOIL_APPLICATION, 1, MoldormHandler::phase2Moldorm);
        registry.registerTick(
                ENTITY_MOLDORM,
                EntityTickStage.PHASE_2_DYING,
                3,
                frame -> phase2Moldorm2(frame.slotFrame(), frame.inputs(), frame.bossRewards()));
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
        MoldormHandlerFrameStep step = state.scratch(
                MoldormHandlerFrameStep.class,
                frame -> new MoldormHandlerFrameStep(
                        frame.slotFrame(),
                        frame.inputs(),
                        frame.gates(),
                        frame.presentation(),
                        frame.slot(),
                        frame.sprites(),
                        frame.roomInspection(),
                        frame::replaceEntity,
                        frame::emit));
        return step.advance(state.motion(MoldormMotion.class, MoldormMotion::new));
    }

    private static boolean phase2Moldorm2(
            EntitySlotExecution slotState, EntityFrameInputLatch frameInput, EntityBossRewards bossRewards) {
        bossRewards.advanceMoldormDestruction(slotState.entity(), frameInput.frame());
        return false;
    }

    private static boolean phase2Moldorm3(EntityFrameExecution state) {
        var slotState = state.slotFrame();

        state.motion(MoldormMotion.class, MoldormMotion::new).initialize(slotState.entity());

        return true;
    }

    private record MoldormHandlerFrameStep(
            EntitySlotExecution slotState,
            EntityFrameInputLatch frameInput,
            EntityTimerGates timerGates,
            EntityPresentationFlags presentationFlags,
            EntitySlotView slotView,
            EntitySpriteViews sprites,
            EntityRoomInspection room,
            Consumer<RoomEntity> replaceEntity,
            Consumer<EntityCombatEvent> emitCombat) {
        boolean advance(MoldormMotion motion) {
            if (slotState.status() != EntityStatus.ACTIVE || slotState.wasInitializing()) return true;

            RoomEntity entity = slotState.entity();
            int slot = entity.slot();
            if (!timerGates.handlerInteractive()
                    || timerGates.countdownBlocksInteraction()
                    || slotState.moldormIntroOpenedThisFrame()) {
                replaceEntity.accept(sprites.present(
                        entity,
                        sprites.forMoldormState(
                                motion.headSpriteVariant(slot),
                                entity.x(),
                                (entity.y() - entity.z()) & 0xFF,
                                motion.currentTailSegments(slot),
                                0,
                                frameInput.frame()),
                        0));
                if (room.roomTransitionActive()) motion.resetHistory(slot);
                return false;
            }

            RoomEntityBackgroundInteraction background = frameInput.hinoxBackgroundInteraction();
            int transitionForMotion = slotView.transitionCountdown(slot);
            if (transitionForMotion != 0) {
                // Common countdowns have already ticked before dispatch.
                // The direct motion API performs the same decrement.
                transitionForMotion = (transitionForMotion + 1) & 0xFF;
            }
            MoldormMotion.Update update = motion.advance(
                    entity,
                    frameInput.frame(),
                    transitionForMotion,
                    slotView.ignoreHits(slot),
                    slotView.flashCountdown(slot),
                    slotView.health(slot),
                    frameInput.randomByteSupplier(),
                    background);
            slotState.updated(update.entity());
            slotView.setTransitionCountdown(slot, update.transitionCountdown());
            if (update.noiseRequested()) {
                emitCombat.accept(new EntityCombatEvent(
                        slot, slotState.entity().type(), 0, false, EntityCombatEvent.SoundChannel.NOISE, 0x1B));
            }

            MoldormMotion.Presentation presentation = update.presentation();
            RoomEntity updated = slotState.updated();
            slotState.updated(sprites.present(
                    updated,
                    sprites.forMoldormPresentation(
                            presentation.headSpriteVariant(),
                            presentation.headX(),
                            presentation.headY(),
                            updated.x(),
                            (updated.y() - updated.z()) & 0xFF,
                            presentation.segments(),
                            0,
                            frameInput.frame()),
                    0));
            slotView.setPhysicsFlags(slot, 0x02);
            presentationFlags.preserveMoldormPresentation(true);
            return true;
        }
    }
}
