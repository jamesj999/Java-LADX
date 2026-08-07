package linksawakening.vfx;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

final class TransientVfxSwordPokeTest {

    @Test
    void decodesBothRomSwordPokePhasesAndOamOffsets() {
        TransientVfxSpriteSheet sheet = TransientVfxSpriteSheet.loadFromRom(loadRom());
        CutLeavesEffectRenderer renderer = new CutLeavesEffectRenderer(sheet);

        var tilePhase = renderer.renderSwordPoke(0x40, 0x50, 0x07);
        assertEquals(2, tilePhase.size());
        assertEquals(0x40 - 1 - 8, tilePhase.get(0).x());
        assertEquals(0x50 - 16, tilePhase.get(0).y());
        assertEquals(0x3C, tilePhase.get(0).tileId());
        assertEquals(0x00, tilePhase.get(0).attributes());
        assertEquals(0x40 + 7 - 8, tilePhase.get(1).x());
        assertEquals(0x50 - 16, tilePhase.get(1).y());
        assertEquals(0x3C, tilePhase.get(1).tileId());
        assertEquals(0x20, tilePhase.get(1).attributes());

        var otherPhase = renderer.renderSwordPoke(0x40, 0x50, 0x08);
        assertEquals(0x3A, otherPhase.get(0).tileId());
        assertEquals(0x3A, otherPhase.get(1).tileId());
        assertEquals(0x00, otherPhase.get(0).attributes());
        assertEquals(0x20, otherPhase.get(1).attributes());
        assertNotNull(otherPhase.get(0).tile());
        assertNotNull(otherPhase.get(1).tile());
    }

    @Test
    void decodesTheRomSmokePhasesAndOamOffsets() {
        TransientVfxSpriteSheet sheet = TransientVfxSpriteSheet.loadFromRom(loadRom());
        CutLeavesEffectRenderer renderer = new CutLeavesEffectRenderer(sheet);

        var first = renderer.renderSmoke(0x40, 0x50, 0x07);
        var second = renderer.renderSmoke(0x40, 0x50, 0x08);

        assertEquals(2, first.size());
        assertEquals(0x40 - 8, first.get(0).x());
        assertEquals(0x50 - 16, first.get(0).y());
        assertEquals(0x1E, first.get(0).tileId());
        assertEquals(0x01, first.get(0).attributes());
        assertEquals(0x1E, first.get(1).tileId());
        assertEquals(0x61, first.get(1).attributes());
        assertEquals(0x30, second.get(0).tileId());
        assertEquals(0x30, second.get(1).tileId());
        assertNotNull(first.get(0).tile());
    }

    private static byte[] loadRom() {
        try (var stream = TransientVfxSwordPokeTest.class.getClassLoader()
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
