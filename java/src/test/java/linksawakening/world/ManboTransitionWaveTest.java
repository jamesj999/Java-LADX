package linksawakening.world;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

final class ManboTransitionWaveTest {

    @Test
    void followsTheRomPhaseTableAndOddFrameComplement() {
        ManboTransitionWave wave = new ManboTransitionWave(loadRom());

        assertEquals(1, wave.offsetFor(0x20, 0));
        assertEquals(-1, wave.offsetFor(0x21, 0));
        assertEquals(0, wave.offsetFor(0x20, 0x17));
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
