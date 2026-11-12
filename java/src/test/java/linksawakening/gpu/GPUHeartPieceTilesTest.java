package linksawakening.gpu;

import static org.junit.jupiter.api.Assertions.assertEquals;

import linksawakening.rom.RomBank;
import linksawakening.testing.TestRoms;
import org.junit.jupiter.api.Test;

@linksawakening.testing.RequiresRom
final class GPUHeartPieceTilesTest {
    @Test
    void meterRestoresEquipmentAndCurrentTradingItem() throws Exception {
        byte[] rom;
        try (var in = linksawakening.testing.TestRom.open()) {
            rom = in.readAllBytes();
        }
        GPU gpu = new GPU();
        gpu.loadBaseTiles(rom);
        gpu.updateHeartPieceMeterTiles(TestRoms.partial(rom), 3);
        gpu.updateHeartPieceMeterTiles(TestRoms.partial(rom), 4);
        assertBytes(gpu, rom, 0x6900, 0x9A0, 96);
        gpu.updateHeartPieceMeterTiles(TestRoms.partial(rom), 5);
        gpu.updateHeartPieceMeterTiles(TestRoms.partial(rom), 6);
        gpu.updateTradingItemTiles(TestRoms.partial(rom), 0);
        assertBytes(gpu, rom, 0x49A0, 0x9A0, 96);
        gpu.updateTradingItemTiles(TestRoms.partial(rom), 3);
        assertBytes(gpu, rom, 0x4440, 0x9A0, 64);
        assertBytes(gpu, rom, 0x49E0, 0x9E0, 32);
    }

    private static void assertBytes(GPU gpu, byte[] rom, int address, int destination, int count) {
        int source = RomBank.romOffset(0x2C, address);
        for (int i = 0; i < count; i++) assertEquals(rom[source + i], gpu.readVRAM(destination + i), "Byte " + i);
    }
}
