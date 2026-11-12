package linksawakening.audio;

import static org.junit.jupiter.api.Assertions.assertTrue;

import com.sun.management.ThreadMXBean;
import java.lang.management.ManagementFactory;
import linksawakening.audio.apu.GameBoyApu;
import linksawakening.audio.music.MusicDriver;
import linksawakening.audio.music.MusicTrack;
import org.junit.jupiter.api.Test;

final class AudioAllocationTest {
    @Test
    void steadyStateAllocation() {
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
        short[] samples = new short[1600];
        var apu = new GameBoyApu(48000);
        var driver = new MusicDriver(rom, apu);
        driver.start(new MusicTrack(1, "allocation", 0x1b, 0x4077, 0x5000, base + 0x5000));
        for (int i = 0; i < 5000; i++) {
            driver.tick60Hz();
            apu.renderInto(samples, 0, 800);
        }
        ThreadMXBean bean = (ThreadMXBean) ManagementFactory.getThreadMXBean();
        bean.setThreadAllocatedMemoryEnabled(true);
        long thread = Thread.currentThread().threadId();
        long before = bean.getThreadAllocatedBytes(thread);
        for (int i = 0; i < 5000; i++) {
            driver.tick60Hz();
            apu.renderInto(samples, 0, 800);
        }
        double bytes = (bean.getThreadAllocatedBytes(thread) - before) / 5000.0;
        System.out.println("Audio allocation bytes/tick=" + bytes);
        assertTrue(bytes < 256);
    }

    private static void word(byte[] data, int at, int value) {
        data[at] = (byte) value;
        data[at + 1] = (byte) (value >>> 8);
    }
}
