package linksawakening.entity;

import linksawakening.rom.RomBank;
import linksawakening.world.EntityRoomLoader;

/** Reads room sprite groups, sheet selectors, and object palettes from the ROM. */
public final class EntitySpriteCatalog {

    public static final int ENTITY_SHEET_SLOT_COUNT = 4;
    public static final int OBJECT_PALETTE_COUNT = 6;
    public static final int EAGLES_TOWER_OBJECT_PALETTE_COUNT = 7;

    private static final int DATA_BANK = 0x20;
    private static final int OVERWORLD_GROUP_ADDRESS = 0x70D3;
    private static final int INDOORS_A_GROUP_ADDRESS = 0x71D3;
    private static final int INDOORS_B_GROUP_ADDRESS = 0x72D3;
    private static final int OVERWORLD_SHEETS_ADDRESS = 0x73F3;
    private static final int INDOOR_SHEETS_ADDRESS = 0x763B;
    private static final int PALETTE_BANK = 0x21;
    private static final int OBJECT_PALETTES_ADDRESS = 0x5518;

    private final byte[] romData;

    public EntitySpriteCatalog(byte[] romData) {
        if (romData == null) {
            throw new IllegalArgumentException("ROM data cannot be null");
        }
        this.romData = romData;
    }

    public EntitySpriteSelection load(EntityRoomLoader.RoomTable roomTable, int roomId) {
        if (roomTable == null) {
            throw new IllegalArgumentException("Room entity table cannot be null");
        }
        if (roomId < 0 || roomId > 0xFF) {
            throw new IllegalArgumentException("Room id out of range: " + roomId);
        }

        int groupAddress = switch (roomTable) {
            case OVERWORLD, COLOR_DUNGEON -> OVERWORLD_GROUP_ADDRESS;
            case INDOORS_A -> INDOORS_A_GROUP_ADDRESS;
            case INDOORS_B -> INDOORS_B_GROUP_ADDRESS;
        };
        int groupOffset = checkedOffset(DATA_BANK, groupAddress + roomId, 1,
            "room spritesheet group");
        int groupIndex = Byte.toUnsignedInt(romData[groupOffset]);
        int[][] palettes = loadObjectPalettes(
            roomTable == EntityRoomLoader.RoomTable.OVERWORLD && roomId == 0x0E);

        // Color Dungeon NPC tiles are loaded by LoadColorDungeonTiles through
        // four entity-specific tables, not through the standard group tables.
        if (roomTable == EntityRoomLoader.RoomTable.COLOR_DUNGEON) {
            return new EntitySpriteSelection(roomTable, roomId, groupIndex,
                new int[0], false, palettes);
        }

        int tableAddress = roomTable == EntityRoomLoader.RoomTable.OVERWORLD
            ? OVERWORLD_SHEETS_ADDRESS
            : INDOOR_SHEETS_ADDRESS;
        int sheetOffset = checkedOffset(DATA_BANK, tableAddress + groupIndex * ENTITY_SHEET_SLOT_COUNT,
            ENTITY_SHEET_SLOT_COUNT, "entity spritesheet table");
        int[] sheetValues = new int[ENTITY_SHEET_SLOT_COUNT];
        for (int slot = 0; slot < ENTITY_SHEET_SLOT_COUNT; slot++) {
            sheetValues[slot] = Byte.toUnsignedInt(romData[sheetOffset + slot]);
        }
        return new EntitySpriteSelection(roomTable, roomId, groupIndex, sheetValues,
            true, palettes);
    }

    public int[][] loadObjectPalettes() {
        return loadObjectPalettes(false);
    }

    public int[][] loadObjectPalettes(boolean includeEaglesTowerPalette) {
        int paletteCount = includeEaglesTowerPalette
            ? EAGLES_TOWER_OBJECT_PALETTE_COUNT
            : OBJECT_PALETTE_COUNT;
        int[][] palettes = new int[paletteCount][4];
        int offset = checkedOffset(PALETTE_BANK, OBJECT_PALETTES_ADDRESS,
            paletteCount * 4 * 2, "object palette table");
        for (int palette = 0; palette < paletteCount; palette++) {
            for (int color = 0; color < 4; color++) {
                int low = Byte.toUnsignedInt(romData[offset++]);
                int high = Byte.toUnsignedInt(romData[offset++]);
                palettes[palette][color] = RomBank.decodeRgb555(low | (high << 8));
            }
        }
        return palettes;
    }

    private int checkedOffset(int bank, int address, int length, String description) {
        int offset;
        try {
            offset = RomBank.romOffset(bank, address);
        } catch (RuntimeException exception) {
            throw new IllegalArgumentException("Invalid " + description + " address", exception);
        }
        if (offset < 0 || length < 0 || offset > romData.length - length) {
            throw new IllegalArgumentException("Truncated " + description + " at ROM offset 0x"
                + Integer.toHexString(Math.max(offset, 0)));
        }
        return offset;
    }
}
