package linksawakening.world.entity.handler;

import static linksawakening.world.entity.EntityRomConstants.ENTITY_GENIE;

import linksawakening.world.entity.EntityFrameExecution;
import linksawakening.world.entity.EntityHandlerRegistration;
import linksawakening.world.entity.EntityLinkCollisionRules;
import linksawakening.world.entity.EntityStatus;
import linksawakening.world.entity.EntityTickStage;
import linksawakening.world.entity.RoomEntityCombatRules;
import linksawakening.world.entity.event.DialogRequest;
import linksawakening.world.entity.motion.*;
import linksawakening.world.entity.motion.BossIntroMotion;
import linksawakening.world.entity.motion.GenieMotion;

/** ROM handler stages for Genie. */
final class GenieHandler {
    static void install(EntityHandlerRegistration registry) {
        registry.registerTick(ENTITY_GENIE, EntityTickStage.PHASE_1_AFTER_STATUS_TIMERS, 0, GenieHandler::phase1Genie);
        registry.registerTick(ENTITY_GENIE, EntityTickStage.PHASE_1_AFTER_STATUS_TIMERS, 1, GenieHandler::phase1Genie2);
        registry.registerTick(ENTITY_GENIE, EntityTickStage.PHASE_2_DYING, 1, GenieHandler::phase2Genie);
        registry.registerTick(ENTITY_GENIE, EntityTickStage.PHASE_4_BEFORE_FOLLOWER, 0, GenieHandler::phase4Genie);
    }

    private static boolean phase1Genie(EntityFrameExecution state) {
        var slotState = state.slotFrame();

        if (state.genie().geniePrivateCountdown1(slotState.entity().slot()) > 0) {
            state.genie().decrementGeniePrivateCountdown1(slotState.entity().slot());
        }
        return true;
    }

    private static boolean phase1Genie2(EntityFrameExecution state) {
        var slotState = state.slotFrame();

        if (state.genie().geniePrivateCountdown2(slotState.entity().slot()) > 0) {
            state.genie().decrementGeniePrivateCountdown2(slotState.entity().slot());
        }
        return true;
    }

    private static boolean phase2Genie(EntityFrameExecution state) {
        var slotState = state.slotFrame();
        var frameInput = state.inputs();

        if (state.genie().geniePrivateState1(slotState.entity().slot()) == 1) {
            state.genie()
                    .advanceGenieDestruction(slotState.entity(), frameInput.frame(), frameInput.randomByteSupplier());
            return false;
        }
        return true;
    }

    private static boolean phase4Genie(EntityFrameExecution state) {
        var slotState = state.slotFrame();
        var frameInput = state.inputs();
        var timerGates = state.gates();

        if (slotState.status() == EntityStatus.ACTIVE && !slotState.wasInitializing()) {
            BossIntroMotion.Update bossIntro = !state.link().inventoryAppearing()
                            && !state.roomInspection().roomTransitionActive()
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
            boolean genieThresholdReached =
                    state.genie().geniePrivateState4(slotState.entity().slot()) >= GenieMotion.JAR_HEALTH_THRESHOLD;
            if (state.genie().geniePrivateState1(slotState.entity().slot()) == 0 && !genieThresholdReached) {
                state.genie().setGenieControllerSlot(slotState.entity().slot());
            }
            if (state.genie().geniePrivateState1(slotState.entity().slot()) == 0
                    && (genieThresholdReached
                            || state.roomInspection().genieState0Interactive(frameInput.creditsGameplay()))) {
                state.genie().advanceGenieJar(slotState.entity(), frameInput.linkEntityY());
                if (!state.entityAt(slotState.entity().slot()).loaded()) {
                    return false;
                }
                slotState.updated(state.entityAt(slotState.entity().slot()));
            }
            if (state.genie().geniePrivateState1(slotState.entity().slot()) == 2
                    && state.roomInspection().genieState0Interactive(frameInput.creditsGameplay())) {
                state.genie().advanceGenieAppearance(slotState.entity());
                if (!state.entityAt(slotState.entity().slot()).loaded()) {
                    return false;
                }
                slotState.updated(state.entityAt(slotState.entity().slot()));
            }
            if (state.genie().geniePrivateState1(slotState.entity().slot()) == 3
                    && state.roomInspection().genieState0Interactive(frameInput.creditsGameplay())) {
                state.genie().advanceGenieSideProjectile(slotState.entity(), frameInput.frameCounter());
                if (!state.entityAt(slotState.entity().slot()).loaded()) {
                    return false;
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
                    return false;
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
                    return false;
                }
                slotState.updated(state.entityAt(slotState.entity().slot()));
            }
        }
        return true;
    }
}
