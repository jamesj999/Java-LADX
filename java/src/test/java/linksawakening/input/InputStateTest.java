package linksawakening.input;

import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;
import static org.lwjgl.glfw.GLFW.*;

final class InputStateTest {
    @Test
    void tapBetweenFramesRetainsBothEdgesUntilConsumed() {
        InputState state = new InputState();
        state.onKeyEvent(GLFW_KEY_Z, GLFW_PRESS);
        state.onKeyEvent(GLFW_KEY_Z, GLFW_RELEASE);
        assertFalse(state.isDown(GLFW_KEY_Z));
        assertTrue(state.wasPressed(GLFW_KEY_Z));
        assertTrue(state.wasReleased(GLFW_KEY_Z));
        state.tickEdges();
        assertFalse(state.wasPressed(GLFW_KEY_Z));
        assertFalse(state.wasReleased(GLFW_KEY_Z));
    }

    @Test
    void heldKeysAndRepeatsDoNotGenerateExtraEdges() {
        InputState state = new InputState();
        state.onKeyEvent(GLFW_KEY_Z, GLFW_PRESS);
        assertTrue(state.wasPressed(GLFW_KEY_Z));
        state.tickEdges();
        state.onKeyEvent(GLFW_KEY_Z, GLFW_REPEAT);
        state.onKeyEvent(GLFW_KEY_Z, GLFW_PRESS);
        assertTrue(state.isDown(GLFW_KEY_Z));
        assertFalse(state.wasPressed(GLFW_KEY_Z));
        state.onKeyEvent(GLFW_KEY_Z, GLFW_RELEASE);
        assertTrue(state.wasReleased(GLFW_KEY_Z));
        state.onKeyEvent(GLFW_KEY_Z, GLFW_PRESS);
        assertTrue(state.wasPressed(GLFW_KEY_Z));
    }
}
