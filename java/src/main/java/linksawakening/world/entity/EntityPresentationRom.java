package linksawakening.world.entity;

import linksawakening.rom.Rom;
import linksawakening.rom.RomAddress;

/** Palette/sprite tables used by HeartPiece and Instrument handlers; addresses verified against azle.sym. */
final class EntityPresentationRom {
    // These symbols are not yet in the shared generated RomSymbols allowlist.
    private static final RomAddress HEART_PIECE_VARIANTS = new RomAddress(0x03, 0x5B17); // HeartPieceSpriteVariants
    private static final RomAddress INSTRUMENT_PALETTE = new RomAddress(0x36, 0x7096); // InstrumentPalette
    private static final RomAddress INSTRUMENT_WARP_SPRITES = new RomAddress(0x06, 0x773C); // Data_006_773C
    private static final RomAddress HINT_TILES = new RomAddress(0x07, 0x6493); // Data_007_6493
    private static final RomAddress HINT_ATTRIBUTES = new RomAddress(0x07, 0x64DB); // Data_007_64F6, unpatched ROM

    private EntityPresentationRom() {}

    static void initialize(EntityRoomInitializationServices runtime, Rom romData) {
        if (romData != null) {
            for (int i = 0; i < runtime.pickups().storageHeartPieceMeterPairs().length; i++)
                runtime.pickups().writeHeartPieceMeterPairs(i, romData.u8(HEART_PIECE_VARIANTS.plus(i)));
            for (int i = 0; i < runtime.rewards().storageInstrumentPaletteCycle().length; i++) {
                int rgb555 = romData.u16le(INSTRUMENT_PALETTE.plus(i * 2));
                runtime.rewards().writeInstrumentPaletteCycle(i, linksawakening.rom.RomBank.decodeRgb555(rgb555));
            }
            for (int i = 0; i < runtime.rewards().storageInstrumentWarpSprites().length; i++) {
                runtime.rewards().writeInstrumentWarpSprites(i, romData.u8(INSTRUMENT_WARP_SPRITES.plus(i)));
            }
            // Eighteen fixed repeat commands, two rows per controller frame.
            // Shipped ROM symbols: Data_007_6493=$6493, Data_007_64F6=$64DB.
            for (int row = 0; row < 18; row++) {
                int tile = romData.u8(HINT_TILES.plus(row * 4 + 3));
                int attr = romData.u8(HINT_ATTRIBUTES.plus(row * 4 + 3));
                java.util.Arrays.fill(runtime.rewards().storageInstrumentHintTiles(), row * 20, (row + 1) * 20, tile);
                java.util.Arrays.fill(
                        runtime.rewards().storageInstrumentHintAttributes(), row * 20, (row + 1) * 20, attr);
            }
        }
    }
}
