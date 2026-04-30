package linksawakening.audio.sfx;

import org.junit.jupiter.api.Test;

import java.io.IOException;

import static org.junit.jupiter.api.Assertions.assertEquals;

final class RomSoundEffectTablesTest {
    @Test
    void readsHandlerPointersFromRomTables() throws IOException {
        byte[] romData = loadRom();
        RomSoundEffectTables tables = new RomSoundEffectTables(romData);
        SoundEffectCatalog catalog = SoundEffectCatalog.fromRom(romData);

        SoundEffect dialogBreak = catalog.find(SoundEffectNamespace.JINGLE, 0x15).orElseThrow();
        SoundEffect textPrint = catalog.find(SoundEffectNamespace.WAVE, 0x0F).orElseThrow();
        SoundEffect explosion = catalog.find(SoundEffectNamespace.NOISE, 0x0C).orElseThrow();

        assertEquals(0x4701, tables.beginHandlerAddress(dialogBreak));
        assertEquals(0x4707, tables.continueHandlerAddress(dialogBreak));
        assertEquals(0x5A19, tables.beginHandlerAddress(textPrint));
        assertEquals(0x5A2A, tables.continueHandlerAddress(textPrint));
        assertEquals(0x67B4, tables.beginHandlerAddress(explosion));
        assertEquals(0x67C7, tables.continueHandlerAddress(explosion));
    }

    private static byte[] loadRom() throws IOException {
        try (var stream = RomSoundEffectTablesTest.class.getClassLoader().getResourceAsStream("rom/azle.gbc")) {
            if (stream == null) {
                throw new IOException("Missing ROM resource: rom/azle.gbc");
            }
            return stream.readAllBytes();
        }
    }
}
