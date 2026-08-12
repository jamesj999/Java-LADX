package linksawakening.vfx;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

final class TransientVfxMovingSparkleTest {

    @Test
    void rendersTheRomSparklePairAndDirectionalMotion() {
        CutLeavesEffectRenderer renderer = new CutLeavesEffectRenderer(
            TransientVfxSpriteSheet.loadFromRom(loadRom()));

        var initial = renderer.renderMovingSparkle(0x40, 0x50, 0x22, 0);
        assertEquals(2, initial.size());
        assertEquals(0x38, initial.get(0).x());
        assertEquals(0x40, initial.get(0).y());
        assertEquals(0x3A, initial.get(0).tileId());
        assertEquals(0x00, initial.get(0).attributes());
        assertEquals(0x40, initial.get(1).x());
        assertEquals(0x20, initial.get(1).attributes());

        var inward = renderer.renderMovingSparkle(0x40, 0x50, 0x20, 3);
        assertEquals(0x36, inward.get(0).x());
        assertEquals(0x3E, inward.get(0).y());
        assertNotNull(inward.get(0).tile());

        var finalPhase = renderer.renderMovingSparkle(0x40, 0x50, 0x06, 0);
        assertEquals(0x3C, finalPhase.get(0).tileId());
    }

    private static byte[] loadRom() {
        try (var stream = TransientVfxMovingSparkleTest.class.getClassLoader()
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
