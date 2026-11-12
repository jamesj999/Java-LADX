package linksawakening.ui;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;
import linksawakening.gameplay.GameplaySoundEvent;
import linksawakening.gameplay.GameplaySoundSink;
import linksawakening.state.PlayerState;
import org.junit.jupiter.api.Test;

@linksawakening.testing.RequiresRom
final class InventoryMenuSoundTest {
    @Test
    void openingAndClosingInventoryPlayDisassemblyJingles() throws Exception {
        RecordingGameplaySoundSink soundSink = new RecordingGameplaySoundSink();
        InventoryMenu menu = new InventoryMenu(loadTilemap(), new PlayerState(), soundSink);

        menu.requestToggle();
        openMenu(menu);
        menu.requestToggle();

        assertEquals(List.of(GameplaySoundEvent.INVENTORY_OPEN, GameplaySoundEvent.INVENTORY_CLOSE), soundSink.events);
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

        assertEquals(List.of(GameplaySoundEvent.MENU_VALIDATE, GameplaySoundEvent.MENU_VALIDATE), soundSink.events);
    }

    @Test
    void menuEventsUseTheRomJingleNamespaceAndIds() throws Exception {
        var map = linksawakening.gameplay.GameplaySoundEffectMap.fromCatalog(
                linksawakening.audio.sfx.SoundEffectCatalog.fromRom(loadRom()));
        var events = List.of(
                GameplaySoundEvent.INVENTORY_OPEN,
                GameplaySoundEvent.INVENTORY_CLOSE,
                GameplaySoundEvent.MENU_MOVE,
                GameplaySoundEvent.MENU_VALIDATE);
        int[] ids = {0x11, 0x12, 0x0A, 0x13};
        for (int i = 0; i < ids.length; i++) {
            var effect = map.resolve(events.get(i)).orElseThrow();
            assertEquals(linksawakening.audio.sfx.SoundEffectNamespace.JINGLE, effect.namespace());
            assertEquals(ids[i], effect.id());
        }
    }

    @Test
    void clampingFromSlotEightToNinePlaysOneCursorSound() throws Exception {
        var sink = new RecordingGameplaySoundSink();
        var menu = new InventoryMenu(loadTilemap(), new PlayerState(), sink);
        openMenu(menu);
        for (int i = 0; i < 4; i++) menu.moveCursor(0, 1);
        sink.events.clear();
        menu.moveCursor(0, 1);
        assertEquals(9, menu.cursorSlot());
        assertEquals(List.of(GameplaySoundEvent.MENU_MOVE), sink.events);
    }

    @Test
    void simultaneousButtonsValidateOnlyTheASwap() throws Exception {
        var sink = new RecordingGameplaySoundSink();
        var menu = new InventoryMenu(loadTilemap(), new PlayerState(), sink);
        openMenu(menu);
        sink.events.clear();
        var input = new linksawakening.input.InputState();
        var config = new linksawakening.input.InputConfig(1, 2, 3, 4, 5, 6, 7);
        input.onKeyEvent(6, org.lwjgl.glfw.GLFW.GLFW_PRESS);
        input.onKeyEvent(7, org.lwjgl.glfw.GLFW.GLFW_PRESS);
        new InventoryController(input, config, menu).dispatchMenuInput();
        assertEquals(List.of(GameplaySoundEvent.MENU_VALIDATE), sink.events);
    }

    @Test
    void diagonalInputEmitsOnlyOneCursorJingle() throws Exception {
        var sink = new RecordingGameplaySoundSink();
        var menu = new InventoryMenu(loadTilemap(), new PlayerState(), sink);
        openMenu(menu);
        sink.events.clear();
        var input = new linksawakening.input.InputState();
        var config = new linksawakening.input.InputConfig(1, 2, 3, 4, 5, 6, 7);
        input.onKeyEvent(3, org.lwjgl.glfw.GLFW.GLFW_PRESS);
        input.onKeyEvent(5, org.lwjgl.glfw.GLFW.GLFW_PRESS);
        new InventoryController(input, config, menu).dispatchMenuInput();
        assertEquals(3, menu.cursorSlot());
        assertEquals(List.of(GameplaySoundEvent.MENU_MOVE), sink.events);
    }

    @Test
    void ocarinaAnimationSuppressesValidationSounds() throws Exception {
        var sink = new RecordingGameplaySoundSink();
        var player = new PlayerState();
        player.inventory().setSubscreenItems(new int[] {0, PlayerState.INVENTORY_OCARINA, 0, 0, 0, 0, 0, 0, 0, 0});
        player.progression().setOcarinaSongFlags(7);
        var menu = new InventoryMenu(loadTilemap(), player, sink);
        openMenu(menu);
        menu.moveCursor(1, 0);
        sink.events.clear();
        menu.pressA();
        menu.pressB();
        assertEquals(List.of(), sink.events);
    }

    private static byte[] loadRom() throws Exception {
        try (var stream = linksawakening.testing.TestRom.open()) {
            return stream.readAllBytes();
        }
    }

    private static void openMenu(InventoryMenu menu) {
        menu.requestToggle();
        for (int i = 0; i < 60; i++) {
            menu.tick();
        }
    }

    private static InventoryTilemapLoader loadTilemap() throws Exception {
        try (InputStream stream = linksawakening.testing.TestRom.open()) {
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
