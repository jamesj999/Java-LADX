package linksawakening.world.entity;

import static linksawakening.world.entity.EntityRomConstants.*;
import static linksawakening.world.entity.EntityRomConstants.ARMOS_INITIAL_PHYSICS_FLAGS;
import static linksawakening.world.entity.EntityRomConstants.ARMOS_KNIGHT_INITIAL_HITBOX_FLAGS;
import static linksawakening.world.entity.EntityRomConstants.ARMOS_KNIGHT_INITIAL_PHYSICS_FLAGS;
import static linksawakening.world.entity.EntityRomConstants.BOMBER_INITIAL_PHYSICS_FLAGS;
import static linksawakening.world.entity.EntityRomConstants.BOMBITE_INITIAL_PHYSICS_FLAGS;
import static linksawakening.world.entity.EntityRomConstants.BOMB_INITIAL_PHYSICS_FLAGS;
import static linksawakening.world.entity.EntityRomConstants.CHEST_INITIAL_PHYSICS_FLAGS;
import static linksawakening.world.entity.EntityRomConstants.ENTITY_ANTI_FAIRY;
import static linksawakening.world.entity.EntityRomConstants.ENTITY_ARMOS_KNIGHT;
import static linksawakening.world.entity.EntityRomConstants.ENTITY_ARMOS_STATUE;
import static linksawakening.world.entity.EntityRomConstants.ENTITY_BLOOPER;
import static linksawakening.world.entity.EntityRomConstants.ENTITY_BOMB;
import static linksawakening.world.entity.EntityRomConstants.ENTITY_BOMBER;
import static linksawakening.world.entity.EntityRomConstants.ENTITY_BOO_BUDDY;
import static linksawakening.world.entity.EntityRomConstants.ENTITY_BOUNCING_BOMBITE;
import static linksawakening.world.entity.EntityRomConstants.ENTITY_BOW_WOW;
import static linksawakening.world.entity.EntityRomConstants.ENTITY_BUSH_CRAWLER;
import static linksawakening.world.entity.EntityRomConstants.ENTITY_BUZZ_BLOB;
import static linksawakening.world.entity.EntityRomConstants.ENTITY_CHEST_WITH_ITEM;
import static linksawakening.world.entity.EntityRomConstants.ENTITY_CROW;
import static linksawakening.world.entity.EntityRomConstants.ENTITY_CUCCO;
import static linksawakening.world.entity.EntityRomConstants.ENTITY_DESERT_LANMOLA;
import static linksawakening.world.entity.EntityRomConstants.ENTITY_DOG;
import static linksawakening.world.entity.EntityRomConstants.ENTITY_DROPPABLE_ARROWS;
import static linksawakening.world.entity.EntityRomConstants.ENTITY_DROPPABLE_BOMBS;
import static linksawakening.world.entity.EntityRomConstants.ENTITY_DROPPABLE_FAIRY;
import static linksawakening.world.entity.EntityRomConstants.ENTITY_DROPPABLE_HEART;
import static linksawakening.world.entity.EntityRomConstants.ENTITY_DROPPABLE_MAGIC_POWDER;
import static linksawakening.world.entity.EntityRomConstants.ENTITY_DROPPABLE_RUPEE;
import static linksawakening.world.entity.EntityRomConstants.ENTITY_FISH;
import static linksawakening.world.entity.EntityRomConstants.ENTITY_GEL;
import static linksawakening.world.entity.EntityRomConstants.ENTITY_GENIE;
import static linksawakening.world.entity.EntityRomConstants.ENTITY_GHINI;
import static linksawakening.world.entity.EntityRomConstants.ENTITY_GHOST;
import static linksawakening.world.entity.EntityRomConstants.ENTITY_GIANT_GHINI;
import static linksawakening.world.entity.EntityRomConstants.ENTITY_GIANT_GOPONGA_FLOWER;
import static linksawakening.world.entity.EntityRomConstants.ENTITY_GOOMBA;
import static linksawakening.world.entity.EntityRomConstants.ENTITY_GOPONGA_FLOWER;
import static linksawakening.world.entity.EntityRomConstants.ENTITY_GOPONGA_FLOWER_PROJECTILE;
import static linksawakening.world.entity.EntityRomConstants.ENTITY_HARDHAT_BEETLE;
import static linksawakening.world.entity.EntityRomConstants.ENTITY_HEART_CONTAINER;
import static linksawakening.world.entity.EntityRomConstants.ENTITY_HIDING_GHINI;
import static linksawakening.world.entity.EntityRomConstants.ENTITY_HIDING_SLIME_KEY;
import static linksawakening.world.entity.EntityRomConstants.ENTITY_HIDING_ZOL;
import static linksawakening.world.entity.EntityRomConstants.ENTITY_HINOX;
import static linksawakening.world.entity.EntityRomConstants.ENTITY_HORSE_PIECE;
import static linksawakening.world.entity.EntityRomConstants.ENTITY_IRON_MASK;
import static linksawakening.world.entity.EntityRomConstants.ENTITY_IRON_MASKS_MASK;
import static linksawakening.world.entity.EntityRomConstants.ENTITY_KEESE;
import static linksawakening.world.entity.EntityRomConstants.ENTITY_KEY_DROP_POINT;
import static linksawakening.world.entity.EntityRomConstants.ENTITY_LASER;
import static linksawakening.world.entity.EntityRomConstants.ENTITY_LASER_BEAM;
import static linksawakening.world.entity.EntityRomConstants.ENTITY_LEEVER;
import static linksawakening.world.entity.EntityRomConstants.ENTITY_LIFTABLE_ROCK;
import static linksawakening.world.entity.EntityRomConstants.ENTITY_LIFTABLE_STATUE;
import static linksawakening.world.entity.EntityRomConstants.ENTITY_LIKE_LIKE;
import static linksawakening.world.entity.EntityRomConstants.ENTITY_MAD_BOMBER;
import static linksawakening.world.entity.EntityRomConstants.ENTITY_MARIN_AT_THE_SHORE;
import static linksawakening.world.entity.EntityRomConstants.ENTITY_MASKED_MIMIC_GORIYA;
import static linksawakening.world.entity.EntityRomConstants.ENTITY_MASTER_STALFOS;
import static linksawakening.world.entity.EntityRomConstants.ENTITY_MIMIC;
import static linksawakening.world.entity.EntityRomConstants.ENTITY_MINI_MOLDORM;
import static linksawakening.world.entity.EntityRomConstants.ENTITY_MOBLIN;
import static linksawakening.world.entity.EntityRomConstants.ENTITY_MOBLIN_ARROW;
import static linksawakening.world.entity.EntityRomConstants.ENTITY_MOBLIN_KING;
import static linksawakening.world.entity.EntityRomConstants.ENTITY_MOBLIN_SWORD;
import static linksawakening.world.entity.EntityRomConstants.ENTITY_MOLDORM;
import static linksawakening.world.entity.EntityRomConstants.ENTITY_OCTOROK;
import static linksawakening.world.entity.EntityRomConstants.ENTITY_OCTOROK_ROCK;
import static linksawakening.world.entity.EntityRomConstants.ENTITY_PAIRODD;
import static linksawakening.world.entity.EntityRomConstants.ENTITY_PEAHAT;
import static linksawakening.world.entity.EntityRomConstants.ENTITY_PHYSICS_GRABBABLE;
import static linksawakening.world.entity.EntityRomConstants.ENTITY_PHYSICS_HARMLESS;
import static linksawakening.world.entity.EntityRomConstants.ENTITY_PHYSICS_SHADOW;
import static linksawakening.world.entity.EntityRomConstants.ENTITY_PINCER;
import static linksawakening.world.entity.EntityRomConstants.ENTITY_PIRANHA_PLANT;
import static linksawakening.world.entity.EntityRomConstants.ENTITY_POKEY;
import static linksawakening.world.entity.EntityRomConstants.ENTITY_POLS_VOICE;
import static linksawakening.world.entity.EntityRomConstants.ENTITY_ROLLING_BONES;
import static linksawakening.world.entity.EntityRomConstants.ENTITY_ROLLING_BONES_BAR;
import static linksawakening.world.entity.EntityRomConstants.ENTITY_ROOSTER;
import static linksawakening.world.entity.EntityRomConstants.ENTITY_SAND_CRAB;
import static linksawakening.world.entity.EntityRomConstants.ENTITY_SIDE_VIEW_POT;
import static linksawakening.world.entity.EntityRomConstants.ENTITY_SNAKE;
import static linksawakening.world.entity.EntityRomConstants.ENTITY_SPARK_CLOCKWISE;
import static linksawakening.world.entity.EntityRomConstants.ENTITY_SPARK_COUNTER_CLOCKWISE;
import static linksawakening.world.entity.EntityRomConstants.ENTITY_SPIKED_BEETLE;
import static linksawakening.world.entity.EntityRomConstants.ENTITY_STALFOS_AGGRESSIVE;
import static linksawakening.world.entity.EntityRomConstants.ENTITY_STALFOS_EVASIVE;
import static linksawakening.world.entity.EntityRomConstants.ENTITY_STAR;
import static linksawakening.world.entity.EntityRomConstants.ENTITY_SWORD_SHIELD_PICKUP;
import static linksawakening.world.entity.EntityRomConstants.ENTITY_TEKTITE;
import static linksawakening.world.entity.EntityRomConstants.ENTITY_TIMER_BOMBITE;
import static linksawakening.world.entity.EntityRomConstants.ENTITY_URCHIN;
import static linksawakening.world.entity.EntityRomConstants.ENTITY_WATER_TEKTITE;
import static linksawakening.world.entity.EntityRomConstants.ENTITY_WINGED_OCTOROK;
import static linksawakening.world.entity.EntityRomConstants.ENTITY_WITCH_RAT;
import static linksawakening.world.entity.EntityRomConstants.ENTITY_WIZROBE;
import static linksawakening.world.entity.EntityRomConstants.ENTITY_WIZROBE_PROJECTILE;
import static linksawakening.world.entity.EntityRomConstants.ENTITY_WRECKING_BALL;
import static linksawakening.world.entity.EntityRomConstants.ENTITY_ZOL;
import static linksawakening.world.entity.EntityRomConstants.ENTITY_ZOMBIE;
import static linksawakening.world.entity.EntityRomConstants.ENTITY_ZORA;
import static linksawakening.world.entity.EntityRomConstants.EVASIVE_PHYSICS_FLAGS;
import static linksawakening.world.entity.EntityRomConstants.FAIRY_INITIAL_PHYSICS_FLAGS;
import static linksawakening.world.entity.EntityRomConstants.GOOMBA_INITIAL_PHYSICS_FLAGS;
import static linksawakening.world.entity.EntityRomConstants.IRON_MASKS_MASK_INITIAL_PHYSICS_FLAGS;
import static linksawakening.world.entity.EntityRomConstants.IRON_MASK_INITIAL_PHYSICS_FLAGS;
import static linksawakening.world.entity.EntityRomConstants.KEY_DROP_POINT_INITIAL_PHYSICS_FLAGS;
import static linksawakening.world.entity.EntityRomConstants.LIKE_LIKE_INITIAL_PHYSICS_FLAGS;
import static linksawakening.world.entity.EntityRomConstants.MAD_BOMBER_INITIAL_PHYSICS_FLAGS;
import static linksawakening.world.entity.EntityRomConstants.SWORD_SHIELD_PICKUP_INITIAL_PHYSICS_FLAGS;

