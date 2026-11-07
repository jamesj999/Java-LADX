package linksawakening.gpu;

import linksawakening.rom.GbcBanks;

import linksawakening.rom.RomSymbols;

final class RoomTileLoader {
    private final RomTileLoader target;
    RoomTileLoader(RomTileLoader target) { this.target = target; }
    private static final int SWITCH_BLOCK_TILES_BANK = GbcBanks.adjust(RomSymbols.SwitchBlockTiles.bank());
    private static final int SWITCH_BLOCK_TILES_ADDR = RomSymbols.SwitchBlockTiles.address();
    private static final int SWITCH_BLOCK_TILE_COUNT = 0x04;
    private static final int PRESSED_FLOOR_SWITCH_TILES_BANK = 0x32;
    private static final int PRESSED_FLOOR_SWITCH_TILES_ADDR = 0x7F00;
    private static final int PRESSED_FLOOR_SWITCH_VRAM_TILE = 0x114;
    private static final int INVENTORY_OVERWORLD_ITEMS_TILES_BANK = RomSymbols.InventoryOverworldItemsTiles.bank();
    private static final int INVENTORY_OVERWORLD_ITEMS_TILES_ADDR = RomSymbols.InventoryOverworldItemsTiles.address();
    private static final int INVENTORY_INDOOR_ITEMS_TILES_BANK = RomSymbols.InventoryIndoorItemsTiles.bank();
    private static final int INVENTORY_INDOOR_ITEMS_TILES_ADDR = RomSymbols.InventoryIndoorItemsTiles.address();
    private static final int W_TILESET_KEEP = 0x0F;
    private static final int TILESET_BANK = RomSymbols.SaveMenuTiles.bank();
    private static final int OVERWORLD_TILESET_SOURCE_BASE = 0x4000;
    private static final int TILESET_TILE_COUNT = 0x20;
    private static final int TILESET_VRAM_INDEX = 0x100;
    private static final int DUNGEONS_TILES_BANK = RomSymbols.DungeonsTiles.bank();
    private static final int DUNGEONS_TILES_ADDR = RomSymbols.DungeonsTiles.address();
    private static final int DUNGEONS_TILES_COUNT = 0x60;
    private static final int DUNGEONS_TILES_VRAM_INDEX = 0x120;
    private static final int INDOOR_TILES_BASE_ADDR = RomSymbols.IndoorTiles.address();
    private static final int INDOOR_TILES_COUNT_PER_SET = 0x10;
    private static final int INDOOR_TILES_VRAM_INDEX = 0x100;
    private static final int DUNGEON_FLOOR_PTRS_BANK = RomSymbols.DungeonFloorTilesPointers.bank();
    private static final int DUNGEON_FLOOR_PTRS_ADDR = RomSymbols.DungeonFloorTilesPointers.address();
    private static final int DUNGEON_WALLS_PTRS_BANK = RomSymbols.DungeonWallsTilesPointers.bank();
    private static final int DUNGEON_WALLS_PTRS_ADDR = RomSymbols.DungeonWallsTilesPointers.address();
    private static final int DUNGEON_ITEMS_PTRS_BANK = RomSymbols.DungeonItemsTilesPointers.bank();
    private static final int DUNGEON_ITEMS_PTRS_ADDR = RomSymbols.DungeonItemsTilesPointers.address();
    private static final int DUNGEON_WALLS_TILES_COUNT = 0x20;
    private static final int DUNGEON_FLOOR_TILES_COUNT = 0x10;
    private static final int DUNGEON_ITEMS_TILES_COUNT = 0x10;
    private static final int DUNGEON_ITEMS_VRAM_INDEX = 0x0F0;
    private static final int MAP_COLOR_DUNGEON = 0xFF;
    private static final int MAP_HOUSE = 0x10;
    private static final int ROOM_CAMERA_SHOP = 0xB5;
    private static final int COLOR_DUNGEON_BG_BANK = RomSymbols.CameraShopIndoorTiles.bank();
    private static final int CAMERA_SHOP_TILES_ADDR = RomSymbols.CameraShopIndoorTiles.address();
    private static final int INDOORS_TILESETS_TABLE_BANK = RomSymbols.IndoorsTilesetsTable.bank();
    private static final int INDOORS_TILESETS_TABLE_ADDR = RomSymbols.IndoorsTilesetsTable.address();
    private static final int W_TILESET_NO_UPDATE = 0xFF;
    public void loadRoomSpecificTiles(byte[] romData, int roomId, int tilesetId) {
        if (tilesetId == W_TILESET_KEEP) {
            return;
        }

        int offset = OVERWORLD_TILESET_SOURCE_BASE + (tilesetId * 0x100);

        target.loadTilesFromROM(romData, GbcBanks.adjust(TILESET_BANK), offset, TILESET_TILE_COUNT, TILESET_VRAM_INDEX);
    }

