package linksawakening.entity;

import static linksawakening.entity.EntitySpriteHandlerCatalog.*;

import linksawakening.world.entity.EntityRoomLoader;

/** Source-ordered ROM sprite registrations beginning with PolsVoice. */
final class EntitySpritePolsVoiceRegistrations {
    private EntitySpritePolsVoiceRegistrations() {}

    static EntitySpriteDefinition lookupPolsVoice(
            EntitySpriteDecodeState catalog, int entityType, EntityRoomLoader.RoomTable roomTable, int mapId) {
        if (entityType == ENTITY_POLS_VOICE) {
            return catalog.decodePair(entityType, 0x06, 0x7373, 2, 0);
        }
        if (entityType == ENTITY_SPIKED_BEETLE) {
            return catalog.decodePair(entityType, 0x07, mapId == 0x03 ? 0x7794 : 0x7784, 4, 0);
        }
        if (entityType == ENTITY_LASER) {
            return catalog.decodePair(entityType, 0x04, 0x6C2D, 8, 0);
        }
        if (entityType == ENTITY_LASER_BEAM) {
            // LaserBeamEntityHandler never renders a normal entity sprite;
            // its visible pixels are transient VFX $06 from bank $02.
            return EntitySpriteDefinition.unsupported(entityType);
        }
        if (entityType == ENTITY_SPIKE_TRAP) {
            return catalog.decodePair(entityType, 0x06, 0x74FA, 1, 0);
        }
        if (entityType == ENTITY_PAIRODD) {
            return catalog.decodePair(entityType, 0x04, 0x5DD1, 8, 0);
        }
        if (entityType == ENTITY_PAIRODD_PROJECTILE) {
            return catalog.decodePair(entityType, 0x04, 0x5EF4, 2, 0);
        }
        if (entityType == ENTITY_WATER_TEKTITE) {
            return catalog.decodePair(entityType, 0x07, 0x752D, 2, 0);
        }
        if (entityType == ENTITY_FISH) {
            return catalog.decodeFish(entityType);
        }
        if (entityType == ENTITY_GRANDPA_ULRIRA) {
            return catalog.decodeRectangle(entityType, 0x06, 0x5C51, 2, 4, 0);
        }
        if (entityType == ENTITY_DROPPABLE_HEART) {
            return catalog.decodeSingle(entityType, 0x03, 0x5D36, 1, 0);
        }
        if (entityType == ENTITY_DROPPABLE_RUPEE) {
            return catalog.decodeSingle(entityType, 0x03, 0x609C, 1, 0);
        }
        if (entityType == ENTITY_DROPPABLE_FAIRY) {
            return catalog.decodePair(entityType, 0x03, 0x6157, 2, 0);
        }
        if (entityType == ENTITY_KEY_DROP_POINT) {
            return catalog.decodeSingle(entityType, 0x03, 0x5C78, 6, 0);
        }
        return null;
    }
}
