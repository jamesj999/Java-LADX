package linksawakening.app.debug;

/**
 * Development-only bitmap text, drawn directly into the game's RGBA buffer.
 * Do not initialize AWT/Java2D here: on macOS its native toolkit interferes
 * with GLFW's main-thread event loop, even when drawing an offscreen image.
 * The small font below is engine-authored and contains no ROM graphics.
 */
public final class DebugMenuOverlay {
    private static final int WIDTH = 160;
    private static final int HEIGHT = 144;
    private static final int BACKGROUND = 0x101828;
    private static final int HIGHLIGHT = 0x284058;
    private static final int ACCENT = 0x70E0FF;
    private static final String CHARACTERS = "ABCDEFGHIJKLMNOPQRSTUVWXYZ0123456789 :/()->.?+";
    // Seven rows per glyph, one four-pixel row per hex nibble, top to bottom.
    private static final int[] GLYPHS = {
        0x699F999, 0xE99E99E, 0x7888887, 0xE99999E, 0xF88E88F, 0xF88E888, 0x788B997, 0x999F999, 0x7222227, 0x1111996,
        0x99ACA99, 0x888888F, 0x9FF9999, 0x9DDBB99, 0x6999996, 0xE99E888, 0x6999B67, 0xE99EA99, 0x788611E, 0xF222222,
        0x9999996, 0x9999966, 0x9999FF9, 0x9966699, 0x9996222, 0xF11248F, 0x6999996, 0x2622227, 0x691248F, 0xE11611E,
        0x999F111, 0xF88E11E, 0x688E996, 0xF112244, 0x6996996, 0x6997116, 0x0000000, 0x0220220, 0x1122448, 0x2488842,
        0x4211124, 0x000F000, 0x8421248, 0x0000022, 0x6912202, 0x0027200
    };

    public void render(DebugMenu menu, byte[] rgba) {
        if (!menu.isOpen()) return;
        if (rgba.length != WIDTH * HEIGHT * 4) throw new IllegalArgumentException("Expected 160x144 RGBA");
        rectangle(rgba, 0, 0, WIDTH, HEIGHT, BACKGROUND);
        text(rgba, menu.title(), 3, 2, ACCENT);
        var entries = menu.entries();
        int first = (menu.selected() / 9) * 9;
        for (int i = first; i < Math.min(first + 9, entries.size()); i++) {
            int y = 16 + (i - first) * 10;
            if (i == menu.selected()) rectangle(rgba, 0, y - 1, WIDTH, 10, HIGHLIGHT);
            text(rgba, (i == menu.selected() ? ">" : " ") + entries.get(i).text(), 2, y, 0xFFFFFF);
        }
        text(rgba, menu.message().isEmpty() ? "Up/Down select  L/R edit" : menu.message(), 2, 107, ACCENT);
        text(rgba, "Tab/PgUp/PgDn pages", 2, 117, ACCENT);
        text(rgba, "Shift x16  F4/Esc close", 2, 127, ACCENT);
    }

    static void text(byte[] rgba, String text, int x, int y, int color) {
        for (int i = 0; i < text.length(); i++, x += 5) {
            int index = CHARACTERS.indexOf(Character.toUpperCase(text.charAt(i)));
            int glyph = GLYPHS[index < 0 ? CHARACTERS.indexOf('?') : index];
            for (int row = 0; row < 7; row++) {
                int bits = (glyph >>> ((6 - row) * 4)) & 15;
                for (int col = 0; col < 4; col++) {
                    if ((bits & (8 >>> col)) != 0) pixel(rgba, x + col, y + row, color);
                }
            }
        }
    }

    private static void rectangle(byte[] rgba, int x, int y, int width, int height, int color) {
        for (int row = y; row < y + height; row++)
            for (int col = x; col < x + width; col++) pixel(rgba, col, row, color);
    }

    private static void pixel(byte[] rgba, int x, int y, int color) {
        if (x < 0 || x >= WIDTH || y < 0 || y >= HEIGHT) return;
        int offset = (y * WIDTH + x) * 4;
        rgba[offset] = (byte) (color >> 16);
        rgba[offset + 1] = (byte) (color >> 8);
        rgba[offset + 2] = (byte) color;
        rgba[offset + 3] = (byte) 255;
    }
}
