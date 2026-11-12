package linksawakening.world.entity.handler;

import static linksawakening.world.entity.EntityMotionMath.sideViewPlatformOverlapsLink;

import linksawakening.world.entity.EntityFrameExecution;
import linksawakening.world.entity.EntityHandlerRegistration;
import linksawakening.world.entity.EntityStatus;
import linksawakening.world.entity.EntityTickStage;
import linksawakening.world.entity.RoomEntity;
import linksawakening.world.entity.event.EntityCombatEvent;
import linksawakening.world.entity.event.SideViewPlatformLinkRequest;
import linksawakening.world.entity.motion.*;
import linksawakening.world.entity.motion.SideViewPlatformMotion;

/** ROM handler stages for SideViewPlatform. */
final class SideViewPlatformHandler {
    static void install(EntityHandlerRegistration registry) {
        registry.registerTick(
                SideViewPlatformMotion.ENTITY_TYPE,
                EntityTickStage.PHASE_2_BEFORE_RECOIL,
                0,
                SideViewPlatformHandler::phase2Type);
    }

    private static boolean phase2Type(EntityFrameExecution state) {
        var slotState = state.slotFrame();
        var frameInput = state.inputs();
        var timerGates = state.gates();

        if (slotState.status() == EntityStatus.ACTIVE
                && !slotState.wasInitializing()
                && timerGates.handlerLinkCollisionEnabled()
                && state.background().entityTimersInteractive()
                && !frameInput.creditsGameplay()
                && state.lifecycle().transitionSequenceCounter() == 0x04) {
            SideViewPlatformMotion.Frame platformFrame = state.motion(
                            SideViewPlatformMotion.class, SideViewPlatformMotion::new)
                    .beginFrame(slotState.entity());
            RoomEntity movedPlatform = platformFrame.entity();
            if (platformFrame.horizontalDelta() != 0
                    && frameInput.backgroundCollision() != null
                    && frameInput
                            .backgroundCollision()
                            .blocks(
                                    slotState.entity(),
                                    platformFrame.horizontalDelta() < 0 ? 1 : 0,
                                    movedPlatform.x(),
                                    movedPlatform.y())) {
                platformFrame = state.motion(SideViewPlatformMotion.class, SideViewPlatformMotion::new)
                        .restoreHorizontalPosition(slotState.entity(), platformFrame);
                movedPlatform = platformFrame.entity();
            }
            if (platformFrame.verticalDelta() != 0 && frameInput.backgroundCollision() != null) {
                int direction = platformFrame.verticalDelta() < 0 ? 2 : 3;
                if (frameInput
                        .backgroundCollision()
                        .blocks(slotState.entity(), direction, movedPlatform.x(), movedPlatform.y())) {
                    platformFrame = state.motion(SideViewPlatformMotion.class, SideViewPlatformMotion::new)
                            .restoreVerticalPosition(slotState.entity(), platformFrame);
                    movedPlatform = platformFrame.entity();
                }
            }
            boolean standing = state.background().groundInteractionSideScrolling()
                    && (frameInput.linkZ() & 0xFF) == 0
                    && (state.link().currentLinkSpeedY() & 0x80) == 0
                    && sideViewPlatformOverlapsLink(movedPlatform, frameInput.linkEntityX(), frameInput.linkEntityY())
                    && (((frameInput.linkEntityY() - movedPlatform.y() + 0x08) & 0x80) != 0);
            boolean activationAllowed = state.roomInspection().entityRoomId() == 0x3B || slotState.carryingEntity();
            if (standing) {
                state.emit(new SideViewPlatformLinkRequest(
                        slotState.entity().slot(),
                        platformFrame.horizontalDelta(),
                        (movedPlatform.y() - 0x10) & 0xFF,
                        0x02,
                        true,
                        activationAllowed));
            }
            SideViewPlatformMotion.Update platformUpdate = state.motion(
                            SideViewPlatformMotion.class, SideViewPlatformMotion::new)
                    .finishFrame(movedPlatform, platformFrame, frameInput.frame(), standing, activationAllowed);
            slotState.updated(platformUpdate.entity());
            if (platformUpdate.rumble()) {
                state.emit(new EntityCombatEvent(
                        slotState.entity().slot(),
                        slotState.entity().type(),
                        0,
                        false,
                        EntityCombatEvent.SoundChannel.NOISE,
                        0x11));
            }
        }
        return true;
    }
}