import linksawakening.world.entity.motion.BooBuddyMotion;
import linksawakening.world.entity.motion.BuzzBlobMotion;
import linksawakening.world.entity.motion.CrowMotion;
import linksawakening.world.entity.motion.CuccoMotion;
import linksawakening.world.entity.motion.DogMotion;
import linksawakening.world.entity.motion.FishMotion;
import linksawakening.world.entity.motion.FloatingItemMotion;
import linksawakening.world.entity.motion.GenieMotion;
import linksawakening.world.entity.motion.GiantGopongaMotion;
import linksawakening.world.entity.motion.GopongaFlowerMotion;
import linksawakening.world.entity.motion.GopongaProjectileMotion;
import linksawakening.world.entity.motion.MoldormMotion;
import linksawakening.world.entity.motion.PiranhaMotion;
import linksawakening.world.entity.motion.PokeyMotion;
import linksawakening.world.entity.motion.RoosterMotion;
import linksawakening.world.entity.motion.SandCrabMotion;
import linksawakening.world.entity.motion.UrchinMotion;
import linksawakening.world.entity.motion.WarpMotion;
import linksawakening.world.entity.motion.WitchRatMotion;
import linksawakening.world.entity.motion.ZombieMotion;
import linksawakening.world.entity.motion.ZoraMotion;

