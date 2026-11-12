package linksawakening.gpu;

import static org.junit.jupiter.api.Assertions.assertEquals;

import linksawakening.rom.Rom;
import linksawakening.rom.RomBank;
import org.junit.jupiter.api.Test;

@linksawakening.testing.RequiresRom
final class GPUMushroomTilesTest {
    @Test
    void mushroomSurvivesBaseReloadAndPowderRewardRestoresTheBag() throws Exception {
        byte[] rom;
        try (var in = linksawakening.testing.TestRom.open()) {
            rom = in.readAllBytes();
        }
        GPU gpu = new GPU();
        gpu.loadBaseTiles(Rom.of(rom));
        gpu.updatePowderInventoryTiles(Rom.of(rom), true);
        assertPair(gpu, rom, 0x68C0);
        gpu.loadBaseOverworldTiles(Rom.of(rom));
        gpu.updatePowderInventoryTiles(Rom.of(rom), true);
        assertPair(gpu, rom, 0x68C0);
        gpu.updatePowderInventoryTiles(Rom.of(rom), false);
        assertPair(gpu, rom, 0x48E0);
    }

    private static void assertPair(GPU gpu, byte[] rom, int address) {
        int source = RomBank.romOffset(0x2C, address);
        for (int i = 0; i < 32; i++) {
            assertEquals(rom[source + i], gpu.readVRAM(0x8E0 + i), "Byte " + i);
        }
    }
}
