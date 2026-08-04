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
    void decodesRectangleDisplayListsWithSignedOffsets() {
        byte[] rom = syntheticRom();
        write(rom, 0x06, 0x5200,
            0xF7, 0x00, 0x70, 0x02,
            0x07, 0xF8, 0x72, 0x22,
            0xF8, 0x08, 0x74, 0x42,
            0x08, 0xF0, 0x76, 0x62);

        EntitySpriteDefinition rectangle = new EntitySpriteHandlerCatalog(rom)
            .decodeRectangle(0xD2, 0x06, 0x5200, 1, 4, 0);

        assertEquals(EntitySpriteDefinition.Shape.RECTANGLE, rectangle.shape());
        assertEquals(1, rectangle.variantCount());
        assertEquals(4, rectangle.rectangleVariant(0).size());
        EntitySpriteDefinition.RectangleSprite first = rectangle.rectangleVariant(0).get(0);
        assertEquals(-9, first.yOffset());
        assertEquals(0, first.xOffset());
        assertEquals(0x70, first.oam().tile());
        assertEquals(0x02, first.oam().attributes());

        EntitySpriteDefinition.RectangleSprite last = rectangle.rectangleVariant(0).get(3);
        assertEquals(8, last.yOffset());
        assertEquals(-16, last.xOffset());
        assertEquals(0x76, last.oam().tile());
        assertEquals(0x62, last.oam().attributes());
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

        EntitySpriteDefinition keese = catalog.forEntityType(
            0x19, EntityRoomLoader.RoomTable.INDOORS_A);
        assertDefinition(keese, 0x06, 0x6708,
            EntitySpriteDefinition.Shape.PAIR, 2, 0);

        EntitySpriteDefinition octorok = catalog.forEntityType(
            0x09, EntityRoomLoader.RoomTable.OVERWORLD);
        assertDefinition(octorok, 0x03, 0x57FB,
            EntitySpriteDefinition.Shape.PAIR, 8, 0);

        EntitySpriteDefinition caveBKeese = catalog.forEntityType(
            0x19, EntityRoomLoader.RoomTable.INDOORS_A, 0x0A);
        assertDefinition(caveBKeese, 0x06, 0x6710,
            EntitySpriteDefinition.Shape.PAIR, 2, 0);

        EntitySpriteDefinition armos = catalog.forEntityType(
            0x0F, EntityRoomLoader.RoomTable.OVERWORLD);
        assertDefinition(armos, 0x06, 0x7446,
            EntitySpriteDefinition.Shape.PAIR, 2, 0);

        EntitySpriteDefinition tektite = catalog.forEntityType(
            0x0D, EntityRoomLoader.RoomTable.OVERWORLD);
        assertDefinition(tektite, 0x06, 0x78B7,
            EntitySpriteDefinition.Shape.PAIR, 2, 0);

        EntitySpriteDefinition leever = catalog.forEntityType(
            0x0E, EntityRoomLoader.RoomTable.OVERWORLD);
        assertDefinition(leever, 0x04, 0x7EE5,
            EntitySpriteDefinition.Shape.PAIR, 4, 0);

        EntitySpriteDefinition stalfos = catalog.forEntityType(
            0x1A, EntityRoomLoader.RoomTable.INDOORS_A);
        assertDefinition(stalfos, 0x06, 0x4AA8,
            EntitySpriteDefinition.Shape.PAIR, 3, 0);

        EntitySpriteDefinition peaHat = catalog.forEntityType(
            0xA0, EntityRoomLoader.RoomTable.INDOORS_B);
        assertDefinition(peaHat, 0x07, 0x6701,
            EntitySpriteDefinition.Shape.PAIR, 2, 0);

        EntitySpriteDefinition ghini = catalog.forEntityType(
            0x12, EntityRoomLoader.RoomTable.OVERWORLD);
        assertDefinition(ghini, 0x04, 0x5BFC,
            EntitySpriteDefinition.Shape.PAIR, 2, 0);

        EntitySpriteDefinition hardHat = catalog.forEntityType(
            0x20, EntityRoomLoader.RoomTable.INDOORS_A);
        assertDefinition(hardHat, 0x06, 0x4F2C,
            EntitySpriteDefinition.Shape.PAIR, 2, 0);

        EntitySpriteDefinition caveBHardHat = catalog.forEntityType(
            0x20, EntityRoomLoader.RoomTable.INDOORS_A, 0x0A);
        assertDefinition(caveBHardHat, 0x06, 0x4F34,
            EntitySpriteDefinition.Shape.PAIR, 2, 0);

        EntitySpriteDefinition grandpa = catalog.forEntityType(
            0x77, EntityRoomLoader.RoomTable.INDOORS_A);
        assertDefinition(grandpa, 0x06, 0x5C51,
            EntitySpriteDefinition.Shape.RECTANGLE, 2, 0);

        EntitySpriteDefinition pieceOfPower = catalog.forEntityType(
            0x33, EntityRoomLoader.RoomTable.OVERWORLD);
        assertDefinition(pieceOfPower, 0x03, 0x5B65,
            EntitySpriteDefinition.Shape.PAIR, 2, 0);

        EntitySpriteDefinition guardianAcorn = catalog.forEntityType(
            0x34, EntityRoomLoader.RoomTable.OVERWORLD);
        assertDefinition(guardianAcorn, 0x03, 0x5B5B,
            EntitySpriteDefinition.Shape.SINGLE, 1, 0);

        EntitySpriteDefinition heartPiece = catalog.forEntityType(
            0x35, EntityRoomLoader.RoomTable.OVERWORLD);
        assertDefinition(heartPiece, 0x03, 0x5A4D,
            EntitySpriteDefinition.Shape.PAIR, 1, 0);

        EntitySpriteDefinition heartContainer = catalog.forEntityType(
            0x36, EntityRoomLoader.RoomTable.OVERWORLD);
        assertDefinition(heartContainer, 0x03, 0x59D8,
            EntitySpriteDefinition.Shape.PAIR, 1, 0);

        assertDefinition(catalog.forEntityType(0x2D, EntityRoomLoader.RoomTable.OVERWORLD),
            0x03, 0x5D36, EntitySpriteDefinition.Shape.SINGLE, 1, 0);
        assertDefinition(catalog.forEntityType(0x2E, EntityRoomLoader.RoomTable.OVERWORLD),
            0x03, 0x609C, EntitySpriteDefinition.Shape.SINGLE, 1, 0);
        assertDefinition(catalog.forEntityType(0x32, EntityRoomLoader.RoomTable.OVERWORLD),
            0x03, 0x5B80, EntitySpriteDefinition.Shape.PAIR, 2, 0);
        assertDefinition(catalog.forEntityType(0x37, EntityRoomLoader.RoomTable.OVERWORLD),
            0x03, 0x6079, EntitySpriteDefinition.Shape.PAIR, 1, 0);
        assertDefinition(catalog.forEntityType(0x38, EntityRoomLoader.RoomTable.OVERWORLD),
            0x03, 0x5FC0, EntitySpriteDefinition.Shape.SINGLE, 1, 0);
        assertDefinition(catalog.forEntityType(0x3A, EntityRoomLoader.RoomTable.OVERWORLD),
            0x03, 0x5D47, EntitySpriteDefinition.Shape.PAIR, 1, 0);
        assertDefinition(catalog.forEntityType(0x3B, EntityRoomLoader.RoomTable.OVERWORLD),
            0x03, 0x6055, EntitySpriteDefinition.Shape.SINGLE, 1, 0);
        assertDefinition(catalog.forEntityType(0x3C, EntityRoomLoader.RoomTable.OVERWORLD),
            0x03, 0x5FFB, EntitySpriteDefinition.Shape.SINGLE, 1, 0);
        assertDefinition(catalog.forEntityType(0x3D, EntityRoomLoader.RoomTable.OVERWORLD),
            0x03, 0x5FD1, EntitySpriteDefinition.Shape.SINGLE, 1, 0);
    }

    @Test
    void mapsFollowerDisplayListOverridesToTheirOwnRomTables() {
        EntitySpriteHandlerCatalog catalog = new EntitySpriteHandlerCatalog(syntheticRom());

        assertDefinition(catalog.forFollowerEntityType(0x6D), 0x05, 0x401C,
            EntitySpriteDefinition.Shape.PAIR, 7, 0);
        assertDefinition(catalog.forFollowerEntityType(0xC1), 0x18, 0x59B8,
            EntitySpriteDefinition.Shape.PAIR, 11, 0);
        assertDefinition(catalog.forFollowerEntityType(0xD4), 0x19, 0x5DF8,
            EntitySpriteDefinition.Shape.PAIR, 6, 0);
        assertDefinition(catalog.forFollowerEntityType(0xD5), 0x19, 0x59BC,
            EntitySpriteDefinition.Shape.PAIR, 8, 0);
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

        EntitySpriteDefinition pieceOfPower = catalog.forEntityType(
            0x33, EntityRoomLoader.RoomTable.OVERWORLD);
        assertEquals(0x14, pieceOfPower.variant(0).first().tile());
        assertEquals(0x02, pieceOfPower.variant(0).first().attributes());
        assertEquals(0x14, pieceOfPower.variant(1).first().tile());
        assertEquals(0x14, pieceOfPower.variant(1).first().attributes());

        EntitySpriteDefinition keese = catalog.forEntityType(
            0x19, EntityRoomLoader.RoomTable.INDOORS_A);
        assertEquals(0x42, keese.variant(0).first().tile());
        assertEquals(0x20, keese.variant(0).second().attributes());
        assertEquals(0x40, keese.variant(1).first().tile());

        EntitySpriteDefinition caveBKeese = catalog.forEntityType(
            0x19, EntityRoomLoader.RoomTable.INDOORS_A, 0x0A);
        assertEquals(0x62, caveBKeese.variant(0).first().tile());
        assertEquals(0x60, caveBKeese.variant(1).first().tile());

        EntitySpriteDefinition octorok = catalog.forEntityType(
            0x09, EntityRoomLoader.RoomTable.OVERWORLD);
        assertEquals(0x30, octorok.variant(0).first().tile());
        assertEquals(0x02, octorok.variant(0).first().attributes());
        assertEquals(0x32, octorok.variant(1).first().tile());
        assertEquals(0x34, octorok.variant(4).first().tile());

        EntitySpriteDefinition moblin = catalog.forEntityType(
            0x0B, EntityRoomLoader.RoomTable.OVERWORLD);
        assertEquals(8, moblin.variantCount());
        assertEquals(0x60, moblin.variant(0).first().tile());
        assertEquals(0x03, moblin.variant(0).first().attributes());
        assertEquals(0x6A, moblin.variant(6).first().tile());
        assertEquals(0x23, moblin.variant(6).first().attributes());

        EntitySpriteDefinition armos = catalog.forEntityType(
            0x0F, EntityRoomLoader.RoomTable.OVERWORLD);
        assertEquals(0x60, armos.variant(0).first().tile());
        assertEquals(0x07, armos.variant(0).first().attributes());
        assertEquals(0x64, armos.variant(1).first().tile());

        EntitySpriteDefinition tektite = catalog.forEntityType(
            0x0D, EntityRoomLoader.RoomTable.OVERWORLD);
        assertEquals(0x58, tektite.variant(0).first().tile());
        assertEquals(0x02, tektite.variant(0).first().attributes());
        assertEquals(0x5A, tektite.variant(1).first().tile());
        assertEquals(0x22, tektite.variant(1).second().attributes());

        EntitySpriteDefinition leever = catalog.forEntityType(
            0x0E, EntityRoomLoader.RoomTable.OVERWORLD);
        assertEquals(0x56, leever.variant(0).first().tile());
        assertEquals(0x02, leever.variant(0).first().attributes());
        assertEquals(0x54, leever.variant(1).first().tile());
        assertEquals(0x52, leever.variant(2).first().tile());
        assertEquals(0x50, leever.variant(3).first().tile());
        assertEquals(0x22, leever.variant(3).second().attributes());

        EntitySpriteDefinition stalfos = catalog.forEntityType(
            0x1A, EntityRoomLoader.RoomTable.INDOORS_A);
        assertEquals(0x4A, stalfos.variant(0).first().tile());
        assertEquals(0x00, stalfos.variant(0).first().attributes());
        assertEquals(0x4C, stalfos.variant(1).first().tile());
        assertEquals(0x20, stalfos.variant(1).first().attributes());
        assertEquals(0x4E, stalfos.variant(2).first().tile());
        assertEquals(0x20, stalfos.variant(2).second().attributes());

        EntitySpriteDefinition peaHat = catalog.forEntityType(
            0xA0, EntityRoomLoader.RoomTable.INDOORS_B);
        assertEquals(0x40, peaHat.variant(0).first().tile());
        assertEquals(0x02, peaHat.variant(0).first().attributes());
        assertEquals(0x42, peaHat.variant(1).first().tile());
        assertEquals(0x22, peaHat.variant(1).second().attributes());

        EntitySpriteDefinition ghini = catalog.forEntityType(
            0x12, EntityRoomLoader.RoomTable.OVERWORLD);
        assertEquals(0x58, ghini.variant(0).first().tile());
        assertEquals(0x02, ghini.variant(0).first().attributes());
        assertEquals(0x5C, ghini.variant(1).first().tile());

        EntitySpriteDefinition hardHat = catalog.forEntityType(
            0x20, EntityRoomLoader.RoomTable.INDOORS_A);
        assertEquals(0x44, hardHat.variant(0).first().tile());
        assertEquals(0x01, hardHat.variant(0).first().attributes());
        assertEquals(0x21, hardHat.variant(0).second().attributes());
        assertEquals(0x46, hardHat.variant(1).first().tile());

        EntitySpriteDefinition caveBHardHat = catalog.forEntityType(
            0x20, EntityRoomLoader.RoomTable.INDOORS_A, 0x0A);
        assertEquals(0x64, caveBHardHat.variant(0).first().tile());
        assertEquals(0x66, caveBHardHat.variant(1).first().tile());

        EntitySpriteDefinition grandpa = catalog.forEntityType(
            0x77, EntityRoomLoader.RoomTable.INDOORS_A);
        assertEquals(-9, grandpa.rectangleVariant(0).get(0).yOffset());
        assertEquals(0, grandpa.rectangleVariant(0).get(0).xOffset());
        assertEquals(0x70, grandpa.rectangleVariant(0).get(0).oam().tile());
        assertEquals(0x78, grandpa.rectangleVariant(1).get(0).oam().tile());

        EntitySpriteDefinition guardianAcorn = catalog.forEntityType(
            0x34, EntityRoomLoader.RoomTable.OVERWORLD);
        assertEquals(0xAE, guardianAcorn.variant(0).first().tile());
        assertEquals(0x14, guardianAcorn.variant(0).first().attributes());

        EntitySpriteDefinition heartPiece = catalog.forEntityType(
            0x35, EntityRoomLoader.RoomTable.OVERWORLD);
        assertEquals(0xAC, heartPiece.variant(0).first().tile());
        assertEquals(0x02, heartPiece.variant(0).first().attributes());
        assertEquals(0x22, heartPiece.variant(0).second().attributes());

        EntitySpriteDefinition heartContainer = catalog.forEntityType(
            0x36, EntityRoomLoader.RoomTable.OVERWORLD);
        assertEquals(0xAA, heartContainer.variant(0).first().tile());
        assertEquals(0x14, heartContainer.variant(0).first().attributes());
        assertEquals(0x34, heartContainer.variant(0).second().attributes());

        EntitySpriteDefinition droppableHeart = catalog.forEntityType(
            0x2D, EntityRoomLoader.RoomTable.OVERWORLD);
        assertEquals(0xA8, droppableHeart.variant(0).first().tile());
        assertEquals(0x14, droppableHeart.variant(0).first().attributes());

        EntitySpriteDefinition droppableRupee = catalog.forEntityType(
            0x2E, EntityRoomLoader.RoomTable.OVERWORLD);
        assertEquals(0xA6, droppableRupee.variant(0).first().tile());
        assertEquals(0x15, droppableRupee.variant(0).first().attributes());

        EntitySpriteDefinition ironMask = catalog.forEntityType(
            0x32, EntityRoomLoader.RoomTable.OVERWORLD);
        assertEquals(0x74, ironMask.variant(0).first().tile());
        assertEquals(0x00, ironMask.variant(0).first().attributes());
        assertEquals(0x76, ironMask.variant(0).second().tile());
        assertEquals(0x20, ironMask.variant(1).first().attributes());

        EntitySpriteDefinition arrows = catalog.forEntityType(
            0x37, EntityRoomLoader.RoomTable.OVERWORLD);
        assertEquals(0x2A, arrows.variant(0).first().tile());
        assertEquals(0x41, arrows.variant(0).first().attributes());
        assertEquals(0x61, arrows.variant(0).second().attributes());

        EntitySpriteDefinition bombs = catalog.forEntityType(
            0x38, EntityRoomLoader.RoomTable.OVERWORLD);
        assertEquals(0x80, bombs.variant(0).first().tile());
        assertEquals(0x15, bombs.variant(0).first().attributes());

        EntitySpriteDefinition toadstool = catalog.forEntityType(
            0x3A, EntityRoomLoader.RoomTable.OVERWORLD);
        assertEquals(0x5E, toadstool.variant(0).first().tile());
        assertEquals(0x02, toadstool.variant(0).first().attributes());
        assertEquals(0x22, toadstool.variant(0).second().attributes());

        EntitySpriteDefinition powder = catalog.forEntityType(
            0x3B, EntityRoomLoader.RoomTable.OVERWORLD);
        assertEquals(0x8E, powder.variant(0).first().tile());
        assertEquals(0x16, powder.variant(0).first().attributes());

        EntitySpriteDefinition slimeKey = catalog.forEntityType(
            0x3C, EntityRoomLoader.RoomTable.OVERWORLD);
        assertEquals(0xCA, slimeKey.variant(0).first().tile());
        assertEquals(0x14, slimeKey.variant(0).first().attributes());

        EntitySpriteDefinition seashell = catalog.forEntityType(
            0x3D, EntityRoomLoader.RoomTable.OVERWORLD);
        assertEquals(0x9E, seashell.variant(0).first().tile());
        assertEquals(0x14, seashell.variant(0).first().attributes());

        EntitySpriteDefinition bowWowFollowing = catalog.forFollowerEntityType(0x6D);
        assertEquals(0x40, bowWowFollowing.variant(0).first().tile());
        assertEquals(0x40, bowWowFollowing.variant(0).second().tile());
        EntitySpriteDefinition marinFollowing = catalog.forFollowerEntityType(0xC1);
        assertEquals(0x42, marinFollowing.variant(0).first().tile());
        assertEquals(0x40, marinFollowing.variant(0).second().tile());
        EntitySpriteDefinition ghostFollowing = catalog.forFollowerEntityType(0xD4);
        assertEquals(0x42, ghostFollowing.variant(0).first().tile());
        EntitySpriteDefinition roosterFollowing = catalog.forFollowerEntityType(0xD5);
        assertEquals(0x42, roosterFollowing.variant(0).first().tile());
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
