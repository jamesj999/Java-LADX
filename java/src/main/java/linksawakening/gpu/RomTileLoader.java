package linksawakening.gpu;

import linksawakening.rom.GbcBanks;
import linksawakening.rom.Rom;
import linksawakening.rom.RomSymbols;

/** Named ROM tile uploads used by the compatibility GPU facade. */
class RomTileLoader extends Vram {
    final AnimatedTiles animatedTiles = new AnimatedTiles(this);

    public int animatedTilesFrameCount() {
        return animatedTiles.animatedTilesFrameCount();
    }

    public void loadAnimatedTilesGroup(Rom rom, int group) {
        animatedTiles.loadAnimatedTilesGroup(rom, group);
    }

    public void tickAnimatedTiles(Rom rom) {
        animatedTiles.tickAnimatedTiles(rom);
    }

    // SwitchBlockTiles in bank0c.asm and the GBC-adjusted bank used by the
    // gameplay tile loader. UpdateSwitchBlockTiles copies four tiles at a
    // time to vTilesSwitchBlockA/B ($9040/$9080).

    // Ping-pong offset table used by dungeon-1 and lava animations.
    // Matches AnimatedTilesDataOffsets in home/animated_tiles.asm:267.

    private static final int INVENTORY_EQUIPMENT_ITEMS_TILES_BANK = RomSymbols.InventoryEquipmentItemsTiles.bank();
    private static final int INVENTORY_EQUIPMENT_ITEMS_TILES_ADDR = RomSymbols.InventoryEquipmentItemsTiles.address();

    // TilesGfxSource._08.._0A in home/interrupts.asm. The source label is
    // OcarinaSymbolsTiles in bank $0C at $6960; the GBC loader selects $2C.

    // SaveMenuTiles in bank0.asm:2E5E. Unlike the GBC-adjusted menu sheet,
    // this source is copied from bank $0F by LoadSaveMenuTiles.

    private static final int LINK_CHARACTER_TILES_BANK = RomSymbols.LinkCharacterTiles.bank();
    private static final int LINK_CHARACTER_TILES_ADDR = RomSymbols.LinkCharacterTiles.address();
    private static final int LINK_CHARACTER_TILES_COUNT = 0x40;

    // NpcTilesBankTable in bank0.asm:$2E6F, adjusted for the GBC banks used
    // by the shipped color ROM. The two high bits of a sheet selector index
    // this table; the low six bits select a 0x100-byte (16-tile) row.

    // NpcTilesDataStart: bank base plus the selector row high byte.

    // ColorDungeonEntitySpritesheetsTable_Slot1..4 in bank20.asm:$46AA.
    // Each room entry is [source address high byte, source bank], unlike the
    // standard bbtttttt selectors above.

    public void loadTitleScreenTiles(Rom romData) {
        new TitleIntroTileLoader(this).loadTitleScreenTiles(romData);
    }

    public void loadIntroSequenceTiles(Rom romData) {
        new TitleIntroTileLoader(this).loadIntroSequenceTiles(romData);
    }

    boolean powderIconIsToadstool;

    public void loadBaseOverworldTiles(Rom romData) {
        powderIconIsToadstool = false;
        animatedTiles.currentMapId = 0;
        // Match the game's LoadBaseTiles: copy 0x100 tiles from InventoryEquipmentItemsTiles
        // to vTiles1 (VRAM tile 0x080). This single bulk copy covers equipment items,
        // inventory items, instruments, and landscape tiles in the correct VRAM positions.
        loadTilesFromROM(
                romData,
                GbcBanks.adjust(INVENTORY_EQUIPMENT_ITEMS_TILES_BANK),
                INVENTORY_EQUIPMENT_ITEMS_TILES_ADDR,
                0x100,
                0x080);
        // Link character tiles to vTiles0
        loadTilesFromROM(
                romData,
                GbcBanks.adjust(LINK_CHARACTER_TILES_BANK),
                LINK_CHARACTER_TILES_ADDR,
                LINK_CHARACTER_TILES_COUNT,
                0x000);
    }

    /** Mirrors LoadBaseTiles (bank0.asm:$2BCF), used before entering an indoor room. */
    public void loadBaseTiles(Rom romData) {
        powderIconIsToadstool = false;
        animatedTiles.currentMapId = 0;
        loadTilesFromROM(
                romData,
                GbcBanks.adjust(LINK_CHARACTER_TILES_BANK),
                LINK_CHARACTER_TILES_ADDR,
                LINK_CHARACTER_TILES_COUNT,
                0x000);
        loadTilesFromROM(
                romData,
                GbcBanks.adjust(INVENTORY_EQUIPMENT_ITEMS_TILES_BANK),
                INVENTORY_EQUIPMENT_ITEMS_TILES_ADDR,
                0x100,
                0x080);
        loadTilesFromROM(
                romData,
                GbcBanks.adjust(INVENTORY_EQUIPMENT_ITEMS_TILES_BANK),
                RomSymbols.Items1Tiles.address() + 0x3A0,
                0x02,
                0x0E0);
    }

