package linksawakening.entity;

import linksawakening.rom.RomBank;
import linksawakening.world.EntityRoomLoader;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;

final class EntitySpriteHandlerCatalogTest {

    @Test
    void decodesSyntheticPairAndSingleDisplayListsWithUnsignedBytes() {
        byte[] rom = syntheticRom();
        write(rom, 0x06, 0x5000,
            0x80, 0xFF, 0x81, 0xFE,
            0x82, 0xFD, 0x83, 0xFC,
            0x84, 0xFB, 0x85, 0xFA,
            0x86, 0xF9, 0x87, 0xF8);
        write(rom, 0x06, 0x5100, 0x90, 0xF7, 0x91, 0xF6);

        EntitySpriteHandlerCatalog catalog = new EntitySpriteHandlerCatalog(rom);
        EntitySpriteDefinition pair = catalog.decodePair(0xD0, 0x06, 0x5000, 4, 2);
        EntitySpriteDefinition single = catalog.decodeSingle(0xD1, 0x06, 0x5100, 2, 1);

        assertEquals(EntitySpriteDefinition.Shape.PAIR, pair.shape());
        assertEquals(4, pair.variantCount());
        assertEquals(2, pair.initialVariant());
        assertEquals(0x80, pair.variant(0).first().tile());
        assertEquals(0xFF, pair.variant(0).first().attributes());
        assertEquals(0x83, pair.variant(1).second().tile());
        assertEquals(0xFC, pair.variant(1).second().attributes());

        assertEquals(EntitySpriteDefinition.Shape.SINGLE, single.shape());
        assertEquals(0x90, single.variant(0).first().tile());
        assertEquals(0xF6, single.variant(1).first().attributes());
        assertEquals(null, single.variant(0).second());
    }

    @Test
    void mapsSupportedHandlersToDisassemblyBanksAddressesAndInitialVariants() {
        EntitySpriteHandlerCatalog catalog = new EntitySpriteHandlerCatalog(syntheticRom());

        EntitySpriteDefinition crow = catalog.forEntityType(
            0x7A, EntityRoomLoader.RoomTable.OVERWORLD);
        assertDefinition(crow, 0x06, 0x5C89, EntitySpriteDefinition.Shape.PAIR, 4, 2);

        EntitySpriteDefinition dog = catalog.forEntityType(
            0x6F, EntityRoomLoader.RoomTable.OVERWORLD);
        assertDefinition(dog, 0x19, 0x48CA, EntitySpriteDefinition.Shape.PAIR, 4, 2);

        EntitySpriteDefinition marinOutdoor = catalog.forEntityType(
            0x3E, EntityRoomLoader.RoomTable.OVERWORLD);
        assertDefinition(marinOutdoor, 0x05, 0x4E2A,
            EntitySpriteDefinition.Shape.PAIR, 11, 6);

        EntitySpriteDefinition marinIndoor = catalog.forEntityType(
            0x3E, EntityRoomLoader.RoomTable.INDOORS_A);
        assertDefinition(marinIndoor, 0x05, 0x4E0A,
            EntitySpriteDefinition.Shape.PAIR, 8, 6);

        EntitySpriteDefinition marinAtTalTal = catalog.forEntityType(
            0xC2, EntityRoomLoader.RoomTable.OVERWORLD);
        assertDefinition(marinAtTalTal, 0x18, 0x5EB7,
            EntitySpriteDefinition.Shape.PAIR, 8, 0);

        EntitySpriteDefinition kid = catalog.forEntityType(
            0x73, EntityRoomLoader.RoomTable.OVERWORLD);
        assertDefinition(kid, 0x06, 0x604D, EntitySpriteDefinition.Shape.PAIR, 4, 0);

        EntitySpriteDefinition butterfly = catalog.forEntityType(
            0x6E, EntityRoomLoader.RoomTable.OVERWORLD);
        assertDefinition(butterfly, 0x06, 0x6BBD,
            EntitySpriteDefinition.Shape.SINGLE, 2, 0);
    }

    @Test
    void unsupportedTypesRemainExplicitAndDoNotReadGuessedArt() {
        EntitySpriteDefinition definition = new EntitySpriteHandlerCatalog(syntheticRom())
            .forEntityType(0x29, EntityRoomLoader.RoomTable.OVERWORLD);

        assertEquals(EntitySpriteDefinition.Shape.UNSUPPORTED, definition.shape());
        assertFalse(definition.supported());
        assertEquals(0x29, definition.entityType());
        assertEquals(0, definition.variantCount());
    }

    @Test
    void rejectsDisplayListsThatWouldReadOutsideTheSelectedRomBank() {
        EntitySpriteHandlerCatalog catalog = new EntitySpriteHandlerCatalog(syntheticRom());

        assertThrows(IllegalArgumentException.class,
            () -> catalog.decodePair(0xD0, 0x06, 0x7FFF, 2, 0));
        assertThrows(IllegalArgumentException.class,
            () -> catalog.decodeSingle(0xD1, 0x06, 0x4000, 0, 0));
    }

    @Test
    void shippedRomDisplayListsContainTheExpectedFirstVariants() throws Exception {
        EntitySpriteHandlerCatalog catalog = new EntitySpriteHandlerCatalog(loadRom());

        EntitySpriteDefinition crow = catalog.forEntityType(
            0x7A, EntityRoomLoader.RoomTable.OVERWORLD);
        assertEquals(0x50, crow.variant(0).first().tile());
        assertEquals(0x03, crow.variant(0).first().attributes());
        assertEquals(0x52, crow.variant(2).first().tile());
        assertEquals(0x23, crow.variant(2).first().attributes());

        EntitySpriteDefinition butterfly = catalog.forEntityType(
            0x6E, EntityRoomLoader.RoomTable.OVERWORLD);
        assertEquals(0x5E, butterfly.variant(0).first().tile());
        assertEquals(0x01, butterfly.variant(0).first().attributes());
        assertEquals(0x41, butterfly.variant(1).first().attributes());
    }

    private static void assertDefinition(EntitySpriteDefinition definition, int bank, int address,
                                          EntitySpriteDefinition.Shape shape, int variants,
                                          int initialVariant) {
        assertEquals(bank, definition.bank());
        assertEquals(address, definition.address());
        assertEquals(shape, definition.shape());
        assertEquals(variants, definition.variantCount());
        assertEquals(initialVariant, definition.initialVariant());
    }

    private static byte[] syntheticRom() {
        return new byte[RomBank.romOffset(0x1A, 0x4000)];
    }

    private static void write(byte[] rom, int bank, int address, int... values) {
        int offset = RomBank.romOffset(bank, address);
        for (int value : values) {
            rom[offset++] = (byte) value;
        }
    }

    private static byte[] loadRom() throws Exception {
        try (var stream = EntitySpriteHandlerCatalogTest.class.getClassLoader()
            .getResourceAsStream("rom/azle.gbc")) {
            if (stream == null) {
                throw new IllegalStateException("ROM resource missing");
            }
            return stream.readAllBytes();
        }
    }
}
