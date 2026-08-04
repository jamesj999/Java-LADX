package linksawakening.world;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

final class RomRandomByteSourceTest {

    @Test
    void followsGetRandomByteSeedFrameAndScanlineSequence() {
        RomRandomByteSource source = new RomRandomByteSource();

        source.beginFrame(0, 0x20);
        assertEquals(0x61, source.getAsInt()); // rrca(0xA2 + 0x20)
        assertEquals(0xC0, source.getAsInt()); // rrca(0x61 + 0x20)

        source.beginFrame(1, 0x00);
        assertEquals(0xE0, source.getAsInt()); // rrca(0xC0 + 1)
    }
}