/** EntityInitHandler policies from PhysicsFlagsForEntity, HitboxFlagsForEntity and pickup handlers. */
public final class EntityTypeRules {
    private EntityTypeRules() {}

    static boolean isLiftableEntity(RoomEntity entity) {
        return switch (entity.type()) {
            case ENTITY_BOMB,
                    ENTITY_LIFTABLE_ROCK,
                    ENTITY_LIFTABLE_STATUE,
                    ENTITY_WRECKING_BALL,
                    ENTITY_SIDE_VIEW_POT,
                    ENTITY_ROOSTER,
                    ENTITY_CUCCO,
                    ENTITY_HORSE_PIECE -> true;
            default -> false;
        };
    }

    static boolean checksThrownRoomEventTriggers(int entityType) {
        return entityType != ENTITY_SIDE_VIEW_POT;
    }

    public static boolean isFollowingNpcType(int type) {
        return type == ENTITY_GHOST
                || type == ENTITY_ROOSTER
                || type == ENTITY_MARIN_AT_THE_SHORE
                || type == ENTITY_BOW_WOW;
    }

    public static boolean isGhiniType(int type) {
        return type == ENTITY_HIDING_GHINI || type == ENTITY_GIANT_GHINI || type == ENTITY_GHINI;
    }

    public static boolean isHidingGhiniType(int type) {
        return type == ENTITY_HIDING_GHINI || type == ENTITY_GIANT_GHINI;
    }

