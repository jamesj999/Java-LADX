package linksawakening.entity;

import static linksawakening.entity.EntitySpriteHandlerCatalog.*;

import linksawakening.rom.RomAddress;
import linksawakening.world.entity.EntityRoomLoader;

/** ArmoredEnemy display-list selection, including ROM {@code MoblinSpriteVariants}, {@code Data_018_77ED}. */
final class EntitySpriteArmoredEnemyRegistrations {
    /** HardHatBeetleCaveBSpriteVariants from azle.sym. */
    private static final RomAddress HARD_HAT_BEETLE_CAVE_BSPRITE_VARIANTS = new RomAddress(0x6, 0x4f34);
    /** HardHatBeetleSpriteVariants from azle.sym. */
    private static final RomAddress HARD_HAT_BEETLE_SPRITE_VARIANTS = new RomAddress(0x6, 0x4f2c);
    /** SpikedBeetle2SpriteVariants from azle.sym. */
    private static final RomAddress SPIKED_BEETLE2_SPRITE_VARIANTS = new RomAddress(0x7, 0x7794);
    /** SpikedBeetle1SpriteVariants from azle.sym. */
    private static final RomAddress SPIKED_BEETLE1_SPRITE_VARIANTS = new RomAddress(0x7, 0x7784);

    /** MoblinSpriteVariants at $03:$5917. */
    private static final RomAddress MOBLIN_SPRITE_VARIANTS = new RomAddress(0x03, 0x5917);
    /** Data_018_77ED at $18:$77ED. */
    private static final RomAddress DATA_018_77ED = new RomAddress(0x18, 0x77ED);
    /** MadBomberSpriteVariants at $06:$4126. */
    private static final RomAddress MAD_BOMBER_SPRITE_VARIANTS = new RomAddress(0x06, 0x4126);
    /** EntityArrowSpriteVariants at $03:$6BC6. */
    private static final RomAddress ENTITY_ARROW_SPRITE_VARIANTS = new RomAddress(0x03, 0x6BC6);
    /** ArmosStatueSpriteVariants at $06:$7446. */
    private static final RomAddress ARMOS_STATUE_SPRITE_VARIANTS = new RomAddress(0x06, 0x7446);
    /** IronMasksMaskSpriteVariants at $03:$5B80. */
    private static final RomAddress IRON_MASKS_MASK_SPRITE_VARIANTS = new RomAddress(0x03, 0x5B80);

    private EntitySpriteArmoredEnemyRegistrations() {}

    static EntitySpriteDefinition lookup(
            EntitySpriteDecodeState catalog, int entityType, EntityRoomLoader.RoomTable roomTable, int mapId) {
        if (entityType == ENTITY_MOBLIN) {
            return catalog.decodePair(
                    entityType, MOBLIN_SPRITE_VARIANTS.bank(), MOBLIN_SPRITE_VARIANTS.address(), 8, 0);
        }
        if (entityType == ENTITY_IRON_MASK) {
            return catalog.forIronMaskState(0);
        }
        if (entityType == ENTITY_BOMBER) {
            return catalog.decodeRectangle(entityType, DATA_018_77ED.bank(), DATA_018_77ED.address(), 4, 3, 0);
        }
        if (entityType == ENTITY_MAD_BOMBER) {
            return catalog.decodePair(
                    entityType, MAD_BOMBER_SPRITE_VARIANTS.bank(), MAD_BOMBER_SPRITE_VARIANTS.address(), 5, 0);
        }
        if (entityType == ENTITY_MOBLIN_SWORD) {
            return catalog.decodeMoblinSword(entityType);
        }
        if (entityType == ENTITY_MOBLIN_ARROW) {
            return catalog.decodePair(
                    entityType, ENTITY_ARROW_SPRITE_VARIANTS.bank(), ENTITY_ARROW_SPRITE_VARIANTS.address(), 4, 0);
        }
        if (entityType == ENTITY_ARMOS_STATUE) {
            return catalog.decodePair(
                    entityType, ARMOS_STATUE_SPRITE_VARIANTS.bank(), ARMOS_STATUE_SPRITE_VARIANTS.address(), 2, 0);
        }
        if (entityType == ENTITY_HARDHAT_BEETLE) {
            return catalog.decodePair(
                    entityType,
                    HARD_HAT_BEETLE_CAVE_BSPRITE_VARIANTS.bank(),
                    mapId == 0x0A
                            ? HARD_HAT_BEETLE_CAVE_BSPRITE_VARIANTS.address()
                            : HARD_HAT_BEETLE_SPRITE_VARIANTS.address(),
                    2,
                    0);
        }
        if (entityType == ENTITY_SPIKED_BEETLE) {
            return catalog.decodePair(
                    entityType,
                    SPIKED_BEETLE2_SPRITE_VARIANTS.bank(),
                    mapId == 0x03 ? SPIKED_BEETLE2_SPRITE_VARIANTS.address() : SPIKED_BEETLE1_SPRITE_VARIANTS.address(),
                    4,
                    0);
        }
        if (entityType == ENTITY_IRON_MASKS_MASK) {
            return catalog.decodePair(
                    entityType,
                    IRON_MASKS_MASK_SPRITE_VARIANTS.bank(),
                    IRON_MASKS_MASK_SPRITE_VARIANTS.address(),
                    2,
                    0);
        }
        return null;
    }
}
