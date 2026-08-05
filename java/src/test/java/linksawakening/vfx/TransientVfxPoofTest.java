package linksawakening.vfx;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

final class TransientVfxPoofTest {

    @Test
    void decodesTheRomPoofFramesAndOamOrigin() {
        TransientVfxSpriteSheet sheet = TransientVfxSpriteSheet.loadFromRom(loadRom());
        CutLeavesEffectRenderer renderer = new CutLeavesEffectRenderer(sheet);

        var small = renderer.renderPoof(0x40, 0x50, 0x03);
        assertEquals(0x38, small.get(0).x());
        assertEquals(0x40, small.get(0).y());
        assertEquals(0x32, small.get(0).tileId());
        assertEquals(0x21, small.get(1).attributes());

        var large = renderer.renderPoof(0x40, 0x50, 0x0F);
        assertEquals(0x30, large.get(0).tileId());
        assertEquals(0x30, large.get(1).tileId());
    }

    private static byte[] loadRom() {
        try (var stream = TransientVfxPoofTest.class.getClassLoader()
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
