package linksawakening.app.debug;

import static org.junit.jupiter.api.Assertions.*;
import static org.lwjgl.glfw.GLFW.*;

import linksawakening.input.InputState;
import linksawakening.world.room.DungeonItemState;
import org.junit.jupiter.api.Test;

class DebugMenuTest {
    @Test
    void destinationUsesSpriteOriginAndRejectsInvalidColorRooms() {
        var warp = DebugMenu.destination(2, 0x0A, 0xFE, 144, 112);
        assertEquals(144, warp.javaPixelX());
        assertEquals(112, warp.javaPixelY());
        assertEquals(2, warp.category());
        assertEquals(0, DebugMenu.destination(0, 255, 255, 0, 0).destMap());
        assertThrows(IllegalArgumentException.class, () -> DebugMenu.destination(1, 255, 0x16, 0, 0));
        assertThrows(IllegalArgumentException.class, () -> DebugMenu.destination(1, 0x20, 0, 0, 0));
        assertThrows(IllegalArgumentException.class, () -> DebugMenu.destination(0, 0, 256, 0, 0));
        assertThrows(IllegalArgumentException.class, () -> DebugMenu.destination(0, 0, 0, -1, 0));
    }

    @Test
    void dungeonEditsPersistAndRefreshOnlyTheSelectedActiveMap() {
        var state = new DungeonItemState();
        state.loadForMap(2, true);
        state.setFlagForDebug(2, DungeonItemState.SMALL_KEYS_INDEX, 9);
        assertEquals(9, state.currentFlag(DungeonItemState.SMALL_KEYS_INDEX));
        assertTrue(state.consumeSmallKey());
        state.setFlagForDebug(255, DungeonItemState.MAP_INDEX, 1);
        assertEquals(0, state.currentFlag(DungeonItemState.MAP_INDEX));
        state.loadForMap(255, true);
        assertEquals(1, state.currentFlag(DungeonItemState.MAP_INDEX));
        state.loadForMap(2, true);
        assertEquals(8, state.currentFlag(DungeonItemState.SMALL_KEYS_INDEX));
    }

    @Test
    void modalInputClearDiscardsHeldKeysAndBothEdges() {
        var input = new InputState();
        input.onKeyEvent(GLFW_KEY_RIGHT, GLFW_PRESS);
        input.onKeyEvent(GLFW_KEY_Z, GLFW_PRESS);
        input.onKeyEvent(GLFW_KEY_Z, GLFW_RELEASE);
        input.clear();
        assertFalse(input.isDown(GLFW_KEY_RIGHT));
        assertFalse(input.wasPressed(GLFW_KEY_RIGHT));
        assertFalse(input.wasReleased(GLFW_KEY_Z));
    }
}
