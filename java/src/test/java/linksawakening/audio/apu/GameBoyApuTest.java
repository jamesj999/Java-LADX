package linksawakening.audio.apu;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

final class GameBoyApuTest {
    @Test
    void squareChannelProducesDeterministicNonSilentPcmAfterTrigger() {
        GameBoyApu apu = new GameBoyApu(48_000);
        apu.writeRegister(GameBoyApu.NR52, 0x80);
        apu.writeRegister(GameBoyApu.NR50, 0x77);
        apu.writeRegister(GameBoyApu.NR51, 0xFF);
        apu.writeRegister(GameBoyApu.NR11, 0x80);
        apu.writeRegister(GameBoyApu.NR12, 0xF0);
        apu.writeRegister(GameBoyApu.NR13, 0x00);
        apu.writeRegister(GameBoyApu.NR14, 0x87);

        short[] first = apu.render(512);
        apu.reset();
        apu.writeRegister(GameBoyApu.NR52, 0x80);
        apu.writeRegister(GameBoyApu.NR50, 0x77);
        apu.writeRegister(GameBoyApu.NR51, 0xFF);
        apu.writeRegister(GameBoyApu.NR11, 0x80);
        apu.writeRegister(GameBoyApu.NR12, 0xF0);
        apu.writeRegister(GameBoyApu.NR13, 0x00);
        apu.writeRegister(GameBoyApu.NR14, 0x87);

        short[] second = apu.render(512);

        assertEquals(1024, first.length);
        assertArrayEquals(first, second);
        assertTrue(hasNonZeroSample(first));
    }

    @Test
    void swordPokingAttackStopsBeforeItsFollowUpPulse() {
        GameBoyApu apu = new GameBoyApu(32_768);
        apu.writeRegister(GameBoyApu.NR52, 0x80);
        apu.writeRegister(GameBoyApu.NR51, 0x11);
        // Sword Poking jingle $07, first pulse at 1F:4471.
        apu.writeRegister(GameBoyApu.NR10, 0x00);
        apu.writeRegister(GameBoyApu.NR11, 0x3D);
        apu.writeRegister(GameBoyApu.NR12, 0xF0);
        apu.writeRegister(GameBoyApu.NR13, 0xD8);
        apu.writeRegister(GameBoyApu.NR14, 0xC7);

        int thirdLengthTick = 64 + 2 * 128;
        assertTrue(hasNonZeroSample(apu.render(thirdLengthTick - 1)));
        assertTrue(apu.isChannelActive(1));
        assertFalse(hasNonZeroSample(apu.render(128)));
        assertEquals(0x80, apu.readRegister(GameBoyApu.NR52));
    }

    @Test
    void channel2RetriggerPreservesRemainingLengthAndReloadsOnlyAtZero() {
        GameBoyApu apu = new GameBoyApu(32_768);
        apu.writeRegister(GameBoyApu.NR52, 0x80);
        apu.writeRegister(GameBoyApu.NR21, 0xBE);
        apu.writeRegister(GameBoyApu.NR22, 0xF0);
        apu.writeRegister(GameBoyApu.NR24, 0xC7);
        apu.render(64);

        // A DAC-off trigger stays silent and preserves the remaining step.
        apu.writeRegister(GameBoyApu.NR22, 0x07);
        apu.writeRegister(GameBoyApu.NR24, 0xC7);
        assertFalse(apu.isChannelActive(2));
        apu.writeRegister(GameBoyApu.NR22, 0x08);
        assertFalse(apu.isChannelActive(2));
        apu.writeRegister(GameBoyApu.NR24, 0xC7);
        assertTrue(apu.isChannelActive(2));
        apu.render(128);
        assertFalse(apu.isChannelActive(2));

        // The next sequencer step skips length, so a zero reload starts at 63.
        apu.writeRegister(GameBoyApu.NR24, 0xC7);
        apu.render(63 * 128 - 1);
        assertTrue(apu.isChannelActive(2));
        apu.render(1);
        assertFalse(apu.isChannelActive(2));
    }

    @Test
    void waveChannelUsesWaveRamAndOutputLevel() {
        GameBoyApu apu = new GameBoyApu(48_000);
        apu.writeRegister(GameBoyApu.NR52, 0x80);
        apu.writeRegister(GameBoyApu.NR50, 0x77);
        apu.writeRegister(GameBoyApu.NR51, 0xFF);
        for (int i = 0; i < 16; i++) {
            apu.writeWaveRam(i, (i << 4) | (15 - i));
        }
        apu.writeRegister(GameBoyApu.NR30, 0x80);
        apu.writeRegister(GameBoyApu.NR32, 0x20);
        apu.writeRegister(GameBoyApu.NR33, 0x00);
        apu.writeRegister(GameBoyApu.NR34, 0x87);

        short[] pcm = apu.render(512);

        assertEquals(1024, pcm.length);
        assertTrue(hasNonZeroSample(pcm));
    }

