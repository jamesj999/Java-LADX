package linksawakening.dialog;

import static org.junit.jupiter.api.Assertions.*;

import java.util.List;
import linksawakening.testing.AllocationProbe;
import org.junit.jupiter.api.Test;

class DialogAllocationTest {
    @Test
    void printingTicksStayWithinAllocationBudget() {
        var dialog = new DialogController(16);
        dialog.open("ABCDEFGHIJKLMNOP");
        int[] printed = {0};
        double bytesPerFrame = AllocationProbe.bytesPerIteration(4000, 1000, () -> {
            if (dialog.visibleCharactersOnPage == 16) {
                dialog.visibleCharactersOnPage = 0;
                dialog.visibleNonSpaceCharactersOnPage = 0;
            }
            int before = dialog.visibleCharactersOnPage;
            dialog.tick();
            if (dialog.visibleCharactersOnPage > before) printed[0]++;
            dialog.consumeSoundEvents();
        });
        assertTrue(printed[0] > 1000);
        System.out.println("Dialog printing bytes/frame=" + bytesPerFrame);
        assertTrue(bytesPerFrame <= 8, "bytes/frame=" + bytesPerFrame);
    }

    @Test
    void retainedSoundEventsRemainImmutableAndIndependent() {
        var dialog = new DialogController(16);
        dialog.open("AB");
        dialog.tick();
        dialog.tick();
        var retained = dialog.consumeSoundEvents();
        assertThrows(UnsupportedOperationException.class, retained::clear);
        dialog.tick();
        dialog.tick();
        assertTrue(dialog.consumeSoundEvents().isEmpty());
        assertEquals(List.of(DialogController.SoundEvent.TEXT_PRINT), retained);
    }
}
