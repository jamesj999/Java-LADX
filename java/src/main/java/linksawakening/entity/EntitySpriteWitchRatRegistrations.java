package linksawakening.entity;

import static linksawakening.entity.EntitySpriteHandlerCatalog.*;

import java.util.List;
import linksawakening.world.entity.EntityRoomLoader;

/** Source-ordered ROM sprite registrations beginning with WitchRat. */
final class EntitySpriteWitchRatRegistrations {
    private EntitySpriteWitchRatRegistrations() {}

    static EntitySpriteDefinition lookupWitchRat(
            EntitySpriteDecodeState catalog, int entityType, EntityRoomLoader.RoomTable roomTable, int mapId) {
        if (entityType == ENTITY_WITCH_RAT) {
            return catalog.decodePair(entityType, 0x15, 0x788D, 4, 0);
        }
        if (entityType == ENTITY_WITCH) {
            return catalog.decodeRectangle(entityType, 0x05, 0x4780, 4, 4, 0);
        }
        if (entityType == ENTITY_BOO_BUDDY) {
            return catalog.decodePair(entityType, 0x06, 0x79A9, 8, 0);
        }
        if (entityType == ENTITY_CRYSTAL_SWITCH) {
            return catalog.decodePair(entityType, 0x15, 0x4320, 1, 0);
        }
        if (entityType == ENTITY_HOOKSHOT_CHAIN) {
            // HookshotChainSpriteVariants is an inline handler list in
            // entities/18_hookshot_chain.asm, not a bank display-list label.
            return new EntitySpriteDefinition(
                    entityType,
                    -1,
                    -1,
                    EntitySpriteDefinition.Shape.PAIR,
                    0,
                    List.of(new EntitySpriteDefinition.Variant(
                            new EntitySpriteDefinition.OamAttribute(0x36, 0x00),
                            new EntitySpriteDefinition.OamAttribute(0x36, 0x20))));
        }
        if (entityType == ENTITY_BOW_WOW) {
            return catalog.decodePair(entityType, 0x05, 0x4000, 7, 0);
        }
        if (entityType == ENTITY_DOG) {
            return catalog.decodePair(entityType, 0x19, 0x48CA, 4, 2);
        }
        if (entityType == ENTITY_MARIN) {
            return roomTable == EntityRoomLoader.RoomTable.INDOORS_A
                            || roomTable == EntityRoomLoader.RoomTable.INDOORS_B
                    ? catalog.decodePair(entityType, 0x05, 0x4E0A, 8, 6)
                    : catalog.decodePair(entityType, 0x05, 0x4E2A, 11, 6);
        }
        if (entityType == ENTITY_TARIN) {
            return roomTable == EntityRoomLoader.RoomTable.INDOORS_A
                            || roomTable == EntityRoomLoader.RoomTable.INDOORS_B
                    ? catalog.decodePair(entityType, 0x05, 0x4932, 4, 0)
                    : catalog.decodePair(entityType, 0x05, 0x4912, 12, 0);
        }
        if (entityType == ENTITY_MARIN_AT_TAL_TAL_HEIGHTS) {
            return catalog.decodePair(entityType, 0x18, 0x5EB7, 8, 0);
        }
        if (entityType == ENTITY_KID_70 || entityType == ENTITY_KID_73) {
            return catalog.decodePair(entityType, 0x06, 0x604D, 4, 0);
        }
        return null;
    }
}
