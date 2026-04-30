package linksawakening.audio.browser;

import linksawakening.audio.sfx.SoundEffect;
import linksawakening.audio.sfx.SoundEffectCatalog;
import linksawakening.audio.sfx.SoundEffectNamespace;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

final class SoundEffectBrowserModelTest {
    @Test
    void filtersSoundEffectsByNameNamespaceOrHexId() {
        SoundEffectBrowserModel model = new SoundEffectBrowserModel(SoundEffectCatalog.firstPass().effects());

        assertEquals(1, model.filteredEffects("rupee").size());
        assertEquals(4, model.filteredEffects("noise").size());
        assertEquals(2, model.filteredEffects("0x05").size());
        assertEquals(8, model.filteredEffects("").size());
    }

    @Test
    void metadataIncludesNamespaceIdAndSourceLabel() {
        SoundEffectBrowserModel model = new SoundEffectBrowserModel(SoundEffectCatalog.firstPass().effects());
        SoundEffect effect = SoundEffectCatalog.firstPass()
                .find(SoundEffectNamespace.NOISE, 0x0C)
                .orElseThrow();

        String metadata = model.metadata(effect);

        assertTrue(metadata.contains("NOISE"));
        assertTrue(metadata.contains("0x0C"));
        assertTrue(metadata.contains("NOISE_SFX_EXPLOSION"));
        assertTrue(metadata.contains("sfx.asm"));
    }
}
