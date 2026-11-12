package linksawakening.entity;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;

import linksawakening.rom.Rom;
import linksawakening.testing.TestRoms;
import linksawakening.world.entity.EntityRoomLoader;
import org.junit.jupiter.api.Test;

@linksawakening.testing.RequiresRom
final class EntitySpriteHandlerCatalogTest extends EntitySpriteHandlerCatalogFixture {

    @Test
    void liftableStatuesUseTheOriginalFourSpriteRectangles() throws Exception {
        var catalog = new EntitySpriteHandlerCatalog(Rom.of(loadRom()));
        for (int map : new int[] {1, 5}) {
            var statue = catalog.forEntityType(0x9D, EntityRoomLoader.RoomTable.INDOORS_A, map);
            var sprites = statue.rectangleVariant(0);
            assertEquals(4, sprites.size());
            assertEquals(-16, sprites.get(0).yOffset());
            assertEquals(0, sprites.get(0).xOffset());
            assertEquals(map == 1 ? 0x78 : 0x48, sprites.get(0).oam().tile());
            assertEquals(0x26, sprites.get(1).oam().attributes());
        }
    }

    @Test
    void dungeonWarpDecodesCenterAndFourOrbitingSprites() throws Exception {
        var catalog = new EntitySpriteHandlerCatalog(Rom.of(loadRom()));
        var warp = catalog.forEntityType(0x61, EntityRoomLoader.RoomTable.INDOORS_A, 0);
        assertDefinition(warp, 0x19, 0x4393, EntitySpriteDefinition.Shape.DYNAMIC, 8, 0);
        var sprites = warp.dynamicVariant(0);
        assertEquals(6, sprites.size());
        assertEquals(0x1E, sprites.get(0).oam().tile());
        assertEquals(0x02, sprites.get(0).oam().attributes());
        assertEquals(0x62, sprites.get(1).oam().attributes());
        int[] x = {4, 12, 4, -4};
        int[] y = {-8, 0, 8, 0};
        for (int i = 0; i < 4; i++) {
            assertEquals(x[i], sprites.get(i + 2).xOffset());
            assertEquals(y[i], sprites.get(i + 2).yOffset());
            assertEquals(0x24, sprites.get(i + 2).oam().tile());
            assertEquals(1, sprites.get(i + 2).oam().attributes());
        }
        assertEquals(0x42, warp.dynamicVariant(1).get(0).oam().attributes());
        assertEquals(0x22, warp.dynamicVariant(1).get(1).oam().attributes());
        assertFalse(catalog.forEntityType(0x61, EntityRoomLoader.RoomTable.OVERWORLD)
                .supported());
    }

    @Test
    void decodesThreeOfAKindsEightCardFaceVariants() throws Exception {
        EntitySpriteDefinition cards = new EntitySpriteHandlerCatalog(Rom.of(loadRom()))
                .forEntityType(EntitySpriteHandlerCatalog.ENTITY_THREE_OF_A_KIND, EntityRoomLoader.RoomTable.INDOORS_A);

        assertDefinition(cards, 0x06, 0x4911, EntitySpriteDefinition.Shape.PAIR, 8, 0);
        assertEquals(0x74, cards.variant(0).first().tile());
        assertEquals(0x76, cards.variant(0).second().tile());
        assertEquals(0x76, cards.variant(1).first().tile());
        assertEquals(0x20, cards.variant(1).first().attributes() & 0x20);
        assertEquals(0x7E, cards.variant(7).first().tile());
    }

