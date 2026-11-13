package linksawakening.render;

import static org.lwjgl.glfw.GLFW.glfwGetFramebufferSize;
import static org.lwjgl.glfw.GLFW.glfwMakeContextCurrent;
import static org.lwjgl.glfw.GLFW.glfwSwapBuffers;

import java.nio.ByteBuffer;
import java.nio.FloatBuffer;
import java.util.ArrayDeque;
import linksawakening.gpu.Framebuffer;
import org.lwjgl.BufferUtils;
import org.lwjgl.opengl.GL;
import org.lwjgl.opengl.GL11;
import org.lwjgl.opengl.GL12;
import org.lwjgl.opengl.GL15;
import org.lwjgl.opengl.GL20;
import org.lwjgl.opengl.GL30;
import org.lwjgl.system.MemoryStack;

/** Owns OpenGL resources that present the composed RGBA framebuffer. */
public final class OpenGlFramePresenter {
    // Each quad vertex stores two position floats followed by two texture-coordinate floats.
    private static final int POSITION_ATTRIBUTE = 0;
    private static final int TEXTURE_ATTRIBUTE = 1;
    private static final int VECTOR_COMPONENTS = 2;
    private static final int VERTEX_STRIDE_BYTES = 4 * Float.BYTES;
    private static final int TEXTURE_OFFSET_BYTES = 2 * Float.BYTES;

    private final ResourceScope resources;
    private final long window;
    private final int vao;

    private final GlShader shader;
    private final int texture;
    private final ByteBuffer textureUploadBuffer;

    private OpenGlFramePresenter(long window, int vao, GlShader shader, int texture, ResourceScope resources) {
        this.resources = resources;
        this.window = window;
        this.vao = vao;

        this.shader = shader;
        this.texture = texture;
        this.textureUploadBuffer = BufferUtils.createByteBuffer(Framebuffer.WIDTH * Framebuffer.HEIGHT * 4);
    }

    public static OpenGlFramePresenter initialize(long window) {
        GL.createCapabilities();
        updateFramebufferViewport(window);

        ResourceScope resources = new ResourceScope();
        try {
            GlShader shader = new GlShader();
            resources.add(shader::cleanup);

            float[] vertices = {
                -1.0f, -1.0f, 0.0f, 1.0f, 1.0f, -1.0f, 1.0f, 1.0f, 1.0f, 1.0f, 1.0f, 0.0f, -1.0f, 1.0f, 0.0f, 0.0f
            };

            FloatBuffer vertexBuffer = BufferUtils.createFloatBuffer(vertices.length);
            vertexBuffer.put(vertices).flip();

            int vao = GL30.glGenVertexArrays();
            resources.add(() -> GL30.glDeleteVertexArrays(vao));
            GL30.glBindVertexArray(vao);

            int vbo = GL15.glGenBuffers();
            resources.add(() -> GL15.glDeleteBuffers(vbo));
            GL15.glBindBuffer(GL15.GL_ARRAY_BUFFER, vbo);
            GL15.glBufferData(GL15.GL_ARRAY_BUFFER, vertexBuffer, GL15.GL_STATIC_DRAW);

            GL20.glEnableVertexAttribArray(POSITION_ATTRIBUTE);
            GL20.glVertexAttribPointer(
                    POSITION_ATTRIBUTE, VECTOR_COMPONENTS, GL11.GL_FLOAT, false, VERTEX_STRIDE_BYTES, 0);
            GL20.glEnableVertexAttribArray(TEXTURE_ATTRIBUTE);
            GL20.glVertexAttribPointer(
                    TEXTURE_ATTRIBUTE,
                    VECTOR_COMPONENTS,
                    GL11.GL_FLOAT,
                    false,
                    VERTEX_STRIDE_BYTES,
                    TEXTURE_OFFSET_BYTES);

            GL30.glBindVertexArray(0);

            int texture = GL11.glGenTextures();
            resources.add(() -> GL11.glDeleteTextures(texture));
            GL11.glBindTexture(GL11.GL_TEXTURE_2D, texture);
            GL11.glTexParameteri(GL11.GL_TEXTURE_2D, GL11.GL_TEXTURE_MIN_FILTER, GL11.GL_NEAREST);
            GL11.glTexParameteri(GL11.GL_TEXTURE_2D, GL11.GL_TEXTURE_MAG_FILTER, GL11.GL_NEAREST);
            GL11.glTexParameteri(GL11.GL_TEXTURE_2D, GL11.GL_TEXTURE_WRAP_S, GL12.GL_CLAMP_TO_EDGE);
            GL11.glTexParameteri(GL11.GL_TEXTURE_2D, GL11.GL_TEXTURE_WRAP_T, GL12.GL_CLAMP_TO_EDGE);

            return new OpenGlFramePresenter(window, vao, shader, texture, resources);
        } catch (RuntimeException | Error failure) {
            resources.closeAfterFailure(failure);
            throw failure;
        }
    }

