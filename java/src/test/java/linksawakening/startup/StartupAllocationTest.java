package linksawakening.startup;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import linksawakening.testing.AllocationProbe;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestReporter;

class StartupAllocationTest {
    private final TestReporter reporter;

    StartupAllocationTest(TestReporter reporter) {
        this.reporter = reporter;
    }

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
        reporter.publishEntry("Startup bed/presentation bytes/tick=" + bytes);
        assertTrue(bytes <= 0, "bytes/tick=" + bytes);
    }
}
