package linksawakening.world.room;

final class RoomDungeonView implements RoomDungeon {
    private final Dependencies dependencies;
    private final RoomSession session;

    RoomDungeonView(Dependencies dependencies, RoomSession session) {
        this.dependencies = dependencies;
        this.session = java.util.Objects.requireNonNull(session);
    }

    public RoomDungeon.View view() {
        return session.dungeonView();
    }

    public void setDungeonItemForDebug(int mapId, int index, int value) {
        dependencies.roomState().dungeonItemState.setFlagForDebug(mapId, index, value);
    }

    public byte[] dungeonMinimapRoomStatusSnapshot() {
        return session.dungeonMinimapRoomStatusSnapshot();
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

    record Dependencies(RoomSessionViews roomSessionViews, RoomWorldState roomState) {
        Dependencies {
            java.util.Objects.requireNonNull(roomSessionViews, "roomSessionViews");
            java.util.Objects.requireNonNull(roomState, "roomState");
        }
    }
}
