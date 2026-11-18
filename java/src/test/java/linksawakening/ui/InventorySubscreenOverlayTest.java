package linksawakening.ui;

import linksawakening.state.PlayerState;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

final class InventorySubscreenOverlayTest {

    @Test
    void dynamicInventoryItemsApplyRomPaletteIndexes() throws Exception {
        int[] tilemap = new int[InventoryTilemapLoader.WIDTH * InventoryTilemapLoader.HEIGHT];
        int[] attrmap = new int[tilemap.length];
        InventoryTilemapLoader loader;
        try (var rom = getClass().getResourceAsStream("/rom/azle.gbc")) {
            loader = InventoryTilemapLoader.loadFromRom(rom.readAllBytes());
        }
        PlayerState player = new PlayerState();

        InventorySubscreenOverlay.apply(tilemap, attrmap, player, 0, false, loader);

        assertEquals(0x88, tilemap[index(5, 6)]);
        assertEquals(0x89, tilemap[index(5, 7)]);
        assertEquals(0x03, attrmap[index(5, 6)]);
        assertEquals(0x03, attrmap[index(5, 7)]);

        assertEquals(0x8A, tilemap[index(1, 9)]);
        assertEquals(0x8B, tilemap[index(1, 10)]);
        assertEquals(0x01, attrmap[index(1, 9)]);
        assertEquals(0x02, attrmap[index(1, 10)]);
    }

    private static int index(int col, int row) {
        return row * InventoryTilemapLoader.WIDTH + col;
    }
}