    @Test
    void balladWaveNoteStopsWhenItsHardwareLengthExpires() {
        GameBoyApu apu = new GameBoyApu(32_768);
        apu.writeRegister(GameBoyApu.NR52, 0x80);
        apu.writeRegister(GameBoyApu.NR50, 0x77);
        apu.writeRegister(GameBoyApu.NR51, 0x44);
        // Ocarina Ballad first note at 1F:5790.
        apu.writeRegister(GameBoyApu.NR30, 0x80);
        apu.writeRegister(GameBoyApu.NR31, 0xE6);
        apu.writeRegister(GameBoyApu.NR32, 0x20);
        apu.writeRegister(GameBoyApu.NR33, 0x8A);
        apu.writeRegister(GameBoyApu.NR34, 0xC7);

        int noteFrames = (256 - 0xE6) * (apu.sampleRate() / 256);
        assertTrue(hasNonZeroSample(apu.render(noteFrames - 1)));
        assertTrue(apu.isChannelActive(3));
        assertFalse(hasNonZeroSample(apu.render(128)));
        assertFalse(apu.isChannelActive(3));
    }

    @Test
    void noiseChannelProducesDeterministicNonSilentPcmAfterTrigger() {
        GameBoyApu apu = new GameBoyApu(48_000);
        apu.writeRegister(GameBoyApu.NR52, 0x80);
        apu.writeRegister(GameBoyApu.NR50, 0x77);
        apu.writeRegister(GameBoyApu.NR51, 0xFF);
        apu.writeRegister(GameBoyApu.NR42, 0xF0);
        apu.writeRegister(GameBoyApu.NR43, 0x23);
        apu.writeRegister(GameBoyApu.NR44, 0x80);

        short[] first = apu.render(512);
        apu.reset();
        apu.writeRegister(GameBoyApu.NR52, 0x80);
        apu.writeRegister(GameBoyApu.NR50, 0x77);
        apu.writeRegister(GameBoyApu.NR51, 0xFF);
        apu.writeRegister(GameBoyApu.NR42, 0xF0);
        apu.writeRegister(GameBoyApu.NR43, 0x23);
        apu.writeRegister(GameBoyApu.NR44, 0x80);

        short[] second = apu.render(512);

        assertEquals(1024, first.length);
        assertArrayEquals(first, second);
        assertTrue(hasNonZeroSample(first));
    }

    @Test
    void noiseChannelUsesGameBoyPolynomialClock() {
        GameBoyApu apu = new GameBoyApu(48_000);
        apu.writeRegister(GameBoyApu.NR52, 0x80);
        apu.writeRegister(GameBoyApu.NR50, 0x77);
        apu.writeRegister(GameBoyApu.NR51, 0x88);
        apu.writeRegister(GameBoyApu.NR42, 0xF0);
        apu.writeRegister(GameBoyApu.NR43, 0x07);
        apu.writeRegister(GameBoyApu.NR44, 0x80);

        short[] pcm = apu.render(32);

        assertTrue(hasSignChangeInLeftChannel(pcm));
    }

    @Test
    void swordSwingAttackUsesTheRomNoiseDividerRate() {
        GameBoyApu apu = new GameBoyApu(48_000);
        apu.writeRegister(GameBoyApu.NR52, 0x80);
        apu.writeRegister(GameBoyApu.NR50, 0x77);
        apu.writeRegister(GameBoyApu.NR51, 0x88);
        // Sword Swing A attack at 1F:6564.
        apu.writeRegister(GameBoyApu.NR42, 0x40);
        apu.writeRegister(GameBoyApu.NR43, 0x21);
        apu.writeRegister(GameBoyApu.NR44, 0x80);

        // For the first 15 shifts at 65,536 Hz, the trigger seed is still reaching the output.
        assertFalse(hasSignChangeInLeftChannel(apu.render(10)));
        assertTrue(hasSignChangeInLeftChannel(apu.render(5)));
    }

