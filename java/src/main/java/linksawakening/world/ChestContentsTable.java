package linksawakening.world;

import linksawakening.rom.RomBank;

import java.util.Objects;

/** ROM-backed chest contents lookup used by the source chest-spawn path. */
public final class ChestContentsTable {

    private static final int TABLE_BANK = 0x14;
    private static final int ROOM_TABLE_ADDRESS = 0x4560;
    private static final int COLOR_DUNGEON_TABLE_ADDRESS = 0x4860;
    private static final int ROOM_TABLE_SIZE = 0x100;
    private static final int COLOR_DUNGEON_TABLE_SIZE = 0x20;

    public static final int MAP_COLOR_DUNGEON = 0xFF;

    public static final int CHEST_POWER_BRACELET = 0x00;
    public static final int CHEST_SHIELD = 0x01;
    public static final int CHEST_BOW = 0x02;
    public static final int CHEST_HOOKSHOT = 0x03;
    public static final int CHEST_MAGIC_ROD = 0x04;
    public static final int CHEST_PEGASUS_BOOTS = 0x05;
    public static final int CHEST_OCARINA = 0x06;
    public static final int CHEST_FEATHER = 0x07;
    public static final int CHEST_SHOVEL = 0x08;
    public static final int CHEST_MAGIC_POWDER_BAG = 0x09;
    public static final int CHEST_BOMB = 0x0A;
    public static final int CHEST_SWORD = 0x0B;
    public static final int CHEST_FLIPPERS = 0x0C;
    public static final int CHEST_MAGNIFYING_LENS = 0x0D;
    public static final int CHEST_MEDICINE = 0x10;
    public static final int CHEST_TAIL_KEY = 0x11;
    public static final int CHEST_ANGLER_KEY = 0x12;
    public static final int CHEST_FACE_KEY = 0x13;
    public static final int CHEST_BIRD_KEY = 0x14;
    public static final int CHEST_GOLD_LEAF = 0x15;
    public static final int CHEST_MAP = 0x16;
    public static final int CHEST_COMPASS = 0x17;
    public static final int CHEST_STONE_BEAK = 0x18;
    public static final int CHEST_NIGHTMARE_KEY = 0x19;
    public static final int CHEST_SMALL_KEY = 0x1A;
    public static final int CHEST_RUPEES_50 = 0x1B;
    public static final int CHEST_RUPEES_20 = 0x1C;
    public static final int CHEST_RUPEES_100 = 0x1D;
    public static final int CHEST_RUPEES_200 = 0x1E;
    public static final int CHEST_RUPEES_500 = 0x1F;
    public static final int CHEST_SEASHELL = 0x20;
    public static final int CHEST_MESSAGE = 0x21;
    public static final int CHEST_ZOL = 0x22;

    private final byte[] romData;

    public ChestContentsTable(byte[] romData) {
        this.romData = Objects.requireNonNull(romData, "romData");
    }

    /** Mirrors GetChestsStatusForRoom's raw room-table lookup. */
    public int itemForRoom(int mapId, int roomId) {
        validateByte(mapId, "mapId");
        validateByte(roomId, "roomId");
        int tableAddress;
        int tableSize;
        if (mapId == MAP_COLOR_DUNGEON) {
            if (roomId >= COLOR_DUNGEON_TABLE_SIZE) {
                throw new IllegalArgumentException("Color Dungeon room id out of range: " + roomId);
            }
            tableAddress = COLOR_DUNGEON_TABLE_ADDRESS;
            tableSize = COLOR_DUNGEON_TABLE_SIZE;
        } else {
            tableAddress = ROOM_TABLE_ADDRESS;
            tableSize = ROOM_TABLE_SIZE;
        }
        int offset = RomBank.romOffset(TABLE_BANK, tableAddress + roomId);
        if (offset < 0 || offset >= romData.length || offset >= RomBank.romOffset(
            TABLE_BANK, tableAddress + tableSize)) {
            throw new IllegalArgumentException("Chest table is truncated at ROM offset 0x"
                + Integer.toHexString(Math.max(offset, 0)));
        }
        return Byte.toUnsignedInt(romData[offset]);
    }

    /** Mirrors func_014_5900's upgraded-sword replacement for secret-shell chests. */
    public int itemForSpawn(int mapId, int roomId, int swordLevel) {
        validateByte(swordLevel, "swordLevel");
        int item = itemForRoom(mapId, roomId);
        return item == CHEST_SEASHELL && swordLevel >= 2 ? CHEST_RUPEES_20 : item;
    }

    private static void validateByte(int value, String label) {
        if (value < 0 || value > 0xFF) {
            throw new IllegalArgumentException(label + " must be an unsigned byte: " + value);
        }
    }
}
