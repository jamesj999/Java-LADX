package linksawakening.entity;

import static linksawakening.entity.EntitySpriteHandlerCatalog.*;

import java.util.List;
import linksawakening.world.entity.EntityRoomLoader;

/** Source-ordered ROM sprite registrations beginning with Sheets. */
final class EntitySpriteSheetsRegistrations {
    private EntitySpriteSheetsRegistrations() {}

    static EntitySpriteDefinition lookupSheets(
            EntitySpriteDecodeState catalog, int entityType, EntityRoomLoader.RoomTable roomTable, int mapId) {
        if (entityType == 0x42 && mapId != 0x16) {
            // Ordinary owl: hActiveEntityVisualPosY + 1, not a world-position change.
            // South Face Shrine instead uses the separate four-sprite mural handler.
            EntitySpriteDefinition.Variant pair =
                    catalog.decodePair(entityType, 0x18, 0x5DF7, 1, 0).variant(0);
            return EntitySpriteDefinition.dynamic(
                    entityType,
                    0x18,
                    0x5DF7,
                    0,
                    List.of(List.of(
                            new EntitySpriteDefinition.DynamicSprite(
                                    1,
                                    0,
                                    pair.first(),
                                    EntitySpriteDefinition.DynamicSprite.TileSource.ENTITY_SHEETS,
                                    true),
                            new EntitySpriteDefinition.DynamicSprite(
                                    1,
                                    8,
                                    pair.second(),
                                    EntitySpriteDefinition.DynamicSprite.TileSource.ENTITY_SHEETS,
                                    true))));
        }
        if (entityType == ENTITY_CHEST_WITH_ITEM) {
            return catalog.decodeSingle(entityType, 0x07, 0x7B57, 0x21, 0);
        }
        if (entityType == 0x06) {
            // Unknown011SpriteVariants, used by the transient pushed block.
            return catalog.decodePair(entityType, 0x03, 0x5235, 2, 1);
        }
        if (entityType == 0x61 && catalog.isIndoorRoomTable(roomTable)) {
            return catalog.decodeDungeonWarp();
        }
        if (entityType == ENTITY_INSTRUMENT_OF_THE_SIRENS) {
            return catalog.decodePair(entityType, 0x03, 0x5D83, 4, mapId < 0 ? 0 : mapId & 0x03);
        }
        if (entityType == 0x5B) return catalog.forSlimeEyeState(false, 0, 0);
        if (entityType == ENTITY_ARROW) {
            return catalog.decodePair(entityType, 0x03, 0x6BC6, 4, 0);
        }
        if (entityType == ENTITY_BOOMERANG) {
            return catalog.decodePair(entityType, 0x19, 0x4451, 4, 0);
        }
        if (entityType == ENTITY_MAGIC_ROD_FIREBALL) {
            return catalog.decodePair(entityType, 0x03, 0x69AA, 2, 0);
        }
        if (entityType == ENTITY_MAGIC_POWDER_SPRINKLE) {
            return catalog.decodeRectangle(entityType, 0x18, 0x7ABA, 8, 3, 0);
        }
        return null;
    }
}
