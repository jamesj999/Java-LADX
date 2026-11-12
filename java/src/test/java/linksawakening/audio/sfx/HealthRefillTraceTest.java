package linksawakening.audio.sfx;

import static linksawakening.audio.sfx.SfxRegisterTrace.*;
import static org.junit.jupiter.api.Assertions.*;

import java.util.ArrayList;
import java.util.List;
import linksawakening.audio.apu.GameBoyApu;
import linksawakening.audio.music.MusicDriver;
import linksawakening.audio.music.MusicTrack;
import linksawakening.rom.Rom;
import linksawakening.rom.RomAddress;
import linksawakening.rom.RomBank;
import linksawakening.testing.TestRoms;
import org.junit.jupiter.api.Test;

@linksawakening.testing.RequiresRom
class HealthRefillTraceTest {
    private static byte[] rom() throws Exception {
        try (var stream = linksawakening.testing.TestRom.open()) {
            return stream.readAllBytes();
        }
    }

    @Test
    void healthRefillMatchesWaveHandTraceAndLoadsRomWaveform() throws Exception {
        byte[] rom = rom();
        var trace = new SfxRegisterTrace(rom);
        trace.start(SoundEffectNamespace.WAVE, 6);
        var expected = new ArrayList<SfxRegisterTrace.Write>();
        // func_01F_55F9 -> 6365 -> 635A -> 62F8.
        addHealthStart(expected, 0);
        var view = TestRoms.partial(rom);
        for (int i = 0; i < 16; i++) {
            assertEquals(view.u8(new RomAddress(0x1F, 0x63BC).plus(i)), trace.apu.readWaveRam(i));
        }
        assertTrue(trace.player.ownsChannel(3));
        assertTrue(trace.apu.isChannelActive(3));
        int frequency = 0x7E0;
        for (int frame = 1; frame <= 14; frame++) {
            trace.advance();
            // func_01F_5610: +3 each frame; after five decrements, output
            // shifts to quarter volume at 5628/561D, then seven more to end.
            if (frame == 6) {
                frequency = 0x7E0;
                add(expected, frame, 0x1A, 0x80, 0xD2, 0x60, 0xE0, 0x87);
            } else if (frame == 14) {
                add(expected, frame, 0x1A, 0x80, 0, 0, 0, 0x81);
                add(expected, frame, 0x1A, 0);
            } else {
                frequency += 3;
                add(expected, frame, 0x1D, frequency & 255, frequency >>> 8);
            }
        }
        assertEquals(expected, trace.writes());
        assertFalse(trace.player.isPlaying());
        assertFalse(trace.player.ownsChannel(3));
    }

    @Test
    void repeatedHealthRefillRestartsRemainAudibleAndProtectWaveChannelFromMusic() throws Exception {
        byte[] data = rom();
        // Synthetic music stream uses a different waveform, loud notes and rests.
        // It shares the real SFX ROM bank; no ROM-derived fixture is stored.
        put(data, 0x5000, 0, 0, 0x51, 0, 0, 0, 0, 0, 0x52, 0, 0);
        put(data, 0x5100, 1);
        put(data, 0x5200, 0, 0x53, 0xFF, 0xFF, 0, 0x52);
        put(data, 0x5300, 0x9D, 0, 0x54, 0x20, 0xA0, 0x4A, 1, 0);
        var trace = new SfxRegisterTrace(data);
        var music = new MusicDriver(Rom.of(data), trace.apu);
        music.setSoundEffectState(trace.player::ownsChannel, trace.player::takeWaveRestoreRequest);
        music.start(new MusicTrack(1, "TEST", 0x1B, 0x4077, 0x5000, RomBank.romOffset(0x1B, 0x5000)));
        music.tick60Hz();
        for (int frame = 0; frame < 16; frame++) {
            if ((frame & 1) == 0) {
                int before = trace.writes().size();
                trace.start(SoundEffectNamespace.WAVE, 6);
                var expectedStart = new ArrayList<SfxRegisterTrace.Write>();
                addHealthStart(expectedStart, frame);
                assertEquals(
                        expectedStart,
                        trace.writes().subList(before, trace.writes().size()));
                assertEquals(0xE0, trace.apu.readRegister(GameBoyApu.NR33));
                assertEquals(0x87, trace.apu.readRegister(GameBoyApu.NR34));
            }
            int count = trace.writes().size();
            music.tick60Hz();
            assertEquals(count, trace.writes().size(), "music must not touch SFX-owned CH3");
            short[] pcm = trace.advance();
            assertTrue(varying(pcm), "health refill must remain audible across restarts at frame " + frame);
            assertTrue(trace.apu.isChannelActive(3));
        }
    }

    private static void addHealthStart(List<SfxRegisterTrace.Write> writes, int frame) {
        add(writes, frame, 0x1A, 0); // DAC off for wave RAM load (635A).
        add(writes, frame, 0x1A, 0); // 62F8 disables DAC before register block.
        add(writes, frame, 0x1A, 0x80, 0xD2, 0x40, 0xE0, 0x87);
    }
}
