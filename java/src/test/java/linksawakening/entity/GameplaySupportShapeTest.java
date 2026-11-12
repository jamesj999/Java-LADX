package linksawakening.entity;

import static org.junit.jupiter.api.Assertions.*;

import java.lang.reflect.Executable;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import org.junit.jupiter.api.Test;

class GameplaySupportShapeTest {
    private static final List<String> PACKAGES = List.of(
            "entity",
            "equipment",
            "state",
            "gameplay",
            "dialog",
            "ui",
            "cutscene",
            "startup",
            "scene",
            "physics",
            "vfx");

    @Test
    void ownedMainFilesStayWithinFourHundredLines() throws Exception {
        for (String area : PACKAGES) {
            try (var files = Files.walk(Path.of("src/main/java/linksawakening", area))) {
                for (Path file :
                        files.filter(p -> p.toString().endsWith(".java")).toList()) {
                    assertTrue(Files.readAllLines(file).size() <= 400, file.toString());
                }
            }
        }
    }

    @Test
    void longSignaturesAreExplicitDeprecatedCompatibilityEntries() throws Exception {
        Path root = Path.of("build/classes/java/main");
        for (String area : PACKAGES) {
            try (var files = Files.walk(root.resolve("linksawakening/" + area))) {
                for (Path file :
                        files.filter(p -> p.toString().endsWith(".class")).toList()) {
                    String name =
                            root.relativize(file).toString().replace('/', '.').replace('\\', '.');
                    Class<?> type = Class.forName(
                            name.substring(0, name.length() - 6),
                            false,
                            getClass().getClassLoader());
                    for (var constructor : type.getDeclaredConstructors()) check(constructor);
                    for (var method : type.getDeclaredMethods()) check(method);
                }
            }
        }
    }

    private static void check(Executable member) {
        if (member.isSynthetic()) return;
        assertTrue(member.getParameterCount() <= 8 || member.isAnnotationPresent(Deprecated.class), member.toString());
    }
}
