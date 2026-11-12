package linksawakening.render;

import static org.junit.jupiter.api.Assertions.assertFalse;

import java.nio.file.Files;
import java.nio.file.Path;
import linksawakening.gpu.GPU;
import org.junit.jupiter.api.Test;

/** Prevents line-limit-driven implementation hierarchies in the platform packages. */
final class PlatformInheritanceTest {
    @Test
    void platformClassesDoNotExtendProjectImplementationClasses() throws Exception {
        Path root = Path.of(
                GPU.class.getProtectionDomain().getCodeSource().getLocation().toURI());
        ClassLoader loader = GPU.class.getClassLoader();
        for (String pkg : new String[] {"audio", "save", "config", "input", "gpu", "render", "rom", "app/host"}) {
            try (var paths = Files.walk(root.resolve("linksawakening/" + pkg))) {
                for (Path path :
                        paths.filter(p -> p.toString().endsWith(".class")).toList()) {
                    String name =
                            root.relativize(path).toString().replace('/', '.').replace(".class", "");
                    Class<?> type = Class.forName(name, false, loader);
                    Class<?> parent = type.getSuperclass();
                    assertFalse(parent != null && parent.getName().startsWith("linksawakening."), name);
                }
            }
        }
    }
}