    /** ROM $00:1E2B/$1E8D: the mushroom and powder share VRAM $88E0-$88FF. */
    public void updatePowderInventoryTiles(Rom romData, boolean hasToadstool) {
        new InventoryTileLoader(this).updatePowderInventoryTiles(romData, hasToadstool);
    }

    /** Mirrors REPLACE_TILES_GOLDEN_LEAF ($09) at bank0:$1EA1. */
    public void replaceSlimeKeyTilesByGoldenLeaf(Rom romData) {
        new InventoryTileLoader(this).replaceSlimeKeyTilesByGoldenLeaf(romData);
    }

    /**
     * Load the base and file-menu tile sheets using the destinations in
     * LoadMenuTiles (bank0.asm:$2C03).
     */
    public void loadMenuTiles(Rom romData) {
        new TitleIntroTileLoader(this).loadMenuTiles(romData);
    }

    /** Mirrors LoadSaveMenuTiles (bank0.asm:$2E5E). */
    public void loadSaveMenuTiles(Rom romData) {
        new TitleIntroTileLoader(this).loadSaveMenuTiles(romData);
    }

    /**
     * Load the four standard entity sheets into the four OAM sprite slots.
     * Each selector is the ROM's {@code bbtttttt} value from
     * {@code wLoadedEntitySpritesheets}; {@code $FF} keeps that slot intact.
     */
    public void loadEntitySpriteSheets(Rom romData, int[] sheetValues) {
        invalidateEntitySnapshot();
        new EntitySheetTileLoader(this).loadEntitySpriteSheets(romData, sheetValues);
    }

    /**
     * Load the Color Dungeon's room-selected entity rows. This mirrors
     * {@code LoadColorDungeonTiles} in bank20.asm: each table entry contains
     * an address high byte and bank, and a zero address high byte leaves that
     * fixed OAM slot unchanged.
     */
    public void loadColorDungeonEntitySheets(Rom romData, int roomId) {
        invalidateEntitySnapshot();
        new EntitySheetTileLoader(this).loadColorDungeonEntitySheets(romData, roomId);
    }

    // constants/tilesets.asm encodes HIGH(label)-$40. LoadRoomSpecificTiles
    // reconstructs the source from the bank base, not Overworld2Tiles.

    public void loadRoomSpecificTiles(Rom romData, int roomId, int tilesetId) {
        new RoomTileLoader(this).loadRoomSpecificTiles(romData, roomId, tilesetId);
    }

    // --- Indoor BG tile loading, mirroring LoadIndoorTiles (bank0.asm:4831) ---

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
    public final void loadIndoorTiles(Rom romData, int mapId, int roomId) {
        new RoomTileLoader(this).loadIndoorTiles(romData, mapId, roomId);
    }

    public final void loadIndoorTiles(Rom romData, int mapId, int roomId, boolean sideScrolling) {
        new RoomTileLoader(this).loadIndoorTiles(romData, mapId, roomId, sideScrolling);
    }

    // Should be called once per VBlank while in gameplay. Advances the animated
    // tiles frame counter and, when the current group's timing elapses, copies
    // the next animation frame into VRAM tile slot 0x16C.
    // Mirrors AnimateTiles in src/code/home/animated_tiles.asm.

    /** Copies one ROM switch-block frame into its gameplay VRAM slot. */
    public void copySwitchBlockTiles(Rom romData, int sourceOffset, int destinationTile) {
        new RoomTileLoader(this).copySwitchBlockTiles(romData, sourceOffset, destinationTile);
    }

    /** Mirrors ReplaceTilesButtonPressed's four-tile copy to vTiles2 + $140. */
    public void copyPressedFloorSwitchTiles(Rom romData) {
        new RoomTileLoader(this).copyPressedFloorSwitchTiles(romData);
    }

    // Returns the new data offset to load this frame, or -1 if the group
    // should not animate on this tick. Timing masks mirror AnimateTilesSlowSpeed
    // (0x0F), AnimateTilesMediumSpeed (0x07), and AnimateTilesFastSpeed (0x03).

    /** Executes the two meter uploads and two equipment-tile restorations from ROM. */
    public void updateHeartPieceMeterTiles(Rom rom, int command) {
        new InventoryTileLoader(this).updateHeartPieceMeterTiles(rom, command);
    }

