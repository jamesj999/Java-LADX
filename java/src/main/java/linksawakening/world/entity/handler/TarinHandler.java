package linksawakening.world.entity.handler;

import static linksawakening.world.entity.EntityRomConstants.ENTITY_TARIN;

import linksawakening.world.entity.EntityFrameExecution;
import linksawakening.world.entity.EntityHandlerRegistration;
import linksawakening.world.entity.EntityLinkCollisionRules;
import linksawakening.world.entity.EntityStatus;
import linksawakening.world.entity.EntityTickStage;
import linksawakening.world.entity.RoomEntityBackgroundInteraction;
import linksawakening.world.entity.RoomEntityCombatRules;
import linksawakening.world.entity.event.DialogRequest;
import linksawakening.world.entity.event.EntityCombatEvent;
import linksawakening.world.entity.event.LinkAttackClearRequest;
import linksawakening.world.entity.event.LinkFacingRequest;
import linksawakening.world.entity.event.LinkMotionBlockRequest;
import linksawakening.world.entity.event.RoomStatusPersistenceRequest;
import linksawakening.world.entity.motion.*;
import linksawakening.world.entity.motion.TarinRaccoonMotion;

/** ROM handler stages for Tarin. */
final class TarinHandler {
    static void install(EntityHandlerRegistration registry) {
        registry.registerTick(ENTITY_TARIN, EntityTickStage.PHASE_1_BEFORE_STATUS_TIMERS, 0, TarinHandler::phase1Tarin);
        registry.registerTick(ENTITY_TARIN, EntityTickStage.PHASE_4_BEFORE_FOLLOWER, 15, TarinHandler::phase4Tarin);
    }

    private static boolean phase1Tarin(EntityFrameExecution state) {
        var slotState = state.slotFrame();

        if ((state.roomInspection().indoorRoom() && state.roomInspection().indoorTarinAbsent())
                || (!state.roomInspection().indoorRoom()
                        && (state.roomInspection().entityRoomStatus() & 0x10) != 0)) {
            state.lifecycle().disableEntityWithoutPersistence(slotState.entity().slot());
            return false;
        }
        return true;
    }

    private static boolean phase4Tarin(EntityFrameExecution state) {
        var slotState = state.slotFrame();
        var frameInput = state.inputs();
        var presentationFlags = state.presentation();

        if (slotState.status() == EntityStatus.ACTIVE
                && !slotState.wasInitializing()
                && !state.roomInspection().indoorRoom()
                && (state.motion(TarinRaccoonMotion.class, TarinRaccoonMotion::new)
                                        .state(slotState.entity().slot())
                                == 0
                        || state.roomInspection().tarinTransformationHandlerInteractive())) {
            RoomEntityBackgroundInteraction tarinBackgroundInteraction =
                    state.background().interaction();
            if (tarinBackgroundInteraction == null && frameInput.backgroundCollision() != null) {
                tarinBackgroundInteraction =
                        RoomEntityBackgroundInteraction.fromBoolean(frameInput.backgroundCollision());
            }
            TarinRaccoonMotion.Update tarinUpdate = state.motion(TarinRaccoonMotion.class, TarinRaccoonMotion::new)
                    .advance(
                            slotState.updated(),
                            new TarinRaccoonMotion.Input(
                                    frameInput.frame(),
                                    frameInput.linkEntityX(),
                                    frameInput.linkEntityY(),
                                    frameInput.romLinkDirection(),
                                    state.link().actionButtonAHeld(),
                                    state.link().dialogActive(),
                                    false,
                                    state.link().linkAttackStepAnimationCountdown(),
                                    frameInput.linkAirborne(),
                                    state.link().inventoryAppearing(),
                                    state.link().dialogCooldown(),
                                    state.link().windowY(),
                                    state.slot()
                                            .slowTransitionCountdown(
                                                    slotState.entity().slot()),
                                    state.slot()
                                            .transitionCountdown(
                                                    slotState.entity().slot())),
                            tarinBackgroundInteraction);
            slotState.updated(tarinUpdate.entity());
            state.slot().setSlowTransitionCountdown(slotState.entity().slot(), tarinUpdate.slowTransitionCountdown());
            state.slot().setTransitionCountdown(slotState.entity().slot(), tarinUpdate.transitionCountdown());
            presentationFlags.preserveTarinPresentation(true);
            state.story().setShouldGetLostInMysteriousWoods(tarinUpdate.shouldGetLost());
            if (tarinUpdate.linkMotionBlocked()) {
                state.emit(new LinkMotionBlockRequest(slotState.entity().slot()));
            }
            if (tarinUpdate.dialogGlobalId() >= 0) {
                state.emit(new DialogRequest(tarinUpdate.dialogGlobalId() >>> 8, tarinUpdate.dialogGlobalId() & 0xFF));
            }
            if (tarinUpdate.clearLinkAttack()) {
                state.emit(new LinkAttackClearRequest(slotState.entity().slot()));
            }
            if (tarinUpdate.linkFacingDirection() >= 0) {
                state.emit(new LinkFacingRequest(slotState.entity().slot(), tarinUpdate.linkFacingDirection(), true));
            }
            if (tarinUpdate.pushLink()) {
                if (RoomEntityCombatRules.overlapsLink(
                        slotState.updated(), frameInput.linkEntityX(), frameInput.linkEntityY())) {
                    state.background().requestLinkPush(slotState.updated(), EntityLinkCollisionRules.STANDARD_PUSH);
                }
            }
            if (tarinUpdate.soundId() >= 0) {
                state.emit(new EntityCombatEvent(
                        slotState.entity().slot(),
                        slotState.entity().type(),
                        0,
                        false,
                        EntityCombatEvent.SoundChannel.JINGLE,
                        tarinUpdate.soundId()));
                if (tarinUpdate.soundId() == 0x23) {
                    state.requestMusic(state.roomInspection().defaultMusicTrack());
                }
            }
            if (tarinUpdate.spawnBomb()) {
                state.enemySpawns().spawnTarinTransformationBomb(slotState.updated());
            }
            if (tarinUpdate.roomChanged() || tarinUpdate.tarinFlag()) {
                state.emit(new RoomStatusPersistenceRequest(slotState.entity().slot(), 0x10, 1));
            }
        }
        return true;
    }
}
