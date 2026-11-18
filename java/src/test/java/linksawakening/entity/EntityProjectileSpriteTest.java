package linksawakening.entity;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

import linksawakening.rom.Rom;
import linksawakening.testing.TestRoms;
import linksawakening.world.entity.EntityRoomLoader;
import org.junit.jupiter.api.Test;

@linksawakening.testing.RequiresRom
final class EntityProjectileSpriteTest extends EntitySpriteHandlerCatalogFixture {

    @Test
    void hookshotUsesTheInlineHookshotChainSpriteBytes() {
        EntitySpriteDefinition hookshot = new EntitySpriteHandlerCatalog(TestRoms.partial(syntheticRom()))
                .forEntityType(0x03, EntityRoomLoader.RoomTable.OVERWORLD);

        assertDefinition(hookshot, -1, -1, EntitySpriteDefinition.Shape.PAIR, 1, 0);
        assertEquals(0x36, hookshot.variant(0).first().tile());
        assertEquals(0x00, hookshot.variant(0).first().attributes());
        assertEquals(0x36, hookshot.variant(0).second().tile());
        assertEquals(0x20, hookshot.variant(0).second().attributes());
    }

    @Test
    void mapsPlayerArrowToTheSharedBankThreeArrowDisplayList() {
        byte[] rom = syntheticRom();
        write(
                rom, 0x03, 0x6BC6, 0x2E, 0x21, 0x2C, 0x21, 0x2C, 0x01, 0x2E, 0x01, 0x2A, 0x41, 0x2A, 0x61, 0x2A, 0x01,
                0x2A, 0x21);

        EntitySpriteDefinition arrow = new EntitySpriteHandlerCatalog(TestRoms.partial(rom))
                .forEntityType(0x00, EntityRoomLoader.RoomTable.OVERWORLD);

        assertDefinition(arrow, 0x03, 0x6BC6, EntitySpriteDefinition.Shape.PAIR, 4, 0);
        assertPairBytes(arrow, new int[][] {
            {0x2E, 0x21, 0x2C, 0x21},
            {0x2C, 0x01, 0x2E, 0x01},
            {0x2A, 0x41, 0x2A, 0x61},
            {0x2A, 0x01, 0x2A, 0x21}
        });
    }

    @Test
    void mapsBoomerangToItsFourFrameBankNineteenDisplayList() {
        byte[] rom = syntheticRom();
        write(
                rom, 0x19, 0x4451, 0x38, 0x14, 0x38, 0x34, 0xA4, 0x14, 0xFF, 0xFF, 0x38, 0x54, 0x38, 0x74, 0xFF, 0xFF,
                0xA4, 0x34);

        EntitySpriteDefinition boomerang = new EntitySpriteHandlerCatalog(TestRoms.partial(rom))
                .forEntityType(0x01, EntityRoomLoader.RoomTable.OVERWORLD);

        assertDefinition(boomerang, 0x19, 0x4451, EntitySpriteDefinition.Shape.PAIR, 4, 0);
        assertPairBytes(boomerang, new int[][] {
            {0x38, 0x14, 0x38, 0x34},
            {0xA4, 0x14, 0xFF, 0xFF},
            {0x38, 0x54, 0x38, 0x74},
            {0xFF, 0xFF, 0xA4, 0x34}
        });
    }

    @Test
    void mapsMagicRodFireballAndItsSharedFireStateToRomDisplayLists() {
        byte[] rom = syntheticRom();
        write(rom, 0x03, 0x69AA, 0x36, 0x02, 0x36, 0x22, 0x36, 0x03, 0x36, 0x23);
        write(rom, 0x03, 0x4C44, 0x34, 0x02, 0x34, 0x22, 0x34, 0x04, 0x34, 0x24);

        EntitySpriteHandlerCatalog catalog = new EntitySpriteHandlerCatalog(TestRoms.partial(rom));
        EntitySpriteDefinition fireball = catalog.forEntityType(0x04, EntityRoomLoader.RoomTable.OVERWORLD);
        EntitySpriteDefinition fire = catalog.forMagicRodFireState();

        assertDefinition(fireball, 0x03, 0x69AA, EntitySpriteDefinition.Shape.PAIR, 2, 0);
        assertPairBytes(fireball, new int[][] {
            {0x36, 0x02, 0x36, 0x22},
            {0x36, 0x03, 0x36, 0x23}
        });
        assertDefinition(fire, 0x03, 0x4C44, EntitySpriteDefinition.Shape.PAIR, 2, 0);
        assertPairBytes(fire, new int[][] {
            {0x34, 0x02, 0x34, 0x22},
            {0x34, 0x04, 0x34, 0x24}
        });
    }

