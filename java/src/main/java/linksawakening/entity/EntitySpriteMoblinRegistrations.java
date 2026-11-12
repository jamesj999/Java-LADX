package linksawakening.entity;

import static linksawakening.entity.EntitySpriteHandlerCatalog.*;

import linksawakening.world.entity.EntityRoomLoader;

/** Source-ordered ROM sprite registrations beginning with Moblin. */
final class EntitySpriteMoblinRegistrations {
    private EntitySpriteMoblinRegistrations() {}

    static EntitySpriteDefinition lookupMoblin(
            EntitySpriteDecodeState catalog, int entityType, EntityRoomLoader.RoomTable roomTable, int mapId) {
        if (entityType == ENTITY_MOBLIN) {
            return catalog.decodePair(entityType, 0x03, 0x5917, 8, 0);
        }
        if (entityType == ENTITY_IRON_MASK) {
            return catalog.forIronMaskState(0);
        }
        if (entityType == ENTITY_BOMBER) {
            return catalog.decodeRectangle(entityType, 0x18, 0x77ED, 4, 3, 0);
        }
        if (entityType == ENTITY_MAD_BOMBER) {
            return catalog.decodePair(entityType, 0x06, 0x4126, 5, 0);
        }
        if (entityType == ENTITY_BOUNCING_BOMBITE) {
            return catalog.decodePair(entityType, 0x04, mapId == 0x07 ? 0x7E0D : 0x7DF5, 2, 0);
        }
        if (entityType == ENTITY_TIMER_BOMBITE) {
            return catalog.decodePair(entityType, 0x04, mapId == 0x07 ? 0x7D07 : 0x7CEF, 6, 0);
        }
        if (entityType == ENTITY_MOBLIN_SWORD) {
            return catalog.decodeMoblinSword(entityType);
        }
        if (entityType == ENTITY_MOBLIN_ARROW) {
            return catalog.decodePair(entityType, 0x03, 0x6BC6, 4, 0);
        }
        if (entityType == ENTITY_TEKTITE) {
            return catalog.decodePair(entityType, 0x06, 0x78B7, 2, 0);
        }
        if (entityType == ENTITY_LEEVER) {
            return catalog.decodePair(entityType, 0x04, 0x7EE5, 4, 0);
        }
        if (entityType == ENTITY_MIMIC) {
            // The eighth pair consumes the $21 byte at Data_019_6AAC,
            // immediately following the source's final commented attribute.
            return catalog.decodePair(entityType, 0x19, 0x6A8D, 8, 0);
        }
        if (entityType == ENTITY_MINI_MOLDORM) {
            return catalog.decodePair(entityType, 0x04, 0x5A49, 10, 0);
        }
        return null;
    }
}