    public void loadIndoorTiles(byte[] romData, int mapId, int roomId) {
        loadIndoorTiles(romData, mapId, roomId, false);
    }

    public void loadIndoorTiles(byte[] romData, int mapId, int roomId, boolean sideScrolling) {
        target.animatedTiles.currentMapId = mapId;
        target.animatedTiles.stageAnimatedScrollingTiles(romData, mapId, roomId);
        if (mapId == MAP_COLOR_DUNGEON) {
            new ColorDungeonTileLoader(target).loadBackground(romData, roomId);
            return;
        }

        // Shared dungeon tiles (96 tiles at VRAM tile 0x120).
        target.loadTilesFromROM(romData, GbcBanks.adjust(DUNGEONS_TILES_BANK),
            DUNGEONS_TILES_ADDR, DUNGEONS_TILES_COUNT, DUNGEONS_TILES_VRAM_INDEX);

        // Per-map walls overlay the first 32 tiles of the shared block. Each
        // entry in DungeonWallsTilesPointers is a single high byte — the
        // actual source address is that byte * 0x100 within bank 0x0D.
        int wallsPtrOffset = bankAddrToRomOffset(DUNGEON_WALLS_PTRS_BANK,
            DUNGEON_WALLS_PTRS_ADDR + mapId);
        int wallsHighByte = Byte.toUnsignedInt(romData[wallsPtrOffset]);
        if (wallsHighByte != 0) {
            target.loadTilesFromROM(romData, GbcBanks.adjust(DUNGEONS_TILES_BANK),
                wallsHighByte << 8, DUNGEON_WALLS_TILES_COUNT, DUNGEONS_TILES_VRAM_INDEX);
        }

        // Per-map floor tiles at VRAM tile 0x110 (vTiles2 + 0x100).
        int floorPtrOffset = bankAddrToRomOffset(DUNGEON_FLOOR_PTRS_BANK,
            DUNGEON_FLOOR_PTRS_ADDR + mapId);
        int floorHighByte = Byte.toUnsignedInt(romData[floorPtrOffset]);
        if (floorHighByte != 0) {
            target.loadTilesFromROM(romData, GbcBanks.adjust(DUNGEONS_TILES_BANK),
                floorHighByte << 8, DUNGEON_FLOOR_TILES_COUNT, INDOOR_TILES_VRAM_INDEX + 0x10);
        }

        // Per-map items at VRAM tile 0x0F0 (vTiles1 + 0x700). The original
        // uses DungeonItemsTilesPointers indexing into bank $12, but bank
        // adjustment depends on GBC (DungeonItemsTiles is bank 0x12 → 0x32).
        int itemsPtrOffset = bankAddrToRomOffset(DUNGEON_ITEMS_PTRS_BANK,
            DUNGEON_ITEMS_PTRS_ADDR + mapId);
        int itemsHighByte = Byte.toUnsignedInt(romData[itemsPtrOffset]);
        if (itemsHighByte != 0) {
            target.loadTilesFromROM(romData, GbcBanks.adjust(0x12),
                itemsHighByte << 8, DUNGEON_ITEMS_TILES_COUNT, DUNGEON_ITEMS_VRAM_INDEX);
        }

        // LoadIndoorTiles copies the map's indoor-item sheet to vTiles1+$400
        // after the shared gameplay base copy. House maps use the overworld
        // item sheet; dungeon maps use InventoryIndoorItemsTiles.
        if (mapId != MAP_COLOR_DUNGEON) {
            if (mapId >= 0x0A) {
                target.loadTilesFromROM(romData, GbcBanks.adjust(INVENTORY_OVERWORLD_ITEMS_TILES_BANK),
                    INVENTORY_OVERWORLD_ITEMS_TILES_ADDR, 0x30, 0x0C0);
            } else {
                target.loadTilesFromROM(romData, GbcBanks.adjust(INVENTORY_INDOOR_ITEMS_TILES_BANK),
                    INVENTORY_INDOOR_ITEMS_TILES_ADDR, 0x30, 0x0C0);
            }
        }

        // LoadRoomSpecificTiles $00:2F24-$2F4A replaces the entire BG-only
        // region for side-view rooms, then returns before the top-view patch.
        if (sideScrolling) {
            int source = mapId == 0x06 || (mapId >= 0x0A && roomId != 0xE9)
                ? 0x7000 : 0x7800;
            target.loadTilesFromROM(romData, GbcBanks.adjust(DUNGEONS_TILES_BANK),
                source, 0x80, 0x100);
            return;
        }

        // Per-room indoor tileset at VRAM tile 0x100 (vTiles2 base).
        // IndoorsTilesetsTable is actually two 256-byte halves: IndoorsA at
        // $6EB3 and IndoorsB at $6FB3 (bank0.asm:1031 — `inc h` for IndoorsB
        // map-id range). Same tileset entries disagree between the two
        // halves, so the map-category dispatch matters.
        boolean isIndoorsB = mapId >= 0x06 && mapId < 0x1A;
        int tilesetTableAddr = INDOORS_TILESETS_TABLE_ADDR + (isIndoorsB ? 0x100 : 0x00);
        int tilesetsTableOffset = bankAddrToRomOffset(INDOORS_TILESETS_TABLE_BANK,
            tilesetTableAddr + roomId);
        int tilesetId = Byte.toUnsignedInt(romData[tilesetsTableOffset]);
        if (tilesetId != W_TILESET_KEEP && tilesetId != W_TILESET_NO_UPDATE) {
            int tilesetAddr = INDOOR_TILES_BASE_ADDR + (tilesetId * 0x100);
            target.loadTilesFromROM(romData, GbcBanks.adjust(DUNGEONS_TILES_BANK),
                tilesetAddr, INDOOR_TILES_COUNT_PER_SET, INDOOR_TILES_VRAM_INDEX);
        }

        if (mapId == MAP_HOUSE && roomId == ROOM_CAMERA_SHOP) {
            target.loadTilesFromROM(romData, COLOR_DUNGEON_BG_BANK, CAMERA_SHOP_TILES_ADDR,
                0x20, DUNGEON_ITEMS_VRAM_INDEX);
            return;
        }
    }

