package linksawakening.cutscene;

import java.util.Arrays;

public final class TitleReveal {

    public static final int ROW_COUNT = 7;

    private static final int[] REVEAL_ROWS = { 5, 4, 6, 3, 7, 2, 8 };
    private static final int LOGO_COL_START = 2;
    private static final int LOGO_COL_END = 18;
    private static final int MASK_TILE = 0x7E;

    private TitleReveal() {
    }

    public static int[] maskedTilemap(int[] tilemap, int revealRows, int width) {
        if (revealRows >= ROW_COUNT) {
            return tilemap;
        }

        int[] masked = Arrays.copyOf(tilemap, tilemap.length);
        boolean[] visibleRows = new boolean[9];
        int rowLimit = Math.max(0, Math.min(revealRows, REVEAL_ROWS.length));
        for (int i = 0; i < rowLimit; i++) {
            visibleRows[REVEAL_ROWS[i]] = true;
        }

        for (int row : REVEAL_ROWS) {
            if (visibleRows[row]) {
                continue;
            }
            int base = row * width;
            for (int col = LOGO_COL_START; col < LOGO_COL_END; col++) {
                masked[base + col] = MASK_TILE;
            }
        }
        return masked;
    }
}
