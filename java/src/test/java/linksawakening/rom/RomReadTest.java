package linksawakening.rom;

import static org.junit.jupiter.api.Assertions.*;

import linksawakening.testing.TestRoms;
import org.junit.jupiter.api.Test;

class RomReadTest {
    private static final RomAddress START = RomAddress.fromOffset(0);

    @Test
    void partialBuffersSkipAlignmentButRetainNullAndReadBoundsChecks() {
        byte[] data = {(byte) 0xFE, 0x12, 0x34};
        Rom rom = TestRoms.partial(data);
        assertEquals(3, rom.size());
        RomOwnershipAssertions.assertBackedBy(data, rom);
        assertEquals(0x12FE, rom.u16le(START));
        data[0] = 7;
        assertEquals(7, rom.u8(START));
        assertThrows(IllegalArgumentException.class, () -> Rom.of(data));
        assertThrows(NullPointerException.class, () -> Rom.unchecked(null));
        assertThrows(NullPointerException.class, () -> TestRoms.partial((byte[]) null));
        assertThrows(IndexOutOfBoundsException.class, () -> rom.u8(START.plus(3)));
        assertThrows(IndexOutOfBoundsException.class, () -> rom.u16le(START.plus(2)));
        assertEquals(0, TestRoms.partial().size());
        assertArrayEquals(new byte[0], rom.bytes(START.plus(3), 0));
    }

    @Test
    void signedUnsignedCopiesAndViews() {
        byte[] data = new byte[0x8000];
        data[0] = (byte) 0xFE;
        data[1] = (byte) 0xAB;
        Rom rom = Rom.of(data);
        assertEquals(data.length, rom.size());
        RomOwnershipAssertions.assertBackedBy(data, rom);
        assertEquals(254, rom.u8(START));
        assertEquals(-2, rom.s8(START));
        assertEquals(0xABFE, rom.u16le(START));
        assertArrayEquals(new int[] {254, 171}, rom.u8Table(START, 2));
        assertArrayEquals(new int[] {-2, -85}, rom.s8Table(START, 2));
        RomSlice view = rom.view(START, 2);
        byte[] copy = rom.bytes(START, 2);
        copy[0] = 0;
        assertEquals(254, view.u8(0));
        assertEquals(-2, view.s8(0));
        assertEquals(0xABFE, view.u16le(0));
        assertEquals(2, view.size());
        assertArrayEquals(new byte[] {(byte) 254}, view.bytes(0, 1));
        data[0] = 3;
        assertEquals(3, view.view(0, 1).u8(0));
        assertEquals(0, rom.bytes(RomAddress.fromOffset(data.length), 0).length);
    }

    @Test
    void boundsAndConstructionFailures() {
        assertThrows(NullPointerException.class, () -> Rom.of(null));
        assertThrows(IllegalArgumentException.class, () -> Rom.of(new byte[1]));
        Rom rom = Rom.of(new byte[0x4000]);
        RomAddress end = RomAddress.fromOffset(0x4000);
        assertTrue(assertThrows(IndexOutOfBoundsException.class, () -> rom.u8(end))
                .getMessage()
                .contains("01:4000 length 1"));
        assertThrows(IndexOutOfBoundsException.class, () -> rom.s8(end));
        assertThrows(IndexOutOfBoundsException.class, () -> rom.u16le(RomAddress.fromOffset(end.offset() - 1)));
        assertThrows(IndexOutOfBoundsException.class, () -> rom.bytes(START, -1));
        assertThrows(IndexOutOfBoundsException.class, () -> rom.bytes(START, Integer.MAX_VALUE));
        assertThrows(IndexOutOfBoundsException.class, () -> rom.view(end, 1));
        assertThrows(IndexOutOfBoundsException.class, () -> rom.u8Table(end, 1));
        assertThrows(IndexOutOfBoundsException.class, () -> rom.s8Table(end, 1));
        assertThrows(NullPointerException.class, () -> rom.u8(null));
        assertThrows(IndexOutOfBoundsException.class, () -> rom.cursor(end.plus(1)));
        RomSlice view = rom.view(START, 1);
        assertThrows(IndexOutOfBoundsException.class, () -> view.u8(-1));
        assertThrows(IndexOutOfBoundsException.class, () -> view.s8(1));
        assertThrows(IndexOutOfBoundsException.class, () -> view.u16le(0));
        assertThrows(IndexOutOfBoundsException.class, () -> view.bytes(0, -1));
        assertThrows(IndexOutOfBoundsException.class, () -> view.view(2, 0));
    }

