package linksawakening.entity;

import static linksawakening.entity.EntitySpriteHandlerCatalog.*;

import linksawakening.world.entity.EntityRoomLoader;
import linksawakening.world.entity.EntityStatus;

/** Source-ordered ROM sprite registrations beginning with FloatingItem. */
final class EntitySpriteFloatingItemRegistrations {
    private EntitySpriteFloatingItemRegistrations() {}

    static EntitySpriteDefinition lookupFloatingItem(
            EntitySpriteDecodeState catalog, int entityType, EntityRoomLoader.RoomTable roomTable, int mapId) {
        if ((entityType & ~0xFF) != 0) {
            throw new IllegalArgumentException("Entity type must be an unsigned byte: " + entityType);
        }
        if (roomTable == null) {
            throw new IllegalArgumentException("Room entity table cannot be null");
        }

        // Color Dungeon's $86 handler is bank-$36 func_036_4F9B, not the
        // ordinary bank-$06 floating-item display path. That branch remains
        // intentionally deferred until its dedicated renderer is modeled.
        if ((entityType == ENTITY_FLOATING_ITEM || entityType == ENTITY_FLOATING_ITEM_2)
                && !(roomTable == EntityRoomLoader.RoomTable.COLOR_DUNGEON && entityType == ENTITY_FLOATING_ITEM)) {
            return catalog.decodeFloatingItemMain(entityType);
        }

        if (catalog.isColorShellType(entityType)) {
            // Room streams begin in INIT. The bank-$36 renderer uses its
            // inactive four-variant list until the first active handler tick.
            return catalog.forColorShellState(entityType, 0, EntityStatus.INIT);
        }

        if (entityType == 0x9D) {
            return catalog.decodeRectangle(entityType, 0x19, mapId == 1 ? 0x4010 : 0x4000, 1, 4, 0);
        }
        if (entityType == ENTITY_LIFTABLE_ROCK) {
            return catalog.decodeLiftableRock(roomTable);
        }
        if (entityType == 0x41) {
            return catalog.forOwlEvent(false);
        }
        return null;
    }
}
