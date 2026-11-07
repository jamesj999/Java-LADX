package linksawakening;

import linksawakening.gpu.Framebuffer;
import linksawakening.render.OpenGlFramePresenter;
import org.lwjgl.glfw.GLFWErrorCallback;
import org.lwjgl.glfw.GLFWKeyCallback;
import java.util.function.BiConsumer;
import static org.lwjgl.glfw.GLFW.*;

/** Owns native window, key callback and presentation resources. */
public final class DesktopHost implements Host {
    private long window;
    private OpenGlFramePresenter framePresenter;
    private GLFWErrorCallback errorCallback;
    private GLFWKeyCallback keyCallback;

    @Override public void open(BiConsumer<Integer, Integer> keys) {
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
            0
        );

        if (window == 0) {
            throw new IllegalStateException("Failed to create window");
        }

        glfwMakeContextCurrent(window);
        glfwSwapInterval(1);
        glfwShowWindow(window);

        try {
            Thread.sleep(200);
        } catch (InterruptedException ignored) {
            Thread.currentThread().interrupt();
        }

        keyCallback = new GLFWKeyCallback() {
            @Override
            public void invoke(long handle, int key, int scancode, int action, int mods) {
                keys.accept(key, action);
            }
        };
        glfwSetKeyCallback(window, keyCallback);
        framePresenter = OpenGlFramePresenter.initialize(window);
    }

    @Override public boolean shouldClose() { return glfwWindowShouldClose(window); }
    @Override public void pollEvents() { glfwPollEvents(); }
    @Override public void present(byte[] pixels) { framePresenter.present(pixels); }
    @Override public void releasePresentation() {
        if (framePresenter != null) {
            framePresenter.cleanup();
            framePresenter = null;
        }
    }
    @Override public void close() {
        releasePresentation();
        if (keyCallback != null) keyCallback.free();
        glfwDestroyWindow(window);
        glfwTerminate();
        if (errorCallback != null) errorCallback.free();
    }
}
