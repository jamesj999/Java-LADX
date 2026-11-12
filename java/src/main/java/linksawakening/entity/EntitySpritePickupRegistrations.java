package linksawakening.entity;

import static linksawakening.entity.EntitySpriteHandlerCatalog.*;

import linksawakening.rom.RomAddress;
import linksawakening.world.entity.EntityRoomLoader;

/** Pickup display-list selection, including ROM {@code SirensInstrument2SpriteVariants}, {@code DroppableHeartSprite}. */
final class EntitySpritePickupRegistrations {
    /** SirensInstrument2SpriteVariants at $03:$5D83. */
    private static final RomAddress SIRENS_INSTRUMENT2_SPRITE_VARIANTS = new RomAddress(0x03, 0x5D83);
    /** DroppableHeartSprite at $03:$5D36. */
    private static final RomAddress DROPPABLE_HEART_SPRITE = new RomAddress(0x03, 0x5D36);
    /** DroppableRupeeSprite at $03:$609C. */
    private static final RomAddress DROPPABLE_RUPEE_SPRITE = new RomAddress(0x03, 0x609C);
    /** data_003_6157 at $03:$6157. */
    private static final RomAddress DATA_003_6157 = new RomAddress(0x03, 0x6157);
    /** KeyDropSpriteTable at $03:$5C78. */
    private static final RomAddress KEY_DROP_SPRITE_TABLE = new RomAddress(0x03, 0x5C78);
    /** Data_003_5B95 at $03:$5B95. */
    private static final RomAddress DATA_003_5B95 = new RomAddress(0x03, 0x5B95);
    /** PieceOfPowerSpriteVariants at $03:$5B65. */
    private static final RomAddress PIECE_OF_POWER_SPRITE_VARIANTS = new RomAddress(0x03, 0x5B65);
    /** Data_003_5B5B at $03:$5B5B. */
    private static final RomAddress DATA_003_5B5B = new RomAddress(0x03, 0x5B5B);
    /** HeartPieceEntitySprite at $03:$5A4D. */
    private static final RomAddress HEART_PIECE_ENTITY_SPRITE = new RomAddress(0x03, 0x5A4D);
    /** Entity13Handler at $03:$59D8. */
    private static final RomAddress ENTITY13_HANDLER = new RomAddress(0x03, 0x59D8);
    /** DroppableArrowSprite at $03:$6079. */
    private static final RomAddress DROPPABLE_ARROW_SPRITE = new RomAddress(0x03, 0x6079);
    /** DroppableBombsSprite at $03:$5FC0. */
    private static final RomAddress DROPPABLE_BOMBS_SPRITE = new RomAddress(0x03, 0x5FC0);
    /** SleepyToadstoolSprite at $03:$5D47. */
    private static final RomAddress SLEEPY_TOADSTOOL_SPRITE = new RomAddress(0x03, 0x5D47);
    /** DroppableMagicPowderSprite at $03:$6055. */
    private static final RomAddress DROPPABLE_MAGIC_POWDER_SPRITE = new RomAddress(0x03, 0x6055);
    /** HidingSlimeKeySprite at $03:$5FFB. */
    private static final RomAddress HIDING_SLIME_KEY_SPRITE = new RomAddress(0x03, 0x5FFB);
    /** DroppableSeashellSprite at $03:$5FD1. */
    private static final RomAddress DROPPABLE_SEASHELL_SPRITE = new RomAddress(0x03, 0x5FD1);

    private EntitySpritePickupRegistrations() {}

