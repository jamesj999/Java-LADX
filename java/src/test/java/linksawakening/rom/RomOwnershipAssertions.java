package linksawakening.rom;

import static org.junit.jupiter.api.Assertions.assertSame;

/** Tests storage identity without publishing mutable ROM bytes in the runtime API. */
public final class RomOwnershipAssertions {
    private RomOwnershipAssertions() {}

    public static void assertBackedBy(byte[] bytes, Rom rom) {
        try {
            var storage = Rom.class.getDeclaredField("data");
            storage.setAccessible(true);
            assertSame(bytes, storage.get(rom));
        } catch (ReflectiveOperationException failure) {
            throw new AssertionError(failure);
        }
    }
}
