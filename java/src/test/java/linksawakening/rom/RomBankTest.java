package linksawakening.rom;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

final class RomBankTest {

    @Test
    void convertsBankedAddressToRomOffset() {
        assertEquals(0x1234, RomBank.romOffset(0, 0x1234));
        assertEquals(0x24200, RomBank.romOffset(0x09, 0x4200));
        assertEquals(0x2310A, RomBank.romOffset(0x08, 0x710A));
    }

    @Test
    void decodesRgb555ToRgb888() {
        assertEquals(0xFF0000, RomBank.decodeRgb555(0x001F));
        assertEquals(0x00FF00, RomBank.decodeRgb555(0x03E0));
        assertEquals(0x0000FF, RomBank.decodeRgb555(0x7C00));
    }
}
