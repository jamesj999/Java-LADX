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

    @Test
    void loadsSwimmingSpeedTablesFromBankTwo() {
        byte[] rom = new byte[0x100000];
        int speedXOffset = RomBank.romOffset(0x02, 0x4EF0);
        int speedYOffset = RomBank.romOffset(0x02, 0x4F10);
        int entryXOffset = RomBank.romOffset(0x02, 0x750A);
        int entryYOffset = RomBank.romOffset(0x02, 0x750E);
        rom[speedXOffset + 0x01] = 0x08;
        rom[speedXOffset + 0x10 + 0x01] = 0x10;
        rom[speedYOffset + 0x04] = (byte) 0xF8;
        rom[speedYOffset + 0x10 + 0x04] = (byte) 0xF0;
        rom[entryXOffset] = 0x08;
        rom[entryYOffset + 0x03] = 0x08;

        RomTables tables = RomTables.loadFromRom(rom);

        assertEquals(0x08, tables.swimmingSpeedX(0x01, false));
        assertEquals(0x10, tables.swimmingSpeedX(0x01, true));
        assertEquals(-0x08, tables.swimmingSpeedY(0x04, false));
        assertEquals(-0x10, tables.swimmingSpeedY(0x04, true));
        assertEquals(0x08, tables.swimmingEntrySpeedX(0));
        assertEquals(0x08, tables.swimmingEntrySpeedY(3));
    }
}
