package linksawakening.entity;

import static linksawakening.entity.EntitySpriteHandlerCatalog.*;

import linksawakening.world.entity.EntityRoomLoader;

/** Source-ordered ROM sprite registrations beginning with SwordShieldPickup. */
final class EntitySpriteSwordShieldPickupRegistrations {
    private EntitySpriteSwordShieldPickupRegistrations() {}

    static EntitySpriteDefinition lookupSwordShieldPickup(
            EntitySpriteDecodeState catalog, int entityType, EntityRoomLoader.RoomTable roomTable, int mapId) {
        if (entityType == ENTITY_SWORD_SHIELD_PICKUP) {
            return catalog.decodeSingle(entityType, 0x03, 0x5B95, 1, 0);
        }
        if (entityType == ENTITY_IRON_MASKS_MASK) {
            return catalog.decodePair(entityType, 0x03, 0x5B80, 2, 0);
        }
        if (entityType == ENTITY_PIECE_OF_POWER) {
            return catalog.decodePair(entityType, 0x03, 0x5B65, 2, 0);
        }
        if (entityType == ENTITY_GUARDIAN_ACORN) {
            return catalog.decodeSingle(entityType, 0x03, 0x5B5B, 1, 0);
        }
        if (entityType == ENTITY_HEART_PIECE) {
            return catalog.decodePair(entityType, 0x03, 0x5A4D, 1, 0);
        }
        if (entityType == ENTITY_HEART_CONTAINER) {
            return catalog.decodePair(entityType, 0x03, 0x59D8, 1, 0);
        }
        if (entityType == ENTITY_DROPPABLE_ARROWS) {
            return catalog.decodePair(entityType, 0x03, 0x6079, 1, 0);
        }
        if (entityType == ENTITY_DROPPABLE_BOMBS) {
            return catalog.decodeSingle(entityType, 0x03, 0x5FC0, 1, 0);
        }
        if (entityType == ENTITY_SLEEPY_TOADSTOOL) {
            return catalog.decodePair(entityType, 0x03, 0x5D47, 1, 0);
        }
        if (entityType == ENTITY_DROPPABLE_MAGIC_POWDER) {
            return catalog.decodeSingle(entityType, 0x03, 0x6055, 1, 0);
        }
        if (entityType == ENTITY_HIDING_SLIME_KEY) {
            return catalog.decodeSingle(entityType, 0x03, 0x5FFB, 1, 0);
        }
        if (entityType == ENTITY_DROPPABLE_SECRET_SEASHELL) {
            return catalog.decodeSingle(entityType, 0x03, 0x5FD1, 1, 0);
        }
        if (entityType == ENTITY_MARIN_AT_THE_SHORE) {
            return catalog.decodePair(entityType, 0x18, 0x5EB7, 8, 0);
        }
        return null;
    }
}
