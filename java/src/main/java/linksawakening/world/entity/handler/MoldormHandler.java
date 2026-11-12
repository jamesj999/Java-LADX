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
        if (!state.wasInitializing
                && !state.roomInspection().roomTransitionActive()
                && !state.link().inventoryAppearing()) {
            BossIntroMotion.Update intro = state.bossIntroMotion()
                    .advance(
                            state.slotProperties().options1(state.entity.slot()),
                            state.entity.type(),
                            state.roomInspection().entityMapId(),
                            state.lifecycle().transitionSequenceCounter());
            if (intro.musicTrack() >= 0) state.requestMusic(intro.musicTrack());
            if (intro.dialogLowId() >= 0) {
                state.emit(new DialogRequest(intro.dialogTableId(), intro.dialogLowId()));
                state.moldormIntroOpenedThisFrame = true;
            }
        }
        return true;
    }

    private static boolean phase1Moldorm2(EntityFrameExecution state) {
        if (!state.wasInitializing
                && !state.roomInspection().roomTransitionActive()
                && !state.roomInspection().freeMovementMode()) {
            int dx = 0;
            int dy = 0;
            if (((state.linkEntityX - 0x11) & 0xFF) >= 0x7E) {
                dx = state.linkEntityX < 0x50 ? 1 : -1;
            } else if (((state.linkEntityY - 0x16) & 0xFF) >= 0x5E) {
                dy = -1;
            }
            if (dx != 0 || dy != 0) {
                state.emit(new LinkMotionBlockRequest(state.entity.slot(), dx, dy, 1));
                state.linkEntityX = (state.linkEntityX + dx) & 0xFF;
                state.linkEntityY = (state.linkEntityY + dy) & 0xFF;
                state.lifecycle().delayInteractions(2);
            }
        }
        return true;
    }

    private static boolean phase2Moldorm(EntityFrameExecution state) {
        if (state.status == EntityStatus.ACTIVE && !state.wasInitializing) {
            int slot = state.entity.slot();
            if (!state.handlerInteractive || state.countdownBlocksInteraction || state.moldormIntroOpenedThisFrame) {
                if (state.sprites() != null) {
                    state.replaceEntity(withDefinition(
                            state.entity,
                            state.sprites()
                                    .forMoldormState(
                                            state.moldormMotion().headSpriteVariant(slot),
                                            state.entity.x(),
                                            (state.entity.y() - state.entity.z()) & 0xFF,
                                            state.moldormMotion().currentTailSegments(slot),
                                            0,
                                            state.frame),
                            0));
                }
                if (state.roomInspection().roomTransitionActive())
                    state.moldormMotion().resetHistory(slot);
                return false;
            }
            RoomEntityBackgroundInteraction moldormBackgroundInteraction =
                    state.background().interaction();
            if (moldormBackgroundInteraction == null && state.backgroundCollision != null) {
                moldormBackgroundInteraction = RoomEntityBackgroundInteraction.fromBoolean(state.backgroundCollision);
            }
            int transitionForMotion = state.slot().transitionCountdown(slot);
            if (transitionForMotion != 0) {
                // Common countdowns have already ticked before dispatch;
                // the direct motion API performs the same decrement.
                transitionForMotion = (transitionForMotion + 1) & 0xFF;
            }
            MoldormMotion.Update moldormUpdate = state.moldormMotion()
                    .advance(
                            state.entity,
                            state.frame,
                            transitionForMotion,
                            state.slot().ignoreHits(slot),
                            state.slot().flashCountdown(slot),
                            state.slot().health(slot),
                            state.randomByteSupplier,
                            moldormBackgroundInteraction);
            state.updated = moldormUpdate.entity();
            state.slot().setTransitionCountdown(slot, moldormUpdate.transitionCountdown());
            if (moldormUpdate.noiseRequested()) {
                state.emit(new EntityCombatEvent(
                        slot, state.entity.type(), 0, false, EntityCombatEvent.SoundChannel.NOISE, 0x1B));
            }
            if (state.sprites() != null) {
                MoldormMotion.Presentation presentation = moldormUpdate.presentation();
                state.updated = withDefinition(
                        state.updated,
                        state.sprites()
                                .forMoldormPresentation(
                                        presentation.headSpriteVariant(),
                                        presentation.headX(),
                                        presentation.headY(),
                                        state.updated.x(),
                                        (state.updated.y() - state.updated.z()) & 0xFF,
                                        presentation.segments(),
                                        0,
                                        state.frame),
                        0);
            }
            state.slot().setPhysicsFlags(slot, 0x02);
            state.preserveMoldormPresentation = true;
        }
        return true;
    }

    private static boolean phase2Moldorm2(EntityFrameExecution state) {
        state.bossRewards().advanceMoldormDestruction(state.entity, state.frame);
        return false;
    }

    private static boolean phase2Moldorm3(EntityFrameExecution state) {
        state.moldormMotion().initialize(state.entity);

        return true;
    }
}
