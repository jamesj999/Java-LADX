package linksawakening.world.entity.handler;

import static linksawakening.world.entity.EntityRomConstants.ENTITY_SIDE_VIEW_POT;
import static linksawakening.world.entity.EntityRomConstants.LIFTABLE_ROCK_SMASH_MODE_ROCK;

import linksawakening.world.entity.EntityFrameExecution;
import linksawakening.world.entity.EntityHandlerRegistration;
import linksawakening.world.entity.EntityStatus;
import linksawakening.world.entity.EntityTickStage;
import linksawakening.world.entity.RoomEntity;
import linksawakening.world.entity.SideViewPotContact;
import linksawakening.world.entity.event.EntityCombatEvent;
import linksawakening.world.entity.motion.*;
import linksawakening.world.entity.motion.SideViewPotMotion;

/** ROM handler stages for SideViewPot. */
final class SideViewPotHandler {
    static void install(EntityHandlerRegistration registry) {
        registry.registerTick(
                ENTITY_SIDE_VIEW_POT,
                EntityTickStage.PHASE_1_BEFORE_PICKUP_SEQUENCE,
                0,
                SideViewPotHandler::phase1SideViewPot);
        registry.registerTick(
                ENTITY_SIDE_VIEW_POT,
                EntityTickStage.PHASE_1_BEFORE_PICKUP_SEQUENCE,
                1,
                SideViewPotHandler::phase1SideViewPot2);
        registry.registerTick(
                ENTITY_SIDE_VIEW_POT, EntityTickStage.PHASE_2_LIFTED, 0, SideViewPotHandler::phase2SideViewPot);
    }

    private static boolean phase1SideViewPot(EntityFrameExecution state) {
        var slotState = state.slotFrame();
        var frameInput = state.inputs();
        var timerGates = state.gates();

        if (slotState.status() == EntityStatus.ACTIVE
                && !state.motion(SideViewPotMotion.class, SideViewPotMotion::new)
                        .active(slotState.entity().slot())) {
            SideViewPotContact.Result contact = SideViewPotContact.Result.none();
            int effectiveLinkX = frameInput.linkEntityX();
            int effectiveLinkY = frameInput.linkEntityY();
            if (timerGates.sideViewPotLinkCollisionInteractive()) {
                contact = SideViewPotContact.resolve(
                        slotState.entity(),
                        frameInput.linkEntityX(),
                        frameInput.linkEntityY(),
                        frameInput.linkAirborne(),
                        state.link().currentLinkSpeedY());
                if (contact.collided()) {
                    state.liftThrow().pendingLinkPotContact(slotState.entity().slot(), contact);
                    if (contact.restoreFinalPositionX()) {
                        effectiveLinkX = state.link().currentLinkFinalPositionXConfigured()
                                ? state.link().currentLinkFinalPositionX()
                                : frameInput.linkEntityX();
                    }
                    if (contact.snapTop()) {
                        effectiveLinkY = contact.positionY();
                    }
                }
            }
            if (state.background().groundInteractionSideScrolling()
                    && timerGates.sideViewPotInteractive()
                    && SideViewPotContact.liftWindow(slotState.entity(), effectiveLinkX, effectiveLinkY)
                    && state.liftThrow().sideViewPotPowerBraceletHeld()
                    && state.link().linkAttackStepAnimationCountdown() == 0
                    && state.liftThrow().beginLift(slotState.entity().slot(), frameInput.romLinkDirection())) {
                state.emit(new EntityCombatEvent(
                        slotState.entity().slot(),
                        slotState.entity().type(),
                        0,
                        false,
                        EntityCombatEvent.SoundChannel.WAVE,
                        0x02));
            }
            return false;
        }
        return true;
    }

    private static boolean phase1SideViewPot2(EntityFrameExecution state) {
        var slotState = state.slotFrame();
        var frameInput = state.inputs();
        var timerGates = state.gates();

        if (slotState.status() == EntityStatus.ACTIVE
                && state.motion(SideViewPotMotion.class, SideViewPotMotion::new)
                        .active(slotState.entity().slot())) {

            if (timerGates.sideViewPotInteractive()) {
                SideViewPotMotion.Update potUpdate = state.motion(SideViewPotMotion.class, SideViewPotMotion::new)
                        .advance(slotState.entity());
                RoomEntity movedPot = potUpdate.entity();
                boolean collision = state.liftThrow()
                        .sideViewPotBackgroundCollision(
                                slotState.entity(), movedPot, frameInput.backgroundCollision(), frameInput.frame());
                if (collision) {
                    state.playerSpawns()
                            .spawnLiftableRockSmash(
                                    movedPot.x(), (movedPot.y() - movedPot.z()) & 0xFF, LIFTABLE_ROCK_SMASH_MODE_ROCK);
                    state.lifecycle().clearEntity(slotState.entity().slot());
                } else {
                    state.replaceEntity(movedPot);
                }
            }
            // ReturnIfNonInteractive_19 leaves the rendered pot and all
            // dedicated motion state untouched while Link is gated.
            return false;
        }
        return true;
    }

    private static boolean phase2SideViewPot(EntityFrameExecution state) {
        var slotState = state.slotFrame();

        if (state.liftThrow().sideViewPotLiftDelay(slotState.entity().slot())) {
            state.replaceEntity(slotState.lifted());
            return false;
        }
        return true;
    }
}
