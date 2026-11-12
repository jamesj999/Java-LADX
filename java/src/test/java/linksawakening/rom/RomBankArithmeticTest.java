package linksawakening.rom;

import static org.junit.jupiter.api.Assertions.*;

import org.junit.jupiter.api.Test;

class RomBankArithmeticTest {
    @Test
    void windowsDoNotWrap() {
        assertThrows(IllegalArgumentException.class, () -> new RomAddress(0, 0x3FFF).plus(1));
        assertThrows(IllegalArgumentException.class, () -> new RomAddress(1, 0x7FFF).plus(1));
        assertThrows(IllegalArgumentException.class, () -> new RomAddress(1, 0x4000).plus(-1));
        assertThrows(ArithmeticException.class, () -> new RomAddress(1, 0x4000).plus(Integer.MAX_VALUE));
        assertEquals(new RomAddress(2, 0x4000), RomAddress.fromOffset(0x8000));
        assertThrows(IllegalArgumentException.class, () -> RomAddress.fromOffset(-1));
        assertThrows(IllegalArgumentException.class, () -> new RomAddress(-1, 0x4000));
        assertEquals(new RomAddress(1, 0x4001), new RomAddress(1, 0x4000).plus(1));
    }

    @Test
    void gbcAdjustmentIsExplicitAndIdempotent() {
        for (int bank = 1; bank < 0x40; bank++) {
            assertEquals(bank | 0x20, GbcBanks.adjust(bank));
            assertEquals(GbcBanks.adjust(bank), GbcBanks.adjust(GbcBanks.adjust(bank)));
            assertEquals(new RomAddress(bank | 0x20, 0x4000), new RomAddress(bank, 0x4000).withGbcBank());
        }
        assertEquals(0x20, GbcBanks.adjust(0));
        assertThrows(IllegalArgumentException.class, () -> new RomAddress(0, 0).withGbcBank());
    }
}
