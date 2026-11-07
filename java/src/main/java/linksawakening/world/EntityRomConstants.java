package linksawakening.world;

import linksawakening.world.entity.motion.BooBuddyMotion;
import linksawakening.world.entity.motion.BoomerangMotion;
import linksawakening.world.entity.motion.BuzzBlobMotion;
import linksawakening.world.entity.motion.CrowMotion;
import linksawakening.world.entity.motion.DogMotion;
import linksawakening.world.entity.motion.FairyMotion;
import linksawakening.world.entity.motion.FishMotion;
import linksawakening.world.entity.motion.GiantGopongaMotion;
import linksawakening.world.entity.motion.GopongaFlowerMotion;
import linksawakening.world.entity.motion.GopongaProjectileMotion;
import linksawakening.world.entity.motion.HinoxMotion;
import linksawakening.world.entity.motion.HookshotBridgeMotion;
import linksawakening.world.entity.motion.HookshotChainMotion;
import linksawakening.world.entity.motion.PiranhaMotion;
import linksawakening.world.entity.motion.PokeyMotion;
import linksawakening.world.entity.motion.PolsVoiceMotion;
import linksawakening.world.entity.motion.SandCrabMotion;
import linksawakening.world.entity.motion.SpikedBeetleMotion;
import linksawakening.world.entity.motion.TarinRaccoonMotion;
import linksawakening.world.entity.motion.UrchinMotion;
import linksawakening.world.entity.motion.WitchMotion;
import linksawakening.world.entity.motion.WitchRatMotion;
import linksawakening.world.entity.motion.ZombieMotion;
import linksawakening.world.entity.motion.ZoraMotion;

import linksawakening.entity.EntitySpriteHandlerCatalog;

/** Immutable ROM handler identifiers, flags, and lookup constants. */
abstract class EntityRomConstants {
    static final int ENTITY_PUSHED_BLOCK = 0x06;

    static final int[] INSTRUMENT_MUSIC_TRACKS = {
        0x20, 0x28, 0x29, 0x2A, 0x2B, 0x2C, 0x2D, 0x2E
    };

    static final int ENTITY_CHEST_WITH_ITEM = 0x07;

    static final int ENTITY_KEY_DROP_POINT = 0x30;

    static final int ENTITY_MASTER_STALFOS = 0x5F;

    static final int ENTITY_GENIE = 0x5C;

    static final int ENTITY_DESERT_LANMOLA = 0x87;

    static final int ENTITY_ARMOS_KNIGHT = 0x88;

    static final int ENTITY_HINOX = HinoxMotion.ENTITY_TYPE;

    static final int ENTITY_ROLLING_BONES =
        EntitySpriteHandlerCatalog.ENTITY_ROLLING_BONES;

    static final int ENTITY_ROLLING_BONES_BAR =
        EntitySpriteHandlerCatalog.ENTITY_ROLLING_BONES_BAR;

    static final int ENTITY_THREE_OF_A_KIND =
        EntitySpriteHandlerCatalog.ENTITY_THREE_OF_A_KIND;

    static final int ENTITY_PIECE_OF_POWER = 0x33;

    static final int ENTITY_CRYSTAL_SWITCH = 0x66;

    static final int ENTITY_BUTTERFLY = 0x6E;

    static final int ENTITY_KID_71 = 0x71;

    static final int ENTITY_KID_72 = 0x72;

    static final int ENTITY_MADAM_MEOWMEOW = 0x79;

    static final int ENTITY_MOBLIN_KING = 0xE4;

    static final int ENTITY_OCTOROK = 0x09;

    static final int ENTITY_OCTOROK_ROCK = 0x0A;

    static final int ENTITY_MOBLIN = 0x0B;

    static final int ENTITY_IRON_MASK = 0x24;

    static final int ENTITY_IRON_MASKS_MASK = 0x32;

    static final int ENTITY_MOBLIN_ARROW = 0x0C;

