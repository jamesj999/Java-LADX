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

    @Test
    void loadsUnsignedFineCollisionRowsFromBankThree() {
        byte[] rom = new byte[0x100000];
        int offset = RomBank.romOffset(0x03, 0x7A85);
        int rowOffset = (0x80 - 0x7C) * 4;
        rom[offset + rowOffset] = (byte) 0xFF;
        rom[offset + rowOffset + 1] = 0x01;
        rom[offset + rowOffset + 2] = 0x00;
        rom[offset + rowOffset + 3] = (byte) 0x80;
        RomTables tables = RomTables.loadFromRom(rom);
        assertEquals(0xFF, tables.entityFineCollisionShape(0x80, 0));
        assertEquals(0x01, tables.entityFineCollisionShape(0x80, 1));
        assertEquals(0x00, tables.entityFineCollisionShape(0x80, 2));
        assertEquals(0x80, tables.entityFineCollisionShape(0x80, 3));
        assertEquals(0, tables.entityFineCollisionShape(0x8E, 0));
    }
}
