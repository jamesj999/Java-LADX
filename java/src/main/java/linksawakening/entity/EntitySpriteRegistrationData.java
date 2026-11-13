package linksawakening.entity;

import static linksawakening.entity.EntitySpriteHandlerCatalog.*;

import linksawakening.rom.RomAddress;
import linksawakening.rom.RomSymbols;
import linksawakening.world.entity.EntityRoomLoader;

/** Immutable family registration recipes. Lookup preserves contextual priority and all map selector branches. */
final class EntitySpriteRegistrationData {
    /** ZoraSpriteVariants at $18:$49C0. */
    private static final RomAddress AQUATIC_ZORA_SPRITE_VARIANTS = new RomAddress(0x18, 0x49C0);
    /** SandCrabSpriteVariants at $15:$7320. */
    private static final RomAddress AQUATIC_SAND_CRAB_SPRITE_VARIANTS = new RomAddress(0x15, 0x7320);
    /** BlooperSpriteVariants at $07:$5BF1. */
    private static final RomAddress AQUATIC_BLOOPER_SPRITE_VARIANTS = new RomAddress(0x07, 0x5BF1);
    /** WaterTektiteSpriteVariants at $07:$752D. */
    private static final RomAddress AQUATIC_WATER_TEKTITE_SPRITE_VARIANTS = new RomAddress(0x07, 0x752D);

    private static EntitySpriteRecipe aquaticLookup(int entityType, EntityRoomLoader.RoomTable roomTable, int mapId) {
        if (entityType == ENTITY_ZORA) {
            return EntitySpriteRecipe.pair(
                    entityType, AQUATIC_ZORA_SPRITE_VARIANTS.bank(), AQUATIC_ZORA_SPRITE_VARIANTS.address(), 5, 0);
        }
        if (entityType == ENTITY_SAND_CRAB) {
            return EntitySpriteRecipe.pair(
                    entityType,
                    AQUATIC_SAND_CRAB_SPRITE_VARIANTS.bank(),
                    AQUATIC_SAND_CRAB_SPRITE_VARIANTS.address(),
                    2,
                    0);
        }
        if (entityType == ENTITY_URCHIN) {
            return EntitySpriteRecipe.special(EntitySpriteRecipe.Kind.URCHIN, entityType);
        }
        if (entityType == ENTITY_BLOOPER) {
            return EntitySpriteRecipe.pair(
                    entityType,
                    AQUATIC_BLOOPER_SPRITE_VARIANTS.bank(),
                    AQUATIC_BLOOPER_SPRITE_VARIANTS.address(),
                    2,
                    0);
        }
        if (entityType == ENTITY_WATER_TEKTITE) {
            return EntitySpriteRecipe.pair(
                    entityType,
                    AQUATIC_WATER_TEKTITE_SPRITE_VARIANTS.bank(),
                    AQUATIC_WATER_TEKTITE_SPRITE_VARIANTS.address(),
                    2,
                    0);
        }
        if (entityType == ENTITY_FISH) {
            return EntitySpriteRecipe.special(EntitySpriteRecipe.Kind.FISH, entityType);
        }
        return null;
    }

    /** HardHatBeetleCaveBSpriteVariants from azle.sym. */
    private static final RomAddress ARMOREDENEMY_HARD_HAT_BEETLE_CAVE_BSPRITE_VARIANTS = new RomAddress(0x6, 0x4f34);
    /** HardHatBeetleSpriteVariants from azle.sym. */
    private static final RomAddress ARMOREDENEMY_HARD_HAT_BEETLE_SPRITE_VARIANTS = new RomAddress(0x6, 0x4f2c);
    /** SpikedBeetle2SpriteVariants from azle.sym. */
    private static final RomAddress ARMOREDENEMY_SPIKED_BEETLE2_SPRITE_VARIANTS = new RomAddress(0x7, 0x7794);
    /** SpikedBeetle1SpriteVariants from azle.sym. */
    private static final RomAddress ARMOREDENEMY_SPIKED_BEETLE1_SPRITE_VARIANTS = new RomAddress(0x7, 0x7784);

    /** MoblinSpriteVariants at $03:$5917. */
    private static final RomAddress ARMOREDENEMY_MOBLIN_SPRITE_VARIANTS = new RomAddress(0x03, 0x5917);
    /** Data_018_77ED at $18:$77ED. */
    private static final RomAddress ARMOREDENEMY_DATA_018_77ED = new RomAddress(0x18, 0x77ED);
    /** MadBomberSpriteVariants at $06:$4126. */
    private static final RomAddress ARMOREDENEMY_MAD_BOMBER_SPRITE_VARIANTS = new RomAddress(0x06, 0x4126);
    /** EntityArrowSpriteVariants at $03:$6BC6. */
    private static final RomAddress ARMOREDENEMY_ENTITY_ARROW_SPRITE_VARIANTS = new RomAddress(0x03, 0x6BC6);
    /** ArmosStatueSpriteVariants at $06:$7446. */
    private static final RomAddress ARMOREDENEMY_ARMOS_STATUE_SPRITE_VARIANTS = new RomAddress(0x06, 0x7446);
    /** IronMasksMaskSpriteVariants at $03:$5B80. */
    private static final RomAddress ARMOREDENEMY_IRON_MASKS_MASK_SPRITE_VARIANTS = new RomAddress(0x03, 0x5B80);

    private static EntitySpriteRecipe armoredEnemyLookup(
            int entityType, EntityRoomLoader.RoomTable roomTable, int mapId) {
        if (entityType == ENTITY_MOBLIN) {
            return EntitySpriteRecipe.pair(
                    entityType,
                    ARMOREDENEMY_MOBLIN_SPRITE_VARIANTS.bank(),
                    ARMOREDENEMY_MOBLIN_SPRITE_VARIANTS.address(),
                    8,
                    0);
        }
        if (entityType == ENTITY_IRON_MASK) {
            return EntitySpriteRecipe.special(EntitySpriteRecipe.Kind.IRON_MASK, entityType);
        }
        if (entityType == ENTITY_BOMBER) {
            return EntitySpriteRecipe.rectangle(
                    entityType, ARMOREDENEMY_DATA_018_77ED.bank(), ARMOREDENEMY_DATA_018_77ED.address(), 4, 3, 0);
        }
        if (entityType == ENTITY_MAD_BOMBER) {
            return EntitySpriteRecipe.pair(
                    entityType,
                    ARMOREDENEMY_MAD_BOMBER_SPRITE_VARIANTS.bank(),
                    ARMOREDENEMY_MAD_BOMBER_SPRITE_VARIANTS.address(),
                    5,
                    0);
        }
        if (entityType == ENTITY_MOBLIN_SWORD) {
            return EntitySpriteRecipe.special(EntitySpriteRecipe.Kind.MOBLIN_SWORD, entityType);
        }
        if (entityType == ENTITY_MOBLIN_ARROW) {
            return EntitySpriteRecipe.pair(
                    entityType,
                    ARMOREDENEMY_ENTITY_ARROW_SPRITE_VARIANTS.bank(),
                    ARMOREDENEMY_ENTITY_ARROW_SPRITE_VARIANTS.address(),
                    4,
                    0);
        }
        if (entityType == ENTITY_ARMOS_STATUE) {
            return EntitySpriteRecipe.pair(
                    entityType,
                    ARMOREDENEMY_ARMOS_STATUE_SPRITE_VARIANTS.bank(),
                    ARMOREDENEMY_ARMOS_STATUE_SPRITE_VARIANTS.address(),
                    2,
                    0);
        }
        if (entityType == ENTITY_HARDHAT_BEETLE) {
            return EntitySpriteRecipe.pair(
                    entityType,
                    ARMOREDENEMY_HARD_HAT_BEETLE_CAVE_BSPRITE_VARIANTS.bank(),
                    mapId == 0x0A
                            ? ARMOREDENEMY_HARD_HAT_BEETLE_CAVE_BSPRITE_VARIANTS.address()
                            : ARMOREDENEMY_HARD_HAT_BEETLE_SPRITE_VARIANTS.address(),
                    2,
                    0);
        }
        if (entityType == ENTITY_SPIKED_BEETLE) {
            return EntitySpriteRecipe.pair(
                    entityType,
                    ARMOREDENEMY_SPIKED_BEETLE2_SPRITE_VARIANTS.bank(),
                    mapId == 0x03
                            ? ARMOREDENEMY_SPIKED_BEETLE2_SPRITE_VARIANTS.address()
                            : ARMOREDENEMY_SPIKED_BEETLE1_SPRITE_VARIANTS.address(),
                    4,
                    0);
        }
        if (entityType == ENTITY_IRON_MASKS_MASK) {
            return EntitySpriteRecipe.pair(
                    entityType,
                    ARMOREDENEMY_IRON_MASKS_MASK_SPRITE_VARIANTS.bank(),
                    ARMOREDENEMY_IRON_MASKS_MASK_SPRITE_VARIANTS.address(),
                    2,
                    0);
        }
        return null;
    }

