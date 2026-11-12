package linksawakening.entity;

import static org.junit.jupiter.api.Assertions.*;

import linksawakening.state.GroundStatus;
import org.junit.jupiter.api.Test;

final class LinkAirborneTest {
    @Test
    void pitFallRecoversRoomEntryPositionOnFrameNinetySix() {
        Link link = new LinkTestBuilder()
                .input(null)
                .config(null)
                .tables(null)
                .collision(null)
                .sprites(null)
                .player(null)
                .items(null)
                .build();
        link.position().setRoomEntryPixelPosition(32, 48);
        link.position().setPixelPosition(64, 64);
        link.linkAirborne.startPitFall();
        for (int frame = 0; frame < 95; frame++) link.linkAirborne.tickPitFall();
        assertTrue(link.airborne().isFallingIntoPit());
        assertEquals(64, link.position().pixelX());
        link.linkAirborne.tickPitFall();
        assertFalse(link.airborne().isFallingIntoPit());
        assertEquals(32, link.position().pixelX());
        assertEquals(48, link.position().pixelY());
    }

    @Test
    void missingPitTargetReturnsToNormalGroundWithoutMoving() {
        Link link = new LinkTestBuilder()
                .input(null)
                .config(null)
                .tables(null)
                .collision(null)
                .sprites(null)
                .player(null)
                .items(null)
                .build();
        link.position().setPixelPosition(64, 64);
        link.linkAirborne.groundStatus = GroundStatus.PIT;
        link.linkAirborne.slipTowardPitCenter();
        assertEquals(GroundStatus.NORMAL, link.linkAirborne.groundStatus);
        assertEquals(64, link.position().pixelX());
        assertEquals(64, link.position().pixelY());
        assertFalse(link.airborne().isFallingIntoPit());
    }
}
