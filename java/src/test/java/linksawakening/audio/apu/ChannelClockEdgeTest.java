package linksawakening.audio.apu;

import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

/** Register sequences exercise the hardware clocks without ROM or audio devices. */
class ChannelClockEdgeTest {
    @Test void noiseFifteenBitSequenceAndTriggerReseed() {
        assertNoiseSequence(0, 15);
    }

    @Test void noiseSevenBitSequenceAndTriggerReseed() {
        assertNoiseSequence(8, 7);
    }

    private void assertNoiseSequence(int polynomial, int width) {
        // NR43 divisor 8 at 524288 Hz produces exactly one LFSR step per sample.
        NoiseChannel channel = new NoiseChannel(524288);
        channel.writeEnvelope(0xF0);
        channel.writePolynomial(polynomial);
        channel.writeControl(0x80);
        // All-one seed outputs digital zero (DAC +1) until bit zero clears.
        for (int i = 0; i < width; i++) assertEquals(1.0, channel.render());
        assertEquals(-1.0, channel.render());
        channel.writeControl(0x80);
        for (int i = 0; i < width; i++) assertEquals(1.0, channel.render());
    }

    @Test void sweepOverflowDisablesAtTrigger() {
        SquareChannel channel = new SquareChannel(65536);
        channel.writeEnvelope(0xF0);
        channel.writeSweep(0x11); // 1500 + 750 exceeds eleven-bit range.
        channel.writeFrequencyLow(1500 & 255);
        channel.writeFrequencyHigh(0x80 | (1500 >>> 8));
        assertFalse(channel.isActive());
    }

    @Test void removingNegateAfterSubtractionDisablesPulse() {
        SquareChannel channel = new SquareChannel(65536);
        channel.writeEnvelope(0xF0);
        channel.writeSweep(0x19);
        channel.writeFrequencyLow(0);
        channel.writeFrequencyHigh(0x84);
        assertTrue(channel.isActive());
        channel.writeSweep(0x11);
        assertFalse(channel.isActive());
    }

    @Test void loadingLengthAndRetriggeringDoesNotReloadNonzeroCounter() {
        LengthCounter counter = new LengthCounter(64, 256);
        counter.load(62);
        counter.trigger();
        assertFalse(counter.tick(true));
        counter.trigger();
        assertTrue(counter.tick(true));
        counter.trigger();
        for (int i = 0; i < 63; i++) assertFalse(counter.tick(true));
        assertTrue(counter.tick(true));
    }
}
