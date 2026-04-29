package linksawakening.render;

import linksawakening.gpu.Framebuffer;
import org.lwjgl.BufferUtils;
import org.lwjgl.opengl.GL;
import org.lwjgl.opengl.GL11;
import org.lwjgl.opengl.GL12;
import org.lwjgl.opengl.GL15;
import org.lwjgl.opengl.GL20;
import org.lwjgl.opengl.GL30;

import java.nio.ByteBuffer;
import java.nio.FloatBuffer;

import static org.lwjgl.glfw.GLFW.glfwMakeContextCurrent;
import static org.lwjgl.glfw.GLFW.glfwSwapBuffers;

public final class OpenGlFramePresenter {
    private final long window;
    private final int vao;
    private final int vbo;
    private final Shader shader;
    private final int texture;

    private OpenGlFramePresenter(long window, int vao, int vbo, Shader shader, int texture) {
        this.window = window;
        this.vao = vao;
        this.vbo = vbo;
        this.shader = shader;
        this.texture = texture;
    }

    public static OpenGlFramePresenter initialize(long window) {
        GL.createCapabilities();
        GL11.glViewport(0, 0, Framebuffer.WIDTH * Framebuffer.SCALE, Framebuffer.HEIGHT * Framebuffer.SCALE);

        Shader shader = new Shader();

        float[] vertices = {
            -1.0f, -1.0f, 0.0f, 1.0f,
             1.0f, -1.0f, 1.0f, 1.0f,
             1.0f,  1.0f, 1.0f, 0.0f,
            -1.0f,  1.0f, 0.0f, 0.0f
        };

        FloatBuffer vertexBuffer = BufferUtils.createFloatBuffer(vertices.length);
        vertexBuffer.put(vertices).flip();

        int vao = GL30.glGenVertexArrays();
        GL30.glBindVertexArray(vao);

        int vbo = GL15.glGenBuffers();
        GL15.glBindBuffer(GL15.GL_ARRAY_BUFFER, vbo);
        GL15.glBufferData(GL15.GL_ARRAY_BUFFER, vertexBuffer, GL15.GL_STATIC_DRAW);

        GL20.glEnableVertexAttribArray(0);
        GL20.glVertexAttribPointer(0, 2, GL11.GL_FLOAT, false, 16, 0);
        GL20.glEnableVertexAttribArray(1);
        GL20.glVertexAttribPointer(1, 2, GL11.GL_FLOAT, false, 16, 8);

        GL30.glBindVertexArray(0);

        int texture = GL11.glGenTextures();
        GL11.glBindTexture(GL11.GL_TEXTURE_2D, texture);
        GL11.glTexParameteri(GL11.GL_TEXTURE_2D, GL11.GL_TEXTURE_MIN_FILTER, GL11.GL_NEAREST);
        GL11.glTexParameteri(GL11.GL_TEXTURE_2D, GL11.GL_TEXTURE_MAG_FILTER, GL11.GL_NEAREST);
        GL11.glTexParameteri(GL11.GL_TEXTURE_2D, GL11.GL_TEXTURE_WRAP_S, GL12.GL_CLAMP_TO_EDGE);
        GL11.glTexParameteri(GL11.GL_TEXTURE_2D, GL11.GL_TEXTURE_WRAP_T, GL12.GL_CLAMP_TO_EDGE);

        return new OpenGlFramePresenter(window, vao, vbo, shader, texture);
    }

    public void present(byte[] indexedDisplayBuffer) {
        uploadTexture(indexedDisplayBuffer);

        glfwMakeContextCurrent(window);
        GL11.glClearColor(0.0f, 0.0f, 0.0f, 1.0f);
        GL11.glClear(GL11.GL_COLOR_BUFFER_BIT);

        GL11.glBindTexture(GL11.GL_TEXTURE_2D, texture);
        shader.use();
        shader.setUniform1i("uTexture", 0);

        GL30.glBindVertexArray(vao);
        GL11.glDrawArrays(GL11.GL_TRIANGLE_FAN, 0, 4);
        GL30.glBindVertexArray(0);

        glfwSwapBuffers(window);
    }

    public void cleanup() {
        GL30.glDeleteVertexArrays(vao);
        GL15.glDeleteBuffers(vbo);
        shader.cleanup();
        GL11.glDeleteTextures(texture);
    }

    private void uploadTexture(byte[] indexedDisplayBuffer) {
        GL11.glBindTexture(GL11.GL_TEXTURE_2D, texture);

        ByteBuffer texBuf = ByteBuffer.allocateDirect(indexedDisplayBuffer.length);
        texBuf.put(indexedDisplayBuffer).flip();

        GL11.glTexImage2D(
            GL11.GL_TEXTURE_2D, 0, GL11.GL_RGBA,
            Framebuffer.WIDTH, Framebuffer.HEIGHT,
            0, GL11.GL_RGBA, GL11.GL_UNSIGNED_BYTE, texBuf
        );
    }
}
