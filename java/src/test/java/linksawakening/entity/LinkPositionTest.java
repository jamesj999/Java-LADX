package linksawakening.entity;

import static org.junit.jupiter.api.Assertions.*;

import org.junit.jupiter.api.Test;

class LinkPositionTest {
    @Test
    void finalPositionShadowRetainsFractionAndSignedByteIntegration() {
        var position = new LinkPosition(new LinkComponentRef<>(), new LinkCollisionState());
        position.setPixelPosition(10, 20);
        position.applyRomFinalPosition(0xFF, 0x11);
        assertEquals(159, position.subX());
        assertEquals(337, position.subY());
        assertEquals(9, position.pixelX());
        assertEquals(21, position.pixelY());
        position.captureRomFinalPosition();
        position.applyRomFinalPosition(5, 6);
        position.restoreRomFinalPositionX();
        assertEquals(159, position.subX());
        assertEquals(343, position.subY());
        position.restoreRomFinalPosition();
        assertEquals(337, position.subY());
        assertEquals(17, position.romFinalPositionX());
    }

    @Test
    void rejectedSpeedDoesNotPartiallyUpdatePositionOrPendingResponse() {
        var position = new LinkPosition(new LinkComponentRef<>(), new LinkCollisionState());
        position.setPixelPosition(10, 20);
        position.applyRomSpeed(0x80, 0x7F);
        assertThrows(IllegalArgumentException.class, () -> position.applyRomSpeed(1, 256));
        assertEquals(-128, position.forcedSpeedX());
        assertEquals(127, position.forcedSpeedY());
        assertThrows(IllegalArgumentException.class, () -> position.applyRomFinalPosition(-1, 1));
        assertEquals(160, position.subX());
        assertEquals(320, position.subY());
    }
}
