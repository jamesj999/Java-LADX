package linksawakening.audio.music;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

final class MusicCatalogTest {
    @Test
    void resolvesRepresentativeTracksFromBothMusicBanks() {
        MusicCatalog catalog = MusicCatalog.fromRom(loadRom());

        assertTrack(catalog.requireTrack(0x01), "MUSIC_TITLE_SCREEN", 0x1B, 0x4077);
        assertTrack(catalog.requireTrack(0x10), "MUSIC_OBTAIN_ITEM", 0x1B, 0x4077 + (0x0F * 2));
        assertTrack(catalog.requireTrack(0x11), "MUSIC_FILE_SELECT", 0x1E, 0x407F);
        assertTrack(catalog.requireTrack(0x50), "MUSIC_MINIBOSS", 0x1E, 0x407F + (0x3F * 2));
        assertTrack(catalog.requireTrack(0x61), "MUSIC_COLOR_DUNGEON", 0x1B, 0x4077 + (0x20 * 2));
    }

    @Test
    void listsPlayableTracksInMusicIdOrder() {
        MusicCatalog catalog = MusicCatalog.fromRom(loadRom());

        assertEquals(0x70, catalog.tracks().size());
        assertEquals(0x01, catalog.tracks().get(0).id());
        assertEquals(0x70, catalog.tracks().get(catalog.tracks().size() - 1).id());
    }

    @Test
    void rejectsUnknownTrackIds() {
        MusicCatalog catalog = MusicCatalog.fromRom(loadRom());

        assertThrows(IllegalArgumentException.class, () -> catalog.requireTrack(0x00));
        assertThrows(IllegalArgumentException.class, () -> catalog.requireTrack(0x71));
    }

    private static void assertTrack(MusicTrack track, String name, int bank, int tableAddress) {
        assertEquals(name, track.name());
        assertEquals(bank, track.bank());
        assertEquals(tableAddress, track.pointerTableAddress());
        assertTrue(track.headerAddress() >= 0x4000 && track.headerAddress() < 0x8000);
        assertTrue(track.romOffset() >= 0);
    }

    private static byte[] loadRom() {
        try (var stream = MusicCatalogTest.class.getClassLoader().getResourceAsStream("rom/azle.gbc")) {
            if (stream == null) {
                throw new IllegalStateException("ROM resource missing");
            }
            return stream.readAllBytes();
        } catch (Exception e) {
            throw new IllegalStateException("Failed to load ROM", e);
        }
    }
}
