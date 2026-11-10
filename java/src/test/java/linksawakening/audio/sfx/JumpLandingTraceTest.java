package linksawakening.audio.sfx;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import java.util.ArrayList;
import java.util.List;
import static org.junit.jupiter.api.Assertions.*;
import static linksawakening.audio.sfx.SfxRegisterTrace.*;

@linksawakening.testing.RequiresRom
class JumpLandingTraceTest {
    private static byte[] rom() throws Exception {
        try (var stream = linksawakening.testing.TestRom.open()) { return stream.readAllBytes(); }
    }

    @ParameterizedTest
    @ValueSource(ints = {8, 0x0D, 0x24})
    void jumpJinglesMatchTheirFrameHandTrace(int id) throws Exception {
        var trace = new SfxRegisterTrace(rom());
        trace.start(SoundEffectNamespace.JINGLE, id);
        var expected = new ArrayList<SfxRegisterTrace.Write>();
        // JumpDown 1F:447D, FeatherJump 1F:459B, Jump 1F:4AD9.
        if (id == 8) add(expected, 0, 0x10, 0x3F, 0x9E, 0x29, 0x80, 0xC7);
        else add(expected, 0, 0x10, 0x27, 0x80, id == 0x0D ? 0xC2 : 0x83, id == 0x0D ? 0x48 : 0, 0x86);
        for (int frame = 1; frame <= 24; frame++) {
            short[] pcm = trace.advance();
            if (frame == 1) assertTrue(varying(pcm));
            if (id == 8 && frame == 8) add(expected, frame, 0x10, 0x1F, 0x9F, 0x81, 0x20, 0x87);
        }
        add(expected, 24, 0x10, 0); // func_01F_53BB.
        assertEquals(expected, trace.writes());
        assertFalse(trace.player.isPlaying());
    }

    @Test void landingFootstepMatchesNoiseHandTrace() throws Exception {
        var trace = new SfxRegisterTrace(rom());
        trace.start(SoundEffectNamespace.NOISE, 7);
        var expected = new ArrayList<SfxRegisterTrace.Write>();
        // func_01F_66A2 / 66C4, Data_01F_66D6 / 66DB.
        add(expected, 0, 0x20, 0x3B, 0xF0, 0x20, 0xC0);
        for (int frame = 1; frame <= 10; frame++) {
            short[] pcm = trace.advance();
            if (frame == 1) assertTrue(varying(pcm));
            if (frame == 2) add(expected, frame, 0x20, 0x39, 0x40, 0x20, 0xC0);
        }
        assertEquals(expected, trace.writes());
        assertFalse(trace.player.isPlaying());
        assertFalse(trace.player.ownsChannel(4));
        assertFalse(trace.apu.isChannelActive(4));
    }

    @Test void landingNoiseDoesNotAbandonTheLedgeJumpJingle() throws Exception {
        byte[] data = rom();
        var jump = new SfxRegisterTrace(data);
        var landing = new SfxRegisterTrace(data);
        var combined = new SfxRegisterTrace(data);
        jump.start(SoundEffectNamespace.JINGLE, 8);
        combined.start(SoundEffectNamespace.JINGLE, 8);
        for (int frame = 1; frame <= 24; frame++) {
            jump.advance();
            landing.advance();
            combined.advance();
            if (frame == 10) {
                landing.start(SoundEffectNamespace.NOISE, 7);
                combined.start(SoundEffectNamespace.NOISE, 7);
            }
            if (frame == 20) {
                assertTrue(combined.player.isPlaying());
                assertTrue(combined.player.ownsChannel(1));
                assertFalse(combined.player.ownsChannel(4));
            }
        }
        assertEquals(jump.writes(), combined.writes().stream().filter(w -> w.register() <= 0x14).toList());
        assertEquals(landing.writes(), combined.writes().stream().filter(w -> w.register() >= 0x20).toList());
        assertFalse(combined.player.isPlaying());
    }

}
