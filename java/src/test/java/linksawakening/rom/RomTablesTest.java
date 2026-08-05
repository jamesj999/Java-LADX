package linksawakening.rom;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

final class RomTablesTest {

    @Test
    void loadsEntityCollisionSelectionAndSignedPointOffsetsFromRom() {
        byte[] rom = new byte[0x100000];
        int hitboxOffset = RomBank.romOffset(0x03, 0x40FB);
        rom[hitboxOffset + 0x27] = (byte) 0x82;
        int pointsXOffset = RomBank.romOffset(0x03, 0x785F);
        int pointsYOffset = RomBank.romOffset(0x03, 0x786F);
        rom[pointsXOffset + 2 * 4 + 1] = (byte) 0xFF;
        rom[pointsYOffset + 2 * 4 + 1] = (byte) 0xFE;

        RomTables tables = RomTables.loadFromRom(rom);

        assertEquals(2, tables.entityCollisionBoxType(0x27));
        assertEquals(-1, tables.entityCollisionPointX(2, 1));
        assertEquals(-2, tables.entityCollisionPointY(2, 1));
    }
}