    public void copySwitchBlockTiles(byte[] romData, int sourceOffset, int destinationTile) {
        if (sourceOffset != 0x00 && sourceOffset != 0x40 && sourceOffset != 0x80) {
            throw new IllegalArgumentException("Unknown switch-block source offset: "
                + sourceOffset);
        }
        if (destinationTile != 0x104 && destinationTile != 0x108) {
            throw new IllegalArgumentException("Unknown switch-block destination tile: "
                + destinationTile);
        }
        target.loadTilesFromROM(romData, SWITCH_BLOCK_TILES_BANK,
            SWITCH_BLOCK_TILES_ADDR + sourceOffset, SWITCH_BLOCK_TILE_COUNT, destinationTile);
    }

    public void copyPressedFloorSwitchTiles(byte[] romData) {
        target.loadTilesFromROM(romData, PRESSED_FLOOR_SWITCH_TILES_BANK,
            PRESSED_FLOOR_SWITCH_TILES_ADDR, 0x04, PRESSED_FLOOR_SWITCH_VRAM_TILE);
    }
    private static int bankAddrToRomOffset(int bank, int address) {
        return bank == 0 ? address : bank * 0x4000 + address - 0x4000;
    }
    private static int romOffset(int bank, int address) { return bankAddrToRomOffset(bank, address); }
}
