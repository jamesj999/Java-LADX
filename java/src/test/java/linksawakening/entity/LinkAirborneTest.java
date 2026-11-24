package linksawakening.entity;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import linksawakening.state.GroundStatus;
import linksawakening.testing.TestLinks;
import org.junit.jupiter.api.Test;

final class LinkAirborneTest {
    @Test
    void pitFallRecoversRoomEntryPositionOnFrameNinetySix() {
        Link link = TestLinks.link().build();
        link.position().setRoomEntryPixelPosition(32, 48);
        link.position().setPixelPosition(64, 64);
        link.linkAirborne.pit().startPitFall();
        for (int frame = 0; frame < 95; frame++) link.linkAirborne.pit().tickPitFall();
        assertTrue(link.airborne().isFallingIntoPit());
        assertEquals(64, link.position().pixelX());
        link.linkAirborne.pit().tickPitFall();
        assertFalse(link.airborne().isFallingIntoPit());
        assertEquals(32, link.position().pixelX());
        assertEquals(48, link.position().pixelY());
    }

    @Test
    void missingPitTargetReturnsToNormalGroundWithoutMoving() {
        Link link = TestLinks.link().build();
        link.position().setPixelPosition(64, 64);
        link.linkAirborne.pit().groundStatus(GroundStatus.PIT);
        link.linkAirborne.pit().slipTowardPitCenter(4);
        assertEquals(GroundStatus.NORMAL, link.linkAirborne.pit().groundStatus());
        assertEquals(64, link.position().pixelX());
        assertEquals(64, link.position().pixelY());
        assertFalse(link.airborne().isFallingIntoPit());
    }
}
