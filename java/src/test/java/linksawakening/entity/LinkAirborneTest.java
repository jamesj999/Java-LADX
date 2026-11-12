package linksawakening.entity;

import static org.junit.jupiter.api.Assertions.*;

import org.junit.jupiter.api.Test;

final class LinkAirborneTest {
    @Test
    void pitFallRecoversRoomEntryPositionOnFrameNinetySix() {
        Link link = new Link(null, null, null, null, null, null, null);
        link.setRoomEntryPixelPosition(32, 48);
        link.setPixelPosition(64, 64);
        link.linkAirborne.startPitFall();
        for (int frame = 0; frame < 95; frame++) link.linkAirborne.tickPitFall();
        assertTrue(link.isFallingIntoPit());
        assertEquals(64, link.pixelX());
        link.linkAirborne.tickPitFall();
        assertFalse(link.isFallingIntoPit());
        assertEquals(32, link.pixelX());
        assertEquals(48, link.pixelY());
    }

    @Test
    void missingPitTargetReturnsToNormalGroundWithoutMoving() {
        Link link = new Link(null, null, null, null, null, null, null);
        link.setPixelPosition(64, 64);
        link.linkAirborne.groundStatus = Link.GROUND_STATUS_PIT;
        link.linkAirborne.slipTowardPitCenter();
        assertEquals(Link.GROUND_STATUS_NORMAL, link.linkAirborne.groundStatus);
        assertEquals(64, link.pixelX());
        assertEquals(64, link.pixelY());
        assertFalse(link.isFallingIntoPit());
    }
}
