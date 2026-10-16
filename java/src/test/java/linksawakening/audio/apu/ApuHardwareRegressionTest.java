package linksawakening.audio.apu;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Hardware sequences from:
 * https://gbdev.io/pandocs/Audio_Registers.html
 * https://gbdev.io/pandocs/Audio_details.html
 */
final class ApuHardwareRegressionTest {
    private static GameBoyApu powered() {
        GameBoyApu apu = new GameBoyApu(65_536);
        apu.writeRegister(GameBoyApu.NR52, 0x80);
        apu.writeRegister(GameBoyApu.NR50, 0x77);
        apu.writeRegister(GameBoyApu.NR51, 0xFF);
        return apu;
    }

    @ParameterizedTest
    @ValueSource(ints = {1, 2})
    void pulseLengthExpiresAndRetriggerKeepsRemainingLength(int channel) {
        GameBoyApu apu = powered();
        int duty = channel == 1 ? GameBoyApu.NR11 : GameBoyApu.NR21;
        int envelope = channel == 1 ? GameBoyApu.NR12 : GameBoyApu.NR22;
        int control = channel == 1 ? GameBoyApu.NR14 : GameBoyApu.NR24;
        apu.writeRegister(duty, 62);
        apu.writeRegister(envelope, 0xF0);
        apu.writeRegister(control, 0xC0);
        apu.render(256);
        assertTrue(apu.isChannelActive(channel));
        apu.writeRegister(control, 0xC0);
        apu.render(256);
        assertFalse(apu.isChannelActive(channel));
    }

    @Test
    void waveLengthExpiresAndCanBeReloadedWhilePlaying() {
        GameBoyApu apu = powered();
        apu.writeRegister(GameBoyApu.NR30, 0x80);
        apu.writeRegister(GameBoyApu.NR31, 254);
        apu.writeRegister(GameBoyApu.NR34, 0xC0);
        apu.render(256);
        assertTrue(apu.isChannelActive(3));
        apu.writeRegister(GameBoyApu.NR31, 255);
        apu.render(256);
        assertFalse(apu.isChannelActive(3));
    }

    @ParameterizedTest
    @ValueSource(ints = {1, 2, 3, 4})
    void disabledLengthHoldsAndExpiredLengthRetriggersWithFullDuration(int channel) {
        GameBoyApu apu = powered();
        int lengthRegister = switch (channel) {
            case 1 -> GameBoyApu.NR11;
            case 2 -> GameBoyApu.NR21;
            case 3 -> GameBoyApu.NR31;
            default -> GameBoyApu.NR41;
        };
        int control = switch (channel) {
            case 1 -> GameBoyApu.NR14;
            case 2 -> GameBoyApu.NR24;
            case 3 -> GameBoyApu.NR34;
            default -> GameBoyApu.NR44;
        };
        apu.writeRegister(GameBoyApu.NR12, 0xF0);
        apu.writeRegister(GameBoyApu.NR22, 0xF0);
        apu.writeRegister(GameBoyApu.NR30, 0x80);
        apu.writeRegister(GameBoyApu.NR42, 0xF0);
        apu.writeRegister(lengthRegister, channel == 3 ? 255 : 63);
        apu.writeRegister(control, 0x80);
        apu.render(65_536);
        assertTrue(apu.isChannelActive(channel));
        apu.writeRegister(control, 0x40);
        apu.render(256);
        assertFalse(apu.isChannelActive(channel));
        apu.writeRegister(control, 0xC0);
        int fullLength = channel == 3 ? 256 : 64;
        apu.render((fullLength - 1) * 256);
        assertTrue(apu.isChannelActive(channel));
        apu.render(256);
        assertFalse(apu.isChannelActive(channel));
    }

    @Test
    void noisePolynomialClockMatchesKnownLfsrSequence() {
        GameBoyApu apu = powered();
        apu.writeRegister(GameBoyApu.NR42, 0xF0);
        // divider 1, shift 2: 65536 LFSR clocks/second, one per sample.
        apu.writeRegister(GameBoyApu.NR43, 0x21);
        apu.writeRegister(GameBoyApu.NR44, 0x80);
        short[] pcm = apu.render(40);
        int lfsr = 0x7FFF;
        for (int i = 0; i < 40; i++) {
            assertEquals((lfsr & 1) == 0 ? 4096 : -4096, pcm[i * 2]);
            int feedback = (lfsr ^ (lfsr >>> 1)) & 1;
            lfsr = (lfsr >>> 1) | (feedback << 14);
        }
    }

