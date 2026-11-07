package linksawakening.audio.apu;

import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class EnvelopeStepTest {
    @Test void envelopeSaturatesAtBothHardwareLimits() {
        for (int volume = 0; volume <= 15; volume++) {
            assertEquals(Math.min(15, volume + 1), EnvelopeStep.next(volume, 8));
            assertEquals(Math.max(0, volume - 1), EnvelopeStep.next(volume, 0));
        }
    }
    @Test void disabledLengthClockPreservesCountAndExpiredTriggerReloads() {
        LengthCounter counter = new LengthCounter(64, 256);
        counter.load(63);
        assertFalse(counter.tick(false));
        assertTrue(counter.tick(true));
        assertFalse(counter.tick(true));
        counter.trigger();
        for (int i = 0; i < 63; i++) assertFalse(counter.tick(true));
        assertTrue(counter.tick(true));
    }
}
