package linksawakening.entity;

import static linksawakening.entity.EntitySpriteHandlerCatalog.*;

import java.util.List;
import linksawakening.world.entity.EntityRoomLoader;
import linksawakening.world.entity.event.MoldormSegmentPosition;

/** Source-ordered ROM sprite registrations beginning with Moldorm. */
final class EntitySpriteMoldormRegistrations {
    private EntitySpriteMoldormRegistrations() {}

    static EntitySpriteDefinition lookupMoldorm(
            EntitySpriteDecodeState catalog, int entityType, EntityRoomLoader.RoomTable roomTable, int mapId) {
        if (entityType == ENTITY_MOLDORM) {
            return catalog.forMoldormState(
                    0,
                    0,
                    0,
                    List.of(
                            new MoldormSegmentPosition(0, 0),
                            new MoldormSegmentPosition(0, 0),
                            new MoldormSegmentPosition(0, 0),
                            new MoldormSegmentPosition(0, 0)),
                    0,
                    0);
        }
        if (entityType == ENTITY_ROLLING_BONES) {
            return catalog.decodeRectangle(entityType, 0x06, 0x6E1E, 6, 4, 0);
        }
        if (entityType == ENTITY_ROLLING_BONES_BAR) {
            return catalog.forRollingBonesBar(0, 0x38);
        }
        if (entityType == ENTITY_THREE_OF_A_KIND) {
            return catalog.decodePair(entityType, 0x06, 0x4911, 8, 0);
        }
        if (entityType == 0xE4) {
            // func_015_4AC9 indexes fourteen three-sprite body variants at
            // Data_015_4A15. Its separate weapon layer is handler-generated.
            return catalog.decodeRectangle(entityType, 0x15, 0x4A15, 14, 3, 0);
        }
        if (entityType == ENTITY_MASKED_MIMIC_GORIYA && mapId != 0x1F) {
            return catalog.decodePair(entityType, 0x19, 0x4796, 8, 0);
        }
        if (entityType == ENTITY_ANTI_FAIRY) {
            return catalog.decodePair(entityType, 0x06, 0x786E, 2, 0);
        }
        if (entityType == ENTITY_SPARK_COUNTER_CLOCKWISE || entityType == ENTITY_SPARK_CLOCKWISE) {
            return catalog.decodePair(entityType, 0x06, 0x6615, 2, 0);
        }
        if (entityType == ENTITY_ZOL) {
            return catalog.decodePair(entityType, 0x06, 0x7C09, 2, 0);
        }
        if (entityType == ENTITY_GEL) {
            return catalog.decodeSingle(entityType, 0x06, 0x7BFA, 2, 0);
        }
        if (entityType == ENTITY_HIDING_ZOL) {
            return catalog.decodeHidingZol(entityType);
        }
        if (entityType == ENTITY_STAR) {
            return catalog.decodePair(entityType, 0x07, 0x7247, 5, 0);
        }
        return null;
    }
}
