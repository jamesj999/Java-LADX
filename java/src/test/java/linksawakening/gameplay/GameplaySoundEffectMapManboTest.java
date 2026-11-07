package linksawakening.gameplay;

import linksawakening.audio.sfx.SoundEffect;
import linksawakening.audio.sfx.SoundEffectCatalog;
import linksawakening.audio.sfx.SoundEffectNamespace;
import org.junit.jupiter.api.Test;

import java.io.IOException;

import static org.junit.jupiter.api.Assertions.assertEquals;

@linksawakening.testing.RequiresRom
final class GameplaySoundEffectMapManboTest {

    @Test
    void mapsTheRomManboWarpJingle() throws IOException {
        GameplaySoundEffectMap map = GameplaySoundEffectMap.fromCatalog(
            SoundEffectCatalog.fromRom(loadRom()));

        SoundEffect effect = map.resolve(GameplaySoundEvent.MANBO_WARP).orElseThrow();

        assertEquals(SoundEffectNamespace.JINGLE, effect.namespace());
        assertEquals(0x2C, effect.id());
        assertEquals("JINGLE_MANBO_WARP", effect.name());
    }

    private static byte[] loadRom() throws IOException {
        try (var stream = linksawakening.testing.TestRom.open()) {
            if (stream == null) {
                throw new IOException("Missing ROM resource: rom/azle.gbc");
            }
            return stream.readAllBytes();
        }
    }
}
