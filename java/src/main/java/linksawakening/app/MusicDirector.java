package linksawakening.app;


import linksawakening.audio.music.GameplayMusicController;
import linksawakening.audio.music.FrameMusicPlayer;
import linksawakening.audio.music.MusicTrackIds;
import linksawakening.audio.music.RoomMusicContext;
import linksawakening.state.PlayerState;
import linksawakening.world.ActiveRoom;
import linksawakening.world.RoomSession;


import static linksawakening.world.RoomConstants.*;
import static org.lwjgl.glfw.GLFW.*;


/** Session-local track selection and finite intro continuation. */
public final class MusicDirector {
    private final FrameMusicPlayer musicPlayer;
    private final GameplayMusicController gameplayMusicController;
    private boolean overworldIntroContinuationPending;

    public MusicDirector(FrameMusicPlayer player, GameplayMusicController controller) {
        musicPlayer = player;
        gameplayMusicController = controller;
    }

    public void selectMusicForLoadedRoom(ActiveRoom room, PlayerState playerState, RoomSession roomSession) {
        if (gameplayMusicController == null || playerState == null || room == null) {
            return;
        }
        gameplayMusicController.selectAfterTransition(RoomMusicContext.from(room, playerState));
        if (roomSession != null) {
            roomSession.setEntityDefaultMusicTrack(gameplayMusicController.currentTrackId());
        }
    }

    public void playDirectMusic(int trackId) {
        if (gameplayMusicController != null) {
            gameplayMusicController.playDirect(trackId);
            overworldIntroContinuationPending =
                (trackId & 0xFF) == MusicTrackIds.MUSIC_OVERWORLD_INTRO;
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
            if (overworldIntroContinuationPending
                && gameplayMusicController != null
                && gameplayMusicController.currentTrackId()
                    == MusicTrackIds.MUSIC_OVERWORLD_INTRO
                && !musicPlayer.isMusicPlaying()) {
                overworldIntroContinuationPending = false;
                gameplayMusicController.playDirect(MusicTrackIds.MUSIC_OVERWORLD);
            }
        }
    }
}