    static final int ENTITY_TEKTITE = 0x0D;

    static final int ENTITY_LEEVER = 0x0E;

    static final int ENTITY_ANTI_FAIRY = 0x15;

    static final int ENTITY_SPARK_COUNTER_CLOCKWISE = 0x16;

    static final int ENTITY_SPARK_CLOCKWISE = 0x17;

    static final int ENTITY_ZOL = 0x1B;

    static final int ENTITY_GEL = 0x1C;

    static final int ENTITY_HIDING_ZOL = 0x9B;

    static final int ENTITY_STAR = EntitySpriteHandlerCatalog.ENTITY_STAR;

    static final int ENTITY_BLOOPER = EntitySpriteHandlerCatalog.ENTITY_BLOOPER;

    static final int ENTITY_WINGED_OCTOROK =
        EntitySpriteHandlerCatalog.ENTITY_WINGED_OCTOROK;

    static final int ENTITY_PINCER = EntitySpriteHandlerCatalog.ENTITY_PINCER;

    static final int ENTITY_BUSH_CRAWLER =
        EntitySpriteHandlerCatalog.ENTITY_BUSH_CRAWLER;

    static final int ENTITY_MIMIC = EntitySpriteHandlerCatalog.ENTITY_MIMIC;

    static final int ENTITY_MINI_MOLDORM =
        EntitySpriteHandlerCatalog.ENTITY_MINI_MOLDORM;

    static final int ENTITY_MOLDORM = EntitySpriteHandlerCatalog.ENTITY_MOLDORM;

    static final int ENTITY_MASKED_MIMIC_GORIYA =
        EntitySpriteHandlerCatalog.ENTITY_MASKED_MIMIC_GORIYA;

    static final int ENTITY_SPIKE_TRAP = 0x27;

    static final int ENTITY_PAIRODD = 0x57;

    static final int ENTITY_PAIRODD_PROJECTILE = 0x58;

    static final int ENTITY_WATER_TEKTITE = 0x99;

    static final int ENTITY_FISH = FishMotion.ENTITY_TYPE;

    static final int ENTITY_CROW = CrowMotion.ENTITY_TYPE;

    static final int ENTITY_KNIGHT = 0x51;

    static final int ENTITY_KANALET_BOMBABLE_WALL = 0x94;

    static final int ENTITY_BOO_BUDDY = BooBuddyMotion.ENTITY_TYPE;

    static final int ENTITY_DROPPABLE_FAIRY = FairyMotion.ENTITY_TYPE;

    static final int ENTITY_DROPPABLE_HEART = 0x2D;

    static final int ENTITY_DROPPABLE_RUPEE = 0x2E;

    static final int ENTITY_DROPPABLE_ARROWS = 0x37;

    static final int ENTITY_DROPPABLE_BOMBS = 0x38;

    static final int ENTITY_DROPPABLE_MAGIC_POWDER = 0x3B;

    static final int ENTITY_SLEEPY_TOADSTOOL = 0x3A;

    static final int ENTITY_HIDING_SLIME_KEY = 0x3C;

    static final int ENTITY_RICHARD = 0x95;

    static final int ROOM_OW_POTHOLE_FIELD_SLIME_KEY = 0xC6;

    static final int GOLDEN_LEAVES_FINAL_COUNT = 0x06;

    static final int GOLDEN_LEAVES_FIVE_COUNT = 0x05;

    static final int DIALOG_SLIME_KEY = 0xA2;

    static final int DIALOG_GOLDEN_LEAF = 0xE8;

    static final int DIALOG_FINAL_GOLDEN_LEAF = 0xE9;

    static final int ENTITY_STALFOS_AGGRESSIVE = 0x1A;

    static final int ENTITY_STALFOS_EVASIVE = 0x1E;

    static final int ENTITY_GIBDO = 0x1F;

    static final int ENTITY_LIKE_LIKE = 0x23;

