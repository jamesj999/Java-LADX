package linksawakening.audio.music;

import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

@linksawakening.testing.RequiresRom
final class GameplayMusicControllerTest {

    @Test
    void thirdContactHitRestoresDefaultUnlessBossOrOwl() {
        for (int scenario = 0; scenario < 3; scenario++) {
            var controller = new GameplayMusicController(AreaMusicResolver.fromRom(loadRom()),
                MusicCatalog.fromRom(loadRom()), new RecordingMusicTrackPlayer());
            controller.playDirect(0x49);
            var player = new linksawakening.state.PlayerState();
            player.setHealth(24);
            player.setActivePowerUp(1);
            boolean boss = scenario == 1;
            int defaultTrack = scenario == 2 ? 0x22 : MusicTrackIds.MUSIC_MABE_VILLAGE;
            var event = new linksawakening.world.entity.event.EntityProjectileEvent(0, 0x0A,
                linksawakening.world.entity.event.EntityProjectileEvent.Kind.LINK_DAMAGE,
                0xFF, 4, linksawakening.world.entity.event.EntityProjectileEvent.SoundChannel.WAVE,
                3, false, false);
            for (int hit = 0; hit < 3; hit++) {
                player.setInvincibilityCounter(0);
                linksawakening.gameplay.EnemyProjectileEventConsumer.consume(List.of(event), player,
                    null, linksawakening.gameplay.GameplaySoundSink.none(), () -> {},
                    () -> controller.restoreAfterPowerUpEnds(boss, defaultTrack));
                if (hit < 2) assertEquals(0x49, controller.currentTrackId());
            }
            assertEquals(0, player.activePowerUp());
            assertEquals(scenario == 0 ? defaultTrack : 0x49, controller.currentTrackId());
            assertEquals(boss ? MusicTrackIds.MUSIC_NONE : defaultTrack, controller.nextDefaultMusicTrack());
        }
    }

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

    @Test
    void sourceFadeTimerStopsTheCurrentTrackWhenItsCountdownExpires() {
        MusicCatalog catalog = MusicCatalog.fromRom(loadRom());
        RecordingMusicTrackPlayer player = new RecordingMusicTrackPlayer();
        GameplayMusicController controller = new GameplayMusicController(
            AreaMusicResolver.fromRom(loadRom()), catalog, player);
        controller.playDirect(MusicTrackIds.MUSIC_INSIDE_BUILDING);

        controller.requestFadeOut(0x30);
        for (int frame = 0; frame < 0x2F; frame++) {
            controller.tickFadeOut();
        }
        assertEquals(1, controller.fadeOutCountdown());
        assertEquals(0, player.stopCount);

        controller.tickFadeOut();

        assertEquals(0, controller.fadeOutCountdown());
        assertEquals(1, player.stopCount);
        assertEquals(MusicTrackIds.MUSIC_NONE, controller.currentTrackId());
    }

    private static byte[] loadRom() {
        try (var stream = linksawakening.testing.TestRom.open()) {
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
