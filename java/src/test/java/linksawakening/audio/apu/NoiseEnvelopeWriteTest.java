package linksawakening.audio.apu;

import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class NoiseEnvelopeWriteTest {
    @Test void initialVolumeWriteWaitsForTriggerButDacOffTakesEffectImmediately() {
        GameBoyApu apu = new GameBoyApu(48000);
        apu.writeRegister(GameBoyApu.NR52, 0x80);
        apu.writeRegister(GameBoyApu.NR50, 0x77);
        apu.writeRegister(GameBoyApu.NR51, 0x88);
        apu.writeRegister(GameBoyApu.NR42, 0xF0);
        apu.writeRegister(GameBoyApu.NR43, 3);
        apu.writeRegister(GameBoyApu.NR44, 0x80);
        assertEquals(4096, peak(apu.render(1600)));

        // Same disabled envelope/direction; avoids model-specific live-write quirks.
        apu.writeRegister(GameBoyApu.NR42, 0x30);
        assertEquals(0x30, apu.readRegister(GameBoyApu.NR42));
        assertTrue(apu.isChannelActive(4));
        assertEquals(4096, peak(apu.render(1600)), "NR42 must not reload live volume");

        apu.writeRegister(GameBoyApu.NR44, 0x80);
        assertEquals(819, peak(apu.render(1600)), "Trigger loads initial volume 3/15");
        apu.writeRegister(GameBoyApu.NR42, 0);
        assertFalse(apu.isChannelActive(4));
        assertEquals(0, peak(apu.render(1600)));
    }
    private static int peak(short[] pcm) {
        int peak = 0;
        for (short sample : pcm) peak = Math.max(peak, Math.abs((int) sample));
        return peak;
    }
}
