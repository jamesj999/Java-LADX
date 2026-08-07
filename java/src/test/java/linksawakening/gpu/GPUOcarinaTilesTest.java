package linksawakening.gpu;

import linksawakening.rom.RomBank;
import org.junit.jupiter.api.Test;

import java.io.InputStream;

import static org.junit.jupiter.api.Assertions.assertEquals;

final class GPUOcarinaTilesTest {

    private static final int ROM_BANK = 0x2C;
    private static final int OCARINA_SYMBOLS_ADDRESS = 0x6960;
    private static final int SHARED_VFX_ADDRESS = 0x4200;

    @Test
    void ocarinaSymbolsUseTheThreeDisassemblyVramCopies() throws Exception {
        byte[] rom = loadRom();
        GPU gpu = new GPU();

        gpu.loadOcarinaSymbolsTiles(rom);

        assertRomByte(gpu, rom, ROM_BANK, OCARINA_SYMBOLS_ADDRESS, 0x20);
        assertRomByte(gpu, rom, ROM_BANK, OCARINA_SYMBOLS_ADDRESS + 0x40, 0x24);
        assertRomByte(gpu, rom, ROM_BANK, OCARINA_SYMBOLS_ADDRESS + 0x60, 0x26);
    }

    @Test
    void closingRestoresTheSharedVfxTilesOverwrittenByThePopup() throws Exception {
        byte[] rom = loadRom();
        GPU gpu = new GPU();

        gpu.loadOcarinaSymbolsTiles(rom);
        gpu.loadSharedVfxTiles(rom);

        assertRomByte(gpu, rom, ROM_BANK, SHARED_VFX_ADDRESS, 0x20);
        assertRomByte(gpu, rom, ROM_BANK, SHARED_VFX_ADDRESS + 0x40, 0x24);
        assertRomByte(gpu, rom, ROM_BANK, SHARED_VFX_ADDRESS + 0x60, 0x26);
    }

    private static void assertRomByte(GPU gpu, byte[] rom, int bank, int address,
                                      int vramTile) {
        int romOffset = RomBank.romOffset(bank, address);
        assertEquals(Byte.toUnsignedInt(rom[romOffset]),
            Byte.toUnsignedInt(gpu.readVRAM(vramTile * GPU.TILE_DATA_SIZE)));
    }

    private static byte[] loadRom() throws Exception {
        try (InputStream stream = GPUOcarinaTilesTest.class.getClassLoader()
                .getResourceAsStream("rom/azle.gbc")) {
            return stream.readAllBytes();
        }
    }
}
