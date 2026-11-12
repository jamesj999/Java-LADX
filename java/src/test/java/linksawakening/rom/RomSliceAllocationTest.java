package linksawakening.rom;

import static org.junit.jupiter.api.Assertions.*;

import linksawakening.testing.AllocationProbe;
import org.junit.jupiter.api.Test;

class RomSliceAllocationTest {
    @Test
    void repeatedTypedByteAndWordReadsAllocateNothing() {
        byte[] bytes = new byte[1024];
        for (int i = 0; i < bytes.length; i++) bytes[i] = (byte) i;
        var view = Rom.unchecked(bytes).view(RomAddress.fromOffset(0), bytes.length);
        int[] checksum = {0};
        double allocated = AllocationProbe.bytesPerIteration(
                4000, 1000, () -> checksum[0] += view.u8(127) + view.s8(128) + view.u16le(200));
        assertEquals(0.0, allocated);
        assertTrue(checksum[0] > 0);
        assertThrows(IndexOutOfBoundsException.class, () -> view.u16le(bytes.length - 1));
    }
}
