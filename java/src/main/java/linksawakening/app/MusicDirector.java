package linksawakening.app;

import linksawakening.audio.music.FrameMusicPlayer;
import linksawakening.audio.music.GameplayMusicController;
import linksawakening.audio.music.MusicTrackIds;
import linksawakening.audio.music.RoomMusicContext;
import linksawakening.state.PlayerState;
import linksawakening.world.room.ActiveRoom;
import linksawakening.world.room.RoomSession;

/** Session-local track selection, preserving ROM area and event track ownership. */
public final class MusicDirector {
    private final FrameMusicPlayer musicPlayer;
    private final GameplayMusicController gameplayMusicController;

    public MusicDirector(FrameMusicPlayer player, GameplayMusicController controller) {
        musicPlayer = player;
        gameplayMusicController = controller;
    }

    public void selectMusicForLoadedRoom(ActiveRoom room, PlayerState playerState, RoomSession roomSession) {
        if (gameplayMusicController == null || playerState == null || room == null) {
            return;
        }
        RoomMusicContext context = RoomMusicContext.from(room, playerState);
        if (roomSession != null && roomSession.rooms().musicSelectionIsScroll()) context = context.forScroll();
        gameplayMusicController.selectAfterTransition(context);
        if (roomSession != null) {
            roomSession.story().setEntityDefaultMusicTrack(gameplayMusicController.defaultTrackId());
        }
    }

    public void restoreMusicAfterPowerUpEnds(RoomSession roomSession) {
        if (gameplayMusicController != null && roomSession != null) {
            gameplayMusicController.restoreAfterPowerUpEnds(
                    roomSession.entities().inBossBattle(),
                    roomSession.entityState().entityDefaultMusicTrack());
        }
    }

    public void playDirectMusic(int trackId) {
        if (gameplayMusicController != null) {
            gameplayMusicController.playDirect(trackId);
        }
    }

    public static int introCutsceneMusicTrack() {
        return MusicTrackIds.MUSIC_TITLE_CUTSCENE;
    }

    public static int naturalIntroTitleMusicTrack() {
        return MusicTrackIds.MUSIC_TITLE_SCREEN;
    }

    public static int newGameHouseMusicTrack() {
        return MusicTrackIds.MUSIC_INTRO_WAKE_UP;
    }

    public static int directTitleScreenMusicTrack() {
        return MusicTrackIds.MUSIC_TITLE_SCREEN_NO_INTRO;
    }

    public static int fileSelectionMusicTrack() {
        return MusicTrackIds.MUSIC_FILE_SELECT;
    }

    public void tick() {
        if (musicPlayer != null) {
            musicPlayer.update();
            if (gameplayMusicController != null) {
                gameplayMusicController.tickFadeOut();
            }
        }
    }
}
