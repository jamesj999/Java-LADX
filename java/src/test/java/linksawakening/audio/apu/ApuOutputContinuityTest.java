package linksawakening.audio.apu;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import static org.junit.jupiter.api.Assertions.*;

class ApuOutputContinuityTest {
    @ParameterizedTest
    @ValueSource(ints = {44101, 48000})
    void twoSecondsOfFourChannelPcmIsIdenticalAcrossArbitraryBufferBoundaries(int rate) {
        GameBoyApu whole = mixed(rate);
        GameBoyApu split = mixed(rate);
        short[] expected = whole.render(rate * 2);
        short[] actual = new short[expected.length];
        int[] sizes = {1, 799, 1024, 735, 87};
        int offset = 0;
        for (int i = 0; offset < actual.length; i++) {
            int frames = Math.min(sizes[i % sizes.length], (actual.length - offset) / 2);
            short[] chunk = split.render(frames);
            System.arraycopy(chunk, 0, actual, offset, chunk.length);
            offset += chunk.length;
        }
        assertArrayEquals(expected, actual, "buffer boundaries must not reset oscillator or filter state");
        int peak = 0;
        for (short sample : actual) peak = Math.max(peak, Math.abs((int) sample));
        assertTrue(peak > 4096);
        assertTrue(peak < 32767, "all four full-volume channels mix without clipping: " + peak);
    }

    @Test void constantDacBiasDecaysUsingTheCgbCapacitorChargeFactor() {
        GameBoyApu apu = new GameBoyApu(48000);
        apu.writeRegister(GameBoyApu.NR52, 0x80);
        apu.writeRegister(GameBoyApu.NR50, 0x77);
        apu.writeRegister(GameBoyApu.NR51, 0x44);
        apu.writeRegister(GameBoyApu.NR30, 0x80);
        apu.writeRegister(GameBoyApu.NR32, 0); // Digital zero, DAC still enabled.
        apu.writeRegister(GameBoyApu.NR34, 0x80);
        short[] pcm = apu.render(800);
        double charge = Math.pow(0.998943, 4194304.0 / 48000);
        for (int i = 0; i < 800; i++) {
            assertEquals((short) Math.round(4096 * Math.pow(charge, i)), pcm[i * 2]);
        }
        apu.writeRegister(GameBoyApu.NR30, 0);
        assertArrayEquals(new short[1600], apu.render(800), "all DACs off disconnects output");
    }

    @Test void pulseAboveNyquistAveragesDutyEdgesInsteadOfAliasingToAConstantExtreme() {
        SquareChannel square = new SquareChannel(65536);
        square.writeDuty(0x80);
        square.writeEnvelope(0xF0);
        square.writeFrequencyLow(0xFF);
        square.writeFrequencyHigh(0x87); // 131072 Hz: exactly two full periods per sample.
        for (int i = 0; i < 100; i++) assertEquals(0, square.render(), 1e-12);
    }

    private static GameBoyApu mixed(int rate) {
        GameBoyApu apu = new GameBoyApu(rate);
        apu.writeRegister(GameBoyApu.NR52, 0x80);
        apu.writeRegister(GameBoyApu.NR50, 0x77);
        apu.writeRegister(GameBoyApu.NR51, 0xFF);
        apu.writeRegister(GameBoyApu.NR11, 0x80);
        apu.writeRegister(GameBoyApu.NR12, 0xF0);
        apu.writeRegister(GameBoyApu.NR13, 0x34);
        apu.writeRegister(GameBoyApu.NR14, 0x87);
        apu.writeRegister(GameBoyApu.NR21, 0x40);
        apu.writeRegister(GameBoyApu.NR22, 0xF0);
        apu.writeRegister(GameBoyApu.NR23, 0x67);
        apu.writeRegister(GameBoyApu.NR24, 0x87);
        for (int i = 0; i < 16; i++) apu.writeWaveRam(i, (i << 4) | (15 - i));
        apu.writeRegister(GameBoyApu.NR30, 0x80);
        apu.writeRegister(GameBoyApu.NR32, 0x20);
        apu.writeRegister(GameBoyApu.NR33, 0x99);
        apu.writeRegister(GameBoyApu.NR34, 0x87);
        apu.writeRegister(GameBoyApu.NR42, 0xF0);
        apu.writeRegister(GameBoyApu.NR43, 0x13);
        apu.writeRegister(GameBoyApu.NR44, 0x80);
        return apu;
    }
}
