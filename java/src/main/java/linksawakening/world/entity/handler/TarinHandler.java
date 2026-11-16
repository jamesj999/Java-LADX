package linksawakening.world.entity.handler;

import static linksawakening.world.entity.EntityRomConstants.ENTITY_TARIN;

import linksawakening.world.entity.EntityFrameExecution;
import linksawakening.world.entity.EntityHandlerRegistry;
import linksawakening.world.entity.EntityLinkCollisionRules;
import linksawakening.world.entity.EntityRoomInspection;
import linksawakening.world.entity.EntitySlotExecution;
import linksawakening.world.entity.EntitySlotLifecycle;
import linksawakening.world.entity.EntityStatus;
import linksawakening.world.entity.EntityTickStage;
import linksawakening.world.entity.RoomEntityCombatRules;
import linksawakening.world.entity.event.DialogRequest;
import linksawakening.world.entity.event.EntityCombatEvent;
import linksawakening.world.entity.event.LinkAttackClearRequest;
import linksawakening.world.entity.event.LinkFacingRequest;
import linksawakening.world.entity.event.LinkMotionBlockRequest;
import linksawakening.world.entity.event.RoomStatusPersistenceRequest;
import linksawakening.world.entity.motion.TarinRaccoonMotion;

/** ROM handler stages for Tarin. */
final class TarinHandler {
    static void install(EntityHandlerRegistry registry) {
        registry.registerTick(
                ENTITY_TARIN,
                EntityTickStage.PHASE_1_BEFORE_STATUS_TIMERS,
                0,
                frame -> phase1Tarin(frame.slotFrame(), frame.roomInspection(), frame.lifecycle()));
        registry.registerTick(ENTITY_TARIN, EntityTickStage.PHASE_4_BEFORE_FOLLOWER, 15, TarinHandler::phase4Tarin);
    }

    private static boolean phase1Tarin(
            EntitySlotExecution slotState, EntityRoomInspection roomInspection, EntitySlotLifecycle lifecycle) {
        if ((roomInspection.indoorRoom() && roomInspection.indoorTarinAbsent())
                || (!roomInspection.indoorRoom() && (roomInspection.entityRoomStatus() & 0x10) != 0)) {
            lifecycle.disableEntityWithoutPersistence(slotState.entity().slot());
            return false;
        }
        return true;
    }

    private static boolean phase4Tarin(EntityFrameExecution state) {
        return state.scratch(TarinHandlerFrameStep.class, TarinHandlerFrameStep::new)
                .execute();
    }

    private static final class TarinHandlerFrameStep {
        // Owning frame execution is stable for this cached scratch holder.
        private final linksawakening.world.entity.EntityFrameExecution state;
        // Absent before initialization or while this reusable execution scratch is inactive.
        private linksawakening.world.entity.EntitySlotExecution slotState;
        // Absent before initialization or while this reusable execution scratch is inactive.
        private linksawakening.world.entity.EntityFrameInputLatch frameInput;
        // Absent before initialization or while this reusable execution scratch is inactive.
        private linksawakening.world.entity.EntityPresentationFlags presentationFlags;

        TarinHandlerFrameStep(linksawakening.world.entity.EntityFrameExecution state) {
            this.state = java.util.Objects.requireNonNull(state, "state");
        }

        boolean run() {
            if (!advanceMotion()) return completedResult;
            if (!emitStoryEffects()) return completedResult;

            return true;
        }

        private linksawakening.world.entity.RoomEntityBackgroundInteraction tarinBackgroundInteraction;
        // Absent before initialization or while this reusable execution scratch is inactive.
        private linksawakening.world.entity.motion.TarinRaccoonMotion.Update tarinUpdate;

        private boolean completedResult;

        private boolean advanceMotion() {
            slotState = state.slotFrame();
            frameInput = state.inputs();
            presentationFlags = state.presentation();

            if (!(slotState.status() == EntityStatus.ACTIVE
                    && !slotState.wasInitializing()
                    && !state.roomInspection().indoorRoom()
                    && (state.motion(TarinRaccoonMotion.class, TarinRaccoonMotion::new)
                                            .state(slotState.entity().slot())
                                    == 0
                            || state.roomInspection().tarinTransformationHandlerInteractive()))) {
                completedResult = true;
                return false;
            }

            tarinBackgroundInteraction = frameInput.hinoxBackgroundInteraction();

            tarinUpdate = state.motion(TarinRaccoonMotion.class, TarinRaccoonMotion::new)
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
            return true;
        }

        private boolean emitStoryEffects() {
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
            return true;
        }

        private boolean inUse;

        boolean execute() {
            if (inUse) {
                return new TarinHandlerFrameStep(state).execute();
            }
            inUse = true;
            slotState = null;
            frameInput = null;
            presentationFlags = null;
            tarinBackgroundInteraction = null;
            tarinUpdate = null;
            completedResult = false;

            try {
                return run();
            } finally {
                inUse = false;

                this.slotState = null;
                this.frameInput = null;
                this.presentationFlags = null;
                this.tarinBackgroundInteraction = null;
                this.tarinUpdate = null;
            }
        }
    }
}
