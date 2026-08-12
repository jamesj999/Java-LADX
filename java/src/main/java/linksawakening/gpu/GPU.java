package linksawakening.gpu;

public class GPU {

    private static final int BANK_SIZE = 0x4000;

    private static final int INTRO_3_TILES_BANK = 0x30;
    private static final int INTRO_3_TILES_ADDR = 0x5400;
    private static final int INTRO_3_TILES_COUNT = 0x60;

    private static final int INTRO_1_TILES_BANK = 0x30;
    private static final int INTRO_1_TILES_ADDR = 0x4000;
    private static final int INTRO_1_TILES_COUNT = 0x100;

    private static final int INTRO_RAIN_TILES_BANK = 0x01;
    private static final int INTRO_RAIN_TILES_ADDR = 0x6D4A;
    private static final int INTRO_RAIN_TILES_COUNT = 0x08;

    private static final int TITLE_LOGO_TILES_BANK = 0x2F;
    private static final int TITLE_LOGO_TILES_ADDR = 0x4900;
    private static final int TITLE_LOGO_TILES_COUNT = 0x70;

    private static final int TITLE_DX_TILES_BANK = 0x38;
    private static final int TITLE_DX_TILES_CGB_ADDR = 0x5800;
    private static final int TITLE_DX_TILES_COUNT = 0x40;
    private static final int TITLE_DX_OAM_TILES_ADDR = 0x6500;
    private static final int TITLE_DX_OAM_TILES_COUNT = 0x10;

    public static final int VRAM_SIZE = 0x2000;
    public static final int TILE_COUNT = 384;
    public static final int TILE_DATA_SIZE = 16;

    private static final int OVERWORLD_TILES_BANK = 0x0C;
    private static final int OVERWORLD_TILES_ADDR = 0x4020;
    private static final int OVERWORLD_TILES_COUNT = 0x30;

    private static final int OVERWORLD_LANDSCAPE_TILES_BANK = 0x2C;
    private static final int OVERWORLD_LANDSCAPE_TILES_ADDR = 0x5400;
    private static final int OVERWORLD_LANDSCAPE_TILES_COUNT = 0x60;

    private static final int ANIMATED_TILES_BANK = 0x2C;
    private static final int ANIMATED_TILES_BASE_ADDR = 0x6A00;
    private static final int ANIMATED_TILES_FRAME_COUNT = 0x04;
    private static final int ANIMATED_TILES_FRAME_VRAM_INDEX = 0x16C;
    private static final int ANIMATED_TILES_FRAME_SIZE = 0x40;
    private static final int ANIMATED_SCROLLING_TILES_BANK = 0x2C;
    private static final int ANIMATED_SCROLLING_TILES_ADDR = 0x47C0;
    private static final int DUNGEON_TWO_ANIMATED_GROUP = 0x07;
    private static final int COLOR_DUNGEON_SCROLLING_VRAM_INDEX = 0x040;
    private static final int DUNGEON_TWO_SCROLLING_VRAM_INDEX = 0x10C;

    // SwitchBlockTiles in bank0c.asm and the GBC-adjusted bank used by the
    // gameplay tile loader. UpdateSwitchBlockTiles copies four tiles at a
    // time to vTilesSwitchBlockA/B ($9040/$9080).
    private static final int SWITCH_BLOCK_TILES_BANK = 0x2C;
    private static final int SWITCH_BLOCK_TILES_ADDR = 0x6800;
    private static final int SWITCH_BLOCK_TILE_COUNT = 0x04;
    private static final int PRESSED_FLOOR_SWITCH_TILES_BANK = 0x32;
    private static final int PRESSED_FLOOR_SWITCH_TILES_ADDR = 0x7F00;
    private static final int PRESSED_FLOOR_SWITCH_VRAM_TILE = 0x114;

    // Ping-pong offset table used by dungeon-1 and lava animations.
    // Matches AnimatedTilesDataOffsets in home/animated_tiles.asm:267.
    private static final int[] ANIMATED_TILES_PING_PONG =
        { 0x00, 0x40, 0x80, 0xC0, 0xC0, 0xC0, 0x80, 0x40 };

    private int currentAnimatedTilesGroup = 0;
    private int animatedTilesFrameCount = 0;
    private int animatedTilesDataOffset = 0;
    private int currentMapId = 0;
    private final byte[] animatedScrollingTiles = new byte[0x40];

    private static final int OVERWORLD2_TILES_BANK = 0x0F;
    private static final int OVERWORLD2_TILES_ADDR = 0x4000;
    private static final int OVERWORLD2_TILES_COUNT = 0x100;

