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
        if (state.status == EntityStatus.ACTIVE && !state.sideViewPotMotion().active(state.entity.slot())) {
            SideViewPotContact.Result contact = SideViewPotContact.Result.none();
            int effectiveLinkX = state.linkEntityX;
            int effectiveLinkY = state.linkEntityY;
            if (state.sideViewPotLinkCollisionInteractive) {
                contact = SideViewPotContact.resolve(
                        state.entity,
                        state.linkEntityX,
                        state.linkEntityY,
                        state.linkAirborne,
                        state.link().currentLinkSpeedY());
                if (contact.collided()) {
                    state.liftThrow().pendingLinkPotContact(state.entity.slot(), contact);
                    if (contact.restoreFinalPositionX()) {
                        effectiveLinkX = state.link().currentLinkFinalPositionXConfigured()
                                ? state.link().currentLinkFinalPositionX()
                                : state.linkEntityX;
                    }
                    if (contact.snapTop()) {
                        effectiveLinkY = contact.positionY();
                    }
                }
            }
            if (state.background().groundInteractionSideScrolling()
                    && state.sideViewPotInteractive
                    && SideViewPotContact.liftWindow(state.entity, effectiveLinkX, effectiveLinkY)
                    && state.liftThrow().sideViewPotPowerBraceletHeld()
                    && state.link().linkAttackStepAnimationCountdown() == 0
                    && state.liftThrow().beginLift(state.entity.slot(), state.romLinkDirection)) {
                state.emit(new EntityCombatEvent(
                        state.entity.slot(), state.entity.type(), 0, false, EntityCombatEvent.SoundChannel.WAVE, 0x02));
            }
            return false;
        }
        return true;
    }

    private static boolean phase1SideViewPot2(EntityFrameExecution state) {
        if (state.status == EntityStatus.ACTIVE && state.sideViewPotMotion().active(state.entity.slot())) {

            if (state.sideViewPotInteractive) {
                SideViewPotMotion.Update potUpdate = state.sideViewPotMotion().advance(state.entity);
                RoomEntity movedPot = potUpdate.entity();
                boolean collision = state.liftThrow()
                        .sideViewPotBackgroundCollision(state.entity, movedPot, state.backgroundCollision, state.frame);
                if (collision) {
                    state.playerSpawns()
                            .spawnLiftableRockSmash(
                                    movedPot.x(), (movedPot.y() - movedPot.z()) & 0xFF, LIFTABLE_ROCK_SMASH_MODE_ROCK);
                    state.lifecycle().clearEntity(state.entity.slot());
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
        if (state.liftThrow().sideViewPotLiftDelay(state.entity.slot())) {
            state.replaceEntity(state.lifted);
            return false;
        }
        return true;
    }
}
