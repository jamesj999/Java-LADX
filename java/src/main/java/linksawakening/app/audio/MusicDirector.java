package linksawakening.app.audio;

import java.util.Objects;
import linksawakening.app.story.MusicRequests;
import linksawakening.audio.music.FrameMusicPlayer;
import linksawakening.audio.music.GameplayMusicController;
import linksawakening.audio.music.MusicTrackIds;
import linksawakening.audio.music.RoomMusicContext;
import linksawakening.state.PlayerState;
import linksawakening.world.room.ActiveRoom;
import linksawakening.world.room.RoomSession;

/** Session-local track selection, preserving ROM area and event track ownership. */
public final class MusicDirector implements MusicRequests {
    private final FrameMusicPlayer musicPlayer;
    private final GameplayMusicController gameplayMusicController;

    public MusicDirector(FrameMusicPlayer player, GameplayMusicController controller) {
        musicPlayer = Objects.requireNonNull(player);
        gameplayMusicController = Objects.requireNonNull(controller);
    }

    public void selectMusicForLoadedRoom(ActiveRoom room, PlayerState playerState, RoomSession roomSession) {
        Objects.requireNonNull(playerState, "playerState");
        Objects.requireNonNull(room, "room");
        Objects.requireNonNull(roomSession, "roomSession");
        RoomMusicContext context = RoomMusicContext.from(room, playerState);
        if (roomSession.rooms().musicSelectionIsScroll()) context = context.forScroll();
        int residentTrack = MusicTrackIds.MUSIC_NONE;
        if (context.indoor() && context.swordLevel() != 0 && !context.scrollingTransition()) {
            for (var entity : room.entities().slots()) {
                switch (entity.type()) {
                    case linksawakening.world.entity.EntityRomConstants.ENTITY_TELEPHONE -> residentTrack =
                            MusicTrackIds.MUSIC_ULRIRA;
                    case linksawakening.world.entity.EntityRomConstants.ENTITY_RICHARD -> residentTrack =
                            MusicTrackIds.MUSIC_RICHARD_HOUSE;
                    default -> {}
                }
            }
        }
        gameplayMusicController.selectAfterTransition(context, residentTrack);
        roomSession.story().setEntityDefaultMusicTrack(gameplayMusicController.defaultTrackId());
    }

    public void restoreMusicAfterPowerUpEnds(RoomSession roomSession) {
        Objects.requireNonNull(roomSession, "roomSession");
        gameplayMusicController.restoreAfterPowerUpEnds(
                roomSession.entities().inBossBattle(), roomSession.entityState().entityDefaultMusicTrack());
    }

    @Override
    public void playDirectMusic(int trackId) {
        gameplayMusicController.playDirect(trackId);
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

    public void setMenuAttenuated(boolean attenuated) {
        musicPlayer.setMenuAttenuated(attenuated);
    }

    public void continueMusicAfterNextWarp() {
        gameplayMusicController.continueMusicAfterNextWarp();
    }

    public void updateWarpSound(linksawakening.world.transition.TransitionController transition) {
        if (transition.stopDoorMusic()) playDirectMusic(MusicTrackIds.MUSIC_NONE);
        musicPlayer.setWarpVolume(transition.doorVolume());
    }

    public void tick() {
        tick(true);
    }

    public void tick(boolean advanceDriver) {
        if (advanceDriver) gameplayMusicController.tickFadeOut();
        musicPlayer.update(advanceDriver);
    }
}
