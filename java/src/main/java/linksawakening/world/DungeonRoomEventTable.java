package linksawakening.world;

import linksawakening.rom.RomBank;

import java.util.Objects;

/** ROM-backed view of {@code DungeonEventsTable} and {@code ColorDungeonEventsTable}. */
public final class DungeonRoomEventTable {

    private static final int TABLE_BANK = 0x14;
    private static final int DUNGEON_EVENTS_ADDRESS = 0x4000;
    private static final int COLOR_DUNGEON_EVENTS_ADDRESS = 0x4200;
    private static final int ROOM_TABLE_SIZE = 0x100;
    private static final int COLOR_DUNGEON_ROOM_COUNT = 0x20;

    private final byte[] romData;

    public DungeonRoomEventTable(byte[] romData) {
        this.romData = Objects.requireNonNull(romData, "romData");
    }

    /** Mirrors bank $14's room-load lookup, including the indoors-B table page. */
    public int eventFor(int mapId, int roomId) {
        requireByte(mapId, "mapId");
        requireByte(roomId, "roomId");
        int address;
        if (mapId == DungeonItemState.MAP_COLOR_DUNGEON) {
            if (roomId >= COLOR_DUNGEON_ROOM_COUNT) {
                throw new IllegalArgumentException("Color Dungeon room id out of range: "
                    + roomId);
            }
            address = COLOR_DUNGEON_EVENTS_ADDRESS + roomId;
        } else {
            int tablePage = mapId >= 0x06 && mapId < 0x1A ? ROOM_TABLE_SIZE : 0;
            address = DUNGEON_EVENTS_ADDRESS + tablePage + roomId;
        }
        int offset = RomBank.romOffset(TABLE_BANK, address);
        if (offset < 0 || offset >= romData.length) {
            throw new IllegalArgumentException("Dungeon event table is truncated at ROM offset 0x"
                + Integer.toHexString(Math.max(offset, 0)));
        }
        return Byte.toUnsignedInt(romData[offset]);
    }

    private static void requireByte(int value, String label) {
        if ((value & ~0xFF) != 0) {
            throw new IllegalArgumentException(label + " must be an unsigned byte: " + value);
        }
    }
}
