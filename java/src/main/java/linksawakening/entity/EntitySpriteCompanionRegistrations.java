package linksawakening.entity;

import static linksawakening.entity.EntitySpriteHandlerCatalog.*;

import linksawakening.rom.RomAddress;
import linksawakening.world.entity.EntityRoomLoader;

/** Companion display-list selection, including ROM {@code CuccoSpriteVariants}, {@code BowWowAtHomeSpriteVariants}. */
final class EntitySpriteCompanionRegistrations {
    /** CuccoSpriteVariants at $05:$4514. */
    private static final RomAddress CUCCO_SPRITE_VARIANTS = new RomAddress(0x05, 0x4514);
    /** BowWowAtHomeSpriteVariants at $05:$4000. */
    private static final RomAddress BOW_WOW_AT_HOME_SPRITE_VARIANTS = new RomAddress(0x05, 0x4000);
    /** DogSpriteVariants at $19:$48CA. */
    private static final RomAddress DOG_SPRITE_VARIANTS = new RomAddress(0x19, 0x48CA);
    /** ButterflyOAMAttributes at $06:$6BBD. */
    private static final RomAddress BUTTERFLY_OAMATTRIBUTES = new RomAddress(0x06, 0x6BBD);

    private EntitySpriteCompanionRegistrations() {}

    static EntitySpriteRecipe lookup(int entityType, EntityRoomLoader.RoomTable roomTable, int mapId) {
        if (entityType == ENTITY_CUCCO) {
            return EntitySpriteRecipe.pair(
                    entityType, CUCCO_SPRITE_VARIANTS.bank(), CUCCO_SPRITE_VARIANTS.address(), 4, 0);
        }
        if (entityType == ENTITY_BOW_WOW) {
            return EntitySpriteRecipe.pair(
                    entityType,
                    BOW_WOW_AT_HOME_SPRITE_VARIANTS.bank(),
                    BOW_WOW_AT_HOME_SPRITE_VARIANTS.address(),
                    7,
                    0);
        }
        if (entityType == ENTITY_DOG) {
            return EntitySpriteRecipe.pair(entityType, DOG_SPRITE_VARIANTS.bank(), DOG_SPRITE_VARIANTS.address(), 4, 2);
        }
        if (entityType == ENTITY_BUTTERFLY) {
            return EntitySpriteRecipe.single(
                    entityType, BUTTERFLY_OAMATTRIBUTES.bank(), BUTTERFLY_OAMATTRIBUTES.address(), 2, 0);
        }
        return null;
    }
}
