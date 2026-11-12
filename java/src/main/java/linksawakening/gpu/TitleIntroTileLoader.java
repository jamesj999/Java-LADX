package linksawakening.gpu;

import linksawakening.rom.GbcBanks;
import linksawakening.rom.Rom;
import linksawakening.rom.RomSymbols;

final class TitleIntroTileLoader {
    private final Vram target;

    TitleIntroTileLoader(Vram target) {
        this.target = target;
    }

    private static final int INTRO_3_TILES_BANK = GbcBanks.adjust(RomSymbols.Intro3Tiles.bank());
    private static final int INTRO_3_TILES_ADDR = RomSymbols.Intro3Tiles.address();
    private static final int INTRO_3_TILES_COUNT = 0x60;
    private static final int INTRO_1_TILES_BANK = GbcBanks.adjust(RomSymbols.Intro1Tiles.bank());
    private static final int INTRO_1_TILES_ADDR = RomSymbols.Intro1Tiles.address();
    private static final int INTRO_1_TILES_COUNT = 0x100;
    private static final int INTRO_RAIN_TILES_BANK = RomSymbols.IntroRainTiles.bank();
    private static final int INTRO_RAIN_TILES_ADDR = RomSymbols.IntroRainTiles.address();
    private static final int INTRO_RAIN_TILES_COUNT = 0x08;
    private static final int TITLE_LOGO_TILES_BANK = GbcBanks.adjust(RomSymbols.TitleLogoTiles.bank());
    private static final int TITLE_LOGO_TILES_ADDR = RomSymbols.TitleLogoTiles.address();
    private static final int TITLE_LOGO_TILES_COUNT = 0x70;
    private static final int TITLE_DX_TILES_BANK = RomSymbols.TitleDXTilesCGB.bank();
    private static final int TITLE_DX_TILES_CGB_ADDR = RomSymbols.TitleDXTilesCGB.address();
    private static final int TITLE_DX_TILES_COUNT = 0x40;
    private static final int TITLE_DX_OAM_TILES_ADDR = RomSymbols.TitleDXOAMTiles.address();
    private static final int TITLE_DX_OAM_TILES_COUNT = 0x10;
    private static final int SAVE_MENU_TILES_BANK = RomSymbols.SaveMenuTiles.bank();
    private static final int SAVE_MENU_TILES_ADDR = RomSymbols.SaveMenuTiles.address();
    private static final int SAVE_MENU_TILES_COUNT = 0x50;

    public void loadTitleScreenTiles(Rom romData) {
        target.loadTilesFromROM(romData, TITLE_LOGO_TILES_BANK, TITLE_LOGO_TILES_ADDR, TITLE_LOGO_TILES_COUNT, 0x80);
        target.loadTilesFromROM(romData, TITLE_DX_TILES_BANK, TITLE_DX_TILES_CGB_ADDR, TITLE_DX_TILES_COUNT, 0x40);
        target.loadTilesFromROM(romData, TITLE_DX_TILES_BANK, TITLE_DX_OAM_TILES_ADDR, TITLE_DX_OAM_TILES_COUNT, 0x20);
    }

    public void loadIntroSequenceTiles(Rom romData) {
        target.loadTilesFromROM(romData, INTRO_RAIN_TILES_BANK, INTRO_RAIN_TILES_ADDR, INTRO_RAIN_TILES_COUNT, 0x70);
        target.loadTilesFromROM(romData, INTRO_3_TILES_BANK, INTRO_3_TILES_ADDR, INTRO_3_TILES_COUNT, 0x00);
        target.loadTilesFromROM(romData, INTRO_1_TILES_BANK, INTRO_1_TILES_ADDR, INTRO_1_TILES_COUNT, 0x80);
    }

    public void loadMenuTiles(Rom romData) {
        target.loadTilesFromROM(
                romData, GbcBanks.adjust(RomSymbols.MenuTiles.bank()), RomSymbols.MenuTiles.address(), 0x40, 0x080);
        target.loadTilesFromROM(
                romData, GbcBanks.adjust(RomSymbols.FontTiles.bank()), RomSymbols.FontTiles.address(), 0x80, 0x100);
        target.loadTilesFromROM(
                romData,
                GbcBanks.adjust(RomSymbols.Items1Tiles.bank()),
                RomSymbols.Items1Tiles.address() + 0x3A0,
                0x02,
                0x0E0);
    }

    public void loadSaveMenuTiles(Rom romData) {
        target.loadTilesFromROM(romData, SAVE_MENU_TILES_BANK, SAVE_MENU_TILES_ADDR, SAVE_MENU_TILES_COUNT, 0x080);
    }
}
