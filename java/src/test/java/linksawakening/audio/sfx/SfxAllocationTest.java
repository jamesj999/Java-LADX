package linksawakening.audio.sfx;

import static org.junit.jupiter.api.Assertions.assertTrue;

import linksawakening.audio.apu.GameBoyApu;
import linksawakening.rom.Rom;
import linksawakening.rom.RomSymbols;
import linksawakening.testing.AllocationProbe;
import org.junit.jupiter.api.Test;

final class SfxAllocationTest {
    @Test
    void activeSfxTickDoesNotAllocate() {
        byte[] bytes = new byte[0x80000];
        // Synthetic handlers emit a continuously retriggered noise voice through the real CPU/bus.
        byte[] code = {
            0x3e,
            (byte) 0xf0,
            (byte) 0xe0,
            0x21,
            0x3e,
            0x21,
            (byte) 0xe0,
            0x22,
            0x3e,
            (byte) 0x80,
            (byte) 0xe0,
            0x23,
            (byte) 0xc9
        };
        for (var address : new linksawakening.rom.RomAddress[] {
            RomSymbols.PlayActiveJingle, RomSymbols.PlayActiveWaveSfx, RomSymbols.PlayActiveNoiseSfx
        }) System.arraycopy(code, 0, bytes, address.offset(), code.length);
        var apu = new GameBoyApu(48000);
        apu.writeRegister(GameBoyApu.NR52, 0x80);
        apu.writeRegister(GameBoyApu.NR50, 0x77);
        apu.writeRegister(GameBoyApu.NR51, 0xff);
        var engine = new RomSoundEffectEngine(Rom.of(bytes), apu);
        engine.write8(0xd379, 1);
        short[] samples = new short[1600];
        double allocated = AllocationProbe.bytesPerIteration(5000, 1000, () -> {
            engine.tick60Hz();
            apu.renderInto(samples, 0, 800);
        });
        assertTrue(engine.isPlaying(), "measurement must retain an active effect");
        assertTrue(apu.isChannelActive(4), "synthetic handlers must emit a live voice");
        System.out.println("Active SFX allocation bytes/tick=" + allocated);
        assertTrue(allocated < 16, "SFX allocated " + allocated + " bytes/tick");
    }
}