    private static final int MABE_VILLAGE_TILES_BANK = 0x0C;
    private static final int MABE_VILLAGE_TILES_ADDR = 0x6A00;
    private static final int MABE_VILLAGE_TILES_COUNT = 0x100;

    private static final int INVENTORY_TILES_BANK = 0x0C;
    private static final int INVENTORY_TILES_ADDR = 0x4C00;
    private static final int INVENTORY_TILES_COUNT = 0x40;

    private static final int INVENTORY_OVERWORLD_ITEMS_TILES_BANK = 0x0C;
    private static final int INVENTORY_OVERWORLD_ITEMS_TILES_ADDR = 0x4C00;
    private static final int INVENTORY_OVERWORLD_ITEMS_TILES_COUNT = 0x40;
    private static final int INVENTORY_INDOOR_ITEMS_TILES_BANK = 0x12;
    private static final int INVENTORY_INDOOR_ITEMS_TILES_ADDR = 0x7D00;

    private static final int INVENTORY_EQUIPMENT_ITEMS_TILES_BANK = 0x0C;
    private static final int INVENTORY_EQUIPMENT_ITEMS_TILES_ADDR = 0x4800;
    private static final int INVENTORY_EQUIPMENT_ITEMS_TILES_COUNT = 0x80;

    // TilesGfxSource._08.._0A in home/interrupts.asm. The source label is
    // OcarinaSymbolsTiles in bank $0C at $6960; the GBC loader selects $2C.
    private static final int OCARINA_SYMBOLS_TILES_BANK = 0x2C;
    private static final int OCARINA_SYMBOLS_TILES_ADDR = 0x6960;
    private static final int SHARED_VFX_TILES_BANK = 0x2C;
    private static final int SHARED_VFX_TILES_ADDR = 0x4200;

    // SaveMenuTiles in bank0.asm:2E5E. Unlike the GBC-adjusted menu sheet,
    // this source is copied from bank $0F by LoadSaveMenuTiles.
    private static final int SAVE_MENU_TILES_BANK = 0x0F;
    private static final int SAVE_MENU_TILES_ADDR = 0x4400;
    private static final int SAVE_MENU_TILES_COUNT = 0x50;

    private static final int LINK_CHARACTER_TILES_BANK = 0x0C;
    private static final int LINK_CHARACTER_TILES_ADDR = 0x4000;
    private static final int LINK_CHARACTER_TILES_COUNT = 0x10;

    // NpcTilesBankTable in bank0.asm:$2E6F, adjusted for the GBC banks used
    // by the shipped color ROM. The two high bits of a sheet selector index
    // this table; the low six bits select a 0x100-byte (16-tile) row.
    private static final int[] NPC_TILES_BANKS_GBC = {0x00, 0x31, 0x2E, 0x32};
    private static final int ENTITY_SHEET_KEEP = 0xFF;
    private static final int ENTITY_SHEET_BYTE_COUNT = 0x100;
    private static final int ENTITY_SHEET_TILE_COUNT = 0x10;
    private static final int ENTITY_SHEET_SOURCE_BASE = 0x4000;
    private static final int ENTITY_SHEET_VRAM_BASE = 0x40;

    // ColorDungeonEntitySpritesheetsTable_Slot1..4 in bank20.asm:$46AA.
    // Each room entry is [source address high byte, source bank], unlike the
    // standard bbtttttt selectors above.
    private static final int COLOR_DUNGEON_SHEET_TABLE_BANK = 0x20;
    private static final int[] COLOR_DUNGEON_SHEET_TABLES = {0x46AA, 0x46D6, 0x4702, 0x472E};
    private static final int COLOR_DUNGEON_ROOM_COUNT = 0x16;

    private final byte[] vram;
    private final Tile[] tiles;

    public GPU() {
        vram = new byte[VRAM_SIZE];
        tiles = new Tile[TILE_COUNT];

        for (int i = 0; i < TILE_COUNT; i++) {
            tiles[i] = new Tile();
            updateTile(i);
        }
    }

    public void loadTitleScreenTiles(byte[] romData) {
        loadTilesFromROM(romData, TITLE_LOGO_TILES_BANK, TITLE_LOGO_TILES_ADDR, TITLE_LOGO_TILES_COUNT, 0x80);
        loadTilesFromROM(romData, TITLE_DX_TILES_BANK, TITLE_DX_TILES_CGB_ADDR, TITLE_DX_TILES_COUNT, 0x40);
        loadTilesFromROM(romData, TITLE_DX_TILES_BANK, TITLE_DX_OAM_TILES_ADDR, TITLE_DX_OAM_TILES_COUNT, 0x20);
    }

