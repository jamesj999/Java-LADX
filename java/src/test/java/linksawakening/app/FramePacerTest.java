package linksawakening.app;

import static org.junit.jupiter.api.Assertions.*;

import org.junit.jupiter.api.Test;

class FramePacerTest {
    private static final class Clock {
        long now, oversleep;
        int parks, spins;
        boolean spurious;

        void park(long nanos) {
            parks++;
            if (spurious) {
                spurious = false;
                return;
            }
            now += nanos + oversleep;
            oversleep = 0;
        }

        void spin() {
            spins++;
            now += 100;
        }

        FramePacer pacer() {
            return new FramePacer(() -> now, this::park, this::spin);
        }
    }

    @Test
    void sixtyStepsTakeOneSecondIncludingWork() {
        var clock = new Clock();
        var pacer = clock.pacer();
        for (int i = 0; i < 600; i++) {
            clock.now += 2_000_000;
            assertTrue(pacer.awaitNextFrame());
        }
        assertTrue(Math.abs(clock.now - 10_000_000_000L) < 100);
        assertEquals(600, clock.parks);
        assertTrue(clock.spins > 0 && clock.spins <= 600_000);
    }

    @Test
    void oversleepIsRecoveredInsteadOfAccumulatingDrift() {
        var clock = new Clock();
        clock.oversleep = 3_000_000;
        var pacer = clock.pacer();
        for (int i = 0; i < 60; i++) {
            clock.now += 1_000_000;
            pacer.awaitNextFrame();
        }
        assertTrue(Math.abs(clock.now - 1_000_000_000L) < 100);
    }

    @Test
    void spuriousWakeupRechecksTheDeadline() {
        var clock = new Clock();
        clock.spurious = true;
        var pacer = clock.pacer();
        pacer.awaitNextFrame();
        assertEquals(2, clock.parks);
        assertTrue(clock.now >= 16_666_666 && clock.now < 16_666_766);
    }

    @Test
    void overrunKeepsAllSixtySimulationDeadlines() {
        var clock = new Clock();
        var pacer = clock.pacer();
        clock.now = 1_000_000_000;
        for (int i = 0; i < 60; i++) assertTrue(pacer.awaitNextFrame());
        assertEquals(0, clock.parks);
        assertEquals(0, clock.spins);
        assertEquals(1_000_000_000, clock.now);
        pacer.awaitNextFrame();
        assertTrue(clock.now >= 1_016_666_666 && clock.now < 1_016_666_766);
    }

    @Test
    void interruptionStopsPacingWithoutClearingTheFlag() {
        var clock = new Clock();
        var pacer = clock.pacer();
        Thread.currentThread().interrupt();
        try {
            assertFalse(pacer.awaitNextFrame());
            assertTrue(Thread.currentThread().isInterrupted());
        } finally {
            Thread.interrupted();
        }
        assertEquals(0, clock.parks);
        assertEquals(0, clock.spins);
    }

    @Test
    void nanoTimeWraparoundRetainsRelativeDeadlines() {
        var clock = new Clock();
        clock.now = Long.MAX_VALUE - 100_000_000;
        long start = clock.now;
        var pacer = clock.pacer();
        for (int i = 0; i < 60; i++) pacer.awaitNextFrame();
        assertTrue(Math.abs((clock.now - start) - 1_000_000_000L) < 100);
    }
}
