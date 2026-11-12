package linksawakening.entity;

import static linksawakening.entity.EntitySpriteHandlerCatalog.*;

import linksawakening.rom.RomAddress;
import linksawakening.world.entity.EntityRoomLoader;

/** UndeadEnemy display-list selection, including ROM {@code ZombieSpriteVariants}, {@code BooBuddyEntityHandler}. */
final class EntitySpriteUndeadEnemyRegistrations {
    /** GibdoTurtleRockSpriteVariants from azle.sym. */
    private static final RomAddress GIBDO_TURTLE_ROCK_SPRITE_VARIANTS = new RomAddress(0x6, 0x7e77);
    /** GibdoSpriteVariants from azle.sym. */
    private static final RomAddress GIBDO_SPRITE_VARIANTS = new RomAddress(0x6, 0x7e6f);

    /** ZombieSpriteVariants at $18:$63F8. */
    private static final RomAddress ZOMBIE_SPRITE_VARIANTS = new RomAddress(0x18, 0x63F8);
    /** BooBuddyEntityHandler at $06:$79A9. */
    private static final RomAddress BOO_BUDDY_ENTITY_HANDLER = new RomAddress(0x06, 0x79A9);
    /** StalfosAggressiveSpriteVariants at $06:$4AA8. */
    private static final RomAddress STALFOS_AGGRESSIVE_SPRITE_VARIANTS = new RomAddress(0x06, 0x4AA8);
    /** StalfosEvasive2SpriteVariants at $15:$4E7D. */
    private static final RomAddress STALFOS_EVASIVE2_SPRITE_VARIANTS = new RomAddress(0x15, 0x4E7D);
    /** GhiniSpriteVariants at $04:$5BFC. */
    private static final RomAddress GHINI_SPRITE_VARIANTS = new RomAddress(0x04, 0x5BFC);
    /** GiantGhiniSpriteRectVariants at $04:$5D26. */
    private static final RomAddress GIANT_GHINI_SPRITE_RECT_VARIANTS = new RomAddress(0x04, 0x5D26);

    private EntitySpriteUndeadEnemyRegistrations() {}

    static EntitySpriteRecipe lookup(int entityType, EntityRoomLoader.RoomTable roomTable, int mapId) {
        if (entityType == ENTITY_ZOMBIE) {
            return EntitySpriteRecipe.pair(
                    entityType, ZOMBIE_SPRITE_VARIANTS.bank(), ZOMBIE_SPRITE_VARIANTS.address(), 5, 0);
        }
        if (entityType == ENTITY_BOO_BUDDY) {
            return EntitySpriteRecipe.pair(
                    entityType, BOO_BUDDY_ENTITY_HANDLER.bank(), BOO_BUDDY_ENTITY_HANDLER.address(), 8, 0);
        }
        if (entityType == ENTITY_STALFOS_AGGRESSIVE) {
            return EntitySpriteRecipe.pair(
                    entityType,
                    STALFOS_AGGRESSIVE_SPRITE_VARIANTS.bank(),
                    STALFOS_AGGRESSIVE_SPRITE_VARIANTS.address(),
                    3,
                    0);
        }
        if (entityType == ENTITY_STALFOS_EVASIVE) {
            return EntitySpriteRecipe.pair(
                    entityType,
                    STALFOS_EVASIVE2_SPRITE_VARIANTS.bank(),
                    STALFOS_EVASIVE2_SPRITE_VARIANTS.address(),
                    3,
                    0);
        }
        if (entityType == ENTITY_GIBDO) {
            return EntitySpriteRecipe.pair(
                    entityType,
                    GIBDO_TURTLE_ROCK_SPRITE_VARIANTS.bank(),
                    mapId == 0x07 ? GIBDO_TURTLE_ROCK_SPRITE_VARIANTS.address() : GIBDO_SPRITE_VARIANTS.address(),
                    2,
                    0);
        }
        if (entityType == ENTITY_HIDING_GHINI || entityType == ENTITY_GHINI) {
            return EntitySpriteRecipe.pair(
                    entityType, GHINI_SPRITE_VARIANTS.bank(), GHINI_SPRITE_VARIANTS.address(), 2, 0);
        }
        if (entityType == ENTITY_GIANT_GHINI) {
            return EntitySpriteRecipe.rectangle(
                    entityType,
                    GIANT_GHINI_SPRITE_RECT_VARIANTS.bank(),
                    GIANT_GHINI_SPRITE_RECT_VARIANTS.address(),
                    4,
                    8,
                    0);
        }
        return null;
    }
}