    @Test
    void pointersUseExplicitBankAndValidateCpuWindow() {
        byte[] data = new byte[0x4000];
        data[1] = 0x40;
        data[3] = 0x12;
        Rom rom = Rom.of(data);
        assertEquals(new RomAddress(3, 0x4000), rom.pointer(START, 3));
        assertArrayEquals(
                new RomAddress[] {new RomAddress(3, 0x4000), new RomAddress(0, 0x1200)}, rom.pointerTable(START, 2, 3));
        assertThrows(IllegalArgumentException.class, () -> rom.pointer(START, 0));
        assertThrows(IllegalArgumentException.class, () -> rom.pointer(START, -1));
        data[1] = (byte) 0x80;
        assertThrows(IllegalArgumentException.class, () -> rom.pointer(START, 3));
        assertThrows(IllegalArgumentException.class, () -> rom.pointerTable(START, -1, 3));
        assertThrows(IllegalArgumentException.class, () -> rom.pointerTable(START, Integer.MAX_VALUE, 3));
        assertThrows(IllegalArgumentException.class, () -> rom.pointerTable(START, 0, -1));
        assertThrows(IndexOutOfBoundsException.class, () -> rom.pointerTable(START, 0x4000, 1));
    }

    @Test
    void cursorIsForwardOnlyAndFailuresDoNotAdvance() {
        byte[] data = new byte[0x4000];
        data[0] = (byte) 255;
        data[1] = (byte) 128;
        data[2] = 1;
        data[3] = 2;
        Rom rom = Rom.of(data);
        RomCursor cursor = rom.view(START, 4).cursor();
        assertEquals(255, cursor.u8());
        assertEquals(-128, cursor.s8());
        assertEquals(513, cursor.u16le());
        assertEquals(START.plus(4), cursor.position());
        assertThrows(IndexOutOfBoundsException.class, cursor::u8);
        assertThrows(IndexOutOfBoundsException.class, cursor::s8);
        assertThrows(IndexOutOfBoundsException.class, cursor::u16le);
        assertThrows(IndexOutOfBoundsException.class, () -> cursor.skip(-1));
        assertThrows(IndexOutOfBoundsException.class, () -> cursor.skip(Integer.MAX_VALUE));
        assertEquals(START.plus(4), cursor.position());
        RomCursor full = rom.cursor(START);
        full.skip(0x4000);
        assertEquals(RomAddress.fromOffset(0x4000), full.position());
    }

    @Test
    void viewsAndPointerTablesTraverseFileOrderExplicitly() {
        byte[] data = new byte[0x8000];
        data[0x3FFF] = 0x34;
        data[0x4000] = 0x12;
        data[0x4001] = 0;
        data[0x4002] = 0x40;
        Rom rom = Rom.of(data);
        RomAddress last = RomAddress.fromOffset(0x3FFF);
        assertEquals(0x1234, rom.u16le(last));
        assertArrayEquals(
                new RomAddress[] {new RomAddress(0, 0x1234), new RomAddress(2, 0x4000)}, rom.pointerTable(last, 2, 2));
        RomCursor cursor = rom.view(last, 4).cursor();
        assertEquals(0x34, cursor.u8());
        assertEquals(new RomAddress(1, 0x4000), cursor.position());
        assertEquals(0x12, cursor.u8());
        cursor.skip(2);
        assertEquals(new RomAddress(1, 0x4003), cursor.position());
        assertArrayEquals(new int[0], rom.u8Table(START, 0));
        assertArrayEquals(new int[0], rom.s8Table(START, 0));
        assertArrayEquals(new RomAddress[0], rom.pointerTable(START, 0, 0));
        assertEquals(0, Rom.of(new byte[0]).size());
    }
}
