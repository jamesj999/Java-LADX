package linksawakening.input;

import static org.lwjgl.glfw.GLFW.GLFW_PRESS;
import static org.lwjgl.glfw.GLFW.GLFW_RELEASE;

/**
 * Polled held-key state, separate from GLFW's one-shot press/release events.
 * The GLFW key callback feeds {@link #onKeyEvent(int, int)}; game logic then
 * queries {@link #isDown(int)} each frame.
 *
 * <p>{@link #wasPressed(int)} / {@link #wasReleased(int)} expose edge
 * detection, mirroring the disassembly's {@code hPressedButtonsMask} /
 * {@code hReleasedButtonsMask}. Call {@link #tickEdges()} once per frame
 * after all game logic has consumed the edges to arm the next frame.
 */
public final class InputState {

    private static final int MAX_KEYS = 512;

    private final boolean[] down = new boolean[MAX_KEYS];
    private final boolean[] pressed = new boolean[MAX_KEYS];
    private final boolean[] released = new boolean[MAX_KEYS];

    public void onKeyEvent(int key, int action) {
        if (key < 0 || key >= MAX_KEYS) {
            return;
        }
        if (action == GLFW_PRESS && !down[key]) {
            down[key] = true;
            pressed[key] = true;
        } else if (action == GLFW_RELEASE && down[key]) {
            down[key] = false;
            released[key] = true;
        }
    }

    public boolean isDown(int key) {
        if (key < 0 || key >= MAX_KEYS) {
            return false;
        }
        return down[key];
    }

    public boolean wasPressed(int key) {
        if (key < 0 || key >= MAX_KEYS) {
            return false;
        }
        return pressed[key];
    }

    public boolean wasReleased(int key) {
        if (key < 0 || key >= MAX_KEYS) {
            return false;
        }
        return released[key];
    }

    /** Clear consumed edges while retaining held state for the next frame. */
    public void tickEdges() {
        java.util.Arrays.fill(pressed, false);
        java.util.Arrays.fill(released, false);
    }
}
