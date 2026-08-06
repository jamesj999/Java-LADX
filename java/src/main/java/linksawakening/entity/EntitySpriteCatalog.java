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

    private static final int MAP_HOUSE = 0x10;
    private static final int ROOM_INDOOR_B_CAMERA_SHOP = 0xB5;
    private static final int ROOM_OW_SIREN = 0xC9;
    private static final int ROOM_OW_WALRUS = 0xFD;
    private static final int OW_ROOM_STATUS_OWL_TALKED = 0x20;

    private final byte[] romData;
    private final EntitySpriteHandlerCatalog entitySpriteHandlerCatalog;

    public EntitySpriteCatalog(byte[] romData) {
        if (romData == null) {
            throw new IllegalArgumentException("ROM data cannot be null");
        }
        this.romData = romData;
        this.entitySpriteHandlerCatalog = new EntitySpriteHandlerCatalog(romData);
    }

    public EntitySpriteSelection load(EntityRoomLoader.RoomTable roomTable, int roomId) {
        return load(roomTable, roomId, -1, null);
    }

    /**
     * Loads the sprite selection using the same room context as the game's
     * OAM-loader path. The status array is the WRAM
     * {@code wOverworldRoomStatus} table; it is only consulted for overworld
     * Siren and Walrus group overrides.
     */
    public EntitySpriteSelection load(EntityRoomLoader.RoomTable roomTable, int roomId,
                                      int mapId, byte[] overworldRoomStatus) {
        if (roomTable == null) {
            throw new IllegalArgumentException("Room entity table cannot be null");
        }
        if (roomId < 0 || roomId > 0xFF) {
            throw new IllegalArgumentException("Room id out of range: " + roomId);
        }
        if (mapId < -1 || mapId > 0xFF) {
            throw new IllegalArgumentException("Map id out of range: " + mapId);
        }
        if (overworldRoomStatus != null && overworldRoomStatus.length > 0x100) {
            throw new IllegalArgumentException("Overworld room status table is too large");
        }

        int groupAddress = switch (roomTable) {
            case OVERWORLD, COLOR_DUNGEON -> OVERWORLD_GROUP_ADDRESS;
            case INDOORS_A -> INDOORS_A_GROUP_ADDRESS;
            case INDOORS_B -> INDOORS_B_GROUP_ADDRESS;
        };
        int groupOffset = checkedOffset(DATA_BANK, groupAddress + roomId, 1,
            "room spritesheet group");
        int groupIndex = Byte.toUnsignedInt(romData[groupOffset]);
        groupIndex = applyRoomContextOverride(roomTable, roomId, groupIndex, mapId,
            overworldRoomStatus);
        int[][] palettes = loadObjectPalettes(
            roomTable == EntityRoomLoader.RoomTable.OVERWORLD && roomId == 0x0E);
        EntitySpriteDefinition death = entitySpriteHandlerCatalog.forDeathEntity();
        EntitySpriteDefinition powerDeath =
            entitySpriteHandlerCatalog.forPowerRecoilDeathEntity();

        // Color Dungeon NPC tiles are loaded by LoadColorDungeonTiles through
        // four entity-specific tables, not through the standard group tables.
        if (roomTable == EntityRoomLoader.RoomTable.COLOR_DUNGEON) {
            return new EntitySpriteSelection(roomTable, roomId, groupIndex,
                new int[0], false, palettes)
                .withBurningSpriteDefinition(entitySpriteHandlerCatalog.forBurningEntity())
                .withDeathSpriteDefinitions(death, powerDeath);
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
            true, palettes)
            .withBurningSpriteDefinition(entitySpriteHandlerCatalog.forBurningEntity())
            .withDeathSpriteDefinitions(death, powerDeath);
    }

    private static int applyRoomContextOverride(EntityRoomLoader.RoomTable roomTable, int roomId,
                                                int groupIndex, int mapId,
                                                byte[] overworldRoomStatus) {
        if (roomTable != EntityRoomLoader.RoomTable.OVERWORLD) {
            if (mapId == MAP_HOUSE && roomId == ROOM_INDOOR_B_CAMERA_SHOP) {
                return 0x3D;
            }
            return groupIndex;
        }

        if (groupIndex == 0x23 && roomStatus(overworldRoomStatus, ROOM_OW_SIREN)) {
            groupIndex++;
        }
        if (groupIndex == 0x21 && roomStatus(overworldRoomStatus, ROOM_OW_WALRUS)) {
            groupIndex++;
        }
        return groupIndex;
    }

    private static boolean roomStatus(byte[] overworldRoomStatus, int roomId) {
        return overworldRoomStatus != null
            && roomId < overworldRoomStatus.length
            && (Byte.toUnsignedInt(overworldRoomStatus[roomId]) & OW_ROOM_STATUS_OWL_TALKED) != 0;
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
