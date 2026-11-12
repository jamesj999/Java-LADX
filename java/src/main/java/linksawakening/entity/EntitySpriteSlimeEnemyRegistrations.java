package linksawakening.entity;

import static linksawakening.entity.EntitySpriteHandlerCatalog.*;

import linksawakening.rom.RomAddress;
import linksawakening.world.entity.EntityRoomLoader;

/** SlimeEnemy display-list selection, including ROM {@code ZolSpriteVariants}, {@code GelSpriteVariants}. */
final class EntitySpriteSlimeEnemyRegistrations {
    /** ZolSpriteVariants at $06:$7C09. */
    private static final RomAddress ZOL_SPRITE_VARIANTS = new RomAddress(0x06, 0x7C09);
    /** GelSpriteVariants at $06:$7BFA. */
    private static final RomAddress GEL_SPRITE_VARIANTS = new RomAddress(0x06, 0x7BFA);
    /** StarSpriteVariants at $07:$7247. */
    private static final RomAddress STAR_SPRITE_VARIANTS = new RomAddress(0x07, 0x7247);

    private EntitySpriteSlimeEnemyRegistrations() {}

    static EntitySpriteRecipe lookup(int entityType, EntityRoomLoader.RoomTable roomTable, int mapId) {
        if (entityType == ENTITY_ZOL) {
            return EntitySpriteRecipe.pair(entityType, ZOL_SPRITE_VARIANTS.bank(), ZOL_SPRITE_VARIANTS.address(), 2, 0);
        }
        if (entityType == ENTITY_GEL) {
            return EntitySpriteRecipe.single(
                    entityType, GEL_SPRITE_VARIANTS.bank(), GEL_SPRITE_VARIANTS.address(), 2, 0);
        }
        if (entityType == ENTITY_HIDING_ZOL) {
            return EntitySpriteRecipe.special(EntitySpriteRecipe.Kind.HIDING_ZOL, entityType);
        }
        if (entityType == ENTITY_STAR) {
            return EntitySpriteRecipe.pair(
                    entityType, STAR_SPRITE_VARIANTS.bank(), STAR_SPRITE_VARIANTS.address(), 5, 0);
        }
        return null;
    }
}
