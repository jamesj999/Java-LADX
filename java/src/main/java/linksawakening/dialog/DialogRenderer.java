package linksawakening.dialog;

import linksawakening.gpu.Framebuffer;

public final class DialogRenderer {

    private static final int BOX_X = 8;
    private static final int BOX_Y = 112;
    private static final int BOX_WIDTH = 144;
    private static final int BOX_HEIGHT = 28;
    private static final int TEXT_X = 16;
    private static final int TEXT_Y = 124;
    private static final int CHAR_WIDTH = 6;
    private static final int LINE_HEIGHT = 8;
    private static final int BOX_COLOR = 0x181818;
    private static final int BORDER_COLOR = 0xF8F8F8;
    private static final int TEXT_COLOR = 0xF8F8F8;

    private DialogRenderer() {
    }

    public static void render(byte[] buffer, DialogController dialog) {
        if (buffer == null || dialog == null || !dialog.isActive()) {
            return;
        }
        fillRect(buffer, BOX_X, BOX_Y, BOX_WIDTH, BOX_HEIGHT, BOX_COLOR);
        drawRect(buffer, BOX_X, BOX_Y, BOX_WIDTH, BOX_HEIGHT, BORDER_COLOR);
        drawText(buffer, dialog.visibleText(), TEXT_X, TEXT_Y);
    }

    private static void drawText(byte[] buffer, String text, int x, int y) {
        int cursorX = x;
        int cursorY = y;
        for (int i = 0; i < text.length(); i++) {
            char ch = Character.toUpperCase(text.charAt(i));
            if (ch == '\n') {
                cursorX = x;
                cursorY += LINE_HEIGHT;
                continue;
            }
            drawGlyph(buffer, ch, cursorX, cursorY);
            cursorX += CHAR_WIDTH;
        }
    }

    private static void drawGlyph(byte[] buffer, char ch, int x, int y) {
        int[] rows = glyph(ch);
        for (int row = 0; row < rows.length; row++) {
            int bits = rows[row];
            for (int col = 0; col < 5; col++) {
                if (((bits >> (4 - col)) & 1) != 0) {
                    putPixel(buffer, x + col, y + row, TEXT_COLOR);
                }
            }
        }
    }

    private static int[] glyph(char ch) {
        return switch (ch) {
            case 'A' -> rows("01110", "10001", "10001", "11111", "10001", "10001", "10001");
            case 'B' -> rows("11110", "10001", "10001", "11110", "10001", "10001", "11110");
            case 'C' -> rows("01111", "10000", "10000", "10000", "10000", "10000", "01111");
            case 'D' -> rows("11110", "10001", "10001", "10001", "10001", "10001", "11110");
            case 'E' -> rows("11111", "10000", "10000", "11110", "10000", "10000", "11111");
            case 'F' -> rows("11111", "10000", "10000", "11110", "10000", "10000", "10000");
            case 'G' -> rows("01111", "10000", "10000", "10011", "10001", "10001", "01111");
            case 'H' -> rows("10001", "10001", "10001", "11111", "10001", "10001", "10001");
            case 'I' -> rows("11111", "00100", "00100", "00100", "00100", "00100", "11111");
            case 'J' -> rows("00111", "00010", "00010", "00010", "10010", "10010", "01100");
            case 'K' -> rows("10001", "10010", "10100", "11000", "10100", "10010", "10001");
            case 'L' -> rows("10000", "10000", "10000", "10000", "10000", "10000", "11111");
            case 'M' -> rows("10001", "11011", "10101", "10101", "10001", "10001", "10001");
            case 'N' -> rows("10001", "11001", "10101", "10011", "10001", "10001", "10001");
            case 'O' -> rows("01110", "10001", "10001", "10001", "10001", "10001", "01110");
            case 'P' -> rows("11110", "10001", "10001", "11110", "10000", "10000", "10000");
            case 'Q' -> rows("01110", "10001", "10001", "10001", "10101", "10010", "01101");
            case 'R' -> rows("11110", "10001", "10001", "11110", "10100", "10010", "10001");
            case 'S' -> rows("01111", "10000", "10000", "01110", "00001", "00001", "11110");
            case 'T' -> rows("11111", "00100", "00100", "00100", "00100", "00100", "00100");
            case 'U' -> rows("10001", "10001", "10001", "10001", "10001", "10001", "01110");
            case 'V' -> rows("10001", "10001", "10001", "10001", "10001", "01010", "00100");
            case 'W' -> rows("10001", "10001", "10001", "10101", "10101", "10101", "01010");
            case 'X' -> rows("10001", "10001", "01010", "00100", "01010", "10001", "10001");
            case 'Y' -> rows("10001", "10001", "01010", "00100", "00100", "00100", "00100");
            case 'Z' -> rows("11111", "00001", "00010", "00100", "01000", "10000", "11111");
            case '0' -> rows("01110", "10001", "10011", "10101", "11001", "10001", "01110");
            case '1' -> rows("00100", "01100", "00100", "00100", "00100", "00100", "01110");
            case '2' -> rows("01110", "10001", "00001", "00010", "00100", "01000", "11111");
            case '3' -> rows("11110", "00001", "00001", "01110", "00001", "00001", "11110");
            case '4' -> rows("00010", "00110", "01010", "10010", "11111", "00010", "00010");
            case '5' -> rows("11111", "10000", "10000", "11110", "00001", "00001", "11110");
            case '6' -> rows("01110", "10000", "10000", "11110", "10001", "10001", "01110");
            case '7' -> rows("11111", "00001", "00010", "00100", "01000", "01000", "01000");
            case '8' -> rows("01110", "10001", "10001", "01110", "10001", "10001", "01110");
            case '9' -> rows("01110", "10001", "10001", "01111", "00001", "00001", "01110");
            case '.', '!' -> rows("00000", "00000", "00000", "00000", "00000", "00100", "00100");
            case ',' -> rows("00000", "00000", "00000", "00000", "00000", "00100", "01000");
            case '?' -> rows("01110", "10001", "00001", "00010", "00100", "00000", "00100");
            case '\'' -> rows("00100", "00100", "01000", "00000", "00000", "00000", "00000");
            case ' ' -> rows("00000", "00000", "00000", "00000", "00000", "00000", "00000");
            default -> rows("11111", "10001", "00010", "00100", "00100", "00000", "00100");
        };
    }

    private static int[] rows(String... values) {
        int[] rows = new int[values.length];
        for (int i = 0; i < values.length; i++) {
            rows[i] = Integer.parseInt(values[i], 2);
        }
        return rows;
    }

    private static void fillRect(byte[] buffer, int x, int y, int width, int height, int color) {
        for (int py = y; py < y + height; py++) {
            for (int px = x; px < x + width; px++) {
                putPixel(buffer, px, py, color);
            }
        }
    }

    private static void drawRect(byte[] buffer, int x, int y, int width, int height, int color) {
        for (int px = x; px < x + width; px++) {
            putPixel(buffer, px, y, color);
            putPixel(buffer, px, y + height - 1, color);
        }
        for (int py = y; py < y + height; py++) {
            putPixel(buffer, x, py, color);
            putPixel(buffer, x + width - 1, py, color);
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
