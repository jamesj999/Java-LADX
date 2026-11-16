package linksawakening.rom;

import static org.junit.jupiter.api.Assertions.assertSame;

/** Tests storage identity without publishing mutable ROM bytes in the runtime API. */
public final class RomOwnershipAssertions {
    private RomOwnershipAssertions() {}

    public static void assertBackedBy(byte[] bytes, Rom rom) {
        assertSame(bytes, rom.backingStorage());
    }
}