    public static boolean isSparkType(int type) {
        return type == ENTITY_SPARK_COUNTER_CLOCKWISE || type == ENTITY_SPARK_CLOCKWISE;
    }

    public static boolean isZolGelType(int type) {
        return type == ENTITY_ZOL || type == ENTITY_GEL;
    }

    public static boolean isRoamingEnemyType(int type) {
        return type == ENTITY_OCTOROK || type == ENTITY_MOBLIN || type == ENTITY_IRON_MASK;
    }

    static boolean usesBank6Recoil(int type) {
        return type == ENTITY_KEESE
                || type == ENTITY_TEKTITE
                || type == ENTITY_ANTI_FAIRY
                || type == ENTITY_STALFOS_AGGRESSIVE
                || type == ENTITY_HARDHAT_BEETLE
                || type == ENTITY_ARMOS_STATUE
                || type == ENTITY_WIZROBE
                || type == ENTITY_CROW
                || type == ENTITY_BOO_BUDDY
                || type == ENTITY_SPARK_COUNTER_CLOCKWISE
                || type == ENTITY_SPARK_CLOCKWISE
                || type == ENTITY_POLS_VOICE
                || type == ENTITY_ZOL
                || type == ENTITY_GEL
                || type == ENTITY_ROLLING_BONES
                || type == ENTITY_LIKE_LIKE
                || type == ENTITY_ARMOS_KNIGHT
                || type == ENTITY_HINOX;
    }