    static EntitySpriteRecipe lookup(int entityType, EntityRoomLoader.RoomTable roomTable, int mapId) {
        if (entityType == ENTITY_INSTRUMENT_OF_THE_SIRENS) {
            return EntitySpriteRecipe.pair(
                    entityType,
                    SIRENS_INSTRUMENT2_SPRITE_VARIANTS.bank(),
                    SIRENS_INSTRUMENT2_SPRITE_VARIANTS.address(),
                    4,
                    mapId < 0 ? 0 : mapId & 0x03);
        }
        if (entityType == ENTITY_DROPPABLE_HEART) {
            return EntitySpriteRecipe.single(
                    entityType, DROPPABLE_HEART_SPRITE.bank(), DROPPABLE_HEART_SPRITE.address(), 1, 0);
        }
        if (entityType == ENTITY_DROPPABLE_RUPEE) {
            return EntitySpriteRecipe.single(
                    entityType, DROPPABLE_RUPEE_SPRITE.bank(), DROPPABLE_RUPEE_SPRITE.address(), 1, 0);
        }
        if (entityType == ENTITY_DROPPABLE_FAIRY) {
            return EntitySpriteRecipe.pair(entityType, DATA_003_6157.bank(), DATA_003_6157.address(), 2, 0);
        }
        if (entityType == ENTITY_KEY_DROP_POINT) {
            return EntitySpriteRecipe.single(
                    entityType, KEY_DROP_SPRITE_TABLE.bank(), KEY_DROP_SPRITE_TABLE.address(), 6, 0);
        }
        if (entityType == ENTITY_SWORD_SHIELD_PICKUP) {
            return EntitySpriteRecipe.single(entityType, DATA_003_5B95.bank(), DATA_003_5B95.address(), 1, 0);
        }
        if (entityType == ENTITY_PIECE_OF_POWER) {
            return EntitySpriteRecipe.pair(
                    entityType, PIECE_OF_POWER_SPRITE_VARIANTS.bank(), PIECE_OF_POWER_SPRITE_VARIANTS.address(), 2, 0);
        }
        if (entityType == ENTITY_GUARDIAN_ACORN) {
            return EntitySpriteRecipe.single(entityType, DATA_003_5B5B.bank(), DATA_003_5B5B.address(), 1, 0);
        }
        if (entityType == ENTITY_HEART_PIECE) {
            return EntitySpriteRecipe.pair(
                    entityType, HEART_PIECE_ENTITY_SPRITE.bank(), HEART_PIECE_ENTITY_SPRITE.address(), 1, 0);
        }
        if (entityType == ENTITY_HEART_CONTAINER) {
            return EntitySpriteRecipe.pair(entityType, ENTITY13_HANDLER.bank(), ENTITY13_HANDLER.address(), 1, 0);
        }
        if (entityType == ENTITY_DROPPABLE_ARROWS) {
            return EntitySpriteRecipe.pair(
                    entityType, DROPPABLE_ARROW_SPRITE.bank(), DROPPABLE_ARROW_SPRITE.address(), 1, 0);
        }
        if (entityType == ENTITY_DROPPABLE_BOMBS) {
            return EntitySpriteRecipe.single(
                    entityType, DROPPABLE_BOMBS_SPRITE.bank(), DROPPABLE_BOMBS_SPRITE.address(), 1, 0);
        }
        if (entityType == ENTITY_SLEEPY_TOADSTOOL) {
            return EntitySpriteRecipe.pair(
                    entityType, SLEEPY_TOADSTOOL_SPRITE.bank(), SLEEPY_TOADSTOOL_SPRITE.address(), 1, 0);
        }
        if (entityType == ENTITY_DROPPABLE_MAGIC_POWDER) {
            return EntitySpriteRecipe.single(
                    entityType, DROPPABLE_MAGIC_POWDER_SPRITE.bank(), DROPPABLE_MAGIC_POWDER_SPRITE.address(), 1, 0);
        }
        if (entityType == ENTITY_HIDING_SLIME_KEY) {
            return EntitySpriteRecipe.single(
                    entityType, HIDING_SLIME_KEY_SPRITE.bank(), HIDING_SLIME_KEY_SPRITE.address(), 1, 0);
        }
        if (entityType == ENTITY_DROPPABLE_SECRET_SEASHELL) {
            return EntitySpriteRecipe.single(
                    entityType, DROPPABLE_SEASHELL_SPRITE.bank(), DROPPABLE_SEASHELL_SPRITE.address(), 1, 0);
        }
        return null;
    }
}
