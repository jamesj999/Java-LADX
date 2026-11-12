package linksawakening.testing;

import java.io.InputStream;

/** Single entry point for test ROM reads, including standalone capture tools. */
public final class TestRom {
    private TestRom() {}

    public static InputStream open() {
        StackWalker.getInstance(StackWalker.Option.RETAIN_CLASS_REFERENCE).forEach(frame -> {
            Class<?> type = frame.getDeclaringClass();
            if (isTestClass(type) && !requiresRom(type)) {
                throw new AssertionError("ROM access from " + type.getName() + ": add @RequiresRom to the test class");
            }
        });
        InputStream stream = TestRom.class.getResourceAsStream("/rom/azle.gbc");
        if (stream == null)
            throw new AssertionError(
                    "Requires user ROM; add @RequiresRom to ROM tests and supply -Pla.rom=/path/azle.gbc");
        return stream;
    }

    private static boolean isTestClass(Class<?> type) {
        return java.util.Arrays.stream(type.getDeclaredMethods()).anyMatch(method -> java.util.Arrays.stream(
                        method.getAnnotations())
                .anyMatch(annotation -> annotation.annotationType().getName().startsWith("org.junit.jupiter.")));
    }

    private static boolean requiresRom(Class<?> type) {
        for (Class<?> current = type; current != null; current = current.getEnclosingClass()) {
            if (current.isAnnotationPresent(RequiresRom.class)) return true;
        }
        return false;
    }
}
