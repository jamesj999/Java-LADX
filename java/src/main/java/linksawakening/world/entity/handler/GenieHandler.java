package linksawakening.world.entity.handler;

import static linksawakening.world.entity.EntityRomConstants.ENTITY_GENIE;

import java.util.Objects;
import linksawakening.world.entity.EntityFrameExecution;
import linksawakening.world.entity.EntityFrameInputLatch;
import linksawakening.world.entity.EntityHandlerRegistry;
import linksawakening.world.entity.EntityLinkCollisionRules;
import linksawakening.world.entity.EntitySlotExecution;
import linksawakening.world.entity.EntityStatus;
import linksawakening.world.entity.EntityTickStage;
import linksawakening.world.entity.GenieEntityController;
import linksawakening.world.entity.RoomEntityCombatRules;
import linksawakening.world.entity.event.DialogRequest;
import linksawakening.world.entity.motion.BossIntroMotion;
import linksawakening.world.entity.motion.GenieMotion;

/** ROM handler stages for Genie. */
final class GenieHandler {
    static void install(EntityHandlerRegistry registry) {
        registry.registerTick(
                ENTITY_GENIE,
                EntityTickStage.PHASE_1_AFTER_STATUS_TIMERS,
                0,
                frame -> phase1Genie(frame.slotFrame(), frame.genie()));
        registry.registerTick(
                ENTITY_GENIE,
                EntityTickStage.PHASE_1_AFTER_STATUS_TIMERS,
                1,
                frame -> phase1Genie2(frame.slotFrame(), frame.genie()));
        registry.registerTick(
                ENTITY_GENIE,
                EntityTickStage.PHASE_2_DYING,
                1,
                frame -> phase2Genie(frame.slotFrame(), frame.inputs(), frame.genie()));
        registry.registerTick(ENTITY_GENIE, EntityTickStage.PHASE_4_BEFORE_FOLLOWER, 0, GenieHandler::phase4Genie);
    }

    private static boolean phase1Genie(EntitySlotExecution slotState, GenieEntityController genie) {
        if (genie.phaseCountdown(slotState.entity().slot()) > 0) {
            genie.decrementPhaseCountdown(slotState.entity().slot());
        }
        return true;
    }

    private static boolean phase1Genie2(EntitySlotExecution slotState, GenieEntityController genie) {
        if (genie.attackFlashCountdown(slotState.entity().slot()) > 0) {
            genie.decrementAttackFlashCountdown(slotState.entity().slot());
        }
        return true;
    }

    private static boolean phase2Genie(
            EntitySlotExecution slotState, EntityFrameInputLatch frameInput, GenieEntityController genie) {
        if (genie.phase(slotState.entity().slot()) == 1) {
            genie.advanceGenieDestruction(slotState.entity(), frameInput.frame(), frameInput.randomByteSupplier());
            return false;
        }
        return true;
    }

    private static boolean phase4Genie(EntityFrameExecution state) {
        return state.scratch(GenieHandlerFrameStep.class, GenieHandlerFrameStep::new)
                .execute();
    }

    private static final class GenieHandlerFrameStep {
        // Owning frame execution is stable for this cached scratch holder.
        private final linksawakening.world.entity.EntityFrameExecution state;
        // Absent before initialization or while this reusable execution scratch is inactive.
        private linksawakening.world.entity.EntitySlotExecution slotState;
        // Absent before initialization or while this reusable execution scratch is inactive.
        private linksawakening.world.entity.EntityFrameInputLatch frameInput;
        // Absent before initialization or while this reusable execution scratch is inactive.
        private linksawakening.world.entity.EntityTimerGates timerGates;

        GenieHandlerFrameStep(linksawakening.world.entity.EntityFrameExecution state) {
            this.state = Objects.requireNonNull(state, "state");
        }

        boolean run() {
            if (!advanceIntroAndJar()) return completedResult;
            if (!advanceBodyOrFireball()) return completedResult;

            return true;
        }

        // Absent before initialization or while this reusable execution scratch is inactive.
        private linksawakening.world.entity.motion.BossIntroMotion.Update bossIntro;
        private boolean genieThresholdReached;

        private boolean completedResult;

