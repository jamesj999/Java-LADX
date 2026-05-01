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

        menu.requestToggle();
        openMenu(menu);
        menu.requestToggle();

        assertEquals(List.of(
                GameplaySoundEvent.INVENTORY_OPEN,
                GameplaySoundEvent.INVENTORY_CLOSE), soundSink.events);
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
        for (int i = 0; i < 16; i++) {
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