    @Test
    void mapsMagicPowderSprinkleAndTorchTransitionListsToRomData() {
        byte[] rom = syntheticRom();
        write(
                rom, 0x18, 0x7ABA, 0x06, 0xFE, 0x24, 0x03, 0x03, 0x04, 0x24, 0x13, 0x05, 0x0A, 0x24, 0x03, 0x05, 0xFE,
                0x24, 0x13, 0x02, 0x04, 0x24, 0x03, 0x04, 0x0A, 0x24, 0x13, 0x03, 0xFF, 0x24, 0x03, 0x01, 0x04, 0x24,
                0x13, 0x02, 0x09, 0x24, 0x03, 0x01, 0x00, 0x24, 0x13, 0xFF, 0x04, 0x24, 0x03, 0x00, 0x06, 0x24, 0x13,
                0x00, 0x01, 0x24, 0x03, 0xFE, 0x03, 0x24, 0x13, 0xFF, 0x05, 0x24, 0x03, 0xFD, 0x03, 0x24, 0x13);
        write(rom, 0x18, 0x795E, 0x6C, 0x74, 0x6D, 0x75);
        write(rom, 0x18, 0x7962, 0x64, 0x74, 0x65, 0x75);

        EntitySpriteHandlerCatalog catalog = new EntitySpriteHandlerCatalog(TestRoms.partial(rom));
        EntitySpriteDefinition sprinkle = catalog.forEntityType(0x08, EntityRoomLoader.RoomTable.OVERWORLD);
        EntitySpriteDefinition ignite = catalog.forMagicPowderTorchState(true);
        EntitySpriteDefinition expire = catalog.forMagicPowderTorchState(false);

        assertDefinition(sprinkle, 0x18, 0x7ABA, EntitySpriteDefinition.Shape.RECTANGLE, 8, 0);
        assertEquals(3, sprinkle.rectangleVariant(0).size());
        assertEquals(6, sprinkle.rectangleVariant(0).get(0).yOffset());
        assertEquals(0x24, sprinkle.rectangleVariant(0).get(0).oam().tile());
        assertEquals(0x03, sprinkle.rectangleVariant(0).get(2).oam().attributes());
        assertDefinition(ignite, 0x18, 0x795E, EntitySpriteDefinition.Shape.PAIR, 1, 0);
        assertPairBytes(ignite, new int[][] {{0x6C, 0x74, 0x6D, 0x75}});
        assertDefinition(expire, 0x18, 0x7962, EntitySpriteDefinition.Shape.PAIR, 1, 0);
        assertPairBytes(expire, new int[][] {{0x64, 0x74, 0x65, 0x75}});
    }

    @Test
    void mapsBomberToItsBankEighteenRectangleDisplayList() throws Exception {
        EntitySpriteHandlerCatalog catalog = new EntitySpriteHandlerCatalog(Rom.of(loadRom()));

        EntitySpriteDefinition bomber = catalog.forEntityType(0xBA, EntityRoomLoader.RoomTable.OVERWORLD);

        assertDefinition(bomber, 0x18, 0x77ED, EntitySpriteDefinition.Shape.RECTANGLE, 4, 0);
        assertRectangleBytes(bomber, new int[][][] {
            {{0, -4, 0x70, 0x02}, {0, 4, 0x72, 0x02}, {0, 12, 0x70, 0x22}},
            {{0, -4, 0x74, 0x02}, {0, 4, 0x72, 0x02}, {0, 12, 0x74, 0x22}},
            {{0, -4, 0x76, 0x02}, {0, 4, 0x72, 0x02}, {0, 12, 0x76, 0x22}},
            {{0, -4, 0x74, 0x02}, {0, 4, 0x72, 0x02}, {0, 12, 0x74, 0x22}}
        });
    }

    @Test
    void mapsEnemyProjectileHandlersToTheirDisassemblyDisplayLists() throws Exception {
        EntitySpriteHandlerCatalog catalog = new EntitySpriteHandlerCatalog(Rom.of(loadRom()));

        EntitySpriteDefinition octorokRock = catalog.forEntityType(0x0A, EntityRoomLoader.RoomTable.OVERWORLD);
        assertDefinition(octorokRock, 0x03, 0x6A1E, EntitySpriteDefinition.Shape.PAIR, 2, 0);

        EntitySpriteDefinition moblinArrow = catalog.forEntityType(0x0C, EntityRoomLoader.RoomTable.OVERWORLD);
        assertDefinition(moblinArrow, 0x03, 0x6BC6, EntitySpriteDefinition.Shape.PAIR, 4, 0);
    }

