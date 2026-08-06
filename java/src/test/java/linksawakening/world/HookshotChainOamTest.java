package linksawakening.world;

import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

final class HookshotChainOamTest {

    @Test
    void wrapsTheRomEightBitDeltaBeforeArithmeticShifting() {
        List<HookshotChainOam.Entry> entries = HookshotChainOam.entries(
            0x10, 0x50, 0x20, 0x50, 0);

        assertEquals(0x20, entries.get(0).rawX());
        assertEquals(0x1C, entries.get(1).rawX());
        assertEquals(0x18, entries.get(2).rawX());
        assertEquals(0x50, entries.get(0).rawY());
    }

    @Test
    void advancesEachLinkByTheOriginalQuarterDistanceAndAddsRomXOffset() {
        List<HookshotChainOam.Entry> entries = HookshotChainOam.entries(
            0x00, 0x50, 0xF0, 0x50, 0);

        assertEquals(0xF8, entries.get(0).rawX());
        assertEquals(0xFC, entries.get(1).rawX());
        assertEquals(0x00, entries.get(2).rawX());
        assertEquals(0x50, entries.get(0).rawY());
        assertEquals(0x24, entries.get(0).tileIndex());
        assertEquals(0x00, entries.get(0).attributes());
    }

    @Test
    void followsTheRomTwoFrameVisibilityCadence() {
        List<HookshotChainOam.Entry> evenFrame = HookshotChainOam.entries(
            0x40, 0x60, 0x40, 0x60, 0);
        List<HookshotChainOam.Entry> oddFrame = HookshotChainOam.entries(
            0x40, 0x60, 0x40, 0x60, 1);

        assertFalse(evenFrame.get(0).visible());
        assertTrue(evenFrame.get(1).visible());
        assertFalse(evenFrame.get(2).visible());
        assertTrue(oddFrame.get(0).visible());
        assertFalse(oddFrame.get(1).visible());
        assertTrue(oddFrame.get(2).visible());
    }
}
