package linksawakening.world;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

final class SwitchBlockLinkInteractionTest {

    @Test
    void mapsLoweredAndRaisedObjectsToRomStates() {
        assertEquals(0x00, SwitchBlockLinkInteraction.expectedState(0xDB));
        assertEquals(0x02, SwitchBlockLinkInteraction.expectedState(0xDC));
        assertTrue(SwitchBlockLinkInteraction.isSwitchBlock(0xDB));
        assertTrue(SwitchBlockLinkInteraction.isSwitchBlock(0xDC));
        assertFalse(SwitchBlockLinkInteraction.isSwitchBlock(0xDA));
    }

    @Test
    void blocksOnlyWhenRomStateDoesNotMatchTheObject() {
        assertFalse(SwitchBlockLinkInteraction.blocks(0xDB, 0x04, 0x00, false));
        assertTrue(SwitchBlockLinkInteraction.blocks(0xDC, 0x04, 0x00, false));
        assertTrue(SwitchBlockLinkInteraction.blocks(0xDB, 0x04, 0x02, false));
        assertFalse(SwitchBlockLinkInteraction.blocks(0xDC, 0x04, 0x02, false));
    }

    @Test
    void standingOverridePassesAMismatchedSwitchBlock() {
        assertFalse(SwitchBlockLinkInteraction.blocks(0xDC, 0x04, 0x00, true));
    }

    @Test
    void groundRefreshMarksOnlyAMismatchedOceanSwitchBlock() {
        assertTrue(SwitchBlockLinkInteraction.marksStanding(0xDC, 0x04, 0x00));
        assertFalse(SwitchBlockLinkInteraction.marksStanding(0xDC, 0x04, 0x02));
        assertFalse(SwitchBlockLinkInteraction.marksStanding(0xDC, 0x01, 0x00));
    }

    @Test
    void rejectsInvalidSwitchState() {
        assertThrows(IllegalArgumentException.class,
            () -> SwitchBlockLinkInteraction.blocks(0xDB, 0x04, 0x01, false));
        assertThrows(IllegalArgumentException.class,
            () -> SwitchBlockLinkInteraction.marksStanding(0xDB, 0x04, 0xFF));
    }

    @Test
    void rejectsUnknownObjectWhenExpectedStateIsRequested() {
        assertThrows(IllegalArgumentException.class,
            () -> SwitchBlockLinkInteraction.expectedState(0xDA));
        assertThrows(IllegalArgumentException.class,
            () -> SwitchBlockLinkInteraction.expectedState(0xDD));
    }
}
