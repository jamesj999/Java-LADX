package linksawakening.gpu;

import linksawakening.rom.RomBank;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

final class GPUFileMenuTilesTest {

    @Test
    void loadMenuTilesFollowsTheRomMenuAndBaseCopyDestinations() {
        byte[] rom = syntheticRom();
        writeBytes(rom, 0x2C, 0x4800, 0x11);
        writeBytes(rom, 0x2C, 0x4800 + 0x400, 0x22);
        writeBytes(rom, 0x2C, 0x4000, 0x33);
        writeBytes(rom, 0x2F, 0x4000, 0x44);
        writeBytes(rom, 0x2F, 0x4000 + 0x3FF, 0x55);
        writeBytes(rom, 0x2F, 0x5000, 0x66);
        writeBytes(rom, 0x2F, 0x5000 + 0x7FF, 0x77);
        writeBytes(rom, 0x2C, 0x47A0, 0x88);
        writeBytes(rom, 0x2C, 0x47A0 + 0x1F, 0x99);

        GPU gpu = new GPU();
        gpu.loadMenuTiles(rom);

        assertEquals(0x33, unsigned(gpu.readVRAM(0x0000)));
        assertEquals(0x44, unsigned(gpu.readVRAM(0x0800)));
        assertEquals(0x55, unsigned(gpu.readVRAM(0x0800 + 0x3FF)));
        assertEquals(0x66, unsigned(gpu.readVRAM(0x1000)));
        assertEquals(0x77, unsigned(gpu.readVRAM(0x1000 + 0x7FF)));
        assertEquals(0x88, unsigned(gpu.readVRAM(0x0E00)));
        assertEquals(0x99, unsigned(gpu.readVRAM(0x0E00 + 0x1F)));
        assertEquals(0x22, unsigned(gpu.readVRAM(0x0C00)));
    }

    private static byte[] syntheticRom() {
        return new byte[RomBank.romOffset(0x2F, 0x5000) + 0x800];
    }

    private static void writeBytes(byte[] rom, int bank, int address, int value) {
        rom[RomBank.romOffset(bank, address)] = (byte) value;
    }

    private static int unsigned(byte value) {
        return Byte.toUnsignedInt(value);
    }
}
