package linksawakening.entity;

import static linksawakening.entity.EntitySpriteHandlerCatalog.*;

import linksawakening.rom.RomAddress;
import linksawakening.world.entity.EntityRoomLoader;

/** Hazard display-list selection, including ROM {@code Data_015_4A15}, {@code MaskedMimicGoriyaSpriteVariants}. */
final class EntitySpriteHazardRegistrations {
    /** Data_015_4A15 at $15:$4A15. */
    private static final RomAddress DATA_015_4A15 = new RomAddress(0x15, 0x4A15);
    /** MaskedMimicGoriyaSpriteVariants at $19:$4796. */
    private static final RomAddress MASKED_MIMIC_GORIYA_SPRITE_VARIANTS = new RomAddress(0x19, 0x4796);
    /** AntiFairySpriteVariants at $06:$786E. */
    private static final RomAddress ANTI_FAIRY_SPRITE_VARIANTS = new RomAddress(0x06, 0x786E);
    /** SparkSpriteVariants at $06:$6615. */
    private static final RomAddress SPARK_SPRITE_VARIANTS = new RomAddress(0x06, 0x6615);
    /** LaserSpriteVariants at $04:$6C2D. */
    private static final RomAddress LASER_SPRITE_VARIANTS = new RomAddress(0x04, 0x6C2D);
    /** SpikeTrapSpriteVariants at $06:$74FA. */
    private static final RomAddress SPIKE_TRAP_SPRITE_VARIANTS = new RomAddress(0x06, 0x74FA);

    private EntitySpriteHazardRegistrations() {}

    static EntitySpriteRecipe lookup(int entityType, EntityRoomLoader.RoomTable roomTable, int mapId) {
        if (entityType == 0xE4) {
            // func_015_4AC9 indexes fourteen three-sprite body variants at
            // Data_015_4A15. Its separate weapon layer is handler-generated.
            return EntitySpriteRecipe.rectangle(entityType, DATA_015_4A15.bank(), DATA_015_4A15.address(), 14, 3, 0);
        }
        if (entityType == ENTITY_MASKED_MIMIC_GORIYA && mapId != 0x1F) {
            return EntitySpriteRecipe.pair(
                    entityType,
                    MASKED_MIMIC_GORIYA_SPRITE_VARIANTS.bank(),
                    MASKED_MIMIC_GORIYA_SPRITE_VARIANTS.address(),
                    8,
                    0);
        }
        if (entityType == ENTITY_ANTI_FAIRY) {
            return EntitySpriteRecipe.pair(
                    entityType, ANTI_FAIRY_SPRITE_VARIANTS.bank(), ANTI_FAIRY_SPRITE_VARIANTS.address(), 2, 0);
        }
        if (entityType == ENTITY_SPARK_COUNTER_CLOCKWISE || entityType == ENTITY_SPARK_CLOCKWISE) {
            return EntitySpriteRecipe.pair(
                    entityType, SPARK_SPRITE_VARIANTS.bank(), SPARK_SPRITE_VARIANTS.address(), 2, 0);
        }
        if (entityType == ENTITY_LASER) {
            return EntitySpriteRecipe.pair(
                    entityType, LASER_SPRITE_VARIANTS.bank(), LASER_SPRITE_VARIANTS.address(), 8, 0);
        }
        if (entityType == ENTITY_LASER_BEAM) {
            // LaserBeamEntityHandler never renders a normal entity sprite;
            // its visible pixels are transient VFX $06 from bank $02.
            return EntitySpriteRecipe.special(EntitySpriteRecipe.Kind.UNSUPPORTED, entityType);
        }
        if (entityType == ENTITY_SPIKE_TRAP) {
            return EntitySpriteRecipe.pair(
                    entityType, SPIKE_TRAP_SPRITE_VARIANTS.bank(), SPIKE_TRAP_SPRITE_VARIANTS.address(), 1, 0);
        }
        return null;
    }
}
