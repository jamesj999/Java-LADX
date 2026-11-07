package linksawakening.gpu;

import linksawakening.rom.RomBank;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.assertEquals;

@linksawakening.testing.RequiresRom
final class GPUAnimatedTileCarryTest {
    @Test
    void dungeonAndLavaPingPongDoNotOverwriteTheLinearAnimationOffset() throws Exception {
        byte[] rom;
        try (var input = linksawakening.testing.TestRom.open()) { rom = input.readAllBytes(); }
        for (int group : new int[]{4, 6}) {
            GPU gpu = new GPU();
            gpu.loadAnimatedTilesGroup(rom, 2);
            for (int i = 0; i < 16; i++) gpu.tickAnimatedTiles(rom); // Linear offset $40.
            gpu.loadAnimatedTilesGroup(rom, group);
            for (int i = 0; i < 8; i++) gpu.tickAnimatedTiles(rom); // Ping-pong frame $C0.
            assertFrame(rom, gpu, (group == 4 ? 0x6D00 : 0x6F00) + 0xC0);
            gpu.loadAnimatedTilesGroup(rom, 2);
            for (int i = 0; i < 8; i++) gpu.tickAnimatedTiles(rom);
            assertEquals(32, gpu.animatedTilesFrameCount());
            assertFrame(rom, gpu, 0x6B80); // Next linear frame is $80, not $00.
        }
    }

    private static void assertFrame(byte[] rom, GPU gpu, int address) {
        int source = RomBank.romOffset(0x2C, address);
        for (int i = 0; i < 0x40; i++) assertEquals(rom[source + i], gpu.readVRAM(0x16C0 + i));
    }
}
