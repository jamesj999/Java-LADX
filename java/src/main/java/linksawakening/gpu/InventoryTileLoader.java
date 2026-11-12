package linksawakening.gpu;

import linksawakening.rom.GbcBanks;
import linksawakening.rom.RomSymbols;

final class InventoryTileLoader {
    private final RomTileLoader target;

    InventoryTileLoader(RomTileLoader target) {
        this.target = target;
    }

    private static final int OCARINA_SYMBOLS_TILES_BANK = GbcBanks.adjust(RomSymbols.OcarinaSymbolsTiles.bank());
    private static final int OCARINA_SYMBOLS_TILES_ADDR = RomSymbols.OcarinaSymbolsTiles.address();
    private static final int SHARED_VFX_TILES_BANK = GbcBanks.adjust(RomSymbols.CharacterVfxTiles.bank());
    private static final int SHARED_VFX_TILES_ADDR = RomSymbols.CharacterVfxTiles.address();

    public void updatePowderInventoryTiles(byte[] romData, boolean hasToadstool) {
        if (target.powderIconIsToadstool == hasToadstool) return;
        target.loadTilesFromROM(
                romData,
                GbcBanks.adjust(RomSymbols.MushroomTiles.bank()),
                hasToadstool
                        ? RomSymbols.MushroomTiles.address()
                        : RomSymbols.InventoryEquipmentItemsTiles.address() + 0xE0,
                2,
                0x08E);
        target.powderIconIsToadstool = hasToadstool;
    }

    public void replaceSlimeKeyTilesByGoldenLeaf(byte[] romData) {
        target.loadTilesFromROM(
                romData,
                GbcBanks.adjust(RomSymbols.SlimeKeyTiles.bank()),
                RomSymbols.SlimeKeyTiles.address(),
                0x02,
                0x0CA);
    }

    public void updateHeartPieceMeterTiles(byte[] rom, int command) {
        int source, destination;
        switch (command) {
            case 3 -> {
                source = RomSymbols.PieceOfHeartMeterTiles.address();
                destination = 0x9A;
            }
            case 4 -> {
                source = RomSymbols.PieceOfHeartMeterTiles.address() + 0x30;
                destination = 0x9D;
            }
            case 5 -> {
                source = RomSymbols.InventoryEquipmentItemsTiles.address() + 0x1D0;
                destination = 0x9D;
            }
            case 6 -> {
                source = RomSymbols.InventoryEquipmentItemsTiles.address() + 0x1A0;
                destination = 0x9A;
            }
            default -> throw new IllegalArgumentException("Heart meter command: " + command);
        }
        target.loadTilesFromROM(rom, GbcBanks.adjust(RomSymbols.PieceOfHeartMeterTiles.bank()), source, 3, destination);
    }

    public void updateTradingItemTiles(byte[] rom, int item) {
        if (item >= 2)
            target.loadTilesFromROM(
                    rom,
                    GbcBanks.adjust(RomSymbols.Items1Tiles.bank()),
                    RomSymbols.Items1Tiles.address() + (item - 2) * 0x40,
                    4,
                    0x9A);
    }

    public void loadOcarinaSymbolsTiles(byte[] romData) {
        target.loadTilesFromROM(romData, OCARINA_SYMBOLS_TILES_BANK, OCARINA_SYMBOLS_TILES_ADDR, 0x04, 0x20);
        target.loadTilesFromROM(romData, OCARINA_SYMBOLS_TILES_BANK, OCARINA_SYMBOLS_TILES_ADDR + 0x40, 0x04, 0x24);
        target.loadTilesFromROM(romData, OCARINA_SYMBOLS_TILES_BANK, OCARINA_SYMBOLS_TILES_ADDR + 0x60, 0x04, 0x26);
    }

    public void loadSharedVfxTiles(byte[] romData) {
        target.loadTilesFromROM(romData, SHARED_VFX_TILES_BANK, SHARED_VFX_TILES_ADDR, 0x04, 0x20);
        target.loadTilesFromROM(romData, SHARED_VFX_TILES_BANK, SHARED_VFX_TILES_ADDR + 0x40, 0x04, 0x24);
        target.loadTilesFromROM(romData, SHARED_VFX_TILES_BANK, SHARED_VFX_TILES_ADDR + 0x60, 0x04, 0x26);
    }
}
