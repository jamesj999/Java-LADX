package linksawakening.gpu;

import linksawakening.rom.RomBank;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

final class GPUSwitchBlockTilesTest {

    @Test
    void copiesFourRomTilesFromTheAdjustedGbcSwitchBlockBlob() {
        int source = RomBank.romOffset(0x2C, 0x6800 + 0x40);
        byte[] rom = new byte[source + 0x40];
        for (int i = 0; i < 0x40; i++) {
            rom[source + i] = (byte) (0x80 + i);
        }

        GPU gpu = new GPU();
        gpu.copySwitchBlockTiles(rom, 0x40, 0x108);

        for (int i = 0; i < 0x40; i++) {
            assertEquals(0x80 + i, Byte.toUnsignedInt(gpu.readVRAM(0x108 * 0x10 + i)),
                "switch-block VRAM byte " + i);
        }
    }
}
