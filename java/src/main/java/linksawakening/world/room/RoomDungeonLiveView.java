package linksawakening.world.room;

import java.util.Objects;

/** Indexed inventory reads; map layout is immutable ROM, statuses remain live WRAM. */
final class RoomDungeonLiveView implements RoomDungeon.View {
    private final RoomWorldState world;
    private int roomMapId;
    private final int[] minimapRooms = new int[64];
    private boolean initialized;
    private int mapId;
    private int towerLayout;
    private byte[] statuses = NO_STATUSES;
    private static final byte[] NO_STATUSES = new byte[0];

    RoomDungeonLiveView(RoomWorldState world, int mapId) {
        this.world = java.util.Objects.requireNonNull(world);
        this.roomMapId = mapId;
    }

    void setRoomMap(int mapId) {
        roomMapId = mapId;
    }

    @Override
    public int progressFlagCount() {
        return 8;
    }

    @Override
    public int progressFlag(int index) {
        return Byte.toUnsignedInt(world.dungeonProgressFlags[Objects.checkIndex(index, progressFlagCount())]);
    }

    @Override
    public int currentItemFlagCount() {
        return DungeonItemState.ITEM_FLAG_SIZE;
    }

    @Override
    public int currentItemFlag(int index) {
        return world.dungeonItemState.currentFlag(index);
    }

    @Override
    public int minimapSize() {
        return minimapRooms.length;
    }

    @Override
    public int minimapStatus(int index) {
        Objects.checkIndex(index, minimapRooms.length);
        refreshMap();
        int roomId = minimapRooms[index];
        return roomId >= statuses.length ? 0 : Byte.toUnsignedInt(statuses[roomId]);
    }

    private void refreshMap() {
        int currentMap = roomMapId;
        int currentTowerLayout = world.dungeonProgressFlags[6] & 4;
        if (initialized && mapId == currentMap && towerLayout == currentTowerLayout) return;
        int offset = currentMap < 0 ? -1 : world.indoorMapLayoutOffset(currentMap);
        if (offset >= 0) {
            for (int index = 0; index < minimapRooms.length; index++) {
                minimapRooms[index] = world.minimapRoomAtOffset(offset + index);
            }
        }
        statuses = offset < 0 ? NO_STATUSES : world.indoorStatusTableForMap(currentMap);
        mapId = currentMap;
        towerLayout = currentTowerLayout;
        initialized = true;
    }
}