    @Test
    void fullVolumeChannelsShareMixerScale() {
        GameBoyApu squareApu = new GameBoyApu(48_000);
        squareApu.writeRegister(GameBoyApu.NR52, 0x80);
        squareApu.writeRegister(GameBoyApu.NR50, 0x77);
        squareApu.writeRegister(GameBoyApu.NR51, 0x11);
        triggerChannel1(squareApu);

        GameBoyApu noiseApu = new GameBoyApu(48_000);
        noiseApu.writeRegister(GameBoyApu.NR52, 0x80);
        noiseApu.writeRegister(GameBoyApu.NR50, 0x77);
        noiseApu.writeRegister(GameBoyApu.NR51, 0x88);
        noiseApu.writeRegister(GameBoyApu.NR42, 0xF0);
        noiseApu.writeRegister(GameBoyApu.NR43, 0x10);
        noiseApu.writeRegister(GameBoyApu.NR44, 0x80);

        short squareSample = squareApu.render(1)[0];
        short noiseSample = noiseApu.render(1)[0];

        assertEquals(4096, Math.abs(squareSample));
        assertEquals(Math.abs(squareSample), Math.abs(noiseSample));
    }

    @Test
    void noiseChannelAppliesHardwareVolumeEnvelope() {
        GameBoyApu apu = new GameBoyApu(48_000);
        apu.writeRegister(GameBoyApu.NR52, 0x80);
        apu.writeRegister(GameBoyApu.NR50, 0x77);
        apu.writeRegister(GameBoyApu.NR51, 0x88);
        apu.writeRegister(GameBoyApu.NR42, 0x51);
        apu.writeRegister(GameBoyApu.NR43, 0x77);
        apu.writeRegister(GameBoyApu.NR44, 0x80);

        short initial = apu.render(1)[0];
        short[] later = apu.render(760);
        short afterEnvelopeTick = later[(760 - 1) * 2];

        assertTrue(Math.abs(afterEnvelopeTick) < Math.abs(initial));
    }

    @Test
    void noiseChannelLengthCounterStopsEnabledNotes() {
        GameBoyApu apu = new GameBoyApu(48_000);
        apu.writeRegister(GameBoyApu.NR52, 0x80);
        apu.writeRegister(GameBoyApu.NR50, 0x77);
        apu.writeRegister(GameBoyApu.NR51, 0x88);
        apu.writeRegister(GameBoyApu.NR41, 0x3A);
        apu.writeRegister(GameBoyApu.NR42, 0xF0);
        apu.writeRegister(GameBoyApu.NR43, 0x10);
        apu.writeRegister(GameBoyApu.NR44, 0xC0);

        apu.render(1_126);

        assertTrue(!apu.isChannelActive(4));
    }

    @Test
    void squareChannelAppliesHardwareVolumeEnvelope() {
        GameBoyApu apu = new GameBoyApu(48_000);
        apu.writeRegister(GameBoyApu.NR52, 0x80);
        apu.writeRegister(GameBoyApu.NR50, 0x77);
        apu.writeRegister(GameBoyApu.NR51, 0x11);
        apu.writeRegister(GameBoyApu.NR11, 0x80);
        apu.writeRegister(GameBoyApu.NR12, 0x51);
        apu.writeRegister(GameBoyApu.NR13, 0x00);
        apu.writeRegister(GameBoyApu.NR14, 0x87);

        short initial = apu.render(1)[0];
        short[] later = apu.render(760);
        short afterEnvelopeTick = later[(760 - 1) * 2];

        assertTrue(Math.abs(afterEnvelopeTick) < Math.abs(initial));
    }

    @Test
    void channel1HardwareSweepRaisesPitchAfterTrigger() {
        GameBoyApu apu = new GameBoyApu(48_000);
        apu.writeRegister(GameBoyApu.NR52, 0x80);
        apu.writeRegister(GameBoyApu.NR50, 0x77);
        apu.writeRegister(GameBoyApu.NR51, 0x11);
        apu.writeRegister(GameBoyApu.NR10, 0x24);
        apu.writeRegister(GameBoyApu.NR11, 0x80);
        apu.writeRegister(GameBoyApu.NR12, 0xF0);
        apu.writeRegister(GameBoyApu.NR13, 0x00);
        apu.writeRegister(GameBoyApu.NR14, 0x83);

        int earlyTransitions = countLeftChannelSignTransitions(apu.render(4096));
        int laterTransitions = countLeftChannelSignTransitions(apu.render(4096));

        assertTrue(laterTransitions > earlyTransitions,
                "expected sweep to raise pitch, early=" + earlyTransitions + " later=" + laterTransitions);
    }

    @Test
    void channel1SweepWithZeroShiftDoesNotMuteChannel() {
        GameBoyApu apu = new GameBoyApu(48_000);
        apu.writeRegister(GameBoyApu.NR52, 0x80);
        apu.writeRegister(GameBoyApu.NR50, 0x77);
        apu.writeRegister(GameBoyApu.NR51, 0x11);
        apu.writeRegister(GameBoyApu.NR10, 0x70);
        apu.writeRegister(GameBoyApu.NR11, 0x80);
        apu.writeRegister(GameBoyApu.NR12, 0xF0);
        apu.writeRegister(GameBoyApu.NR13, 0xF0);
        apu.writeRegister(GameBoyApu.NR14, 0x87);

        short[] pcm = apu.render(8192);

        assertTrue(hasNonZeroSample(pcm));
        assertTrue(apu.isChannelActive(1));
    }

