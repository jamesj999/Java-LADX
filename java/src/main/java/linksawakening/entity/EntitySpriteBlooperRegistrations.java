package linksawakening.entity;

import static linksawakening.entity.EntitySpriteHandlerCatalog.*;

import linksawakening.world.entity.EntityRoomLoader;

/** Source-ordered ROM sprite registrations beginning with Blooper. */
final class EntitySpriteBlooperRegistrations {
    private EntitySpriteBlooperRegistrations() {}

    static EntitySpriteDefinition lookupBlooper(
            EntitySpriteDecodeState catalog, int entityType, EntityRoomLoader.RoomTable roomTable, int mapId) {
        if (entityType == ENTITY_BLOOPER) {
            return catalog.decodePair(entityType, 0x07, 0x5BF1, 2, 0);
        }
        if (entityType == ENTITY_WINGED_OCTOROK) {
            return catalog.decodePair(entityType, 0x07, 0x562D, 8, 0);
        }
        if (entityType == ENTITY_PINCER) {
            return catalog.decodePair(entityType, 0x07, 0x542B, 10, 0);
        }
        if (entityType == ENTITY_STALFOS_AGGRESSIVE) {
            return catalog.decodePair(entityType, 0x06, 0x4AA8, 3, 0);
        }
        if (entityType == ENTITY_STALFOS_EVASIVE) {
            return catalog.decodePair(entityType, 0x15, 0x4E7D, 3, 0);
        }
        if (entityType == ENTITY_GIBDO) {
            return catalog.decodePair(entityType, 0x06, mapId == 0x07 ? 0x7E77 : 0x7E6F, 2, 0);
        }
        if (entityType == ENTITY_LIKE_LIKE) {
            return catalog.decodePair(entityType, 0x06, 0x7DD4, 2, 0);
        }
        if (entityType == ENTITY_PEAHAT) {
            return catalog.decodePair(entityType, 0x07, 0x6701, 2, 0);
        }
        if (entityType == ENTITY_ARMOS_STATUE) {
            return catalog.decodePair(entityType, 0x06, 0x7446, 2, 0);
        }
        if (entityType == ENTITY_ARMOS_KNIGHT) {
            return catalog.decodeRectangle(entityType, 0x06, 0x5523, 4, 8, 0);
        }
        if (entityType == ENTITY_HINOX) {
            int address = mapId == 0x07 ? 0x527A : mapId == 0x06 ? 0x523A : 0x51FA;
            return catalog.decodeRectangle(entityType, 0x06, address, 4, 8, 0);
        }
        if (entityType == ENTITY_HIDING_GHINI || entityType == ENTITY_GHINI) {
            return catalog.decodePair(entityType, 0x04, 0x5BFC, 2, 0);
        }
        if (entityType == ENTITY_GIANT_GHINI) {
            return catalog.decodeRectangle(entityType, 0x04, 0x5D26, 4, 8, 0);
        }
        if (entityType == ENTITY_HARDHAT_BEETLE) {
            return catalog.decodePair(entityType, 0x06, mapId == 0x0A ? 0x4F34 : 0x4F2C, 2, 0);
        }
        return null;
    }
}