    static boolean usesSharedRecoil(int type) {
        return type == ENTITY_HARDHAT_BEETLE
                || type == ENTITY_LEEVER
                || type == ENTITY_PEAHAT
                || type == ENTITY_WATER_TEKTITE
                || type == ENTITY_STALFOS_EVASIVE
                || type == ENTITY_MOBLIN_SWORD
                || type == ENTITY_BOUNCING_BOMBITE
                || type == ENTITY_TIMER_BOMBITE
                || type == ENTITY_MAD_BOMBER
                || type == ENTITY_BOMBER
                || type == ENTITY_GOOMBA
                || type == ENTITY_SNAKE
                || type == ENTITY_HIDING_ZOL
                || type == ENTITY_STAR
                || type == ENTITY_BLOOPER
                || type == ENTITY_WINGED_OCTOROK
                || type == ENTITY_BUSH_CRAWLER
                || type == ENTITY_PAIRODD
                || type == ENTITY_MIMIC
                || type == ENTITY_MASKED_MIMIC_GORIYA
                || type == ENTITY_MINI_MOLDORM
                || type == ENTITY_ZOMBIE
                || type == ENTITY_BUZZ_BLOB
                || type == ENTITY_SAND_CRAB
                || type == ENTITY_URCHIN
                || isRoamingEnemyType(type)
                || usesBank6Recoil(type)
                || isGhiniType(type);
    }

    public static boolean isBombiteType(int type) {
        return type == ENTITY_BOUNCING_BOMBITE || type == ENTITY_TIMER_BOMBITE;
    }

    public static boolean isEnemyProjectileType(int type) {
        return type == ENTITY_OCTOROK_ROCK || type == ENTITY_MOBLIN_ARROW;
    }

    static boolean isLaserType(int type) {
        return type == ENTITY_LASER || type == ENTITY_LASER_BEAM;
    }

    static boolean isDynamicFollowingNpc(RoomEntity entity) {
        return entity.sourceLoadOrder() == -1 && isFollowingNpcType(entity.type());
    }

    static boolean isBossKeyDropProducer(int entityType) {
        return entityType == ENTITY_MASTER_STALFOS
                || entityType == ENTITY_DESERT_LANMOLA
                || entityType == ENTITY_ARMOS_KNIGHT
                || entityType == ENTITY_HINOX;
    }

