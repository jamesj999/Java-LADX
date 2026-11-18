package linksawakening.ui;

import linksawakening.input.InputConfig;
import linksawakening.input.InputState;
import linksawakening.state.PlayerState;
import org.junit.jupiter.api.Test;

import java.io.InputStream;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.lwjgl.glfw.GLFW.GLFW_KEY_A;
import static org.lwjgl.glfw.GLFW.GLFW_KEY_B;
import static org.lwjgl.glfw.GLFW.GLFW_KEY_DOWN;
import static org.lwjgl.glfw.GLFW.GLFW_KEY_ENTER;
import static org.lwjgl.glfw.GLFW.GLFW_KEY_LEFT;
import static org.lwjgl.glfw.GLFW.GLFW_KEY_RIGHT;
import static org.lwjgl.glfw.GLFW.GLFW_KEY_UP;
import static org.lwjgl.glfw.GLFW.GLFW_PRESS;
import static org.lwjgl.glfw.GLFW.GLFW_RELEASE;

final class InventoryControllerTest {

    @Test
    void ownsMenuToggleTickAndCursorInput() throws Exception {
        InputState input = new InputState();
        InputConfig inputConfig = new InputConfig(GLFW_KEY_ENTER, GLFW_KEY_UP, GLFW_KEY_DOWN,
            GLFW_KEY_LEFT, GLFW_KEY_RIGHT, GLFW_KEY_A, GLFW_KEY_B);
        PlayerState playerState = new PlayerState();
        InventoryMenu menu = new InventoryMenu(InventoryTilemapLoader.loadFromRom(loadRom()), playerState);
        InventoryController controller = new InventoryController(input, inputConfig, menu);

        input.onKeyEvent(GLFW_KEY_ENTER, GLFW_PRESS);
        controller.dispatchToggleInput();
        for (int i = 0; i < 53; i++) {
            controller.tick();
        }

        assertTrue(controller.shouldBlockOverworldInput());
        assertTrue(menu.isFullyOpen());

        input.tickEdges();
        input.onKeyEvent(GLFW_KEY_DOWN, GLFW_PRESS);
        controller.dispatchMenuInput();

        assertEquals(2, menu.cursorSlot());
    }

    @Test
    void holdingSelectSlidesStatusInAndReleaseReturnsToInventory() throws Exception {
        InputState input = new InputState();
        InputConfig config = new InputConfig(GLFW_KEY_ENTER, GLFW_KEY_UP, GLFW_KEY_DOWN,
            GLFW_KEY_LEFT, GLFW_KEY_RIGHT, GLFW_KEY_A, GLFW_KEY_B);
        InventoryMenu menu = new InventoryMenu(InventoryTilemapLoader.loadFromRom(loadRom()), new PlayerState());
        InventoryController controller = new InventoryController(input, config, menu);
        menu.requestToggle();
        for (int i = 0; i < 53; i++) {
            controller.tick();
        }

        input.onKeyEvent(config.selectKey(), GLFW_PRESS);
        controller.dispatchMenuInput();
        input.tickEdges();
        controller.tick();
        assertEquals(140, menu.statusPanelY());

        for (int i = 0; i < 8; i++) {
            controller.tick();
        }
        assertEquals(120, menu.statusPanelY());
        input.onKeyEvent(config.downKey(), GLFW_PRESS);
        controller.dispatchMenuInput();
        assertEquals(0, menu.cursorSlot());

        input.onKeyEvent(config.selectKey(), GLFW_RELEASE);
        controller.tick();
        assertEquals(120, menu.statusPanelY());
        for (int i = 0; i < 6; i++) {
            controller.tick();
        }
        assertEquals(144, menu.statusPanelY());
        assertFalse(menu.isStatusPanelActive());
        assertTrue(menu.isFullyOpen());
    }

    private static byte[] loadRom() throws Exception {
        try (InputStream stream = InventoryControllerTest.class.getClassLoader().getResourceAsStream("rom/azle.gbc")) {
            return stream.readAllBytes();
        }
    }
}
