package linksawakening;

import java.nio.file.Path;
import java.util.concurrent.ConcurrentHashMap;
import static org.junit.jupiter.api.Assertions.*;

/** Checks compiled invocation boundaries rather than Java source formatting. */
final class CompiledCode {
    private static final ConcurrentHashMap<String, String> CACHE = new ConcurrentHashMap<>();

    static String code(String type) {
        return CACHE.computeIfAbsent(type, name -> {
            try {
                var javap = Path.of(System.getProperty("java.home"), "bin", "javap").toString();
                var location = Class.forName(name).getProtectionDomain().getCodeSource().getLocation();
                var process = new ProcessBuilder(javap, "-c", "-p", "-classpath",
                    Path.of(location.toURI()).toString(), name).redirectErrorStream(true).start();
                String result = new String(process.getInputStream().readAllBytes(),
                    java.nio.charset.StandardCharsets.UTF_8);
                assertEquals(0, process.waitFor(), result);
                return result;
            } catch (Exception exception) {
                throw new AssertionError(exception);
            }
        });
    }

    static String implementation(Class<?> owner, Class<?> contract) throws Exception {
        var directory = Path.of(owner.getProtectionDomain().getCodeSource().getLocation().toURI())
            .resolve(owner.getPackageName().replace('.', '/'));
        try (var files = java.nio.file.Files.list(directory)) {
            for (var file : files.filter(f -> f.getFileName().toString()
                    .startsWith(owner.getSimpleName() + "$")).toList()) {
                String name = owner.getPackageName() + "." + file.getFileName().toString()
                    .replace(".class", "");
                if (contract.isAssignableFrom(Class.forName(name))) return code(name);
            }
        }
        throw new AssertionError("No implementation of " + contract + " in " + owner);
    }

    static String method(String type, String name) {
        String code = code(type);
        // Match the declaration, not an invocation comment.
        for (String block : code.split("(?m)(?=^  [^ \n][^\n]*\\([^\n]*\\);)")) {
            if (block.lines().findFirst().orElse("").contains(name + "(")) return block;
        }
        throw new AssertionError("Missing compiled method " + type + "." + name);
    }

    static void calls(String type, String method, String... calls) {
        String body = method(type, method);
        int previous = -1;
        for (String call : calls) {
            int position = body.indexOf(call, previous + 1);
            assertTrue(position > previous, () -> "Missing or reordered " + call + " in " + type + "." + method);
            previous = position;
        }
    }
}
