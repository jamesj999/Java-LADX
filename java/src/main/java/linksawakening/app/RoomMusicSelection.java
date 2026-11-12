package linksawakening.app;

import java.util.Objects;
import linksawakening.state.PlayerState;
import linksawakening.world.room.*;

/** Adapts room loading to music selection after the world is assembled. */
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