    @ParameterizedTest
    @ValueSource(ints = {14, 15})
    void highNoiseShiftsStopLfsrClock(int shift) {
        GameBoyApu apu = powered();
        apu.writeRegister(GameBoyApu.NR42, 0xF0);
        apu.writeRegister(GameBoyApu.NR43, shift << 4);
        apu.writeRegister(GameBoyApu.NR44, 0x80);
        short[] pcm = apu.render(65_536);
        for (short sample : pcm) {
            assertEquals(-4096, sample);
        }
    }

    @Test
    void noiseLengthWritesReloadImmediatelyAndRetriggerDoesNotExtendNote() {
        GameBoyApu apu = powered();
        apu.writeRegister(GameBoyApu.NR41, 60);
        apu.writeRegister(GameBoyApu.NR42, 0xF0);
        apu.writeRegister(GameBoyApu.NR44, 0xC0);
        apu.render(256);
        apu.writeRegister(GameBoyApu.NR41, 62);
        apu.render(256);
        apu.writeRegister(GameBoyApu.NR44, 0xC0);
        apu.render(256);
        assertFalse(apu.isChannelActive(4));
    }

    @Test
    void powerOffClearsRegistersAndChannels() {
        GameBoyApu apu = powered();
        apu.writeRegister(GameBoyApu.NR12, 0xF0);
        apu.writeRegister(GameBoyApu.NR14, 0x80);
        apu.writeRegister(GameBoyApu.NR52, 0);
        assertEquals(0, apu.readRegister(GameBoyApu.NR50));
        assertEquals(0, apu.readRegister(GameBoyApu.NR12));
        apu.writeRegister(GameBoyApu.NR52, 0x80);
        assertFalse(apu.isChannelActive(1));
    }

    @Test
    void writesWhilePoweredOffCannotPreloadOrTriggerChannels() {
        GameBoyApu apu = new GameBoyApu(65_536);
        apu.writeRegister(GameBoyApu.NR12, 0xF0);
        apu.writeRegister(GameBoyApu.NR14, 0x80);
        apu.writeRegister(GameBoyApu.NR52, 0x80);
        assertFalse(apu.isChannelActive(1));
        assertEquals(0, apu.readRegister(GameBoyApu.NR12));
    }

    @Test
    void waveRamSurvivesPowerCycleAndRemainsWritableWhileOff() {
        GameBoyApu apu = powered();
        apu.writeWaveRam(3, 0xAB);
        apu.writeRegister(GameBoyApu.NR52, 0);
        assertEquals(0xAB, apu.readWaveRam(3));
        apu.writeWaveRam(4, 0xCD);
        apu.writeRegister(GameBoyApu.NR52, 0x80);
        assertEquals(0xCD, apu.readWaveRam(4));
    }

    @Test
    void minimumMasterVolumeIsOneEighthOnBothOutputs() {
        GameBoyApu apu = powered();
        apu.writeRegister(GameBoyApu.NR50, 0);
        apu.writeRegister(GameBoyApu.NR12, 0xF0);
        apu.writeRegister(GameBoyApu.NR14, 0x80);
        short[] pcm = apu.render(1);
        assertEquals(512, Math.abs(pcm[0]));
        assertEquals(512, Math.abs(pcm[1]));
    }

    @Test
    void clearingSweepNegateAfterSubtractionDisablesPulse() {
        GameBoyApu apu = powered();
        apu.writeRegister(GameBoyApu.NR10, 0x19);
        apu.writeRegister(GameBoyApu.NR12, 0xF0);
        apu.writeRegister(GameBoyApu.NR14, 0x83);
        assertTrue(apu.isChannelActive(1));
        apu.writeRegister(GameBoyApu.NR10, 0x11);
        assertFalse(apu.isChannelActive(1));
        apu.writeRegister(GameBoyApu.NR14, 0x83);
        assertTrue(apu.isChannelActive(1));
    }
}
