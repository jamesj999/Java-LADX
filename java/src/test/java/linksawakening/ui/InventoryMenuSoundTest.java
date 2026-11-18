package linksawakening.ui;

import linksawakening.gameplay.GameplaySoundEvent;
import linksawakening.gameplay.GameplaySoundSink;
import linksawakening.state.PlayerState;
import org.junit.jupiter.api.Test;

import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;

final class InventoryMenuSoundTest {
    @Test
    void openingAndClosingInventoryPlayDisassemblyJingles() throws Exception {
        RecordingGameplaySoundSink soundSink = new RecordingGameplaySoundSink();
        InventoryMenu menu = new InventoryMenu(loadTilemap(), new PlayerState(), soundSink);

        openMenu(menu);
        menu.requestToggle();

        assertEquals(List.of(
                GameplaySoundEvent.INVENTORY_OPEN,
                GameplaySoundEvent.INVENTORY_CLOSE), soundSink.events);
    }

    @Test
    void dxInventoryChangesGainAndWindowDuringTheFade() throws Exception {
        InventoryMenu menu = new InventoryMenu(loadTilemap(), new PlayerState(), event -> { });
        menu.requestToggle();
        for (int frame = 0; frame <= 52; frame++) {
            menu.tick();
            assertEquals(frame >= 25, menu.audioAttenuated());
            assertEquals(frame >= 36 ? 0 : 128, menu.windowY());
            assertEquals(frame == 52, menu.isFullyOpen());
        }

        menu.requestToggle();
        for (int frame = 0; frame <= 51; frame++) {
            menu.tick();
            assertEquals(frame < 19, menu.audioAttenuated());
            assertEquals(frame < 17 ? 0 : 128, menu.windowY());
            assertEquals(frame == 51, menu.isFullyClosed());
        }
    }

    @Test
    void acceptedCursorMovePlaysMoveSelectionJingle() throws Exception {
        RecordingGameplaySoundSink soundSink = new RecordingGameplaySoundSink();
        InventoryMenu menu = new InventoryMenu(loadTilemap(), new PlayerState(), soundSink);
        openMenu(menu);
        soundSink.events.clear();

        menu.moveCursor(-1, 0);
        assertEquals(List.of(), soundSink.events);

        menu.moveCursor(0, 1);

        assertEquals(2, menu.cursorSlot());
        assertEquals(List.of(GameplaySoundEvent.MENU_MOVE), soundSink.events);
    }

    @Test
    void selectingAnInventorySlotPlaysValidateJingle() throws Exception {
        RecordingGameplaySoundSink soundSink = new RecordingGameplaySoundSink();
        InventoryMenu menu = new InventoryMenu(loadTilemap(), new PlayerState(), soundSink);
        openMenu(menu);
        soundSink.events.clear();

        menu.pressA();
        menu.pressB();

        assertEquals(List.of(
                GameplaySoundEvent.MENU_VALIDATE,
                GameplaySoundEvent.MENU_VALIDATE), soundSink.events);
    }

    private static void openMenu(InventoryMenu menu) {
        menu.requestToggle();
        for (int i = 0; i < 53; i++) {
            menu.tick();
        }
    }

    private static InventoryTilemapLoader loadTilemap() throws Exception {
        try (InputStream stream = InventoryMenuSoundTest.class.getClassLoader()
                .getResourceAsStream("rom/azle.gbc")) {
            return InventoryTilemapLoader.loadFromRom(stream.readAllBytes());
        }
    }

    private static final class RecordingGameplaySoundSink implements GameplaySoundSink {
        private final List<GameplaySoundEvent> events = new ArrayList<>();

        @Override
        public void play(GameplaySoundEvent event) {
            events.add(event);
        }
    }
}
