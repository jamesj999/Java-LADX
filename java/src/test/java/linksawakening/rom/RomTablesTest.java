package linksawakening.rom;

import static org.junit.jupiter.api.Assertions.assertEquals;

import org.junit.jupiter.api.Test;

final class RomTablesTest {

    @Test
    void preservesPieceOfPowerGroundSpeedRowAndSignedSpeeds() {
        byte[] rom = new byte[0x100000];
        int x = RomSymbols.HorizontalIncrementForLinkPosition.offset();
        int y = RomSymbols.VerticalIncrementForLinkPosition.offset();
        rom[x + 1] = 0x10;
        rom[x + 0x11] = 0x14;
        rom[y + 5] = (byte) -0x0C;
        rom[y + 0x15] = (byte) -0x0F;
        RomTables tables = RomTables.load(Rom.unchecked(rom));
        assertEquals(0x10, tables.linkMotionTables().linkSpeedX(1));
        assertEquals(0x14, tables.linkMotionTables().linkSpeedX(0x11));
        assertEquals(-0x0C, tables.linkMotionTables().linkSpeedY(5));
        assertEquals(-0x0F, tables.linkMotionTables().linkSpeedY(0x15));
    }

    @Test
    void loadsEntityCollisionSelectionAndSignedPointOffsetsFromRom() {
        byte[] rom = new byte[0x100000];
        int hitboxOffset = RomBank.romOffset(0x03, 0x40FB);
        rom[hitboxOffset + 0x27] = (byte) 0x82;
        int pointsXOffset = RomBank.romOffset(0x03, 0x785F);
        int pointsYOffset = RomBank.romOffset(0x03, 0x786F);
        rom[pointsXOffset + 2 * 4 + 1] = (byte) 0xFF;
        rom[pointsYOffset + 2 * 4 + 1] = (byte) 0xFE;

        RomTables tables = RomTables.load(Rom.unchecked(rom));

        assertEquals(2, tables.entityPropertyTables().entityCollisionBoxType(0x27));
        assertEquals(-1, tables.collisionTables().entityCollisionPointX(2, 1));
        assertEquals(-2, tables.collisionTables().entityCollisionPointY(2, 1));
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
        RomTables tables = RomTables.load(Rom.unchecked(rom));
        assertEquals(0xFF, tables.collisionTables().entityFineCollisionShape(0x80, 0));
        assertEquals(0x01, tables.collisionTables().entityFineCollisionShape(0x80, 1));
        assertEquals(0x00, tables.collisionTables().entityFineCollisionShape(0x80, 2));
        assertEquals(0x80, tables.collisionTables().entityFineCollisionShape(0x80, 3));
        assertEquals(0, tables.collisionTables().entityFineCollisionShape(0x8E, 0));
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

        RomTables tables = RomTables.load(Rom.unchecked(rom));

        assertEquals(0x08, tables.linkMotionTables().swimmingSpeedX(0x01, false));
        assertEquals(0x10, tables.linkMotionTables().swimmingSpeedX(0x01, true));
        assertEquals(-0x08, tables.linkMotionTables().swimmingSpeedY(0x04, false));
        assertEquals(-0x10, tables.linkMotionTables().swimmingSpeedY(0x04, true));
        assertEquals(0x08, tables.linkMotionTables().swimmingEntrySpeedX(0));
        assertEquals(0x08, tables.linkMotionTables().swimmingEntrySpeedY(3));
    }

    @Test
    void loadsMagicRodLinkOamTablesFromBankTwo() {
        byte[] rom = new byte[0x100000];
        int xOffset = RomBank.romOffset(0x02, 0x52E0);
        int yOffset = RomBank.romOffset(0x02, 0x52E8);
        int tileOffset = RomBank.romOffset(0x02, 0x52F0);
        int attrOffset = RomBank.romOffset(0x02, 0x5300);
        rom[xOffset + 0] = 0x0D;
        rom[xOffset + 7] = (byte) 0xF5;
        rom[yOffset + 2] = (byte) 0xF3;
        rom[tileOffset + 8] = 0x06;
        rom[tileOffset + 9] = 0x08;
        rom[attrOffset + 14] = 0x22;
        rom[attrOffset + 15] = 0x22;

        RomTables tables = RomTables.load(Rom.unchecked(rom));

        assertEquals(0x0D, tables.equipmentTables().magicRodXOffset(0));
        assertEquals(-0x0B, tables.equipmentTables().magicRodXOffset(7));
        assertEquals(-0x0D, tables.equipmentTables().magicRodYOffset(2));
        assertEquals(0x06, tables.equipmentTables().magicRodTile(4, 0));
        assertEquals(0x08, tables.equipmentTables().magicRodTile(4, 1));
        assertEquals(0x22, tables.equipmentTables().magicRodAttribute(7, 0));
        assertEquals(0x22, tables.equipmentTables().magicRodAttribute(7, 1));
    }

    @Test
    void loadsShovelAnimationPhasesFromBankTwo() {
        byte[] rom = new byte[0x100000];
        int offset = RomBank.romOffset(0x02, 0x4B41);
        int[] states = {0x71, 0x72, 0x6F, 0x70, 0x73, 0x74, 0x6D, 0x6E};
        for (int i = 0; i < states.length; i++) {
            rom[offset + i] = (byte) states[i];
        }

        RomTables tables = RomTables.load(Rom.unchecked(rom));

        assertEquals(0x71, tables.equipmentTables().shovelAnimationState(1, 0));
        assertEquals(0x72, tables.equipmentTables().shovelAnimationState(0x10, 0));
        assertEquals(0x6E, tables.equipmentTables().shovelAnimationState(0x10, 3));
    }
}
