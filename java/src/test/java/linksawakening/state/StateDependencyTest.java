package linksawakening.state;

import static org.junit.jupiter.api.Assertions.*;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.regex.Pattern;
import org.junit.jupiter.api.Test;

class StateDependencyTest {
    @Test
    void stateDependsOnlyOnStateRomAndSave() throws Exception {
        Path root = Path.of("src/main/java/linksawakening/state");
        Pattern dependency = Pattern.compile("linksawakening\\.([a-z]+)\\.");
        try (var files = Files.list(root)) {
            for (Path file : files.filter(p -> p.toString().endsWith(".java")).toList()) {
                String source = Files.readString(file);
                var matcher = dependency.matcher(source);
                while (matcher.find()) {
                    assertTrue(
                            java.util.Set.of("state", "rom", "save").contains(matcher.group(1)),
                            file + " depends on " + matcher.group());
                }
            }
        }
    }

    @Test
    void physicsIsInputFree() throws Exception {
        try (var files = Files.walk(Path.of("src/main/java/linksawakening/physics"))) {
            for (Path file : files.filter(p -> p.toString().endsWith(".java")).toList()) {
                assertFalse(Files.readString(file).contains("linksawakening.input"), file.toString());
            }
        }
    }
}
