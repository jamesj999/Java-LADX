package linksawakening.app.host;

import static org.lwjgl.glfw.GLFW.*;

import java.util.function.BiConsumer;
import linksawakening.gpu.Framebuffer;
import linksawakening.input.KeyCodes;
import linksawakening.render.OpenGlFramePresenter;
import org.lwjgl.glfw.GLFWErrorCallback;
import org.lwjgl.glfw.GLFWKeyCallback;
import org.lwjgl.system.MemoryStack;

/** Owns native window, key callback and presentation resources. */
public final class DesktopHost implements Host {
    @Override
    public linksawakening.gameplay.PcmSoundOutput createPcmOutput() {
        return new linksawakening.audio.openal.OpenAlPcmSoundOutput();
    }

    private long window;
    private OpenGlFramePresenter framePresenter;
    private GLFWErrorCallback errorCallback;
    private GLFWKeyCallback keyCallback;

    @Override
    public void open(BiConsumer<Integer, Integer> keys) {
        errorCallback = GLFWErrorCallback.createPrint(System.err).set();

        if (!glfwInit()) {
            throw new IllegalStateException("Failed to initialize GLFW");
        }

        glfwDefaultWindowHints();
        glfwWindowHint(GLFW_VISIBLE, GLFW_FALSE);
        glfwWindowHint(GLFW_RESIZABLE, GLFW_FALSE);
        glfwWindowHint(GLFW_CONTEXT_VERSION_MAJOR, 3);
        glfwWindowHint(GLFW_CONTEXT_VERSION_MINOR, 3);
        glfwWindowHint(GLFW_OPENGL_PROFILE, GLFW_OPENGL_CORE_PROFILE);
        glfwWindowHint(GLFW_OPENGL_FORWARD_COMPAT, GLFW_TRUE);

        window = glfwCreateWindow(
                Framebuffer.WIDTH * Framebuffer.SCALE,
                Framebuffer.HEIGHT * Framebuffer.SCALE,
                "Link's Awakening DX",
                0,
                0);

        if (window == 0) {
            throw new IllegalStateException("Failed to create window");
        }

        centerWindowOnPrimaryMonitor();
        glfwMakeContextCurrent(window);
        glfwSwapInterval(1);
        framePresenter = OpenGlFramePresenter.initialize(window);

        keyCallback = new GLFWKeyCallback() {
            @Override
            public void invoke(long handle, int key, int scancode, int action, int mods) {
                // Key codes intentionally share the engine's stable numeric namespace.
                int engineAction =
                        switch (action) {
                            case GLFW_PRESS -> KeyCodes.PRESS;
                            case GLFW_RELEASE -> KeyCodes.RELEASE;
                            case GLFW_REPEAT -> KeyCodes.REPEAT;
                            default -> action;
                        };
                keys.accept(key, engineAction);
            }
        };
        glfwSetKeyCallback(window, keyCallback);
        glfwShowWindow(window);
        glfwPollEvents();
    }

    private void centerWindowOnPrimaryMonitor() {
        long monitor = glfwGetPrimaryMonitor();
        if (monitor == 0) return;
        try (MemoryStack stack = MemoryStack.stackPush()) {
            var x = stack.mallocInt(1);
            var y = stack.mallocInt(1);
            var width = stack.mallocInt(1);
            var height = stack.mallocInt(1);
            var windowWidth = stack.mallocInt(1);
            var windowHeight = stack.mallocInt(1);
            // Positions and work areas use screen coordinates, unlike the GL
            // viewport, which must use framebuffer pixels on HiDPI displays.
            glfwGetMonitorWorkarea(monitor, x, y, width, height);
            glfwGetWindowSize(window, windowWidth, windowHeight);
            glfwSetWindowPos(
                    window,
                    x.get(0) + (width.get(0) - windowWidth.get(0)) / 2,
                    y.get(0) + (height.get(0) - windowHeight.get(0)) / 2);
        }
    }

    @Override
    public boolean shouldClose() {
        return glfwWindowShouldClose(window);
    }

    @Override
    public void pollEvents() {
        glfwPollEvents();
    }

    @Override
    public void present(byte[] pixels) {
        framePresenter.present(pixels);
    }

    @Override
    public void releasePresentation() {
        if (framePresenter != null) {
            framePresenter.cleanup();
            framePresenter = null;
        }
    }

    @Override
    public void close() {
        releasePresentation();
        if (keyCallback != null) keyCallback.free();
        glfwDestroyWindow(window);
        glfwTerminate();
        if (errorCallback != null) {
            glfwSetErrorCallback(null);
            errorCallback.free();
            errorCallback = null;
        }
    }
}
