package linksawakening.rom;

import static org.junit.jupiter.api.Assertions.*;

import org.junit.jupiter.api.Test;

final class RomCopyIntoTest {
    @Test
    void copiesDirectlyFromRetainedSourceAndChecksBothRanges() {
        byte[] data = {1, 2, 3, 4};
        Rom rom = Rom.unchecked(data);
        byte[] out = new byte[4];
        rom.copyInto(1, out, 1, 2);
        assertArrayEquals(new byte[] {0, 2, 3, 0}, out);
        data[1] = 9;
        rom.copyInto(1, out, 0, 1);
        assertEquals(9, out[0]);
        assertThrows(IndexOutOfBoundsException.class, () -> rom.copyInto(-1, out, 0, 1));
        assertThrows(IndexOutOfBoundsException.class, () -> rom.copyInto(3, out, 0, 2));
        assertThrows(IndexOutOfBoundsException.class, () -> rom.copyInto(0, out, 3, 2));
        assertThrows(IndexOutOfBoundsException.class, () -> rom.copyInto(0, out, 0, -1));
    }
}