    static int initialPhysicsFlags(int type) {
        return switch (type) {
                // PhysicsFlagsForEntity ($03:4000), Zol/Gel entries $1B/$1C.
            case ENTITY_ZOL -> 0x12;
            case ENTITY_GEL -> 0x11;
            case ENTITY_GENIE -> GenieMotion.INITIAL_PHYSICS_FLAGS;
            case ENTITY_ARMOS_STATUE -> ARMOS_INITIAL_PHYSICS_FLAGS;
            case ENTITY_ARMOS_KNIGHT -> ARMOS_KNIGHT_INITIAL_PHYSICS_FLAGS;
            case ENTITY_HINOX -> 0x0C;
            case ENTITY_MOBLIN_KING -> 0x13;
            case ENTITY_ROLLING_BONES_BAR -> 0x42;
            case ENTITY_STALFOS_EVASIVE -> EVASIVE_PHYSICS_FLAGS;
            case ENTITY_STAR -> 0x12;
            case ENTITY_BLOOPER -> 0x02;
            case ENTITY_WINGED_OCTOROK -> 0x12;
            case ENTITY_BUSH_CRAWLER -> 0x12;
            case ENTITY_PINCER -> 0x02;
            case ENTITY_IRON_MASK -> IRON_MASK_INITIAL_PHYSICS_FLAGS;
            case ENTITY_GOOMBA, ENTITY_SNAKE -> GOOMBA_INITIAL_PHYSICS_FLAGS;
            case ENTITY_WIZROBE -> 0x02;
            case ENTITY_MIMIC -> 0x12;
            case ENTITY_MASKED_MIMIC_GORIYA -> 0x12;
            case ENTITY_MINI_MOLDORM -> 0x02;
            case ENTITY_MOLDORM -> MoldormMotion.INITIAL_PHYSICS_FLAGS;
            case ENTITY_SWORD_SHIELD_PICKUP -> SWORD_SHIELD_PICKUP_INITIAL_PHYSICS_FLAGS;
            case ENTITY_KEY_DROP_POINT -> KEY_DROP_POINT_INITIAL_PHYSICS_FLAGS;
            case ENTITY_LIKE_LIKE -> LIKE_LIKE_INITIAL_PHYSICS_FLAGS;
            case ENTITY_POLS_VOICE -> 0x12;
            case ENTITY_SPIKED_BEETLE -> 0x12;
            case ENTITY_WIZROBE_PROJECTILE -> 0x42;
            case ENTITY_IRON_MASKS_MASK -> IRON_MASKS_MASK_INITIAL_PHYSICS_FLAGS;
            case ENTITY_BOMB -> BOMB_INITIAL_PHYSICS_FLAGS;
            case ENTITY_CHEST_WITH_ITEM -> CHEST_INITIAL_PHYSICS_FLAGS;
            case ENTITY_BOUNCING_BOMBITE, ENTITY_TIMER_BOMBITE -> BOMBITE_INITIAL_PHYSICS_FLAGS;
            case ENTITY_FISH -> FishMotion.INITIAL_PHYSICS_FLAGS;
            case ENTITY_CROW -> CrowMotion.INITIAL_PHYSICS_FLAGS;
            case ENTITY_CUCCO -> CuccoMotion.INITIAL_PHYSICS_FLAGS;
            case ENTITY_GOPONGA_FLOWER -> GopongaFlowerMotion.INITIAL_PHYSICS_FLAGS;
            case ENTITY_GIANT_GOPONGA_FLOWER -> GiantGopongaMotion.INITIAL_PHYSICS_FLAGS;
            case ENTITY_GOPONGA_FLOWER_PROJECTILE -> GopongaProjectileMotion.INITIAL_PHYSICS_FLAGS;
            case ENTITY_POKEY -> PokeyMotion.INITIAL_PHYSICS_FLAGS;
            case ENTITY_PIRANHA_PLANT -> PiranhaMotion.INITIAL_PHYSICS_FLAGS;
            case ENTITY_ZORA -> ZoraMotion.INITIAL_PHYSICS_FLAGS;
            case ENTITY_ZOMBIE -> ZombieMotion.INITIAL_PHYSICS_FLAGS;
            case ENTITY_BUZZ_BLOB -> BuzzBlobMotion.INITIAL_PHYSICS_FLAGS;
            case ENTITY_SAND_CRAB -> SandCrabMotion.INITIAL_PHYSICS_FLAGS;
            case ENTITY_DOG -> DogMotion.INITIAL_PHYSICS_FLAGS;
            case ENTITY_URCHIN -> UrchinMotion.INITIAL_PHYSICS_FLAGS;
            case ENTITY_WITCH_RAT -> WitchRatMotion.INITIAL_PHYSICS_FLAGS;
            case ENTITY_ROOSTER -> RoosterMotion.INITIAL_PHYSICS_FLAGS;
            case ENTITY_BOO_BUDDY -> BooBuddyMotion.INITIAL_PHYSICS_FLAGS;
            case ENTITY_DROPPABLE_FAIRY -> FAIRY_INITIAL_PHYSICS_FLAGS;
            case ENTITY_HIDING_SLIME_KEY -> 0x02
                    | ENTITY_PHYSICS_HARMLESS
                    | ENTITY_PHYSICS_SHADOW
                    | ENTITY_PHYSICS_GRABBABLE;
            case ENTITY_MAD_BOMBER -> MAD_BOMBER_INITIAL_PHYSICS_FLAGS;
            case ENTITY_BOMBER -> BOMBER_INITIAL_PHYSICS_FLAGS;
            case ENTITY_LIFTABLE_ROCK,
                    ENTITY_LIFTABLE_STATUE,
                    ENTITY_WRECKING_BALL,
                    ENTITY_SIDE_VIEW_POT,
                    ENTITY_HORSE_PIECE -> ENTITY_PHYSICS_GRABBABLE;
            default -> 0;
        };
    }

