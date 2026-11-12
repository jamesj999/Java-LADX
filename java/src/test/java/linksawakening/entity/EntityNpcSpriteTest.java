package linksawakening.entity;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;

import linksawakening.rom.Rom;
import linksawakening.testing.TestRoms;
import linksawakening.world.entity.EntityRoomLoader;
import org.junit.jupiter.api.Test;

@linksawakening.testing.RequiresRom
final class EntityNpcSpriteTest extends EntitySpriteHandlerCatalogFixture {

    @Test
    void owlStatueUsesRomPairAndOnePixelVisualYOffset() throws Exception {
        EntitySpriteHandlerCatalog catalog = new EntitySpriteHandlerCatalog(Rom.of(loadRom()));
        EntitySpriteDefinition statue = catalog.forEntityType(0x42, EntityRoomLoader.RoomTable.INDOORS_A, 2);
        assertDefinition(statue, 0x18, 0x5DF7, EntitySpriteDefinition.Shape.DYNAMIC, 1, 0);
        var pair = statue.dynamicVariant(0);
        assertEquals(2, pair.size());
        assertEquals(1, pair.get(0).yOffset());
        assertEquals(1, pair.get(1).yOffset());
        assertEquals(0, pair.get(0).xOffset());
        assertEquals(8, pair.get(1).xOffset());
        assertEquals(0x5E, pair.get(0).oam().tile());
        assertEquals(0x5E, pair.get(1).oam().tile());
        assertEquals(0x01, pair.get(0).oam().attributes());
        assertEquals(0x21, pair.get(1).oam().attributes());
        assertFalse(catalog.forEntityType(0x42, EntityRoomLoader.RoomTable.INDOORS_B, 0x16)
                .supported());
    }

    @Test
    void mapsWitchToHerFourFrameBankFiveRectangleDisplayList() throws Exception {
        EntitySpriteDefinition witch = new EntitySpriteHandlerCatalog(Rom.of(loadRom()))
                .forEntityType(0x40, EntityRoomLoader.RoomTable.INDOORS_B);

        assertDefinition(witch, 0x05, 0x4780, EntitySpriteDefinition.Shape.RECTANGLE, 4, 0);
        assertEquals(4, witch.rectangleVariant(0).size());
        EntitySpriteDefinition.RectangleSprite first = witch.rectangleVariant(0).get(0);
        assertEquals(0, first.yOffset());
        assertEquals(0, first.xOffset());
        assertEquals(0x60, first.oam().tile());
        assertEquals(0x01, first.oam().attributes());
        EntitySpriteDefinition.RectangleSprite flipped =
                witch.rectangleVariant(2).get(0);
        assertEquals(0x62, flipped.oam().tile());
        assertEquals(0x21, flipped.oam().attributes());
    }

    @Test
    void mapsFollowerDisplayListOverridesToTheirOwnRomTables() {
        EntitySpriteHandlerCatalog catalog = new EntitySpriteHandlerCatalog(TestRoms.partial(syntheticRom()));

        assertDefinition(catalog.forFollowerEntityType(0x6D), 0x05, 0x401C, EntitySpriteDefinition.Shape.PAIR, 7, 0);
        assertDefinition(catalog.forFollowerEntityType(0xC1), 0x18, 0x59B8, EntitySpriteDefinition.Shape.PAIR, 11, 0);
        assertDefinition(catalog.forFollowerEntityType(0xD4), 0x19, 0x5DF8, EntitySpriteDefinition.Shape.PAIR, 6, 0);
        assertDefinition(catalog.forFollowerEntityType(0xD5), 0x19, 0x59BC, EntitySpriteDefinition.Shape.PAIR, 8, 0);
    }

    @Test
    void crystalSwitchUsesItsRomPairAndSingleDisplayListVariant() throws Exception {
        EntitySpriteHandlerCatalog catalog = new EntitySpriteHandlerCatalog(Rom.of(loadRom()));

        EntitySpriteDefinition crystal = catalog.forEntityType(0x66, EntityRoomLoader.RoomTable.INDOORS_A);

        assertDefinition(crystal, 0x15, 0x4320, EntitySpriteDefinition.Shape.PAIR, 1, 0);
        assertPairBytes(crystal, new int[][] {{0x58, 0x03, 0x58, 0x23}});
    }
}