    @Test
    void routedChannelsAreSummedInsteadOfAveraged() {
        GameBoyApu oneChannel = new GameBoyApu(48_000);
        oneChannel.writeRegister(GameBoyApu.NR52, 0x80);
        oneChannel.writeRegister(GameBoyApu.NR50, 0x77);
        oneChannel.writeRegister(GameBoyApu.NR51, 0x11);
        triggerChannel1(oneChannel);

        GameBoyApu twoChannels = new GameBoyApu(48_000);
        twoChannels.writeRegister(GameBoyApu.NR52, 0x80);
        twoChannels.writeRegister(GameBoyApu.NR50, 0x77);
        twoChannels.writeRegister(GameBoyApu.NR51, 0x33);
        triggerChannel1(twoChannels);
        triggerChannel2(twoChannels);

        short singleLeft = oneChannel.render(1)[0];
        short mixedLeft = twoChannels.render(1)[0];

        assertEquals(singleLeft * 2, mixedLeft);
    }

    @Test
    void nr50ScalesEachSideIndependentlyWithLevelZeroStillAudible() {
        GameBoyApu apu = new GameBoyApu(48_000);
        apu.writeRegister(GameBoyApu.NR52, 0x80);
        apu.writeRegister(GameBoyApu.NR51, 0x11);
        triggerChannel1(apu);

        apu.writeRegister(GameBoyApu.NR50, 0x77);
        short full = apu.render(1)[0];
        assertTrue(full != 0);

        apu.writeRegister(GameBoyApu.NR50, 0x03);
        short[] leftMinimum = apu.render(1);
        assertEquals(full / 8, leftMinimum[0]);
        assertEquals(full / 2, leftMinimum[1]);

        apu.writeRegister(GameBoyApu.NR50, 0x30);
        short[] rightMinimum = apu.render(1);
        assertEquals(full / 2, rightMinimum[0]);
        assertEquals(full / 8, rightMinimum[1]);
    }

    @Test
    void disabledMasterPowerOutputsSilence() {
        GameBoyApu apu = new GameBoyApu(48_000);
        apu.writeRegister(GameBoyApu.NR52, 0x00);
        apu.writeRegister(GameBoyApu.NR50, 0x77);
        apu.writeRegister(GameBoyApu.NR51, 0xFF);
        apu.writeRegister(GameBoyApu.NR11, 0x80);
        apu.writeRegister(GameBoyApu.NR12, 0xF0);
        apu.writeRegister(GameBoyApu.NR13, 0x00);
        apu.writeRegister(GameBoyApu.NR14, 0x87);

        short[] pcm = apu.render(128);

        assertEquals(256, pcm.length);
        for (short sample : pcm) {
            assertEquals(0, sample);
        }
    }

    private static void triggerChannel1(GameBoyApu apu) {
        apu.writeRegister(GameBoyApu.NR11, 0x80);
        apu.writeRegister(GameBoyApu.NR12, 0xF0);
        apu.writeRegister(GameBoyApu.NR13, 0x00);
        apu.writeRegister(GameBoyApu.NR14, 0x87);
    }

    private static void triggerChannel2(GameBoyApu apu) {
        apu.writeRegister(GameBoyApu.NR21, 0x80);
        apu.writeRegister(GameBoyApu.NR22, 0xF0);
        apu.writeRegister(GameBoyApu.NR23, 0x00);
        apu.writeRegister(GameBoyApu.NR24, 0x87);
    }

    private static boolean hasNonZeroSample(short[] pcm) {
        for (short sample : pcm) {
            if (sample != 0) {
                return true;
            }
        }
        return false;
    }

    private static int countLeftChannelSignTransitions(short[] pcm) {
        int transitions = 0;
        int previousSign = 0;
        for (int i = 0; i < pcm.length; i += 2) {
            int sign = Integer.signum(pcm[i]);
            if (sign == 0) {
                continue;
            }
            if (previousSign != 0 && sign != previousSign) {
                transitions++;
            }
            previousSign = sign;
        }
        return transitions;
    }

    private static boolean hasSignChangeInLeftChannel(short[] pcm) {
        int firstSign = Integer.signum(pcm[0]);
        for (int i = 2; i < pcm.length; i += 2) {
            int sign = Integer.signum(pcm[i]);
            if (sign != 0 && sign != firstSign) {
                return true;
            }
        }
        return false;
    }
}
