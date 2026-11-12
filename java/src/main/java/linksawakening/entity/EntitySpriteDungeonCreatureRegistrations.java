package linksawakening.entity;

import static linksawakening.entity.EntitySpriteHandlerCatalog.*;

import linksawakening.rom.RomAddress;
import linksawakening.world.entity.EntityRoomLoader;

/** DungeonCreature display-list selection, including ROM {@code SnakeSpriteVariants}, {@code WizrobeSpriteVariants}. */
final class EntitySpriteDungeonCreatureRegistrations {
    /** KeeseCaveBSpriteVariants from azle.sym. */
    private static final RomAddress KEESE_CAVE_BSPRITE_VARIANTS = new RomAddress(0x6, 0x6710);
    /** KeeseSpriteVariants from azle.sym. */
    private static final RomAddress KEESE_SPRITE_VARIANTS = new RomAddress(0x6, 0x6708);
    /** BouncingBombiteEntityHandler from azle.sym. */
    private static final RomAddress BOUNCING_BOMBITE_ENTITY_HANDLER = new RomAddress(0x4, 0x7e0d);
    /** BouncingBombite1SpriteVariants from azle.sym. */
    private static final RomAddress BOUNCING_BOMBITE1_SPRITE_VARIANTS = new RomAddress(0x4, 0x7df5);
    /** Bombite2SpriteVariants from azle.sym. */
    private static final RomAddress BOMBITE2_SPRITE_VARIANTS = new RomAddress(0x4, 0x7d07);
    /** Bombite1SpriteVariants from azle.sym. */
    private static final RomAddress BOMBITE1_SPRITE_VARIANTS = new RomAddress(0x4, 0x7cef);

    /** SnakeSpriteVariants at $07:$683E. */
    private static final RomAddress SNAKE_SPRITE_VARIANTS = new RomAddress(0x07, 0x683E);
    /** WizrobeSpriteVariants at $06:$7604. */
    private static final RomAddress WIZROBE_SPRITE_VARIANTS = new RomAddress(0x06, 0x7604);
    /** WizrobeProjectileSpriteVariants at $06:$65E1. */
    private static final RomAddress WIZROBE_PROJECTILE_SPRITE_VARIANTS = new RomAddress(0x06, 0x65E1);
    /** MimicSpriteVariants at $19:$6A8D. */
    private static final RomAddress MIMIC_SPRITE_VARIANTS = new RomAddress(0x19, 0x6A8D);
    /** MiniMoldormSpriteVariants at $04:$5A49. */
    private static final RomAddress MINI_MOLDORM_SPRITE_VARIANTS = new RomAddress(0x04, 0x5A49);
    /** LikeLikeSpriteVariants at $06:$7DD4. */
    private static final RomAddress LIKE_LIKE_SPRITE_VARIANTS = new RomAddress(0x06, 0x7DD4);
    /** PolsVoiceSpriteVariants at $06:$7373. */
    private static final RomAddress POLS_VOICE_SPRITE_VARIANTS = new RomAddress(0x06, 0x7373);
    /** PairoddSpriteVariants at $04:$5DD1. */
    private static final RomAddress PAIRODD_SPRITE_VARIANTS = new RomAddress(0x04, 0x5DD1);
    /** PairoddProjectileSpriteVariants at $04:$5EF4. */
    private static final RomAddress PAIRODD_PROJECTILE_SPRITE_VARIANTS = new RomAddress(0x04, 0x5EF4);

    private EntitySpriteDungeonCreatureRegistrations() {}

    static EntitySpriteRecipe lookup(int entityType, EntityRoomLoader.RoomTable roomTable, int mapId) {
        if (entityType == ENTITY_KEESE) {
            return EntitySpriteRecipe.pair(
                    entityType,
                    KEESE_CAVE_BSPRITE_VARIANTS.bank(),
                    mapId == 0x0A ? KEESE_CAVE_BSPRITE_VARIANTS.address() : KEESE_SPRITE_VARIANTS.address(),
                    2,
                    0);
        }
        if (entityType == ENTITY_SNAKE) {
            return EntitySpriteRecipe.pair(
                    entityType, SNAKE_SPRITE_VARIANTS.bank(), SNAKE_SPRITE_VARIANTS.address(), 4, 0);
        }
        if (entityType == ENTITY_WIZROBE) {
            return EntitySpriteRecipe.pair(
                    entityType, WIZROBE_SPRITE_VARIANTS.bank(), WIZROBE_SPRITE_VARIANTS.address(), 5, 0);
        }
        if (entityType == ENTITY_WIZROBE_PROJECTILE) {
            return EntitySpriteRecipe.pair(
                    entityType,
                    WIZROBE_PROJECTILE_SPRITE_VARIANTS.bank(),
                    WIZROBE_PROJECTILE_SPRITE_VARIANTS.address(),
                    4,
                    0);
        }
        if (entityType == ENTITY_BOUNCING_BOMBITE) {
            return EntitySpriteRecipe.pair(
                    entityType,
                    BOUNCING_BOMBITE_ENTITY_HANDLER.bank(),
                    mapId == 0x07
                            ? BOUNCING_BOMBITE_ENTITY_HANDLER.address()
                            : BOUNCING_BOMBITE1_SPRITE_VARIANTS.address(),
                    2,
                    0);
        }
        if (entityType == ENTITY_TIMER_BOMBITE) {
            return EntitySpriteRecipe.pair(
                    entityType,
                    BOMBITE2_SPRITE_VARIANTS.bank(),
                    mapId == 0x07 ? BOMBITE2_SPRITE_VARIANTS.address() : BOMBITE1_SPRITE_VARIANTS.address(),
                    6,
                    0);
        }
        if (entityType == ENTITY_MIMIC) {
            // The eighth pair consumes the $21 byte at Data_019_6AAC,
            // immediately following the source's final commented attribute.
            return EntitySpriteRecipe.pair(
                    entityType, MIMIC_SPRITE_VARIANTS.bank(), MIMIC_SPRITE_VARIANTS.address(), 8, 0);
        }
        if (entityType == ENTITY_MINI_MOLDORM) {
            return EntitySpriteRecipe.pair(
                    entityType, MINI_MOLDORM_SPRITE_VARIANTS.bank(), MINI_MOLDORM_SPRITE_VARIANTS.address(), 10, 0);
        }
        if (entityType == ENTITY_LIKE_LIKE) {
            return EntitySpriteRecipe.pair(
                    entityType, LIKE_LIKE_SPRITE_VARIANTS.bank(), LIKE_LIKE_SPRITE_VARIANTS.address(), 2, 0);
        }
        if (entityType == ENTITY_POLS_VOICE) {
            return EntitySpriteRecipe.pair(
                    entityType, POLS_VOICE_SPRITE_VARIANTS.bank(), POLS_VOICE_SPRITE_VARIANTS.address(), 2, 0);
        }
        if (entityType == ENTITY_PAIRODD) {
            return EntitySpriteRecipe.pair(
                    entityType, PAIRODD_SPRITE_VARIANTS.bank(), PAIRODD_SPRITE_VARIANTS.address(), 8, 0);
        }
        if (entityType == ENTITY_PAIRODD_PROJECTILE) {
            return EntitySpriteRecipe.pair(
                    entityType,
                    PAIRODD_PROJECTILE_SPRITE_VARIANTS.bank(),
                    PAIRODD_PROJECTILE_SPRITE_VARIANTS.address(),
                    2,
                    0);
        }
        return null;
    }
}
