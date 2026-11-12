package linksawakening.dialog;

import static org.junit.jupiter.api.Assertions.*;

import java.lang.invoke.MethodHandles;
import java.util.List;
import linksawakening.testing.AllocationProbe;
import org.junit.jupiter.api.Test;

class DialogAllocationTest {
    @Test
    void printingTicksStayWithinAllocationBudget() throws ReflectiveOperationException {
        var lookup = MethodHandles.privateLookupIn(DialogController.class, MethodHandles.lookup());
        var visible = lookup.findVarHandle(DialogController.class, "visibleCharactersOnPage", int.class);
        var nonSpace = lookup.findVarHandle(DialogController.class, "visibleNonSpaceCharactersOnPage", int.class);
        var dialog = new DialogController(16);
        dialog.open("ABCDEFGHIJKLMNOP");
        int[] printed = {0};
        double bytesPerFrame = AllocationProbe.bytesPerIteration(4000, 1000, () -> {
            if ((int) visible.get(dialog) == 16) {
                visible.set(dialog, 0);
                nonSpace.set(dialog, 0);
            }
            int before = (int) visible.get(dialog);
            dialog.tick();
            if ((int) visible.get(dialog) > before) printed[0]++;
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
