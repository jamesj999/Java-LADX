package linksawakening.world;

import linksawakening.rom.RomBank;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

final class ManboTransitionWaveTest {

    @Test
    void followsTheRomPhaseTableAndOddFrameComplement() {
        byte[] rom = loadRom();
        ManboTransitionWave wave = new ManboTransitionWave(rom);

        assertEquals(1, wave.offsetFor(0x20, 0));
        assertEquals(-1, wave.offsetFor(0x21, 0));
        assertEquals(0, wave.offsetFor(0x20, 0x17));

        int reversedSource = RomBank.romOffset(0x14, 0x4EE8 + 0xC9);
        assertEquals((byte) rom[reversedSource], wave.offsetFor(0x20, 0, true));
    }

    private static byte[] loadRom() {
        try (var stream = ManboTransitionWaveTest.class.getClassLoader()
            .getResourceAsStream("rom/azle.gbc")) {
            if (stream == null) {
                throw new IllegalStateException("ROM resource missing");
            }
            return stream.readAllBytes();
        } catch (Exception e) {
            throw new IllegalStateException("Failed to load ROM", e);
        }
    }
}
