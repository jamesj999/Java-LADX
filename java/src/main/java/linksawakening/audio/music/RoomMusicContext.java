package linksawakening.audio.music;

import linksawakening.state.PlayerState;
import linksawakening.world.ActiveRoom;
import linksawakening.world.Warp;

public record RoomMusicContext(int roomId,
                               int mapId,
                               boolean indoor,
                               boolean sideScrolling,
                               int swordLevel,
                               boolean bossDefeated,
                               int activePowerUp) {

    public RoomMusicContext {
        roomId &= 0xFF;
        mapId &= 0xFF;
        swordLevel = Math.max(0, Math.min(2, swordLevel));
        activePowerUp &= 0xFF;
    }

    public static RoomMusicContext from(ActiveRoom room, PlayerState playerState) {
        int category = room.mapCategory();
        return new RoomMusicContext(
            room.roomId(),
            room.mapId(),
            category != Warp.CATEGORY_OVERWORLD,
            category == Warp.CATEGORY_SIDESCROLL,
            playerState.swordLevel(),
            false,
            0
        );
    }

    public static RoomMusicContext overworld(int roomId, int swordLevel) {
        return new RoomMusicContext(roomId, 0, false, false, swordLevel, false, 0);
    }

    public static RoomMusicContext indoor(int mapId, int roomId, int swordLevel) {
        return new RoomMusicContext(roomId, mapId, true, false, swordLevel, false, 0);
    }

    public static RoomMusicContext sideScrolling(int mapId, int roomId, int swordLevel) {
        return new RoomMusicContext(roomId, mapId, true, true, swordLevel, false, 0);
    }
}
