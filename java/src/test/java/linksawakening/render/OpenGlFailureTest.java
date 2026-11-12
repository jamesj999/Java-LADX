package linksawakening.render;

import static org.junit.jupiter.api.Assertions.*;

import java.util.ArrayList;
import java.util.List;
import org.junit.jupiter.api.Test;

final class OpenGlFailureTest {
    @Test
    void compileAndLinkFailuresExposeLogsAndReleaseEveryAcquiredObject() {
        for (int stage = 1; stage <= 3; stage++) {
            var gl = new FakeShaderApi();
            gl.fail = stage;
            var error = assertThrows(IllegalStateException.class, () -> new OpenGlFramePresenter.GlShader(gl));
            assertTrue(error.getMessage().contains("fake driver log"));
            assertEquals(stage == 1 ? List.of(1) : List.of(2, 1), gl.deletedShaders);
            assertEquals(stage == 3 ? List.of(100) : List.of(), gl.deletedPrograms);
        }
    }

    @Test
    void successfulProgramCleanupIsIdempotent() {
        var gl = new FakeShaderApi();
        var shader = new OpenGlFramePresenter.GlShader(gl);
        assertEquals(List.of(2, 1), gl.deletedShaders);
        shader.cleanup();
        shader.cleanup();
        assertEquals(List.of(100), gl.deletedPrograms);
    }

    @Test
    void initializationScopeContinuesAfterCleanupFailure() {
        var scope = new OpenGlFramePresenter.ResourceScope();
        var events = new ArrayList<Integer>();
        scope.add(() -> events.add(1));
        scope.add(() -> {
            events.add(2);
            throw new IllegalStateException("fake delete");
        });
        assertThrows(IllegalStateException.class, scope::close);
        scope.close();
        assertEquals(List.of(2, 1), events);
    }

    private static final class FakeShaderApi implements OpenGlFramePresenter.ShaderApi {
        int next;
        int fail;
        final List<Integer> deletedShaders = new ArrayList<>(), deletedPrograms = new ArrayList<>();

        public int createShader(int kind) {
            return ++next;
        }

        public void source(int shader, String source) {}

        public void compile(int shader) {}

        public boolean compiled(int shader) {
            return shader != fail;
        }

        public String shaderLog(int shader) {
            return "fake driver log";
        }

        public void deleteShader(int shader) {
            deletedShaders.add(shader);
        }

        public int createProgram() {
            return 100;
        }

        public void attach(int program, int shader) {}

        public void link(int program) {}

        public boolean linked(int program) {
            return fail != 3;
        }

        public String programLog(int program) {
            return "fake driver log";
        }

        public void deleteProgram(int program) {
            deletedPrograms.add(program);
        }
    }
}
