package linksawakening.world.room;

final class RoomDungeonView implements RoomDungeon {
    private final Dependencies dependencies;
    private final RoomSession session;

    RoomDungeonView(Dependencies dependencies, RoomSession session) {
        this.dependencies = dependencies;
        this.session = java.util.Objects.requireNonNull(session);
    }

    @Override
    public RoomDungeon.View view() {
        return session.dungeonView();
    }

    @Override
    public void setDungeonItemForDebug(int mapId, int index, int value) {
        dependencies.roomState().dungeonItemState.setFlagForDebug(mapId, index, value);
    }

    @Override
    public byte[] dungeonMinimapRoomStatusSnapshot() {
        return session.dungeonMinimapRoomStatusSnapshot();
    }

    @Override
    public byte[] overworldRoomStatusSnapshot() {
        return dependencies.roomSessionViews().overworldRoomStatusSnapshot();
    }

    @Override
    public byte[] indoorARoomStatusSnapshot() {
        return dependencies.roomSessionViews().indoorARoomStatusSnapshot();
    }

    @Override
    public byte[] indoorBRoomStatusSnapshot() {
        return dependencies.roomSessionViews().indoorBRoomStatusSnapshot();
    }

    @Override
    public byte[] colorDungeonRoomStatusSnapshot() {
        return dependencies.roomSessionViews().colorDungeonRoomStatusSnapshot();
    }

    @Override
    public byte[] dungeonItemFlagsSnapshot() {
        return dependencies.roomSessionViews().dungeonItemFlagsSnapshot();
    }

    @Override
    public byte[] colorDungeonItemFlagsSnapshot() {
        return dependencies.roomSessionViews().colorDungeonItemFlagsSnapshot();
    }

    @Override
    public byte[] currentDungeonItemFlagsSnapshot() {
        return dependencies.roomSessionViews().currentDungeonItemFlagsSnapshot();
    }

    @Override
    public void restoreDungeonItemFlags(byte[] dungeonFlags, byte[] colorFlags) {
        dependencies.roomSessionViews().restoreDungeonItemFlags(dungeonFlags, colorFlags);
    }

    @Override
    public byte[] dungeonProgressFlagsSnapshot() {
        return dependencies.roomSessionViews().dungeonProgressFlagsSnapshot();
    }

    @Override
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
