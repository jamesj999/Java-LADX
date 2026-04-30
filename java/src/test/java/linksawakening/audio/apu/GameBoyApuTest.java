package linksawakening.audio.apu;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
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
