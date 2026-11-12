package linksawakening.entity;

import static org.junit.jupiter.api.Assertions.*;

import linksawakening.state.Direction;
import linksawakening.state.LinkMotionState;
import linksawakening.state.PlayerState;
import org.junit.jupiter.api.Test;

final class LinkStateMachineTest {
    @Test
    void spikesReverseSpeedBufferOneHeartAndGateRepeatedHits() {
        PlayerState player = new PlayerState();
        Link link = new LinkTestBuilder()
                .input(null)
                .config(null)
                .tables(null)
                .collision(null)
                .sprites(null)
                .player(player)
                .items(null)
                .build();
        link.linkMotion.position().lastRomSpeedX(0x14);
        link.linkMotion.position().lastRomSpeedY(0xF1);
        link.linkDamage.hurtBySpikes();
        assertEquals(0xEC, link.position().romSpeedX());
        assertEquals(0x0F, link.position().romSpeedY());
        assertTrue(link.linkAirborne.elevation().airborne());
        assertEquals(0x10, link.linkAirborne.elevation().zVelocity());
        assertEquals(2 << Link.SUB_PIXEL_SHIFT, link.linkAirborne.elevation().zSubPixels());
        assertEquals(0x10, link.linkDamage.collisionIgnoreFramesRemaining);
        assertEquals(0x30, player.vitals().invincibilityCounter());
        assertEquals(4, player.vitals().subtractHealthBuffer());
        link.linkDamage.hurtBySpikes();
        assertEquals(4, player.vitals().subtractHealthBuffer());
        assertEquals(0xEC, link.position().romSpeedX());
    }

    @Test
    void sideViewSpikesPreserveVerticalJumpRegisters() {
        PlayerState player = new PlayerState();
        Link link = new LinkTestBuilder()
                .input(null)
                .config(null)
                .tables(null)
                .collision(null)
                .sprites(null)
                .player(player)
                .items(null)
                .build();
        link.linkSideView.sideScrolling = true;
        link.linkAirborne.elevation().zVelocity(7);
        link.linkDamage.hurtBySpikes();
        assertEquals(7, link.linkAirborne.elevation().zVelocity());
        assertEquals(0, link.linkAirborne.elevation().zSubPixels());
        assertEquals(4, player.vitals().subtractHealthBuffer());
    }

    @Test
    void scrollEntryPreservesBothFractionalAccumulatorsAndRecordsLanding() {
        Link link = new LinkTestBuilder()
                .input(null)
                .config(null)
                .tables(null)
                .collision(null)
                .sprites(null)
                .player(null)
                .items(null)
                .build();
        link.linkMotion.position().subX((148 << Link.SUB_PIXEL_SHIFT) + 10);
        link.linkMotion.position().subY((80 << Link.SUB_PIXEL_SHIFT) + 5);
        link.position().setRoomScrollEntryPixelPosition(3, 80);
        assertEquals(
                (3 << Link.SUB_PIXEL_SHIFT) + 10, link.linkMotion.position().subX());
        assertEquals(
                (80 << Link.SUB_PIXEL_SHIFT) + 5, link.linkMotion.position().subY());
        assertEquals(
                link.linkMotion.position().subX(), link.linkMotion.position().roomEntrySubX());
        assertEquals(
                link.linkMotion.position().subY(), link.linkMotion.position().roomEntrySubY());
        assertEquals(11, link.position().roomEntryRomPositionX());
        assertEquals(96, link.position().roomEntryRomPositionY());
    }

    @Test
    void interactiveBlockIsConsumedAfterInvincibilityTicks() {
        PlayerState player = new PlayerState();
        Link link = new LinkTestBuilder()
                .input(null)
                .config(null)
                .tables(null)
                .collision(null)
                .sprites(null)
                .player(player)
                .items(null)
                .build();
        link.position().setPixelPosition(64, 64);
        link.motion().blockNextRomMotionFrame();
        player.vitals().setInvincibilityCounter(2);
        link.position().applyRomSpeed(16, 0);
        link.motion().update();
        assertFalse(link.linkStateMachine.status().romInteractiveMotionBlocked());
        assertEquals(1, player.vitals().invincibilityCounter());
        assertEquals(64, link.position().pixelX());
        assertEquals(0, link.position().romSpeedX());
        assertTrue(link.linkMotion.position().forcedSpeedPending());
    }

    @Test
    void revolvingDoorExitsOnFrameSeventyTwoAndEmitsOneTransition() {
        Link link = new LinkTestBuilder()
                .input(null)
                .config(null)
                .tables(null)
                .collision(null)
                .sprites(null)
                .player(null)
                .items(null)
                .build();
        link.linkStateMachine.status().motionState(LinkMotionState.REVOLVING_DOOR);
        link.linkStateMachine.door().revolvingDoorFrame(70);
        link.linkStateMachine.door().tickRevolvingDoor();
        assertEquals(5, link.airborne().romMotionState());
        assertFalse(link.motion().consumeRevolvingDoorTransition());
        link.linkStateMachine.door().tickRevolvingDoor();
        assertEquals(LinkMotionState.DEFAULT.romValue(), link.airborne().romMotionState());
        assertEquals(Direction.UP.javaIndex(), link.animation().direction());
        assertEquals(-5, link.position().romEntityY());
        assertTrue(link.motion().consumeRevolvingDoorTransition());
        assertFalse(link.motion().consumeRevolvingDoorTransition());
    }
}
