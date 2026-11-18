package linksawakening.world.entity;

import linksawakening.rom.Rom;
import linksawakening.testing.TestRoms;

/** Typed views over test-owned bytes, including deliberately truncated decoder fixtures. */
public final class EntityRomTestData {
    private EntityRomTestData() {}

    public static Rom view(byte[] bytes) {
        if (bytes == null) return null;
        return bytes.length % 0x4000 == 0 ? Rom.of(bytes) : TestRoms.partial(bytes);
    }
}