    static final int ENTITY_GOOMBA = 0x9F;

    static final int ENTITY_SNAKE = 0xA1;

    static final int ENTITY_OPT1_NO_GROUND_INTERACTION = 0x10;

    static final int ENTITY_OPT1_NO_WALL_COLLISION = 0x01;

    static final int ENTITY_OPT1_SPLASH_IN_WATER = 0x08;

    static final int ENTITY_OPT1_EXCLUDED_FROM_KILL_ALL = 0x02;

    static final int ENTITY_OPT1_ALLOW_OUT_OF_BOUNDS = 0x20;

    static final int GENIE_OPTIONS1 = 0xD0;

    static final int WIZROBE_PROJECTILE_OPTIONS1 =
        ENTITY_OPT1_NO_GROUND_INTERACTION | ENTITY_OPT1_EXCLUDED_FROM_KILL_ALL;

    static final int ENTITY_PHYSICS_HARMLESS = 0x80;

    static final int ENTITY_PHYSICS_PROJECTILE_NOCLIP = 0x40;

    static final int ENTITY_PHYSICS_SHADOW = 0x10;

    static final int HITFLAGS_IGNORE_HITS = 0x80;

    static final int LIFTABLE_ROCK_SMASH_PHYSICS_FLAGS =
        0x04 | ENTITY_PHYSICS_HARMLESS | ENTITY_PHYSICS_PROJECTILE_NOCLIP
            | 0x10;

    static final int LIFTABLE_ROCK_RUBBLE_PHYSICS_FLAGS =
        0x04 | ENTITY_PHYSICS_HARMLESS | ENTITY_PHYSICS_PROJECTILE_NOCLIP;

    static final int LIFTABLE_ROCK_SMASH_MODE_ROCK = 0;

    static final int LIFTABLE_ROCK_SMASH_MODE_BUSH = 1;

    static final int LIFTABLE_ROCK_SMASH_MODE_GRASS = 0xFF;

    static final int BOMB_INITIAL_PHYSICS_FLAGS = 0xD2;

    static final int CHEST_INITIAL_PHYSICS_FLAGS = 0xC2;

    static final int CHEST_OPTIONS1 = 0x02;

    static final int CHEST_OPEN_NOISE_ID = 0x04;

    static final int CHEST_TREASURE_JINGLE_ID = 0x01;

    static final int BOMB_OPTIONS1 = ENTITY_OPT1_SPLASH_IN_WATER
        | ENTITY_OPT1_EXCLUDED_FROM_KILL_ALL;

    static final int BOMB_EXPLOSION_PHYSICS_LOW_BITS = 0x08;

    static final int BOMB_ARROW_COOLDOWN = 0x06;

    static final int BOMB_ARROW_EXPLOSION_COUNTDOWN = 0x17;

    static final int EVASIVE_PHYSICS_FLAGS = 0x12;

    static final int IRON_MASK_INITIAL_PHYSICS_FLAGS = 0x12;

    static final int GOOMBA_INITIAL_PHYSICS_FLAGS = 0x12;

    static final int EVASIVE_CLONE_PHYSICS_FLAGS = 0x52;

    static final int EVASIVE_CLONE_OPTIONS = ENTITY_OPT1_NO_GROUND_INTERACTION
        | ENTITY_OPT1_SPLASH_IN_WATER | ENTITY_OPT1_EXCLUDED_FROM_KILL_ALL;

    static final int ENTITY_PEAHAT = 0xA0;

    static final int ENTITY_ARMOS_STATUE = 0x0F;

    static final int ENTITY_HIDING_GHINI = 0x10;

    static final int ENTITY_GIANT_GHINI = 0x11;

    static final int ARMOS_INITIAL_PHYSICS_FLAGS = 0x92;

    static final int ARMOS_KNIGHT_INITIAL_PHYSICS_FLAGS = 0x84;

    static final int ARMOS_KNIGHT_OPTIONS1 = 0xC4;

