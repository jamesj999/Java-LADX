package linksawakening.world.entity;

import linksawakening.rom.Rom;

/** Typed views over test-owned bytes, including deliberately truncated decoder fixtures. */
public final class EntityRomTestData {
    private EntityRomTestData() {}

    @SuppressWarnings("removal") // Partial buffers test legacy decoder bounds; Rom.of rejects them before decoding.
    public static Rom view(byte[] bytes) {
        if (bytes == null) return null;
        return bytes.length % 0x4000 == 0 ? Rom.of(bytes) : new Rom(bytes);
    }
}
