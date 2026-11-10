package linksawakening.audio.openal;

import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class PcmStreamBufferingTest {
    @Test void startupAndRecoveryPrefillBeforePlayingAndCountEachUnderrunOnce() {
        var buffering = new PcmStreamBuffering();
        buffering.observePlaying(false);
        assertFalse(buffering.ready(800, 48000));
        assertFalse(buffering.ready(1600, 48000));
        assertTrue(buffering.ready(2400, 48000));
        assertEquals(0, buffering.underrunCount());
        buffering.started();
        buffering.observePlaying(true);
        buffering.observePlaying(false);
        buffering.observePlaying(false);
        assertEquals(1, buffering.underrunCount());
        assertFalse(buffering.ready(800, 48000));
        assertTrue(buffering.ready(2400, 48000));
        buffering.started();
        buffering.reset();
        buffering.observePlaying(false);
        assertEquals(1, buffering.underrunCount(), "explicit stop is not an underrun");
    }

    @Test void prefillUsesAudioDurationAtNonDivisibleSampleRates() {
        var buffering = new PcmStreamBuffering();
        assertFalse(buffering.ready(2205, 44101));
        assertTrue(buffering.ready(2206, 44101));
        assertTrue(buffering.ready(4800, 48000), "long standalone effects start immediately");
    }
}
