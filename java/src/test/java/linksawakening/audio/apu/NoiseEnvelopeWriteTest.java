package linksawakening.audio.apu;

import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class NoiseEnvelopeWriteTest {
    @Test void initialVolumeWriteWaitsForTriggerButDacOffTakesEffectImmediately() {
        NoiseChannel channel = new NoiseChannel(65536);
        NoiseChannel reference = new NoiseChannel(65536);
        for (NoiseChannel noise : new NoiseChannel[] {channel, reference}) {
            noise.writeEnvelope(0xF0);
            noise.writePolynomial(0x21);
            noise.writeControl(0x80);
            for (int i = 0; i < 40; i++) noise.render();
        }
        // Same disabled envelope/direction; avoids model-specific live-write quirks.
        channel.writeEnvelope(0x30);
        for (int i = 0; i < 1600; i++) assertEquals(reference.render(), channel.render());
        channel.writeControl(0x80);
        double minimum = 1;
        for (int i = 0; i < 40; i++) minimum = Math.min(minimum, channel.render());
        assertEquals(0.6, minimum, 1e-9, "digital volume 3 maps to DAC value 0.6");
        channel.writeEnvelope(0);
        assertFalse(channel.isActive());
        assertEquals(0, channel.render());
    }

    @Test void squareInitialVolumeAlsoWaitsForTrigger() {
        SquareChannel channel = new SquareChannel(65536);
        SquareChannel reference = new SquareChannel(65536);
        for (SquareChannel square : new SquareChannel[] {channel, reference}) {
            square.writeEnvelope(0xF0);
            square.writeDuty(0x80);
            square.writeFrequencyHigh(0x80);
        }
        channel.writeEnvelope(0x30);
        for (int i = 0; i < 1600; i++) assertEquals(reference.render(), channel.render());
        channel.writeEnvelope(0);
        assertFalse(channel.isActive());
        assertEquals(0, channel.render());
    }
}
