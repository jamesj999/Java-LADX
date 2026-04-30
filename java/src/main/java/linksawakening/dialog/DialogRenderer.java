package linksawakening.dialog;

import linksawakening.gpu.Framebuffer;

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

    private DialogRenderer() {
    }

    public static void render(byte[] buffer, DialogController dialog) {
        if (buffer == null || dialog == null || !dialog.isActive()) {
            return;
        }

        int boxY = dialog.boxY();
        fillRect(buffer, BOX_X, boxY, BOX_WIDTH, BOX_HEIGHT, BOX_COLOR);
        drawText(buffer, dialog.visibleText(), TEXT_X, boxY + TEXT_Y_OFFSET);
    }

    private static void drawText(byte[] buffer, String text, int x, int y) {
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
            if (codepoint != ' ') {
                drawGlyph(buffer, FONT.tileFor(codepoint), x + column * CELL_SIZE, y + row * LINE_HEIGHT);
            }
            column++;
        }
    }

    private static void drawGlyph(byte[] buffer, byte[] tile, int x, int y) {
        for (int row = 0; row < CELL_SIZE; row++) {
            int low = Byte.toUnsignedInt(tile[row * 2]);
            int high = Byte.toUnsignedInt(tile[row * 2 + 1]);
            for (int col = 0; col < CELL_SIZE; col++) {
                int bit = 7 - col;
                int color = ((high >> bit) & 1) << 1 | ((low >> bit) & 1);
                if (color == 0) {
                    putPixel(buffer, x + col, y + row, TEXT_COLOR);
                }
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
}
