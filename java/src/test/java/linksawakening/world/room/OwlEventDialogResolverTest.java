package linksawakening.world.room;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.io.IOException;
import linksawakening.rom.Rom;
import linksawakening.testing.TestRoms;
import org.junit.jupiter.api.Test;

@linksawakening.testing.RequiresRom
final class OwlEventDialogResolverTest {
    @Test
    void resolvesBeachAndTailCaveApproachDialogsFromRomTables() throws IOException {
        OwlEventDialogResolver resolver =
                new OwlEventDialogResolver((loadRom() == null ? (Rom) null : TestRoms.partial(loadRom())));

        assertEquals(0x0D9, resolver.globalDialogId(0xF2));
        assertEquals(0x0C1, resolver.globalDialogId(0x41));
        assertEquals(0x0C2, resolver.globalDialogId(0xD2));
        assertEquals(0x09, resolver.defaultMusicTrack(0x80));
    }

    @Test
    void rejectsRomThatCannotContainTheMusicTable() {
        org.junit.jupiter.api.Assertions.assertThrows(
                IllegalArgumentException.class,
                () -> new OwlEventDialogResolver(
                        (new byte[0x8000] == null ? (Rom) null : TestRoms.partial(new byte[0x8000]))));
    }

    private static byte[] loadRom() throws IOException {
        try (var stream = linksawakening.testing.TestRom.open()) {
            if (stream == null) throw new IOException("Missing ROM resource: rom/azle.gbc");
            return stream.readAllBytes();
        }
    }
}
