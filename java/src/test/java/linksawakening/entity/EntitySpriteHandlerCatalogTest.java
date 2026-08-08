package linksawakening.entity;

import linksawakening.rom.RomBank;
import linksawakening.world.EntityRoomLoader;
import linksawakening.world.EntityStatus;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

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
    void hookshotUsesTheInlineHookshotChainSpriteBytes() {
        EntitySpriteDefinition hookshot = new EntitySpriteHandlerCatalog(syntheticRom())
            .forEntityType(0x03, EntityRoomLoader.RoomTable.OVERWORLD);

        assertDefinition(hookshot, -1, -1, EntitySpriteDefinition.Shape.PAIR, 1, 0);
        assertEquals(0x36, hookshot.variant(0).first().tile());
        assertEquals(0x00, hookshot.variant(0).first().attributes());
        assertEquals(0x36, hookshot.variant(0).second().tile());
        assertEquals(0x20, hookshot.variant(0).second().attributes());
    }

    @Test
    void decodesTheChestItemDisplayListAndItsTwoRoomSpecificAlternates() throws Exception {
        EntitySpriteHandlerCatalog catalog = new EntitySpriteHandlerCatalog(loadRom());

        EntitySpriteDefinition ordinary = catalog.forEntityType(
            EntitySpriteHandlerCatalog.ENTITY_CHEST_WITH_ITEM,
            EntityRoomLoader.RoomTable.INDOORS_A);
        assertDefinition(ordinary, 0x07, 0x7B57,
            EntitySpriteDefinition.Shape.PAIR, 0x21, 0);
        assertPairPrefixBytes(ordinary, new int[][] {
            {0x82, 0x15, 0x86, 0x15},
            {0x88, 0x10, 0x8A, 0x10},
            {0x8C, 0x14, 0x98, 0x16}
        });

        EntitySpriteDefinition braceletAlternate = catalog.forChestState(0x05, 0xCE, 0x00);
        assertDefinition(braceletAlternate, 0x07, 0x7B53,
            EntitySpriteDefinition.Shape.PAIR, 1, 0);
        assertPairBytes(braceletAlternate, new int[][] {{0x82, 0x17, 0x86, 0x14}});

        EntitySpriteDefinition shieldAlternate = catalog.forChestState(0x06, 0x1A, 0x01);
        assertDefinition(shieldAlternate, 0x07, 0x7B53,
            EntitySpriteDefinition.Shape.PAIR, 1, 0);
        assertPairBytes(shieldAlternate, new int[][] {{0x82, 0x17, 0x86, 0x14}});
    }

    @Test
    void mapsPlayerArrowToTheSharedBankThreeArrowDisplayList() {
        byte[] rom = syntheticRom();
        write(rom, 0x03, 0x6BC6,
            0x2E, 0x21, 0x2C, 0x21,
            0x2C, 0x01, 0x2E, 0x01,
            0x2A, 0x41, 0x2A, 0x61,
            0x2A, 0x01, 0x2A, 0x21);

        EntitySpriteDefinition arrow = new EntitySpriteHandlerCatalog(rom)
            .forEntityType(0x00, EntityRoomLoader.RoomTable.OVERWORLD);

        assertDefinition(arrow, 0x03, 0x6BC6,
            EntitySpriteDefinition.Shape.PAIR, 4, 0);
        assertPairBytes(arrow, new int[][] {
            {0x2E, 0x21, 0x2C, 0x21},
            {0x2C, 0x01, 0x2E, 0x01},
            {0x2A, 0x41, 0x2A, 0x61},
            {0x2A, 0x01, 0x2A, 0x21}
        });
    }

    @Test
    void mapsKeyDropPointToItsSixVariantBankThreeDisplayList() {
        byte[] rom = syntheticRom();
        write(rom, 0x03, 0x5C78,
            0xCA, 0x17,
            0xC0, 0x17,
            0xC2, 0x14,
            0xC4, 0x17,
            0xC6, 0x14,
            0xCA, 0x17);

        EntitySpriteDefinition key = new EntitySpriteHandlerCatalog(rom)
            .forEntityType(EntitySpriteHandlerCatalog.ENTITY_KEY_DROP_POINT,
                EntityRoomLoader.RoomTable.INDOORS_A);

        assertDefinition(key, 0x03, 0x5C78,
            EntitySpriteDefinition.Shape.SINGLE, 6, 0);
        assertEquals(0xCA, key.variant(0).first().tile());
        assertEquals(0x17, key.variant(0).first().attributes());
        assertEquals(0xC0, key.variant(1).first().tile());
        assertEquals(0x14, key.variant(2).first().attributes());
        assertEquals(0xC6, key.variant(4).first().tile());
        assertNull(key.variant(5).second());
    }

    @Test
    void mapsArmosKnightToItsFourFramesOfEightSpritesBankSixRectangleList() throws Exception {
        EntitySpriteDefinition armos = new EntitySpriteHandlerCatalog(loadRom())
            .forEntityType(0x88, EntityRoomLoader.RoomTable.INDOORS_B);

        assertDefinition(armos, 0x06, 0x5523,
            EntitySpriteDefinition.Shape.RECTANGLE, 4, 0);
        assertEquals(8, armos.rectangleVariant(0).size());
        EntitySpriteDefinition.RectangleSprite first = armos.rectangleVariant(0).get(0);
        assertEquals(-12, first.yOffset());
        assertEquals(-8, first.xOffset());
        assertEquals(0x70, first.oam().tile());
        assertEquals(0x03, first.oam().attributes());

        EntitySpriteDefinition.RectangleSprite last = armos.rectangleVariant(3).get(7);
        assertEquals(4, last.yOffset());
        assertEquals(0x10, last.xOffset());
        assertEquals(0x74, last.oam().tile());
        assertEquals(0x22, last.oam().attributes());
    }

    @Test
    void mapsMusicalNoteToItsBankFiveSingleSprite() {
        byte[] rom = syntheticRom();
        write(rom, 0x05, 0x7EF8, 0x0E, 0x13);

        EntitySpriteDefinition note = new EntitySpriteHandlerCatalog(rom)
            .forEntityType(0xC9, EntityRoomLoader.RoomTable.OVERWORLD);

        assertDefinition(note, 0x05, 0x7EF8,
            EntitySpriteDefinition.Shape.SINGLE, 1, 0);
        assertEquals(0x0E, note.variant(0).first().tile());
        assertEquals(0x13, note.variant(0).first().attributes());
        assertNull(note.variant(0).second());
    }

    @Test
    void mapsFishToItsMixedBankFifteenDisplayLists() {
        byte[] rom = syntheticRom();
        write(rom, 0x15, 0x449F,
            0xFF, 0x00, 0xFF, 0x00,
            0x54, 0x00, 0x56, 0x00,
            0x58, 0x00, 0x5A, 0x00,
            0x56, 0x20, 0x54, 0x20,
            0x5A, 0x20, 0x58, 0x20);
        write(rom, 0x15, 0x44B3,
            0x5C, 0x00, 0x5C, 0x20,
            0x5E, 0x00, 0x5E, 0x00);

        EntitySpriteDefinition fish = new EntitySpriteHandlerCatalog(rom)
            .forEntityType(0xCC, EntityRoomLoader.RoomTable.OVERWORLD);

        assertDefinition(fish, 0x15, 0x449F,
            EntitySpriteDefinition.Shape.PAIR, 9, 0);
        assertPairPrefixBytes(fish, new int[][] {
            {0xFF, 0x00, 0xFF, 0x00},
            {0x54, 0x00, 0x56, 0x00},
            {0x58, 0x00, 0x5A, 0x00},
            {0x56, 0x20, 0x54, 0x20},
            {0x5A, 0x20, 0x58, 0x20}
        });
        assertEquals(0x5C, fish.variant(5).first().tile());
        assertEquals(0x00, fish.variant(5).first().attributes());
        assertNull(fish.variant(5).second());
        assertEquals(0x5C, fish.variant(6).first().tile());
        assertEquals(0x20, fish.variant(6).first().attributes());
        assertNull(fish.variant(6).second());
        assertEquals(0x5E, fish.variant(7).first().tile());
        assertEquals(0x00, fish.variant(7).first().attributes());
        assertNull(fish.variant(7).second());
        assertEquals(0x5E, fish.variant(8).first().tile());
        assertEquals(0x00, fish.variant(8).first().attributes());
        assertNull(fish.variant(8).second());
    }

    @Test
    void mapsCuccoToItsFourVariantBankFivePairDisplayList() throws Exception {
        EntitySpriteDefinition cucco = new EntitySpriteHandlerCatalog(loadRom())
            .forEntityType(EntitySpriteHandlerCatalog.ENTITY_CUCCO,
                EntityRoomLoader.RoomTable.OVERWORLD);

        assertDefinition(cucco, 0x05, 0x4514,
            EntitySpriteDefinition.Shape.PAIR, 4, 0);
        assertPairBytes(cucco, new int[][] {
            {0x50, 0x01, 0x52, 0x01},
            {0x54, 0x01, 0x56, 0x01},
            {0x52, 0x21, 0x50, 0x21},
            {0x56, 0x21, 0x54, 0x21}
        });
    }

    @Test
    void mapsMimicToTheEightVariantBankNineteenListIncludingTheCrossLineByte()
        throws Exception {
        EntitySpriteDefinition mimic = new EntitySpriteHandlerCatalog(loadRom())
            .forEntityType(EntitySpriteHandlerCatalog.ENTITY_MIMIC,
                EntityRoomLoader.RoomTable.INDOORS_B);

        assertDefinition(mimic, 0x19, 0x6A8D,
            EntitySpriteDefinition.Shape.PAIR, 8, 0);
        assertPairBytes(mimic, new int[][] {
            {0x60, 0x01, 0x62, 0x01},
            {0x62, 0x21, 0x60, 0x21},
            {0x64, 0x01, 0x66, 0x01},
            {0x66, 0x21, 0x64, 0x21},
            {0x68, 0x01, 0x6A, 0x01},
            {0x6C, 0x01, 0x6E, 0x01},
            {0x6A, 0x21, 0x68, 0x21},
            {0x6E, 0x21, 0x6C, 0x21}
        });
    }

    @Test
    void mapsMaskedMimicGoriyaToItsBankNineteenEightVariantPairList() throws Exception {
        EntitySpriteDefinition maskedMimic = new EntitySpriteHandlerCatalog(loadRom())
            .forEntityType(0x8F, EntityRoomLoader.RoomTable.INDOORS_A);

        assertDefinition(maskedMimic, 0x19, 0x4796,
            EntitySpriteDefinition.Shape.PAIR, 8, 0);
        assertPairBytes(maskedMimic, new int[][] {
            {0x6A, 0x22, 0x68, 0x22},
            {0x6E, 0x22, 0x6C, 0x22},
            {0x68, 0x02, 0x6A, 0x02},
            {0x6C, 0x02, 0x6E, 0x02},
            {0x64, 0x02, 0x66, 0x02},
            {0x66, 0x22, 0x64, 0x22},
            {0x60, 0x02, 0x62, 0x02},
            {0x62, 0x22, 0x60, 0x22}
        });
    }

    @Test
    void mapsBoomerangToItsFourFrameBankNineteenDisplayList() {
        byte[] rom = syntheticRom();
        write(rom, 0x19, 0x4451,
            0x38, 0x14, 0x38, 0x34,
            0xA4, 0x14, 0xFF, 0xFF,
            0x38, 0x54, 0x38, 0x74,
            0xFF, 0xFF, 0xA4, 0x34);

        EntitySpriteDefinition boomerang = new EntitySpriteHandlerCatalog(rom)
            .forEntityType(0x01, EntityRoomLoader.RoomTable.OVERWORLD);

        assertDefinition(boomerang, 0x19, 0x4451,
            EntitySpriteDefinition.Shape.PAIR, 4, 0);
        assertPairBytes(boomerang, new int[][] {
            {0x38, 0x14, 0x38, 0x34},
            {0xA4, 0x14, 0xFF, 0xFF},
            {0x38, 0x54, 0x38, 0x74},
            {0xFF, 0xFF, 0xA4, 0x34}
        });
    }

    @Test
    void mapsSwordBeamToItsFourFrameBankNineteenRectangleDisplayList() {
        byte[] rom = syntheticRom();
        write(rom, 0x19, 0x44FC,
            0x00, 0x00, 0x08, 0x20, 0x00, 0x08, 0x06, 0x20,
            0x00, 0x00, 0x06, 0x00, 0x00, 0x08, 0x08, 0x00,
            0x00, 0x04, 0x04, 0x40, 0xFF, 0xFF, 0xFF, 0xFF,
            0x00, 0x04, 0x04, 0x00, 0xFF, 0xFF, 0xFF, 0xFF);

        EntitySpriteDefinition beam = new EntitySpriteHandlerCatalog(rom)
            .forEntityType(0xDF, EntityRoomLoader.RoomTable.OVERWORLD);

        assertDefinition(beam, 0x19, 0x44FC,
            EntitySpriteDefinition.Shape.RECTANGLE, 4, 0);
        assertEquals(2, beam.rectangleVariant(0).size());
        assertEquals(0x08, beam.rectangleVariant(0).get(0).oam().tile());
        assertEquals(0x20, beam.rectangleVariant(0).get(0).oam().attributes());
        assertEquals(-1, beam.rectangleVariant(2).get(1).xOffset());
        assertEquals(0xFF, beam.rectangleVariant(2).get(1).oam().tile());
    }

    @Test
    void mapsMagicRodFireballAndItsSharedFireStateToRomDisplayLists() {
        byte[] rom = syntheticRom();
        write(rom, 0x03, 0x69AA,
            0x36, 0x02, 0x36, 0x22,
            0x36, 0x03, 0x36, 0x23);
        write(rom, 0x03, 0x4C44,
            0x34, 0x02, 0x34, 0x22,
            0x34, 0x04, 0x34, 0x24);

        EntitySpriteHandlerCatalog catalog = new EntitySpriteHandlerCatalog(rom);
        EntitySpriteDefinition fireball = catalog.forEntityType(
            0x04, EntityRoomLoader.RoomTable.OVERWORLD);
        EntitySpriteDefinition fire = catalog.forMagicRodFireState();

        assertDefinition(fireball, 0x03, 0x69AA,
            EntitySpriteDefinition.Shape.PAIR, 2, 0);
        assertPairBytes(fireball, new int[][] {
            {0x36, 0x02, 0x36, 0x22},
            {0x36, 0x03, 0x36, 0x23}
        });
        assertDefinition(fire, 0x03, 0x4C44,
            EntitySpriteDefinition.Shape.PAIR, 2, 0);
        assertPairBytes(fire, new int[][] {
            {0x34, 0x02, 0x34, 0x22},
            {0x34, 0x04, 0x34, 0x24}
        });
    }

    @Test
    void mapsMagicPowderSprinkleAndTorchTransitionListsToRomData() {
        byte[] rom = syntheticRom();
        write(rom, 0x18, 0x7ABA,
            0x06, 0xFE, 0x24, 0x03, 0x03, 0x04, 0x24, 0x13,
            0x05, 0x0A, 0x24, 0x03, 0x05, 0xFE, 0x24, 0x13,
            0x02, 0x04, 0x24, 0x03, 0x04, 0x0A, 0x24, 0x13,
            0x03, 0xFF, 0x24, 0x03, 0x01, 0x04, 0x24, 0x13,
            0x02, 0x09, 0x24, 0x03, 0x01, 0x00, 0x24, 0x13,
            0xFF, 0x04, 0x24, 0x03, 0x00, 0x06, 0x24, 0x13,
            0x00, 0x01, 0x24, 0x03, 0xFE, 0x03, 0x24, 0x13,
            0xFF, 0x05, 0x24, 0x03, 0xFD, 0x03, 0x24, 0x13);
        write(rom, 0x18, 0x795E, 0x6C, 0x74, 0x6D, 0x75);
        write(rom, 0x18, 0x7962, 0x64, 0x74, 0x65, 0x75);

        EntitySpriteHandlerCatalog catalog = new EntitySpriteHandlerCatalog(rom);
        EntitySpriteDefinition sprinkle = catalog.forEntityType(
            0x08, EntityRoomLoader.RoomTable.OVERWORLD);
        EntitySpriteDefinition ignite = catalog.forMagicPowderTorchState(true);
        EntitySpriteDefinition expire = catalog.forMagicPowderTorchState(false);

        assertDefinition(sprinkle, 0x18, 0x7ABA,
            EntitySpriteDefinition.Shape.RECTANGLE, 8, 0);
        assertEquals(3, sprinkle.rectangleVariant(0).size());
        assertEquals(6, sprinkle.rectangleVariant(0).get(0).yOffset());
        assertEquals(0x24, sprinkle.rectangleVariant(0).get(0).oam().tile());
        assertEquals(0x03, sprinkle.rectangleVariant(0).get(2).oam().attributes());
        assertDefinition(ignite, 0x18, 0x795E,
            EntitySpriteDefinition.Shape.PAIR, 1, 0);
        assertPairBytes(ignite, new int[][] {{0x6C, 0x74, 0x6D, 0x75}});
        assertDefinition(expire, 0x18, 0x7962,
            EntitySpriteDefinition.Shape.PAIR, 1, 0);
        assertPairBytes(expire, new int[][] {{0x64, 0x74, 0x65, 0x75}});
    }

    @Test
    void decodesSpikedBeetleDisplayListsFromTheShippedRom() throws Exception {
        EntitySpriteHandlerCatalog catalog = new EntitySpriteHandlerCatalog(loadRom());

        EntitySpriteDefinition ordinary = catalog.forEntityType(
            0x2C, EntityRoomLoader.RoomTable.INDOORS_A, 0x00);
        assertDefinition(ordinary, 0x07, 0x7784,
            EntitySpriteDefinition.Shape.PAIR, 4, 0);
        assertPairBytes(ordinary, new int[][] {
            {0x70, 0x00, 0x70, 0x20},
            {0x72, 0x00, 0x72, 0x20},
            {0x74, 0x00, 0x74, 0x20},
            {0x76, 0x00, 0x76, 0x20}
        });

        EntitySpriteDefinition anglersTunnel = catalog.forEntityType(
            0x2C, EntityRoomLoader.RoomTable.INDOORS_A, 0x03);
        assertDefinition(anglersTunnel, 0x07, 0x7794,
            EntitySpriteDefinition.Shape.PAIR, 4, 0);
        assertPairBytes(anglersTunnel, new int[][] {
            {0x60, 0x00, 0x60, 0x20},
            {0x62, 0x00, 0x62, 0x20},
            {0x64, 0x00, 0x64, 0x20},
            {0x66, 0x00, 0x66, 0x20}
        });
    }

    @Test
    void decodesPolsVoiceDisplayListFromTheShippedRom() throws Exception {
        EntitySpriteDefinition polsVoice = new EntitySpriteHandlerCatalog(loadRom())
            .forEntityType(0x18, EntityRoomLoader.RoomTable.INDOORS_A);

        assertDefinition(polsVoice, 0x06, 0x7373,
            EntitySpriteDefinition.Shape.PAIR, 2, 0);
        assertPairBytes(polsVoice, new int[][] {
            {0x70, 0x01, 0x70, 0x21},
            {0x72, 0x01, 0x72, 0x21}
        });
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
    void decodesLiftableRockIntactAndSmashDisplayListsFromTheRom() throws Exception {
        EntitySpriteHandlerCatalog catalog = new EntitySpriteHandlerCatalog(loadRom());

        EntitySpriteDefinition outdoor = catalog.forEntityType(
            EntitySpriteHandlerCatalog.ENTITY_LIFTABLE_ROCK,
            EntityRoomLoader.RoomTable.OVERWORLD);
        assertDefinition(outdoor, 0x19, 0x7B50,
            EntitySpriteDefinition.Shape.DYNAMIC, 22, 0);
        assertEquals(0xF0, outdoor.dynamicVariant(0).get(0).oam().tile());
        assertEquals(0xF4, outdoor.dynamicVariant(1).get(0).oam().tile());
        assertEquals(-3, outdoor.dynamicVariant(2).get(0).yOffset());
        assertEquals(-4, outdoor.dynamicVariant(2).get(0).xOffset());
        assertEquals(0x28, outdoor.dynamicVariant(6).get(0).oam().tile());
        assertEquals(0x00, outdoor.dynamicVariant(6).get(0).oam().attributes());
        assertEquals(0x06, outdoor.dynamicVariant(14).get(0).oam().attributes());

        EntitySpriteDefinition indoor = catalog.forEntityType(
            EntitySpriteHandlerCatalog.ENTITY_LIFTABLE_ROCK,
            EntityRoomLoader.RoomTable.INDOORS_A);
        assertEquals(0xF0, indoor.dynamicVariant(0).get(0).oam().tile());
        assertEquals(0x16, indoor.dynamicVariant(0).get(0).oam().attributes());
    }

    @Test
    void mapsSupportedHandlersToDisassemblyBanksAddressesAndInitialVariants() {
        EntitySpriteHandlerCatalog catalog = new EntitySpriteHandlerCatalog(syntheticRom());

        EntitySpriteDefinition crow = catalog.forEntityType(
            0x7A, EntityRoomLoader.RoomTable.OVERWORLD);
        assertDefinition(crow, 0x06, 0x5C89, EntitySpriteDefinition.Shape.PAIR, 4, 2);

        EntitySpriteDefinition booBuddy = catalog.forEntityType(
            0x50, EntityRoomLoader.RoomTable.INDOORS_A);
        assertDefinition(booBuddy, 0x06, 0x79A9,
            EntitySpriteDefinition.Shape.PAIR, 8, 0);

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

        EntitySpriteDefinition goomba = catalog.forEntityType(
            0x9F, EntityRoomLoader.RoomTable.INDOORS_A);
        assertDefinition(goomba, 0x07, 0x65CE,
            EntitySpriteDefinition.Shape.PAIR, 3, 0);

        EntitySpriteDefinition snake = catalog.forEntityType(
            0xA1, EntityRoomLoader.RoomTable.INDOORS_A);
        assertDefinition(snake, 0x07, 0x683E,
            EntitySpriteDefinition.Shape.PAIR, 4, 0);

        EntitySpriteDefinition wizrobe = catalog.forEntityType(
            0x21, EntityRoomLoader.RoomTable.INDOORS_A);
        assertDefinition(wizrobe, 0x06, 0x7604,
            EntitySpriteDefinition.Shape.PAIR, 5, 0);

        EntitySpriteDefinition wizrobeProjectile = catalog.forEntityType(
            0x22, EntityRoomLoader.RoomTable.INDOORS_A);
        assertDefinition(wizrobeProjectile, 0x06, 0x65E1,
            EntitySpriteDefinition.Shape.PAIR, 4, 0);

        EntitySpriteDefinition octorok = catalog.forEntityType(
            0x09, EntityRoomLoader.RoomTable.OVERWORLD);
        assertDefinition(octorok, 0x03, 0x57FB,
            EntitySpriteDefinition.Shape.PAIR, 8, 0);

        EntitySpriteDefinition maskedIronMask = catalog.forIronMaskState(0);
        assertDefinition(maskedIronMask, 0x03, 0x4FCB,
            EntitySpriteDefinition.Shape.PAIR, 8, 0);

        EntitySpriteDefinition unmaskedIronMask = catalog.forIronMaskState(1);
        assertDefinition(unmaskedIronMask, 0x03, 0x4FEB,
            EntitySpriteDefinition.Shape.PAIR, 2, 0);

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

        EntitySpriteDefinition evasiveStalfos = catalog.forEntityType(
            0x1E, EntityRoomLoader.RoomTable.INDOORS_A);
        assertDefinition(evasiveStalfos, 0x15, 0x4E7D,
            EntitySpriteDefinition.Shape.PAIR, 3, 0);

        EntitySpriteDefinition antiFairy = catalog.forEntityType(
            0x15, EntityRoomLoader.RoomTable.INDOORS_B);
        assertDefinition(antiFairy, 0x06, 0x786E,
            EntitySpriteDefinition.Shape.PAIR, 2, 0);

        EntitySpriteDefinition sparkCounterClockwise = catalog.forEntityType(
            0x16, EntityRoomLoader.RoomTable.INDOORS_B);
        assertDefinition(sparkCounterClockwise, 0x06, 0x6615,
            EntitySpriteDefinition.Shape.PAIR, 2, 0);

        EntitySpriteDefinition sparkClockwise = catalog.forEntityType(
            0x17, EntityRoomLoader.RoomTable.INDOORS_B);
        assertDefinition(sparkClockwise, 0x06, 0x6615,
            EntitySpriteDefinition.Shape.PAIR, 2, 0);

        EntitySpriteDefinition zol = catalog.forEntityType(
            0x1B, EntityRoomLoader.RoomTable.INDOORS_A);
        assertDefinition(zol, 0x06, 0x7C09,
            EntitySpriteDefinition.Shape.PAIR, 2, 0);

        EntitySpriteDefinition gel = catalog.forEntityType(
            0x1C, EntityRoomLoader.RoomTable.INDOORS_A);
        assertDefinition(gel, 0x06, 0x7BFA,
            EntitySpriteDefinition.Shape.SINGLE, 2, 0);

        EntitySpriteDefinition hidingZol = catalog.forEntityType(
            0x9B, EntityRoomLoader.RoomTable.OVERWORLD);
        assertDefinition(hidingZol, 0x07, 0x729B,
            EntitySpriteDefinition.Shape.PAIR, 4, 0);
        assertNull(hidingZol.variant(1).second());
        assertEquals(0x00, hidingZol.variant(1).first().tile());

        EntitySpriteDefinition star = catalog.forEntityType(
            EntitySpriteHandlerCatalog.ENTITY_STAR, EntityRoomLoader.RoomTable.INDOORS_A);
        assertDefinition(star, 0x07, 0x7247,
            EntitySpriteDefinition.Shape.PAIR, 5, 0);

        EntitySpriteDefinition gibdo = catalog.forEntityType(
            0x1F, EntityRoomLoader.RoomTable.INDOORS_B);
        assertDefinition(gibdo, 0x06, 0x7E6F,
            EntitySpriteDefinition.Shape.PAIR, 2, 0);

        EntitySpriteDefinition turtleRockGibdo = catalog.forEntityType(
            0x1F, EntityRoomLoader.RoomTable.INDOORS_B, 0x07);
        assertDefinition(turtleRockGibdo, 0x06, 0x7E77,
            EntitySpriteDefinition.Shape.PAIR, 2, 0);

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

        EntitySpriteDefinition spikeTrap = catalog.forEntityType(
            0x27, EntityRoomLoader.RoomTable.INDOORS_A);
        assertDefinition(spikeTrap, 0x06, 0x74FA,
            EntitySpriteDefinition.Shape.PAIR, 1, 0);

        EntitySpriteDefinition waterTektite = catalog.forEntityType(
            0x99, EntityRoomLoader.RoomTable.INDOORS_A);
        assertDefinition(waterTektite, 0x07, 0x752D,
            EntitySpriteDefinition.Shape.PAIR, 2, 0);

        EntitySpriteDefinition pairodd = catalog.forEntityType(
            0x57, EntityRoomLoader.RoomTable.INDOORS_A);
        assertDefinition(pairodd, 0x04, 0x5DD1,
            EntitySpriteDefinition.Shape.PAIR, 8, 0);

        EntitySpriteDefinition pairoddProjectile = catalog.forEntityType(
            0x58, EntityRoomLoader.RoomTable.INDOORS_A);
        assertDefinition(pairoddProjectile, 0x04, 0x5EF4,
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
    void wizrobeDisplayListsMatchTheShippedRomBytes() throws Exception {
        EntitySpriteHandlerCatalog catalog = new EntitySpriteHandlerCatalog(loadRom());

        EntitySpriteDefinition wizrobe = catalog.forEntityType(
            0x21, EntityRoomLoader.RoomTable.INDOORS_A);
        assertPairBytes(wizrobe, new int[][] {
            {0x6E, 0x00, 0x6E, 0x20},
            {0x66, 0x20, 0x64, 0x20},
            {0x64, 0x00, 0x66, 0x00},
            {0x62, 0x00, 0x62, 0x20},
            {0x60, 0x00, 0x60, 0x20}
        });

        EntitySpriteDefinition projectile = catalog.forEntityType(
            0x22, EntityRoomLoader.RoomTable.INDOORS_A);
        assertPairBytes(projectile, new int[][] {
            {0x6A, 0x23, 0x68, 0x23},
            {0x68, 0x03, 0x6A, 0x03},
            {0x6C, 0x43, 0x6C, 0x63},
            {0x6C, 0x03, 0x6C, 0x23}
        });
    }

    @Test
    void mapsGoombaToItsExactBankSevenDisplayList() throws Exception {
        EntitySpriteDefinition goomba = new EntitySpriteHandlerCatalog(loadRom())
            .forEntityType(0x9F, EntityRoomLoader.RoomTable.INDOORS_A);
        assertPairBytes(goomba, new int[][] {
            {0x4A, 0x02, 0x4C, 0x02},
            {0x4C, 0x22, 0x4A, 0x22},
            {0x4E, 0x02, 0x4E, 0x22}
        });
    }

    @Test
    void mapsSnakeToItsExactBankSevenDisplayList() throws Exception {
        EntitySpriteDefinition snake = new EntitySpriteHandlerCatalog(loadRom())
            .forEntityType(0xA1, EntityRoomLoader.RoomTable.INDOORS_A);
        assertPairBytes(snake, new int[][] {
            {0x44, 0x03, 0x46, 0x03},
            {0x44, 0x03, 0x48, 0x03},
            {0x46, 0x23, 0x44, 0x23},
            {0x48, 0x23, 0x44, 0x23}
        });
    }

    @Test
    void mapsLikeLikeToItsExactBankSixDisplayList() throws Exception {
        EntitySpriteDefinition likeLike = new EntitySpriteHandlerCatalog(loadRom())
            .forEntityType(0x23, EntityRoomLoader.RoomTable.INDOORS_A);
        assertDefinition(likeLike, 0x06, 0x7DD4,
            EntitySpriteDefinition.Shape.PAIR, 2, 0);
        assertPairBytes(likeLike, new int[][] {
            {0x7C, 0x01, 0x7C, 0x21},
            {0x7E, 0x01, 0x7E, 0x21}
        });
    }

    @Test
    void mapsTheLikeLikeShieldDropToItsExactBankThreeSprite() throws Exception {
        EntitySpriteDefinition shield = new EntitySpriteHandlerCatalog(loadRom())
            .forEntityType(0x31, EntityRoomLoader.RoomTable.INDOORS_A);
        assertDefinition(shield, 0x03, 0x5B95,
            EntitySpriteDefinition.Shape.SINGLE, 1, 0);
        assertEquals(0x86, shield.variant(0).first().tile());
        assertEquals(0x17, shield.variant(0).first().attributes());
        assertNull(shield.variant(0).second());
    }

    @Test
    void mapsBomberToItsBankEighteenRectangleDisplayList() throws Exception {
        EntitySpriteHandlerCatalog catalog = new EntitySpriteHandlerCatalog(loadRom());

        EntitySpriteDefinition bomber = catalog.forEntityType(
            0xBA, EntityRoomLoader.RoomTable.OVERWORLD);

        assertDefinition(bomber, 0x18, 0x77ED,
            EntitySpriteDefinition.Shape.RECTANGLE, 4, 0);
        assertRectangleBytes(bomber, new int[][][] {
            {{0, -4, 0x70, 0x02}, {0, 4, 0x72, 0x02}, {0, 12, 0x70, 0x22}},
            {{0, -4, 0x74, 0x02}, {0, 4, 0x72, 0x02}, {0, 12, 0x74, 0x22}},
            {{0, -4, 0x76, 0x02}, {0, 4, 0x72, 0x02}, {0, 12, 0x76, 0x22}},
            {{0, -4, 0x74, 0x02}, {0, 4, 0x72, 0x02}, {0, 12, 0x74, 0x22}}
        });
    }

    @Test
    void mapsTheGhiniFamilyToExactShippedRomDisplayLists() throws Exception {
        EntitySpriteHandlerCatalog catalog = new EntitySpriteHandlerCatalog(loadRom());

        for (int entityType : new int[] {0x10, 0x12}) {
            EntitySpriteDefinition ghini = catalog.forEntityType(
                entityType, EntityRoomLoader.RoomTable.OVERWORLD);
            assertDefinition(ghini, 0x04, 0x5BFC,
                EntitySpriteDefinition.Shape.PAIR, 2, 0);
            assertPairBytes(ghini, new int[][] {
                {0x58, 0x02, 0x5A, 0x02},
                {0x5C, 0x02, 0x5E, 0x02}
            });
        }

        EntitySpriteDefinition giant = catalog.forEntityType(
            0x11, EntityRoomLoader.RoomTable.OVERWORLD);
        assertDefinition(giant, 0x04, 0x5D26,
            EntitySpriteDefinition.Shape.RECTANGLE, 4, 0);
        int[][][] expectedGiant = {
            {
                {-8, -8, 0x60, 0x02}, {-8, 0, 0x62, 0x02},
                {-8, 8, 0x62, 0x22}, {-8, 16, 0x60, 0x22},
                {8, -8, 0x64, 0x02}, {8, 0, 0x66, 0x02},
                {8, 8, 0x68, 0x02}, {8, 16, 0x6A, 0x02}
            },
            {
                {-8, -8, 0x60, 0x02}, {-8, 0, 0x62, 0x02},
                {-8, 8, 0x62, 0x22}, {-8, 16, 0x60, 0x22},
                {8, -8, 0x64, 0x02}, {8, 0, 0x6C, 0x02},
                {8, 8, 0x6E, 0x02}, {8, 16, 0x6A, 0x02}
            },
            {
                {-8, -8, 0x60, 0x02}, {-8, 0, 0x62, 0x02},
                {-8, 8, 0x62, 0x22}, {-8, 16, 0x60, 0x22},
                {8, -8, 0x6A, 0x22}, {8, 0, 0x68, 0x22},
                {8, 8, 0x66, 0x22}, {8, 16, 0x64, 0x22}
            },
            {
                {-8, -8, 0x60, 0x02}, {-8, 0, 0x62, 0x02},
                {-8, 8, 0x62, 0x22}, {-8, 16, 0x60, 0x22},
                {8, -8, 0x6A, 0x22}, {8, 0, 0x6E, 0x22},
                {8, 8, 0x6C, 0x22}, {8, 16, 0x64, 0x22}
            }
        };
        for (int variant = 0; variant < expectedGiant.length; variant++) {
            assertEquals(8, giant.rectangleVariant(variant).size());
            for (int spriteIndex = 0; spriteIndex < expectedGiant[variant].length;
                 spriteIndex++) {
                int[] expected = expectedGiant[variant][spriteIndex];
                EntitySpriteDefinition.RectangleSprite actual = giant
                    .rectangleVariant(variant).get(spriteIndex);
                assertEquals(expected[0], actual.yOffset());
                assertEquals(expected[1], actual.xOffset());
                assertEquals(expected[2], actual.oam().tile());
                assertEquals(expected[3], actual.oam().attributes());
            }
        }
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
    void moblinSwordDynamicDefinitionMirrorsItsRomOamConstruction() throws Exception {
        EntitySpriteDefinition sword = new EntitySpriteHandlerCatalog(loadRom())
            .forEntityType(0x14, EntityRoomLoader.RoomTable.OVERWORLD);

        assertDefinition(sword, 0x07, 0x7A95,
            EntitySpriteDefinition.Shape.DYNAMIC, 8, 0);
        assertEquals(5, sword.dynamicVariant(0).size());

        EntitySpriteDefinition.DynamicSprite warning = sword.dynamicVariant(0).get(0);
        assertEquals(5, warning.yOffset());
        assertEquals(-2, warning.xOffset());
        assertEquals(0x86, warning.oam().tile());
        assertEquals(0x16, warning.oam().attributes());
        assertEquals(EntitySpriteDefinition.DynamicSprite.TileSource.GPU,
            warning.tileSource());
        assertFalse(warning.appliesEntityFlipAttribute());

        EntitySpriteDefinition.DynamicSprite hiddenSword = sword.dynamicVariant(0).get(1);
        assertEquals(8, hiddenSword.yOffset());
        assertEquals(0, hiddenSword.xOffset());
        assertEquals(0xF0, hiddenSword.oam().tile());
        assertEquals(0x03, hiddenSword.oam().attributes());
        assertEquals(EntitySpriteDefinition.DynamicSprite.TileSource.GPU,
            hiddenSword.tileSource());

        EntitySpriteDefinition.DynamicSprite swordTip = sword.dynamicVariant(0).get(2);
        assertEquals(8, swordTip.yOffset());
        assertEquals(8, swordTip.xOffset());
        assertEquals(0x04, swordTip.oam().tile());
        assertEquals(0x03, swordTip.oam().attributes());

        EntitySpriteDefinition.DynamicSprite body = sword.dynamicVariant(0).get(3);
        assertEquals(0, body.yOffset());
        assertEquals(0, body.xOffset());
        assertEquals(0x60, body.oam().tile());
        assertEquals(0x03, body.oam().attributes());
        assertEquals(EntitySpriteDefinition.DynamicSprite.TileSource.ENTITY_SHEETS,
            body.tileSource());
        assertTrue(body.appliesEntityFlipAttribute());

        assertEquals(4, sword.dynamicVariant(2).size());
        assertEquals(0x64, sword.dynamicVariant(2).get(0).oam().tile());
        assertEquals(-8, sword.dynamicVariant(2).get(2).yOffset());
        assertEquals(-7, sword.dynamicVariant(2).get(2).xOffset());
        assertEquals(0xF0, sword.dynamicVariant(2).get(2).oam().tile());
    }

    @Test
    void mapsMiniMoldormToItsBankFourPairDisplayList() throws Exception {
        EntitySpriteHandlerCatalog catalog = new EntitySpriteHandlerCatalog(loadRom());

        EntitySpriteDefinition definition = catalog.forEntityType(
            EntitySpriteHandlerCatalog.ENTITY_MINI_MOLDORM,
            EntityRoomLoader.RoomTable.INDOORS_A);

        assertDefinition(definition, 0x04, 0x5A49,
            EntitySpriteDefinition.Shape.PAIR, 10, 0);
        assertPairBytes(definition, new int[][] {
            {0x70, 0x00, 0x70, 0x20},
            {0x70, 0x40, 0x70, 0x60},
            {0x72, 0x00, 0x74, 0x00},
            {0x74, 0x20, 0x72, 0x20},
            {0x76, 0x00, 0x78, 0x00},
            {0x78, 0x20, 0x76, 0x20},
            {0x76, 0x40, 0x78, 0x40},
            {0x78, 0x60, 0x76, 0x60},
            {0x7A, 0x00, 0x7A, 0x20},
            {0x7C, 0x00, 0x7C, 0x20}
        });
    }

    @Test
    void buildsMiniMoldormHeadAndDelayedTailAsOneDynamicDisplayList() throws Exception {
        EntitySpriteHandlerCatalog catalog = new EntitySpriteHandlerCatalog(loadRom());

        EntitySpriteDefinition definition = catalog.forMiniMoldormState(
            0x03, 0x50, 0x60, 0x48, 0x58, 0x40, 0x50);

        assertDefinition(definition, 0x04, 0x5A49,
            EntitySpriteDefinition.Shape.DYNAMIC, 1, 0);
        assertEquals(6, definition.dynamicVariant(0).size());

        EntitySpriteDefinition.DynamicSprite head = definition.dynamicVariant(0).get(0);
        assertEquals(0, head.yOffset());
        assertEquals(0, head.xOffset());
        assertEquals(0x74, head.oam().tile());
        assertEquals(0x20, head.oam().attributes());
        assertTrue(head.appliesEntityFlipAttribute());

        EntitySpriteDefinition.DynamicSprite firstTail = definition.dynamicVariant(0).get(2);
        assertEquals(-8, firstTail.yOffset());
        assertEquals(-8, firstTail.xOffset());
        assertEquals(0x7A, firstTail.oam().tile());
        assertEquals(EntitySpriteDefinition.DynamicSprite.TileSource.ENTITY_SHEETS,
            firstTail.tileSource());

        EntitySpriteDefinition.DynamicSprite secondTail = definition.dynamicVariant(0).get(4);
        assertEquals(-16, secondTail.yOffset());
        assertEquals(-16, secondTail.xOffset());
        assertEquals(0x7C, secondTail.oam().tile());
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

        EntitySpriteDefinition maskedIronMask = catalog.forIronMaskState(0);
        assertEquals(0x60, maskedIronMask.variant(0).first().tile());
        assertEquals(0x02, maskedIronMask.variant(0).first().attributes());
        assertEquals(0x6E, maskedIronMask.variant(7).first().tile());
        assertEquals(0x22, maskedIronMask.variant(7).first().attributes());

        EntitySpriteDefinition unmaskedIronMask = catalog.forIronMaskState(1);
        assertEquals(0x70, unmaskedIronMask.variant(0).first().tile());
        assertEquals(0x02, unmaskedIronMask.variant(0).first().attributes());
        assertEquals(0x70, unmaskedIronMask.variant(1).second().tile());
        assertEquals(0x22, unmaskedIronMask.variant(1).second().attributes());

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

        EntitySpriteDefinition waterTektite = catalog.forEntityType(
            0x99, EntityRoomLoader.RoomTable.INDOORS_A);
        assertEquals(0x70, waterTektite.variant(0).first().tile());
        assertEquals(0x00, waterTektite.variant(0).first().attributes());
        assertEquals(0x70, waterTektite.variant(0).second().tile());
        assertEquals(0x20, waterTektite.variant(0).second().attributes());
        assertEquals(0x72, waterTektite.variant(1).first().tile());
        assertEquals(0x00, waterTektite.variant(1).first().attributes());
        assertEquals(0x72, waterTektite.variant(1).second().tile());
        assertEquals(0x20, waterTektite.variant(1).second().attributes());

        EntitySpriteDefinition pairodd = catalog.forEntityType(
            0x57, EntityRoomLoader.RoomTable.INDOORS_A);
        int[] pairoddTiles = {0x70, 0x72, 0x72, 0x70, 0x74, 0x74, 0x00, 0x00,
            0x7A, 0x7A, 0xFF, 0xFF, 0x76, 0x78, 0x78, 0x76};
        int[] pairoddAttributes = {0x01, 0x01, 0x21, 0x21, 0x01, 0x21, 0x00, 0x00,
            0x01, 0x21, 0x00, 0x00, 0x01, 0x01, 0x21, 0x21};
        for (int variant = 0; variant < pairodd.variantCount(); variant++) {
            EntitySpriteDefinition.Variant pair = pairodd.variant(variant);
            assertEquals(pairoddTiles[variant * 2], pair.first().tile());
            assertEquals(pairoddAttributes[variant * 2], pair.first().attributes());
            assertEquals(pairoddTiles[variant * 2 + 1], pair.second().tile());
            assertEquals(pairoddAttributes[variant * 2 + 1], pair.second().attributes());
        }

        EntitySpriteDefinition pairoddProjectile = catalog.forEntityType(
            0x58, EntityRoomLoader.RoomTable.INDOORS_A);
        assertEquals(0x7C, pairoddProjectile.variant(0).first().tile());
        assertEquals(0x00, pairoddProjectile.variant(0).first().attributes());
        assertEquals(0x7C, pairoddProjectile.variant(0).second().tile());
        assertEquals(0x20, pairoddProjectile.variant(0).second().attributes());
        assertEquals(0x7E, pairoddProjectile.variant(1).first().tile());
        assertEquals(0x00, pairoddProjectile.variant(1).first().attributes());
        assertEquals(0x7E, pairoddProjectile.variant(1).second().tile());
        assertEquals(0x20, pairoddProjectile.variant(1).second().attributes());

        EntitySpriteDefinition star = catalog.forEntityType(
            EntitySpriteHandlerCatalog.ENTITY_STAR, EntityRoomLoader.RoomTable.INDOORS_A);
        assertDefinition(star, 0x07, 0x7247,
            EntitySpriteDefinition.Shape.PAIR, 5, 0);
        assertPairBytes(star, new int[][] {
            {0x74, 0x01, 0x74, 0x21},
            {0x76, 0x01, 0x78, 0x01},
            {0x7A, 0x01, 0x7A, 0x21},
            {0x78, 0x21, 0x76, 0x21},
            {0x7C, 0x01, 0x7C, 0x01}
        });

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

        EntitySpriteDefinition antiFairy = catalog.forEntityType(
            0x15, EntityRoomLoader.RoomTable.INDOORS_B);
        assertEquals(0x5A, antiFairy.variant(0).first().tile());
        assertEquals(0x00, antiFairy.variant(0).first().attributes());
        assertEquals(0x20, antiFairy.variant(0).second().attributes());
        assertEquals(0x14, antiFairy.variant(1).first().attributes());
        assertEquals(0x34, antiFairy.variant(1).second().attributes());

        EntitySpriteDefinition spark = catalog.forEntityType(
            0x16, EntityRoomLoader.RoomTable.INDOORS_B);
        assertEquals(0x5C, spark.variant(0).first().tile());
        assertEquals(0x00, spark.variant(0).first().attributes());
        assertEquals(0x20, spark.variant(0).second().attributes());
        assertEquals(0x14, spark.variant(1).first().attributes());
        assertEquals(0x34, spark.variant(1).second().attributes());

        EntitySpriteDefinition zol = catalog.forEntityType(
            0x1B, EntityRoomLoader.RoomTable.INDOORS_A);
        assertEquals(0x52, zol.variant(0).first().tile());
        assertEquals(0x02, zol.variant(0).first().attributes());
        assertEquals(0x54, zol.variant(1).first().tile());

        EntitySpriteDefinition slimeEyeZol = catalog.forZolSlimeEye();
        assertEquals(0x7C11, slimeEyeZol.address());
        assertEquals(0x00, slimeEyeZol.variant(0).first().attributes());
        assertEquals(0x20, slimeEyeZol.variant(0).second().attributes());

        EntitySpriteDefinition gel = catalog.forEntityType(
            0x1C, EntityRoomLoader.RoomTable.INDOORS_A);
        assertEquals(0x56, gel.variant(0).first().tile());
        assertEquals(0x02, gel.variant(0).first().attributes());
        assertEquals(0x22, gel.variant(1).first().attributes());

        EntitySpriteDefinition hidingZol = catalog.forEntityType(
            0x9B, EntityRoomLoader.RoomTable.OVERWORLD);
        assertEquals(0xFF, hidingZol.variant(0).first().tile());
        assertEquals(0x56, hidingZol.variant(1).first().tile());
        assertNull(hidingZol.variant(1).second());
        assertEquals(0x54, hidingZol.variant(2).first().tile());
        assertEquals(0x52, hidingZol.variant(3).first().tile());

        EntitySpriteDefinition gibdo = catalog.forEntityType(
            0x1F, EntityRoomLoader.RoomTable.INDOORS_B);
        assertEquals(0x74, gibdo.variant(0).first().tile());
        assertEquals(0x02, gibdo.variant(0).first().attributes());
        assertEquals(0x76, gibdo.variant(0).second().tile());
        assertEquals(0x22, gibdo.variant(1).second().attributes());

        EntitySpriteDefinition turtleRockGibdo = catalog.forEntityType(
            0x1F, EntityRoomLoader.RoomTable.INDOORS_B, 0x07);
        assertEquals(0x44, turtleRockGibdo.variant(0).first().tile());
        assertEquals(0x46, turtleRockGibdo.variant(0).second().tile());
        assertEquals(0x22, turtleRockGibdo.variant(1).first().attributes());

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

        EntitySpriteDefinition spikeTrap = catalog.forEntityType(
            0x27, EntityRoomLoader.RoomTable.INDOORS_A);
        assertEquals(0x50, spikeTrap.variant(0).first().tile());
        assertEquals(0x02, spikeTrap.variant(0).first().attributes());
        assertEquals(0x50, spikeTrap.variant(0).second().tile());
        assertEquals(0x22, spikeTrap.variant(0).second().attributes());

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

    @Test
    void mapsEnemyProjectileHandlersToTheirDisassemblyDisplayLists() throws Exception {
        EntitySpriteHandlerCatalog catalog = new EntitySpriteHandlerCatalog(loadRom());

        EntitySpriteDefinition octorokRock = catalog.forEntityType(
            0x0A, EntityRoomLoader.RoomTable.OVERWORLD);
        assertDefinition(octorokRock, 0x03, 0x6A1E,
            EntitySpriteDefinition.Shape.PAIR, 2, 0);

        EntitySpriteDefinition moblinArrow = catalog.forEntityType(
            0x0C, EntityRoomLoader.RoomTable.OVERWORLD);
        assertDefinition(moblinArrow, 0x03, 0x6BC6,
            EntitySpriteDefinition.Shape.PAIR, 4, 0);
    }

    @Test
    void mapsColorShellStatesToTheSixBankTwentyRectangleFamilies() throws Exception {
        EntitySpriteHandlerCatalog catalog = new EntitySpriteHandlerCatalog(loadRom());
        int[] types = {0xE9, 0xEA, 0xEB};
        int[] activeAddresses = {0x6688, 0x66E8, 0x6748};
        int[] inactiveAddresses = {0x67A8, 0x67D8, 0x6808};

        for (int index = 0; index < types.length; index++) {
            EntitySpriteDefinition active = catalog.forColorShellState(
                types[index], 0x00, EntityStatus.ACTIVE);
            assertDefinition(active, 0x20, activeAddresses[index],
                EntitySpriteDefinition.Shape.RECTANGLE, 8, 0);
            assertEquals(3, active.rectangleVariant(0).size());

            EntitySpriteDefinition inactive = catalog.forColorShellState(
                types[index], 0x06, EntityStatus.ACTIVE);
            assertDefinition(inactive, 0x20, inactiveAddresses[index],
                EntitySpriteDefinition.Shape.RECTANGLE, 4, 0);
            assertEquals(3, inactive.rectangleVariant(0).size());

            EntitySpriteDefinition stunned = catalog.forColorShellState(
                types[index], 0x00, EntityStatus.STUNNED);
            assertEquals(inactive.address(), stunned.address());
        }
    }

    @Test
    void colorShellRectangleBytesPreserveRomOffsetsAttributesAndHiddenOam() throws Exception {
        EntitySpriteHandlerCatalog catalog = new EntitySpriteHandlerCatalog(loadRom());

        EntitySpriteDefinition active = catalog.forColorShellState(
            0xE9, 0x00, EntityStatus.ACTIVE);
        EntitySpriteDefinition.RectangleSprite first = active.rectangleVariant(0).get(0);
        assertEquals(0, first.yOffset());
        assertEquals(8, first.xOffset());
        assertEquals(0x48, first.oam().tile());
        assertEquals(0x02, first.oam().attributes());

        EntitySpriteDefinition.RectangleSprite hidden = active.rectangleVariant(1).get(2);
        assertEquals(0xFF, hidden.oam().tile());
        assertEquals(0xFF, hidden.oam().attributes());
    }

    @Test
    void decodesTheRomBurningFirePair() throws Exception {
        EntitySpriteDefinition fire = new EntitySpriteHandlerCatalog(loadRom())
            .forBurningEntity();

        assertDefinition(fire, 0x03, 0x4C44, EntitySpriteDefinition.Shape.PAIR, 2, 0);
        assertEquals(0x34, fire.variant(0).first().tile());
        assertEquals(0x02, fire.variant(0).first().attributes());
        assertEquals(0x34, fire.variant(0).second().tile());
        assertEquals(0x22, fire.variant(0).second().attributes());
        assertEquals(0x14, fire.variant(1).first().attributes());
        assertEquals(0x34, fire.variant(1).second().attributes());
    }

    @Test
    void decodesTheRomDeathRectangleFamiliesAndPowerFinalFrame() throws Exception {
        EntitySpriteHandlerCatalog catalog = new EntitySpriteHandlerCatalog(loadRom());

        EntitySpriteDefinition death = catalog.forDeathEntity();
        assertDefinition(death, 0x03, 0x5488,
            EntitySpriteDefinition.Shape.RECTANGLE, 4, 0);
        EntitySpriteDefinition.RectangleSprite signed = death.rectangleVariant(3).get(0);
        assertEquals(4, signed.yOffset());
        assertEquals(-4, signed.xOffset());
        assertEquals(0x30, signed.oam().tile());
        assertEquals(0x01, signed.oam().attributes());

        EntitySpriteDefinition.RectangleSprite hidden = death.rectangleVariant(0).get(2);
        assertEquals(0xFF, hidden.oam().tile());
        assertEquals(0xFF, hidden.oam().attributes());

        EntitySpriteDefinition power = catalog.forPowerRecoilDeathEntity();
        assertDefinition(power, 0x03, 0x54C8,
            EntitySpriteDefinition.Shape.RECTANGLE, 4, 0);
        assertEquals(8, power.rectangleVariant(3).size());
        EntitySpriteDefinition.RectangleSprite fifth = power.rectangleVariant(3).get(4);
        assertEquals(0x10, fifth.oam().tile());
        assertEquals(0x42, fifth.oam().attributes());
    }

    @Test
    void decodesEveryBombDisplayListFromTheShippedRom() throws Exception {
        EntitySpriteHandlerCatalog catalog = new EntitySpriteHandlerCatalog(loadRom());

        EntitySpriteDefinition bomb = catalog.forEntityType(
            0x02, EntityRoomLoader.RoomTable.OVERWORLD);
        assertDefinition(bomb, 0x03, 0x652E,
            EntitySpriteDefinition.Shape.SINGLE, 1, 0);
        assertEquals(0x80, bomb.variant(0).first().tile());
        assertEquals(0x15, bomb.variant(0).first().attributes());
        assertNull(bomb.variant(0).second());

        EntitySpriteDefinition warning = catalog.forBombRightBeforeExploding();
        assertDefinition(warning, 0x03, 0x5484,
            EntitySpriteDefinition.Shape.PAIR, 1, 0);
        assertPairBytes(warning, new int[][] {{0x30, 0x01, 0x30, 0x61}});

        EntitySpriteDefinition explosion = catalog.forBombExplosion();
        assertDefinition(explosion, 0x03, 0x6530,
            EntitySpriteDefinition.Shape.RECTANGLE, 4, 0);
        assertRectangleBytes(explosion, new int[][][] {
            {
                {-8, -8, 0x32, 0x01}, {-8, 0, 0x32, 0x21},
                {-8, 8, 0x32, 0x01}, {-8, 16, 0x32, 0x21},
                {8, -8, 0x32, 0x01}, {8, 0, 0x32, 0x21},
                {8, 8, 0x32, 0x01}, {8, 16, 0x32, 0x21}
            },
            {
                {-8, -8, 0x10, 0x02}, {-8, 0, 0x12, 0x02},
                {-8, 8, 0x12, 0x22}, {-8, 16, 0x10, 0x22},
                {8, -8, 0x10, 0x42}, {8, 0, 0x12, 0x42},
                {8, 8, 0x12, 0x62}, {8, 16, 0x10, 0x62}
            },
            {
                {-4, -4, 0x30, 0x11}, {-4, 4, 0x30, 0x31},
                {-4, 4, 0x30, 0x11}, {-4, 12, 0x30, 0x31},
                {4, -4, 0x30, 0x11}, {4, 4, 0x30, 0x31},
                {4, 4, 0x30, 0x11}, {4, 12, 0x30, 0x31}
            },
            {
                {-4, -4, 0x30, 0x01}, {-4, 4, 0x30, 0x21},
                {-4, 4, 0x30, 0x01}, {-4, 12, 0x30, 0x21},
                {4, -4, 0x30, 0x01}, {4, 4, 0x30, 0x21},
                {4, 4, 0x30, 0x01}, {4, 12, 0x30, 0x21}
            }
        });
    }

    @Test
    void composesTheBombArrowHandlerPresentationFromTheTwoRomLists() throws Exception {
        EntitySpriteHandlerCatalog catalog = new EntitySpriteHandlerCatalog(loadRom());

        EntitySpriteDefinition arrow = catalog.forEntityType(
            0x00, EntityRoomLoader.RoomTable.OVERWORLD);
        EntitySpriteDefinition bombArrow = catalog.forBombArrow();

        assertDefinition(bombArrow, 0x03, 0x6BC6,
            EntitySpriteDefinition.Shape.DYNAMIC, 4, 0);
        assertEquals(3, bombArrow.dynamicVariant(0).size());
        EntitySpriteDefinition.DynamicSprite bomb = bombArrow.dynamicVariant(0).get(0);
        assertEquals(-2, bomb.yOffset());
        assertEquals(8, bomb.xOffset());
        assertEquals(0x80, bomb.oam().tile());
        assertEquals(0x15, bomb.oam().attributes());
        assertEquals(arrow.variant(0).first(), bombArrow.dynamicVariant(0).get(1).oam());
        assertEquals(arrow.variant(0).second(), bombArrow.dynamicVariant(0).get(2).oam());

        EntitySpriteDefinition.DynamicSprite upBomb = bombArrow.dynamicVariant(2).get(0);
        assertEquals(-6, upBomb.yOffset());
        assertEquals(4, upBomb.xOffset());
    }

    @Test
    void decodesFloatingItemsMixedMainListSourceQuirkAndRectangleOverlay() throws Exception {
        EntitySpriteHandlerCatalog catalog = new EntitySpriteHandlerCatalog(loadRom());

        EntitySpriteDefinition floating = catalog.forEntityType(
            0x86, EntityRoomLoader.RoomTable.OVERWORLD);
        assertDefinition(floating, 0x06, 0x7ADD,
            EntitySpriteDefinition.Shape.PAIR, 7, 0);
        int[][] singles = {
            {0xA6, 0x15}, {0x8E, 0x16}, {0x80, 0x15}, {0xA6, 0x15},
            {0xA9, 0x14}, {0x2A, 0x41}, {0x2A, 0x61}
        };
        for (int variant = 0; variant < singles.length; variant++) {
            assertEquals(singles[variant][0], floating.variant(variant).first().tile());
            assertEquals(singles[variant][1], floating.variant(variant).first().attributes());
            assertNull(floating.variant(variant).second());
        }

        EntitySpriteDefinition second = catalog.forEntityType(
            0xE5, EntityRoomLoader.RoomTable.OVERWORLD);
        assertEquals(0xA9, second.variant(4).first().tile());
        assertEquals(0x14, second.variant(4).first().attributes());
        assertEquals(0x0C, second.variant(5).first().tile());
        assertEquals(0xFE, second.variant(5).first().attributes());
        assertEquals(0x01, second.variant(5).second().tile());
        assertEquals(0xC0, second.variant(5).second().attributes());

        EntitySpriteDefinition overlay = catalog.forFloatingItemOverlay();
        assertDefinition(overlay, 0x06, 0x7AEB,
            EntitySpriteDefinition.Shape.RECTANGLE, 2, 0);
        assertEquals(0, overlay.rectangleVariant(0).get(0).yOffset());
        assertEquals(-4, overlay.rectangleVariant(0).get(0).xOffset());
        assertEquals(0x22, overlay.rectangleVariant(0).get(0).oam().tile());
        assertEquals(0x00, overlay.rectangleVariant(0).get(0).oam().attributes());
        assertEquals(12, overlay.rectangleVariant(1).get(1).xOffset());
        assertEquals(0x60, overlay.rectangleVariant(1).get(1).oam().attributes());
    }

    @Test
    void enemyProjectileVariantsMatchAllShippedRomTileAndAttributeBytes() throws Exception {
        EntitySpriteHandlerCatalog catalog = new EntitySpriteHandlerCatalog(loadRom());

        assertPairBytes(catalog.forEntityType(0x0A, EntityRoomLoader.RoomTable.OVERWORLD),
            new int[][] {
                {0x6C, 0x01, 0x6C, 0x21},
                {0x5C, 0x01, 0x5C, 0x21}
            });
        assertPairBytes(catalog.forEntityType(0x0C, EntityRoomLoader.RoomTable.OVERWORLD),
            new int[][] {
                {0x2E, 0x21, 0x2C, 0x21},
                {0x2C, 0x01, 0x2E, 0x01},
                {0x2A, 0x41, 0x2A, 0x61},
                {0x2A, 0x01, 0x2A, 0x21}
            });
    }

    @Test
    void crystalSwitchUsesItsRomPairAndSingleDisplayListVariant() throws Exception {
        EntitySpriteHandlerCatalog catalog = new EntitySpriteHandlerCatalog(loadRom());

        EntitySpriteDefinition crystal = catalog.forEntityType(
            0x66, EntityRoomLoader.RoomTable.INDOORS_A);

        assertDefinition(crystal, 0x15, 0x4320,
            EntitySpriteDefinition.Shape.PAIR, 1, 0);
        assertPairBytes(crystal, new int[][] {{0x58, 0x03, 0x58, 0x23}});
    }

    @Test
    void evasiveStalfosVariantsMatchBothRomDisplayListFamilies() throws Exception {
        EntitySpriteHandlerCatalog catalog = new EntitySpriteHandlerCatalog(loadRom());

        assertPairBytes(catalog.forEntityType(0x1E, EntityRoomLoader.RoomTable.INDOORS_A),
            new int[][] {
                {0x4A, 0x01, 0x4C, 0x01},
                {0x4C, 0x21, 0x4A, 0x21},
                {0x4E, 0x01, 0x4E, 0x21}
            });
        assertPairBytes(catalog.forStalfosEvasiveState(1),
            new int[][] {
                {0x48, 0x01, 0x48, 0x61},
                {0x48, 0x41, 0x48, 0x21}
            });
    }

    @Test
    void mapsLaserParentToTheRomRotationDisplayListAndLeavesBeamInvisible() throws Exception {
        EntitySpriteHandlerCatalog catalog = new EntitySpriteHandlerCatalog(loadRom());

        EntitySpriteDefinition laser = catalog.forEntityType(
            0x2A, EntityRoomLoader.RoomTable.INDOORS_A);
        assertDefinition(laser, 0x04, 0x6C2D,
            EntitySpriteDefinition.Shape.PAIR, 8, 0);
        assertPairBytes(laser, new int[][] {
            {0x70, 0x03, 0x70, 0x23},
            {0x78, 0x03, 0x7A, 0x03},
            {0x74, 0x03, 0x76, 0x03},
            {0x7C, 0x03, 0x7E, 0x03},
            {0x72, 0x03, 0x72, 0x23},
            {0x7E, 0x23, 0x7C, 0x23},
            {0x76, 0x23, 0x74, 0x23},
            {0x7A, 0x23, 0x78, 0x23}
        });

        EntitySpriteDefinition beam = catalog.forEntityType(
            0x2B, EntityRoomLoader.RoomTable.INDOORS_A);
        assertFalse(beam.supported());
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

    private static void assertPairBytes(EntitySpriteDefinition definition, int[][] expected) {
        assertEquals(expected.length, definition.variantCount());
        for (int variant = 0; variant < expected.length; variant++) {
            EntitySpriteDefinition.Variant actual = definition.variant(variant);
            assertEquals(expected[variant][0], actual.first().tile());
            assertEquals(expected[variant][1], actual.first().attributes());
            assertEquals(expected[variant][2], actual.second().tile());
            assertEquals(expected[variant][3], actual.second().attributes());
        }
    }

    private static void assertPairPrefixBytes(EntitySpriteDefinition definition, int[][] expected) {
        for (int variant = 0; variant < expected.length; variant++) {
            EntitySpriteDefinition.Variant actual = definition.variant(variant);
            assertEquals(expected[variant][0], actual.first().tile());
            assertEquals(expected[variant][1], actual.first().attributes());
            assertEquals(expected[variant][2], actual.second().tile());
            assertEquals(expected[variant][3], actual.second().attributes());
        }
    }

    private static void assertRectangleBytes(EntitySpriteDefinition definition,
                                              int[][][] expected) {
        assertEquals(expected.length, definition.variantCount());
        for (int variant = 0; variant < expected.length; variant++) {
            assertEquals(expected[variant].length, definition.rectangleVariant(variant).size());
            for (int sprite = 0; sprite < expected[variant].length; sprite++) {
                EntitySpriteDefinition.RectangleSprite actual =
                    definition.rectangleVariant(variant).get(sprite);
                assertEquals(expected[variant][sprite][0], actual.yOffset());
                assertEquals(expected[variant][sprite][1], actual.xOffset());
                assertEquals(expected[variant][sprite][2], actual.oam().tile());
                assertEquals(expected[variant][sprite][3], actual.oam().attributes());
            }
        }
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
