package linksawakening.entity;

import static linksawakening.entity.EntitySpriteHandlerCatalog.*;

import linksawakening.world.entity.EntityRoomLoader;

/** Source-ordered ROM sprite registrations beginning with Kid71. */
final class EntitySpriteKid71Registrations {
    private EntitySpriteKid71Registrations() {}

    static EntitySpriteDefinition lookupKid71(
            EntitySpriteDecodeState catalog, int entityType, EntityRoomLoader.RoomTable roomTable, int mapId) {
        if (entityType == ENTITY_KID_71 || entityType == ENTITY_KID_72) {
            return catalog.decodePair(entityType, 0x06, entityType == ENTITY_KID_71 ? 0x604D : 0x605D, 4, 0);
        }
        if (entityType == ENTITY_MADAM_MEOWMEOW) {
            return catalog.decodePair(entityType, 0x06, 0x5B56, 8, 0);
        }
        if (entityType == 0x75) {
            // GrandmaUlriraEntityHandler $18:4D36/4D58 supplies this pair
            // list before PushLinkOutOfEntity_18 ($18:4D69).
            var variants = linksawakening.rom.RomSymbols.GrandmaUlrira1SpriteVariants;
            return catalog.decodePair(entityType, variants.bank(), variants.address(), 3, 0);
        }
        if (entityType == ENTITY_BUTTERFLY) {
            return catalog.decodeSingle(entityType, 0x06, 0x6BBD, 2, 0);
        }
        if (entityType == ENTITY_KEESE) {
            return catalog.decodePair(entityType, 0x06, mapId == 0x0A ? 0x6710 : 0x6708, 2, 0);
        }
        if (entityType == ENTITY_GOOMBA) {
            return catalog.decodePair(entityType, 0x07, 0x65CE, 3, 0);
        }
        if (entityType == ENTITY_SNAKE) {
            return catalog.decodePair(entityType, 0x07, 0x683E, 4, 0);
        }
        if (entityType == ENTITY_BUSH_CRAWLER) {
            return catalog.decodePair(
                    entityType,
                    0x07,
                    catalog.isIndoorRoomTable(roomTable) ? 0x401A : 0x4012,
                    2,
                    0,
                    EntitySpriteDefinition.DynamicSprite.TileSource.GPU);
        }
        if (entityType == ENTITY_WIZROBE) {
            return catalog.decodePair(entityType, 0x06, 0x7604, 5, 0);
        }
        if (entityType == ENTITY_WIZROBE_PROJECTILE) {
            return catalog.decodePair(entityType, 0x06, 0x65E1, 4, 0);
        }
        if (entityType == ENTITY_OCTOROK) {
            return catalog.decodePair(entityType, 0x03, 0x57FB, 8, 0);
        }
        if (entityType == ENTITY_OCTOROK_ROCK) {
            return catalog.decodePair(entityType, 0x03, 0x6A1E, 2, 0);
        }
        return null;
    }
}
