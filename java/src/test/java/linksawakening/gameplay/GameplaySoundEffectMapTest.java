package linksawakening.gameplay;

import linksawakening.audio.sfx.SoundEffect;
import linksawakening.audio.sfx.SoundEffectCatalog;
import linksawakening.audio.sfx.SoundEffectNamespace;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.io.InputStream;

import static org.junit.jupiter.api.Assertions.assertEquals;

final class GameplaySoundEffectMapTest {

    @Test
    void mapsWaterSplashToTheRomWaterSplashJingle() throws IOException {
        GameplaySoundEffectMap map = GameplaySoundEffectMap.fromCatalog(
            SoundEffectCatalog.fromRom(loadRom()));

        SoundEffect effect = map.resolve(GameplaySoundEvent.WATER_SPLASH).orElseThrow();

        assertEquals(SoundEffectNamespace.JINGLE, effect.namespace());
        assertEquals(0x0E, effect.id());
        assertEquals("JINGLE_WATER_SPLASH", effect.name());
    }

    @Test
    void mapsCrystalSwitchToTheRomFloorSwitchWave() throws IOException {
        GameplaySoundEffectMap map = GameplaySoundEffectMap.fromCatalog(
            SoundEffectCatalog.fromRom(loadRom()));

        SoundEffect effect = map.resolve(GameplaySoundEvent.SWITCH_BLOCK_TOGGLE).orElseThrow();

        assertEquals(SoundEffectNamespace.WAVE, effect.namespace());
        assertEquals(0x0E, effect.id());
        assertEquals("WAVE_SFX_FLOOR_SWITCH", effect.name());
    }

    @Test
    void mapsHookshotToTheRomHookshotNoise() throws IOException {
        GameplaySoundEffectMap map = GameplaySoundEffectMap.fromCatalog(
            SoundEffectCatalog.fromRom(loadRom()));

        SoundEffect effect = map.resolve(GameplaySoundEvent.HOOKSHOT).orElseThrow();

        assertEquals(SoundEffectNamespace.NOISE, effect.namespace());
        assertEquals(0x0B, effect.id());
        assertEquals("NOISE_SFX_HOOKSHOT", effect.name());
    }

    @Test
    void mapsBombExplosionToTheRomExplosionNoiseWithoutChangingHookshot() throws IOException {
        GameplaySoundEffectMap map = GameplaySoundEffectMap.fromCatalog(
            SoundEffectCatalog.fromRom(loadRom()));

        SoundEffect effect = map.resolve(GameplaySoundEvent.BOMB_EXPLOSION).orElseThrow();

        assertEquals(SoundEffectNamespace.NOISE, effect.namespace());
        assertEquals(0x0C, effect.id());
        assertEquals("NOISE_SFX_EXPLOSION", effect.name());
        assertEquals(0x0B, map.resolve(GameplaySoundEvent.HOOKSHOT).orElseThrow().id());
    }

    @Test
    void mapsArrowShotToTheRomWhooshNoise() throws IOException {
        GameplaySoundEffectMap map = GameplaySoundEffectMap.fromCatalog(
            SoundEffectCatalog.fromRom(loadRom()));

        SoundEffect effect = map.resolve(GameplaySoundEvent.ARROW_SHOT).orElseThrow();

        assertEquals(SoundEffectNamespace.NOISE, effect.namespace());
        assertEquals(0x0A, effect.id());
        assertEquals("NOISE_SFX_WHOOSH", effect.name());
    }

    private static byte[] loadRom() throws IOException {
        try (InputStream stream = GameplaySoundEffectMapTest.class.getClassLoader()
                .getResourceAsStream("rom/azle.gbc")) {
            if (stream == null) {
                throw new IOException("Missing ROM resource: rom/azle.gbc");
            }
            return stream.readAllBytes();
        }
    }
}