    private static final int BOSS_MAP_TURTLE_ROCK = 0x07;
    private static final int BOSS_MAP_EAGLES_TOWER = 0x06;
    /** Data_006_527A, Data_006_523A and Data_006_51FA select the dungeon Hinox palette lists. */
    private static final RomAddress BOSS_TURTLE_ROCK_HINOX = new RomAddress(0x06, 0x527A);

    private static final RomAddress BOSS_EAGLES_TOWER_HINOX = new RomAddress(0x06, 0x523A);
    private static final RomAddress BOSS_DEFAULT_HINOX = new RomAddress(0x06, 0x51FA);

    /** Data_006_6E1E at $06:$6E1E. */
    private static final RomAddress BOSS_DATA_006_6E1E = new RomAddress(0x06, 0x6E1E);
    /** ThreeOfAKindSpriteVariants at $06:$4911. */
    private static final RomAddress BOSS_THREE_OF_AKIND_SPRITE_VARIANTS = new RomAddress(0x06, 0x4911);
    /** Data_006_5523 at $06:$5523. */
    private static final RomAddress BOSS_DATA_006_5523 = new RomAddress(0x06, 0x5523);

    private static EntitySpriteRecipe bossLookup(int entityType, EntityRoomLoader.RoomTable roomTable, int mapId) {
        if (entityType == 0x5B) return EntitySpriteRecipe.special(EntitySpriteRecipe.Kind.SLIME_EYE, entityType);
        if (entityType == ENTITY_MOLDORM) {
            return EntitySpriteRecipe.special(EntitySpriteRecipe.Kind.MOLDORM, entityType);
        }
        if (entityType == ENTITY_ROLLING_BONES) {
            return EntitySpriteRecipe.rectangle(
                    entityType, BOSS_DATA_006_6E1E.bank(), BOSS_DATA_006_6E1E.address(), 6, 4, 0);
        }
        if (entityType == ENTITY_ROLLING_BONES_BAR) {
            return EntitySpriteRecipe.special(EntitySpriteRecipe.Kind.ROLLING_BAR, entityType);
        }
        if (entityType == ENTITY_THREE_OF_A_KIND) {
            return EntitySpriteRecipe.pair(
                    entityType,
                    BOSS_THREE_OF_AKIND_SPRITE_VARIANTS.bank(),
                    BOSS_THREE_OF_AKIND_SPRITE_VARIANTS.address(),
                    8,
                    0);
        }
        if (entityType == ENTITY_ARMOS_KNIGHT) {
            return EntitySpriteRecipe.rectangle(
                    entityType, BOSS_DATA_006_5523.bank(), BOSS_DATA_006_5523.address(), 4, 8, 0);
        }
        if (entityType == ENTITY_HINOX) {
            int address = mapId == BOSS_MAP_TURTLE_ROCK
                    ? BOSS_TURTLE_ROCK_HINOX.address()
                    : mapId == BOSS_MAP_EAGLES_TOWER ? BOSS_EAGLES_TOWER_HINOX.address() : BOSS_DEFAULT_HINOX.address();
            return EntitySpriteRecipe.rectangle(entityType, 0x06, address, 4, 8, 0);
        }
        return null;
    }

    /** CuccoSpriteVariants at $05:$4514. */
    private static final RomAddress COMPANION_CUCCO_SPRITE_VARIANTS = new RomAddress(0x05, 0x4514);
    /** BowWowAtHomeSpriteVariants at $05:$4000. */
    private static final RomAddress COMPANION_BOW_WOW_AT_HOME_SPRITE_VARIANTS = new RomAddress(0x05, 0x4000);
    /** DogSpriteVariants at $19:$48CA. */
    private static final RomAddress COMPANION_DOG_SPRITE_VARIANTS = new RomAddress(0x19, 0x48CA);
    /** ButterflyOAMAttributes at $06:$6BBD. */
    private static final RomAddress COMPANION_BUTTERFLY_OAMATTRIBUTES = new RomAddress(0x06, 0x6BBD);

    private static EntitySpriteRecipe companionLookup(int entityType, EntityRoomLoader.RoomTable roomTable, int mapId) {
        if (entityType == ENTITY_CUCCO) {
            return EntitySpriteRecipe.pair(
                    entityType,
                    COMPANION_CUCCO_SPRITE_VARIANTS.bank(),
                    COMPANION_CUCCO_SPRITE_VARIANTS.address(),
                    4,
                    0);
        }
        if (entityType == ENTITY_BOW_WOW) {
            return EntitySpriteRecipe.pair(
                    entityType,
                    COMPANION_BOW_WOW_AT_HOME_SPRITE_VARIANTS.bank(),
                    COMPANION_BOW_WOW_AT_HOME_SPRITE_VARIANTS.address(),
                    7,
                    0);
        }
        if (entityType == ENTITY_DOG) {
            return EntitySpriteRecipe.pair(
                    entityType, COMPANION_DOG_SPRITE_VARIANTS.bank(), COMPANION_DOG_SPRITE_VARIANTS.address(), 4, 2);
        }
        if (entityType == ENTITY_BUTTERFLY) {
            return EntitySpriteRecipe.single(
                    entityType,
                    COMPANION_BUTTERFLY_OAMATTRIBUTES.bank(),
                    COMPANION_BUTTERFLY_OAMATTRIBUTES.address(),
                    2,
                    0);
        }
        return null;
    }

    /** LiftableStatueSpriteRect_BottleGrotto from azle.sym. */
    private static final RomAddress CONTEXTUALOBJECT_LIFTABLE_STATUE_SPRITE_RECT_BOTTLE_GROTTO =
            new RomAddress(0x19, 0x4010);
    /** LiftableStatueSpriteRect from azle.sym. */
    private static final RomAddress CONTEXTUALOBJECT_LIFTABLE_STATUE_SPRITE_RECT = new RomAddress(0x19, 0x4000);

