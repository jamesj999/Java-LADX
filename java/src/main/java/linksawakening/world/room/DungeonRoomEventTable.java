package linksawakening.world.room;

import java.util.Objects;
import linksawakening.rom.Rom;
import linksawakening.rom.RomAddress;
import linksawakening.rom.RomSymbols;

/** ROM-backed view of {@code DungeonEventsTable} and {@code ColorDungeonEventsTable}. */
final class DungeonRoomEventTable {

    private static final int ROOM_TABLE_SIZE = 0x100;
    private static final int COLOR_DUNGEON_ROOM_COUNT = 0x20;

    private final Rom romData;

    public DungeonRoomEventTable(Rom romData) {
        this.romData = Objects.requireNonNull(romData, "romData");
    }

    /** Mirrors bank $14's room-load lookup, including the indoors-B table page. */
    public int eventFor(int mapId, int roomId) {
        requireByte(mapId, "mapId");
        requireByte(roomId, "roomId");
        RomAddress address;
        if (mapId == DungeonItemState.MAP_COLOR_DUNGEON) {
            if (roomId >= COLOR_DUNGEON_ROOM_COUNT) {
                throw new IllegalArgumentException("Color Dungeon room id out of range: " + roomId);
            }
            address = RomSymbols.ColorDungeonEventsTable.plus(roomId);
        } else {
            int tablePage = mapId >= 0x06 && mapId < 0x1A ? ROOM_TABLE_SIZE : 0;
            address = RomSymbols.DungeonEventsTable.plus(tablePage + roomId);
        }
        int offset = address.offset();
        if (offset < 0 || offset >= romData.size()) {
            throw new IllegalArgumentException(
                    "Dungeon event table is truncated at ROM offset 0x" + Integer.toHexString(Math.max(offset, 0)));
        }
        return romData.u8(address);
    }

    private static void requireByte(int value, String label) {
        if ((value & ~0xFF) != 0) {
            throw new IllegalArgumentException(label + " must be an unsigned byte: " + value);
        }
    }
}
