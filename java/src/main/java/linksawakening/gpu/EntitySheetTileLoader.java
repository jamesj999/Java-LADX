package linksawakening.gpu;

import linksawakening.rom.RomSymbols;
import linksawakening.rom.GbcBanks;

final class EntitySheetTileLoader {
    private final RomTileLoader target;
    EntitySheetTileLoader(RomTileLoader target) { this.target = target; }
    private static final int[] NPC_TILES_BANKS_GBC = {0x00, GbcBanks.adjust(RomSymbols.Npc2Tiles.bank()),
        GbcBanks.adjust(RomSymbols.Npc1Tiles.bank()), GbcBanks.adjust(RomSymbols.Npc3Tiles.bank())};
    private static final int ENTITY_SHEET_KEEP = 0xFF;
    private static final int ENTITY_SHEET_BYTE_COUNT = 0x100;
    private static final int ENTITY_SHEET_TILE_COUNT = 0x10;
    private static final int ENTITY_SHEET_SOURCE_BASE = RomSymbols.NpcTilesDataStart.address();
    private static final int ENTITY_SHEET_VRAM_BASE = 0x40;
    private static final int COLOR_DUNGEON_SHEET_TABLE_BANK = RomSymbols.ColorDungeonEntitySpritesheetsTable_Slot1.bank();
    private static final int[] COLOR_DUNGEON_SHEET_TABLES = {RomSymbols.ColorDungeonEntitySpritesheetsTable_Slot1.address(), RomSymbols.ColorDungeonEntitySpritesheetsTable_Slot2.address(), RomSymbols.ColorDungeonEntitySpritesheetsTable_Slot3.address(), RomSymbols.ColorDungeonEntitySpritesheetsTable_Slot4.address()};
    private static final int COLOR_DUNGEON_ROOM_COUNT = 0x16;
    public void loadEntitySpriteSheets(byte[] romData, int[] sheetValues) {
        if (sheetValues == null || sheetValues.length != 4) {
            throw new IllegalArgumentException("Exactly four entity sheet selectors are required");
        }
        for (int slot = 0; slot < sheetValues.length; slot++) {
            int sheet = sheetValues[slot];
            if ((sheet & ~0xFF) != 0) {
                throw new IllegalArgumentException("Entity sheet selector must be an unsigned byte: "
                    + sheet);
            }
            if (sheet == ENTITY_SHEET_KEEP) {
                continue;
            }
            int bank = NPC_TILES_BANKS_GBC[(sheet >>> 6) & 0x03];
            int sourceAddress = ENTITY_SHEET_SOURCE_BASE
                + (sheet & 0x3F) * ENTITY_SHEET_BYTE_COUNT;
            int destinationTile = ENTITY_SHEET_VRAM_BASE + slot * ENTITY_SHEET_TILE_COUNT;
            target.loadTilesFromROM(romData, bank, sourceAddress, ENTITY_SHEET_TILE_COUNT,
                destinationTile);
        }
    }

    public void loadColorDungeonEntitySheets(byte[] romData, int roomId) {
        if (romData == null) {
            throw new IllegalArgumentException("ROM data cannot be null");
        }
        if (roomId < 0 || roomId >= COLOR_DUNGEON_ROOM_COUNT) {
            throw new IllegalArgumentException("Color Dungeon room id out of range: " + roomId);
        }
        for (int slot = 0; slot < COLOR_DUNGEON_SHEET_TABLES.length; slot++) {
            int tableOffset = romOffset(COLOR_DUNGEON_SHEET_TABLE_BANK,
                COLOR_DUNGEON_SHEET_TABLES[slot] + roomId * 2);
            if (tableOffset < 0 || tableOffset + 2 > romData.length) {
                throw new IllegalArgumentException("Color Dungeon sheet table exceeds ROM bounds");
            }
            int sourceHighByte = Byte.toUnsignedInt(romData[tableOffset]);
            if (sourceHighByte == 0) {
                continue;
            }
            int sourceBank = Byte.toUnsignedInt(romData[tableOffset + 1]);
            int destinationTile = ENTITY_SHEET_VRAM_BASE + slot * ENTITY_SHEET_TILE_COUNT;
            target.loadTilesFromROM(romData, sourceBank, sourceHighByte << 8,
                ENTITY_SHEET_TILE_COUNT, destinationTile);
        }
    }
    private static int bankAddrToRomOffset(int bank, int address) {
        return bank == 0 ? address : bank * 0x4000 + address - 0x4000;
    }
    private static int romOffset(int bank, int address) { return bankAddrToRomOffset(bank, address); }
}
