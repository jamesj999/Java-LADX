package linksawakening.entity;

import static linksawakening.entity.EntitySpriteHandlerCatalog.*;

import linksawakening.rom.RomAddress;
import linksawakening.world.entity.EntityRoomLoader;

/** Context-dependent FloatingItemEntityHandler and ColorShellBlueEntityHandler display lists; also validates lookup inputs. */
final class EntitySpriteContextualObjectRegistrations {
    /** LiftableStatueSpriteRect_BottleGrotto from azle.sym. */
    private static final RomAddress LIFTABLE_STATUE_SPRITE_RECT_BOTTLE_GROTTO = new RomAddress(0x19, 0x4010);
    /** LiftableStatueSpriteRect from azle.sym. */
    private static final RomAddress LIFTABLE_STATUE_SPRITE_RECT = new RomAddress(0x19, 0x4000);

    private EntitySpriteContextualObjectRegistrations() {}

    static EntitySpriteRecipe lookupFloatingItem(int entityType, EntityRoomLoader.RoomTable roomTable, int mapId) {
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
            return EntitySpriteRecipe.special(EntitySpriteRecipe.Kind.FLOATING_ITEM, entityType);
        }

        if (EntitySpriteGeometry.isColorShellType(entityType)) {
            // Room streams begin in INIT. The bank-$36 renderer uses its
            // inactive four-variant list until the first active handler tick.
            return EntitySpriteRecipe.special(EntitySpriteRecipe.Kind.COLOR_SHELL, entityType);
        }

        if (entityType == 0x9D) {
            return EntitySpriteRecipe.rectangle(
                    entityType,
                    LIFTABLE_STATUE_SPRITE_RECT_BOTTLE_GROTTO.bank(),
                    mapId == 1
                            ? LIFTABLE_STATUE_SPRITE_RECT_BOTTLE_GROTTO.address()
                            : LIFTABLE_STATUE_SPRITE_RECT.address(),
                    1,
                    4,
                    0);
        }
        if (entityType == ENTITY_LIFTABLE_ROCK) {
            return EntitySpriteRecipe.special(EntitySpriteRecipe.Kind.ROCK, entityType);
        }
        if (entityType == 0x41) {
            return EntitySpriteRecipe.special(EntitySpriteRecipe.Kind.OWL, entityType);
        }
        return null;
    }
}
