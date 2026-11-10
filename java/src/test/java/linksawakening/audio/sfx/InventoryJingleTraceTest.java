package linksawakening.audio.sfx;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import java.util.ArrayList;
import java.util.List;
import static org.junit.jupiter.api.Assertions.*;
import static linksawakening.audio.sfx.SfxRegisterTrace.*;
import linksawakening.audio.apu.GameBoyApu;
import linksawakening.audio.music.MusicDriver;
import linksawakening.audio.music.MusicTrack;
import linksawakening.rom.RomBank;

@linksawakening.testing.RequiresRom
class InventoryJingleTraceTest {
    private static byte[] rom() throws Exception {
        try (var stream = linksawakening.testing.TestRom.open()) { return stream.readAllBytes(); }
    }

    @ParameterizedTest
    @ValueSource(ints = {0x11, 0x12})
    void inventoryJinglesMatchEveryFrameOfTheirTwoPulseChannelHandTrace(int id) throws Exception {
        var trace = new SfxRegisterTrace(rom());
        trace.start(SoundEffectNamespace.JINGLE, id);
        boolean open = id == 0x11;
        int end = open ? 24 : 9;
        var expected = new ArrayList<SfxRegisterTrace.Write>();
        // BeginJingleOpenInventory 1F:45F3 / BeginJingleCloseInventory 1F:464B.
        // Both own CH1 AND CH2. 7AD0/7AB7 save the initial frequencies.
        add(expected, 0, 0x16, open ? 0x45 : 0x40, open ? 0x0C : 0x0A, open ? 0 : 0xB0, open ? 0x84 : 0x87);
        add(expected, 0, 0x10, 0, open ? 0x85 : 0x80, open ? 0x0B : 0x09, open ? 0 : 0xC0, open ? 0x81 : 0x86);
        int first = open ? 0x100 : 0x6C0;
        int second = open ? 0x400 : 0x7B0;
        int delta = open ? 0x21 : -0x30;
        for (int frame = 1; frame < end; frame++) {
            trace.advance();
            first = (first + delta) & 0xFFFF;
            second = (second + delta) & 0xFFFF;
            // Continue handlers 1F:460F / 466F call 7B11 then 7AE1.
            // The delta table is big endian into BC, not a little endian pointer.
            add(expected, frame, 0x18, second & 255, second >>> 8);
            add(expected, frame, 0x13, first & 255, first >>> 8);
        }
        trace.advance();
        // label_01F_463D -> 53B5 -> 53BB: explicit silent length-trigger writes.
        add(expected, end, 0x16, 0x3F, 0, 0, 0xC1);
        add(expected, end, 0x10, 0, 0x3F, 0, 0, 0xC1);
        add(expected, end, 0x10, 0);
        assertEquals(expected, trace.writes());
        assertFalse(trace.player.isPlaying());
        assertFalse(trace.player.ownsChannel(1));
        assertFalse(trace.player.ownsChannel(2));
    }

    @ParameterizedTest
    @ValueSource(ints = {0x11, 0x12})
    void inventoryBendsAndEnvelopesSurviveConcurrentMusic(int id) throws Exception {
        byte[] data = rom();
        put(data, 0x5000, 0, 0, 0x51, 0, 0x52, 0x10, 0x52, 0, 0, 0, 0);
        put(data, 0x5100, 1);
        put(data, 0x5200, 0, 0x53, 0xFF, 0xFF, 0, 0x52);
        put(data, 0x5210, 0x20, 0x53, 0xFF, 0xFF, 0x10, 0x52);
        for (int address : new int[] {0x5300, 0x5320}) {
            put(data, address, 0x9D, 0xF0, 0x11, 0x80, 0xA0, 0x4A, 1, 0);
        }
        var isolated = new SfxRegisterTrace(data);
        var mixed = new SfxRegisterTrace(data);
        var music = new MusicDriver(data, mixed.apu);
        music.setSoundEffectState(mixed.player::ownsChannel, mixed.player::takeWaveRestoreRequest);
        music.start(new MusicTrack(1, "TEST", 0x1B, 0x4077, 0x5000, RomBank.romOffset(0x1B, 0x5000)));
        music.tick60Hz();
        mixed.clearWrites();
        isolated.start(SoundEffectNamespace.JINGLE, id);
        mixed.start(SoundEffectNamespace.JINGLE, id);
        int end = id == 0x11 ? 24 : 9;
        boolean audibleAfterEnvelopeAttack = false;
        for (int frame = 1; frame <= end; frame++) {
            music.tick60Hz();
            short[] pcm = mixed.advance();
            isolated.advance();
            if (frame >= 4) audibleAfterEnvelopeAttack |= varying(pcm);
        }
        assertTrue(audibleAfterEnvelopeAttack, "zero-initial-volume increasing envelopes must become audible");
        assertEquals(isolated.writes(), mixed.writes(), "CH1 and CH2 must retain the jingle's complete trace");
        int count = mixed.writes().size();
        music.tick60Hz();
        assertTrue(mixed.writes().size() > count, "music resumes register writes after both channels release");
    }

}
