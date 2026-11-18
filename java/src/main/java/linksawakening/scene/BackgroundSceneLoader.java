package linksawakening.scene;

import linksawakening.rom.RomBank;

public final class BackgroundSceneLoader {

    public static final int SCREEN_TILE_WIDTH = 20;
    public static final int SCREEN_TILE_HEIGHT = 18;
    public static final int SCREEN_TILE_COUNT = SCREEN_TILE_WIDTH * SCREEN_TILE_HEIGHT;
    public static final int BG_MAP_TILE_WIDTH = 32;
    public static final int BG_MAP_TILE_HEIGHT = 32;
    public static final int BG_MAP_TILE_COUNT = BG_MAP_TILE_WIDTH * BG_MAP_TILE_HEIGHT;

    private final byte[] romData;

    public BackgroundSceneLoader(byte[] romData) {
        this.romData = romData;
    }

    public BackgroundScene load(BackgroundSceneSpec spec) {
        boolean gameOver = BackgroundSceneCatalog.GAME_OVER.equals(spec);
        boolean saveMenu = BackgroundSceneCatalog.FILE_SAVE.equals(spec) || gameOver;
        int[] tilemap = saveMenu
            ? loadSaveMenuTilemap(spec)
            : RomBank.decodeBackgroundMap(romData, spec.tilemapBank(), spec.tilemapAddr(), 0x7E);
        // LoadTileset0F runs during GAMEPLAY_WORLD when Link passes out (01:6D11).
        int initialAttribute = gameOver ? 0x05 : saveMenu ? 0x06 : 0x00;
        int[] attrmap = RomBank.decodeBackgroundMap(romData, spec.attrmapBank(), spec.attrmapAddr(),
            initialAttribute);
        return new BackgroundScene(
            tilemap,
            attrmap,
            RomBank.loadPalettes(romData, spec.palettesBank(), spec.palettesAddr(), 8),
            RomBank.loadPalettes(romData, spec.palettesBank(), spec.palettesAddr() + 0x40, 8)
        );
    }

    private int[] loadSaveMenuTilemap(BackgroundSceneSpec spec) {
        // LoadTileset0F fills the visible columns before the save panel's encoded writes.
        int[] tilemap = new int[BG_MAP_TILE_COUNT];
        for (int y = 0; y < BG_MAP_TILE_HEIGHT; y++) {
            for (int x = 0; x < SCREEN_TILE_WIDTH; x++) {
                tilemap[y * BG_MAP_TILE_WIDTH + x] = 0xAE + ((x + y) & 1);
            }
        }
        RomBank.applyBackgroundMap(romData, spec.tilemapBank(), spec.tilemapAddr(), tilemap);
        return tilemap;
    }
}
