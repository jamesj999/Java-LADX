package linksawakening.app.audio;

import java.util.Objects;
import linksawakening.state.PlayerState;
import linksawakening.world.room.ActiveRoom;
import linksawakening.world.room.RoomLoadListener;
import linksawakening.world.room.RoomSession;

/**
 * Adapts room loading to music selection after the world is assembled. One instance is retained for the
 * owning session.
 */
public final class RoomMusicSelection implements RoomLoadListener {
    private final MusicDirector music;
    private final PlayerState player;
    private final RoomSession rooms;

    public RoomMusicSelection(MusicDirector music, PlayerState player, RoomSession rooms) {
        this.music = Objects.requireNonNull(music);
        this.player = Objects.requireNonNull(player);
        this.rooms = Objects.requireNonNull(rooms);
    }

    public void roomLoaded(ActiveRoom room) {
        music.selectMusicForLoadedRoom(room, player, rooms);
    }
}
