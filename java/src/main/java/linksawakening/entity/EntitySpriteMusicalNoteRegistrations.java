package linksawakening.entity;

import static linksawakening.entity.EntitySpriteHandlerCatalog.*;

import linksawakening.world.entity.EntityRoomLoader;

/** Source-ordered ROM sprite registrations beginning with MusicalNote. */
final class EntitySpriteMusicalNoteRegistrations {
    private EntitySpriteMusicalNoteRegistrations() {}

    static EntitySpriteDefinition lookupMusicalNote(
            EntitySpriteDecodeState catalog, int entityType, EntityRoomLoader.RoomTable roomTable, int mapId) {
        if (entityType == ENTITY_MUSICAL_NOTE) {
            return catalog.decodeSingle(entityType, 0x05, 0x7EF8, 1, 0);
        }
        if (entityType == ENTITY_SWORD_BEAM) {
            return catalog.decodeRectangle(entityType, 0x19, 0x44FC, 4, 2, 0);
        }
        if (entityType == ENTITY_BOMB) {
            return catalog.decodeSingle(entityType, 0x03, 0x652E, 1, 0);
        }
        if (entityType == ENTITY_CROW) {
            return catalog.decodePair(entityType, 0x06, 0x5C89, 4, 2);
        }
        if (entityType == ENTITY_CUCCO) {
            return catalog.decodePair(entityType, 0x05, 0x4514, 4, 0);
        }
        if (entityType == ENTITY_GOPONGA_FLOWER) {
            return catalog.decodePair(entityType, 0x06, 0x63F4, 2, 0);
        }
        if (entityType == ENTITY_GIANT_GOPONGA_FLOWER) {
            return catalog.decodeRectangle(entityType, 0x06, 0x6316, 3, 8, 0);
        }
        if (entityType == ENTITY_GOPONGA_FLOWER_PROJECTILE) {
            return catalog.decodePair(entityType, 0x06, 0x638F, 4, 0);
        }
        if (entityType == ENTITY_POKEY) {
            return catalog.forPokeyState(0);
        }
        if (entityType == ENTITY_PIRANHA_PLANT) {
            return catalog.decodeRectangle(entityType, 0x36, 0x6FC2, 6, 4, 0);
        }
        if (entityType == ENTITY_ZORA) {
            return catalog.decodePair(entityType, 0x18, 0x49C0, 5, 0);
        }
        if (entityType == ENTITY_ZOMBIE) {
            return catalog.decodePair(entityType, 0x18, 0x63F8, 5, 0);
        }
        if (entityType == ENTITY_BUZZ_BLOB) {
            return catalog.decodePair(entityType, 0x18, 0x7729, 9, 0);
        }
        if (entityType == ENTITY_SAND_CRAB) {
            return catalog.decodePair(entityType, 0x15, 0x7320, 2, 0);
        }
        if (entityType == ENTITY_URCHIN) {
            return catalog.forUrchinState(false);
        }
        return null;
    }
}
