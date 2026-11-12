package linksawakening.dialog;

/** Retained two-row character positions for DialogDrawNextCharacterHandler's visible page. */
final class DialogTextLayout {
    private static final int LINE_WIDTH = 16;
    private static final int ROWS = 2;
    private final char[] characters = new char[LINE_WIDTH * ROWS];
    private final int[] columns = new int[LINE_WIDTH * ROWS];
    private final int[] rows = new int[LINE_WIDTH * ROWS];
    private String source;
    private int size;

    void update(String text) {
        if (source == text) return;
        source = text;
        size = 0;
        int column = 0, row = 0;
        for (int i = 0; i < text.length() && row < ROWS; i++) {
            char codepoint = text.charAt(i);
            if (codepoint == '\n') {
                column = 0;
                row++;
                continue;
            }
            if (column >= LINE_WIDTH) {
                column = 0;
                if (++row >= ROWS) break;
            }
            if (codepoint != ' ') {
                characters[size] = codepoint;
                columns[size] = column;
                rows[size++] = row;
            }
            column++;
        }
    }

    int size() {
        return size;
    }

    char character(int index) {
        return characters[index];
    }

    int column(int index) {
        return columns[index];
    }

    int row(int index) {
        return rows[index];
    }
}
