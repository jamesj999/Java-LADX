package linksawakening.audio.sfx;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.IOException;
import java.io.InputStream;
import linksawakening.audio.apu.GameBoyApu;
import linksawakening.rom.Rom;
import org.junit.jupiter.api.Test;

@linksawakening.testing.RequiresRom
final class SoundEffectPcmRendererTest {
    @Test
    void oneShotRenderStartsCleanAndLeavesApuResetForNextPreview() throws IOException {
        byte[] romData = loadRom();
        SoundEffectCatalog catalog = SoundEffectCatalog.fromRom(Rom.of(romData));
        SoundEffect breakJingle =
                catalog.find(SoundEffectNamespace.JINGLE, 0x15).orElseThrow();
        SoundEffect textPrint = catalog.find(SoundEffectNamespace.WAVE, 0x0F).orElseThrow();
        GameBoyApu reusedApu = new GameBoyApu(44_100);
        SoundEffectPlayer reusedPlayer = new SoundEffectPlayer(reusedApu, Rom.of(romData), catalog);

        short[] breakPcm = SoundEffectPcmRenderer.renderOneShot(reusedApu, reusedPlayer, breakJingle);
        short[] textAfterBreak = SoundEffectPcmRenderer.renderOneShot(reusedApu, reusedPlayer, textPrint);

        GameBoyApu freshApu = new GameBoyApu(44_100);
        SoundEffectPlayer freshPlayer = new SoundEffectPlayer(freshApu, Rom.of(romData), catalog);
        short[] freshText = SoundEffectPcmRenderer.renderOneShot(freshApu, freshPlayer, textPrint);

        assertTrue(hasNonZeroSample(breakPcm));
        assertArrayEquals(freshText, textAfterBreak);
        assertEquals(0x00, reusedApu.readRegister(GameBoyApu.NR52));
        assertEquals(0x00, freshApu.readRegister(GameBoyApu.NR52));
    }

    @Test
    void completedNoiseOneShotsReachSilenceWithoutClippingOrAnAbruptFinalSample() throws IOException {
        byte[] rom = loadRom();
        GameBoyApu apu = new GameBoyApu(48000);
        SoundEffectPlayer player = new SoundEffectPlayer(apu, Rom.of(rom));
        for (int id : new int[] {2, 12}) {
            SoundEffect effect =
                    player.catalog().find(SoundEffectNamespace.NOISE, id).orElseThrow();
            short[] pcm = SoundEffectPcmRenderer.renderOneShot(apu, player, effect);
            assertTrue(hasNonZeroSample(pcm));
            for (short sample : pcm) assertTrue(Math.abs((int) sample) < 32767);
            for (int i = pcm.length - 64; i < pcm.length; i++) assertEquals(0, pcm[i], effect.name());
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

    private static byte[] loadRom() throws IOException {
        try (InputStream stream = linksawakening.testing.TestRom.open()) {
            if (stream == null) {
                throw new IOException("Missing ROM resource: rom/azle.gbc");
            }
            return stream.readAllBytes();
        }
    }
}