    @Test
    void decodesTheRomBurningFirePair() throws Exception {
        EntitySpriteDefinition fire = new EntitySpriteHandlerCatalog(Rom.of(loadRom())).forBurningEntity();

        assertDefinition(fire, 0x03, 0x4C44, EntitySpriteDefinition.Shape.PAIR, 2, 0);
        assertEquals(0x34, fire.variant(0).first().tile());
        assertEquals(0x02, fire.variant(0).first().attributes());
        assertEquals(0x34, fire.variant(0).second().tile());
        assertEquals(0x22, fire.variant(0).second().attributes());
        assertEquals(0x14, fire.variant(1).first().attributes());
        assertEquals(0x34, fire.variant(1).second().attributes());
    }

    @Test
    void decodesEveryBombDisplayListFromTheShippedRom() throws Exception {
        EntitySpriteHandlerCatalog catalog = new EntitySpriteHandlerCatalog(Rom.of(loadRom()));

        EntitySpriteDefinition bomb = catalog.forEntityType(0x02, EntityRoomLoader.RoomTable.OVERWORLD);
        assertDefinition(bomb, 0x03, 0x652E, EntitySpriteDefinition.Shape.SINGLE, 1, 0);
        assertEquals(0x80, bomb.variant(0).first().tile());
        assertEquals(0x15, bomb.variant(0).first().attributes());
        assertNull(bomb.variant(0).second());

        EntitySpriteDefinition warning = catalog.forBombRightBeforeExploding();
        assertDefinition(warning, 0x03, 0x5484, EntitySpriteDefinition.Shape.PAIR, 1, 0);
        assertPairBytes(warning, new int[][] {{0x30, 0x01, 0x30, 0x61}});

        EntitySpriteDefinition explosion = catalog.forBombExplosion();
        assertDefinition(explosion, 0x03, 0x6530, EntitySpriteDefinition.Shape.RECTANGLE, 4, 0);
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
        EntitySpriteHandlerCatalog catalog = new EntitySpriteHandlerCatalog(Rom.of(loadRom()));

        EntitySpriteDefinition arrow = catalog.forEntityType(0x00, EntityRoomLoader.RoomTable.OVERWORLD);
        EntitySpriteDefinition bombArrow = catalog.forBombArrow();

        assertDefinition(bombArrow, 0x03, 0x6BC6, EntitySpriteDefinition.Shape.DYNAMIC, 4, 0);
        assertEquals(3, bombArrow.dynamicVariant(0).size());
        EntitySpriteDefinition.DynamicSprite bomb = bombArrow.dynamicVariant(0).get(0);
        assertEquals(-2, bomb.yOffset());
        assertEquals(8, bomb.xOffset());
        assertEquals(0x80, bomb.oam().tile());
        assertEquals(0x15, bomb.oam().attributes());
        assertEquals(
                arrow.variant(0).first(), bombArrow.dynamicVariant(0).get(1).oam());
        assertEquals(
                arrow.variant(0).second(), bombArrow.dynamicVariant(0).get(2).oam());

        EntitySpriteDefinition.DynamicSprite upBomb =
                bombArrow.dynamicVariant(2).get(0);
        assertEquals(-6, upBomb.yOffset());
        assertEquals(4, upBomb.xOffset());
    }

    @Test
    void enemyProjectileVariantsMatchAllShippedRomTileAndAttributeBytes() throws Exception {
        EntitySpriteHandlerCatalog catalog = new EntitySpriteHandlerCatalog(Rom.of(loadRom()));

        assertPairBytes(catalog.forEntityType(0x0A, EntityRoomLoader.RoomTable.OVERWORLD), new int[][] {
            {0x6C, 0x01, 0x6C, 0x21},
            {0x5C, 0x01, 0x5C, 0x21}
        });
        assertPairBytes(catalog.forEntityType(0x0C, EntityRoomLoader.RoomTable.OVERWORLD), new int[][] {
            {0x2E, 0x21, 0x2C, 0x21},
            {0x2C, 0x01, 0x2E, 0x01},
            {0x2A, 0x41, 0x2A, 0x61},
            {0x2A, 0x01, 0x2A, 0x21}
        });
    }
}