    @Test
    void decodesSyntheticPairAndSingleDisplayListsWithUnsignedBytes() {
        byte[] rom = syntheticRom();
        write(
                rom, 0x06, 0x5000, 0x80, 0xFF, 0x81, 0xFE, 0x82, 0xFD, 0x83, 0xFC, 0x84, 0xFB, 0x85, 0xFA, 0x86, 0xF9,
                0x87, 0xF8);
        write(rom, 0x06, 0x5100, 0x90, 0xF7, 0x91, 0xF6);

        EntitySpriteHandlerCatalog catalog = new EntitySpriteHandlerCatalog(TestRoms.partial(rom));
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
    void decodesTheChestItemDisplayListAndItsTwoRoomSpecificAlternates() throws Exception {
        EntitySpriteHandlerCatalog catalog = new EntitySpriteHandlerCatalog(Rom.of(loadRom()));

        EntitySpriteDefinition ordinary = catalog.forEntityType(
                EntitySpriteHandlerCatalog.ENTITY_CHEST_WITH_ITEM, EntityRoomLoader.RoomTable.INDOORS_A);
        assertDefinition(ordinary, 0x07, 0x7B57, EntitySpriteDefinition.Shape.SINGLE, 0x21, 0);
        // RenderActiveEntitySprite ($07:7C5B): one tile/attribute pair per item.
        assertEquals(0xCA, ordinary.variant(0x1A).first().tile());
        assertEquals(0x17, ordinary.variant(0x1A).first().attributes());
        assertNull(ordinary.variant(0x1A).second());
        assertEquals(0xC0, ordinary.variant(0x16).first().tile());
        assertEquals(0x14, ordinary.variant(0x16).first().attributes());
        assertEquals(0xC2, ordinary.variant(0x17).first().tile());
        assertEquals(0x15, ordinary.variant(0x17).first().attributes());

        EntitySpriteDefinition braceletAlternate = catalog.forChestState(0x05, 0xCE, 0x00);
        assertDefinition(braceletAlternate, 0x07, 0x7B53, EntitySpriteDefinition.Shape.SINGLE, 2, 0);
        assertEquals(0x82, braceletAlternate.variant(0).first().tile());
        assertEquals(0x17, braceletAlternate.variant(0).first().attributes());

        EntitySpriteDefinition shieldAlternate = catalog.forChestState(0x06, 0x1A, 0x01);
        assertDefinition(shieldAlternate, 0x07, 0x7B53, EntitySpriteDefinition.Shape.SINGLE, 2, 0);
        assertEquals(0x86, shieldAlternate.variant(1).first().tile());
        assertEquals(0x14, shieldAlternate.variant(1).first().attributes());
    }

    @Test
    void mapsKeyDropPointToItsSixVariantBankThreeDisplayList() {
        byte[] rom = syntheticRom();
        write(rom, 0x03, 0x5C78, 0xCA, 0x17, 0xC0, 0x17, 0xC2, 0x14, 0xC4, 0x17, 0xC6, 0x14, 0xCA, 0x17);

        EntitySpriteDefinition key = new EntitySpriteHandlerCatalog(TestRoms.partial(rom))
                .forEntityType(EntitySpriteHandlerCatalog.ENTITY_KEY_DROP_POINT, EntityRoomLoader.RoomTable.INDOORS_A);

        assertDefinition(key, 0x03, 0x5C78, EntitySpriteDefinition.Shape.SINGLE, 6, 0);
        assertEquals(0xCA, key.variant(0).first().tile());
        assertEquals(0x17, key.variant(0).first().attributes());
        assertEquals(0xC0, key.variant(1).first().tile());
        assertEquals(0x14, key.variant(2).first().attributes());
        assertEquals(0xC6, key.variant(4).first().tile());
        assertNull(key.variant(5).second());
    }

    @Test
    void mapsArmosKnightToItsFourFramesOfEightSpritesBankSixRectangleList() throws Exception {
        EntitySpriteDefinition armos = new EntitySpriteHandlerCatalog(Rom.of(loadRom()))
                .forEntityType(0x88, EntityRoomLoader.RoomTable.INDOORS_B);

        assertDefinition(armos, 0x06, 0x5523, EntitySpriteDefinition.Shape.RECTANGLE, 4, 0);
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

        EntitySpriteDefinition note = new EntitySpriteHandlerCatalog(TestRoms.partial(rom))
                .forEntityType(0xC9, EntityRoomLoader.RoomTable.OVERWORLD);

        assertDefinition(note, 0x05, 0x7EF8, EntitySpriteDefinition.Shape.SINGLE, 1, 0);
        assertEquals(0x0E, note.variant(0).first().tile());
        assertEquals(0x13, note.variant(0).first().attributes());
        assertNull(note.variant(0).second());
    }

    @Test
    void mapsFishToItsMixedBankFifteenDisplayLists() {
        byte[] rom = syntheticRom();
        write(
                rom, 0x15, 0x449F, 0xFF, 0x00, 0xFF, 0x00, 0x54, 0x00, 0x56, 0x00, 0x58, 0x00, 0x5A, 0x00, 0x56, 0x20,
                0x54, 0x20, 0x5A, 0x20, 0x58, 0x20);
        write(rom, 0x15, 0x44B3, 0x5C, 0x00, 0x5C, 0x20, 0x5E, 0x00, 0x5E, 0x00);

        EntitySpriteDefinition fish = new EntitySpriteHandlerCatalog(TestRoms.partial(rom))
                .forEntityType(0xCC, EntityRoomLoader.RoomTable.OVERWORLD);

        assertDefinition(fish, 0x15, 0x449F, EntitySpriteDefinition.Shape.PAIR, 9, 0);
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
        EntitySpriteDefinition cucco = new EntitySpriteHandlerCatalog(Rom.of(loadRom()))
                .forEntityType(EntitySpriteHandlerCatalog.ENTITY_CUCCO, EntityRoomLoader.RoomTable.OVERWORLD);

        assertDefinition(cucco, 0x05, 0x4514, EntitySpriteDefinition.Shape.PAIR, 4, 0);
        assertPairBytes(cucco, new int[][] {
            {0x50, 0x01, 0x52, 0x01},
            {0x54, 0x01, 0x56, 0x01},
            {0x52, 0x21, 0x50, 0x21},
            {0x56, 0x21, 0x54, 0x21}
        });
    }

    @Test
    void mapsMimicToTheEightVariantBankNineteenListIncludingTheCrossLineByte() throws Exception {
        EntitySpriteDefinition mimic = new EntitySpriteHandlerCatalog(Rom.of(loadRom()))
                .forEntityType(EntitySpriteHandlerCatalog.ENTITY_MIMIC, EntityRoomLoader.RoomTable.INDOORS_B);

        assertDefinition(mimic, 0x19, 0x6A8D, EntitySpriteDefinition.Shape.PAIR, 8, 0);
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
        EntitySpriteDefinition maskedMimic = new EntitySpriteHandlerCatalog(Rom.of(loadRom()))
                .forEntityType(0x8F, EntityRoomLoader.RoomTable.INDOORS_A);

        assertDefinition(maskedMimic, 0x19, 0x4796, EntitySpriteDefinition.Shape.PAIR, 8, 0);
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
    void mapsSwordBeamToItsFourFrameBankNineteenRectangleDisplayList() {
        byte[] rom = syntheticRom();
        write(
                rom, 0x19, 0x44FC, 0x00, 0x00, 0x08, 0x20, 0x00, 0x08, 0x06, 0x20, 0x00, 0x00, 0x06, 0x00, 0x00, 0x08,
                0x08, 0x00, 0x00, 0x04, 0x04, 0x40, 0xFF, 0xFF, 0xFF, 0xFF, 0x00, 0x04, 0x04, 0x00, 0xFF, 0xFF, 0xFF,
                0xFF);

        EntitySpriteDefinition beam = new EntitySpriteHandlerCatalog(TestRoms.partial(rom))
                .forEntityType(0xDF, EntityRoomLoader.RoomTable.OVERWORLD);

        assertDefinition(beam, 0x19, 0x44FC, EntitySpriteDefinition.Shape.RECTANGLE, 4, 0);
        assertEquals(2, beam.rectangleVariant(0).size());
        assertEquals(0x08, beam.rectangleVariant(0).get(0).oam().tile());
        assertEquals(0x20, beam.rectangleVariant(0).get(0).oam().attributes());
        assertEquals(-1, beam.rectangleVariant(2).get(1).xOffset());
        assertEquals(0xFF, beam.rectangleVariant(2).get(1).oam().tile());
    }

    @Test
    void decodesSpikedBeetleDisplayListsFromTheShippedRom() throws Exception {
        EntitySpriteHandlerCatalog catalog = new EntitySpriteHandlerCatalog(Rom.of(loadRom()));

        EntitySpriteDefinition ordinary = catalog.forEntityType(0x2C, EntityRoomLoader.RoomTable.INDOORS_A, 0x00);
        assertDefinition(ordinary, 0x07, 0x7784, EntitySpriteDefinition.Shape.PAIR, 4, 0);
        assertPairBytes(ordinary, new int[][] {
            {0x70, 0x00, 0x70, 0x20},
            {0x72, 0x00, 0x72, 0x20},
            {0x74, 0x00, 0x74, 0x20},
            {0x76, 0x00, 0x76, 0x20}
        });

        EntitySpriteDefinition anglersTunnel = catalog.forEntityType(0x2C, EntityRoomLoader.RoomTable.INDOORS_A, 0x03);
        assertDefinition(anglersTunnel, 0x07, 0x7794, EntitySpriteDefinition.Shape.PAIR, 4, 0);
        assertPairBytes(anglersTunnel, new int[][] {
            {0x60, 0x00, 0x60, 0x20},
            {0x62, 0x00, 0x62, 0x20},
            {0x64, 0x00, 0x64, 0x20},
            {0x66, 0x00, 0x66, 0x20}
        });
    }

    @Test
    void decodesPolsVoiceDisplayListFromTheShippedRom() throws Exception {
        EntitySpriteDefinition polsVoice = new EntitySpriteHandlerCatalog(Rom.of(loadRom()))
                .forEntityType(0x18, EntityRoomLoader.RoomTable.INDOORS_A);

        assertDefinition(polsVoice, 0x06, 0x7373, EntitySpriteDefinition.Shape.PAIR, 2, 0);
        assertPairBytes(polsVoice, new int[][] {
            {0x70, 0x01, 0x70, 0x21},
            {0x72, 0x01, 0x72, 0x21}
        });
    }

    @Test
    void decodesRectangleDisplayListsWithSignedOffsets() {
        byte[] rom = syntheticRom();
        write(
                rom, 0x06, 0x5200, 0xF7, 0x00, 0x70, 0x02, 0x07, 0xF8, 0x72, 0x22, 0xF8, 0x08, 0x74, 0x42, 0x08, 0xF0,
                0x76, 0x62);

        EntitySpriteDefinition rectangle =
                new EntitySpriteHandlerCatalog(TestRoms.partial(rom)).decodeRectangle(0xD2, 0x06, 0x5200, 1, 4, 0);

        assertEquals(EntitySpriteDefinition.Shape.RECTANGLE, rectangle.shape());
        assertEquals(1, rectangle.variantCount());
        assertEquals(4, rectangle.rectangleVariant(0).size());
        EntitySpriteDefinition.RectangleSprite first =
                rectangle.rectangleVariant(0).get(0);
        assertEquals(-9, first.yOffset());
        assertEquals(0, first.xOffset());
        assertEquals(0x70, first.oam().tile());
        assertEquals(0x02, first.oam().attributes());

        EntitySpriteDefinition.RectangleSprite last =
                rectangle.rectangleVariant(0).get(3);
        assertEquals(8, last.yOffset());
        assertEquals(-16, last.xOffset());
        assertEquals(0x76, last.oam().tile());
        assertEquals(0x62, last.oam().attributes());
    }

    @Test
    void decodesLiftableRockIntactAndSmashDisplayListsFromTheRom() throws Exception {
        EntitySpriteHandlerCatalog catalog = new EntitySpriteHandlerCatalog(Rom.of(loadRom()));

        EntitySpriteDefinition outdoor = catalog.forEntityType(
                EntitySpriteHandlerCatalog.ENTITY_LIFTABLE_ROCK, EntityRoomLoader.RoomTable.OVERWORLD);
        assertDefinition(outdoor, 0x19, 0x7B50, EntitySpriteDefinition.Shape.DYNAMIC, 22, 0);
        assertEquals(0xF0, outdoor.dynamicVariant(0).get(0).oam().tile());
        assertEquals(0xF4, outdoor.dynamicVariant(1).get(0).oam().tile());
        assertEquals(-3, outdoor.dynamicVariant(2).get(0).yOffset());
        assertEquals(-4, outdoor.dynamicVariant(2).get(0).xOffset());
        assertEquals(0x28, outdoor.dynamicVariant(6).get(0).oam().tile());
        assertEquals(0x00, outdoor.dynamicVariant(6).get(0).oam().attributes());
        assertEquals(0x06, outdoor.dynamicVariant(14).get(0).oam().attributes());

        EntitySpriteDefinition indoor = catalog.forEntityType(
                EntitySpriteHandlerCatalog.ENTITY_LIFTABLE_ROCK, EntityRoomLoader.RoomTable.INDOORS_A);
        assertEquals(0xF0, indoor.dynamicVariant(0).get(0).oam().tile());
        assertEquals(0x16, indoor.dynamicVariant(0).get(0).oam().attributes());
    }

    @Test
    void mapsSupportedHandlersToDisassemblyBanksAddressesAndInitialVariants() {
        EntitySpriteHandlerCatalog catalog = new EntitySpriteHandlerCatalog(TestRoms.partial(syntheticRom()));

        EntitySpriteDefinition crow = catalog.forEntityType(0x7A, EntityRoomLoader.RoomTable.OVERWORLD);
        assertDefinition(crow, 0x06, 0x5C89, EntitySpriteDefinition.Shape.PAIR, 4, 2);

        EntitySpriteDefinition booBuddy = catalog.forEntityType(0x50, EntityRoomLoader.RoomTable.INDOORS_A);
        assertDefinition(booBuddy, 0x06, 0x79A9, EntitySpriteDefinition.Shape.PAIR, 8, 0);

        EntitySpriteDefinition dog = catalog.forEntityType(0x6F, EntityRoomLoader.RoomTable.OVERWORLD);
        assertDefinition(dog, 0x19, 0x48CA, EntitySpriteDefinition.Shape.PAIR, 4, 2);

        EntitySpriteDefinition marinOutdoor = catalog.forEntityType(0x3E, EntityRoomLoader.RoomTable.OVERWORLD);
        assertDefinition(marinOutdoor, 0x05, 0x4E2A, EntitySpriteDefinition.Shape.PAIR, 11, 6);

        EntitySpriteDefinition marinIndoor = catalog.forEntityType(0x3E, EntityRoomLoader.RoomTable.INDOORS_A);
        assertDefinition(marinIndoor, 0x05, 0x4E0A, EntitySpriteDefinition.Shape.PAIR, 8, 6);

        EntitySpriteDefinition marinAtTalTal = catalog.forEntityType(0xC2, EntityRoomLoader.RoomTable.OVERWORLD);
        assertDefinition(marinAtTalTal, 0x18, 0x5EB7, EntitySpriteDefinition.Shape.PAIR, 8, 0);

        EntitySpriteDefinition kid = catalog.forEntityType(0x73, EntityRoomLoader.RoomTable.OVERWORLD);
        assertDefinition(kid, 0x06, 0x604D, EntitySpriteDefinition.Shape.PAIR, 4, 0);

        EntitySpriteDefinition butterfly = catalog.forEntityType(0x6E, EntityRoomLoader.RoomTable.OVERWORLD);
        assertDefinition(butterfly, 0x06, 0x6BBD, EntitySpriteDefinition.Shape.SINGLE, 2, 0);

        EntitySpriteDefinition keese = catalog.forEntityType(0x19, EntityRoomLoader.RoomTable.INDOORS_A);
        assertDefinition(keese, 0x06, 0x6708, EntitySpriteDefinition.Shape.PAIR, 2, 0);

        EntitySpriteDefinition goomba = catalog.forEntityType(0x9F, EntityRoomLoader.RoomTable.INDOORS_A);
        assertDefinition(goomba, 0x07, 0x65CE, EntitySpriteDefinition.Shape.PAIR, 3, 0);

        EntitySpriteDefinition snake = catalog.forEntityType(0xA1, EntityRoomLoader.RoomTable.INDOORS_A);
        assertDefinition(snake, 0x07, 0x683E, EntitySpriteDefinition.Shape.PAIR, 4, 0);

        EntitySpriteDefinition wizrobe = catalog.forEntityType(0x21, EntityRoomLoader.RoomTable.INDOORS_A);
        assertDefinition(wizrobe, 0x06, 0x7604, EntitySpriteDefinition.Shape.PAIR, 5, 0);

        EntitySpriteDefinition wizrobeProjectile = catalog.forEntityType(0x22, EntityRoomLoader.RoomTable.INDOORS_A);
        assertDefinition(wizrobeProjectile, 0x06, 0x65E1, EntitySpriteDefinition.Shape.PAIR, 4, 0);

        EntitySpriteDefinition octorok = catalog.forEntityType(0x09, EntityRoomLoader.RoomTable.OVERWORLD);
        assertDefinition(octorok, 0x03, 0x57FB, EntitySpriteDefinition.Shape.PAIR, 8, 0);

        EntitySpriteDefinition maskedIronMask = catalog.forIronMaskState(0);
        assertDefinition(maskedIronMask, 0x03, 0x4FCB, EntitySpriteDefinition.Shape.PAIR, 8, 0);

        EntitySpriteDefinition unmaskedIronMask = catalog.forIronMaskState(1);
        assertDefinition(unmaskedIronMask, 0x03, 0x4FEB, EntitySpriteDefinition.Shape.PAIR, 2, 0);

        EntitySpriteDefinition caveBKeese = catalog.forEntityType(0x19, EntityRoomLoader.RoomTable.INDOORS_A, 0x0A);
        assertDefinition(caveBKeese, 0x06, 0x6710, EntitySpriteDefinition.Shape.PAIR, 2, 0);

        EntitySpriteDefinition armos = catalog.forEntityType(0x0F, EntityRoomLoader.RoomTable.OVERWORLD);
        assertDefinition(armos, 0x06, 0x7446, EntitySpriteDefinition.Shape.PAIR, 2, 0);

        EntitySpriteDefinition tektite = catalog.forEntityType(0x0D, EntityRoomLoader.RoomTable.OVERWORLD);
        assertDefinition(tektite, 0x06, 0x78B7, EntitySpriteDefinition.Shape.PAIR, 2, 0);

        EntitySpriteDefinition leever = catalog.forEntityType(0x0E, EntityRoomLoader.RoomTable.OVERWORLD);
        assertDefinition(leever, 0x04, 0x7EE5, EntitySpriteDefinition.Shape.PAIR, 4, 0);

        EntitySpriteDefinition stalfos = catalog.forEntityType(0x1A, EntityRoomLoader.RoomTable.INDOORS_A);
        assertDefinition(stalfos, 0x06, 0x4AA8, EntitySpriteDefinition.Shape.PAIR, 3, 0);

        EntitySpriteDefinition evasiveStalfos = catalog.forEntityType(0x1E, EntityRoomLoader.RoomTable.INDOORS_A);
        assertDefinition(evasiveStalfos, 0x15, 0x4E7D, EntitySpriteDefinition.Shape.PAIR, 3, 0);

        EntitySpriteDefinition antiFairy = catalog.forEntityType(0x15, EntityRoomLoader.RoomTable.INDOORS_B);
        assertDefinition(antiFairy, 0x06, 0x786E, EntitySpriteDefinition.Shape.PAIR, 2, 0);

        EntitySpriteDefinition sparkCounterClockwise =
                catalog.forEntityType(0x16, EntityRoomLoader.RoomTable.INDOORS_B);
        assertDefinition(sparkCounterClockwise, 0x06, 0x6615, EntitySpriteDefinition.Shape.PAIR, 2, 0);

        EntitySpriteDefinition sparkClockwise = catalog.forEntityType(0x17, EntityRoomLoader.RoomTable.INDOORS_B);
        assertDefinition(sparkClockwise, 0x06, 0x6615, EntitySpriteDefinition.Shape.PAIR, 2, 0);

        EntitySpriteDefinition zol = catalog.forEntityType(0x1B, EntityRoomLoader.RoomTable.INDOORS_A);
        assertDefinition(zol, 0x06, 0x7C09, EntitySpriteDefinition.Shape.PAIR, 2, 0);

        EntitySpriteDefinition gel = catalog.forEntityType(0x1C, EntityRoomLoader.RoomTable.INDOORS_A);
        assertDefinition(gel, 0x06, 0x7BFA, EntitySpriteDefinition.Shape.SINGLE, 2, 0);

        EntitySpriteDefinition hidingZol = catalog.forEntityType(0x9B, EntityRoomLoader.RoomTable.OVERWORLD);
        assertDefinition(hidingZol, 0x07, 0x729B, EntitySpriteDefinition.Shape.PAIR, 4, 0);
        assertNull(hidingZol.variant(1).second());
        assertEquals(0x00, hidingZol.variant(1).first().tile());

        EntitySpriteDefinition star =
                catalog.forEntityType(EntitySpriteHandlerCatalog.ENTITY_STAR, EntityRoomLoader.RoomTable.INDOORS_A);
        assertDefinition(star, 0x07, 0x7247, EntitySpriteDefinition.Shape.PAIR, 5, 0);

        EntitySpriteDefinition gibdo = catalog.forEntityType(0x1F, EntityRoomLoader.RoomTable.INDOORS_B);
        assertDefinition(gibdo, 0x06, 0x7E6F, EntitySpriteDefinition.Shape.PAIR, 2, 0);

        EntitySpriteDefinition turtleRockGibdo =
                catalog.forEntityType(0x1F, EntityRoomLoader.RoomTable.INDOORS_B, 0x07);
        assertDefinition(turtleRockGibdo, 0x06, 0x7E77, EntitySpriteDefinition.Shape.PAIR, 2, 0);

        EntitySpriteDefinition peaHat = catalog.forEntityType(0xA0, EntityRoomLoader.RoomTable.INDOORS_B);
        assertDefinition(peaHat, 0x07, 0x6701, EntitySpriteDefinition.Shape.PAIR, 2, 0);

        EntitySpriteDefinition ghini = catalog.forEntityType(0x12, EntityRoomLoader.RoomTable.OVERWORLD);
        assertDefinition(ghini, 0x04, 0x5BFC, EntitySpriteDefinition.Shape.PAIR, 2, 0);

        EntitySpriteDefinition hardHat = catalog.forEntityType(0x20, EntityRoomLoader.RoomTable.INDOORS_A);
        assertDefinition(hardHat, 0x06, 0x4F2C, EntitySpriteDefinition.Shape.PAIR, 2, 0);

        EntitySpriteDefinition caveBHardHat = catalog.forEntityType(0x20, EntityRoomLoader.RoomTable.INDOORS_A, 0x0A);
        assertDefinition(caveBHardHat, 0x06, 0x4F34, EntitySpriteDefinition.Shape.PAIR, 2, 0);

        EntitySpriteDefinition spikeTrap = catalog.forEntityType(0x27, EntityRoomLoader.RoomTable.INDOORS_A);
        assertDefinition(spikeTrap, 0x06, 0x74FA, EntitySpriteDefinition.Shape.PAIR, 1, 0);

        EntitySpriteDefinition waterTektite = catalog.forEntityType(0x99, EntityRoomLoader.RoomTable.INDOORS_A);
        assertDefinition(waterTektite, 0x07, 0x752D, EntitySpriteDefinition.Shape.PAIR, 2, 0);

        EntitySpriteDefinition pairodd = catalog.forEntityType(0x57, EntityRoomLoader.RoomTable.INDOORS_A);
        assertDefinition(pairodd, 0x04, 0x5DD1, EntitySpriteDefinition.Shape.PAIR, 8, 0);

        EntitySpriteDefinition pairoddProjectile = catalog.forEntityType(0x58, EntityRoomLoader.RoomTable.INDOORS_A);
        assertDefinition(pairoddProjectile, 0x04, 0x5EF4, EntitySpriteDefinition.Shape.PAIR, 2, 0);

        EntitySpriteDefinition grandpa = catalog.forEntityType(0x77, EntityRoomLoader.RoomTable.INDOORS_A);
        assertDefinition(grandpa, 0x06, 0x5C51, EntitySpriteDefinition.Shape.RECTANGLE, 2, 0);

        EntitySpriteDefinition pieceOfPower = catalog.forEntityType(0x33, EntityRoomLoader.RoomTable.OVERWORLD);
        assertDefinition(pieceOfPower, 0x03, 0x5B65, EntitySpriteDefinition.Shape.PAIR, 2, 0);

        EntitySpriteDefinition guardianAcorn = catalog.forEntityType(0x34, EntityRoomLoader.RoomTable.OVERWORLD);
        assertDefinition(guardianAcorn, 0x03, 0x5B5B, EntitySpriteDefinition.Shape.SINGLE, 1, 0);

        EntitySpriteDefinition heartPiece = catalog.forEntityType(0x35, EntityRoomLoader.RoomTable.OVERWORLD);
        assertDefinition(heartPiece, 0x03, 0x5A4D, EntitySpriteDefinition.Shape.PAIR, 1, 0);

        EntitySpriteDefinition heartContainer = catalog.forEntityType(0x36, EntityRoomLoader.RoomTable.OVERWORLD);
        assertDefinition(heartContainer, 0x03, 0x59D8, EntitySpriteDefinition.Shape.PAIR, 1, 0);

        assertDefinition(
                catalog.forEntityType(0x2D, EntityRoomLoader.RoomTable.OVERWORLD),
                0x03,
                0x5D36,
                EntitySpriteDefinition.Shape.SINGLE,
                1,
                0);
        assertDefinition(
                catalog.forEntityType(0x2E, EntityRoomLoader.RoomTable.OVERWORLD),
                0x03,
                0x609C,
                EntitySpriteDefinition.Shape.SINGLE,
                1,
                0);
        assertDefinition(
                catalog.forEntityType(0x32, EntityRoomLoader.RoomTable.OVERWORLD),
                0x03,
                0x5B80,
                EntitySpriteDefinition.Shape.PAIR,
                2,
                0);
        assertDefinition(
                catalog.forEntityType(0x37, EntityRoomLoader.RoomTable.OVERWORLD),
                0x03,
                0x6079,
                EntitySpriteDefinition.Shape.PAIR,
                1,
                0);
        assertDefinition(
                catalog.forEntityType(0x38, EntityRoomLoader.RoomTable.OVERWORLD),
                0x03,
                0x5FC0,
                EntitySpriteDefinition.Shape.SINGLE,
                1,
                0);
        assertDefinition(
                catalog.forEntityType(0x3A, EntityRoomLoader.RoomTable.OVERWORLD),
                0x03,
                0x5D47,
                EntitySpriteDefinition.Shape.PAIR,
                1,
                0);
        assertDefinition(
                catalog.forEntityType(0x3B, EntityRoomLoader.RoomTable.OVERWORLD),
                0x03,
                0x6055,
                EntitySpriteDefinition.Shape.SINGLE,
                1,
                0);
        assertDefinition(
                catalog.forEntityType(0x3C, EntityRoomLoader.RoomTable.OVERWORLD),
                0x03,
                0x5FFB,
                EntitySpriteDefinition.Shape.SINGLE,
                1,
                0);
        assertDefinition(
                catalog.forEntityType(0x3D, EntityRoomLoader.RoomTable.OVERWORLD),
                0x03,
                0x5FD1,
                EntitySpriteDefinition.Shape.SINGLE,
                1,
                0);
    }

    @Test
    void wizrobeDisplayListsMatchTheShippedRomBytes() throws Exception {
        EntitySpriteHandlerCatalog catalog = new EntitySpriteHandlerCatalog(Rom.of(loadRom()));

        EntitySpriteDefinition wizrobe = catalog.forEntityType(0x21, EntityRoomLoader.RoomTable.INDOORS_A);
        assertPairBytes(wizrobe, new int[][] {
            {0x6E, 0x00, 0x6E, 0x20},
            {0x66, 0x20, 0x64, 0x20},
            {0x64, 0x00, 0x66, 0x00},
            {0x62, 0x00, 0x62, 0x20},
            {0x60, 0x00, 0x60, 0x20}
        });

        EntitySpriteDefinition projectile = catalog.forEntityType(0x22, EntityRoomLoader.RoomTable.INDOORS_A);
        assertPairBytes(projectile, new int[][] {
            {0x6A, 0x23, 0x68, 0x23},
            {0x68, 0x03, 0x6A, 0x03},
            {0x6C, 0x43, 0x6C, 0x63},
            {0x6C, 0x03, 0x6C, 0x23}
        });
    }

    @Test
    void mapsGoombaToItsExactBankSevenDisplayList() throws Exception {
        EntitySpriteDefinition goomba = new EntitySpriteHandlerCatalog(Rom.of(loadRom()))
                .forEntityType(0x9F, EntityRoomLoader.RoomTable.INDOORS_A);
        assertPairBytes(goomba, new int[][] {
            {0x4A, 0x02, 0x4C, 0x02},
            {0x4C, 0x22, 0x4A, 0x22},
            {0x4E, 0x02, 0x4E, 0x22}
        });
    }

    @Test
    void mapsSnakeToItsExactBankSevenDisplayList() throws Exception {
        EntitySpriteDefinition snake = new EntitySpriteHandlerCatalog(Rom.of(loadRom()))
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
        EntitySpriteDefinition likeLike = new EntitySpriteHandlerCatalog(Rom.of(loadRom()))
                .forEntityType(0x23, EntityRoomLoader.RoomTable.INDOORS_A);
        assertDefinition(likeLike, 0x06, 0x7DD4, EntitySpriteDefinition.Shape.PAIR, 2, 0);
        assertPairBytes(likeLike, new int[][] {
            {0x7C, 0x01, 0x7C, 0x21},
            {0x7E, 0x01, 0x7E, 0x21}
        });
    }

    @Test
    void mapsTheLikeLikeShieldDropToItsExactBankThreeSprite() throws Exception {
        EntitySpriteDefinition shield = new EntitySpriteHandlerCatalog(Rom.of(loadRom()))
                .forEntityType(0x31, EntityRoomLoader.RoomTable.INDOORS_A);
        assertDefinition(shield, 0x03, 0x5B95, EntitySpriteDefinition.Shape.SINGLE, 1, 0);
        assertEquals(0x86, shield.variant(0).first().tile());
        assertEquals(0x17, shield.variant(0).first().attributes());
        assertNull(shield.variant(0).second());
    }

    @Test
    void mapsTheBeachSwordToTheAlternateBankThreeSprite() throws Exception {
        EntitySpriteDefinition sword = new EntitySpriteHandlerCatalog(Rom.of(loadRom())).forSwordShieldPickup(true);

        assertDefinition(sword, 0x03, 0x5B97, EntitySpriteDefinition.Shape.SINGLE, 1, 0);
        assertEquals(0x84, sword.variant(0).first().tile());
        assertEquals(0x17, sword.variant(0).first().attributes());
        assertNull(sword.variant(0).second());
    }
}