    static int initialHitboxFlags(int type) {
        // HitboxFlagsForEntity: Gel's small contact box and the side-view pot's
        // HITFLAGS_COLLISION_BOX_SMALL | HITFLAGS_HITBOX_SIDE_VIEW_POT | HITFLAGS_IGNORE_HITS.
        return switch (type) {
            case ENTITY_GEL -> 0x10;
            case ENTITY_GENIE -> GenieMotion.INITIAL_HITBOX_FLAGS;
            case ENTITY_SIDE_VIEW_POT -> 0xBD;
            case ENTITY_MOLDORM -> MoldormMotion.INITIAL_HITBOX_FLAGS;
            case ENTITY_MOBLIN_KING -> 0x84;
            case ENTITY_ARMOS_KNIGHT -> ARMOS_KNIGHT_INITIAL_HITBOX_FLAGS;
            case ENTITY_HINOX -> 0x08;
            default -> 0;
        };
    }

    public static int persistentClearMask(RoomEntity entity) {
        return entity.sourceLoadOrder() >= 0 && entity.sourceLoadOrder() < 8 ? 1 << entity.sourceLoadOrder() : 0;
    }

    static boolean requiresHeldPickupTransition(int type) {
        return switch (type) {
            case ENTITY_KEY_DROP_POINT,
                    ENTITY_SWORD_SHIELD_PICKUP,
                    ENTITY_HEART_CONTAINER,
                    ENTITY_HIDING_SLIME_KEY -> true;
            default -> false;
        };
    }

    static boolean isRuntimeFloatingItem(RoomEntity entity) {
        return FloatingItemMotion.isFloatingItem(entity.type())
                && entity.spriteDefinition().supported();
    }

    public static boolean isCommonDroppableType(int type) {
        return type == ENTITY_DROPPABLE_HEART
                || type == ENTITY_DROPPABLE_RUPEE
                || type == ENTITY_DROPPABLE_FAIRY
                || type == ENTITY_DROPPABLE_ARROWS
                || type == ENTITY_DROPPABLE_BOMBS
                || type == ENTITY_DROPPABLE_MAGIC_POWDER
                || type == ENTITY_HIDING_SLIME_KEY;
    }

    static int variantFor(RoomEntity entity, int frameCounter) {
        if (!entity.spriteDefinition().supported() || entity.spriteDefinition().variantCount() < 2) {
            return entity.spriteVariant();
        }
        return switch (entity.type()) {
            case ENTITY_PIECE_OF_POWER -> (frameCounter >>> 3) & 0x01;
            case ENTITY_BOMBER -> (frameCounter >>> 2) & 0x03;
            case ENTITY_BUTTERFLY -> ((frameCounter + entity.slot() * 8) >>> 3) & 0x01;
            case ENTITY_ARMOS_STATUE -> (frameCounter >>> 4) & 0x01;
            case ENTITY_GHINI -> ((frameCounter >>> 4) ^ entity.slot()) & 0x01;
            case ENTITY_HARDHAT_BEETLE -> (frameCounter >>> 3) & 0x01;
            case ENTITY_SPARK_COUNTER_CLOCKWISE, ENTITY_SPARK_CLOCKWISE -> (frameCounter >>> 1) & 0x01;
            case 0x70, 0x73, 0x75 -> (frameCounter >>> 4) & 0x01;
            default -> entity.spriteVariant();
        };
    }

    public static boolean isDungeonWarpType(int type) {
        return type == WarpMotion.ENTITY_TYPE;
    }
}
