package linksawakening.dialog;

import static org.junit.jupiter.api.Assertions.*;

import linksawakening.testing.AllocationProbe;
import org.junit.jupiter.api.Test;

class DialogTextAllocationTest {
    private static volatile String retained;

    @Test
    void unchangedPageTextAllocation() {
        var dialog = new DialogController(16);
        dialog.open("ABCDEFGHIJKLMNOPABCDEFGHIJKLMNOPABCDEFGHIJKLMNOP");
        for (int i = 0; i < 200; i++) dialog.tick();
        String expected = dialog.visibleText();
        double bytes = AllocationProbe.bytesPerIteration(4000, 1000, () -> retained = dialog.visibleText());
        assertEquals(expected, retained);
        assertTrue(expected.length() >= 16);
        System.out.println("Static dialog text bytes/read=" + bytes);
        assertTrue(bytes <= 100, "bytes/read=" + bytes);
    }
}
