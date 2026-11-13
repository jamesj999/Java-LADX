package linksawakening.architecture;

import static org.junit.jupiter.api.Assertions.assertFalse;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.regex.Pattern;
import org.junit.jupiter.api.Test;

/** Protects source hygiene without file-size limits or exceptions for large cohesive classes. */
class OwnedSourceHygieneTest {
    @Test
    void ownedMainAndTestSourcesUseExplicitImports() throws Exception {
        Pattern wildcard = Pattern.compile("(?m)^import\\s+(?:static\\s+)?[^;]+\\.\\*;");
        for (String sourceSet : new String[] {"main", "test"}) {
            try (var files = Files.walk(Path.of("src", sourceSet, "java", "linksawakening"))) {
                for (Path file : files.filter(p -> p.toString().endsWith(".java"))
                        .filter(p -> !p.toString().contains("/world/"))
                        .toList()) {
                    assertFalse(wildcard.matcher(Files.readString(file)).find(), file.toString());
                }
            }
        }
    }

    @Test
    void runtimeDoesNotExposeTimerInspectionHooksForTests() throws Exception {
        Pattern hook = Pattern.compile("\\b\\w*ForTest\\w*\\s*\\(");
        try (var files = Files.walk(Path.of("src/main/java/linksawakening"))) {
            for (Path file : files.filter(p -> p.toString().endsWith(".java"))
                    .filter(p -> !p.toString().contains("/world/"))
                    .toList()) {
                assertFalse(hook.matcher(Files.readString(file)).find(), file.toString());
            }
        }
    }
}
