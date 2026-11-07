package linksawakening.app.debug;

import java.nio.charset.StandardCharsets;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class DebugMenuOverlayTest {
    @Test void rendererNeverInitializesAnotherWindowToolkit() throws Exception {
        try (var stream = DebugMenuOverlay.class.getResourceAsStream("DebugMenuOverlay.class")) {
            assertNotNull(stream);
            String bytecode = new String(stream.readAllBytes(), StandardCharsets.ISO_8859_1);
            assertFalse(bytecode.contains("java/awt"), "AWT can take over the macOS GLFW event loop");
            assertFalse(bytecode.contains("javax/swing"));
        }
    }

    @Test void textWritesRgbaAndClipsAtFramebufferEdges() {
        byte[] pixels = new byte[160 * 144 * 4];
        DebugMenuOverlay.text(pixels, "ABCDEFGHIJKLMNOPQRSTUVWXYZ0123456789 :/()->.?+", 0, 0, 0x123456);
        boolean painted = false;
        for (int i = 0; i < pixels.length; i += 4) {
            if (pixels[i + 3] == 0) continue;
            painted = true;
            assertEquals(0x12, Byte.toUnsignedInt(pixels[i]));
            assertEquals(0x34, Byte.toUnsignedInt(pixels[i + 1]));
            assertEquals(0x56, Byte.toUnsignedInt(pixels[i + 2]));
            assertEquals(255, Byte.toUnsignedInt(pixels[i + 3]));
            assertTrue(i / (160 * 4) < 7);
        }
        assertTrue(painted);
        assertDoesNotThrow(() -> DebugMenuOverlay.text(pixels, "Outside", -8, -4, 0));
        assertDoesNotThrow(() -> DebugMenuOverlay.text(pixels, "Outside", 158, 142, 0));
        byte[] lower = new byte[pixels.length], upper = new byte[pixels.length];
        DebugMenuOverlay.text(lower, "F4/Esc close", 2, 2, 0xFFFFFF);
        DebugMenuOverlay.text(upper, "F4/ESC CLOSE", 2, 2, 0xFFFFFF);
        assertArrayEquals(upper, lower);
    }
}
