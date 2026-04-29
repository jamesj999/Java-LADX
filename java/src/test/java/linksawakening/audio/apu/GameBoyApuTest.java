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

    private static boolean hasNonZeroSample(short[] pcm) {
        for (short sample : pcm) {
            if (sample != 0) {
                return true;
            }
        }
        return false;
    }
}
