package linksawakening.world;

import org.junit.jupiter.api.Test;

import java.io.IOException;

import static org.junit.jupiter.api.Assertions.assertEquals;

final class OwlEventDialogResolverTest {
    @Test
    void resolvesBeachAndTailCaveApproachDialogsFromRomTables() throws IOException {
        OwlEventDialogResolver resolver = new OwlEventDialogResolver(loadRom());

        assertEquals(0x0D9, resolver.globalDialogId(0xF2));
        assertEquals(0x0C2, resolver.globalDialogId(0xD2));
        assertEquals(0x09, resolver.defaultMusicTrack(0x80));
    }

    private static byte[] loadRom() throws IOException {
        try (var stream = OwlEventDialogResolverTest.class.getClassLoader()
            .getResourceAsStream("rom/azle.gbc")) {
            if (stream == null) throw new IOException("Missing ROM resource: rom/azle.gbc");
            return stream.readAllBytes();
        }
    }
}