    static final int HINOX_OPTIONS1 = 0x94;

    static final int ARMOS_KNIGHT_INITIAL_HITBOX_FLAGS = 0x80;

    static final int LIKE_LIKE_INITIAL_PHYSICS_FLAGS = 0x92;

    static final int ENTITY_GHINI = 0x12;

    static final int ENTITY_POLS_VOICE = PolsVoiceMotion.ENTITY_TYPE;

    static final int ENTITY_KEESE = 0x19;

    static final int ENTITY_HARDHAT_BEETLE = 0x20;

    static final int ENTITY_SPIKED_BEETLE = SpikedBeetleMotion.ENTITY_TYPE;

    static final int ENTITY_WIZROBE = 0x21;

    static final int ENTITY_WIZROBE_PROJECTILE = 0x22;

    static final int ENTITY_GHOST = 0xD4;

    static final int ENTITY_ROOSTER = 0xD5;

    static final int ENTITY_MARIN_AT_THE_SHORE = 0xC1;

    static final int ENTITY_MARIN_INDOOR = 0x3E;

    static final int ENTITY_TARIN = TarinRaccoonMotion.ENTITY_TYPE;

    static final int ENTITY_BOW_WOW = 0x6D;

    static final int ENTITY_KIKI_THE_MONKEY = 0xAD;

    static final int ENTITY_DROPPABLE_SECRET_SEASHELL = 0x3D;

    static final int SECRET_SEASHELL_HIDDEN_OPTIONS1 =
        ENTITY_OPT1_NO_GROUND_INTERACTION | ENTITY_OPT1_NO_WALL_COLLISION;

    static final int SECRET_SEASHELL_REVEALED_OPTIONS1 =
        ENTITY_OPT1_SPLASH_IN_WATER | ENTITY_OPT1_EXCLUDED_FROM_KILL_ALL;

    static final int DROPPABLE_HIDDEN_OPTIONS1 =
        ENTITY_OPT1_NO_GROUND_INTERACTION | ENTITY_OPT1_NO_WALL_COLLISION;

    static final int HIDING_SLIME_KEY_HIDDEN_OPTIONS1 =
        ENTITY_OPT1_NO_GROUND_INTERACTION | ENTITY_OPT1_SPLASH_IN_WATER
            | ENTITY_OPT1_EXCLUDED_FROM_KILL_ALL | ENTITY_OPT1_NO_WALL_COLLISION;

    static final int DROPPABLE_REVEALED_OPTIONS1 =
        ENTITY_OPT1_SPLASH_IN_WATER | ENTITY_OPT1_EXCLUDED_FROM_KILL_ALL;

    static final int DROPPABLE_HIDDEN_STATE_BURIED = 0x02;

    static final int DROPPABLE_HIDDEN_STATE_PEGASUS = 0x01;

    static final int DROPPABLE_REVEAL_COUNTDOWN = 0x18;

    static final int DROPPABLE_REVEAL_SLOW_COUNTDOWN = 0x80;

    static final int DROPPABLE_REVEAL_VECTOR_LENGTH = 0x0C;

    static final int DROPPABLE_REVEAL_SPEED_Z = 0x20;

    static final int DROPPABLE_OBJECT_SHORT_GRASS = 0x04;

    static final int DROPPABLE_OBJECT_SHOVEL_HOLE = 0xCC;

    static final int ENTITY_HEART_CONTAINER = 0x36;

    static final int ENTITY_MOBLIN_SWORD = 0x14;

    static final int ENTITY_LASER = 0x2A;

    static final int ENTITY_LASER_BEAM = 0x2B;

    static final int ENTITY_ARROW = 0x00;

    static final int ENTITY_BOOMERANG = BoomerangMotion.ENTITY_TYPE;

    static final int ENTITY_MAGIC_ROD_FIREBALL = 0x04;

    static final int ENTITY_MAGIC_POWDER_SPRINKLE = 0x08;

