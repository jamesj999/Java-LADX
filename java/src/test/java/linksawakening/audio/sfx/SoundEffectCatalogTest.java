package linksawakening.audio.sfx;

import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.io.InputStream;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

final class SoundEffectCatalogTest {
    @Test
    void fromRomCatalogIncludesAllNamedEffects() throws IOException {
        SoundEffectCatalog catalog = SoundEffectCatalog.fromRom(loadRom());
        List<SoundEffect> effects = catalog.effects();

        assertEquals(164, effects.size());
        assertEffect(catalog, SoundEffectNamespace.JINGLE, 0x0A, "JINGLE_MOVE_SELECTION");
        assertEffect(catalog, SoundEffectNamespace.JINGLE, 0x15, "JINGLE_DIALOG_BREAK");
        assertEffect(catalog, SoundEffectNamespace.NOISE, 0x02, "NOISE_SFX_SWORD_SWING_A");
        assertEffect(catalog, SoundEffectNamespace.NOISE, 0x05, "NOISE_SFX_CUT_GRASS");
        assertEffect(catalog, SoundEffectNamespace.NOISE, 0x09, "NOISE_SFX_POT_SMASHED");
        assertEffect(catalog, SoundEffectNamespace.NOISE, 0x0C, "NOISE_SFX_EXPLOSION");
        assertEffect(catalog, SoundEffectNamespace.WAVE, 0x03, "WAVE_SFX_LINK_HURT");
        assertEffect(catalog, SoundEffectNamespace.WAVE, 0x04, "WAVE_SFX_LOW_HEARTS");
        assertEffect(catalog, SoundEffectNamespace.WAVE, 0x05, "WAVE_SFX_RUPEE");
        assertEffect(catalog, SoundEffectNamespace.WAVE, 0x0F, "WAVE_SFX_TEXT_PRINT");
    }

    @Test
    void fromRomCatalogCoversAllSfxIds() throws IOException {
        SoundEffectCatalog catalog = SoundEffectCatalog.fromRom(loadRom());

        assertEquals(65, catalog.effects().stream()
                .filter(effect -> effect.namespace() == SoundEffectNamespace.JINGLE)
                .count());
        assertEquals(35, catalog.effects().stream()
                .filter(effect -> effect.namespace() == SoundEffectNamespace.WAVE)
                .count());
        assertEquals(64, catalog.effects().stream()
                .filter(effect -> effect.namespace() == SoundEffectNamespace.NOISE)
                .count());

        // JINGLE 0x01 = id 1, index 0: begin ptr 1F:4100, continue ptr 1F:4182
        assertMapped(catalog, SoundEffectNamespace.JINGLE, 0x01, "1F:4100", "1F:4182");
        // WAVE 0x23 = id 35, index 34: begin ptr 1F:545F, continue ptr 1F:54A5
        assertMapped(catalog, SoundEffectNamespace.WAVE, 0x23, "1F:545F", "1F:54A5");
        // NOISE 0x40 = id 64, index 63: begin ptr 1F:646A, continue ptr 1F:64EA
        assertMapped(catalog, SoundEffectNamespace.NOISE, 0x40, "1F:646A", "1F:64EA");
        assertFalse(catalog.find(SoundEffectNamespace.WAVE, 0x00).isPresent());
        assertFalse(catalog.find(SoundEffectNamespace.NOISE, 0x41).isPresent());
    }

    @Test
    void fromRomBuildsSourceFromRomPointerAddresses() throws IOException {
        byte[] romData = loadRom();
        SoundEffectCatalog catalog = SoundEffectCatalog.fromRom(romData);

        // JINGLE 0x15 = id 21, index 20: begin ptr 1F:4128, continue ptr 1F:41AA
        SoundEffect dialogBreak = catalog.find(SoundEffectNamespace.JINGLE, 0x15).orElseThrow();
        assertTrue(dialogBreak.source().contains("1F:4128"), dialogBreak.source());
        assertTrue(dialogBreak.source().contains("1F:41AA"), dialogBreak.source());
        assertTrue(dialogBreak.source().contains("1F:4701"), dialogBreak.source());
        assertTrue(dialogBreak.source().contains("1F:4707"), dialogBreak.source());
        assertFalse(dialogBreak.source().contains("LADX-Disassembly"), dialogBreak.source());
        assertFalse(dialogBreak.source().contains("BeginJingle"), dialogBreak.source());
        assertFalse(dialogBreak.source().contains("sfx.asm"), dialogBreak.source());

        // WAVE 0x0F = id 15, index 14: begin ptr 1F:5437, continue ptr 1F:547D
        SoundEffect textPrint = catalog.find(SoundEffectNamespace.WAVE, 0x0F).orElseThrow();
        assertTrue(textPrint.source().contains("1F:5437"), textPrint.source());
        assertTrue(textPrint.source().contains("1F:547D"), textPrint.source());

        // NOISE 0x0C = id 12, index 11: begin ptr 1F:6402, continue ptr 1F:6482
        SoundEffect explosion = catalog.find(SoundEffectNamespace.NOISE, 0x0C).orElseThrow();
        assertTrue(explosion.source().contains("1F:6402"), explosion.source());
        assertTrue(explosion.source().contains("1F:6482"), explosion.source());
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
        assertTrue(effect.source().contains("1F:"), effect.source());
    }

    private static void assertMapped(
            SoundEffectCatalog catalog,
            SoundEffectNamespace namespace,
            int id,
            String beginPtr,
            String continuePtr) {
        SoundEffect effect = catalog.find(namespace, id).orElseThrow();

        assertTrue(effect.source().contains("begin=" + beginPtr), effect.source());
        assertTrue(effect.source().contains("continue=" + continuePtr), effect.source());
    }

    private static byte[] loadRom() throws IOException {
        try (InputStream stream = SoundEffectCatalogTest.class.getClassLoader()
                .getResourceAsStream("rom/azle.gbc")) {
            if (stream == null) {
                throw new IOException("Missing ROM resource: rom/azle.gbc");
            }
            return stream.readAllBytes();
        }
    }
}
