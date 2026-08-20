package linksawakening.gpu;

import linksawakening.rom.RomBank;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

final class GPUGoldenLeafTilesTest {

    @Test
    void sourceReplacementCopiesLinkCharacterTwoGoldenLeafPairToVramTileCa() {
        int source = RomBank.romOffset(0x2C, 0x68E0);
        byte[] rom = new byte[source + 0x20];
        for (int index = 0; index < 0x20; index++) {
            rom[source + index] = (byte) (0x40 + index);
        }
        GPU gpu = new GPU();

        gpu.replaceSlimeKeyTilesByGoldenLeaf(rom);

        int destination = 0x0CA * GPU.TILE_DATA_SIZE;
        for (int index = 0; index < 0x20; index++) {
            assertEquals(0x40 + index,
                Byte.toUnsignedInt(gpu.readVRAM(destination + index)));
        }
    }
}