    static final int ENTITY_MUSICAL_NOTE = 0xC9;

    static final int ENTITY_SWORD_BEAM = 0xDF;

    static final int ENTITY_BOMB = 0x02;

    static final int ENTITY_SWORD_SHIELD_PICKUP = 0x31;

    static final int ENTITY_OWL_EVENT = 0x41;

    static final int ROOM_OW_TAIL_KEY_CHEST = 0x41;

    static final int ROOM_OW_EAST_OF_MOBLIN_HIDEOUT = 0x36;

    static final int ROOM_OW_WEST_OF_TAIL_CAVE = 0xD2;

    static final int ENTITY_WITCH = WitchMotion.ENTITY_TYPE;

    static final int ROOM_OW_BEACH_WITH_SWORD = 0xF2;

    static final int ENTITY_BOUNCING_BOMBITE = 0x55;

    static final int ENTITY_TIMER_BOMBITE = 0x56;

    static final int ENTITY_MAD_BOMBER = 0x93;

    static final int ENTITY_BOMBER = 0xBA;

    static final int BOMBITE_INITIAL_PHYSICS_FLAGS = 0x02;

    static final int SWORD_SHIELD_PICKUP_INITIAL_PHYSICS_FLAGS = 0xB1;

    static final int KEY_DROP_POINT_INITIAL_PHYSICS_FLAGS = 0xB1;

    static final int BOMBITE_OPTIONS1 = ENTITY_OPT1_SPLASH_IN_WATER;

    static final int KEY_DROP_POINT_OPTIONS1 = ENTITY_OPT1_SPLASH_IN_WATER
        | ENTITY_OPT1_EXCLUDED_FROM_KILL_ALL;

    static final int BOMBITE_EXPLOSION_SOUND_ID = 0x0C;

    static final int BOMBITE_EXPLOSION_COUNTDOWN = 0x17;

    static final int MAD_BOMBER_INITIAL_PHYSICS_FLAGS = 0x12;

    static final int MAD_BOMBER_OPTIONS1 = ENTITY_OPT1_NO_GROUND_INTERACTION;

    static final int BOMBER_INITIAL_PHYSICS_FLAGS = 0x13;

    static final int BOMBER_OPTIONS1 = ENTITY_OPT1_NO_WALL_COLLISION;

    static final int BOMBER_THROW_JINGLE_ID = 0x08;

    static final int LIFTED_THROW_JINGLE_ID = 0x08;

    static final int DAMAGE_TYPE_ARROW = 0x05;

    static final int DAMAGE_TYPE_BOOMERANG = 0x08;

    static final int DAMAGE_TYPE_MAGIC_ROD = 0x0A;

    static final int DAMAGE_TYPE_MAGIC_POWDER = 0x09;

    static final int DAMAGE_TYPE_SWORD_BEAM = 0x01;

    static final int DAMAGE_TYPE_BOMB = BombExplosionEvent.DAMAGE_TYPE_BOMB;

    static final int DAMAGE_TYPE_BOMB_ARROW = 0x0C;

    static final int DAMAGE_TYPE_THROW_AT = 0x0B;

    static final int ENTITY_HOOKSHOT_CHAIN = HookshotChainMotion.ENTITY_TYPE;

    static final int ENTITY_HOOKSHOT_BRIDGE = HookshotBridgeMotion.ENTITY_TYPE;

    static final int OBJECT_HOOKSHOT_BRIDGE_PULL_DOWN = 0x9E;

    static final int OBJECT_HOOKSHOT_BRIDGE_PULL_UP = 0x9F;

    static final int OBJECT_HOOKSHOT_BRIDGE_REPLACEMENT = 0x9D;

    static final int ENTITY_LIFTABLE_ROCK = 0x05;

    static final int ENTITY_LIFTABLE_STATUE = 0x9D;

    static final int ENTITY_WRECKING_BALL = 0xA8;

