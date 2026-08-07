package linksawakening.gpu;

import linksawakening.rom.RomBank;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

final class GPUSaveMenuTilesTest {

    @Test
    void loadSaveMenuTilesCopiesTheRomSaveSheetToVramTileOne() {
        byte[] rom = new byte[RomBank.romOffset(0x0F, 0x4400) + 0x500];
        rom[RomBank.romOffset(0x0F, 0x4400)] = 0x11;
        rom[RomBank.romOffset(0x0F, 0x4400) + 0x4FF] = 0x22;

        GPU gpu = new GPU();
        gpu.loadSaveMenuTiles(rom);

        assertEquals(0x11, Byte.toUnsignedInt(gpu.readVRAM(0x0800)));
        assertEquals(0x22, Byte.toUnsignedInt(gpu.readVRAM(0x0800 + 0x4FF)));
    }
}
