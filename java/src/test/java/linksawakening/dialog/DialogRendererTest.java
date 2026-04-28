package linksawakening.dialog;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;

final class DialogRendererTest {

    @Test
    void rendersDarkBoxAndTextPixels() {
        byte[] buffer = new byte[160 * 144 * 4];
        DialogController dialog = new DialogController(16);
        dialog.open("A");
        dialog.advance();

        DialogRenderer.render(buffer, dialog);

        assertEquals(0x18, Byte.toUnsignedInt(buffer[((113 * 160) + 9) * 4]));
        assertEquals(0xFF, Byte.toUnsignedInt(buffer[((124 * 160) + 18) * 4 + 3]));
        assertNotEquals(0x18, Byte.toUnsignedInt(buffer[((124 * 160) + 18) * 4]));
    }
}