    static final int ENTITY_SIDE_VIEW_POT = 0xD6;

    static final int ENTITY_CUCCO = 0x6C;

    static final int ENTITY_GOPONGA_FLOWER = GopongaFlowerMotion.ENTITY_TYPE;

    static final int ENTITY_GIANT_GOPONGA_FLOWER = GiantGopongaMotion.ENTITY_TYPE;

    static final int ENTITY_GOPONGA_FLOWER_PROJECTILE =
        GopongaProjectileMotion.ENTITY_TYPE;

    static final int ENTITY_POKEY = PokeyMotion.ENTITY_TYPE;

    static final int ENTITY_PIRANHA_PLANT = PiranhaMotion.ENTITY_TYPE;

    static final int ENTITY_ZORA = ZoraMotion.ENTITY_TYPE;

    static final int ENTITY_ZOMBIE = ZombieMotion.ENTITY_TYPE;

    static final int ENTITY_BUZZ_BLOB = BuzzBlobMotion.ENTITY_TYPE;

    static final int ENTITY_SAND_CRAB = SandCrabMotion.ENTITY_TYPE;

    static final int ENTITY_DOG = DogMotion.ENTITY_TYPE;

    static final int ENTITY_URCHIN = UrchinMotion.ENTITY_TYPE;

    static final int ENTITY_WITCH_RAT = WitchRatMotion.ENTITY_TYPE;

    static final int ENTITY_HORSE_PIECE = 0x98;

    static final int ENTITY_PHYSICS_GRABBABLE = 0x20;

    static final int OBJECT_BUSH = 0x5C;

    static final int OBJECT_BUSH_GROUND_STAIRS = 0xD3;

    static final int OBJECT_TORCH_UNLIT = 0xAB;

    static final int OBJECT_TORCH_LIT = 0xAC;

    static final int MAGIC_POWDER_TRANSITION_COUNTDOWN = 0x17;

    static final int MAGIC_POWDER_SLOW_TRANSITION_COUNTDOWN = 0x80;

    static final int MAGIC_POWDER_POOF_JINGLE_ID = 0x2F;

    static final int MUSICAL_NOTE_INITIAL_INERTIA = 0x40;

    static final int MUSICAL_NOTE_SPEED_Y = 0xFC;

    static final int BOOMERANG_SFX_ID = 0x2D;

    static final int BOOMERANG_SFX_COUNTER_PERIOD = 0x1A;

    static final int SWORD_BEAM_JINGLE_ID = 0x3B;

    static final int IRON_MASKS_MASK_INITIAL_PHYSICS_FLAGS =
        0x02 | ENTITY_PHYSICS_HARMLESS | ENTITY_PHYSICS_SHADOW | ENTITY_PHYSICS_GRABBABLE;

    static final int MAP_COLOR_DUNGEON = 0xFF;

    static final int DIALOG_MOBLIN = 0x90;

    static final int DIALOG_BOW_WOW = 0x15;

    static final int FALLING_JINGLE_ID = 0x18;

    static final int[] FALLING_VISUAL_Y_OFFSETS = {0, 0, 4, 0};

    static final int[] FALLING_VECTOR_LENGTHS = {0, 1, 3, 6};

    static final int GHINI_OPTIONS1 = ENTITY_OPT1_NO_GROUND_INTERACTION
        | ENTITY_OPT1_NO_WALL_COLLISION;

    static final int IRON_MASKS_MASK_OPTIONS1 = ENTITY_OPT1_SPLASH_IN_WATER
        | ENTITY_OPT1_EXCLUDED_FROM_KILL_ALL;

    static final int FAIRY_INITIAL_PHYSICS_FLAGS = 0xB1;

    static final int FAIRY_OPTIONS1 = ENTITY_OPT1_NO_GROUND_INTERACTION
        | ENTITY_OPT1_NO_WALL_COLLISION | ENTITY_OPT1_EXCLUDED_FROM_KILL_ALL;

}
