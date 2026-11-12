package linksawakening.rom;

import static org.junit.jupiter.api.Assertions.*;

import linksawakening.testing.TestRoms;
import org.junit.jupiter.api.Test;

class RomAddressTest {
    @Test
    void bankWindowsAndExplicitOverrides() {
        assertEquals(0x100, new RomAddress(0, 0x100).offset());
        assertEquals(0x7C100, RomSymbols.SfxPointersTable1.offset());
        assertEquals(0xBC100, RomSymbols.SfxPointersTable1.inBank(0x2F).offset());
        assertThrows(IllegalArgumentException.class, () -> new RomAddress(1, 0x8000));
        assertThrows(IllegalArgumentException.class, () -> new RomAddress(0, 0x4000));
    }

    @Test
    void unsignedAndLittleEndianReadsAndSliceOwnership() {
        byte[] bytes = {(byte) 0xFE, (byte) 0xAB};
        Rom rom = TestRoms.partial(bytes);
        RomAddress start = new RomAddress(0, 0);
        assertEquals(254, rom.u8(start));
        assertEquals(0xABFE, rom.u16le(start));
        byte[] copy = rom.slice(start, 2);
        copy[0] = 0;
        assertEquals(254, rom.u8(start));
        assertThrows(IndexOutOfBoundsException.class, () -> rom.slice(start, 3));
    }
}