    private static EntitySpriteRecipe contextualObjectLookupFloatingItem(
            int entityType, EntityRoomLoader.RoomTable roomTable, int mapId) {
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
                    CONTEXTUALOBJECT_LIFTABLE_STATUE_SPRITE_RECT_BOTTLE_GROTTO.bank(),
                    mapId == 1
                            ? CONTEXTUALOBJECT_LIFTABLE_STATUE_SPRITE_RECT_BOTTLE_GROTTO.address()
                            : CONTEXTUALOBJECT_LIFTABLE_STATUE_SPRITE_RECT.address(),
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

    /** KeeseCaveBSpriteVariants from azle.sym. */
    private static final RomAddress DUNGEONCREATURE_KEESE_CAVE_BSPRITE_VARIANTS = new RomAddress(0x6, 0x6710);
    /** KeeseSpriteVariants from azle.sym. */
    private static final RomAddress DUNGEONCREATURE_KEESE_SPRITE_VARIANTS = new RomAddress(0x6, 0x6708);
    /** BouncingBombiteEntityHandler from azle.sym. */
    private static final RomAddress DUNGEONCREATURE_BOUNCING_BOMBITE_ENTITY_HANDLER = new RomAddress(0x4, 0x7e0d);
    /** BouncingBombite1SpriteVariants from azle.sym. */
    private static final RomAddress DUNGEONCREATURE_BOUNCING_BOMBITE1_SPRITE_VARIANTS = new RomAddress(0x4, 0x7df5);
    /** Bombite2SpriteVariants from azle.sym. */
    private static final RomAddress DUNGEONCREATURE_BOMBITE2_SPRITE_VARIANTS = new RomAddress(0x4, 0x7d07);
    /** Bombite1SpriteVariants from azle.sym. */
    private static final RomAddress DUNGEONCREATURE_BOMBITE1_SPRITE_VARIANTS = new RomAddress(0x4, 0x7cef);

    /** SnakeSpriteVariants at $07:$683E. */
    private static final RomAddress DUNGEONCREATURE_SNAKE_SPRITE_VARIANTS = new RomAddress(0x07, 0x683E);
    /** WizrobeSpriteVariants at $06:$7604. */
    private static final RomAddress DUNGEONCREATURE_WIZROBE_SPRITE_VARIANTS = new RomAddress(0x06, 0x7604);
    /** WizrobeProjectileSpriteVariants at $06:$65E1. */
    private static final RomAddress DUNGEONCREATURE_WIZROBE_PROJECTILE_SPRITE_VARIANTS = new RomAddress(0x06, 0x65E1);
    /** MimicSpriteVariants at $19:$6A8D. */
    private static final RomAddress DUNGEONCREATURE_MIMIC_SPRITE_VARIANTS = new RomAddress(0x19, 0x6A8D);
    /** MiniMoldormSpriteVariants at $04:$5A49. */
    private static final RomAddress DUNGEONCREATURE_MINI_MOLDORM_SPRITE_VARIANTS = new RomAddress(0x04, 0x5A49);
    /** LikeLikeSpriteVariants at $06:$7DD4. */
    private static final RomAddress DUNGEONCREATURE_LIKE_LIKE_SPRITE_VARIANTS = new RomAddress(0x06, 0x7DD4);
    /** PolsVoiceSpriteVariants at $06:$7373. */
    private static final RomAddress DUNGEONCREATURE_POLS_VOICE_SPRITE_VARIANTS = new RomAddress(0x06, 0x7373);
    /** PairoddSpriteVariants at $04:$5DD1. */
    private static final RomAddress DUNGEONCREATURE_PAIRODD_SPRITE_VARIANTS = new RomAddress(0x04, 0x5DD1);
    /** PairoddProjectileSpriteVariants at $04:$5EF4. */
    private static final RomAddress DUNGEONCREATURE_PAIRODD_PROJECTILE_SPRITE_VARIANTS = new RomAddress(0x04, 0x5EF4);

    private static EntitySpriteRecipe dungeonCreatureLookup(
            int entityType, EntityRoomLoader.RoomTable roomTable, int mapId) {
        if (entityType == ENTITY_KEESE) {
            return EntitySpriteRecipe.pair(
                    entityType,
                    DUNGEONCREATURE_KEESE_CAVE_BSPRITE_VARIANTS.bank(),
                    mapId == 0x0A
                            ? DUNGEONCREATURE_KEESE_CAVE_BSPRITE_VARIANTS.address()
                            : DUNGEONCREATURE_KEESE_SPRITE_VARIANTS.address(),
                    2,
                    0);
        }
        if (entityType == ENTITY_SNAKE) {
            return EntitySpriteRecipe.pair(
                    entityType,
                    DUNGEONCREATURE_SNAKE_SPRITE_VARIANTS.bank(),
                    DUNGEONCREATURE_SNAKE_SPRITE_VARIANTS.address(),
                    4,
                    0);
        }
        if (entityType == ENTITY_WIZROBE) {
            return EntitySpriteRecipe.pair(
                    entityType,
                    DUNGEONCREATURE_WIZROBE_SPRITE_VARIANTS.bank(),
                    DUNGEONCREATURE_WIZROBE_SPRITE_VARIANTS.address(),
                    5,
                    0);
        }
        if (entityType == ENTITY_WIZROBE_PROJECTILE) {
            return EntitySpriteRecipe.pair(
                    entityType,
                    DUNGEONCREATURE_WIZROBE_PROJECTILE_SPRITE_VARIANTS.bank(),
                    DUNGEONCREATURE_WIZROBE_PROJECTILE_SPRITE_VARIANTS.address(),
                    4,
                    0);
        }
        if (entityType == ENTITY_BOUNCING_BOMBITE) {
            return EntitySpriteRecipe.pair(
                    entityType,
                    DUNGEONCREATURE_BOUNCING_BOMBITE_ENTITY_HANDLER.bank(),
                    mapId == 0x07
                            ? DUNGEONCREATURE_BOUNCING_BOMBITE_ENTITY_HANDLER.address()
                            : DUNGEONCREATURE_BOUNCING_BOMBITE1_SPRITE_VARIANTS.address(),
                    2,
                    0);
        }
        if (entityType == ENTITY_TIMER_BOMBITE) {
            return EntitySpriteRecipe.pair(
                    entityType,
                    DUNGEONCREATURE_BOMBITE2_SPRITE_VARIANTS.bank(),
                    mapId == 0x07
                            ? DUNGEONCREATURE_BOMBITE2_SPRITE_VARIANTS.address()
                            : DUNGEONCREATURE_BOMBITE1_SPRITE_VARIANTS.address(),
                    6,
                    0);
        }
        if (entityType == ENTITY_MIMIC) {
            // The eighth pair consumes the $21 byte at Data_019_6AAC,
            // immediately following the source's final commented attribute.
            return EntitySpriteRecipe.pair(
                    entityType,
                    DUNGEONCREATURE_MIMIC_SPRITE_VARIANTS.bank(),
                    DUNGEONCREATURE_MIMIC_SPRITE_VARIANTS.address(),
                    8,
                    0);
        }
        if (entityType == ENTITY_MINI_MOLDORM) {
            return EntitySpriteRecipe.pair(
                    entityType,
                    DUNGEONCREATURE_MINI_MOLDORM_SPRITE_VARIANTS.bank(),
                    DUNGEONCREATURE_MINI_MOLDORM_SPRITE_VARIANTS.address(),
                    10,
                    0);
        }
        if (entityType == ENTITY_LIKE_LIKE) {
            return EntitySpriteRecipe.pair(
                    entityType,
                    DUNGEONCREATURE_LIKE_LIKE_SPRITE_VARIANTS.bank(),
                    DUNGEONCREATURE_LIKE_LIKE_SPRITE_VARIANTS.address(),
                    2,
                    0);
        }
        if (entityType == ENTITY_POLS_VOICE) {
            return EntitySpriteRecipe.pair(
                    entityType,
                    DUNGEONCREATURE_POLS_VOICE_SPRITE_VARIANTS.bank(),
                    DUNGEONCREATURE_POLS_VOICE_SPRITE_VARIANTS.address(),
                    2,
                    0);
        }
        if (entityType == ENTITY_PAIRODD) {
            return EntitySpriteRecipe.pair(
                    entityType,
                    DUNGEONCREATURE_PAIRODD_SPRITE_VARIANTS.bank(),
                    DUNGEONCREATURE_PAIRODD_SPRITE_VARIANTS.address(),
                    8,
                    0);
        }
        if (entityType == ENTITY_PAIRODD_PROJECTILE) {
            return EntitySpriteRecipe.pair(
                    entityType,
                    DUNGEONCREATURE_PAIRODD_PROJECTILE_SPRITE_VARIANTS.bank(),
                    DUNGEONCREATURE_PAIRODD_PROJECTILE_SPRITE_VARIANTS.address(),
                    2,
                    0);
        }
        return null;
    }

    /** Data_015_4A15 at $15:$4A15. */
    private static final RomAddress HAZARD_DATA_015_4A15 = new RomAddress(0x15, 0x4A15);
    /** MaskedMimicGoriyaSpriteVariants at $19:$4796. */
    private static final RomAddress HAZARD_MASKED_MIMIC_GORIYA_SPRITE_VARIANTS = new RomAddress(0x19, 0x4796);
    /** AntiFairySpriteVariants at $06:$786E. */
    private static final RomAddress HAZARD_ANTI_FAIRY_SPRITE_VARIANTS = new RomAddress(0x06, 0x786E);
    /** SparkSpriteVariants at $06:$6615. */
    private static final RomAddress HAZARD_SPARK_SPRITE_VARIANTS = new RomAddress(0x06, 0x6615);
    /** LaserSpriteVariants at $04:$6C2D. */
    private static final RomAddress HAZARD_LASER_SPRITE_VARIANTS = new RomAddress(0x04, 0x6C2D);
    /** SpikeTrapSpriteVariants at $06:$74FA. */
    private static final RomAddress HAZARD_SPIKE_TRAP_SPRITE_VARIANTS = new RomAddress(0x06, 0x74FA);

    private static EntitySpriteRecipe hazardLookup(int entityType, EntityRoomLoader.RoomTable roomTable, int mapId) {
        if (entityType == 0xE4) {
            // func_015_4AC9 indexes fourteen three-sprite body variants at
            // Data_015_4A15. Its separate weapon layer is handler-generated.
            return EntitySpriteRecipe.rectangle(
                    entityType, HAZARD_DATA_015_4A15.bank(), HAZARD_DATA_015_4A15.address(), 14, 3, 0);
        }
        if (entityType == ENTITY_MASKED_MIMIC_GORIYA && mapId != 0x1F) {
            return EntitySpriteRecipe.pair(
                    entityType,
                    HAZARD_MASKED_MIMIC_GORIYA_SPRITE_VARIANTS.bank(),
                    HAZARD_MASKED_MIMIC_GORIYA_SPRITE_VARIANTS.address(),
                    8,
                    0);
        }
        if (entityType == ENTITY_ANTI_FAIRY) {
            return EntitySpriteRecipe.pair(
                    entityType,
                    HAZARD_ANTI_FAIRY_SPRITE_VARIANTS.bank(),
                    HAZARD_ANTI_FAIRY_SPRITE_VARIANTS.address(),
                    2,
                    0);
        }
        if (entityType == ENTITY_SPARK_COUNTER_CLOCKWISE || entityType == ENTITY_SPARK_CLOCKWISE) {
            return EntitySpriteRecipe.pair(
                    entityType, HAZARD_SPARK_SPRITE_VARIANTS.bank(), HAZARD_SPARK_SPRITE_VARIANTS.address(), 2, 0);
        }
        if (entityType == ENTITY_LASER) {
            return EntitySpriteRecipe.pair(
                    entityType, HAZARD_LASER_SPRITE_VARIANTS.bank(), HAZARD_LASER_SPRITE_VARIANTS.address(), 8, 0);
        }
        if (entityType == ENTITY_LASER_BEAM) {
            // LaserBeamEntityHandler never renders a normal entity sprite;
            // its visible pixels are transient VFX $06 from bank $02.
            return EntitySpriteRecipe.special(EntitySpriteRecipe.Kind.UNSUPPORTED, entityType);
        }
        if (entityType == ENTITY_SPIKE_TRAP) {
            return EntitySpriteRecipe.pair(
                    entityType,
                    HAZARD_SPIKE_TRAP_SPRITE_VARIANTS.bank(),
                    HAZARD_SPIKE_TRAP_SPRITE_VARIANTS.address(),
                    1,
                    0);
        }
        return null;
    }

    /** CrowSpriteVariants at $06:$5C89. */
    private static final RomAddress OVERWORLDCREATURE_CROW_SPRITE_VARIANTS = new RomAddress(0x06, 0x5C89);
    /** BuzzBlobSpriteVariants at $18:$7729. */
    private static final RomAddress OVERWORLDCREATURE_BUZZ_BLOB_SPRITE_VARIANTS = new RomAddress(0x18, 0x7729);
    /** OctorokSpriteVariants at $03:$57FB. */
    private static final RomAddress OVERWORLDCREATURE_OCTOROK_SPRITE_VARIANTS = new RomAddress(0x03, 0x57FB);
    /** OctorokRockSpriteVariants at $03:$6A1E. */
    private static final RomAddress OVERWORLDCREATURE_OCTOROK_ROCK_SPRITE_VARIANTS = new RomAddress(0x03, 0x6A1E);
    /** TektiteSpriteVariants at $06:$78B7. */
    private static final RomAddress OVERWORLDCREATURE_TEKTITE_SPRITE_VARIANTS = new RomAddress(0x06, 0x78B7);
    /** LeeverSpriteVariants at $04:$7EE5. */
    private static final RomAddress OVERWORLDCREATURE_LEEVER_SPRITE_VARIANTS = new RomAddress(0x04, 0x7EE5);
    /** WingedOctorokSpriteVariants at $07:$562D. */
    private static final RomAddress OVERWORLDCREATURE_WINGED_OCTOROK_SPRITE_VARIANTS = new RomAddress(0x07, 0x562D);
    /** PincerSpriteVariants at $07:$542B. */
    private static final RomAddress OVERWORLDCREATURE_PINCER_SPRITE_VARIANTS = new RomAddress(0x07, 0x542B);
    /** PeaHatSpriteVariants at $07:$6701. */
    private static final RomAddress OVERWORLDCREATURE_PEA_HAT_SPRITE_VARIANTS = new RomAddress(0x07, 0x6701);

    private static EntitySpriteRecipe overworldCreatureLookup(
            int entityType, EntityRoomLoader.RoomTable roomTable, int mapId) {
        if (entityType == ENTITY_CROW) {
            return EntitySpriteRecipe.pair(
                    entityType,
                    OVERWORLDCREATURE_CROW_SPRITE_VARIANTS.bank(),
                    OVERWORLDCREATURE_CROW_SPRITE_VARIANTS.address(),
                    4,
                    2);
        }
        if (entityType == ENTITY_BUZZ_BLOB) {
            return EntitySpriteRecipe.pair(
                    entityType,
                    OVERWORLDCREATURE_BUZZ_BLOB_SPRITE_VARIANTS.bank(),
                    OVERWORLDCREATURE_BUZZ_BLOB_SPRITE_VARIANTS.address(),
                    9,
                    0);
        }
        if (entityType == ENTITY_OCTOROK) {
            return EntitySpriteRecipe.pair(
                    entityType,
                    OVERWORLDCREATURE_OCTOROK_SPRITE_VARIANTS.bank(),
                    OVERWORLDCREATURE_OCTOROK_SPRITE_VARIANTS.address(),
                    8,
                    0);
        }
        if (entityType == ENTITY_OCTOROK_ROCK) {
            return EntitySpriteRecipe.pair(
                    entityType,
                    OVERWORLDCREATURE_OCTOROK_ROCK_SPRITE_VARIANTS.bank(),
                    OVERWORLDCREATURE_OCTOROK_ROCK_SPRITE_VARIANTS.address(),
                    2,
                    0);
        }
        if (entityType == ENTITY_TEKTITE) {
            return EntitySpriteRecipe.pair(
                    entityType,
                    OVERWORLDCREATURE_TEKTITE_SPRITE_VARIANTS.bank(),
                    OVERWORLDCREATURE_TEKTITE_SPRITE_VARIANTS.address(),
                    2,
                    0);
        }
        if (entityType == ENTITY_LEEVER) {
            return EntitySpriteRecipe.pair(
                    entityType,
                    OVERWORLDCREATURE_LEEVER_SPRITE_VARIANTS.bank(),
                    OVERWORLDCREATURE_LEEVER_SPRITE_VARIANTS.address(),
                    4,
                    0);
        }
        if (entityType == ENTITY_WINGED_OCTOROK) {
            return EntitySpriteRecipe.pair(
                    entityType,
                    OVERWORLDCREATURE_WINGED_OCTOROK_SPRITE_VARIANTS.bank(),
                    OVERWORLDCREATURE_WINGED_OCTOROK_SPRITE_VARIANTS.address(),
                    8,
                    0);
        }
        if (entityType == ENTITY_PINCER) {
            return EntitySpriteRecipe.pair(
                    entityType,
                    OVERWORLDCREATURE_PINCER_SPRITE_VARIANTS.bank(),
                    OVERWORLDCREATURE_PINCER_SPRITE_VARIANTS.address(),
                    10,
                    0);
        }
        if (entityType == ENTITY_PEAHAT) {
            return EntitySpriteRecipe.pair(
                    entityType,
                    OVERWORLDCREATURE_PEA_HAT_SPRITE_VARIANTS.bank(),
                    OVERWORLDCREATURE_PEA_HAT_SPRITE_VARIANTS.address(),
                    2,
                    0);
        }
        return null;
    }

    /** SirensInstrument2SpriteVariants at $03:$5D83. */
    private static final RomAddress PICKUP_SIRENS_INSTRUMENT2_SPRITE_VARIANTS = new RomAddress(0x03, 0x5D83);
    /** DroppableHeartSprite at $03:$5D36. */
    private static final RomAddress PICKUP_DROPPABLE_HEART_SPRITE = new RomAddress(0x03, 0x5D36);
    /** DroppableRupeeSprite at $03:$609C. */
    private static final RomAddress PICKUP_DROPPABLE_RUPEE_SPRITE = new RomAddress(0x03, 0x609C);
    /** data_003_6157 at $03:$6157. */
    private static final RomAddress PICKUP_DATA_003_6157 = new RomAddress(0x03, 0x6157);
    /** KeyDropSpriteTable at $03:$5C78. */
    private static final RomAddress PICKUP_KEY_DROP_SPRITE_TABLE = new RomAddress(0x03, 0x5C78);
    /** Data_003_5B95 at $03:$5B95. */
    private static final RomAddress PICKUP_DATA_003_5B95 = new RomAddress(0x03, 0x5B95);
    /** PieceOfPowerSpriteVariants at $03:$5B65. */
    private static final RomAddress PICKUP_PIECE_OF_POWER_SPRITE_VARIANTS = new RomAddress(0x03, 0x5B65);
    /** Data_003_5B5B at $03:$5B5B. */
    private static final RomAddress PICKUP_DATA_003_5B5B = new RomAddress(0x03, 0x5B5B);
    /** HeartPieceEntitySprite at $03:$5A4D. */
    private static final RomAddress PICKUP_HEART_PIECE_ENTITY_SPRITE = new RomAddress(0x03, 0x5A4D);
    /** Entity13Handler at $03:$59D8. */
    private static final RomAddress PICKUP_ENTITY13_HANDLER = new RomAddress(0x03, 0x59D8);
    /** DroppableArrowSprite at $03:$6079. */
    private static final RomAddress PICKUP_DROPPABLE_ARROW_SPRITE = new RomAddress(0x03, 0x6079);
    /** DroppableBombsSprite at $03:$5FC0. */
    private static final RomAddress PICKUP_DROPPABLE_BOMBS_SPRITE = new RomAddress(0x03, 0x5FC0);
    /** SleepyToadstoolSprite at $03:$5D47. */
    private static final RomAddress PICKUP_SLEEPY_TOADSTOOL_SPRITE = new RomAddress(0x03, 0x5D47);
    /** DroppableMagicPowderSprite at $03:$6055. */
    private static final RomAddress PICKUP_DROPPABLE_MAGIC_POWDER_SPRITE = new RomAddress(0x03, 0x6055);
    /** HidingSlimeKeySprite at $03:$5FFB. */
    private static final RomAddress PICKUP_HIDING_SLIME_KEY_SPRITE = new RomAddress(0x03, 0x5FFB);
    /** DroppableSeashellSprite at $03:$5FD1. */
    private static final RomAddress PICKUP_DROPPABLE_SEASHELL_SPRITE = new RomAddress(0x03, 0x5FD1);

    private static EntitySpriteRecipe pickupLookup(int entityType, EntityRoomLoader.RoomTable roomTable, int mapId) {
        if (entityType == ENTITY_INSTRUMENT_OF_THE_SIRENS) {
            return EntitySpriteRecipe.pair(
                    entityType,
                    PICKUP_SIRENS_INSTRUMENT2_SPRITE_VARIANTS.bank(),
                    PICKUP_SIRENS_INSTRUMENT2_SPRITE_VARIANTS.address(),
                    4,
                    mapId < 0 ? 0 : mapId & 0x03);
        }
        if (entityType == ENTITY_DROPPABLE_HEART) {
            return EntitySpriteRecipe.single(
                    entityType, PICKUP_DROPPABLE_HEART_SPRITE.bank(), PICKUP_DROPPABLE_HEART_SPRITE.address(), 1, 0);
        }
        if (entityType == ENTITY_DROPPABLE_RUPEE) {
            return EntitySpriteRecipe.single(
                    entityType, PICKUP_DROPPABLE_RUPEE_SPRITE.bank(), PICKUP_DROPPABLE_RUPEE_SPRITE.address(), 1, 0);
        }
        if (entityType == ENTITY_DROPPABLE_FAIRY) {
            return EntitySpriteRecipe.pair(
                    entityType, PICKUP_DATA_003_6157.bank(), PICKUP_DATA_003_6157.address(), 2, 0);
        }
        if (entityType == ENTITY_KEY_DROP_POINT) {
            return EntitySpriteRecipe.single(
                    entityType, PICKUP_KEY_DROP_SPRITE_TABLE.bank(), PICKUP_KEY_DROP_SPRITE_TABLE.address(), 6, 0);
        }
        if (entityType == ENTITY_SWORD_SHIELD_PICKUP) {
            return EntitySpriteRecipe.single(
                    entityType, PICKUP_DATA_003_5B95.bank(), PICKUP_DATA_003_5B95.address(), 1, 0);
        }
        if (entityType == ENTITY_PIECE_OF_POWER) {
            return EntitySpriteRecipe.pair(
                    entityType,
                    PICKUP_PIECE_OF_POWER_SPRITE_VARIANTS.bank(),
                    PICKUP_PIECE_OF_POWER_SPRITE_VARIANTS.address(),
                    2,
                    0);
        }
        if (entityType == ENTITY_GUARDIAN_ACORN) {
            return EntitySpriteRecipe.single(
                    entityType, PICKUP_DATA_003_5B5B.bank(), PICKUP_DATA_003_5B5B.address(), 1, 0);
        }
        if (entityType == ENTITY_HEART_PIECE) {
            return EntitySpriteRecipe.pair(
                    entityType,
                    PICKUP_HEART_PIECE_ENTITY_SPRITE.bank(),
                    PICKUP_HEART_PIECE_ENTITY_SPRITE.address(),
                    1,
                    0);
        }
        if (entityType == ENTITY_HEART_CONTAINER) {
            return EntitySpriteRecipe.pair(
                    entityType, PICKUP_ENTITY13_HANDLER.bank(), PICKUP_ENTITY13_HANDLER.address(), 1, 0);
        }
        if (entityType == ENTITY_DROPPABLE_ARROWS) {
            return EntitySpriteRecipe.pair(
                    entityType, PICKUP_DROPPABLE_ARROW_SPRITE.bank(), PICKUP_DROPPABLE_ARROW_SPRITE.address(), 1, 0);
        }
        if (entityType == ENTITY_DROPPABLE_BOMBS) {
            return EntitySpriteRecipe.single(
                    entityType, PICKUP_DROPPABLE_BOMBS_SPRITE.bank(), PICKUP_DROPPABLE_BOMBS_SPRITE.address(), 1, 0);
        }
        if (entityType == ENTITY_SLEEPY_TOADSTOOL) {
            return EntitySpriteRecipe.pair(
                    entityType, PICKUP_SLEEPY_TOADSTOOL_SPRITE.bank(), PICKUP_SLEEPY_TOADSTOOL_SPRITE.address(), 1, 0);
        }
        if (entityType == ENTITY_DROPPABLE_MAGIC_POWDER) {
            return EntitySpriteRecipe.single(
                    entityType,
                    PICKUP_DROPPABLE_MAGIC_POWDER_SPRITE.bank(),
                    PICKUP_DROPPABLE_MAGIC_POWDER_SPRITE.address(),
                    1,
                    0);
        }
        if (entityType == ENTITY_HIDING_SLIME_KEY) {
            return EntitySpriteRecipe.single(
                    entityType, PICKUP_HIDING_SLIME_KEY_SPRITE.bank(), PICKUP_HIDING_SLIME_KEY_SPRITE.address(), 1, 0);
        }
        if (entityType == ENTITY_DROPPABLE_SECRET_SEASHELL) {
            return EntitySpriteRecipe.single(
                    entityType,
                    PICKUP_DROPPABLE_SEASHELL_SPRITE.bank(),
                    PICKUP_DROPPABLE_SEASHELL_SPRITE.address(),
                    1,
                    0);
        }
        return null;
    }

    /** BushcrawlerIndoorSpriteVariants from azle.sym. */
    private static final RomAddress PLANT_BUSHCRAWLER_INDOOR_SPRITE_VARIANTS = new RomAddress(0x7, 0x401a);
    /** BushcrawlerOutdoorSpriteVariants from azle.sym. */
    private static final RomAddress PLANT_BUSHCRAWLER_OUTDOOR_SPRITE_VARIANTS = new RomAddress(0x7, 0x4012);

    /** GopongaFlowerSpriteVariants at $06:$63F4. */
    private static final RomAddress PLANT_GOPONGA_FLOWER_SPRITE_VARIANTS = new RomAddress(0x06, 0x63F4);
    /** Data_006_6316 at $06:$6316. */
    private static final RomAddress PLANT_DATA_006_6316 = new RomAddress(0x06, 0x6316);
    /** GopongaFlowerProjectileSpriteList at $06:$638F. */
    private static final RomAddress PLANT_GOPONGA_FLOWER_PROJECTILE_SPRITE_LIST = new RomAddress(0x06, 0x638F);
    /** Data_036_6FC2 at $36:$6FC2. */
    private static final RomAddress PLANT_DATA_036_6FC2 = new RomAddress(0x36, 0x6FC2);

    private static EntitySpriteRecipe plantLookup(int entityType, EntityRoomLoader.RoomTable roomTable, int mapId) {
        if (entityType == ENTITY_GOPONGA_FLOWER) {
            return EntitySpriteRecipe.pair(
                    entityType,
                    PLANT_GOPONGA_FLOWER_SPRITE_VARIANTS.bank(),
                    PLANT_GOPONGA_FLOWER_SPRITE_VARIANTS.address(),
                    2,
                    0);
        }
        if (entityType == ENTITY_GIANT_GOPONGA_FLOWER) {
            return EntitySpriteRecipe.rectangle(
                    entityType, PLANT_DATA_006_6316.bank(), PLANT_DATA_006_6316.address(), 3, 8, 0);
        }
        if (entityType == ENTITY_GOPONGA_FLOWER_PROJECTILE) {
            return EntitySpriteRecipe.pair(
                    entityType,
                    PLANT_GOPONGA_FLOWER_PROJECTILE_SPRITE_LIST.bank(),
                    PLANT_GOPONGA_FLOWER_PROJECTILE_SPRITE_LIST.address(),
                    4,
                    0);
        }
        if (entityType == ENTITY_POKEY) {
            return EntitySpriteRecipe.special(EntitySpriteRecipe.Kind.POKEY, entityType);
        }
        if (entityType == ENTITY_PIRANHA_PLANT) {
            return EntitySpriteRecipe.rectangle(
                    entityType, PLANT_DATA_036_6FC2.bank(), PLANT_DATA_036_6FC2.address(), 6, 4, 0);
        }
        if (entityType == ENTITY_BUSH_CRAWLER) {
            return EntitySpriteRecipe.pair(
                    entityType,
                    PLANT_BUSHCRAWLER_INDOOR_SPRITE_VARIANTS.bank(),
                    EntitySpriteGeometry.isIndoorRoomTable(roomTable)
                            ? PLANT_BUSHCRAWLER_INDOOR_SPRITE_VARIANTS.address()
                            : PLANT_BUSHCRAWLER_OUTDOOR_SPRITE_VARIANTS.address(),
                    2,
                    0,
                    EntitySpriteDefinition.DynamicSprite.TileSource.GPU);
        }
        return null;
    }

    /** EntityArrowSpriteVariants at $03:$6BC6. */
    private static final RomAddress PLAYERPROJECTILE_ENTITY_ARROW_SPRITE_VARIANTS = new RomAddress(0x03, 0x6BC6);
    /** BoomerangSpriteVariants at $19:$4451. */
    private static final RomAddress PLAYERPROJECTILE_BOOMERANG_SPRITE_VARIANTS = new RomAddress(0x19, 0x4451);
    /** MagicRodFireballSpriteVariants at $03:$69AA. */
    private static final RomAddress PLAYERPROJECTILE_MAGIC_ROD_FIREBALL_SPRITE_VARIANTS = new RomAddress(0x03, 0x69AA);
    /** Data_018_7ABA at $18:$7ABA. */
    private static final RomAddress PLAYERPROJECTILE_DATA_018_7ABA = new RomAddress(0x18, 0x7ABA);
    /** MusicalNoteEntityHandler at $05:$7EF8. */
    private static final RomAddress PLAYERPROJECTILE_MUSICAL_NOTE_ENTITY_HANDLER = new RomAddress(0x05, 0x7EF8);
    /** Data_019_44FC at $19:$44FC. */
    private static final RomAddress PLAYERPROJECTILE_DATA_019_44FC = new RomAddress(0x19, 0x44FC);

    private static EntitySpriteRecipe playerProjectileLookup(
            int entityType, EntityRoomLoader.RoomTable roomTable, int mapId) {
        if (entityType == ENTITY_ARROW) {
            return EntitySpriteRecipe.pair(
                    entityType,
                    PLAYERPROJECTILE_ENTITY_ARROW_SPRITE_VARIANTS.bank(),
                    PLAYERPROJECTILE_ENTITY_ARROW_SPRITE_VARIANTS.address(),
                    4,
                    0);
        }
        if (entityType == ENTITY_BOOMERANG) {
            return EntitySpriteRecipe.pair(
                    entityType,
                    PLAYERPROJECTILE_BOOMERANG_SPRITE_VARIANTS.bank(),
                    PLAYERPROJECTILE_BOOMERANG_SPRITE_VARIANTS.address(),
                    4,
                    0);
        }
        if (entityType == ENTITY_MAGIC_ROD_FIREBALL) {
            return EntitySpriteRecipe.pair(
                    entityType,
                    PLAYERPROJECTILE_MAGIC_ROD_FIREBALL_SPRITE_VARIANTS.bank(),
                    PLAYERPROJECTILE_MAGIC_ROD_FIREBALL_SPRITE_VARIANTS.address(),
                    2,
                    0);
        }
        if (entityType == ENTITY_MAGIC_POWDER_SPRINKLE) {
            return EntitySpriteRecipe.rectangle(
                    entityType,
                    PLAYERPROJECTILE_DATA_018_7ABA.bank(),
                    PLAYERPROJECTILE_DATA_018_7ABA.address(),
                    8,
                    3,
                    0);
        }
        if (entityType == ENTITY_MUSICAL_NOTE) {
            return EntitySpriteRecipe.single(
                    entityType,
                    PLAYERPROJECTILE_MUSICAL_NOTE_ENTITY_HANDLER.bank(),
                    PLAYERPROJECTILE_MUSICAL_NOTE_ENTITY_HANDLER.address(),
                    1,
                    0);
        }
        if (entityType == ENTITY_SWORD_BEAM) {
            return EntitySpriteRecipe.rectangle(
                    entityType,
                    PLAYERPROJECTILE_DATA_019_44FC.bank(),
                    PLAYERPROJECTILE_DATA_019_44FC.address(),
                    4,
                    2,
                    0);
        }
        if (entityType == ENTITY_BOMB) {
            return EntitySpriteRecipe.single(
                    entityType, RomSymbols.BombSprite.bank(), RomSymbols.BombSprite.address(), 1, 0);
        }
        if (entityType == ENTITY_HOOKSHOT_CHAIN) {
            return EntitySpriteRecipe.special(EntitySpriteRecipe.Kind.HOOKSHOT_CHAIN, entityType);
        }
        return null;
    }

    /** OwlStatueSpriteVariants at $18:$5DF7. */
    private static final RomAddress ROOMOBJECT_OWL_STATUE_SPRITE_VARIANTS = new RomAddress(0x18, 0x5DF7);
    /** ChestItemSpriteTable at $07:$7B57. */
    private static final RomAddress ROOMOBJECT_CHEST_ITEM_SPRITE_TABLE = new RomAddress(0x07, 0x7B57);
    /** Unknown011SpriteVariants at $03:$5235. */
    private static final RomAddress ROOMOBJECT_UNKNOWN011_SPRITE_VARIANTS = new RomAddress(0x03, 0x5235);
    /** CrystalSwitchSpriteVariants at $15:$4320. */
    private static final RomAddress ROOMOBJECT_CRYSTAL_SWITCH_SPRITE_VARIANTS = new RomAddress(0x15, 0x4320);

    private static EntitySpriteRecipe roomObjectLookup(
            int entityType, EntityRoomLoader.RoomTable roomTable, int mapId) {
        if (entityType == 0x42 && mapId != 0x16) {
            return EntitySpriteRecipe.owlStatue(
                    entityType,
                    ROOMOBJECT_OWL_STATUE_SPRITE_VARIANTS.bank(),
                    ROOMOBJECT_OWL_STATUE_SPRITE_VARIANTS.address());
        }
        if (entityType == ENTITY_CHEST_WITH_ITEM) {
            return EntitySpriteRecipe.single(
                    entityType,
                    ROOMOBJECT_CHEST_ITEM_SPRITE_TABLE.bank(),
                    ROOMOBJECT_CHEST_ITEM_SPRITE_TABLE.address(),
                    0x21,
                    0);
        }
        if (entityType == 0x06) {
            // Unknown011SpriteVariants, used by the transient pushed block.
            return EntitySpriteRecipe.pair(
                    entityType,
                    ROOMOBJECT_UNKNOWN011_SPRITE_VARIANTS.bank(),
                    ROOMOBJECT_UNKNOWN011_SPRITE_VARIANTS.address(),
                    2,
                    1);
        }
        if (entityType == 0x61 && EntitySpriteGeometry.isIndoorRoomTable(roomTable)) {
            return EntitySpriteRecipe.special(EntitySpriteRecipe.Kind.DUNGEON_WARP, entityType);
        }
        if (entityType == ENTITY_CRYSTAL_SWITCH) {
            return EntitySpriteRecipe.pair(
                    entityType,
                    ROOMOBJECT_CRYSTAL_SWITCH_SPRITE_VARIANTS.bank(),
                    ROOMOBJECT_CRYSTAL_SWITCH_SPRITE_VARIANTS.address(),
                    1,
                    0);
        }
        return null;
    }

    /** GoombaSpriteVariants at $07:$65CE. */
    private static final RomAddress SIDEVIEW_GOOMBA_SPRITE_VARIANTS = new RomAddress(0x07, 0x65CE);

    private static EntitySpriteRecipe sideViewLookup(int entityType, EntityRoomLoader.RoomTable roomTable, int mapId) {
        if (entityType == ENTITY_GOOMBA) {
            return EntitySpriteRecipe.pair(
                    entityType,
                    SIDEVIEW_GOOMBA_SPRITE_VARIANTS.bank(),
                    SIDEVIEW_GOOMBA_SPRITE_VARIANTS.address(),
                    3,
                    0);
        }
        return null;
    }

    /** ZolSpriteVariants at $06:$7C09. */
    private static final RomAddress SLIMEENEMY_ZOL_SPRITE_VARIANTS = new RomAddress(0x06, 0x7C09);
    /** GelSpriteVariants at $06:$7BFA. */
    private static final RomAddress SLIMEENEMY_GEL_SPRITE_VARIANTS = new RomAddress(0x06, 0x7BFA);
    /** StarSpriteVariants at $07:$7247. */
    private static final RomAddress SLIMEENEMY_STAR_SPRITE_VARIANTS = new RomAddress(0x07, 0x7247);

    private static EntitySpriteRecipe slimeEnemyLookup(
            int entityType, EntityRoomLoader.RoomTable roomTable, int mapId) {
        if (entityType == ENTITY_ZOL) {
            return EntitySpriteRecipe.pair(
                    entityType, SLIMEENEMY_ZOL_SPRITE_VARIANTS.bank(), SLIMEENEMY_ZOL_SPRITE_VARIANTS.address(), 2, 0);
        }
        if (entityType == ENTITY_GEL) {
            return EntitySpriteRecipe.single(
                    entityType, SLIMEENEMY_GEL_SPRITE_VARIANTS.bank(), SLIMEENEMY_GEL_SPRITE_VARIANTS.address(), 2, 0);
        }
        if (entityType == ENTITY_HIDING_ZOL) {
            return EntitySpriteRecipe.special(EntitySpriteRecipe.Kind.HIDING_ZOL, entityType);
        }
        if (entityType == ENTITY_STAR) {
            return EntitySpriteRecipe.pair(
                    entityType,
                    SLIMEENEMY_STAR_SPRITE_VARIANTS.bank(),
                    SLIMEENEMY_STAR_SPRITE_VARIANTS.address(),
                    5,
                    0);
        }
        return null;
    }

    /** GibdoTurtleRockSpriteVariants from azle.sym. */
    private static final RomAddress UNDEADENEMY_GIBDO_TURTLE_ROCK_SPRITE_VARIANTS = new RomAddress(0x6, 0x7e77);
    /** GibdoSpriteVariants from azle.sym. */
    private static final RomAddress UNDEADENEMY_GIBDO_SPRITE_VARIANTS = new RomAddress(0x6, 0x7e6f);

    /** ZombieSpriteVariants at $18:$63F8. */
    private static final RomAddress UNDEADENEMY_ZOMBIE_SPRITE_VARIANTS = new RomAddress(0x18, 0x63F8);
    /** BooBuddyEntityHandler at $06:$79A9. */
    private static final RomAddress UNDEADENEMY_BOO_BUDDY_ENTITY_HANDLER = new RomAddress(0x06, 0x79A9);
    /** StalfosAggressiveSpriteVariants at $06:$4AA8. */
    private static final RomAddress UNDEADENEMY_STALFOS_AGGRESSIVE_SPRITE_VARIANTS = new RomAddress(0x06, 0x4AA8);
    /** StalfosEvasive2SpriteVariants at $15:$4E7D. */
    private static final RomAddress UNDEADENEMY_STALFOS_EVASIVE2_SPRITE_VARIANTS = new RomAddress(0x15, 0x4E7D);
    /** GhiniSpriteVariants at $04:$5BFC. */
    private static final RomAddress UNDEADENEMY_GHINI_SPRITE_VARIANTS = new RomAddress(0x04, 0x5BFC);
    /** GiantGhiniSpriteRectVariants at $04:$5D26. */
    private static final RomAddress UNDEADENEMY_GIANT_GHINI_SPRITE_RECT_VARIANTS = new RomAddress(0x04, 0x5D26);

    private static EntitySpriteRecipe undeadEnemyLookup(
            int entityType, EntityRoomLoader.RoomTable roomTable, int mapId) {
        if (entityType == ENTITY_ZOMBIE) {
            return EntitySpriteRecipe.pair(
                    entityType,
                    UNDEADENEMY_ZOMBIE_SPRITE_VARIANTS.bank(),
                    UNDEADENEMY_ZOMBIE_SPRITE_VARIANTS.address(),
                    5,
                    0);
        }
        if (entityType == ENTITY_BOO_BUDDY) {
            return EntitySpriteRecipe.pair(
                    entityType,
                    UNDEADENEMY_BOO_BUDDY_ENTITY_HANDLER.bank(),
                    UNDEADENEMY_BOO_BUDDY_ENTITY_HANDLER.address(),
                    8,
                    0);
        }
        if (entityType == ENTITY_STALFOS_AGGRESSIVE) {
            return EntitySpriteRecipe.pair(
                    entityType,
                    UNDEADENEMY_STALFOS_AGGRESSIVE_SPRITE_VARIANTS.bank(),
                    UNDEADENEMY_STALFOS_AGGRESSIVE_SPRITE_VARIANTS.address(),
                    3,
                    0);
        }
        if (entityType == ENTITY_STALFOS_EVASIVE) {
            return EntitySpriteRecipe.pair(
                    entityType,
                    UNDEADENEMY_STALFOS_EVASIVE2_SPRITE_VARIANTS.bank(),
                    UNDEADENEMY_STALFOS_EVASIVE2_SPRITE_VARIANTS.address(),
                    3,
                    0);
        }
        if (entityType == ENTITY_GIBDO) {
            return EntitySpriteRecipe.pair(
                    entityType,
                    UNDEADENEMY_GIBDO_TURTLE_ROCK_SPRITE_VARIANTS.bank(),
                    mapId == 0x07
                            ? UNDEADENEMY_GIBDO_TURTLE_ROCK_SPRITE_VARIANTS.address()
                            : UNDEADENEMY_GIBDO_SPRITE_VARIANTS.address(),
                    2,
                    0);
        }
        if (entityType == ENTITY_HIDING_GHINI || entityType == ENTITY_GHINI) {
            return EntitySpriteRecipe.pair(
                    entityType,
                    UNDEADENEMY_GHINI_SPRITE_VARIANTS.bank(),
                    UNDEADENEMY_GHINI_SPRITE_VARIANTS.address(),
                    2,
                    0);
        }
        if (entityType == ENTITY_GIANT_GHINI) {
            return EntitySpriteRecipe.rectangle(
                    entityType,
                    UNDEADENEMY_GIANT_GHINI_SPRITE_RECT_VARIANTS.bank(),
                    UNDEADENEMY_GIANT_GHINI_SPRITE_RECT_VARIANTS.address(),
                    4,
                    8,
                    0);
        }
        return null;
    }

    /** Kid72SpriteVariants from azle.sym. */
    private static final RomAddress VILLAGENPC_KID72_SPRITE_VARIANTS = new RomAddress(0x6, 0x605d);

    /** WitchRatSpriteVariants at $15:$788D. */
    private static final RomAddress VILLAGENPC_WITCH_RAT_SPRITE_VARIANTS = new RomAddress(0x15, 0x788D);
    /** Data_005_4780 at $05:$4780. */
    private static final RomAddress VILLAGENPC_DATA_005_4780 = new RomAddress(0x05, 0x4780);
    /** MarinIndoorSpriteVariants at $05:$4E0A. */
    private static final RomAddress VILLAGENPC_MARIN_INDOOR_SPRITE_VARIANTS = new RomAddress(0x05, 0x4E0A);
    /** MarinCreditsSpriteVariants at $05:$4E2A. */
    private static final RomAddress VILLAGENPC_MARIN_CREDITS_SPRITE_VARIANTS = new RomAddress(0x05, 0x4E2A);
    /** Tarin3SpriteVariants at $05:$4932. */
    private static final RomAddress VILLAGENPC_TARIN3_SPRITE_VARIANTS = new RomAddress(0x05, 0x4932);
    /** Tarin1SpriteVariants at $05:$4912. */
    private static final RomAddress VILLAGENPC_TARIN1_SPRITE_VARIANTS = new RomAddress(0x05, 0x4912);
    /** MarinAtTalTalAndInStoreSpriteVariants at $18:$5EB7. */
    private static final RomAddress VILLAGENPC_MARIN_AT_TAL_TAL_AND_IN_STORE_SPRITE_VARIANTS =
            new RomAddress(0x18, 0x5EB7);
    /** Kid71SpriteVariants at $06:$604D. */
    private static final RomAddress VILLAGENPC_KID71_SPRITE_VARIANTS = new RomAddress(0x06, 0x604D);
    /** MadamMeowMeowSpriteVariants at $06:$5B56. */
    private static final RomAddress VILLAGENPC_MADAM_MEOW_MEOW_SPRITE_VARIANTS = new RomAddress(0x06, 0x5B56);
    /** Data_006_5C51 at $06:$5C51. */
    private static final RomAddress VILLAGENPC_DATA_006_5C51 = new RomAddress(0x06, 0x5C51);

    private static EntitySpriteRecipe villageNpcLookup(
            int entityType, EntityRoomLoader.RoomTable roomTable, int mapId) {
        if (entityType == ENTITY_WITCH_RAT) {
            return EntitySpriteRecipe.pair(
                    entityType,
                    VILLAGENPC_WITCH_RAT_SPRITE_VARIANTS.bank(),
                    VILLAGENPC_WITCH_RAT_SPRITE_VARIANTS.address(),
                    4,
                    0);
        }
        if (entityType == ENTITY_WITCH) {
            return EntitySpriteRecipe.rectangle(
                    entityType, VILLAGENPC_DATA_005_4780.bank(), VILLAGENPC_DATA_005_4780.address(), 4, 4, 0);
        }
        if (entityType == ENTITY_MARIN) {
            return roomTable == EntityRoomLoader.RoomTable.INDOORS_A
                            || roomTable == EntityRoomLoader.RoomTable.INDOORS_B
                    ? EntitySpriteRecipe.pair(
                            entityType,
                            VILLAGENPC_MARIN_INDOOR_SPRITE_VARIANTS.bank(),
                            VILLAGENPC_MARIN_INDOOR_SPRITE_VARIANTS.address(),
                            8,
                            6)
                    : EntitySpriteRecipe.pair(
                            entityType,
                            VILLAGENPC_MARIN_CREDITS_SPRITE_VARIANTS.bank(),
                            VILLAGENPC_MARIN_CREDITS_SPRITE_VARIANTS.address(),
                            11,
                            6);
        }
        if (entityType == ENTITY_TARIN) {
            return roomTable == EntityRoomLoader.RoomTable.INDOORS_A
                            || roomTable == EntityRoomLoader.RoomTable.INDOORS_B
                    ? EntitySpriteRecipe.pair(
                            entityType,
                            VILLAGENPC_TARIN3_SPRITE_VARIANTS.bank(),
                            VILLAGENPC_TARIN3_SPRITE_VARIANTS.address(),
                            4,
                            0)
                    : EntitySpriteRecipe.pair(
                            entityType,
                            VILLAGENPC_TARIN1_SPRITE_VARIANTS.bank(),
                            VILLAGENPC_TARIN1_SPRITE_VARIANTS.address(),
                            12,
                            0);
        }
        if (entityType == ENTITY_MARIN_AT_TAL_TAL_HEIGHTS) {
            return EntitySpriteRecipe.pair(
                    entityType,
                    VILLAGENPC_MARIN_AT_TAL_TAL_AND_IN_STORE_SPRITE_VARIANTS.bank(),
                    VILLAGENPC_MARIN_AT_TAL_TAL_AND_IN_STORE_SPRITE_VARIANTS.address(),
                    8,
                    0);
        }
        if (entityType == ENTITY_KID_70 || entityType == ENTITY_KID_73) {
            return EntitySpriteRecipe.pair(
                    entityType,
                    VILLAGENPC_KID71_SPRITE_VARIANTS.bank(),
                    VILLAGENPC_KID71_SPRITE_VARIANTS.address(),
                    4,
                    0);
        }
        if (entityType == ENTITY_KID_71 || entityType == ENTITY_KID_72) {
            return EntitySpriteRecipe.pair(
                    entityType,
                    VILLAGENPC_KID71_SPRITE_VARIANTS.bank(),
                    entityType == ENTITY_KID_71
                            ? VILLAGENPC_KID71_SPRITE_VARIANTS.address()
                            : VILLAGENPC_KID72_SPRITE_VARIANTS.address(),
                    4,
                    0);
        }
        if (entityType == ENTITY_MADAM_MEOWMEOW) {
            return EntitySpriteRecipe.pair(
                    entityType,
                    VILLAGENPC_MADAM_MEOW_MEOW_SPRITE_VARIANTS.bank(),
                    VILLAGENPC_MADAM_MEOW_MEOW_SPRITE_VARIANTS.address(),
                    8,
                    0);
        }
        if (entityType == 0x75) {
            // GrandmaUlriraEntityHandler $18:4D36/4D58 supplies this pair
            // list before PushLinkOutOfEntity_18 ($18:4D69).
            var variants = linksawakening.rom.RomSymbols.GrandmaUlrira1SpriteVariants;
            return EntitySpriteRecipe.pair(entityType, variants.bank(), variants.address(), 3, 0);
        }
        if (entityType == ENTITY_GRANDPA_ULRIRA) {
            return EntitySpriteRecipe.rectangle(
                    entityType, VILLAGENPC_DATA_006_5C51.bank(), VILLAGENPC_DATA_006_5C51.address(), 2, 4, 0);
        }
        if (entityType == ENTITY_MARIN_AT_THE_SHORE) {
            return EntitySpriteRecipe.pair(
                    entityType,
                    VILLAGENPC_MARIN_AT_TAL_TAL_AND_IN_STORE_SPRITE_VARIANTS.bank(),
                    VILLAGENPC_MARIN_AT_TAL_TAL_AND_IN_STORE_SPRITE_VARIANTS.address(),
                    8,
                    0);
        }
        return null;
    }

    private static final int[] MAPS = {-1, 1, 3, 6, 7, 10, 22, 31, 128, 129, 130, 131};
    private static final EntitySpriteRegistrationData INSTANCE = new EntitySpriteRegistrationData();
    private final EntitySpriteRecipe[][][] recipes;

    private EntitySpriteRegistrationData() {
        var rooms = EntityRoomLoader.RoomTable.values();
        recipes = new EntitySpriteRecipe[rooms.length][MAPS.length][256];
        for (var room : rooms)
            for (int map = 0; map < MAPS.length; map++)
                for (int entity = 0; entity < 256; entity++)
                    recipes[room.ordinal()][map][entity] = select(entity, room, MAPS[map]);
    }

    static EntitySpriteRegistrationData instance() {
        return INSTANCE;
    }

    EntitySpriteRecipe lookup(int entityType, EntityRoomLoader.RoomTable roomTable, int mapId) {
        if ((entityType & ~255) != 0)
            throw new IllegalArgumentException("Entity type must be an unsigned byte: " + entityType);
        if (roomTable == null) throw new IllegalArgumentException("Room entity table cannot be null");
        int map =
                switch (mapId) {
                    case 1 -> 1;
                    case 3 -> 2;
                    case 6 -> 3;
                    case 7 -> 4;
                    case 10 -> 5;
                    case 22 -> 6;
                    case 31 -> 7;
                    default -> mapId < 0 ? 0 : 8 + (mapId & 3);
                };
        return recipes[roomTable.ordinal()][map][entityType];
    }

    private static EntitySpriteRecipe select(int entityType, EntityRoomLoader.RoomTable roomTable, int mapId) {

        EntitySpriteRecipe result;
        result = contextualObjectLookupFloatingItem(entityType, roomTable, mapId);
        if (result != null) return result;
        result = roomObjectLookup(entityType, roomTable, mapId);
        if (result != null) return result;
        result = pickupLookup(entityType, roomTable, mapId);
        if (result != null) return result;
        result = bossLookup(entityType, roomTable, mapId);
        if (result != null) return result;
        result = playerProjectileLookup(entityType, roomTable, mapId);
        if (result != null) return result;
        result = overworldCreatureLookup(entityType, roomTable, mapId);
        if (result != null) return result;
        result = companionLookup(entityType, roomTable, mapId);
        if (result != null) return result;
        result = plantLookup(entityType, roomTable, mapId);
        if (result != null) return result;
        result = aquaticLookup(entityType, roomTable, mapId);
        if (result != null) return result;
        result = undeadEnemyLookup(entityType, roomTable, mapId);
        if (result != null) return result;
        result = villageNpcLookup(entityType, roomTable, mapId);
        if (result != null) return result;
        result = dungeonCreatureLookup(entityType, roomTable, mapId);
        if (result != null) return result;
        result = sideViewLookup(entityType, roomTable, mapId);
        if (result != null) return result;
        result = armoredEnemyLookup(entityType, roomTable, mapId);
        if (result != null) return result;
        result = hazardLookup(entityType, roomTable, mapId);
        if (result != null) return result;
        result = slimeEnemyLookup(entityType, roomTable, mapId);
        if (result != null) return result;

        return EntitySpriteRecipe.special(EntitySpriteRecipe.Kind.UNSUPPORTED, entityType);
    }
}
