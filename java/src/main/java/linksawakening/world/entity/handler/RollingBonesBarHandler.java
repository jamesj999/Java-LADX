package linksawakening.world.entity.handler;

import static linksawakening.world.entity.EntityRomConstants.ENTITY_ROLLING_BONES;
import static linksawakening.world.entity.EntityRomConstants.ENTITY_ROLLING_BONES_BAR;
import static linksawakening.world.entity.EntitySnapshots.withDefinition;

import linksawakening.world.entity.EntityFrameExecution;
import linksawakening.world.entity.EntityHandlerRegistration;
import linksawakening.world.entity.EntityStatus;
import linksawakening.world.entity.EntityTickStage;
import linksawakening.world.entity.event.EntityCombatEvent;
import linksawakening.world.entity.event.ScreenShakeRequest;
import linksawakening.world.entity.motion.*;
import linksawakening.world.entity.motion.BossIntroMotion;
import linksawakening.world.entity.motion.RollingBonesMotion;

/** ROM handler stages for RollingBonesBar. */
final class RollingBonesBarHandler {
    static void install(EntityHandlerRegistration registry) {
        registry.registerTick(
                ENTITY_ROLLING_BONES,
                EntityTickStage.PHASE_2_BEFORE_RECOIL_APPLICATION,
                9,
                RollingBonesBarHandler::phase2RollingBonesBar);
        registry.registerTick(
                ENTITY_ROLLING_BONES_BAR,
                EntityTickStage.PHASE_2_BEFORE_RECOIL_APPLICATION,
                9,
                RollingBonesBarHandler::phase2RollingBonesBar);
        registry.registerTick(
                ENTITY_ROLLING_BONES_BAR,
                EntityTickStage.PHASE_4_BEFORE_FOLLOWER,
                4,
                RollingBonesBarHandler::phase4RollingBonesBar);
    }

    private static boolean phase2RollingBonesBar(EntityFrameExecution state) {
        var slotState = state.slotFrame();
        var frameInput = state.inputs();
        var timerGates = state.gates();

        if (slotState.status() == EntityStatus.ACTIVE && !slotState.wasInitializing()) {
            // BossIntro runs before ReturnIfNonInteractive_06.
            if (slotState.entity().type() == ENTITY_ROLLING_BONES
                    && !state.roomInspection().roomTransitionActive()
                    && !state.link().inventoryAppearing()) {
                BossIntroMotion.Update intro = state.motion(BossIntroMotion.class, BossIntroMotion::new)
                        .advance(
                                state.slotProperties()
                                        .options1(slotState.entity().slot()),
                                slotState.entity().type(),
                                state.roomInspection().entityMapId(),
                                state.lifecycle().transitionSequenceCounter());
                if (intro.musicTrack() >= 0) state.requestMusic(intro.musicTrack());
            }
            // The bar's room-clear check precedes its interaction guard.
            if (slotState.entity().type() == ENTITY_ROLLING_BONES_BAR
                    && (state.roomInspection().entityRoomStatus() & 0x20) != 0) {
                state.lifecycle()
                        .disableEntityWithoutPersistence(slotState.entity().slot());
                return false;
            }
            // ReturnIfNonInteractive_06 reads the pre-decrement wC1A8.
            if (!timerGates.handlerInteractive() || timerGates.countdownBlocksInteraction()) return false;
            if (slotState.entity().type() == ENTITY_ROLLING_BONES
                    && !slotState.rollingBonesHitThisFrame()
                    && state.slot().ignoreHits(slotState.entity().slot()) != 0) {
                // ApplyRecoilIfNeeded_06 pops the handler return address:
                // no jump physics or AI movement occurs during recoil.
                state.replaceEntity(state.background()
                        .applyEnemyRecoilIfNeeded(slotState.entity(), frameInput.backgroundCollision())
                        .entity());
                return false;
            }
        }
        return true;
    }

    private static boolean phase4RollingBonesBar(EntityFrameExecution state) {
        var slotState = state.slotFrame();
        var frameInput = state.inputs();
        var presentationFlags = state.presentation();

        if (slotState.status() == EntityStatus.ACTIVE && !slotState.wasInitializing()) {
            if ((state.roomInspection().entityRoomStatus() & 0x20) != 0) {
                state.lifecycle()
                        .disableEntityWithoutPersistence(slotState.entity().slot());
                return false;
            }
            RollingBonesMotion.BarUpdate barUpdate = state.motion(RollingBonesMotion.class, RollingBonesMotion::new)
                    .advanceBar(slotState.entity(), frameInput.frame(), frameInput.backgroundCollision());
            slotState.updated(barUpdate.entity());
            if (state.sprites() != null) {
                slotState.updated(withDefinition(
                        slotState.updated(),
                        state.sprites()
                                .forRollingBonesBar(0, slotState.updated().y(), barUpdate.animationVariant()),
                        0));
            }
            presentationFlags.preserveRollingBonesPresentation(true);
            if (barUpdate.rollingSound()) {
                state.emit(new EntityCombatEvent(
                        slotState.entity().slot(),
                        slotState.entity().type(),
                        0,
                        false,
                        EntityCombatEvent.SoundChannel.WAVE,
                        0x1A));
            }
            if (barUpdate.strongBump()) {
                state.emit(new EntityCombatEvent(
                        slotState.entity().slot(),
                        slotState.entity().type(),
                        0,
                        false,
                        EntityCombatEvent.SoundChannel.JINGLE,
                        0x0B));
                state.emit(new ScreenShakeRequest(slotState.entity().slot(), barUpdate.screenShakeCountdown(), 0));
            }
        }
        return true;
    }
}