    public void loadIntroSequenceTiles(byte[] romData) {
        loadTilesFromROM(romData, INTRO_RAIN_TILES_BANK, INTRO_RAIN_TILES_ADDR, INTRO_RAIN_TILES_COUNT, 0x70);
        loadTilesFromROM(romData, INTRO_3_TILES_BANK, INTRO_3_TILES_ADDR, INTRO_3_TILES_COUNT, 0x00);
        loadTilesFromROM(romData, INTRO_1_TILES_BANK, INTRO_1_TILES_ADDR, INTRO_1_TILES_COUNT, 0x80);
    }

    public void loadBaseOverworldTiles(byte[] romData) {
        currentMapId = 0;
        // Match the game's LoadBaseTiles: copy 0x100 tiles from InventoryEquipmentItemsTiles
        // to vTiles1 (VRAM tile 0x080). This single bulk copy covers equipment items,
        // inventory items, instruments, and landscape tiles in the correct VRAM positions.
        loadTilesFromROM(romData, INVENTORY_EQUIPMENT_ITEMS_TILES_BANK | 0x20,
            INVENTORY_EQUIPMENT_ITEMS_TILES_ADDR, 0x100, 0x080);
        // Link character tiles to vTiles0
        loadTilesFromROM(romData, LINK_CHARACTER_TILES_BANK | 0x20,
            LINK_CHARACTER_TILES_ADDR, 0x40, 0x000);
    }

    /** Mirrors LoadBaseTiles (bank0.asm:$2BCF), used before entering an indoor room. */
    public void loadBaseTiles(byte[] romData) {
        currentMapId = 0;
        loadTilesFromROM(romData, LINK_CHARACTER_TILES_BANK | 0x20,
            LINK_CHARACTER_TILES_ADDR, 0x40, 0x000);
        loadTilesFromROM(romData, INVENTORY_EQUIPMENT_ITEMS_TILES_BANK | 0x20,
            INVENTORY_EQUIPMENT_ITEMS_TILES_ADDR, 0x100, 0x080);
        loadTilesFromROM(romData, INVENTORY_EQUIPMENT_ITEMS_TILES_BANK | 0x20,
            0x47A0, 0x02, 0x0E0);
    }

    /**
     * Load the base and file-menu tile sheets using the destinations in
     * LoadMenuTiles (bank0.asm:$2C03).
     */
    public void loadMenuTiles(byte[] romData) {
        loadBaseTiles(romData);
        loadTilesFromROM(romData, 0x0F | 0x20, 0x4000, 0x40, 0x080);
        loadTilesFromROM(romData, 0x0F | 0x20, 0x5000, 0x80, 0x100);
        loadTilesFromROM(romData, 0x0C | 0x20, 0x47A0, 0x02, 0x0E0);
    }

    /** Mirrors LoadSaveMenuTiles (bank0.asm:$2E5E). */
    public void loadSaveMenuTiles(byte[] romData) {
        loadTilesFromROM(romData, SAVE_MENU_TILES_BANK, SAVE_MENU_TILES_ADDR,
            SAVE_MENU_TILES_COUNT, 0x080);
    }

