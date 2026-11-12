package linksawakening.audio;

import static org.junit.jupiter.api.Assertions.assertTrue;

import linksawakening.audio.apu.GameBoyApu;
import linksawakening.audio.music.FrameAudioMixer;
import linksawakening.audio.music.MusicDriver;
import linksawakening.audio.music.MusicTrack;
import linksawakening.rom.Rom;
import linksawakening.testing.AllocationProbe;
import org.junit.jupiter.api.Test;

final class AudioAllocationTest {
    @Test
    void steadyStateAllocation() {
        byte[] rom = syntheticRom();
        short[] samples = new short[1600];
        var apu = new GameBoyApu(48000);
        var driver = new MusicDriver(Rom.of(rom), apu);
        driver.start(new MusicTrack(1, "allocation", 0x1b, 0x4077, 0x5000, 0x1b * 0x4000 + 0x1000));
        double bytes = AllocationProbe.bytesPerIteration(5000, 1000, () -> {
            driver.tick60Hz();
            apu.renderInto(samples, 0, 800);
        });
        System.out.println("Audio allocation bytes/tick=" + bytes);
        assertTrue(bytes < 16, "Audio allocated " + bytes + " bytes/tick");
    }

    @Test
    void steadyStateFrameMixerAllocation() {
        var apu = new GameBoyApu(48001);
        var driver = new MusicDriver(Rom.of(syntheticRom()), apu);
        driver.start(new MusicTrack(1, "mixer allocation", 0x1b, 0x4077, 0x5000, 0x1b * 0x4000 + 0x1000));
        var mixer = new FrameAudioMixer(driver, apu, null);
        double bytes = AllocationProbe.bytesPerIteration(5000, 1000, mixer::renderBufferPcm);
        assertTrue(driver.isPlaying(), "measurement must keep sequencing active");
        System.out.println("Frame mixer allocation bytes/buffer=" + bytes);
        assertTrue(bytes < 16, "Frame mixer allocated " + bytes + " bytes/buffer");
    }

    private static byte[] syntheticRom() {
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
        return rom;
    }

    private static void word(byte[] data, int at, int value) {
        data[at] = (byte) value;
        data[at + 1] = (byte) (value >>> 8);
    }
}
