package linksawakening.world.room;

import static linksawakening.world.room.RoomSession.*;
import static linksawakening.world.room.RoomSessionRules.*;

final class RoomDungeonView implements RoomDungeon {
    private final Dependencies dependencies;

    RoomDungeonView(Dependencies dependencies) {
        this.dependencies = dependencies;
    }

    public void setDungeonItemForDebug(int mapId, int index, int value) {
        dependencies.roomState().dungeonItemState.setFlagForDebug(mapId, index, value);
    }

    public byte[] dungeonMinimapRoomStatusSnapshot() {
        return dependencies.roomSessionViews().dungeonMinimapRoomStatusSnapshot();
    }

    public byte[] overworldRoomStatusSnapshot() {
        return dependencies.roomSessionViews().overworldRoomStatusSnapshot();
    }

    public byte[] indoorARoomStatusSnapshot() {
        return dependencies.roomSessionViews().indoorARoomStatusSnapshot();
    }

    public byte[] indoorBRoomStatusSnapshot() {
        return dependencies.roomSessionViews().indoorBRoomStatusSnapshot();
    }

    public byte[] colorDungeonRoomStatusSnapshot() {
        return dependencies.roomSessionViews().colorDungeonRoomStatusSnapshot();
    }

    public byte[] dungeonItemFlagsSnapshot() {
        return dependencies.roomSessionViews().dungeonItemFlagsSnapshot();
    }

    public byte[] colorDungeonItemFlagsSnapshot() {
        return dependencies.roomSessionViews().colorDungeonItemFlagsSnapshot();
    }

    public byte[] currentDungeonItemFlagsSnapshot() {
        return dependencies.roomSessionViews().currentDungeonItemFlagsSnapshot();
    }

    public void restoreDungeonItemFlags(byte[] dungeonFlags, byte[] colorFlags) {
        dependencies.roomSessionViews().restoreDungeonItemFlags(dungeonFlags, colorFlags);
    }

    public byte[] dungeonProgressFlagsSnapshot() {
        return dependencies.roomSessionViews().dungeonProgressFlagsSnapshot();
    }

    public void restoreDungeonProgressFlags(byte[] flags) {
        dependencies.roomSessionViews().restoreDungeonProgressFlags(flags);
    }

    record Dependencies(RoomSessionViews roomSessionViews, RoomWorldState roomState) {}
}
