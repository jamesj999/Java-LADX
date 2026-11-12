package linksawakening.entity;

import static org.junit.jupiter.api.Assertions.*;

import org.junit.jupiter.api.Test;

class LinkMotionStatusTest {
    @Test
    void interactionWriteRetainsExactByteAndRejectsOutOfRangeBeforeChangingState() {
        var status = new LinkMotionStatus();
        status.blockNextRomMotionFrame();
        assertEquals(2, status.romInteractiveMotionBlockValue());
        assertTrue(status.romInteractiveMotionBlocked());
        status.writeRomInteractiveMotionBlock(255);
        assertThrows(IllegalArgumentException.class, () -> status.writeRomInteractiveMotionBlock(256));
        assertEquals(255, status.romInteractiveMotionBlockValue());
        assertThrows(IllegalArgumentException.class, () -> status.writeRomInteractiveMotionBlock(-1));
        assertTrue(status.romInteractiveMotionBlocked());
        status.writeRomInteractiveMotionBlock(0);
        assertEquals(0, status.romInteractiveMotionBlockValue());
        assertFalse(status.romInteractiveMotionBlocked());
    }
}
