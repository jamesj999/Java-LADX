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
