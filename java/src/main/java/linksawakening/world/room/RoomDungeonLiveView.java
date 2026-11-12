package linksawakening.world.room;

import java.util.Objects;
import java.util.function.Supplier;

/** Indexed inventory reads; map layout is immutable ROM, statuses remain live WRAM. */
final class RoomDungeonLiveView implements RoomDungeon.View {
    private final RoomWorldState world;
    private final Supplier<ActiveRoom> room;
    private final int[] minimapRooms = new int[64];
    private boolean initialized;
    private int mapId;
    private int towerLayout;
    private byte[] statuses;

    RoomDungeonLiveView(RoomWorldState world, Supplier<ActiveRoom> room) {
        this.world = world;
        this.room = room;
    }

    public int progressFlagCount() {
        return 8;
    }

    public int progressFlag(int index) {
        return Byte.toUnsignedInt(world.dungeonProgressFlags[Objects.checkIndex(index, progressFlagCount())]);
    }

    public int currentItemFlagCount() {
        return DungeonItemState.ITEM_FLAG_SIZE;
    }

    public int currentItemFlag(int index) {
        return world.dungeonItemState.currentFlag(index);
    }

    public int minimapSize() {
        return minimapRooms.length;
    }

    public int minimapStatus(int index) {
        Objects.checkIndex(index, minimapRooms.length);
        refreshMap();
        int roomId = minimapRooms[index];
        return statuses == null || roomId >= statuses.length ? 0 : Byte.toUnsignedInt(statuses[roomId]);
    }

    private void refreshMap() {
        ActiveRoom active = room.get();
        int currentMap = active == null ? -1 : active.mapId();
        int currentTowerLayout = world.dungeonProgressFlags[6] & 4;
        if (initialized && mapId == currentMap && towerLayout == currentTowerLayout) return;
        int offset = active == null ? -1 : world.indoorMapLayoutOffset(currentMap);
        if (offset >= 0) {
            for (int index = 0; index < minimapRooms.length; index++) {
                minimapRooms[index] = world.minimapRoomAtOffset(offset + index);
            }
        }
        statuses = offset < 0 ? null : world.indoorStatusTableForMap(currentMap);
        mapId = currentMap;
        towerLayout = currentTowerLayout;
        initialized = true;
    }
}
