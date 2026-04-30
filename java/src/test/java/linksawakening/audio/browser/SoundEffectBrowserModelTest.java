package linksawakening.audio.browser;

import linksawakening.audio.sfx.SoundEffect;
import linksawakening.audio.sfx.SoundEffectCatalog;
import linksawakening.audio.sfx.SoundEffectNamespace;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.io.InputStream;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

final class SoundEffectBrowserModelTest {
    @Test
    void filtersSoundEffectsByNameNamespaceOrHexId() throws IOException {
        SoundEffectBrowserModel model = new SoundEffectBrowserModel(
                SoundEffectCatalog.fromRom(loadRom()).effects());

        assertEquals(1, model.filteredEffects("rupee").size());
        assertEquals(65, model.filteredEffects("jingle").size());
        assertEquals(64, model.filteredEffects("noise").size());
        assertEquals(3, model.filteredEffects("0x05").size());
        assertEquals(164, model.filteredEffects("").size());
    }

    @Test
    void metadataIncludesNamespaceIdNameAndRomPointers() throws IOException {
        byte[] romData = loadRom();
        SoundEffectBrowserModel model = new SoundEffectBrowserModel(
                SoundEffectCatalog.fromRom(romData).effects());
        SoundEffect effect = SoundEffectCatalog.fromRom(romData)
                .find(SoundEffectNamespace.NOISE, 0x0C)
                .orElseThrow();

        String metadata = model.metadata(effect);

        assertTrue(metadata.contains("NOISE"));
        assertTrue(metadata.contains("0x0C"));
        assertTrue(metadata.contains("NOISE_SFX_EXPLOSION"));
        assertTrue(metadata.contains("1F:"));
    }

    private static byte[] loadRom() throws IOException {
        try (InputStream stream = SoundEffectBrowserModelTest.class.getClassLoader()
                .getResourceAsStream("rom/azle.gbc")) {
            if (stream == null) {
                throw new IOException("Missing ROM resource: rom/azle.gbc");
            }
            return stream.readAllBytes();
        }
    }
}
