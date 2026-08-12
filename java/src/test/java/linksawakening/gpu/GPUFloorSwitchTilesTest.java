package linksawakening.gpu;

import linksawakening.rom.RomBank;
import org.junit.jupiter.api.Test;

import java.io.IOException;

import static org.junit.jupiter.api.Assertions.assertEquals;

final class GPUFloorSwitchTilesTest {

    @Test
    void copiesThePressedButtonTilesToTheSourceVramDestination() throws IOException {
        byte[] rom = loadRom();
        GPU gpu = new GPU();

        gpu.copyPressedFloorSwitchTiles(rom);

        int romOffset = RomBank.romOffset(0x32, 0x7F00);
        int vramOffset = 0x114 * GPU.TILE_DATA_SIZE;
        for (int index = 0; index < 0x40; index++) {
            assertEquals(Byte.toUnsignedInt(rom[romOffset + index]),
                Byte.toUnsignedInt(gpu.readVRAM(vramOffset + index)));
        }
    }

    private static byte[] loadRom() throws IOException {
        try (var stream = GPUFloorSwitchTilesTest.class.getClassLoader()
            .getResourceAsStream("rom/azle.gbc")) {
            if (stream == null) {
                throw new IOException("ROM resource missing");
            }
            return stream.readAllBytes();
        }
    }
}
