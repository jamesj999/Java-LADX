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
        int[] tilemap = RomBank.decodeBackgroundMap(romData, spec.tilemapBank(), spec.tilemapAddr(), 0x7E);
        int[] attrmap = RomBank.decodeBackgroundMap(romData, spec.attrmapBank(), spec.attrmapAddr(), 0x00);
        return new BackgroundScene(
            tilemap,
            attrmap,
            RomBank.loadPalettes(romData, spec.palettesBank(), spec.palettesAddr(), 8),
            RomBank.loadPalettes(romData, spec.palettesBank(), spec.palettesAddr() + 0x40, 8)
        );
    }
}