        private boolean advanceIntroAndJar() {
            slotState = state.slotFrame();
            frameInput = state.inputs();
            timerGates = state.gates();

            if (!(slotState.status() == EntityStatus.ACTIVE && !slotState.wasInitializing())) {
                completedResult = true;
                return false;
            }

            boolean introCanAdvance = !state.link().inventoryAppearing()
                    && !state.roomInspection().roomTransitionActive();
            bossIntro = introCanAdvance
                    ? state.motion(BossIntroMotion.class, BossIntroMotion::new)
                            .advance(
                                    state.slotProperties()
                                            .options1(slotState.entity().slot()),
                                    slotState.entity().type(),
                                    state.roomInspection().entityMapId(),
                                    state.lifecycle().transitionSequenceCounter())
                    : BossIntroMotion.Update.none();
            if (bossIntro.musicTrack() >= 0) {
                state.requestMusic(bossIntro.musicTrack());
            }
            if (bossIntro.dialogLowId() >= 0) {
                state.emit(new DialogRequest(bossIntro.dialogTableId(), bossIntro.dialogLowId()));
            }
            genieThresholdReached =
                    state.genie().jarDamageCounter(slotState.entity().slot()) >= GenieMotion.JAR_HEALTH_THRESHOLD;
            if (state.genie().phase(slotState.entity().slot()) == 0 && !genieThresholdReached) {
                state.genie().setGenieControllerSlot(slotState.entity().slot());
            }
            if (state.genie().phase(slotState.entity().slot()) == 0
                    && (genieThresholdReached
                            || state.roomInspection().genieState0Interactive(frameInput.creditsGameplay()))) {
                state.genie().advanceGenieJar(slotState.entity(), frameInput.linkEntityY());
                if (!state.entityAt(slotState.entity().slot()).loaded()) {
                    completedResult = false;
                    return false;
                }
                slotState.updated(state.entityAt(slotState.entity().slot()));
            }
            if (state.genie().phase(slotState.entity().slot()) == 2
                    && state.roomInspection().genieState0Interactive(frameInput.creditsGameplay())) {
                state.genie().advanceGenieAppearance(slotState.entity());
                if (!state.entityAt(slotState.entity().slot()).loaded()) {
                    completedResult = false;
                    return false;
                }
                slotState.updated(state.entityAt(slotState.entity().slot()));
            }
            return true;
        }

        private boolean advanceBodyOrFireball() {
            if (state.genie().phase(slotState.entity().slot()) == 3
                    && state.roomInspection().genieState0Interactive(frameInput.creditsGameplay())) {
                state.genie().advanceGenieSideProjectile(slotState.entity(), frameInput.frameCounter());
                if (!state.entityAt(slotState.entity().slot()).loaded()) {
                    completedResult = false;
                    return false;
                }
                slotState.updated(state.entityAt(slotState.entity().slot()));
            }
            if (state.genie().phase(slotState.entity().slot()) == 4
                    && state.roomInspection().genieState0Interactive(frameInput.creditsGameplay())) {
                if (timerGates.handlerLinkCollisionEnabled()
                        && RoomEntityCombatRules.overlapsLink(
                                slotState.entity(), frameInput.linkEntityX(), frameInput.linkEntityY())) {
                    state.background().requestLinkPush(slotState.entity(), EntityLinkCollisionRules.STANDARD_PUSH);
                }
                state.genie().advanceGenieFireball(slotState.entity(), frameInput.frameCounter());
                if (!state.entityAt(slotState.entity().slot()).loaded()) {
                    completedResult = false;
                    return false;
                }
                slotState.updated(state.entityAt(slotState.entity().slot()));
            }
            if (state.genie().phase(slotState.entity().slot()) == 1
                    && state.roomInspection().genieState0Interactive(frameInput.creditsGameplay())) {
                state.genie()
                        .setGenieSharedBodySpeedX(
                                state.genie().genieSpeedX(slotState.entity().slot()) & 0xFF);
                state.genie()
                        .setGenieSharedBodySpeedY(
                                state.genie().genieSpeedY(slotState.entity().slot()) & 0xFF);
                if (state.genie().genieActiveState(slotState.entity().slot()) != 5
                        && timerGates.handlerLinkCollisionEnabled()
                        && RoomEntityCombatRules.overlapsLink(
                                slotState.entity(), frameInput.linkEntityX(), frameInput.linkEntityY())) {
                    state.background().requestLinkPush(slotState.entity(), EntityLinkCollisionRules.STANDARD_PUSH);
                }
                state.genie()
                        .advanceGenieBody(
                                slotState.entity(),
                                frameInput.frameCounter(),
                                frameInput.linkEntityX(),
                                frameInput.linkEntityY(),
                                frameInput.randomByteSupplier());
                if (!state.entityAt(slotState.entity().slot()).loaded()) {
                    completedResult = false;
                    return false;
                }
                slotState.updated(state.entityAt(slotState.entity().slot()));
            }
            return true;
        }

        private boolean inUse;

        boolean execute() {
            if (inUse) {
                return new GenieHandlerFrameStep(state).execute();
            }
            inUse = true;
            slotState = null;
            frameInput = null;
            timerGates = null;
            bossIntro = null;
            genieThresholdReached = false;
            completedResult = false;

            try {
                return run();
            } finally {
                inUse = false;

                this.slotState = null;
                this.frameInput = null;
                this.timerGates = null;
                this.bossIntro = null;
            }
        }
    }
}
