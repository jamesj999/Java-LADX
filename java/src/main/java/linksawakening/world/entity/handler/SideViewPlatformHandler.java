package linksawakening.world.entity.handler;

import static linksawakening.world.entity.EntityMotionMath.sideViewPlatformOverlapsLink;

import linksawakening.world.entity.EntityFrameExecution;
import linksawakening.world.entity.EntityHandlerRegistry;
import linksawakening.world.entity.EntityStatus;
import linksawakening.world.entity.EntityTickStage;
import linksawakening.world.entity.RoomEntity;
import linksawakening.world.entity.event.EntityCombatEvent;
import linksawakening.world.entity.event.SideViewPlatformLinkRequest;
import linksawakening.world.entity.motion.SideViewPlatformMotion;

/** ROM handler stages for SideViewPlatform. */
final class SideViewPlatformHandler {
    static void install(EntityHandlerRegistry registry) {
        registry.registerTick(
                SideViewPlatformMotion.ENTITY_TYPE,
                EntityTickStage.PHASE_2_BEFORE_RECOIL,
                0,
                SideViewPlatformHandler::phase2Type);
    }

    private static boolean phase2Type(EntityFrameExecution state) {
        var resolvedSideViewPlatformMotion = state.motion(SideViewPlatformMotion.class, SideViewPlatformMotion::new);

        var slotState = state.slotFrame();
        var frameInput = state.inputs();
        var timerGates = state.gates();

        if (slotState.status() == EntityStatus.ACTIVE
                && !slotState.wasInitializing()
                && timerGates.handlerLinkCollisionEnabled()
                && state.background().entityTimersInteractive()
                && !frameInput.creditsGameplay()
                && state.lifecycle().transitionSequenceCounter() == 0x04) {
            SideViewPlatformMotion.Frame platformFrame = resolvedSideViewPlatformMotion.beginFrame(slotState.entity());
            RoomEntity movedPlatform = platformFrame.entity();
            if (platformFrame.horizontalDelta() != 0
                    && frameInput
                            .backgroundCollision()
                            .blocks(
                                    slotState.entity(),
                                    platformFrame.horizontalDelta() < 0 ? 1 : 0,
                                    movedPlatform.x(),
                                    movedPlatform.y())) {
                platformFrame =
                        resolvedSideViewPlatformMotion.restoreHorizontalPosition(slotState.entity(), platformFrame);
                movedPlatform = platformFrame.entity();
            }
            if (platformFrame.verticalDelta() != 0) {
                int direction = platformFrame.verticalDelta() < 0 ? 2 : 3;
                if (frameInput
                        .backgroundCollision()
                        .blocks(slotState.entity(), direction, movedPlatform.x(), movedPlatform.y())) {
                    platformFrame =
                            resolvedSideViewPlatformMotion.restoreVerticalPosition(slotState.entity(), platformFrame);
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
            SideViewPlatformMotion.Update platformUpdate = resolvedSideViewPlatformMotion.finishFrame(
                    movedPlatform, platformFrame, frameInput.frame(), standing, activationAllowed);
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
