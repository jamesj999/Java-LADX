package linksawakening.audio.music;

import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

final class GameplayMusicControllerTest {

    @Test
    void startsResolvedTrackAfterRoomLoad() {
        MusicCatalog catalog = MusicCatalog.fromRom(loadRom());
        RecordingMusicTrackPlayer player = new RecordingMusicTrackPlayer();
        GameplayMusicController controller = new GameplayMusicController(
            AreaMusicResolver.fromRom(loadRom()), catalog, player);

        controller.selectAfterTransition(RoomMusicContext.overworld(0x92, 1));

        assertEquals(List.of(MusicTrackIds.MUSIC_MABE_VILLAGE), player.playedTrackIds);
    }

    @Test
    void doesNotRestartWhenResolvedTrackIsAlreadyCurrent() {
        MusicCatalog catalog = MusicCatalog.fromRom(loadRom());
        RecordingMusicTrackPlayer player = new RecordingMusicTrackPlayer();
        GameplayMusicController controller = new GameplayMusicController(
            AreaMusicResolver.fromRom(loadRom()), catalog, player);

        controller.selectAfterTransition(RoomMusicContext.overworld(0x92, 1));
        controller.selectAfterTransition(RoomMusicContext.overworld(0x93, 1));

        assertEquals(List.of(MusicTrackIds.MUSIC_MABE_VILLAGE), player.playedTrackIds);
    }

    @Test
    void continueMusicAfterWarpSkipsOneSelectionAndThenClearsFlag() {
        MusicCatalog catalog = MusicCatalog.fromRom(loadRom());
        RecordingMusicTrackPlayer player = new RecordingMusicTrackPlayer();
        GameplayMusicController controller = new GameplayMusicController(
            AreaMusicResolver.fromRom(loadRom()), catalog, player);

        controller.continueMusicAfterNextWarp();
        controller.selectAfterTransition(RoomMusicContext.indoor(0x10, 0x00, 1));
        controller.selectAfterTransition(RoomMusicContext.indoor(0x10, 0x00, 1));

        assertEquals(List.of(MusicTrackIds.MUSIC_INSIDE_BUILDING), player.playedTrackIds);
    }

    @Test
    void canResolveSilenceWithoutOpeningUnknownCatalogTrack() {
        MusicCatalog catalog = MusicCatalog.fromRom(loadRom());
        RecordingMusicTrackPlayer player = new RecordingMusicTrackPlayer();
        GameplayMusicController controller = new GameplayMusicController(
            AreaMusicResolver.fromRom(loadRom()), catalog, player);

        controller.selectAfterTransition(RoomMusicContext.indoor(0x17, 0x00, 1));

        assertTrue(player.playedTrackIds.isEmpty());
        assertEquals(MusicTrackIds.MUSIC_NONE, controller.currentTrackId());
    }

    @Test
    void directScreenMusicStartsRequestedTrackAndDoesNotRestartIt() {
        MusicCatalog catalog = MusicCatalog.fromRom(loadRom());
        RecordingMusicTrackPlayer player = new RecordingMusicTrackPlayer();
        GameplayMusicController controller = new GameplayMusicController(
            AreaMusicResolver.fromRom(loadRom()), catalog, player);

        controller.playDirect(MusicTrackIds.MUSIC_TITLE_CUTSCENE);
        controller.playDirect(MusicTrackIds.MUSIC_TITLE_CUTSCENE);
        controller.playDirect(MusicTrackIds.MUSIC_TITLE_SCREEN);

        assertEquals(List.of(
            MusicTrackIds.MUSIC_TITLE_CUTSCENE,
            MusicTrackIds.MUSIC_TITLE_SCREEN
        ), player.playedTrackIds);
        assertEquals(MusicTrackIds.MUSIC_TITLE_SCREEN, controller.currentTrackId());
    }

    @Test
    void directSilenceStopsMusic() {
        MusicCatalog catalog = MusicCatalog.fromRom(loadRom());
        RecordingMusicTrackPlayer player = new RecordingMusicTrackPlayer();
        GameplayMusicController controller = new GameplayMusicController(
            AreaMusicResolver.fromRom(loadRom()), catalog, player);

        controller.playDirect(MusicTrackIds.MUSIC_TITLE_SCREEN_NO_INTRO);
        controller.playDirect(MusicTrackIds.MUSIC_NONE);

        assertEquals(List.of(MusicTrackIds.MUSIC_TITLE_SCREEN_NO_INTRO), player.playedTrackIds);
        assertEquals(1, player.stopCount);
        assertEquals(MusicTrackIds.MUSIC_NONE, controller.currentTrackId());
    }

    private static byte[] loadRom() {
        try (var stream = GameplayMusicControllerTest.class.getClassLoader().getResourceAsStream("rom/azle.gbc")) {
            if (stream == null) {
                throw new IllegalStateException("ROM resource missing");
            }
            return stream.readAllBytes();
        } catch (Exception e) {
            throw new IllegalStateException("Failed to load ROM", e);
        }
    }

    private static final class RecordingMusicTrackPlayer implements MusicTrackPlayer {
        private final List<Integer> playedTrackIds = new ArrayList<>();
        private int stopCount;

        @Override
        public void play(MusicTrack track) {
            playedTrackIds.add(track.id());
        }

        @Override
        public void stop() {
            stopCount++;
        }
    }
}
