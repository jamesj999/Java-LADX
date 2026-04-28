package linksawakening.world;

public final class IndoorRoomPointerTables {

    private static final int INDOORS_A_ROOM_POINTERS_BANK = 0x0A;
    private static final int INDOORS_B_ROOM_POINTERS_BANK = 0x0B;
    private static final int INDOORS_ROOM_POINTERS_ADDR = 0x4000;
    private static final int COLOR_DUNGEON_ROOM_POINTERS_BANK = 0x0A;
    private static final int COLOR_DUNGEON_ROOM_POINTERS_ADDR = 0x7B77;
    private static final int MAP_INDOORS_B_START = 0x06;
    private static final int MAP_INDOORS_B_END = 0x1A;
    private static final int MAP_COLOR_DUNGEON = 0xFF;

    private IndoorRoomPointerTables() {
    }

    public static RoomPointerTable forMap(int mapId) {
        if (mapId == MAP_COLOR_DUNGEON) {
            return new RoomPointerTable(COLOR_DUNGEON_ROOM_POINTERS_BANK, COLOR_DUNGEON_ROOM_POINTERS_ADDR);
        }
        if (mapId >= MAP_INDOORS_B_START && mapId < MAP_INDOORS_B_END) {
            return new RoomPointerTable(INDOORS_B_ROOM_POINTERS_BANK, INDOORS_ROOM_POINTERS_ADDR);
        }
        return new RoomPointerTable(INDOORS_A_ROOM_POINTERS_BANK, INDOORS_ROOM_POINTERS_ADDR);
    }
}
