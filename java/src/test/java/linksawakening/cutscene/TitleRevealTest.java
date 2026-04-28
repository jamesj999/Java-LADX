package linksawakening.cutscene;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

final class TitleRevealTest {

    @Test
    void masksLogoRowsInTheDisassemblyRevealOrder() {
        int[] tilemap = syntheticTitleTilemap();

        int[] oneRow = TitleReveal.maskedTilemap(tilemap, 1, 32);
        assertLogoRowVisible(oneRow, 5);
        assertLogoRowMasked(oneRow, 4);
        assertLogoRowMasked(oneRow, 6);
        assertLogoRowMasked(oneRow, 2);

        int[] threeRows = TitleReveal.maskedTilemap(tilemap, 3, 32);
        assertLogoRowVisible(threeRows, 5);
        assertLogoRowVisible(threeRows, 4);
        assertLogoRowVisible(threeRows, 6);
        assertLogoRowMasked(threeRows, 3);

        int[] allRows = TitleReveal.maskedTilemap(tilemap, 7, 32);
        for (int row = 2; row <= 8; row++) {
            assertLogoRowVisible(allRows, row);
        }
    }

    private static int[] syntheticTitleTilemap() {
        int[] tilemap = new int[32 * 32];
        for (int row = 2; row <= 8; row++) {
            int base = row * 32;
            for (int col = 2; col < 18; col++) {
                tilemap[base + col] = row;
            }
        }
        return tilemap;
    }

    private static void assertLogoRowVisible(int[] tilemap, int row) {
        for (int col = 2; col < 18; col++) {
            assertEquals(row, tilemap[row * 32 + col]);
        }
    }

    private static void assertLogoRowMasked(int[] tilemap, int row) {
        for (int col = 2; col < 18; col++) {
            assertEquals(0x7E, tilemap[row * 32 + col]);
        }
    }
}
