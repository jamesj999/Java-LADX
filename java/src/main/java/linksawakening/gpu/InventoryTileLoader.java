package linksawakening.gpu;

import linksawakening.rom.RomSymbols;

final class InventoryTileLoader {
    private final RomTileLoader target;
    InventoryTileLoader(RomTileLoader target) { this.target = target; }
    private static final int OCARINA_SYMBOLS_TILES_BANK = GbcBanks.adjust(RomSymbols.OcarinaSymbolsTiles.bank());
    private static final int OCARINA_SYMBOLS_TILES_ADDR = RomSymbols.OcarinaSymbolsTiles.address();
    private static final int SHARED_VFX_TILES_BANK = 0x2C;
    private static final int SHARED_VFX_TILES_ADDR = 0x4200;
    public void updatePowderInventoryTiles(byte[] romData, boolean hasToadstool) {
        if (target.powderIconIsToadstool == hasToadstool) return;
        target.loadTilesFromROM(romData, 0x2C, hasToadstool ? 0x68C0 : 0x48E0, 2, 0x08E);
        target.powderIconIsToadstool = hasToadstool;
    }

    public void replaceSlimeKeyTilesByGoldenLeaf(byte[] romData) {
        target.loadTilesFromROM(romData, GbcBanks.adjust(0x0C), 0x68E0, 0x02, 0x0CA);
    }

    public void updateHeartPieceMeterTiles(byte[] rom, int command) {
        int source, destination;
        switch (command) {
            case 3 -> { source = 0x6900; destination = 0x9A; }
            case 4 -> { source = 0x6930; destination = 0x9D; }
            case 5 -> { source = 0x49D0; destination = 0x9D; }
            case 6 -> { source = 0x49A0; destination = 0x9A; }
            default -> throw new IllegalArgumentException("Heart meter command: " + command);
        }
        target.loadTilesFromROM(rom, 0x2C, source, 3, destination);
    }

    public void updateTradingItemTiles(byte[] rom, int item) {
        if (item >= 2) target.loadTilesFromROM(rom, 0x2C, 0x4400 + (item - 2) * 0x40, 4, 0x9A);
    }

    public void loadOcarinaSymbolsTiles(byte[] romData) {
        target.loadTilesFromROM(romData, OCARINA_SYMBOLS_TILES_BANK,
            OCARINA_SYMBOLS_TILES_ADDR, 0x04, 0x20);
        target.loadTilesFromROM(romData, OCARINA_SYMBOLS_TILES_BANK,
            OCARINA_SYMBOLS_TILES_ADDR + 0x40, 0x04, 0x24);
        target.loadTilesFromROM(romData, OCARINA_SYMBOLS_TILES_BANK,
            OCARINA_SYMBOLS_TILES_ADDR + 0x60, 0x04, 0x26);
    }

    public void loadSharedVfxTiles(byte[] romData) {
        target.loadTilesFromROM(romData, SHARED_VFX_TILES_BANK,
            SHARED_VFX_TILES_ADDR, 0x04, 0x20);
        target.loadTilesFromROM(romData, SHARED_VFX_TILES_BANK,
            SHARED_VFX_TILES_ADDR + 0x40, 0x04, 0x24);
        target.loadTilesFromROM(romData, SHARED_VFX_TILES_BANK,
            SHARED_VFX_TILES_ADDR + 0x60, 0x04, 0x26);
    }
    private static int bankAddrToRomOffset(int bank, int address) {
        return bank == 0 ? address : bank * 0x4000 + address - 0x4000;
    }
    private static int romOffset(int bank, int address) { return bankAddrToRomOffset(bank, address); }
}
