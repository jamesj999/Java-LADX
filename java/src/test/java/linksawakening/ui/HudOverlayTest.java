package linksawakening.ui;

import linksawakening.state.PlayerState;
import org.junit.jupiter.api.Test;

import java.util.Arrays;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;

final class HudOverlayTest {
    @Test
    void drawsEquippedLevelsAndQuantitiesAndClearsReplacedSlotDetails() throws Exception {
        InventoryTilemapLoader loader;
        try (var rom = getClass().getResourceAsStream("/rom/azle.gbc")) {
            loader = InventoryTilemapLoader.loadFromRom(rom.readAllBytes());
        }
        PlayerState player = new PlayerState();
        player.setItemB(PlayerState.INVENTORY_SHIELD);
        player.setItemA(PlayerState.INVENTORY_BOMBS);
        player.setShieldLevel(2);
        player.setMaxBombs(30);
        player.setBombCount(12);
        int[] tilemap = new int[InventoryTilemapLoader.WIDTH * InventoryTilemapLoader.HEIGHT];

        HudOverlay.apply(tilemap, new int[tilemap.length], player, loader);

        int rowStart = InventoryTilemapLoader.WIDTH;
        assertArrayEquals(new int[] {0x87, 0xBA, 0xB2},
            Arrays.copyOfRange(tilemap, rowStart + 1, rowStart + 4));
        assertArrayEquals(new int[] {0x81, 0xB1, 0xB2},
            Arrays.copyOfRange(tilemap, rowStart + 6, rowStart + 9));

        player.setItemB(PlayerState.INVENTORY_EMPTY);
        player.setBombCount(0);
        HudOverlay.apply(tilemap, new int[tilemap.length], player, loader);

        assertArrayEquals(new int[] {0x7F, 0x7F, 0x7F},
            Arrays.copyOfRange(tilemap, rowStart + 1, rowStart + 4));
        assertArrayEquals(new int[] {0x81, 0xB0, 0xB0},
            Arrays.copyOfRange(tilemap, rowStart + 6, rowStart + 9));
    }
}
