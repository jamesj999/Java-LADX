package linksawakening.rom;

import static org.junit.jupiter.api.Assertions.*;

import org.junit.jupiter.api.Test;

class RomHelperReadTest {
    @Test
    void streamPreservesRepeatVerticalAndUntouchedCells() {
        byte[] data = new byte[0x4000];
        byte[] stream = {(byte) 0x98, 1, 0x41, 7, (byte) 0x98, 2, (byte) 0x81, 8, 9, 0};
        System.arraycopy(stream, 0, data, 0, stream.length);
        Rom rom = Rom.of(data);
        RomAddress start = RomAddress.fromOffset(0);
        int[] map = RomBank.decodeBackgroundMap(rom, start, 0x105);
        assertEquals(5, map[0]);
        assertEquals(7, map[1]);
        assertEquals(8, map[2]);
        assertEquals(9, map[34]);
        int[] original = new int[1024];
        original[5] = 123;
        assertEquals(123, RomBank.decodeBackgroundMap(rom, start, original)[5]);
        assertEquals(123, original[5]);
        assertEquals(0, original[1]);
        assertEquals(8, RomBank.decodeBackground(rom, start, 10, 5)[2]);
        int[] sparse = RomUtil.decodeBackgroundFromRom(rom, start, 10, 99);
        assertEquals(7, sparse[0]);
        assertEquals(8, sparse[1]);
        assertEquals(99, sparse[2]);
        assertEquals(9, sparse[11]);
    }

    @Test
    void emptyAndTruncatedStreamsAndPaletteDecoding() {
        Rom empty = Rom.of(new byte[0]);
        assertEquals(0, RomUtil.decodeBackgroundFromRom(empty, RomAddress.fromOffset(0), 10, 0).length);
        assertEquals(1024, RomBank.decodeBackgroundMap(empty, RomAddress.fromOffset(0), 0).length);
        byte[] data = new byte[0x4000];
        data[0] = 31;
        data[2] = (byte) 0xE0;
        data[3] = 3;
        int[][] palettes = RomBank.loadPalettes(Rom.of(data), RomAddress.fromOffset(0), 1);
        assertArrayEquals(new int[] {0xFF0000, 0x00FF00, 0, 0}, palettes[0]);
        data[data.length - 1] = (byte) 0x98;
        Rom rom = Rom.of(data);
        RomAddress last = RomAddress.fromOffset(data.length - 1);
        assertThrows(IndexOutOfBoundsException.class, () -> RomBank.decodeBackgroundMap(rom, last, 0));
        assertThrows(IndexOutOfBoundsException.class, () -> RomUtil.decodeBackgroundFromRom(rom, last, 10, 0));
        assertThrows(IndexOutOfBoundsException.class, () -> RomBank.loadPalettes(rom, last, 1));
    }
}
