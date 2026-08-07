package linksawakening.ui;

import linksawakening.input.InputConfig;
import linksawakening.input.InputState;
import linksawakening.scene.BackgroundScene;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.lwjgl.glfw.GLFW.GLFW_KEY_A;
import static org.lwjgl.glfw.GLFW.GLFW_KEY_B;
import static org.lwjgl.glfw.GLFW.GLFW_KEY_DOWN;
import static org.lwjgl.glfw.GLFW.GLFW_KEY_ENTER;
import static org.lwjgl.glfw.GLFW.GLFW_KEY_UP;
import static org.lwjgl.glfw.GLFW.GLFW_PRESS;
import static org.lwjgl.glfw.GLFW.GLFW_RELEASE;

final class FileSaveControllerTest {

    private static final InputConfig INPUT = new InputConfig(
        GLFW_KEY_ENTER, GLFW_KEY_UP, GLFW_KEY_DOWN, 4, 5, GLFW_KEY_A, GLFW_KEY_B);

    @Test
    void startsOnReturnToGameWithTheRomSelectionArrow() {
        FileSaveController controller = newController();

        assertEquals(0, controller.snapshot().selectedSlot());
        assertEquals("save", controller.snapshot().sceneId());
        assertEquals(1, controller.snapshot().sprites().size());
        assertEquals(0xBE, controller.snapshot().sprites().get(0).tileIndex());
        assertEquals(0x1C, controller.snapshot().sprites().get(0).x());
        assertEquals(0x40, controller.snapshot().sprites().get(0).y());
    }

    @Test
    void upAndDownToggleTheTwoSaveOptions() {
        FileSaveController controller = newController();

        press(controller, GLFW_KEY_DOWN);
        assertEquals(1, controller.snapshot().selectedSlot());
        press(controller, GLFW_KEY_UP);
        assertEquals(0, controller.snapshot().selectedSlot());
        press(controller, GLFW_KEY_UP);
        assertEquals(1, controller.snapshot().selectedSlot());
    }

    @Test
    void actionButtonsReturnTheSelectedRomCommand() {
        FileSaveController controller = newController();

        assertEquals(FileSaveAction.Type.RETURN_TO_GAME,
            press(controller, GLFW_KEY_A).type());
        press(controller, GLFW_KEY_DOWN);
        assertEquals(FileSaveAction.Type.SAVE_AND_QUIT,
            press(controller, GLFW_KEY_B).type());
        assertFalse(controller.snapshot().creation());
        assertTrue(controller.snapshot().nameBytes().length == 5);
    }

    private static FileSaveController newController() {
        return new FileSaveController(sceneId -> new BackgroundScene(
            new int[32 * 32], new int[32 * 32],
            new int[][] {{0, 0, 0, 0}}, new int[][] {{0, 0, 0, 0}}));
    }

    private static FileSaveAction press(FileSaveController controller, int key) {
        InputState input = new InputState();
        input.onKeyEvent(key, GLFW_PRESS);
        FileSaveAction action = controller.tick(input, INPUT);
        input.tickEdges();
        input.onKeyEvent(key, GLFW_RELEASE);
        input.tickEdges();
        return action;
    }
}
