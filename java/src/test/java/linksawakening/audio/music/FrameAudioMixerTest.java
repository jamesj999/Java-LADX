package linksawakening.audio.music;

import static org.junit.jupiter.api.Assertions.*;

import linksawakening.audio.apu.GameBoyApu;
import linksawakening.rom.Rom;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

final class FrameAudioMixerTest {
    @ParameterizedTest
    @ValueSource(ints = {48000, 48001, 30})
    void sampleClockMatchesOriginalChunkOrdering(int rate) {
        byte[] rom = new byte[0x80000];
        int base = 0x1b * 0x4000 - 0x4000;
        word(rom, base + 0x5001, 0x5100);
        word(rom, base + 0x5003, 0x5200);
        rom[base + 0x5100] = 1;
        word(rom, base + 0x5200, 0x5300);
        word(rom, base + 0x5202, 0xffff);
        word(rom, base + 0x5204, 0x5200);
        byte[] notes = {(byte) 0x9d, (byte) 0xf0, 0, (byte) 0x80, (byte) 0xa0, 0x4a, 0};
        System.arraycopy(notes, 0, rom, base + 0x5300, notes.length);
        var apu = new GameBoyApu(rate);
        var reference = new GameBoyApu(rate);
        var driver = new MusicDriver(Rom.of(rom), apu);
        var other = new MusicDriver(Rom.of(rom), reference);
        var track = new MusicTrack(1, "clock", 0x1b, 0x4077, 0x5000, base + 0x5000);
        driver.start(track);
        other.start(track);
        driver.tick60Hz();
        other.tick60Hz();
        var mixer = new FrameAudioMixer(driver, apu, null);
        int remainder = rate % 60;
        int until = Math.max(1, rate / 60);
        for (int buffer = 0; buffer < 30; buffer++) {
            short[] expected = new short[2048];
            int rendered = 0;
            while (rendered < 1024) {
                int chunk = Math.min(1024 - rendered, until);
                short[] pcm = reference.render(chunk);
                System.arraycopy(pcm, 0, expected, rendered * 2, pcm.length);
                rendered += chunk;
                until -= chunk;
                if (until == 0) {
                    other.tick60Hz();
                    remainder += rate;
                    until = Math.max(1, remainder / 60);
                    remainder %= 60;
                }
            }
            assertArrayEquals(expected, mixer.renderBufferPcm());
        }
        driver.stop();
        assertArrayEquals(new short[2048], mixer.renderBufferPcm());
    }

    private static void word(byte[] bytes, int at, int value) {
        bytes[at] = (byte) value;
        bytes[at + 1] = (byte) (value >>> 8);
    }
}
