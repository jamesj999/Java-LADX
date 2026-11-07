package linksawakening.entity;

import linksawakening.state.PlayerState;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

final class LinkStateMachineTest {
    @Test void scrollEntryPreservesBothFractionalAccumulatorsAndRecordsLanding() {
        Link link = new Link(null, null, null, null, null, null, null);
        link.linkMotion.subX = (148 << Link.SUB_PIXEL_SHIFT) + 10;
        link.linkMotion.subY = (80 << Link.SUB_PIXEL_SHIFT) + 5;
        link.setRoomScrollEntryPixelPosition(3, 80);
        assertEquals((3 << Link.SUB_PIXEL_SHIFT) + 10, link.linkMotion.subX);
        assertEquals((80 << Link.SUB_PIXEL_SHIFT) + 5, link.linkMotion.subY);
        assertEquals(link.linkMotion.subX, link.linkMotion.roomEntrySubX);
        assertEquals(link.linkMotion.subY, link.linkMotion.roomEntrySubY);
        assertEquals(11, link.roomEntryRomPositionX());
        assertEquals(96, link.roomEntryRomPositionY());
    }

    @Test void interactiveBlockIsConsumedAfterInvincibilityTicks() {
        PlayerState player = new PlayerState();
        Link link = new Link(null, null, null, null, null, player, null);
        link.setPixelPosition(64, 64);
        link.blockNextRomMotionFrame();
        player.setInvincibilityCounter(2);
        link.applyRomSpeed(16, 0);
        link.update();
        assertFalse(link.linkStateMachine.romInteractiveMotionBlocked);
        assertEquals(1, player.invincibilityCounter());
        assertEquals(64, link.pixelX());
        assertEquals(0, link.romSpeedX());
        assertTrue(link.linkMotion.forcedSpeedPending);
    }

    @Test void revolvingDoorExitsOnFrameSeventyTwoAndEmitsOneTransition() {
        Link link = new Link(null, null, null, null, null, null, null);
        link.linkStateMachine.motionState = 5;
        link.linkStateMachine.revolvingDoorFrame = 70;
        link.linkStateMachine.tickRevolvingDoor();
        assertEquals(5, link.romMotionState());
        assertFalse(link.consumeRevolvingDoorTransition());
        link.linkStateMachine.tickRevolvingDoor();
        assertEquals(Link.LINK_MOTION_DEFAULT, link.romMotionState());
        assertEquals(Link.DIRECTION_UP, link.direction());
        assertEquals(-5, link.romEntityY());
        assertTrue(link.consumeRevolvingDoorTransition());
        assertFalse(link.consumeRevolvingDoorTransition());
    }
}
