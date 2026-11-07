package linksawakening.world.room;







import linksawakening.rom.RomSymbols;

public final class IndoorRoomPointerTables {

    private static final int MAP_INDOORS_B_START = 0x06;
    private static final int MAP_INDOORS_B_END = 0x1A;
    private static final int MAP_COLOR_DUNGEON = 0xFF;

    private IndoorRoomPointerTables() {
    }

    public static RoomPointerTable forMap(int mapId) {
        if (mapId == MAP_COLOR_DUNGEON) {
            return new RoomPointerTable(RomSymbols.ColorDungeonRoomPointers.bank(), RomSymbols.ColorDungeonRoomPointers.address());
        }
        if (mapId >= MAP_INDOORS_B_START && mapId < MAP_INDOORS_B_END) {
            return new RoomPointerTable(RomSymbols.IndoorsBRoomPointers.bank(), RomSymbols.IndoorsBRoomPointers.address());
        }
        return new RoomPointerTable(RomSymbols.IndoorsARoomPointers.bank(), RomSymbols.IndoorsARoomPointers.address());
    }
}
