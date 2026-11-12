package linksawakening.entity;

import static org.junit.jupiter.api.Assertions.*;

import linksawakening.state.LinkMotionState;
import linksawakening.state.PlayerState;
import org.junit.jupiter.api.Test;

final class LinkStateMachineTest {
    @Test
    void spikesReverseSpeedBufferOneHeartAndGateRepeatedHits() {
        PlayerState player = new PlayerState();
        Link link = new Link(null, null, null, null, null, player, null);
        link.linkMotion.lastRomSpeedX = 0x14;
        link.linkMotion.lastRomSpeedY = 0xF1;
        link.linkDamage.hurtBySpikes();
        assertEquals(0xEC, link.romSpeedX());
        assertEquals(0x0F, link.romSpeedY());
        assertTrue(link.linkAirborne.airborne);
        assertEquals(0x10, link.linkAirborne.zVelocity);
        assertEquals(2 << Link.SUB_PIXEL_SHIFT, link.linkAirborne.zSubPixels);
        assertEquals(0x10, link.linkDamage.collisionIgnoreFramesRemaining);
        assertEquals(0x30, player.invincibilityCounter());
        assertEquals(4, player.subtractHealthBuffer());
        link.linkDamage.hurtBySpikes();
        assertEquals(4, player.subtractHealthBuffer());
        assertEquals(0xEC, link.romSpeedX());
    }

    @Test
    void sideViewSpikesPreserveVerticalJumpRegisters() {
        PlayerState player = new PlayerState();
        Link link = new Link(null, null, null, null, null, player, null);
        link.linkSideView.sideScrolling = true;
        link.linkAirborne.zVelocity = 7;
        link.linkDamage.hurtBySpikes();
        assertEquals(7, link.linkAirborne.zVelocity);
        assertEquals(0, link.linkAirborne.zSubPixels);
        assertEquals(4, player.subtractHealthBuffer());
    }

    @Test
    void scrollEntryPreservesBothFractionalAccumulatorsAndRecordsLanding() {
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

    @Test
    void interactiveBlockIsConsumedAfterInvincibilityTicks() {
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

    @Test
    void revolvingDoorExitsOnFrameSeventyTwoAndEmitsOneTransition() {
        Link link = new Link(null, null, null, null, null, null, null);
        link.linkStateMachine.motionState = LinkMotionState.REVOLVING_DOOR;
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
