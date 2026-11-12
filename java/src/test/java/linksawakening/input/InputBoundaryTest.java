package linksawakening.input;

import static org.junit.jupiter.api.Assertions.assertFalse;

import java.nio.file.Files;
import java.nio.file.Path;
import org.junit.jupiter.api.Test;

final class InputBoundaryTest {
    @Test
    void inputSourcesDoNotImportLwjglEvenForInlinedConstants() throws Exception {
        try (var paths = Files.walk(Path.of("src/main/java/linksawakening/input"))) {
            for (Path path : paths.filter(p -> p.toString().endsWith(".java")).toList()) {
                String source = Files.readString(path);
                assertFalse(
                        source.contains("import org.lwjgl") || source.contains("import static org.lwjgl"),
                        path.toString());
            }
        }
    }
}
