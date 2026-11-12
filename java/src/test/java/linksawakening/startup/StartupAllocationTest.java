package linksawakening.startup;

import static org.junit.jupiter.api.Assertions.*;

import linksawakening.testing.AllocationProbe;
import org.junit.jupiter.api.Test;

class StartupAllocationTest {
    private static volatile Object retained;

    @Test
    void idleBedAndMarinPresentationAllocation() {
        var motion = new MarinWakeUpMotion();
        motion.tickMarinPresentation(0, 80, 64, 56, 52);
        double bytes = AllocationProbe.bytesPerIteration(4000, 1000, () -> {
            retained = motion.tick(0, true, false);
            retained = motion.tickMarinPresentation(0, 80, 64, 56, 52);
        });
        assertFalse(motion.complete());
        System.out.println("Startup bed/presentation bytes/tick=" + bytes);
        assertTrue(bytes <= 80, "bytes/tick=" + bytes);
    }
}
