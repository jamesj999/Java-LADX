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
        assertRgb(buffer, BOX_X, 80, SENTINEL);
    }

    @Test
    void rendersBottomBoxAtOriginalYAndLeavesTopAreaUntouched() {
        byte[] buffer = sentinelBuffer();
        DialogController dialog = new DialogController(16);
        dialog.open("A", DialogController.BoxPosition.BOTTOM);
        dialog.advance();

        DialogRenderer.render(buffer, dialog);

        assertRgb(buffer, BOX_X, 80, BLACK);
        assertRgb(buffer, BOX_X + BOX_WIDTH - 1, 80 + BOX_HEIGHT - 1, BLACK);
        assertRgb(buffer, BOX_X - 1, 80, SENTINEL);
        assertRgb(buffer, BOX_X + BOX_WIDTH, 80, SENTINEL);
        assertRgb(buffer, BOX_X, 80 - 1, SENTINEL);
        assertRgb(buffer, BOX_X, 80 + BOX_HEIGHT, SENTINEL);
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
    void gameplayPaletteColorsTheBoxAndGlyph() throws IOException {
        byte[] buffer = sentinelBuffer();
        DialogController dialog = new DialogController(16);
        dialog.open("A");
        dialog.advance();
        int[] palette = { 0xFFFF8B, 0, 0, 0x181818 };

        DialogRenderer.render(buffer, dialog, palette);

        assertRgb(buffer, BOX_X, 80, palette[3]);
        assertRenderedGlyph(buffer, 'A', TEXT_X, 80 + TEXT_Y_OFFSET, palette[0], palette[3]);
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

    @Test
    void drawsBlinkingRomCursorAtBothChoicesAndBoxPositions() {
        for (DialogController.BoxPosition position : DialogController.BoxPosition.values()) {
            DialogController dialog = new DialogController(16);
            dialog.openPreformatted("Choose\n    Okay No Way<ask>", position);
            dialog.advance();
            dialog.advance();
            byte[] buffer = sentinelBuffer();
            int y = position == DialogController.BoxPosition.TOP ? 32 : 104;

            DialogRenderer.render(buffer, dialog);
            assertRgb(buffer, 42, y + 2, BLACK);

            for (int frame = 0; frame < 16; frame++) {
                dialog.tick();
            }
            DialogRenderer.render(buffer, dialog);
            // Tile $3E uses OBJ palette 0 color 2 at row 2, column 2.
            assertRgb(buffer, 42, y + 2, 0x10AC41);
            assertRgb(buffer, 82, y + 2, BLACK);

            dialog.toggleChoiceSelection();
            DialogRenderer.render(buffer, dialog);
            assertRgb(buffer, 42, y + 2, BLACK);
            assertRgb(buffer, 82, y + 2, 0x10AC41);
        }
    }

    private static void assertRenderedGlyph(byte[] buffer, char codepoint, int x, int y) throws IOException {
        assertRenderedGlyph(buffer, codepoint, x, y, WHITE, BLACK);
    }

    private static void assertRenderedGlyph(byte[] buffer, char codepoint, int x, int y,
                                            int textColor, int boxColor) throws IOException {
        byte[] tile = fontTileFor(codepoint);
        for (int row = 0; row < 8; row++) {
            int low = Byte.toUnsignedInt(tile[row * 2]);
            int high = Byte.toUnsignedInt(tile[row * 2 + 1]);
            for (int col = 0; col < 8; col++) {
                int bit = 7 - col;
                int color = ((high >> bit) & 1) << 1 | ((low >> bit) & 1);
                assertRgb(buffer, x + col, y + row, color == 0 ? textColor : boxColor);
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
