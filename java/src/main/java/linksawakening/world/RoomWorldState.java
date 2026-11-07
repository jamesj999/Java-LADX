package linksawakening.world;

import java.util.Arrays;
import linksawakening.rom.RomBank;

/** Persistent room-status regions, dungeon inventory, and ROM map-layout queries. */
final class RoomWorldState {
    private static final int ROOM_STATUS_TABLE_SIZE = 0x100;
    private final byte[] romData;
    private static final int COLOR_DUNGEON_SAVE_STATUS_SIZE = 0x20;
    private static final int INDOOR_MAP_LAYOUT_BANK = 0x14;
    private static final int INDOOR_MAP_LAYOUT_BASE_ADDR = 0x4220;
    private static final int COLOR_DUNGEON_MAP_LAYOUT_ADDR = 0x44E0;
    private static final int EAGLES_TOWER_COLLAPSED_MAP_LAYOUT_ADDR = 0x4520;
    private static final int INDOOR_MAP_LAYOUT_SIZE = 0x40;
    final byte[] overworldRoomStatus = new byte[0x100];
    final byte[] indoorARoomStatus = new byte[0x100];
    final byte[] indoorBRoomStatus = new byte[0x100];
    final byte[] colorDungeonRoomStatus = new byte[0x100];
    final byte[] dungeonProgressFlags = new byte[0x1A];
    final DungeonItemState dungeonItemState = new DungeonItemState();

    RoomWorldState(byte[] romData) { this.romData = romData; }

    byte[] overworldRoomStatusSnapshot() {
        return overworldRoomStatus.clone();
    }

    byte[] indoorARoomStatusSnapshot() {
        return indoorARoomStatus.clone();
    }

    byte[] indoorBRoomStatusSnapshot() {
        return indoorBRoomStatus.clone();
    }

    byte[] colorDungeonRoomStatusSnapshot() {
        return Arrays.copyOf(colorDungeonRoomStatus, COLOR_DUNGEON_SAVE_STATUS_SIZE);
    }

    byte[] dungeonItemFlagsSnapshot() {
        return dungeonItemState.dungeonItemFlagsSnapshot();
    }

    byte[] colorDungeonItemFlagsSnapshot() {
        return dungeonItemState.colorDungeonItemFlagsSnapshot();
    }

    byte[] currentDungeonItemFlagsSnapshot() {
        return dungeonItemState.currentFlagsSnapshot();
    }

    void restoreDungeonItemFlags(byte[] dungeonFlags, byte[] colorFlags) {
        dungeonItemState.restore(dungeonFlags, colorFlags);
    }

    byte[] dungeonProgressFlagsSnapshot() {
        return Arrays.copyOf(dungeonProgressFlags, 0x08);
    }

    void restoreDungeonProgressFlags(byte[] flags) {
        requireLength(flags, 0x08, "dungeonProgressFlags");
        Arrays.fill(dungeonProgressFlags, (byte) 0);
        System.arraycopy(flags, 0, dungeonProgressFlags, 0, flags.length);
    }

    int overworldRoomStatusForTest(int roomId) {
        if (roomId < 0 || roomId >= overworldRoomStatus.length) {
            throw new IllegalArgumentException("Room id out of range: " + roomId);
        }
        return Byte.toUnsignedInt(overworldRoomStatus[roomId]);
    }

    int indoorRoomStatusForTest(int mapId, int roomId) {
        if (roomId < 0 || roomId >= 0x100) {
            throw new IllegalArgumentException("Room id out of range: " + roomId);
        }
        return Byte.toUnsignedInt(indoorStatusTableForMap(mapId)[roomId]);
    }

    int findIndoorMapPosition(int mapId, int roomId) {
        int layoutOffset = indoorMapLayoutOffset(mapId);
        if (layoutOffset < 0 || layoutOffset + INDOOR_MAP_LAYOUT_SIZE > romData.length) {
            return -1;
        }
        if ((roomId & 0xFF) == 0) {
            // MapLayout11 intentionally uses room $00 as both filler and the
            // Color Dungeon boss room. External loads target the real room;
            // subsequent transitions retain this position as wIndoorRoom.
            return mapId == 0xFF ? 0x19 : -1;
        }
        for (int position = 0; position < INDOOR_MAP_LAYOUT_SIZE; position++) {
            if (Byte.toUnsignedInt(romData[layoutOffset + position]) == (roomId & 0xFF)) {
                return position;
            }
        }
        return -1;
    }

