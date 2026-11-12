package linksawakening.app.host;

import static org.lwjgl.glfw.GLFW.*;

import java.util.function.BiConsumer;
import linksawakening.audio.openal.OpenAlPcmSoundOutput;
import linksawakening.gameplay.PcmSoundOutput;
import linksawakening.gpu.Framebuffer;
import linksawakening.input.KeyCodes;
import linksawakening.render.OpenGlFramePresenter;
import org.lwjgl.glfw.GLFWErrorCallback;
import org.lwjgl.glfw.GLFWKeyCallback;
import org.lwjgl.system.MemoryStack;

/**
 * Owns native window, key callback and presentation resources. One instance is retained for the owning
 * session.
 */
public final class DesktopHost implements Host {
    @Override
    public PcmSoundOutput createPcmOutput() {
        return new OpenAlPcmSoundOutput();
    }

    private long window;
    private HostResources resources;

    /** Native handle for the package-local viewport check. */
    long windowHandle() {
        return window;
    }

    private OpenGlFramePresenter framePresenter;
    private GLFWErrorCallback errorCallback;
    private GLFWKeyCallback keyCallback;

    @Override
    public void open(BiConsumer<Integer, Integer> keys) {
        if (resources != null) throw new IllegalStateException("Desktop host is already open");
        resources = new HostResources();
        try {
            resources.initialize(() -> openWindow(keys));
        } catch (RuntimeException | Error failure) {
            resources = null;
            throw failure;
        }
    }

    private void openWindow(BiConsumer<Integer, Integer> keys) {
        errorCallback = GLFWErrorCallback.createPrint(System.err);
        resources.add(() -> {
            var callback = errorCallback;
            errorCallback = null;
            try {
                glfwSetErrorCallback(null);
            } finally {
                callback.free();
            }
        });
        errorCallback.set();
        resources.add(() -> glfwTerminate());

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

        resources.add(() -> {
            long handle = window;
            window = 0;
            glfwDestroyWindow(handle);
        });
        centerWindowOnPrimaryMonitor();
        glfwMakeContextCurrent(window);
        // FramePacer owns the rational 60 Hz simulation cadence, independent of display refresh.
        glfwSwapInterval(0);
        framePresenter = OpenGlFramePresenter.initialize(window);
        resources.add(this::releasePresentation);

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
        resources.add(() -> {
            var callback = keyCallback;
            keyCallback = null;
            callback.free();
        });
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
            var presenter = framePresenter;
            framePresenter = null;
            presenter.cleanup();
        }
    }

    @Override
    public void close() {
        var owned = resources;
        resources = null;
        if (owned != null) owned.close();
    }
}