    /**
     * Load the four standard entity sheets into the four OAM sprite slots.
     * Each selector is the ROM's {@code bbtttttt} value from
     * {@code wLoadedEntitySpritesheets}; {@code $FF} keeps that slot intact.
     */
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
            loadTilesFromROM(romData, bank, sourceAddress, ENTITY_SHEET_TILE_COUNT,
                destinationTile);
        }
    }

    /**
     * Load the Color Dungeon's room-selected entity rows. This mirrors
     * {@code LoadColorDungeonTiles} in bank20.asm: each table entry contains
     * an address high byte and bank, and a zero address high byte leaves that
     * fixed OAM slot unchanged.
     */
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
            loadTilesFromROM(romData, sourceBank, sourceHighByte << 8,
                ENTITY_SHEET_TILE_COUNT, destinationTile);
        }
    }

    private static final int W_TILESET_KEEP = 0x0F;
    
    private static final int TILESET_BANK = 0x0F;
    private static final int TILESET_BASE_ADDR = 0x4000;
    private static final int TILESET_TILE_COUNT = 0x20;
    private static final int TILESET_VRAM_INDEX = 0x100;
    
    public void loadRoomSpecificTiles(byte[] romData, int roomId, int tilesetId) {
        if (tilesetId == W_TILESET_KEEP) {
            return;
        }

        int offset = TILESET_BASE_ADDR + (tilesetId * 0x100);

        loadTilesFromROM(romData, TILESET_BANK | 0x20, offset, TILESET_TILE_COUNT, TILESET_VRAM_INDEX);
    }

    // --- Indoor BG tile loading, mirroring LoadIndoorTiles (bank0.asm:4831) ---

    private static final int DUNGEONS_TILES_BANK = 0x0D;
    private static final int DUNGEONS_TILES_ADDR = 0x4000;
    private static final int DUNGEONS_TILES_COUNT = 0x60;
    private static final int DUNGEONS_TILES_VRAM_INDEX = 0x120;

    private static final int INDOOR_TILES_BASE_ADDR = 0x5000;
    private static final int INDOOR_TILES_COUNT_PER_SET = 0x10;
    private static final int INDOOR_TILES_VRAM_INDEX = 0x100;

    private static final int DUNGEON_FLOOR_PTRS_BANK = 0x20;
    private static final int DUNGEON_FLOOR_PTRS_ADDR = 0x4589;
    private static final int DUNGEON_WALLS_PTRS_BANK = 0x20;
    private static final int DUNGEON_WALLS_PTRS_ADDR = 0x45A9;
    private static final int DUNGEON_ITEMS_PTRS_BANK = 0x20;
    private static final int DUNGEON_ITEMS_PTRS_ADDR = 0x45CA;
    private static final int DUNGEON_WALLS_TILES_COUNT = 0x20;
    private static final int DUNGEON_FLOOR_TILES_COUNT = 0x10;
    private static final int DUNGEON_ITEMS_TILES_COUNT = 0x10;
    private static final int DUNGEON_ITEMS_VRAM_INDEX = 0x0F0;

    private static final int MAP_COLOR_DUNGEON = 0xFF;
    private static final int MAP_HOUSE = 0x10;
    private static final int ROOM_CAMERA_SHOP = 0xB5;
    private static final int COLOR_DUNGEON_BG_BANK = 0x35;
    private static final int COLOR_DUNGEON_BG_FLOOR_ADDR = 0x6000;
    private static final int COLOR_DUNGEON_BG_ITEMS_ADDR = 0x6100;
    private static final int CAMERA_SHOP_TILES_ADDR = 0x6600;
    private static final int COLOR_DUNGEON_ROOM_TILES_TABLE_BANK = 0x20;
    private static final int COLOR_DUNGEON_ROOM_TILES_TABLE_ADDR = 0x45EA;
    private static final int COLOR_DUNGEON_WALLS_POINTER_ADDR = 0x45C9;

    private static final int INDOORS_TILESETS_TABLE_BANK = 0x20;
    private static final int INDOORS_TILESETS_TABLE_ADDR = 0x6EB3;

    private static final int W_TILESET_NO_UPDATE = 0xFF;

    /**
     * Load the BG tiles for an indoor room. Mirrors {@code LoadIndoorTiles}
     * (bank0.asm:4831) + the indoor branch of {@code LoadRoomSpecificTiles}
     * (bank0.asm:5389). Bulk shared dungeon tiles go to {@code vTiles2+$200},
     * the per-map walls overlay the first 32 of that block, per-map floor
     * goes to {@code vTiles2+$100}, per-map items go to {@code vTiles1+$700},
     * and the per-room {@code Indoor(tilesetId)Tiles} goes to {@code vTiles2}.
     *
     * <p>Skipped vs. the original: inventory-items tile patching, Link OAM
     * tiles (unchanged from overworld) and toadstool/golden-leaf dynamic swaps.
     */
    public void loadIndoorTiles(byte[] romData, int mapId, int roomId) {
        currentMapId = mapId;
        stageAnimatedScrollingTiles(romData, mapId, roomId);
        if (mapId == MAP_COLOR_DUNGEON) {
            loadColorDungeonBgTiles(romData, roomId);
            return;
        }

        // Shared dungeon tiles (96 tiles at VRAM tile 0x120).
        loadTilesFromROM(romData, DUNGEONS_TILES_BANK | 0x20,
            DUNGEONS_TILES_ADDR, DUNGEONS_TILES_COUNT, DUNGEONS_TILES_VRAM_INDEX);

        // Per-map walls overlay the first 32 tiles of the shared block. Each
        // entry in DungeonWallsTilesPointers is a single high byte — the
        // actual source address is that byte * 0x100 within bank 0x0D.
        int wallsPtrOffset = bankAddrToRomOffset(DUNGEON_WALLS_PTRS_BANK,
            DUNGEON_WALLS_PTRS_ADDR + mapId);
        int wallsHighByte = Byte.toUnsignedInt(romData[wallsPtrOffset]);
        if (wallsHighByte != 0) {
            loadTilesFromROM(romData, DUNGEONS_TILES_BANK | 0x20,
                wallsHighByte << 8, DUNGEON_WALLS_TILES_COUNT, DUNGEONS_TILES_VRAM_INDEX);
        }

        // Per-map floor tiles at VRAM tile 0x110 (vTiles2 + 0x100).
        int floorPtrOffset = bankAddrToRomOffset(DUNGEON_FLOOR_PTRS_BANK,
            DUNGEON_FLOOR_PTRS_ADDR + mapId);
        int floorHighByte = Byte.toUnsignedInt(romData[floorPtrOffset]);
        if (floorHighByte != 0) {
            loadTilesFromROM(romData, DUNGEONS_TILES_BANK | 0x20,
                floorHighByte << 8, DUNGEON_FLOOR_TILES_COUNT, INDOOR_TILES_VRAM_INDEX + 0x10);
        }

        // Per-map items at VRAM tile 0x0F0 (vTiles1 + 0x700). The original
        // uses DungeonItemsTilesPointers indexing into bank $12, but bank
        // adjustment depends on GBC (DungeonItemsTiles is bank 0x12 → 0x32).
        int itemsPtrOffset = bankAddrToRomOffset(DUNGEON_ITEMS_PTRS_BANK,
            DUNGEON_ITEMS_PTRS_ADDR + mapId);
        int itemsHighByte = Byte.toUnsignedInt(romData[itemsPtrOffset]);
        if (itemsHighByte != 0) {
            loadTilesFromROM(romData, 0x12 | 0x20,
                itemsHighByte << 8, DUNGEON_ITEMS_TILES_COUNT, DUNGEON_ITEMS_VRAM_INDEX);
        }

        // LoadIndoorTiles copies the map's indoor-item sheet to vTiles1+$400
        // after the shared gameplay base copy. House maps use the overworld
        // item sheet; dungeon maps use InventoryIndoorItemsTiles.
        if (mapId != MAP_COLOR_DUNGEON) {
            if (mapId >= 0x0A) {
                loadTilesFromROM(romData, INVENTORY_OVERWORLD_ITEMS_TILES_BANK | 0x20,
                    INVENTORY_OVERWORLD_ITEMS_TILES_ADDR, 0x30, 0x0C0);
            } else {
                loadTilesFromROM(romData, INVENTORY_INDOOR_ITEMS_TILES_BANK | 0x20,
                    INVENTORY_INDOOR_ITEMS_TILES_ADDR, 0x30, 0x0C0);
            }
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
            loadTilesFromROM(romData, DUNGEONS_TILES_BANK | 0x20,
                tilesetAddr, INDOOR_TILES_COUNT_PER_SET, INDOOR_TILES_VRAM_INDEX);
        }

        if (mapId == MAP_HOUSE && roomId == ROOM_CAMERA_SHOP) {
            loadTilesFromROM(romData, COLOR_DUNGEON_BG_BANK, CAMERA_SHOP_TILES_ADDR,
                0x20, DUNGEON_ITEMS_VRAM_INDEX);
            return;
        }
    }

    private void stageAnimatedScrollingTiles(byte[] romData, int mapId, int roomId) {
        System.arraycopy(romData, bankAddrToRomOffset(
                ANIMATED_SCROLLING_TILES_BANK, ANIMATED_SCROLLING_TILES_ADDR),
            animatedScrollingTiles, 0, animatedScrollingTiles.length);
        if (mapId == MAP_COLOR_DUNGEON && roomId == 0x01) {
            System.arraycopy(romData, bankAddrToRomOffset(0x35, 0x4F00),
                animatedScrollingTiles, 0, 0x20);
        }
    }

    private void loadColorDungeonBgTiles(byte[] romData, int roomId) {
        if (roomId < 0 || roomId >= COLOR_DUNGEON_ROOM_COUNT) {
            throw new IllegalArgumentException("Color Dungeon room id out of range: " + roomId);
        }

        int roomTilesOffset = bankAddrToRomOffset(COLOR_DUNGEON_ROOM_TILES_TABLE_BANK,
            COLOR_DUNGEON_ROOM_TILES_TABLE_ADDR + roomId * 2);
        int roomTilesHighByte = Byte.toUnsignedInt(romData[roomTilesOffset]);
        int roomTilesBank = Byte.toUnsignedInt(romData[roomTilesOffset + 1]);
        loadTilesFromROM(romData, roomTilesBank, roomTilesHighByte << 8,
            INDOOR_TILES_COUNT_PER_SET, INDOOR_TILES_VRAM_INDEX);

        loadTilesFromROM(romData, COLOR_DUNGEON_BG_BANK, COLOR_DUNGEON_BG_FLOOR_ADDR,
            INDOOR_TILES_COUNT_PER_SET, INDOOR_TILES_VRAM_INDEX + 0x10);

        loadTilesFromROM(romData, DUNGEONS_TILES_BANK | 0x20,
            DUNGEONS_TILES_ADDR, DUNGEONS_TILES_COUNT, DUNGEONS_TILES_VRAM_INDEX);

        int wallsPointerOffset = bankAddrToRomOffset(COLOR_DUNGEON_ROOM_TILES_TABLE_BANK,
            COLOR_DUNGEON_WALLS_POINTER_ADDR);
        int wallsHighByte = Byte.toUnsignedInt(romData[wallsPointerOffset]);
        loadTilesFromROM(romData, DUNGEONS_TILES_BANK | 0x20, wallsHighByte << 8,
            DUNGEON_WALLS_TILES_COUNT, DUNGEONS_TILES_VRAM_INDEX);

        loadTilesFromROM(romData, COLOR_DUNGEON_BG_BANK, COLOR_DUNGEON_BG_ITEMS_ADDR,
            DUNGEON_ITEMS_TILES_COUNT, DUNGEON_ITEMS_VRAM_INDEX);
    }

    private static int bankAddrToRomOffset(int bank, int address) {
        if (bank == 0) {
            return address;
        }
        return bank * BANK_SIZE + (address - BANK_SIZE);
    }

    public void loadAnimatedTilesGroup(byte[] romData, int animatedTilesGroup) {
        currentAnimatedTilesGroup = animatedTilesGroup;
        // Keep animatedTilesDataOffset across rooms, matching the original
        // (hAnimatedTilesDataOffset is never reset on room change).
        copyAnimatedTilesFrame(romData, animatedTilesDataOffset);
    }

    // Should be called once per VBlank while in gameplay. Advances the animated
    // tiles frame counter and, when the current group's timing elapses, copies
    // the next animation frame into VRAM tile slot 0x16C.
    // Mirrors AnimateTiles in src/code/home/animated_tiles.asm.
    public void tickAnimatedTiles(byte[] romData) {
        animatedTilesFrameCount = (animatedTilesFrameCount + 1) & 0xFF;

        if (currentAnimatedTilesGroup == DUNGEON_TWO_ANIMATED_GROUP) {
            if (currentMapId == MAP_COLOR_DUNGEON) {
                copyAnimatedScrollingTiles(COLOR_DUNGEON_SCROLLING_VRAM_INDEX);
            } else if (((animatedTilesFrameCount + 1) & 0x03) == 0) {
                copyAnimatedScrollingTiles(DUNGEON_TWO_SCROLLING_VRAM_INDEX);
            } else if ((animatedTilesFrameCount & 0x07) == 0) {
                int sourceOffset = ANIMATED_TILES_PING_PONG[
                    (animatedTilesFrameCount >> 3) & 0x07];
                loadTilesFromROM(romData, ANIMATED_TILES_BANK,
                    ANIMATED_TILES_BASE_ADDR + 0x0300 + sourceOffset,
                    ANIMATED_TILES_FRAME_COUNT, ANIMATED_TILES_FRAME_VRAM_INDEX);
            }
            return;
        }

        int newOffset = nextAnimatedTilesDataOffset(
            currentAnimatedTilesGroup, animatedTilesFrameCount, animatedTilesDataOffset);
        if (newOffset < 0) {
            return;
        }
        animatedTilesDataOffset = newOffset;
        copyAnimatedTilesFrame(romData, animatedTilesDataOffset);
    }

    private void copyAnimatedScrollingTiles(int destinationTile) {
        System.arraycopy(animatedScrollingTiles, 0, vram, destinationTile * TILE_DATA_SIZE,
            animatedScrollingTiles.length);
        for (int tile = destinationTile; tile < destinationTile + 4; tile++) {
            updateTile(tile);
        }
    }

    /** Copies one ROM switch-block frame into its gameplay VRAM slot. */
    public void copySwitchBlockTiles(byte[] romData, int sourceOffset, int destinationTile) {
        if (sourceOffset != 0x00 && sourceOffset != 0x40 && sourceOffset != 0x80) {
            throw new IllegalArgumentException("Unknown switch-block source offset: "
                + sourceOffset);
        }
        if (destinationTile != 0x104 && destinationTile != 0x108) {
            throw new IllegalArgumentException("Unknown switch-block destination tile: "
                + destinationTile);
        }
        loadTilesFromROM(romData, SWITCH_BLOCK_TILES_BANK,
            SWITCH_BLOCK_TILES_ADDR + sourceOffset, SWITCH_BLOCK_TILE_COUNT, destinationTile);
    }

    /** Mirrors ReplaceTilesButtonPressed's four-tile copy to vTiles2 + $140. */
    public void copyPressedFloorSwitchTiles(byte[] romData) {
        loadTilesFromROM(romData, PRESSED_FLOOR_SWITCH_TILES_BANK,
            PRESSED_FLOOR_SWITCH_TILES_ADDR, 0x04, PRESSED_FLOOR_SWITCH_VRAM_TILE);
    }

    private void copyAnimatedTilesFrame(byte[] romData, int dataOffset) {
        int groupOffset = animatedTilesGroupOffset(currentAnimatedTilesGroup);
        if (groupOffset < 0) {
            return;
        }
        loadTilesFromROM(
            romData,
            ANIMATED_TILES_BANK,
            ANIMATED_TILES_BASE_ADDR + groupOffset + dataOffset,
            ANIMATED_TILES_FRAME_COUNT,
            ANIMATED_TILES_FRAME_VRAM_INDEX
        );
    }

    private static int animatedTilesGroupOffset(int group) {
        return switch (group) {
            case 0x02 -> 0x0100; // tide
            case 0x03 -> 0x0200; // village
            case 0x04 -> 0x0300; // dungeon 1
            case 0x05 -> 0x0400; // underground
            case 0x06 -> 0x0500; // lava
            case 0x08 -> 0x0600; // warp tile
            case 0x09 -> 0x0700; // water currents
            case 0x0A -> 0x0800; // waterfall
            case 0x0B -> 0x0000; // slow waterfall
            case 0x0C -> 0x0900; // water dungeon
            case 0x0D -> 0x0B00; // light beam
            case 0x0E -> 0x0C00; // crystal block
            case 0x0F -> 0x0A00; // bubbles
            case 0x10 -> 0x0D00; // weather vane
            default -> -1;
        };
    }

    // Returns the new data offset to load this frame, or -1 if the group
    // should not animate on this tick. Timing masks mirror AnimateTilesSlowSpeed
    // (0x0F), AnimateTilesMediumSpeed (0x07), and AnimateTilesFastSpeed (0x03).
    private static int nextAnimatedTilesDataOffset(int group, int frameCount, int currentOffset) {
        int linearNext = (currentOffset + ANIMATED_TILES_FRAME_SIZE) & 0xFF;
        return switch (group) {
            case 0x02, 0x03, 0x05, 0x0B, 0x0C ->
                (frameCount & 0x0F) == 0 ? linearNext : -1;
            case 0x08, 0x0E, 0x0F, 0x10 ->
                (frameCount & 0x07) == 0 ? linearNext : -1;
            case 0x09, 0x0A, 0x0D ->
                (frameCount & 0x03) == 0 ? linearNext : -1;
            case 0x04, 0x06 ->
                (frameCount & 0x07) == 0
                    ? ANIMATED_TILES_PING_PONG[(frameCount >> 3) & 0x07]
                    : -1;
            default -> -1;
        };
    }

    public void loadOverworldTiles(byte[] romData) {
        loadTilesFromROM(romData, OVERWORLD_TILES_BANK, OVERWORLD_TILES_ADDR, OVERWORLD_TILES_COUNT, 0x00);
    }

    public void loadOverworldLandscapeTiles(byte[] romData) {
        loadTilesFromROM(romData, OVERWORLD_LANDSCAPE_TILES_BANK, OVERWORLD_LANDSCAPE_TILES_ADDR, OVERWORLD_LANDSCAPE_TILES_COUNT, 0x80);
    }

    public void loadMabeVillageTiles(byte[] romData) {
        loadTilesFromROM(romData, MABE_VILLAGE_TILES_BANK, MABE_VILLAGE_TILES_ADDR, MABE_VILLAGE_TILES_COUNT, 0x00);
    }

    public void loadInventoryTiles(byte[] romData) {
        loadTilesFromROM(romData, INVENTORY_TILES_BANK, INVENTORY_TILES_ADDR, INVENTORY_TILES_COUNT, 0x00);
    }

    /** Copies the three Ocarina popup chunks to vTiles0+$200/$240/$260. */
    public void loadOcarinaSymbolsTiles(byte[] romData) {
        loadTilesFromROM(romData, OCARINA_SYMBOLS_TILES_BANK,
            OCARINA_SYMBOLS_TILES_ADDR, 0x04, 0x20);
        loadTilesFromROM(romData, OCARINA_SYMBOLS_TILES_BANK,
            OCARINA_SYMBOLS_TILES_ADDR + 0x40, 0x04, 0x24);
        loadTilesFromROM(romData, OCARINA_SYMBOLS_TILES_BANK,
            OCARINA_SYMBOLS_TILES_ADDR + 0x60, 0x04, 0x26);
    }

    /** Restores the shared VFX chunks displaced by the Ocarina popup. */
    public void loadSharedVfxTiles(byte[] romData) {
        loadTilesFromROM(romData, SHARED_VFX_TILES_BANK,
            SHARED_VFX_TILES_ADDR, 0x04, 0x20);
        loadTilesFromROM(romData, SHARED_VFX_TILES_BANK,
            SHARED_VFX_TILES_ADDR + 0x40, 0x04, 0x24);
        loadTilesFromROM(romData, SHARED_VFX_TILES_BANK,
            SHARED_VFX_TILES_ADDR + 0x60, 0x04, 0x26);
    }

    private void loadTilesFromROM(byte[] romData, int bank, int address, int tileCount, int vramTileIndex) {
        int romOffset = romOffset(bank, address);
        int vramOffset = vramTileIndex * TILE_DATA_SIZE;
        int byteCount = tileCount * TILE_DATA_SIZE;

        if (romOffset < 0 || romOffset + byteCount > romData.length) {
            throw new IllegalArgumentException("Tile source exceeds ROM bounds");
        }
        if (vramOffset < 0 || vramOffset + byteCount > VRAM_SIZE) {
            throw new IllegalArgumentException("Tile destination exceeds VRAM bounds");
        }

        System.arraycopy(romData, romOffset, vram, vramOffset, byteCount);
        for (int tile = 0; tile < tileCount; tile++) {
            updateTile(vramTileIndex + tile);
        }
    }

    private int romOffset(int bank, int address) {
        if (bank == 0) {
            return address;
        }
        return bank * BANK_SIZE + (address - BANK_SIZE);
    }

    public void writeVRAM(int address, byte value) {
        if (address >= 0 && address < VRAM_SIZE) {
            vram[address] = value;
            int tileIndex = address / TILE_DATA_SIZE;
            if (tileIndex < TILE_COUNT) {
                updateTile(tileIndex);
            }
        }
    }

    public byte readVRAM(int address) {
        if (address >= 0 && address < VRAM_SIZE) {
            return vram[address];
        }
        return 0;
    }

    private void updateTile(int tileIndex) {
        int address = tileIndex * TILE_DATA_SIZE;
        if (address + TILE_DATA_SIZE > VRAM_SIZE) {
            return;
        }

        Tile tile = tiles[tileIndex];
        for (int y = 0; y < 8; y++) {
            int lowByte = Byte.toUnsignedInt(vram[address + y * 2]);
            int highByte = Byte.toUnsignedInt(vram[address + y * 2 + 1]);

            for (int x = 0; x < 8; x++) {
                int bitIndex = 7 - x;
                int lowBit = (lowByte >> bitIndex) & 1;
                int highBit = (highByte >> bitIndex) & 1;
                tile.setPixel(x, y, (highBit << 1) | lowBit);
            }
        }
    }

    public Tile getTile(int index) {
        if (index >= 0 && index < TILE_COUNT) {
            return tiles[index];
        }
        return null;
    }

    public EntitySpriteTileSnapshot snapshotEntityTiles() {
        Tile[] entityTiles = new Tile[EntitySpriteTileSnapshot.TILE_COUNT];
        for (int i = 0; i < entityTiles.length; i++) {
            entityTiles[i] = tiles[EntitySpriteTileSnapshot.BASE_TILE_INDEX + i];
        }
        return new EntitySpriteTileSnapshot(entityTiles);
    }
}