    int indoorMapLayoutOffset(int mapId) {
        if (mapId == 0xFF) {
            return RomBank.romOffset(INDOOR_MAP_LAYOUT_BANK, COLOR_DUNGEON_MAP_LAYOUT_ADDR);
        }
        if (mapId < 0 || mapId >= 0x0B) {
            return -1;
        }
        // bank0:$192E and bank14:$58B7 select MapLayout12 once Eagle's
        // Tower's pillar-collapse bit is set in wHasInstrument7.
        if (mapId == 0x06 && (dungeonProgressFlags[0x06] & 0x04) != 0) {
            return RomBank.romOffset(
                INDOOR_MAP_LAYOUT_BANK, EAGLES_TOWER_COLLAPSED_MAP_LAYOUT_ADDR);
        }
        return RomBank.romOffset(INDOOR_MAP_LAYOUT_BANK,
            INDOOR_MAP_LAYOUT_BASE_ADDR + mapId * INDOOR_MAP_LAYOUT_SIZE);
    }

    byte[] indoorStatusTableForMap(int mapId) {
        if (mapId == 0xFF) {
            return colorDungeonRoomStatus;
        }
        return mapId >= 0x06 && mapId < 0x1A
            ? indoorBRoomStatus : indoorARoomStatus;
    }
    private static void requireLength(byte[] values, int expectedLength, String label) {
        if (values == null) {
            throw new NullPointerException(label);
        }
        if (values.length != expectedLength) {
            throw new IllegalArgumentException(label + " must contain " + expectedLength
                + " bytes, got " + values.length);
        }
    }

    byte[] dungeonMinimapRoomStatusSnapshot(ActiveRoom activeRoom) {
        byte[] result = new byte[64];
        if (activeRoom == null) return result;
        int offset = indoorMapLayoutOffset(activeRoom.mapId());
        if (offset < 0) return result;
        byte[] status = indoorStatusTableForMap(activeRoom.mapId());
        for (int i = 0; i < result.length; i++) {
            int room = Byte.toUnsignedInt(romData[offset + i]);
            if (room < status.length) result[i] = status[room];
        }
        return result;
    }

    int activeRoomStatusFlags(ActiveRoom activeRoom) {
        if (activeRoom == null) {
            return 0;
        }
        byte[] status = activeRoom.mapCategory() == Warp.CATEGORY_OVERWORLD
            ? overworldRoomStatus : indoorStatusTableForMap(activeRoom.mapId());
        return Byte.toUnsignedInt(status[activeRoom.roomId()]);
    }

    void restoreRoomStatuses(byte[] overworldStatus, byte[] indoorAStatus,
                                    byte[] indoorBStatus, byte[] colorDungeonStatus) {
        requireLength(overworldStatus, ROOM_STATUS_TABLE_SIZE, "overworldStatus");
        requireLength(indoorAStatus, ROOM_STATUS_TABLE_SIZE, "indoorAStatus");
        requireLength(indoorBStatus, ROOM_STATUS_TABLE_SIZE, "indoorBStatus");
        requireLength(colorDungeonStatus, COLOR_DUNGEON_SAVE_STATUS_SIZE,
            "colorDungeonStatus");
        System.arraycopy(overworldStatus, 0, overworldRoomStatus, 0, ROOM_STATUS_TABLE_SIZE);
        System.arraycopy(indoorAStatus, 0, indoorARoomStatus, 0, ROOM_STATUS_TABLE_SIZE);
        System.arraycopy(indoorBStatus, 0, indoorBRoomStatus, 0, ROOM_STATUS_TABLE_SIZE);
        Arrays.fill(colorDungeonRoomStatus, (byte) 0);
        System.arraycopy(colorDungeonStatus, 0, colorDungeonRoomStatus, 0,
            COLOR_DUNGEON_SAVE_STATUS_SIZE);
    }
}
