package linksawakening.entity;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import linksawakening.world.entity.EntityRoomLoader;
import org.junit.jupiter.api.Test;

@linksawakening.testing.RequiresRom
final class EntityBossSpriteTest extends EntitySpriteHandlerCatalogFixture {

    @Test
    void decodesMoblinKingsFourteenSourceBodyVariants() throws Exception {
        EntitySpriteDefinition king = new EntitySpriteHandlerCatalog(new linksawakening.rom.Rom(loadRom()))
                .forEntityType(0xE4, EntityRoomLoader.RoomTable.INDOORS_B);

        assertDefinition(king, 0x15, 0x4A15, EntitySpriteDefinition.Shape.RECTANGLE, 14, 0);
        assertEquals(3, king.rectangleVariant(0).size());
        assertEquals(0x56, king.rectangleVariant(0).get(0).oam().tile());
        assertEquals(0x5A, king.rectangleVariant(0).get(2).oam().tile());
    }

    @Test
    void buildsMoblinKingsBodyWeaponAndAirborneShadowLayers() throws Exception {
        EntitySpriteDefinition king =
                new EntitySpriteHandlerCatalog(new linksawakening.rom.Rom(loadRom())).forMoblinKingState(0, 0, true);

        assertEquals(EntitySpriteDefinition.Shape.DYNAMIC, king.shape());
        assertEquals(9, king.dynamicVariant(0).size());
        assertEquals(0x54, king.dynamicVariant(0).get(0).oam().tile());
        assertEquals(0x56, king.dynamicVariant(0).get(3).oam().tile());
        assertEquals(0x26, king.dynamicVariant(0).get(6).oam().tile());
    }

    @Test
    void decodesRollingBonesAndBuildsTheSixPairRollingBar() throws Exception {
        EntitySpriteHandlerCatalog catalog = new EntitySpriteHandlerCatalog(new linksawakening.rom.Rom(loadRom()));

        EntitySpriteDefinition boss = catalog.forEntityType(
                EntitySpriteHandlerCatalog.ENTITY_ROLLING_BONES, EntityRoomLoader.RoomTable.INDOORS_A);
        assertDefinition(boss, 0x06, 0x6E1E, EntitySpriteDefinition.Shape.RECTANGLE, 6, 0);
        assertEquals(4, boss.rectangleVariant(0).size());
        assertEquals(0x60, boss.rectangleVariant(0).get(0).oam().tile());

        EntitySpriteDefinition bar = catalog.forRollingBonesBar(0, 0x38);
        assertEquals(EntitySpriteDefinition.Shape.DYNAMIC, bar.shape());
        assertEquals(12, bar.dynamicVariant(0).size());
        assertEquals(-0x18, bar.dynamicVariant(0).get(0).yOffset());
        assertEquals(0x6C, bar.dynamicVariant(0).get(0).oam().tile());
        assertEquals(0x07, bar.dynamicVariant(0).get(0).oam().attributes());
        assertEquals(0x27, bar.dynamicVariant(0).get(1).oam().attributes());

        EntitySpriteDefinition shortened = catalog.forRollingBonesBar(5, 0x38);
        assertEquals(2, shortened.dynamicVariant(0).size());
    }

    @Test
    void selectsHinoxMapSpecificSpriteTablesFromTheRom() throws Exception {
        EntitySpriteHandlerCatalog catalog = new EntitySpriteHandlerCatalog(new linksawakening.rom.Rom(loadRom()));

        assertDefinition(
                catalog.forEntityType(
                        EntitySpriteHandlerCatalog.ENTITY_HINOX, EntityRoomLoader.RoomTable.INDOORS_A, 0x05),
                0x06,
                0x51FA,
                EntitySpriteDefinition.Shape.RECTANGLE,
                4,
                0);
        assertDefinition(
                catalog.forEntityType(
                        EntitySpriteHandlerCatalog.ENTITY_HINOX, EntityRoomLoader.RoomTable.INDOORS_A, 0x06),
                0x06,
                0x523A,
                EntitySpriteDefinition.Shape.RECTANGLE,
                4,
                0);
        assertDefinition(
                catalog.forEntityType(
                        EntitySpriteHandlerCatalog.ENTITY_HINOX, EntityRoomLoader.RoomTable.INDOORS_A, 0x07),
                0x06,
                0x527A,
                EntitySpriteDefinition.Shape.RECTANGLE,
                4,
                0);
    }

    @Test
    void mapsMiniMoldormToItsBankFourPairDisplayList() throws Exception {
        EntitySpriteHandlerCatalog catalog = new EntitySpriteHandlerCatalog(new linksawakening.rom.Rom(loadRom()));

        EntitySpriteDefinition definition = catalog.forEntityType(
                EntitySpriteHandlerCatalog.ENTITY_MINI_MOLDORM, EntityRoomLoader.RoomTable.INDOORS_A);

        assertDefinition(definition, 0x04, 0x5A49, EntitySpriteDefinition.Shape.PAIR, 10, 0);
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
        EntitySpriteHandlerCatalog catalog = new EntitySpriteHandlerCatalog(new linksawakening.rom.Rom(loadRom()));

        EntitySpriteDefinition definition = catalog.forMiniMoldormState(0x03, 0x50, 0x60, 0x48, 0x58, 0x40, 0x50);

        assertDefinition(definition, 0x04, 0x5A49, EntitySpriteDefinition.Shape.DYNAMIC, 1, 0);
        assertEquals(6, definition.dynamicVariant(0).size());

        EntitySpriteDefinition.DynamicSprite head = definition.dynamicVariant(0).get(0);
        assertEquals(0, head.yOffset());
        assertEquals(0, head.xOffset());
        assertEquals(0x74, head.oam().tile());
        assertEquals(0x20, head.oam().attributes());
        assertTrue(head.appliesEntityFlipAttribute());

        EntitySpriteDefinition.DynamicSprite firstTail =
                definition.dynamicVariant(0).get(2);
        assertEquals(-8, firstTail.yOffset());
        assertEquals(-8, firstTail.xOffset());
        assertEquals(0x7A, firstTail.oam().tile());
        assertEquals(EntitySpriteDefinition.DynamicSprite.TileSource.ENTITY_SHEETS, firstTail.tileSource());

        EntitySpriteDefinition.DynamicSprite secondTail =
                definition.dynamicVariant(0).get(4);
        assertEquals(-16, secondTail.yOffset());
        assertEquals(-16, secondTail.xOffset());
        assertEquals(0x7C, secondTail.oam().tile());
    }
}
