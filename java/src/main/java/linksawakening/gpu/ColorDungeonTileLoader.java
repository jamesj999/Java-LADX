package linksawakening.gpu;

import linksawakening.rom.GbcBanks;
import linksawakening.rom.Rom;
import linksawakening.rom.RomSymbols;

/** Color Dungeon BG uploads, selected by the room's high-byte/bank table. */
final class ColorDungeonTileLoader {
    private final RomTileLoader target;

    ColorDungeonTileLoader(RomTileLoader target) {
        this.target = target;
    }

    private static final int COLOR_DUNGEON_ROOM_COUNT = 0x16;
    private static final int DUNGEONS_TILES_BANK = RomSymbols.DungeonsTiles.bank();
    private static final int DUNGEONS_TILES_ADDR = RomSymbols.DungeonsTiles.address();
    private static final int DUNGEONS_TILES_COUNT = 0x60;
    private static final int DUNGEONS_TILES_VRAM_INDEX = 0x120;
    private static final int INDOOR_TILES_COUNT_PER_SET = 0x10;
    private static final int INDOOR_TILES_VRAM_INDEX = 0x100;
    private static final int DUNGEON_WALLS_TILES_COUNT = 0x20;
    private static final int DUNGEON_ITEMS_TILES_COUNT = 0x10;
    private static final int DUNGEON_ITEMS_VRAM_INDEX = 0x0F0;
    private static final int COLOR_DUNGEON_BG_BANK = RomSymbols.ColorDungeonTiles.bank();
    private static final int COLOR_DUNGEON_BG_FLOOR_ADDR = RomSymbols.ColorDungeonTiles.address();
    private static final int COLOR_DUNGEON_BG_ITEMS_ADDR = RomSymbols.ColorDungeonTiles.address() + 0x100;

    void loadBackground(Rom romData, int roomId) {
        if (roomId < 0 || roomId >= COLOR_DUNGEON_ROOM_COUNT) {
            throw new IllegalArgumentException("Color Dungeon room id out of range: " + roomId);
        }

        Rom rom = romData;
        int roomTilesHighByte = rom.u8(RomSymbols.data_020_45EA.plus(roomId * 2));
        int roomTilesBank = rom.u8(RomSymbols.data_020_45EA.plus(roomId * 2 + 1));
        target.loadTilesFromROM(
                romData, roomTilesBank, roomTilesHighByte << 8, INDOOR_TILES_COUNT_PER_SET, INDOOR_TILES_VRAM_INDEX);

        target.loadTilesFromROM(
                romData,
                COLOR_DUNGEON_BG_BANK,
                COLOR_DUNGEON_BG_FLOOR_ADDR,
                INDOOR_TILES_COUNT_PER_SET,
                INDOOR_TILES_VRAM_INDEX + 0x10);

        target.loadTilesFromROM(
                romData,
                GbcBanks.adjust(DUNGEONS_TILES_BANK),
                DUNGEONS_TILES_ADDR,
                DUNGEONS_TILES_COUNT,
                DUNGEONS_TILES_VRAM_INDEX);

        int wallsHighByte = rom.u8(RomSymbols.ColorDungeonWallsTilesPointers);
        target.loadTilesFromROM(
                romData,
                GbcBanks.adjust(DUNGEONS_TILES_BANK),
                wallsHighByte << 8,
                DUNGEON_WALLS_TILES_COUNT,
                DUNGEONS_TILES_VRAM_INDEX);

        target.loadTilesFromROM(
                romData,
                COLOR_DUNGEON_BG_BANK,
                COLOR_DUNGEON_BG_ITEMS_ADDR,
                DUNGEON_ITEMS_TILES_COUNT,
                DUNGEON_ITEMS_VRAM_INDEX);
    }
}
