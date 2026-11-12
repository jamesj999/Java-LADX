package linksawakening.scene;

import linksawakening.rom.Rom;
import linksawakening.rom.RomAddress;
import linksawakening.rom.RomBank;

/** Decodes ROM background draw-command streams used by IntroHandler and FileSelectionHandler. */
public final class BackgroundSceneLoader {

    public static final int SCREEN_TILE_WIDTH = 20;
    public static final int SCREEN_TILE_HEIGHT = 18;
    public static final int SCREEN_TILE_COUNT = SCREEN_TILE_WIDTH * SCREEN_TILE_HEIGHT;
    public static final int BG_MAP_TILE_WIDTH = 32;
    public static final int BG_MAP_TILE_HEIGHT = 32;
    public static final int BG_MAP_TILE_COUNT = BG_MAP_TILE_WIDTH * BG_MAP_TILE_HEIGHT;

    private final Rom romData;

    public BackgroundSceneLoader(Rom romData) {
        this.romData = romData;
    }

    public BackgroundScene load(BackgroundSceneSpec spec) {
        int[] tilemap =
                RomBank.decodeBackgroundMap(romData, sceneAddress(spec.tilemapBank(), spec.tilemapAddr()), 0x7E);
        int[] attrmap =
                RomBank.decodeBackgroundMap(romData, sceneAddress(spec.attrmapBank(), spec.attrmapAddr()), 0x00);
        // Erase/copy attrmaps are partial writes over the selection screen's VRAM.
        if (spec.equals(BackgroundSceneCatalog.FILE_ERASE) || spec.equals(BackgroundSceneCatalog.FILE_COPY)) {
            var selection = BackgroundSceneCatalog.FILE_SELECTION_COMMANDS;
            int[] initial = RomBank.decodeBackgroundMap(
                    romData, sceneAddress(selection.attrmapBank(), selection.attrmapAddr()), 0);
            attrmap =
                    RomBank.decodeBackgroundMap(romData, sceneAddress(spec.attrmapBank(), spec.attrmapAddr()), initial);
        }
        return new BackgroundScene(
                tilemap,
                attrmap,
                RomBank.loadPalettes(romData, sceneAddress(spec.palettesBank(), spec.palettesAddr()), 8),
                RomBank.loadPalettes(romData, sceneAddress(spec.palettesBank(), spec.palettesAddr() + 0x40), 8));
    }

    /** @deprecated Use the Rom overload; retains legacy partial-buffer validation. */
    @Deprecated(forRemoval = true)
    public BackgroundSceneLoader(byte[] romData) {
        this(romData == null ? null : new Rom(romData));
    }
    /** Preserve the legacy scene specification's file-offset arithmetic, including CPU-window aliases. */
    private static RomAddress sceneAddress(int bank, int address) {
        return RomAddress.fromOffset(RomBank.romOffset(bank, address));
    }
}
