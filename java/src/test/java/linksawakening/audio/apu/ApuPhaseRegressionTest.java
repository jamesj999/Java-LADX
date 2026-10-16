package linksawakening.audio.apu;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import static org.junit.jupiter.api.Assertions.*;

/** Hardware reference: https://gbdev.io/pandocs/Audio_details.html */
final class ApuPhaseRegressionTest {
    @Test
    void pulseRetriggerPreservesDutyStep() {
        SquareChannel channel = new SquareChannel(65_536);
        channel.writeDuty(0x80);
        channel.writeEnvelope(0xF0);
        channel.writeFrequencyHigh(0x80);
        for (int i = 0; i < 300; i++) {
            channel.tick();
            channel.render();
        }
        assertTrue(channel.render() < 0);
        channel.writeFrequencyHigh(0x80);
        assertTrue(channel.render() < 0);
    }

    @ParameterizedTest
    @ValueSource(booleans = {false, true})
    void silentEnvelopeStillClocksOscillator(boolean noise) {
        if (noise) {
            NoiseChannel audible = new NoiseChannel(65_536);
            NoiseChannel silent = new NoiseChannel(65_536);
            audible.writeEnvelope(0xF1);
            silent.writeEnvelope(0x09);
            audible.writePolynomial(0x21);
            silent.writePolynomial(0x21);
            audible.writeControl(0x80);
            silent.writeControl(0x80);
            for (int i = 0; i < 1024; i++) {
                audible.tick(); silent.tick();
                audible.render(); silent.render();
            }
            for (int i = 0; i < 100; i++) {
                assertEquals(Math.signum(audible.render()), Math.signum(silent.render()));
            }
        } else {
            SquareChannel audible = new SquareChannel(65_536);
            SquareChannel silent = new SquareChannel(65_536);
            audible.writeEnvelope(0xF1);
            silent.writeEnvelope(0x09);
            audible.writeFrequencyLow(0xAB);
            silent.writeFrequencyLow(0xAB);
            audible.writeFrequencyHigh(0x83);
            silent.writeFrequencyHigh(0x83);
            for (int i = 0; i < 1024; i++) {
                audible.tick(); silent.tick();
                audible.render(); silent.render();
            }
            for (int i = 0; i < 1000; i++) {
                assertEquals(Math.signum(audible.render()), Math.signum(silent.render()));
            }
        }
    }

    @Test
    void waveOutputLevelShiftsDigitalSamplesBeforeConversion() {
        WaveChannel channel = new WaveChannel(65_536);
        for (int i = 0; i < 16; i++) {
            channel.writeWaveRam(i, 0xFF);
        }
        channel.writeDac(0x80);
        channel.writeOutputLevel(0x40);
        channel.writeFrequencyHigh(0x80);
        assertEquals(-1.0 / 15, channel.render(), 1e-9);
        channel.writeOutputLevel(0x60);
        assertEquals(-0.6, channel.render(), 1e-9);
        channel.writeOutputLevel(0);
        assertEquals(0, channel.render());
    }

    @Test
    void zeroShiftStillPerformsSweepOverflowCheckAtPaceTick() {
        SquareChannel channel = new SquareChannel(65_536);
        channel.writeSweep(0x10);
        channel.writeEnvelope(0xF0);
        channel.writeFrequencyHigh(0x86);
        assertTrue(channel.isActive());
        for (int i = 0; i < 512; i++) {
            channel.tick();
        }
        assertFalse(channel.isActive());
    }
}
