package linksawakening.world.room;

import static linksawakening.world.room.RoomSession.*;
import static linksawakening.world.room.RoomSessionRules.*;

public interface RoomDungeon {
    void setDungeonItemForDebug(int mapId, int index, int value);

    byte[] dungeonMinimapRoomStatusSnapshot();

    byte[] overworldRoomStatusSnapshot();

    byte[] indoorARoomStatusSnapshot();

    byte[] indoorBRoomStatusSnapshot();

    byte[] colorDungeonRoomStatusSnapshot();

    byte[] dungeonItemFlagsSnapshot();

    byte[] colorDungeonItemFlagsSnapshot();

    byte[] currentDungeonItemFlagsSnapshot();

    void restoreDungeonItemFlags(byte[] dungeonFlags, byte[] colorFlags);

    byte[] dungeonProgressFlagsSnapshot();

    void restoreDungeonProgressFlags(byte[] flags);
}
