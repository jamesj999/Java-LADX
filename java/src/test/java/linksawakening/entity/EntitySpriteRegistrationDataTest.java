package linksawakening.entity;

import static org.junit.jupiter.api.Assertions.*;

import linksawakening.testing.TestRoms;
import linksawakening.world.entity.EntityRoomLoader.RoomTable;
import org.junit.jupiter.api.Test;

class EntitySpriteRegistrationDataTest {
    @Test
    void immutableLookupRetainsContextPriorityAndArbitraryMapSelectors() {
        var data = EntitySpriteRegistrationData.instance();
        assertEquals(
                EntitySpriteRecipe.Kind.UNSUPPORTED,
                data.lookup(0x86, RoomTable.COLOR_DUNGEON, 0).kind());
        assertEquals(
                EntitySpriteRecipe.Kind.FLOATING_ITEM,
                data.lookup(0x86, RoomTable.OVERWORLD, 0).kind());
        assertEquals(
                EntitySpriteRecipe.Kind.FLOATING_ITEM,
                data.lookup(0xE5, RoomTable.COLOR_DUNGEON, 0).kind());
        assertEquals(0, data.lookup(0x39, RoomTable.INDOORS_A, -2).initialVariant());
        assertEquals(2, data.lookup(0x39, RoomTable.INDOORS_A, 0x102).initialVariant());
        assertEquals(
                EntitySpriteRecipe.Kind.OWL_STATUE,
                data.lookup(0x42, RoomTable.INDOORS_A, 0).kind());
        assertNotEquals(
                EntitySpriteRecipe.Kind.OWL_STATUE,
                data.lookup(0x42, RoomTable.INDOORS_A, 0x16).kind());
        assertSame(data.lookup(0x39, RoomTable.INDOORS_A, 0x102), data.lookup(0x39, RoomTable.INDOORS_A, 0x202));
        assertThrows(IllegalArgumentException.class, () -> data.lookup(256, RoomTable.OVERWORLD, 0));
        assertThrows(IllegalArgumentException.class, () -> data.lookup(0, null, 0));
    }

    @Test
    void decoderReadsSyntheticOamWithoutRegistrationOrCatalogState() {
        byte[] bytes = new byte[0x4004];
        bytes[0x4000] = 0x12;
        bytes[0x4001] = 0x20;
        bytes[0x4002] = 0x34;
        bytes[0x4003] = 0x40;
        var decoder = new EntitySpriteDecoder(TestRoms.partial(bytes));
        var recipe = EntitySpriteRecipe.pair(0, 1, 0x4000, 1, 0, EntitySpriteDefinition.DynamicSprite.TileSource.GPU);
        var result = decoder.decode(recipe);
        assertEquals(
                new EntitySpriteDefinition.OamAttribute(0x12, 0x20),
                result.variant(0).first());
        assertEquals(
                new EntitySpriteDefinition.OamAttribute(0x34, 0x40),
                result.variant(0).second());
        assertThrows(IllegalArgumentException.class, () -> decoder.decodePair(0, 1, 0x4000, 2, 0));
    }
}
