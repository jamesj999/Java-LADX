package linksawakening.audio.sfx;

import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

final class SoundEffectCatalogTest {
    @Test
    void firstPassCatalogIncludesTraceableRecommendedEffects() {
        SoundEffectCatalog catalog = SoundEffectCatalog.firstPass();
        List<SoundEffect> effects = catalog.effects();

        assertEquals(8, effects.size());
        assertEffect(catalog, SoundEffectNamespace.NOISE, 0x02, "NOISE_SFX_SWORD_SWING_A");
        assertEffect(catalog, SoundEffectNamespace.NOISE, 0x05, "NOISE_SFX_CUT_GRASS");
        assertEffect(catalog, SoundEffectNamespace.NOISE, 0x09, "NOISE_SFX_POT_SMASHED");
        assertEffect(catalog, SoundEffectNamespace.NOISE, 0x0C, "NOISE_SFX_EXPLOSION");
        assertEffect(catalog, SoundEffectNamespace.WAVE, 0x03, "WAVE_SFX_LINK_HURT");
        assertEffect(catalog, SoundEffectNamespace.WAVE, 0x04, "WAVE_SFX_LOW_HEARTS");
        assertEffect(catalog, SoundEffectNamespace.WAVE, 0x05, "WAVE_SFX_RUPEE");
        assertEffect(catalog, SoundEffectNamespace.WAVE, 0x0F, "WAVE_SFX_TEXT_PRINT");
    }

    private static void assertEffect(
            SoundEffectCatalog catalog,
            SoundEffectNamespace namespace,
            int id,
            String name) {
        SoundEffect effect = catalog.find(namespace, id).orElseThrow();

        assertEquals(namespace, effect.namespace());
        assertEquals(id, effect.id());
        assertEquals(name, effect.name());
        assertTrue(effect.source().contains("constants/sfx.asm"));
    }
}
