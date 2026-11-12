package linksawakening.entity;

import static org.junit.jupiter.api.Assertions.*;

import java.nio.file.Files;
import java.nio.file.Path;
import org.junit.jupiter.api.Test;

/** Prevents raw ROM ownership from returning behind typed loader entry points. */
class OwnedRomBoundaryTest {
    @Test
    void rawRomParametersExistOnlyInDeprecatedDelegates() throws Exception {
        for (String area : new String[] {
            "entity", "equipment", "state", "dialog", "ui", "cutscene", "startup", "scene", "physics", "vfx"
        }) {
            try (var files = Files.walk(Path.of("src/main/java/linksawakening", area))) {
                for (Path file :
                        files.filter(p -> p.toString().endsWith(".java")).toList()) {
                    String source = Files.readString(file)
                            .replaceAll("@Deprecated\\(forRemoval = true\\)\\s+public [^{}]+\\{[^{}]*\\}", "");
                    assertFalse(source.matches("(?s).*byte\\[\\]\\s+rom(?:Data)?\\b.*"), file.toString());
                }
            }
        }
    }
}
