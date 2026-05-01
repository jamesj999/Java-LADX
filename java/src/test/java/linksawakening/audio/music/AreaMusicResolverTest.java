package linksawakening.audio.music;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

final class AreaMusicResolverTest {

    @Test
    void resolvesRepresentativeOverworldRoomsFromRomTable() {
        AreaMusicResolver resolver = AreaMusicResolver.fromRom(loadRom());

        assertEquals(MusicTrackIds.MUSIC_MABE_VILLAGE,
            resolver.resolve(RoomMusicContext.overworld(0x92, 1)));
        assertEquals(MusicTrackIds.MUSIC_MYSTERIOUS_FOREST,
            resolver.resolve(RoomMusicContext.overworld(0x40, 1)));
        assertEquals(MusicTrackIds.MUSIC_ANIMAL_VILLAGE,
            resolver.resolve(RoomMusicContext.overworld(0xCC, 1)));
        assertEquals(MusicTrackIds.MUSIC_TAL_TAL_RANGE,
            resolver.resolve(RoomMusicContext.overworld(0x00, 1)));
    }

    @Test
    void swordlessOverworldUsesAdventureStartMusicBeforeRoomTable() {
        AreaMusicResolver resolver = AreaMusicResolver.fromRom(loadRom());

        assertEquals(MusicTrackIds.MUSIC_OVERWORLD_SWORDLESS,
            resolver.resolve(RoomMusicContext.overworld(0x92, 0)));
    }

    @Test
    void resolvesIndoorMapsFromHouseMusicTable() {
        AreaMusicResolver resolver = AreaMusicResolver.fromRom(loadRom());

        assertEquals(MusicTrackIds.MUSIC_TAIL_CAVE,
            resolver.resolve(RoomMusicContext.indoor(0x00, 0x00, 1)));
        assertEquals(MusicTrackIds.MUSIC_CAVE,
            resolver.resolve(RoomMusicContext.indoor(0x0A, 0x00, 1)));
        assertEquals(MusicTrackIds.MUSIC_SHOP,
            resolver.resolve(RoomMusicContext.indoor(0x0E, 0x00, 1)));
        assertEquals(MusicTrackIds.MUSIC_INSIDE_BUILDING,
            resolver.resolve(RoomMusicContext.indoor(0x10, 0x00, 1)));
        assertEquals(MusicTrackIds.MUSIC_COLOR_DUNGEON,
            resolver.resolve(RoomMusicContext.indoor(0xFF, 0x00, 1)));
    }

    @Test
    void sideScrollingDungeonMapsUseTwoDUndergroundMusic() {
        AreaMusicResolver resolver = AreaMusicResolver.fromRom(loadRom());

        assertEquals(MusicTrackIds.MUSIC_2D_UNDERGROUND,
            resolver.resolve(RoomMusicContext.sideScrolling(0x00, 0x00, 1)));
        assertEquals(MusicTrackIds.MUSIC_2D_UNDERGROUND,
            resolver.resolve(RoomMusicContext.sideScrolling(0x09, 0x00, 1)));
        assertEquals(MusicTrackIds.MUSIC_CAVE,
            resolver.resolve(RoomMusicContext.sideScrolling(0x0A, 0x00, 1)));
        assertEquals(MusicTrackIds.MUSIC_CAVE,
            resolver.resolve(RoomMusicContext.sideScrolling(0x11, 0x00, 1)));
    }

    private static byte[] loadRom() {
        try (var stream = AreaMusicResolverTest.class.getClassLoader().getResourceAsStream("rom/azle.gbc")) {
            if (stream == null) {
                throw new IllegalStateException("ROM resource missing");
            }
            return stream.readAllBytes();
        } catch (Exception e) {
            throw new IllegalStateException("Failed to load ROM", e);
        }
    }
}
