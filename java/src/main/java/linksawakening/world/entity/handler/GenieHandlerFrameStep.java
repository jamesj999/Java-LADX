package linksawakening.world.entity.handler;

import linksawakening.world.entity.EntityLinkCollisionRules;
import linksawakening.world.entity.EntityStatus;
import linksawakening.world.entity.RoomEntityCombatRules;
import linksawakening.world.entity.event.DialogRequest;
import linksawakening.world.entity.motion.*;
import linksawakening.world.entity.motion.BossIntroMotion;
import linksawakening.world.entity.motion.GenieMotion;

/** Reused, reentrancy-safe scratch for GenieHandler.phase4Genie; values are assigned in source order. */
final class GenieHandlerFrameStep {
    private linksawakening.world.entity.EntityFrameExecution state;
    private linksawakening.world.entity.EntitySlotExecution slotState;
    private linksawakening.world.entity.EntityFrameInputs frameInput;
    private linksawakening.world.entity.EntityTimerGates timerGates;

    GenieHandlerFrameStep() {}

    boolean run() {
        if (!advanceIntroAndJar()) return completedResult;
        if (!advanceBodyOrFireball()) return completedResult;

        return true;
    }

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

        bossIntro =
                !state.link().inventoryAppearing() && !state.roomInspection().roomTransitionActive()
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
                state.genie().geniePrivateState4(slotState.entity().slot()) >= GenieMotion.JAR_HEALTH_THRESHOLD;
        if (state.genie().geniePrivateState1(slotState.entity().slot()) == 0 && !genieThresholdReached) {
            state.genie().setGenieControllerSlot(slotState.entity().slot());
        }
        if (state.genie().geniePrivateState1(slotState.entity().slot()) == 0
                && (genieThresholdReached
                        || state.roomInspection().genieState0Interactive(frameInput.creditsGameplay()))) {
            state.genie().advanceGenieJar(slotState.entity(), frameInput.linkEntityY());
            if (!state.entityAt(slotState.entity().slot()).loaded()) {
                {
                    completedResult = false;
                    return false;
                }
            }
            slotState.updated(state.entityAt(slotState.entity().slot()));
        }
        if (state.genie().geniePrivateState1(slotState.entity().slot()) == 2
                && state.roomInspection().genieState0Interactive(frameInput.creditsGameplay())) {
            state.genie().advanceGenieAppearance(slotState.entity());
            if (!state.entityAt(slotState.entity().slot()).loaded()) {
                {
                    completedResult = false;
                    return false;
                }
            }
            slotState.updated(state.entityAt(slotState.entity().slot()));
        }
        return true;
    }

    private boolean advanceBodyOrFireball() {

        if (state.genie().geniePrivateState1(slotState.entity().slot()) == 3
                && state.roomInspection().genieState0Interactive(frameInput.creditsGameplay())) {
            state.genie().advanceGenieSideProjectile(slotState.entity(), frameInput.frameCounter());
            if (!state.entityAt(slotState.entity().slot()).loaded()) {
                {
                    completedResult = false;
                    return false;
                }
            }
            slotState.updated(state.entityAt(slotState.entity().slot()));
        }
        if (state.genie().geniePrivateState1(slotState.entity().slot()) == 4
                && state.roomInspection().genieState0Interactive(frameInput.creditsGameplay())) {
            if (timerGates.handlerLinkCollisionEnabled()
                    && RoomEntityCombatRules.overlapsLink(
                            slotState.entity(), frameInput.linkEntityX(), frameInput.linkEntityY())) {
                state.background().requestLinkPush(slotState.entity(), EntityLinkCollisionRules.STANDARD_PUSH);
            }
            state.genie().advanceGenieFireball(slotState.entity(), frameInput.frameCounter());
            if (!state.entityAt(slotState.entity().slot()).loaded()) {
                {
                    completedResult = false;
                    return false;
                }
            }
            slotState.updated(state.entityAt(slotState.entity().slot()));
        }
        if (state.genie().geniePrivateState1(slotState.entity().slot()) == 1
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
                {
                    completedResult = false;
                    return false;
                }
            }
            slotState.updated(state.entityAt(slotState.entity().slot()));
        }
        return true;
    }

    private boolean inUse;

    boolean run(linksawakening.world.entity.EntityFrameExecution state) {
        if (inUse) {
            return new GenieHandlerFrameStep().run(state);
        }
        inUse = true;
        slotState = null;
        frameInput = null;
        timerGates = null;
        bossIntro = null;
        genieThresholdReached = false;
        completedResult = false;
        this.state = state;
        try {
            return run();
        } finally {
            inUse = false;
            this.state = null;
            this.slotState = null;
            this.frameInput = null;
            this.timerGates = null;
            this.bossIntro = null;
        }
    }
}
