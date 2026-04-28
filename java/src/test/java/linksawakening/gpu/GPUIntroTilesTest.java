package linksawakening.gpu;

import linksawakening.rom.RomBank;
import org.junit.jupiter.api.Test;

import java.io.InputStream;

import static org.junit.jupiter.api.Assertions.assertEquals;

final class GPUIntroTilesTest {

    private static final byte[] ROM = loadRom();

    @Test
    void introSequenceTilesFollowTilesetIntroLoadOrder() {
        GPU gpu = new GPU();

        gpu.loadIntroSequenceTiles(ROM);

        assertVramMatchesRom(gpu, 0x0000, 0x30, 0x5400, 0x10);
        assertVramMatchesRom(gpu, 0x0700, 0x01, 0x6D4A, 0x10);
        assertVramMatchesRom(gpu, 0x0800, 0x30, 0x4000, 0x10);
    }

    @Test
    void titleTilesOverwriteIntroTilesOnlyWhenTitleTilesetIsLoaded() {
        GPU gpu = new GPU();

        gpu.loadIntroSequenceTiles(ROM);
        assertVramMatchesRom(gpu, 0x0800, 0x30, 0x4000, 0x10);

        gpu.loadTitleScreenTiles(ROM);
        assertVramMatchesRom(gpu, 0x0800, 0x2F, 0x4900, 0x10);
        assertVramMatchesRom(gpu, 0x0200, 0x38, 0x6500, 0x10);
    }

    private static void assertVramMatchesRom(GPU gpu, int vramOffset,
                                             int bank, int address, int length) {
        int romOffset = RomBank.romOffset(bank, address);
        for (int i = 0; i < length; i++) {
            assertEquals(Byte.toUnsignedInt(ROM[romOffset + i]),
                Byte.toUnsignedInt(gpu.readVRAM(vramOffset + i)),
                "VRAM " + String.format("0x%04X", vramOffset + i));
        }
    }

    private static byte[] loadRom() {
        try (InputStream stream = GPUIntroTilesTest.class.getClassLoader().getResourceAsStream("rom/azle.gbc")) {
            if (stream == null) {
                throw new IllegalStateException("ROM resource missing");
            }
            return stream.readAllBytes();
        } catch (Exception e) {
            throw new IllegalStateException("Failed to load ROM", e);
        }
    }
}
