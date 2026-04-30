package linksawakening.dialog;

import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.io.InputStream;

import static org.junit.jupiter.api.Assertions.assertEquals;

final class DialogRendererTest {

    private static final int SCREEN_WIDTH = 160;
    private static final int SCREEN_HEIGHT = 144;
    private static final int BYTES_PER_PIXEL = 4;
    private static final int BOX_X = 8;
    private static final int BOX_WIDTH = 18 * 8;
    private static final int BOX_HEIGHT = 0x28;
    private static final int TEXT_X = 16;
    private static final int TEXT_Y_OFFSET = 8;
    private static final int TEXT_ROW_STRIDE = 16;
    private static final int BLACK = 0x000000;
    private static final int WHITE = 0xF8F8F8;
    private static final int SENTINEL = 0x123456;
    private static final DialogFont EXPECTED_FONT = DialogFont.loadFromRom(classpathRom());

    @Test
    void rendersTopBoxAtOriginalYAndLeavesBottomAreaUntouched() {
        byte[] buffer = sentinelBuffer();
        DialogController dialog = new DialogController(16);
        dialog.open("A", DialogController.BoxPosition.TOP);
        dialog.advance();

        DialogRenderer.render(buffer, dialog);

        assertRgb(buffer, BOX_X, 8, BLACK);
        assertRgb(buffer, BOX_X + BOX_WIDTH - 1, 8 + BOX_HEIGHT - 1, BLACK);
        assertRgb(buffer, BOX_X - 1, 8, SENTINEL);
        assertRgb(buffer, BOX_X + BOX_WIDTH, 8, SENTINEL);
        assertRgb(buffer, BOX_X, 8 - 1, SENTINEL);
        assertRgb(buffer, BOX_X, 8 + BOX_HEIGHT, SENTINEL);
        assertRgb(buffer, BOX_X, 96, SENTINEL);
    }

    @Test
    void rendersBottomBoxAtOriginalYAndLeavesTopAreaUntouched() {
        byte[] buffer = sentinelBuffer();
        DialogController dialog = new DialogController(16);
        dialog.open("A", DialogController.BoxPosition.BOTTOM);
        dialog.advance();

        DialogRenderer.render(buffer, dialog);

        assertRgb(buffer, BOX_X, 96, BLACK);
        assertRgb(buffer, BOX_X + BOX_WIDTH - 1, 96 + BOX_HEIGHT - 1, BLACK);
        assertRgb(buffer, BOX_X - 1, 96, SENTINEL);
        assertRgb(buffer, BOX_X + BOX_WIDTH, 96, SENTINEL);
        assertRgb(buffer, BOX_X, 96 - 1, SENTINEL);
        assertRgb(buffer, BOX_X, 96 + BOX_HEIGHT, SENTINEL);
        assertRgb(buffer, BOX_X, 8, SENTINEL);
    }

    @Test
    void rendersGlyphPixelsFromRomFontTileSelectedByCodepointMap() throws IOException {
        byte[] buffer = sentinelBuffer();
        DialogController dialog = new DialogController(16);
        dialog.open("A", DialogController.BoxPosition.TOP);
        dialog.advance();

        DialogRenderer.render(buffer, dialog);

        assertRenderedGlyph(buffer, 'A', TEXT_X, 8 + TEXT_Y_OFFSET);
    }

    @Test
    void spacesRemainBlankButStillAdvanceToTheNextCell() throws IOException {
        byte[] buffer = sentinelBuffer();
        DialogController dialog = new DialogController(16);
        dialog.open("A A", DialogController.BoxPosition.TOP);
        dialog.advance();

        DialogRenderer.render(buffer, dialog);

        assertRenderedGlyph(buffer, 'A', TEXT_X, 8 + TEXT_Y_OFFSET);
        assertBlankGlyphCell(buffer, TEXT_X + 8, 8 + TEXT_Y_OFFSET);
        assertRenderedGlyph(buffer, 'A', TEXT_X + 16, 8 + TEXT_Y_OFFSET);
    }

    @Test
    void rendersOnlyVisibleTextFromCurrentPage() {
        byte[] buffer = sentinelBuffer();
        DialogController dialog = new DialogController(16);
        dialog.open("12345678901234567890123456789012A", DialogController.BoxPosition.TOP);
        dialog.advance();

        DialogRenderer.render(buffer, dialog);

        assertRgb(buffer, TEXT_X, 8 + TEXT_Y_OFFSET, BLACK);
        assertRgb(buffer, TEXT_X, 8 + TEXT_Y_OFFSET + TEXT_ROW_STRIDE, BLACK);
    }

    private static void assertRenderedGlyph(byte[] buffer, char codepoint, int x, int y) throws IOException {
        byte[] tile = fontTileFor(codepoint);
        for (int row = 0; row < 8; row++) {
            int low = Byte.toUnsignedInt(tile[row * 2]);
            int high = Byte.toUnsignedInt(tile[row * 2 + 1]);
            for (int col = 0; col < 8; col++) {
                int bit = 7 - col;
                int color = ((high >> bit) & 1) << 1 | ((low >> bit) & 1);
                assertRgb(buffer, x + col, y + row, color == 0 ? WHITE : BLACK);
            }
        }
    }

    private static void assertBlankGlyphCell(byte[] buffer, int x, int y) {
        for (int row = 0; row < 8; row++) {
            for (int col = 0; col < 8; col++) {
                assertRgb(buffer, x + col, y + row, BLACK);
            }
        }
    }

    private static byte[] fontTileFor(char codepoint) throws IOException {
        return EXPECTED_FONT.tileFor(codepoint);
    }

    private static byte[] classpathRom() {
        try (InputStream stream = DialogRendererTest.class.getResourceAsStream("/rom/azle.gbc")) {
            if (stream == null) {
                throw new IllegalStateException("Missing classpath ROM resource /rom/azle.gbc");
            }
            return stream.readAllBytes();
        } catch (IOException exception) {
            throw new IllegalStateException("Unable to read classpath ROM resource /rom/azle.gbc", exception);
        }
    }

    private static byte[] sentinelBuffer() {
        byte[] buffer = new byte[SCREEN_WIDTH * SCREEN_HEIGHT * BYTES_PER_PIXEL];
        for (int y = 0; y < SCREEN_HEIGHT; y++) {
            for (int x = 0; x < SCREEN_WIDTH; x++) {
                writeRgb(buffer, x, y, SENTINEL);
            }
        }
        return buffer;
    }

    private static void assertRgb(byte[] buffer, int x, int y, int expected) {
        int offset = ((y * SCREEN_WIDTH) + x) * BYTES_PER_PIXEL;
        int actual = (Byte.toUnsignedInt(buffer[offset]) << 16)
                | (Byte.toUnsignedInt(buffer[offset + 1]) << 8)
                | Byte.toUnsignedInt(buffer[offset + 2]);
        assertEquals(expected, actual, "pixel " + x + "," + y);
        assertEquals(0xFF, Byte.toUnsignedInt(buffer[offset + 3]), "alpha " + x + "," + y);
    }

    private static void writeRgb(byte[] buffer, int x, int y, int color) {
        int offset = ((y * SCREEN_WIDTH) + x) * BYTES_PER_PIXEL;
        buffer[offset] = (byte) ((color >> 16) & 0xFF);
        buffer[offset + 1] = (byte) ((color >> 8) & 0xFF);
        buffer[offset + 2] = (byte) (color & 0xFF);
        buffer[offset + 3] = (byte) 0xFF;
    }
}
