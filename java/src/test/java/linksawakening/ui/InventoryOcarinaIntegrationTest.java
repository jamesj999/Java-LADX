package linksawakening.ui;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.InputStream;
import linksawakening.rom.Rom;
import linksawakening.state.InventoryItem;
import linksawakening.state.OcarinaSong;
import linksawakening.state.PlayerState;
import org.junit.jupiter.api.Test;

@linksawakening.testing.RequiresRom
final class InventoryOcarinaIntegrationTest {

    @Test
    void selectingOcarinaOpensPopupAndHorizontalInputChangesSong() throws Exception {
        PlayerState player = new PlayerState();
        player.inventory().setSubscreenItems(new int[] {
            InventoryItem.BOMBS.romValue(),
            InventoryItem.OCARINA.romValue(),
            InventoryItem.EMPTY.romValue(),
            InventoryItem.EMPTY.romValue(),
            InventoryItem.EMPTY.romValue(),
            InventoryItem.EMPTY.romValue(),
            InventoryItem.EMPTY.romValue(),
            InventoryItem.EMPTY.romValue(),
            InventoryItem.EMPTY.romValue(),
            InventoryItem.EMPTY.romValue()
        });
        player.progression()
                .setOcarinaSongFlags(
                        OcarinaSong.BALLAD_OF_THE_WIND_FISH.romValue() | OcarinaSong.FROGS_SONG_OF_THE_SOUL.romValue());
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
            InventoryItem.EMPTY.romValue(),
            InventoryItem.OCARINA.romValue(),
            InventoryItem.EMPTY.romValue(),
            InventoryItem.EMPTY.romValue(),
            InventoryItem.EMPTY.romValue(),
            InventoryItem.EMPTY.romValue(),
            InventoryItem.EMPTY.romValue(),
            InventoryItem.EMPTY.romValue(),
            InventoryItem.EMPTY.romValue(),
            InventoryItem.EMPTY.romValue()
        });
        player.progression().setOcarinaSongFlags(OcarinaSong.MANBO_MAMBO.romValue());
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
            return InventoryTilemapLoader.loadFromRom(Rom.of(stream.readAllBytes()));
        }
    }
}
