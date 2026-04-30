package linksawakening.audio.sfx;

import linksawakening.audio.apu.GameBoyApu;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.io.InputStream;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

final class SoundEffectPcmRendererTest {
    @Test
    void oneShotRenderStartsCleanAndLeavesApuResetForNextPreview() throws IOException {
        byte[] romData = loadRom();
        SoundEffectCatalog catalog = SoundEffectCatalog.fromRom(romData);
        SoundEffect breakJingle = catalog.find(SoundEffectNamespace.JINGLE, 0x15).orElseThrow();
        SoundEffect textPrint = catalog.find(SoundEffectNamespace.WAVE, 0x0F).orElseThrow();
        GameBoyApu reusedApu = new GameBoyApu(44_100);
        SoundEffectPlayer reusedPlayer = new SoundEffectPlayer(reusedApu, romData, catalog);

        short[] breakPcm = SoundEffectPcmRenderer.renderOneShot(reusedApu, reusedPlayer, breakJingle);
        short[] textAfterBreak = SoundEffectPcmRenderer.renderOneShot(reusedApu, reusedPlayer, textPrint);

        GameBoyApu freshApu = new GameBoyApu(44_100);
        SoundEffectPlayer freshPlayer = new SoundEffectPlayer(freshApu, romData, catalog);
        short[] freshText = SoundEffectPcmRenderer.renderOneShot(freshApu, freshPlayer, textPrint);

        assertTrue(hasNonZeroSample(breakPcm));
        assertArrayEquals(freshText, textAfterBreak);
        assertEquals(0x00, reusedApu.readRegister(GameBoyApu.NR52));
        assertEquals(0x00, freshApu.readRegister(GameBoyApu.NR52));
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
        try (InputStream stream = SoundEffectPcmRendererTest.class.getClassLoader()
                .getResourceAsStream("rom/azle.gbc")) {
            if (stream == null) {
                throw new IOException("Missing ROM resource: rom/azle.gbc");
            }
            return stream.readAllBytes();
        }
    }
}
