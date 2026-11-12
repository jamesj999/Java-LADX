package linksawakening.dialog;

import linksawakening.gpu.Framebuffer;

/** Java pixel adapter for the tile rows published by DialogDrawNextCharacterHandler. */
public final class DialogRenderer {

    private static final int BOX_X = 8;
    private static final int BOX_WIDTH = 18 * 8;
    private static final int BOX_HEIGHT = 0x28;
    private static final int TEXT_X = 16;
    private static final int TEXT_Y_OFFSET = 8;
    private static final int CELL_SIZE = 8;
    private static final int LINE_HEIGHT = 16;
    private static final int LINE_WIDTH = 16;
    private static final int VISIBLE_LINES = 2;
    private static final int BOX_COLOR = 0x000000;
    private static final int TEXT_COLOR = 0xF8F8F8;
    private static final DialogFont FONT = DialogFont.loadDefault();

    private DialogRenderer() {}

    public static void render(byte[] buffer, DialogController dialog) {
        render(buffer, dialog, new int[] {TEXT_COLOR, BOX_COLOR, BOX_COLOR, BOX_COLOR});
    }

    public static void render(byte[] buffer, DialogController dialog, int[] palette) {
        if (buffer == null || dialog == null || !dialog.isActive()) {
            return;
        }

        int boxY = dialog.boxY();
        // DialogOpenAnimationEnd assigns CGB BG palette 1 to the whole box.
        for (int row = 0; row < BOX_HEIGHT / CELL_SIZE; row++) {
            if (dialog.isBoxRowVisible(row)) {
                fillRect(buffer, BOX_X, boxY + row * CELL_SIZE, BOX_WIDTH, CELL_SIZE, palette[3]);
            }
        }
        drawText(
                buffer,
                dialog.visibleText(),
                TEXT_X,
                boxY + TEXT_Y_OFFSET + dialog.textScrollOffset(),
                palette,
                dialog);
    }

    private static void drawText(byte[] buffer, String text, int x, int y, int[] palette, DialogController dialog) {
        int column = 0;
        int row = 0;
        for (int i = 0; i < text.length() && row < VISIBLE_LINES; i++) {
            char codepoint = text.charAt(i);
            if (codepoint == '\n') {
                column = 0;
                row++;
                continue;
            }
            if (column >= LINE_WIDTH) {
                column = 0;
                row++;
                if (row >= VISIBLE_LINES) {
                    break;
                }
            }
            int boxRow = (y + row * LINE_HEIGHT - dialog.boxY()) / CELL_SIZE;
            if (codepoint != ' ' && dialog.isBoxRowVisible(boxRow)) {
                drawGlyph(buffer, FONT.tileFor(codepoint), x + column * CELL_SIZE, y + row * LINE_HEIGHT, palette);
            }
            column++;
        }
    }

    private static void drawGlyph(byte[] buffer, byte[] tile, int x, int y, int[] palette) {
        for (int row = 0; row < CELL_SIZE; row++) {
            int low = Byte.toUnsignedInt(tile[row * 2]);
            int high = Byte.toUnsignedInt(tile[row * 2 + 1]);
            for (int col = 0; col < CELL_SIZE; col++) {
                int bit = 7 - col;
                int color = ((high >> bit) & 1) << 1 | ((low >> bit) & 1);
                putPixel(buffer, x + col, y + row, palette[color]);
            }
        }
    }

    private static void fillRect(byte[] buffer, int x, int y, int width, int height, int color) {
        for (int py = y; py < y + height; py++) {
            for (int px = x; px < x + width; px++) {
                putPixel(buffer, px, py, color);
            }
        }
    }

    private static void putPixel(byte[] buffer, int x, int y, int color) {
        if (x < 0 || x >= Framebuffer.WIDTH || y < 0 || y >= Framebuffer.HEIGHT) {
            return;
        }
        int offset = (y * Framebuffer.WIDTH + x) * 4;
        buffer[offset] = (byte) ((color >> 16) & 0xFF);
        buffer[offset + 1] = (byte) ((color >> 8) & 0xFF);
        buffer[offset + 2] = (byte) (color & 0xFF);
        buffer[offset + 3] = (byte) 0xFF;
    }
    /** Indexed counterpart; dialog_letters.asm $4A84 writes attribute $01 (no priority bit). */
    public static void submitWindow(
            linksawakening.render.HardwareFrame frame, DialogController dialog, int[] suppliedPalette) {
        if (!dialog.isActive()) return;
        int[] palette =
                suppliedPalette == null ? new int[] {TEXT_COLOR, BOX_COLOR, BOX_COLOR, BOX_COLOR} : suppliedPalette;
        int boxY = dialog.boxY();
        for (int row = 0; row < BOX_HEIGHT / CELL_SIZE; row++) {
            if (!dialog.isBoxRowVisible(row)) continue;
            for (int y = 0; y < CELL_SIZE; y++)
                for (int x = 0; x < BOX_WIDTH; x++)
                    frame.backgroundPixel(BOX_X + x, boxY + row * CELL_SIZE + y, 3, palette, -1, 0x01);
        }
        int column = 0, row = 0;
        String text = dialog.visibleText();
        int originY = boxY + TEXT_Y_OFFSET + dialog.textScrollOffset();
        for (int i = 0; i < text.length() && row < VISIBLE_LINES; i++) {
            char codepoint = text.charAt(i);
            if (codepoint == '\n') {
                column = 0;
                row++;
                continue;
            }
            if (column >= LINE_WIDTH) {
                column = 0;
                if (++row >= VISIBLE_LINES) break;
            }
            int y = originY + row * LINE_HEIGHT;
            int boxRow = (y - boxY) / CELL_SIZE;
            if (codepoint != ' ' && dialog.isBoxRowVisible(boxRow)) {
                byte[] tile = FONT.tileFor(codepoint);
                for (int ty = 0; ty < 8; ty++)
                    for (int tx = 0; tx < 8; tx++) {
                        int bit = 7 - tx;
                        int index = ((tile[ty * 2] >> bit) & 1) | (((tile[ty * 2 + 1] >> bit) & 1) << 1);
                        frame.backgroundPixel(TEXT_X + column * CELL_SIZE + tx, y + ty, index, palette, -1, 0x01);
                    }
            }
            column++;
        }
    }
}
