package linksawakening.dialog;

import static org.junit.jupiter.api.Assertions.*;

import linksawakening.testing.AllocationProbe;
import org.junit.jupiter.api.Test;

class DialogTextAllocationTest {
    private static volatile String retained;

    @Test
    void retainedTextSurvivesPrintingAndOpeningAnotherPage() {
        var dialog = new DialogController(16);
        dialog.open("ABCDEFGHIJKLMNOP");
        for (int i = 0; i < 4; i++) dialog.tick();
        String retainedText = dialog.visibleText();
        int visibleLength = retainedText.length();
        for (int i = 0; i < 100; i++) dialog.tick();
        assertTrue(dialog.visibleText().length() > visibleLength);
        assertEquals("ABCDEFGHIJKLMNOP".substring(0, visibleLength), retainedText);
        dialog.open("XYZ");
        for (int i = 0; i < 100; i++) dialog.tick();
        assertEquals("XYZ", dialog.visibleText());
        assertEquals("ABCDEFGHIJKLMNOP".substring(0, visibleLength), retainedText);
    }

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
        assertTrue(bytes <= 0, "bytes/read=" + bytes);
    }
}
