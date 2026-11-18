package linksawakening.app.audio;

import java.util.Objects;
import linksawakening.state.PlayerState;
import linksawakening.world.room.ActiveRoom;
import linksawakening.world.room.RoomLoadListener;
import linksawakening.world.room.RoomSession;
import linksawakening.world.transition.TransitionController;

/**
 * Adapts room loading to music selection after the world is assembled. One instance is retained for the
 * owning session.
 */
public final class RoomMusicSelection implements RoomLoadListener {
    private final TransitionController transition;
    private final MusicDirector music;
    private final PlayerState player;
    private final RoomSession rooms;

    public RoomMusicSelection(
            MusicDirector music, PlayerState player, RoomSession rooms, TransitionController transition) {
        this.transition = Objects.requireNonNull(transition);
        this.music = Objects.requireNonNull(music);
        this.player = Objects.requireNonNull(player);
        this.rooms = Objects.requireNonNull(rooms);
    }

    @Override
    public void roomLoaded(ActiveRoom room) {
        transition.whenRoomReady(() -> {
            if (transition.continuesMusicOnArrival()) music.continueMusicAfterNextWarp();
            music.selectMusicForLoadedRoom(room, player, rooms);
        });
    }
}
