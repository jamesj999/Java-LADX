package linksawakening.ui;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.InputStream;
import linksawakening.state.PlayerState;
import org.junit.jupiter.api.Test;

@linksawakening.testing.RequiresRom
final class InventoryOcarinaIntegrationTest {

    @Test
    void selectingOcarinaOpensPopupAndHorizontalInputChangesSong() throws Exception {
        PlayerState player = new PlayerState();
        player.inventory().setSubscreenItems(new int[] {
            PlayerState.INVENTORY_BOMBS,
            PlayerState.INVENTORY_OCARINA,
            PlayerState.INVENTORY_EMPTY,
            PlayerState.INVENTORY_EMPTY,
            PlayerState.INVENTORY_EMPTY,
            PlayerState.INVENTORY_EMPTY,
            PlayerState.INVENTORY_EMPTY,
            PlayerState.INVENTORY_EMPTY,
            PlayerState.INVENTORY_EMPTY,
            PlayerState.INVENTORY_EMPTY
        });
        player.progression()
                .setOcarinaSongFlags(
                        PlayerState.BALLAD_OF_THE_WIND_FISH_FLAG | PlayerState.FROGS_SONG_OF_THE_SOUL_FLAG);
        player.progression().setSelectedSongIndex(0);
        InventoryMenu menu = new InventoryMenu(loadTilemap(), player);
        openInventory(menu);

        menu.moveCursor(+1, 0);

        assertEquals(1, menu.cursorSlot());
        assertTrue(menu.isOcarinaMenuVisible());
        finishOcarinaOpening(menu);

        menu.moveCursor(+1, 0);

        assertEquals(2, player.progression().selectedSongIndex());
        assertEquals(1, menu.cursorSlot());
    }

    @Test
    void menuButtonClosesOcarinaBeforeClosingInventory() throws Exception {
        PlayerState player = new PlayerState();
        player.inventory().setSubscreenItems(new int[] {
            PlayerState.INVENTORY_EMPTY,
            PlayerState.INVENTORY_OCARINA,
            PlayerState.INVENTORY_EMPTY,
            PlayerState.INVENTORY_EMPTY,
            PlayerState.INVENTORY_EMPTY,
            PlayerState.INVENTORY_EMPTY,
            PlayerState.INVENTORY_EMPTY,
            PlayerState.INVENTORY_EMPTY,
            PlayerState.INVENTORY_EMPTY,
            PlayerState.INVENTORY_EMPTY
        });
        player.progression().setOcarinaSongFlags(PlayerState.MANBO_MAMBO_FLAG);
        InventoryMenu menu = new InventoryMenu(loadTilemap(), player);
        openInventory(menu);
        menu.moveCursor(+1, 0);
        finishOcarinaOpening(menu);

        menu.requestToggle();
        assertTrue(menu.isOcarinaMenuVisible());
        for (int frame = 0; frame < 16; frame++) {
            menu.tick();
        }

        assertFalse(menu.isOcarinaMenuVisible());
        assertFalse(menu.isFullyOpen());
        for (int frame = 0; frame < 60; frame++) {
            menu.tick();
        }
        assertTrue(menu.isFullyClosed());
    }

    private static void finishOcarinaOpening(InventoryMenu menu) {
        for (int frame = 0; frame < 16; frame++) {
            menu.tick();
        }
        assertTrue(menu.isOcarinaMenuReady());
    }

    private static void openInventory(InventoryMenu menu) {
        menu.requestToggle();
        for (int frame = 0; frame < 60; frame++) {
            menu.tick();
        }
        assertTrue(menu.isFullyOpen());
    }

    private static InventoryTilemapLoader loadTilemap() throws Exception {
        try (InputStream stream = linksawakening.testing.TestRom.open()) {
            return InventoryTilemapLoader.loadFromRom(stream.readAllBytes());
        }
    }
}