    public void present(byte[] indexedDisplayBuffer) {
        glfwMakeContextCurrent(window);
        // Query pixels before every draw, including the first visible frame. On
        // macOS the backing scale can change when showing/moving the window,
        // independently of its logical size and of window resize events.
        if (!updateFramebufferViewport(window)) {
            return; // A minimized window may have no drawable framebuffer.
        }
        uploadTexture(indexedDisplayBuffer);
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

    private static boolean updateFramebufferViewport(long window) {
        try (MemoryStack stack = MemoryStack.stackPush()) {
            var width = stack.mallocInt(1);
            var height = stack.mallocInt(1);
            glfwGetFramebufferSize(window, width, height);
            GL11.glViewport(0, 0, width.get(0), height.get(0));
            return width.get(0) > 0 && height.get(0) > 0;
        }
    }

    public void cleanup() {
        resources.close();
    }

    private void uploadTexture(byte[] indexedDisplayBuffer) {
        GL11.glBindTexture(GL11.GL_TEXTURE_2D, texture);

        textureUploadBuffer.clear();
        textureUploadBuffer.put(indexedDisplayBuffer).flip();

        GL11.glTexImage2D(
                GL11.GL_TEXTURE_2D,
                0,
                GL11.GL_RGBA,
                Framebuffer.WIDTH,
                Framebuffer.HEIGHT,
                0,
                GL11.GL_RGBA,
                GL11.GL_UNSIGNED_BYTE,
                textureUploadBuffer);
    }

    static final class GlShader {

        private int program;
        private final ShaderApi api;

        public GlShader() {
            this(new LwjglShaderApi());
        }

        GlShader(ShaderApi api) {
            this.api = api;
            createShaders();
        }

        private void createShaders() {
            String vertexSource =
                    """
                                  #version 330 core
                                  layout(location = 0) in vec2 aPosition;
                                  layout(location = 1) in vec2 aTexCoord;
                                  out vec2 vTexCoord;
                                  void main() {
                                      gl_Position = vec4(aPosition, 0.0, 1.0);
                                      vTexCoord = aTexCoord;
                                  }
                                  """;

            String fragmentSource =
                    """
                                    #version 330 core
                                    in vec2 vTexCoord;
                                    uniform sampler2D uTexture;
                                    out vec4 fragColor;
                                    void main() {
                                        fragColor = texture(uTexture, vTexCoord);
                                    }
                                    """;

            ResourceScope shaders = new ResourceScope();
            try {
                int vertexShader = compileShader(GL20.GL_VERTEX_SHADER, vertexSource, "Vertex", shaders);
                int fragmentShader = compileShader(GL20.GL_FRAGMENT_SHADER, fragmentSource, "Fragment", shaders);
                program = api.createProgram();
                api.attach(program, vertexShader);
                api.attach(program, fragmentShader);
                api.link(program);
                if (!api.linked(program))
                    throw new IllegalStateException("GlShader program link error: " + api.programLog(program));
                shaders.close();
            } catch (RuntimeException | Error failure) {
                shaders.closeAfterFailure(failure);
                try {
                    cleanup();
                } catch (RuntimeException | Error cleanup) {
                    if (cleanup != failure) failure.addSuppressed(cleanup);
                }
                throw failure;
            }
        }

        private int compileShader(int kind, String source, String stage, ResourceScope shaders) {
            int shader = api.createShader(kind);
            shaders.add(() -> api.deleteShader(shader));
            api.source(shader, source);
            api.compile(shader);
            if (!api.compiled(shader))
                throw new IllegalStateException(stage + " shader compile error: " + api.shaderLog(shader));
            return shader;
        }

        public void use() {
            GL20.glUseProgram(program);
        }

        public void setUniform1i(String name, int value) {
            int loc = GL20.glGetUniformLocation(program, name);
            GL20.glUniform1i(loc, value);
        }

        public void setUniform1fv(String name, float[] values) {
            int loc = GL20.glGetUniformLocation(program, name);
            GL20.glUniform1fv(loc, values);
        }

        public int getPositionAttrib() {
            return GL20.glGetAttribLocation(program, "aPosition");
        }

        public int getTexCoordAttrib() {
            return GL20.glGetAttribLocation(program, "aTexCoord");
        }

        public void cleanup() {
            int owned = program;
            program = 0;
            if (owned != 0) api.deleteProgram(owned);
        }
    }

    interface ShaderApi {
        int createShader(int kind);

        void source(int shader, String source);

        void compile(int shader);

        boolean compiled(int shader);

        String shaderLog(int shader);

        void deleteShader(int shader);

        int createProgram();

        void attach(int program, int shader);

        void link(int program);

        boolean linked(int program);

        String programLog(int program);

        void deleteProgram(int program);
    }

    private static final class LwjglShaderApi implements ShaderApi {
        @Override
        public int createShader(int kind) {
            return GL20.glCreateShader(kind);
        }

        @Override
        public void source(int shader, String source) {
            GL20.glShaderSource(shader, source);
        }

        @Override
        public void compile(int shader) {
            GL20.glCompileShader(shader);
        }

        @Override
        public boolean compiled(int shader) {
            return GL20.glGetShaderi(shader, GL20.GL_COMPILE_STATUS) != GL20.GL_FALSE;
        }

        @Override
        public String shaderLog(int shader) {
            return GL20.glGetShaderInfoLog(shader);
        }

        @Override
        public void deleteShader(int shader) {
            GL20.glDeleteShader(shader);
        }

        @Override
        public int createProgram() {
            return GL20.glCreateProgram();
        }

        @Override
        public void attach(int program, int shader) {
            GL20.glAttachShader(program, shader);
        }

        @Override
        public void link(int program) {
            GL20.glLinkProgram(program);
        }

        @Override
        public boolean linked(int program) {
            return GL20.glGetProgrami(program, GL20.GL_LINK_STATUS) != GL20.GL_FALSE;
        }

        @Override
        public String programLog(int program) {
            return GL20.glGetProgramInfoLog(program);
        }

        @Override
        public void deleteProgram(int program) {
            GL20.glDeleteProgram(program);
        }
    }

    static final class ResourceScope implements AutoCloseable {
        private final ArrayDeque<Runnable> actions = new ArrayDeque<>();
        private boolean closed;

        void add(Runnable action) {
            actions.addFirst(action);
        }

        void closeAfterFailure(Throwable failure) {
            try {
                close();
            } catch (RuntimeException | Error cleanup) {
                if (cleanup != failure) failure.addSuppressed(cleanup);
            }
        }

        @Override
        public void close() {
            if (closed) return;
            closed = true;
            Throwable first = null;
            while (!actions.isEmpty())
                try {
                    actions.removeFirst().run();
                } catch (RuntimeException | Error failure) {
                    if (first == null) first = failure;
                    else if (first != failure) first.addSuppressed(failure);
                }
            if (first instanceof Error error) throw error;
            if (first != null) throw (RuntimeException) first;
        }
    }
}