    /** ReplaceTradingItemTiles ($00:1E01), also restores tiles overwritten by the heart meter. */
    public void updateTradingItemTiles(Rom rom, int item) {
        new InventoryTileLoader(this).updateTradingItemTiles(rom, item);
    }

    /** Copies the three Ocarina popup chunks to vTiles0+$200/$240/$260. */
    public void loadOcarinaSymbolsTiles(Rom romData) {
        new InventoryTileLoader(this).loadOcarinaSymbolsTiles(romData);
    }

    /** Restores the shared VFX chunks displaced by the Ocarina popup. */
    public void loadSharedVfxTiles(Rom romData) {
        new InventoryTileLoader(this).loadSharedVfxTiles(romData);
    }

    final void loadTilesFromROM(Rom romData, int bank, int address, int tileCount, int vramTileIndex) {
        if (bank < 0) throw new IllegalArgumentException("Bank cannot be negative");
        int romOffset = bank == 0 ? address : bank * 0x4000 + (address - 0x4000);
        int vramOffset = vramTileIndex * TILE_DATA_SIZE;
        int byteCount = tileCount * TILE_DATA_SIZE;

        if (romOffset < 0 || romOffset + byteCount > romData.size()) {
            throw new IllegalArgumentException("Tile source exceeds ROM bounds");
        }
        if (vramOffset < 0 || vramOffset + byteCount > VRAM_SIZE) {
            throw new IllegalArgumentException("Tile destination exceeds VRAM bounds");
        }

        // The legacy selector's bank-zero row uses a flat ROM offset,
        // including addresses outside ROM0's CPU window. Preserve that API.
        // Retain the original CPU-window validation for switchable banks.
        if (!(bank == 0 && address >= 0x4000)
                && (bank > Integer.MAX_VALUE / 0x4000
                        || address < (bank == 0 ? 0 : 0x4000)
                        || address >= (bank == 0 ? 0x4000 : 0x8000))) {
            throw new IllegalArgumentException("Invalid ROM bank/address");
        }
        romData.copyInto(romOffset, vram, vramOffset, byteCount);
        for (int tile = 0; tile < tileCount; tile++) {
            updateTile(vramTileIndex + tile);
        }
    }

    @Deprecated(forRemoval = true)
    public void loadAnimatedTilesGroup(byte[] rom, int group) {
        loadAnimatedTilesGroup(rom == null ? null : Rom.unchecked(rom), group);
    }

    @Deprecated(forRemoval = true)
    public void tickAnimatedTiles(byte[] rom) {
        tickAnimatedTiles(rom == null ? null : Rom.unchecked(rom));
    }

    @Deprecated(forRemoval = true)
    public void loadBaseOverworldTiles(byte[] romData) {
        loadBaseOverworldTiles(romData == null ? null : Rom.unchecked(romData));
    }

    @Deprecated(forRemoval = true)
    public void loadBaseTiles(byte[] romData) {
        loadBaseTiles(romData == null ? null : Rom.unchecked(romData));
    }

    @Deprecated(forRemoval = true)
    public void loadEntitySpriteSheets(byte[] romData, int[] sheetValues) {
        loadEntitySpriteSheets(romData == null ? null : Rom.unchecked(romData), sheetValues);
    }

    @Deprecated(forRemoval = true)
    public void loadColorDungeonEntitySheets(byte[] romData, int roomId) {
        loadColorDungeonEntitySheets(romData == null ? null : Rom.unchecked(romData), roomId);
    }

    @Deprecated(forRemoval = true)
    public void loadRoomSpecificTiles(byte[] romData, int roomId, int tilesetId) {
        loadRoomSpecificTiles(romData == null ? null : Rom.unchecked(romData), roomId, tilesetId);
    }

    @Deprecated(forRemoval = true)
    public final void loadIndoorTiles(byte[] romData, int mapId, int roomId) {
        loadIndoorTiles(romData == null ? null : Rom.unchecked(romData), mapId, roomId);
    }

    @Deprecated(forRemoval = true)
    public final void loadIndoorTiles(byte[] romData, int mapId, int roomId, boolean sideScrolling) {
        loadIndoorTiles(romData == null ? null : Rom.unchecked(romData), mapId, roomId, sideScrolling);
    }

    @Deprecated(forRemoval = true)
    public void copySwitchBlockTiles(byte[] romData, int sourceOffset, int destinationTile) {
        copySwitchBlockTiles(romData == null ? null : Rom.unchecked(romData), sourceOffset, destinationTile);
    }

    @Deprecated(forRemoval = true)
    public void copyPressedFloorSwitchTiles(byte[] romData) {
        copyPressedFloorSwitchTiles(romData == null ? null : Rom.unchecked(romData));
    }
}
