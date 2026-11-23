package linksawakening.app.host;

import static org.lwjgl.glfw.GLFW.GLFW_COCOA_RETINA_FRAMEBUFFER;
import static org.lwjgl.glfw.GLFW.GLFW_CONTEXT_VERSION_MAJOR;
import static org.lwjgl.glfw.GLFW.GLFW_CONTEXT_VERSION_MINOR;
import static org.lwjgl.glfw.GLFW.GLFW_FALSE;
import static org.lwjgl.glfw.GLFW.GLFW_OPENGL_CORE_PROFILE;
import static org.lwjgl.glfw.GLFW.GLFW_OPENGL_FORWARD_COMPAT;
import static org.lwjgl.glfw.GLFW.GLFW_OPENGL_PROFILE;
import static org.lwjgl.glfw.GLFW.GLFW_TRUE;
import static org.lwjgl.glfw.GLFW.GLFW_VISIBLE;
import static org.lwjgl.glfw.GLFW.glfwCreateWindow;
import static org.lwjgl.glfw.GLFW.glfwDefaultWindowHints;
import static org.lwjgl.glfw.GLFW.glfwDestroyWindow;
import static org.lwjgl.glfw.GLFW.glfwGetFramebufferSize;
import static org.lwjgl.glfw.GLFW.glfwGetWindowSize;
import static org.lwjgl.glfw.GLFW.glfwInit;
import static org.lwjgl.glfw.GLFW.glfwMakeContextCurrent;
import static org.lwjgl.glfw.GLFW.glfwSetErrorCallback;
import static org.lwjgl.glfw.GLFW.glfwSetWindowSize;
import static org.lwjgl.glfw.GLFW.glfwTerminate;
import static org.lwjgl.glfw.GLFW.glfwWindowHint;
import static org.lwjgl.opengl.GL11.GL_VIEWPORT;
import static org.lwjgl.opengl.GL11.glGetIntegerv;

import java.nio.ByteBuffer;
import java.util.Arrays;
import linksawakening.gpu.Framebuffer;
import org.lwjgl.BufferUtils;
import org.lwjgl.opengl.GL11;
import org.lwjgl.system.MemoryStack;

/** Native regression check; launch on the main OS thread (-XstartOnFirstThread on macOS). */
public final class DesktopHostViewportCheck {
    public static void main(String[] args) throws Exception {
        DesktopHost host = new DesktopHost();
        host.open((key, action) -> {});
        try {
            long window = host.windowHandle();
            byte[] pixels = new byte[Framebuffer.WIDTH * Framebuffer.HEIGHT * 4];
            assertViewport(window, "initialized");
            host.present(pixels);
            assertViewport(window, "first frame");
            // Programmatic resizing also works for a non-user-resizable window.
            glfwSetWindowSize(window, 480, 432);
            host.pollEvents();
            host.present(pixels);
            assertViewport(window, "resized framebuffer");
            glfwSetWindowSize(window, Framebuffer.WIDTH * Framebuffer.SCALE, Framebuffer.HEIGHT * Framebuffer.SCALE);
            host.pollEvents();
            host.present(pixels);
            assertViewport(window, "restored framebuffer");
        } finally {
            host.close();
        }
        checkUnscaledFramebuffer();
    }

    private static void checkUnscaledFramebuffer() {
        glfwSetErrorCallback(null);
        if (!glfwInit()) throw new AssertionError("GLFW initialization failed");
        glfwDefaultWindowHints();
        glfwWindowHint(GLFW_VISIBLE, GLFW_FALSE);
        glfwWindowHint(GLFW_CONTEXT_VERSION_MAJOR, 3);
        glfwWindowHint(GLFW_CONTEXT_VERSION_MINOR, 3);
        glfwWindowHint(GLFW_OPENGL_PROFILE, GLFW_OPENGL_CORE_PROFILE);
        glfwWindowHint(GLFW_OPENGL_FORWARD_COMPAT, GLFW_TRUE);
        // Exercise a 1x backing framebuffer even on a Retina monitor.
        if (System.getProperty("os.name").toLowerCase(java.util.Locale.ROOT).contains("mac")) {
            glfwWindowHint(GLFW_COCOA_RETINA_FRAMEBUFFER, GLFW_FALSE);
        }
        long window = glfwCreateWindow(640, 576, "Unscaled viewport check", 0, 0);
        if (window == 0) throw new AssertionError("Unscaled window creation failed");
        try {
            glfwMakeContextCurrent(window);
            var presenter = linksawakening.render.OpenGlFramePresenter.initialize(window);
            try {
                byte[] frame = new byte[Framebuffer.WIDTH * Framebuffer.HEIGHT * 4];
                ByteBuffer uploaded = BufferUtils.createByteBuffer(frame.length);
                for (byte color : new byte[] {0x31, 0x62}) {
                    Arrays.fill(frame, color);
                    presenter.present(frame);
                    GL11.glGetTexImage(GL11.GL_TEXTURE_2D, 0, GL11.GL_RGBA, GL11.GL_UNSIGNED_BYTE, uploaded);
                    if (!uploaded.equals(ByteBuffer.wrap(frame))) throw new AssertionError("Texture upload mismatch");
                }
                assertViewport(window, "unscaled first frame");
            } finally {
                presenter.cleanup();
            }
        } finally {
            glfwDestroyWindow(window);
            glfwTerminate();
        }
    }

    private static void assertViewport(long window, String checkpoint) {
        try (MemoryStack stack = MemoryStack.stackPush()) {
            var width = stack.mallocInt(1);
            var height = stack.mallocInt(1);
            var logicalWidth = stack.mallocInt(1);
            var logicalHeight = stack.mallocInt(1);
            var viewport = stack.mallocInt(4);
            glfwGetFramebufferSize(window, width, height);
            glfwGetWindowSize(window, logicalWidth, logicalHeight);
            glGetIntegerv(GL_VIEWPORT, viewport);
            if (width.get(0) <= 0
                    || height.get(0) <= 0
                    || viewport.get(0) != 0
                    || viewport.get(1) != 0
                    || viewport.get(2) != width.get(0)
                    || viewport.get(3) != height.get(0)) {
                throw new AssertionError(checkpoint + ": viewport must cover the drawable framebuffer");
            }
            System.out.printf(
                    "%s: window %dx%d, framebuffer/viewport %dx%d%n",
                    checkpoint, logicalWidth.get(0), logicalHeight.get(0), width.get(0), height.get(0));
        }
    }
}
