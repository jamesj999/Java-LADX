package linksawakening.world;

import linksawakening.entity.EntitySpriteSelection;
import linksawakening.entity.EntitySpriteDefinition;
import linksawakening.entity.EntitySpriteHandlerCatalog;
import linksawakening.gpu.EntitySpriteTileSnapshot;
import linksawakening.vfx.TransientVfxType;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Objects;
import java.util.function.IntSupplier;

/**
 * Mutable per-room entity state for the handler work that can be expressed by
 * the current room snapshot model. ROM display lists remain immutable; this
 * class only advances handler-owned status and sprite-variant fields.
 */
public final class RoomEntityRuntime {
    private static final int ENTITY_CHEST_WITH_ITEM = 0x07;
    private static final int ENTITY_KEY_DROP_POINT = 0x30;
    private static final int ENTITY_MASTER_STALFOS = 0x5F;
    private static final int ENTITY_DESERT_LANMOLA = 0x87;
    private static final int ENTITY_ARMOS_KNIGHT = 0x88;
    private static final int ENTITY_PIECE_OF_POWER = 0x33;
    private static final int ENTITY_CRYSTAL_SWITCH = 0x66;
    private static final int ENTITY_BUTTERFLY = 0x6E;
    private static final int ENTITY_OCTOROK = 0x09;
    private static final int ENTITY_OCTOROK_ROCK = 0x0A;
    private static final int ENTITY_MOBLIN = 0x0B;
    private static final int ENTITY_IRON_MASK = 0x24;
    private static final int ENTITY_IRON_MASKS_MASK = 0x32;
    private static final int ENTITY_MOBLIN_ARROW = 0x0C;
    private static final int ENTITY_TEKTITE = 0x0D;
    private static final int ENTITY_LEEVER = 0x0E;
    private static final int ENTITY_ANTI_FAIRY = 0x15;
    private static final int ENTITY_SPARK_COUNTER_CLOCKWISE = 0x16;
    private static final int ENTITY_SPARK_CLOCKWISE = 0x17;
    private static final int ENTITY_ZOL = 0x1B;
    private static final int ENTITY_GEL = 0x1C;
    private static final int ENTITY_HIDING_ZOL = 0x9B;
    private static final int ENTITY_SPIKE_TRAP = 0x27;
    private static final int ENTITY_PAIRODD = 0x57;
    private static final int ENTITY_PAIRODD_PROJECTILE = 0x58;
    private static final int ENTITY_WATER_TEKTITE = 0x99;
    private static final int ENTITY_FISH = FishMotion.ENTITY_TYPE;
    private static final int ENTITY_CROW = CrowMotion.ENTITY_TYPE;
    private static final int ENTITY_BOO_BUDDY = BooBuddyMotion.ENTITY_TYPE;
    private static final int ENTITY_DROPPABLE_FAIRY = FairyMotion.ENTITY_TYPE;
    private static final int ENTITY_STALFOS_AGGRESSIVE = 0x1A;
    private static final int ENTITY_STALFOS_EVASIVE = 0x1E;
    private static final int ENTITY_GIBDO = 0x1F;
    private static final int ENTITY_LIKE_LIKE = 0x23;
    private static final int ENTITY_GOOMBA = 0x9F;
    private static final int ENTITY_SNAKE = 0xA1;
    private static final int ENTITY_OPT1_NO_GROUND_INTERACTION = 0x10;
    private static final int ENTITY_OPT1_NO_WALL_COLLISION = 0x01;
    private static final int ENTITY_OPT1_SPLASH_IN_WATER = 0x08;
    private static final int ENTITY_OPT1_EXCLUDED_FROM_KILL_ALL = 0x02;
    private static final int ENTITY_OPT1_ALLOW_OUT_OF_BOUNDS = 0x20;
    private static final int WIZROBE_PROJECTILE_OPTIONS1 =
        ENTITY_OPT1_NO_GROUND_INTERACTION | ENTITY_OPT1_EXCLUDED_FROM_KILL_ALL;
    private static final int ENTITY_PHYSICS_HARMLESS = 0x80;
    private static final int ENTITY_PHYSICS_PROJECTILE_NOCLIP = 0x40;
    private static final int ENTITY_PHYSICS_SHADOW = 0x10;
    private static final int HITFLAGS_IGNORE_HITS = 0x80;
    private static final int LIFTABLE_ROCK_SMASH_PHYSICS_FLAGS =
        0x04 | ENTITY_PHYSICS_HARMLESS | ENTITY_PHYSICS_PROJECTILE_NOCLIP
            | 0x10;
    private static final int LIFTABLE_ROCK_RUBBLE_PHYSICS_FLAGS =
        0x04 | ENTITY_PHYSICS_HARMLESS | ENTITY_PHYSICS_PROJECTILE_NOCLIP;
    private static final int LIFTABLE_ROCK_SMASH_MODE_ROCK = 0;
    private static final int LIFTABLE_ROCK_SMASH_MODE_BUSH = 1;
    private static final int LIFTABLE_ROCK_SMASH_MODE_GRASS = 0xFF;
    private static final int BOMB_INITIAL_PHYSICS_FLAGS = 0xD2;
    private static final int CHEST_INITIAL_PHYSICS_FLAGS = 0xC2;
    private static final int CHEST_OPTIONS1 = 0x02;
    private static final int CHEST_OPEN_NOISE_ID = 0x04;
    private static final int CHEST_TREASURE_JINGLE_ID = 0x01;
    private static final int BOMB_OPTIONS1 = ENTITY_OPT1_SPLASH_IN_WATER
        | ENTITY_OPT1_EXCLUDED_FROM_KILL_ALL;
    private static final int BOMB_EXPLOSION_PHYSICS_LOW_BITS = 0x08;
    private static final int BOMB_ARROW_COOLDOWN = 0x06;
    private static final int BOMB_ARROW_EXPLOSION_COUNTDOWN = 0x17;
    private static final int EVASIVE_PHYSICS_FLAGS = 0x12;
    private static final int IRON_MASK_INITIAL_PHYSICS_FLAGS = 0x12;
    private static final int GOOMBA_INITIAL_PHYSICS_FLAGS = 0x12;
    private static final int EVASIVE_CLONE_PHYSICS_FLAGS = 0x52;
    private static final int EVASIVE_CLONE_OPTIONS = ENTITY_OPT1_NO_GROUND_INTERACTION
        | ENTITY_OPT1_SPLASH_IN_WATER | ENTITY_OPT1_EXCLUDED_FROM_KILL_ALL;
    private static final int ENTITY_PEAHAT = 0xA0;
    private static final int ENTITY_ARMOS_STATUE = 0x0F;
    private static final int ENTITY_HIDING_GHINI = 0x10;
    private static final int ENTITY_GIANT_GHINI = 0x11;
    private static final int ARMOS_INITIAL_PHYSICS_FLAGS = 0x92;
    private static final int ARMOS_KNIGHT_INITIAL_PHYSICS_FLAGS = 0x84;
    private static final int ARMOS_KNIGHT_OPTIONS1 = 0xC4;
    private static final int ARMOS_KNIGHT_INITIAL_HITBOX_FLAGS = 0x80;
    private static final int LIKE_LIKE_INITIAL_PHYSICS_FLAGS = 0x92;
    private static final int ENTITY_GHINI = 0x12;
    private static final int ENTITY_POLS_VOICE = PolsVoiceMotion.ENTITY_TYPE;
    private static final int ENTITY_KEESE = 0x19;
    private static final int ENTITY_HARDHAT_BEETLE = 0x20;
    private static final int ENTITY_SPIKED_BEETLE = SpikedBeetleMotion.ENTITY_TYPE;
    private static final int ENTITY_WIZROBE = 0x21;
    private static final int ENTITY_WIZROBE_PROJECTILE = 0x22;
    private static final int ENTITY_GHOST = 0xD4;
    private static final int ENTITY_ROOSTER = 0xD5;
    private static final int ENTITY_MARIN_AT_THE_SHORE = 0xC1;
    private static final int ENTITY_BOW_WOW = 0x6D;
    private static final int ENTITY_HEART_CONTAINER = 0x36;
    private static final int ENTITY_MOBLIN_SWORD = 0x14;
    private static final int ENTITY_LASER = 0x2A;
    private static final int ENTITY_LASER_BEAM = 0x2B;
    private static final int ENTITY_ARROW = 0x00;
    private static final int ENTITY_BOOMERANG = BoomerangMotion.ENTITY_TYPE;
    private static final int ENTITY_MAGIC_ROD_FIREBALL = 0x04;
    private static final int ENTITY_MAGIC_POWDER_SPRINKLE = 0x08;
    private static final int ENTITY_MUSICAL_NOTE = 0xC9;
    private static final int ENTITY_SWORD_BEAM = 0xDF;
    private static final int ENTITY_BOMB = 0x02;
    private static final int ENTITY_SWORD_SHIELD_PICKUP = 0x31;
    private static final int ENTITY_BOUNCING_BOMBITE = 0x55;
    private static final int ENTITY_TIMER_BOMBITE = 0x56;
    private static final int ENTITY_MAD_BOMBER = 0x93;
    private static final int ENTITY_BOMBER = 0xBA;
    private static final int BOMBITE_INITIAL_PHYSICS_FLAGS = 0x02;
    private static final int SWORD_SHIELD_PICKUP_INITIAL_PHYSICS_FLAGS = 0xB1;
    private static final int KEY_DROP_POINT_INITIAL_PHYSICS_FLAGS = 0xB1;
    private static final int BOMBITE_OPTIONS1 = ENTITY_OPT1_SPLASH_IN_WATER;
    private static final int KEY_DROP_POINT_OPTIONS1 = ENTITY_OPT1_SPLASH_IN_WATER
        | ENTITY_OPT1_EXCLUDED_FROM_KILL_ALL;
    private static final int BOMBITE_EXPLOSION_SOUND_ID = 0x0C;
    private static final int BOMBITE_EXPLOSION_COUNTDOWN = 0x17;
    private static final int MAD_BOMBER_INITIAL_PHYSICS_FLAGS = 0x12;
    private static final int MAD_BOMBER_OPTIONS1 = ENTITY_OPT1_NO_GROUND_INTERACTION;
    private static final int BOMBER_INITIAL_PHYSICS_FLAGS = 0x13;
    private static final int BOMBER_OPTIONS1 = ENTITY_OPT1_NO_WALL_COLLISION;
    private static final int BOMBER_THROW_JINGLE_ID = 0x08;
    private static final int DAMAGE_TYPE_ARROW = 0x05;
    private static final int DAMAGE_TYPE_BOOMERANG = 0x08;
    private static final int DAMAGE_TYPE_MAGIC_ROD = 0x0A;
    private static final int DAMAGE_TYPE_MAGIC_POWDER = 0x09;
    private static final int DAMAGE_TYPE_SWORD_BEAM = 0x01;
    private static final int DAMAGE_TYPE_BOMB = BombExplosionEvent.DAMAGE_TYPE_BOMB;
    private static final int DAMAGE_TYPE_BOMB_ARROW = 0x0C;
    private static final int ENTITY_HOOKSHOT_CHAIN = HookshotChainMotion.ENTITY_TYPE;
    private static final int ENTITY_HOOKSHOT_BRIDGE = HookshotBridgeMotion.ENTITY_TYPE;
    private static final int OBJECT_HOOKSHOT_BRIDGE_PULL_DOWN = 0x9E;
    private static final int OBJECT_HOOKSHOT_BRIDGE_PULL_UP = 0x9F;
    private static final int OBJECT_HOOKSHOT_BRIDGE_REPLACEMENT = 0x9D;
    private static final int ENTITY_LIFTABLE_ROCK = 0x05;
    private static final int ENTITY_LIFTABLE_STATUE = 0x9D;
    private static final int ENTITY_WRECKING_BALL = 0xA8;
    private static final int ENTITY_SIDE_VIEW_POT = 0xD6;
    private static final int ENTITY_CUCCO = 0x6C;
    private static final int ENTITY_HORSE_PIECE = 0x98;
    private static final int ENTITY_PHYSICS_GRABBABLE = 0x20;
    private static final int OBJECT_BUSH = 0x5C;
    private static final int OBJECT_BUSH_GROUND_STAIRS = 0xD3;
    private static final int OBJECT_TORCH_UNLIT = 0xAB;
    private static final int OBJECT_TORCH_LIT = 0xAC;
    private static final int MAGIC_POWDER_TRANSITION_COUNTDOWN = 0x17;
    private static final int MAGIC_POWDER_SLOW_TRANSITION_COUNTDOWN = 0x80;
    private static final int MAGIC_POWDER_POOF_JINGLE_ID = 0x2F;
    private static final int MUSICAL_NOTE_INITIAL_INERTIA = 0x40;
    private static final int MUSICAL_NOTE_SPEED_Y = 0xFC;
    private static final int BOOMERANG_SFX_ID = 0x2D;
    private static final int BOOMERANG_SFX_COUNTER_PERIOD = 0x1A;
    private static final int SWORD_BEAM_JINGLE_ID = 0x3B;
    private static final int IRON_MASKS_MASK_INITIAL_PHYSICS_FLAGS =
        0x02 | ENTITY_PHYSICS_HARMLESS | ENTITY_PHYSICS_SHADOW | ENTITY_PHYSICS_GRABBABLE;
    private static final int MAP_COLOR_DUNGEON = 0xFF;
    private static final int DIALOG_MOBLIN = 0x90;
    private static final int FALLING_JINGLE_ID = 0x18;
    private static final int[] FALLING_VISUAL_Y_OFFSETS = {0, 0, 4, 0};
    private static final int[] FALLING_VECTOR_LENGTHS = {0, 1, 3, 6};
    private static final int GHINI_OPTIONS1 = ENTITY_OPT1_NO_GROUND_INTERACTION
        | ENTITY_OPT1_NO_WALL_COLLISION;
    private static final int IRON_MASKS_MASK_OPTIONS1 = ENTITY_OPT1_SPLASH_IN_WATER
        | ENTITY_OPT1_EXCLUDED_FROM_KILL_ALL;
    private static final int FAIRY_INITIAL_PHYSICS_FLAGS = 0xB1;
    private static final int FAIRY_OPTIONS1 = ENTITY_OPT1_NO_GROUND_INTERACTION
        | ENTITY_OPT1_NO_WALL_COLLISION | ENTITY_OPT1_EXCLUDED_FROM_KILL_ALL;

    private final RoomEntity[] slots;
    private EntitySpriteSelection spriteSelection;
    private final EntitySpriteTileSnapshot spriteTiles;
    private final boolean indoorRoom;
    private final IntSupplier defaultRandomByteSupplier;
    private final RomRandomByteSource fallbackRomRandomByteSource;
    private final EntitySpriteHandlerCatalog spriteHandlers;
    private final RomEnemyCombatTables enemyCombatTables;
    private final ChestContentsTable chestContentsTable;
    private RoomEntityGroundInteraction groundInteraction =
        (entity, frameCounter, previousGroundStatus, speedZ, sideScrolling) ->
            RoomEntityGroundInteraction.Result.unchanged(entity, 0);
    private RoomEntityBackgroundInteraction backgroundInteraction;
    private FollowingNpcState followingNpcState = FollowingNpcState.none();
    private LinkPositionHistory followingLinkPositionHistory = new LinkPositionHistory();
    private int followingLinkZ;
    private int followingLinkDirection;
    private int lastRomLinkDirection;
    private int followingEntityYOffset;
    private final ButterflyMotion butterflyMotion = new ButterflyMotion();
    private final KeeseMotion keeseMotion = new KeeseMotion();
    private final RoamingEnemyMotion roamingEnemyMotion = new RoamingEnemyMotion();
    private final UnmaskedIronMaskMotion unmaskedIronMaskMotion =
        new UnmaskedIronMaskMotion();
    private final MoblinSwordMotion moblinSwordMotion = new MoblinSwordMotion();
    private final PlayerArrowMotion playerArrowMotion = new PlayerArrowMotion();
    private final EnemyProjectileMotion enemyProjectileMotion = new EnemyProjectileMotion();
    private final HookshotChainMotion hookshotChainMotion = new HookshotChainMotion();
    private final HookshotBridgeMotion hookshotBridgeMotion = new HookshotBridgeMotion();
    private final LaserMotion laserMotion = new LaserMotion();
    private final TektiteMotion tektiteMotion = new TektiteMotion();
    private final LeeverMotion leeverMotion = new LeeverMotion();
    private final AntiFairyMotion antiFairyMotion = new AntiFairyMotion();
    private final SparkMotion sparkMotion = new SparkMotion();
    private final ZolGelMotion zolGelMotion = new ZolGelMotion();
    private final HidingZolMotion hidingZolMotion = new HidingZolMotion();
    private final SpikeTrapMotion spikeTrapMotion = new SpikeTrapMotion();
    private final PairoddMotion pairoddMotion = new PairoddMotion();
    private final PairoddProjectileMotion pairoddProjectileMotion =
        new PairoddProjectileMotion();
    private final WaterTektiteMotion waterTektiteMotion = new WaterTektiteMotion();
    private final FishMotion fishMotion = new FishMotion();
    private final CrowMotion crowMotion = new CrowMotion();
    private final BooBuddyMotion booBuddyMotion = new BooBuddyMotion();
    private final FairyMotion fairyMotion = new FairyMotion();
    private final StalfosAggressiveMotion stalfosAggressiveMotion =
        new StalfosAggressiveMotion();
    private final StalfosEvasiveMotion stalfosEvasiveMotion = new StalfosEvasiveMotion();
    private final GibdoMotion gibdoMotion = new GibdoMotion();
    private final LikeLikeMotion likeLikeMotion = new LikeLikeMotion();
    private final GoombaMotion goombaMotion = new GoombaMotion();
    private final SnakeMotion snakeMotion = new SnakeMotion();
    private final WizrobeMotion wizrobeMotion = new WizrobeMotion();
    private final WizrobeProjectileMotion wizrobeProjectileMotion =
        new WizrobeProjectileMotion();
    private final PeaHatMotion peaHatMotion = new PeaHatMotion();
    private final ArmosMotion armosMotion = new ArmosMotion();
    private final ArmosKnightMotion armosKnightMotion = new ArmosKnightMotion();
    private final BossIntroMotion bossIntroMotion = new BossIntroMotion();
    private final GhiniMotion ghiniMotion = new GhiniMotion();
    private final HardHatMotion hardHatMotion = new HardHatMotion();
    private final PolsVoiceMotion polsVoiceMotion = new PolsVoiceMotion();
    private final SpikedBeetleMotion spikedBeetleMotion = new SpikedBeetleMotion();
    private final MadBomberMotion madBomberMotion = new MadBomberMotion();
    private final BomberMotion bomberMotion = new BomberMotion();
    private final BombiteMotion bombiteMotion = new BombiteMotion();
    private final EnemyRecoilMotion enemyRecoilMotion = new EnemyRecoilMotion();
    private final FollowingNpcMotion followingNpcMotion = new FollowingNpcMotion();
    private final BowWowMotion bowWowMotion = new BowWowMotion();
    private final ColorShellMotion colorShellMotion = new ColorShellMotion();
    private final ThrownEntityMotion thrownEntityMotion = new ThrownEntityMotion();
    private final int[] slowTransitionCountdown = new int[EntityRoomLoader.MAX_ENTITIES];
    private final boolean[] slowTimerInitialized = new boolean[EntityRoomLoader.MAX_ENTITIES];
    private final int[] droppedItemBySlot = new int[EntityRoomLoader.MAX_ENTITIES];
    private final int[] dropPrivateCountdown1 = new int[EntityRoomLoader.MAX_ENTITIES];
    private final int[] dropPrivateCountdown3 = new int[EntityRoomLoader.MAX_ENTITIES];
    private final boolean[] enemyDropActive = new boolean[EntityRoomLoader.MAX_ENTITIES];
    private final EnemyDropMotion enemyDropMotion = new EnemyDropMotion();
    private final int[] enemyTransitionCountdown = new int[EntityRoomLoader.MAX_ENTITIES];
    private final int[] bossDeathProducerState = new int[EntityRoomLoader.MAX_ENTITIES];
    private final int[] enemyStunnedCountdown = new int[EntityRoomLoader.MAX_ENTITIES];
    private final int[] dyingCountdown = new int[EntityRoomLoader.MAX_ENTITIES];
    private final boolean[] powerRecoilDeath = new boolean[EntityRoomLoader.MAX_ENTITIES];
    private final int[] enemyPhysicsFlags = new int[EntityRoomLoader.MAX_ENTITIES];
    private final int[] enemyHealth = new int[EntityRoomLoader.MAX_ENTITIES];
    private final int[] ironMaskPrivateState2 = new int[EntityRoomLoader.MAX_ENTITIES];
    private final int[] ironMasksMaskSourceHookshotSlot =
        new int[EntityRoomLoader.MAX_ENTITIES];
    private final int[] enemyFlashCountdown = new int[EntityRoomLoader.MAX_ENTITIES];
    private final int[] enemyIgnoreHitsCountdown = new int[EntityRoomLoader.MAX_ENTITIES];
    private final int[] enemyHitboxFlags = new int[EntityRoomLoader.MAX_ENTITIES];
    private final int[] entityGroundStatus = new int[EntityRoomLoader.MAX_ENTITIES];
    private final int[] fallingTargetX = new int[EntityRoomLoader.MAX_ENTITIES];
    private final int[] fallingTargetY = new int[EntityRoomLoader.MAX_ENTITIES];
    private final int[] fallingSpeedX = new int[EntityRoomLoader.MAX_ENTITIES];
    private final int[] fallingSpeedY = new int[EntityRoomLoader.MAX_ENTITIES];
    private final int[] fallingSpeedXAccumulator = new int[EntityRoomLoader.MAX_ENTITIES];
    private final int[] fallingSpeedYAccumulator = new int[EntityRoomLoader.MAX_ENTITIES];
    private final int[] fallingVisualYOffset = new int[EntityRoomLoader.MAX_ENTITIES];
    private final int[] baseEntityFlipAttribute = new int[EntityRoomLoader.MAX_ENTITIES];
    private final int[] entityOptions1Override = new int[EntityRoomLoader.MAX_ENTITIES];
    private final boolean[] enemyProjectileSpawnedThisFrame =
        new boolean[EntityRoomLoader.MAX_ENTITIES];
    private final boolean[] dynamicEntitySpawnedThisFrame =
        new boolean[EntityRoomLoader.MAX_ENTITIES];
    private List<HookshotChainOam.Entry> hookshotChainOam = List.of();
    private final int[] liftedPhase = new int[EntityRoomLoader.MAX_ENTITIES];
    private final int[] liftedSourceDirection = new int[EntityRoomLoader.MAX_ENTITIES];
    private final boolean[] liftedStateInitialized = new boolean[EntityRoomLoader.MAX_ENTITIES];
    private final int[] bombDirection = new int[EntityRoomLoader.MAX_ENTITIES];
    private final boolean[] playerArrowBombArrow =
        new boolean[EntityRoomLoader.MAX_ENTITIES];
    private final int[] bombPrivateCountdown1 = new int[EntityRoomLoader.MAX_ENTITIES];
    private final int[] bombPrivateCountdown3 = new int[EntityRoomLoader.MAX_ENTITIES];
    private final int[] liftableRockSmashCountdown =
        new int[EntityRoomLoader.MAX_ENTITIES];
    private final int[] liftableRockSmashSourceVariant =
        new int[EntityRoomLoader.MAX_ENTITIES];
    private final boolean[] liftableRockSmashActive =
        new boolean[EntityRoomLoader.MAX_ENTITIES];
    private final boolean[] bombFinalPresentationPending =
        new boolean[EntityRoomLoader.MAX_ENTITIES];
    private final int[] bombPrivateState4 = new int[EntityRoomLoader.MAX_ENTITIES];
    private final int[] magicPowderPrivateState4 = new int[EntityRoomLoader.MAX_ENTITIES];
    private final int[] entityUnknownJ = new int[EntityRoomLoader.MAX_ENTITIES];
    private final boolean[] placedBombMotionInitialized =
        new boolean[EntityRoomLoader.MAX_ENTITIES];
    private final boolean[] enemyBombMotionInitialized =
        new boolean[EntityRoomLoader.MAX_ENTITIES];
    private final int[] thrownDirection = new int[EntityRoomLoader.MAX_ENTITIES];
    private final int[] ledgeTransitionTimer = new int[EntityRoomLoader.MAX_ENTITIES];
    private final boolean[] thrownMotionInitialized = new boolean[EntityRoomLoader.MAX_ENTITIES];
    private final int[] chestSpeedY = new int[EntityRoomLoader.MAX_ENTITIES];
    private final int[] chestSpeedYAccumulator = new int[EntityRoomLoader.MAX_ENTITIES];
    private final int[] chestInertia = new int[EntityRoomLoader.MAX_ENTITIES];
    private final int[] chestItemBySlot = new int[EntityRoomLoader.MAX_ENTITIES];
    private final List<RoamingEnemyMotion.LaunchRequest> projectileLaunchRequests =
        new ArrayList<>();
    private final List<TransientVfxRequest> transientVfxRequests = new ArrayList<>();
    private final List<LinkFinalPositionRequest> pendingLinkFinalPositionRequests =
        new ArrayList<>();
    private final List<LinkMotionBlockRequest> pendingLinkMotionBlockRequests =
        new ArrayList<>();
    private final List<ScreenShakeRequest> pendingScreenShakeRequests = new ArrayList<>();
    private final List<DialogRequest> pendingDialogRequests = new ArrayList<>();
    private final List<EntityCombatEvent> pendingEntityEvents = new ArrayList<>();
    private final List<ChestRewardEvent> pendingChestRewardEvents = new ArrayList<>();
    private final List<KeyRewardEvent> pendingKeyRewardEvents = new ArrayList<>();
    private final List<KeyQuicksandEvent> pendingKeyQuicksandEvents = new ArrayList<>();
    private final List<LikeLikeEvent> pendingLikeLikeEvents = new ArrayList<>();
    private final List<BombExplosionEvent> pendingBombExplosionEvents = new ArrayList<>();
    private final List<HookshotBridgeUpdate> hookshotBridgeUpdates = new ArrayList<>();
    private boolean switchBlockAnimationActive;
    private boolean pendingSwitchBlockAnimationRequest;
    private ColorShellWorld colorShellWorld = ColorShellWorld.none();
    private int pendingClearedEntityMask;
    private EnemyDropResolver enemyDropResolver;
    private EnemyDropResolver.CounterState enemyDropCounters =
        new EnemyDropResolver.CounterState(0, 0);
    private int enemyDropMaxHearts = 3;
    private int enemyDropHealth = 6;
    private boolean enemyDropActivePowerUp;
    private boolean enemyDropBossBattle;
    private int killCount;
    private int booBuddyTriggerCount;
    private final int[] killOrder = new int[0x100];
    private boolean actionButtonsHeld;
    private int linkItemA;
    private int linkItemB;
    private boolean powerBraceletButtonHeld;
    private boolean bombButtonHeld;
    private boolean runningWithPegasusBoots;
    // The three WRAM values read by PolsVoiceEntityHandler's Ocarina preamble.
    // RoomSession supplies these from the active Ocarina item state once per
    // entity tick; the zero values preserve the ordinary no-song path.
    private int linkPlayingOcarinaCountdown;
    private int ocarinaSongFlags;
    private int selectedSongIndex;
    private int ocarinaAnimationCounter;
    private int ocarinaAnimationPhase;
    // LinkMotionMapFadeInHandler leaves wTransitionSequenceCounter at $04 once
    // the active room is ready for interaction. Entity ticks are gated during
    // the host transition, so this is the source value visible to gameplay.
    private int transitionSequenceCounter = 0x04;
    private int swordMoblinAlertingSoundCounter;
    private int bombArrowCooldown;
    private int latestDroppedBombEntityIndex = -1;
    private int latestShotArrowEntityIndex = -1;
    private boolean lastArrowShotPlayedWhoosh;
    private boolean lastBombPlacementPlayedBump;
    // hLinkSpeedX/Y are written by Link before AnimateEntities. These are set
    // only around the live projectile tick so the enemy-bomb branch can mirror
    // its post-collision SLA writes without making Link state part of the
    // entity snapshot.
    private int currentLinkSpeedX;
    private int currentLinkSpeedY;
    private int chestShieldLevel = 1;
    private int chestSwordLevel = 1;
    private int chestPowerBraceletLevel = 1;
    private int pendingMusicTrack = -1;
    private boolean groundInteractionSideScrolling;
    private int entityMapId = -1;
    private int entityRoomId = -1;
    private int liftedEntitySlot = -1;
    private int liftedCarryState;
    private int liftedEffectiveDirection;
    private int liftedLinkC13B;
    private RoomEntityObjectQuery objectQuery;
    private RoomEntityObjectQuery objectIntersectionQuery;
    private final BoomerangMotion boomerangMotion = new BoomerangMotion();
    private final MagicRodFireballMotion magicRodFireballMotion =
        new MagicRodFireballMotion();
    private final SwordBeamMotion swordBeamMotion = new SwordBeamMotion();
    private final List<BoomerangObjectRequest> boomerangObjectRequests = new ArrayList<>();
    private final List<MagicRodObjectRequest> magicRodObjectRequests = new ArrayList<>();
    private final int[] magicPowderState = new int[EntityRoomLoader.MAX_ENTITIES];
    private final List<MagicPowderObjectRequest> magicPowderObjectRequests = new ArrayList<>();
    private final int[] musicalNoteInertia = new int[EntityRoomLoader.MAX_ENTITIES];
    private final int[] musicalNoteSpeedX = new int[EntityRoomLoader.MAX_ENTITIES];
    private final int[] musicalNoteSpeedY = new int[EntityRoomLoader.MAX_ENTITIES];
    private int boomerangSfxCounter;

    /** The ROM-facing state needed by Link's carry animation and throw input. */
    public record LiftedEntityState(int slot, int type, int phase,
                                   int transitionCountdown, int carryState,
                                   int sourceRomDirection, int effectiveRomDirection) {
        static LiftedEntityState none() {
            return new LiftedEntityState(-1, 0xFF, 0, 0, 0, 0, 0);
        }

        public boolean active() {
            return slot >= 0;
        }
    }

    /** A ROM transient-VFX creation requested by an entity handler this frame. */
    public record TransientVfxRequest(TransientVfxType type, int worldX, int worldY,
                                      int variant) {
        public TransientVfxRequest(TransientVfxType type, int worldX, int worldY) {
            this(type, worldX, worldY, 0);
        }
    }

    /** A ROM handler request to restore Link's pre-entity final position. */
    public record LinkFinalPositionRequest(int sourceSlot) {
        public LinkFinalPositionRequest {
            if (sourceSlot < 0 || sourceSlot >= EntityRoomLoader.MAX_ENTITIES) {
                throw new IllegalArgumentException("Link final-position source slot out of range: "
                    + sourceSlot);
            }
        }
    }

    /** A ROM handler request to block Link's next interactive motion frame. */
    public record LinkMotionBlockRequest(int sourceSlot) {
        public LinkMotionBlockRequest {
            if (sourceSlot < 0 || sourceSlot >= EntityRoomLoader.MAX_ENTITIES) {
                throw new IllegalArgumentException("Link motion-block source slot out of range: "
                    + sourceSlot);
            }
        }
    }

    /** A ROM handler request to start the source screen-shake timer. */
    public record ScreenShakeRequest(int sourceSlot, int countdown, int phase) {
        public ScreenShakeRequest {
            if (sourceSlot < 0 || sourceSlot >= EntityRoomLoader.MAX_ENTITIES) {
                throw new IllegalArgumentException("Screen-shake source slot out of range: "
                    + sourceSlot);
            }
            if (countdown < 0 || countdown > 0xFF) {
                throw new IllegalArgumentException("Screen-shake countdown must be an unsigned byte");
            }
            if (phase < 0 || phase > 0x04 || (phase & 0x01) != 0) {
                throw new IllegalArgumentException("Screen-shake phase must be 0, 2, or 4");
            }
        }
    }

    /** A boomerang request to reveal the object it intersected after movement. */
    public record BoomerangObjectRequest(int sourceSlot, int location,
                                         int objectLeft, int objectTop) {
        public BoomerangObjectRequest {
            if (sourceSlot < 0 || sourceSlot >= EntityRoomLoader.MAX_ENTITIES) {
                throw new IllegalArgumentException("Boomerang source slot out of range: "
                    + sourceSlot);
            }
            location &= 0xFF;
            objectLeft &= 0xF0;
            objectTop &= 0xF0;
        }
    }

    /** A Magic Rod fireball request to reveal the object it burned. */
    public record MagicRodObjectRequest(int sourceSlot, int location,
                                        int objectLeft, int objectTop) {
        public MagicRodObjectRequest {
            if (sourceSlot < 0 || sourceSlot >= EntityRoomLoader.MAX_ENTITIES) {
                throw new IllegalArgumentException("Magic Rod source slot out of range: "
                    + sourceSlot);
            }
            location &= 0xFF;
            objectLeft &= 0xF0;
            objectTop &= 0xF0;
        }
    }

    /** Object-side effect emitted by MagicPowderSprinkleEntityHandler. */
    public enum MagicPowderObjectAction {
        REVEAL,
        IGNITE_TORCH,
        EXTINGUISH_TORCH
    }

    /** A ROM powder-sprinkle object mutation applied by RoomSession. */
    public record MagicPowderObjectRequest(int sourceSlot, int location,
                                           int objectLeft, int objectTop,
                                           MagicPowderObjectAction action) {
        public MagicPowderObjectRequest {
            if (sourceSlot < 0 || sourceSlot >= EntityRoomLoader.MAX_ENTITIES) {
                throw new IllegalArgumentException("Magic Powder source slot out of range: "
                    + sourceSlot);
            }
            location &= 0xFF;
            objectLeft &= 0xF0;
            objectTop &= 0xF0;
            if (action == null) {
                throw new IllegalArgumentException("Magic Powder object action cannot be null");
            }
        }
    }

    /** A ROM dialog-table entry requested by an entity handler this frame. */
    public record DialogRequest(int tableId, int dialogLowId) {
        public DialogRequest {
            if (tableId < 0 || tableId > 2) {
                throw new IllegalArgumentException("Dialog table id must be 0, 1, or 2");
            }
            if ((dialogLowId & ~0xFF) != 0) {
                throw new IllegalArgumentException("Dialog id must be an unsigned byte: "
                    + dialogLowId);
            }
        }

        public int globalDialogId() {
            return (tableId << 8) | dialogLowId;
        }
    }

    /** Reward application requested by EntityInitChestWithItem. */
    public record ChestRewardEvent(int slot, int itemType) {
        public ChestRewardEvent {
            if (slot < 0 || slot >= EntityRoomLoader.MAX_ENTITIES) {
                throw new IllegalArgumentException("Chest entity slot out of range: " + slot);
            }
            if ((itemType & ~0xFF) != 0) {
                throw new IllegalArgumentException("Chest item must be an unsigned byte: "
                    + itemType);
            }
        }
    }

    /** Reward application requested by PickDroppableKey. */
    public record KeyRewardEvent(int slot, int itemType) {
        public KeyRewardEvent {
            if (slot < 0 || slot >= EntityRoomLoader.MAX_ENTITIES) {
                throw new IllegalArgumentException("Key entity slot out of range: " + slot);
            }
            if ((itemType & ~0xFF) != 0) {
                throw new IllegalArgumentException("Key item must be an unsigned byte: "
                    + itemType);
            }
        }
    }

    /** Status propagation requested by CheckForEntityFallingDownQuicksandHole. */
    public record KeyQuicksandEvent(int slot) {
        public KeyQuicksandEvent {
            if (slot < 0 || slot >= EntityRoomLoader.MAX_ENTITIES) {
                throw new IllegalArgumentException("Key entity slot out of range: " + slot);
            }
        }
    }

    /** Link capture/release side effects emitted by Like Like's handler. */
    public record LikeLikeEvent(int slot, Kind kind, int entityX, int entityY,
                                int stolenInventorySlot, int stolenShieldLevel) {
        public enum Kind { CAPTURE, RELEASE }
    }

    /** A ROM bridge handler request to replace one padded object cell and draw its columns. */
    public record HookshotBridgeUpdate(int objectLeft, int objectTop, int direction,
                                       boolean active) {
        public HookshotBridgeUpdate(int objectLeft, int objectTop, int direction) {
            this(objectLeft, objectTop, direction, true);
        }

        public HookshotBridgeUpdate {
            objectLeft &= 0xF0;
            objectTop &= 0xF0;
            if (direction < HookshotBridgeMotion.PULL_DOWN_DIRECTION
                || direction > HookshotBridgeMotion.PULL_UP_DIRECTION) {
                throw new IllegalArgumentException("Bridge direction must be 0 or 1: " + direction);
            }
        }
    }

    private RoomEntityRuntime(RoomEntitySnapshot initial, boolean indoorRoom,
                               IntSupplier defaultRandomByteSupplier,
                               EntitySpriteHandlerCatalog spriteHandlers,
                               RomEnemyCombatTables enemyCombatTables,
                               ChestContentsTable chestContentsTable) {
        this.slots = initial.slots().toArray(RoomEntity[]::new);
        this.spriteSelection = initial.spriteSelection();
        this.spriteTiles = initial.spriteTiles();
        this.groundInteractionSideScrolling = initial.sideScrolling();
        this.indoorRoom = indoorRoom;
        this.defaultRandomByteSupplier = defaultRandomByteSupplier;
        this.fallbackRomRandomByteSource = defaultRandomByteSupplier == null
            ? new RomRandomByteSource() : null;
        this.spriteHandlers = spriteHandlers;
        this.enemyCombatTables = enemyCombatTables;
        this.chestContentsTable = chestContentsTable;
        this.hookshotChainOam = initial.hookshotChainOam();
        Arrays.fill(entityOptions1Override, -1);
        Arrays.fill(droppedItemBySlot, 0);
        Arrays.fill(bombDirection, 0xFF);
        Arrays.fill(bombFinalPresentationPending, false);
        Arrays.fill(thrownDirection, 0xFF);
        for (RoomEntity entity : slots) {
            baseEntityFlipAttribute[entity.slot()] = entity.entityFlipAttribute();
            fallingVisualYOffset[entity.slot()] = initial.visualYOffset(entity.slot());
            enemyHealth[entity.slot()] = entity.loaded() ? initialHealth(entity.type()) : 0;
            enemyPhysicsFlags[entity.slot()] = entity.loaded()
                ? initialPhysicsFlags(entity.type()) : 0;
            enemyHitboxFlags[entity.slot()] = entity.loaded()
                ? initialHitboxFlags(entity.type()) : 0;
            if (entity.loaded() && entity.type() == ENTITY_CHEST_WITH_ITEM) {
                chestItemBySlot[entity.slot()] = entity.spriteVariant();
            }
            if (entity.loaded() && isBombiteType(entity.type())) {
                bombiteMotion.initialize(entity.slot());
                // The bank-$04 handlers use the global slow-transition tick
                // for TimerBombite's fuse. Keep it live for either family;
                // BouncingBombite simply leaves its value at zero.
                slowTimerInitialized[entity.slot()] = true;
            }
            if (entity.loaded() && isGhiniType(entity.type())) {
                ghiniMotion.initialize(entity.slot(), entity.type());
            }
            if (entity.loaded() && entity.type() == ENTITY_LIKE_LIKE) {
                likeLikeMotion.initialize(entity.slot());
            }
            if (entity.loaded() && entity.type() == ENTITY_POLS_VOICE) {
                polsVoiceMotion.initialize(entity.slot());
            }
            if (entity.loaded() && entity.type() == ENTITY_FISH) {
                fishMotion.initialize(entity.slot());
            }
            if (entity.loaded() && entity.type() == ENTITY_CROW) {
                crowMotion.initialize(entity.slot());
            }
            if (entity.loaded() && entity.type() == ENTITY_BOO_BUDDY) {
                booBuddyMotion.initialize(entity.slot());
            }
            if (entity.loaded() && entity.type() == ENTITY_DROPPABLE_FAIRY) {
                fairyMotion.initialize(entity.slot());
            }
            if (entity.loaded() && entity.type() == ENTITY_SPIKED_BEETLE) {
                spikedBeetleMotion.initialize(entity.slot());
            }
            if (entity.status() == EntityStatus.DYING) {
                powerRecoilDeath[entity.slot()] = entity.powerRecoilDeath();
            }
            if (isLaserType(entity.type())) {
                laserMotion.initializeForEntity(entity);
            }
            if (isFollowingNpcType(entity.type())) {
                if (entity.type() == ENTITY_BOW_WOW) {
                    bowWowMotion.initialize(entity.slot());
                } else {
                    followingNpcMotion.initialize(entity.slot(), entity.type());
                }
            }
            if (entity.status() == EntityStatus.LIFTED) {
                liftedStateInitialized[entity.slot()] = true;
                liftedSourceDirection[entity.slot()] = LiftedEntityMotion.ROM_DIRECTION_DOWN;
                liftedEffectiveDirection = LiftedEntityMotion.ROM_DIRECTION_DOWN;
                enemyTransitionCountdown[entity.slot()] = 0x02;
                liftedEntitySlot = entity.slot();
            }
            if (entity.status() == EntityStatus.THROWN) {
                thrownDirection[entity.slot()] = ThrownEntityMotion.ROM_DIRECTION_DOWN;
            }
        }
    }

    public static RoomEntityRuntime from(RoomEntitySnapshot initial) {
        return from(initial, false);
    }

    public static RoomEntityRuntime from(RoomEntitySnapshot initial, boolean indoorRoom) {
        if (initial == null) {
            throw new IllegalArgumentException("Initial entity snapshot cannot be null");
        }
        return new RoomEntityRuntime(initial, indoorRoom, null, null, null, null);
    }

    public static RoomEntityRuntime from(RoomEntitySnapshot initial, boolean indoorRoom,
                                         IntSupplier randomByteSupplier) {
        if (initial == null) {
            throw new IllegalArgumentException("Initial entity snapshot cannot be null");
        }
        return new RoomEntityRuntime(initial, indoorRoom, randomByteSupplier, null, null, null);
    }

    public static RoomEntityRuntime from(RoomEntitySnapshot initial, boolean indoorRoom,
                                         IntSupplier randomByteSupplier,
                                         EntitySpriteHandlerCatalog spriteHandlers) {
        if (initial == null) {
            throw new IllegalArgumentException("Initial entity snapshot cannot be null");
        }
        return new RoomEntityRuntime(initial, indoorRoom, randomByteSupplier, spriteHandlers,
            null, null);
    }

    public static RoomEntityRuntime from(RoomEntitySnapshot initial, boolean indoorRoom,
                                         IntSupplier randomByteSupplier,
                                         EntitySpriteHandlerCatalog spriteHandlers,
                                         RomEnemyCombatTables enemyCombatTables) {
        if (initial == null) {
            throw new IllegalArgumentException("Initial entity snapshot cannot be null");
        }
        if (enemyCombatTables == null) {
            throw new IllegalArgumentException("ROM enemy combat tables cannot be null");
        }
        return new RoomEntityRuntime(initial, indoorRoom, randomByteSupplier, spriteHandlers,
            enemyCombatTables, null);
    }

    public static RoomEntityRuntime from(RoomEntitySnapshot initial, boolean indoorRoom,
                                         IntSupplier randomByteSupplier,
                                         EntitySpriteHandlerCatalog spriteHandlers,
                                         RomEnemyCombatTables enemyCombatTables,
                                         ChestContentsTable chestContentsTable) {
        if (initial == null) {
            throw new IllegalArgumentException("Initial entity snapshot cannot be null");
        }
        if (enemyCombatTables == null) {
            throw new IllegalArgumentException("ROM enemy combat tables cannot be null");
        }
        if (chestContentsTable == null) {
            throw new IllegalArgumentException("ROM chest contents table cannot be null");
        }
        return new RoomEntityRuntime(initial, indoorRoom, randomByteSupplier, spriteHandlers,
            enemyCombatTables, chestContentsTable);
    }

    /** Advances the ROM handlers that have deterministic frame-only variants. */
    public void tick(int frameCounter) {
        IntSupplier randomByteSupplier = defaultRandomByteSupplier;
        if (randomByteSupplier == null) {
            fallbackRomRandomByteSource.beginFrame(frameCounter & 0xFF, 0);
            randomByteSupplier = fallbackRomRandomByteSource;
        }
        tickInternal(frameCounter, 0, 0, randomByteSupplier, null,
            null, 0, 0, 0, EnemyProjectileCollision.LinkState.nonInteractive(), false);
    }

    /**
     * Advances the active entity handlers with Link's ROM entity coordinates
     * (hLinkPositionX/Y), not Java's top-left sprite coordinates. The
     * random-byte supplier is deliberately injected so ROM handlers can be
     * tested independently from the host's entropy source.
     */
    public void tick(int frameCounter, int linkEntityX, int linkEntityY,
                     IntSupplier randomByteSupplier) {
        tickInternal(frameCounter, linkEntityX, linkEntityY, randomByteSupplier, null,
            null, 0, 0, 0, EnemyProjectileCollision.LinkState.nonInteractive(), false);
    }

    /** Advances handlers with the current source-shaped Link collision byte. */
    public void tick(int frameCounter, int linkEntityX, int linkEntityY,
                     int collisionType, IntSupplier randomByteSupplier) {
        tickInternal(frameCounter, linkEntityX, linkEntityY, collisionType,
            randomByteSupplier, null, null, 0, 0, 0,
            EnemyProjectileCollision.LinkState.nonInteractive(), false);
    }

    public void tick(int frameCounter, int linkEntityX, int linkEntityY,
                     IntSupplier randomByteSupplier,
                     RoomEntityBackgroundCollision backgroundCollision) {
        tickInternal(frameCounter, linkEntityX, linkEntityY, randomByteSupplier,
            backgroundCollision, null, 0, 0, 0,
            EnemyProjectileCollision.LinkState.nonInteractive(), false);
    }

    /** Background-collision compatibility path with an explicit Link byte. */
    public void tick(int frameCounter, int linkEntityX, int linkEntityY,
                     int collisionType, IntSupplier randomByteSupplier,
                     RoomEntityBackgroundCollision backgroundCollision) {
        tickInternal(frameCounter, linkEntityX, linkEntityY, collisionType,
            randomByteSupplier, backgroundCollision, null, 0, 0, 0,
            EnemyProjectileCollision.LinkState.nonInteractive(), false);
    }

    public void tick(int frameCounter, int linkEntityX, int linkEntityY,
                     IntSupplier randomByteSupplier, boolean creditsGameplay) {
        tickInternal(frameCounter, linkEntityX, linkEntityY, randomByteSupplier, null,
            null, 0, 0, 0, EnemyProjectileCollision.LinkState.nonInteractive(),
            creditsGameplay);
    }

    void tick(int frameCounter, int linkEntityX, int linkEntityY,
              IntSupplier randomByteSupplier,
              RoomEntityBackgroundCollision backgroundCollision,
              LinkPositionHistory linkPositionHistory,
              int linkZ, int linkDirection, int entityYOffset) {
        tickInternal(frameCounter, linkEntityX, linkEntityY, randomByteSupplier,
            backgroundCollision, linkPositionHistory, linkZ, linkDirection,
            entityYOffset, EnemyProjectileCollision.LinkState.nonInteractive(), false);
    }

    void tick(int frameCounter, int linkEntityX, int linkEntityY, int collisionType,
              IntSupplier randomByteSupplier,
              RoomEntityBackgroundCollision backgroundCollision,
              LinkPositionHistory linkPositionHistory,
              int linkZ, int linkDirection, int entityYOffset) {
        tickInternal(frameCounter, linkEntityX, linkEntityY, collisionType,
            randomByteSupplier, backgroundCollision, linkPositionHistory, linkZ,
            linkDirection, entityYOffset, EnemyProjectileCollision.LinkState.nonInteractive(),
            false);
    }

    List<EntityProjectileEvent> tickWithProjectileEvents(
        int frameCounter, int linkEntityX, int linkEntityY,
        IntSupplier randomByteSupplier, RoomEntityBackgroundCollision backgroundCollision,
        EnemyProjectileCollision.LinkState projectileLinkState) {
        return tickInternal(frameCounter, linkEntityX, linkEntityY, randomByteSupplier,
            backgroundCollision, null, projectileLinkState.z(), projectileLinkState.direction(),
            0, projectileLinkState, false);
    }

    List<EntityProjectileEvent> tickWithProjectileEvents(
        int frameCounter, int linkEntityX, int linkEntityY,
        IntSupplier randomByteSupplier, RoomEntityBackgroundCollision backgroundCollision,
        EnemyProjectileCollision.LinkState projectileLinkState,
        int linkSpeedX, int linkSpeedY) {
        validateByte(linkSpeedX, "Link X speed");
        validateByte(linkSpeedY, "Link Y speed");
        currentLinkSpeedX = linkSpeedX;
        currentLinkSpeedY = linkSpeedY;
        try {
            return tickWithProjectileEvents(frameCounter, linkEntityX, linkEntityY,
                randomByteSupplier, backgroundCollision, projectileLinkState);
        } finally {
            currentLinkSpeedX = 0;
            currentLinkSpeedY = 0;
        }
    }

    List<EntityProjectileEvent> tickWithProjectileEvents(
        int frameCounter, int linkEntityX, int linkEntityY,
        IntSupplier randomByteSupplier, RoomEntityBackgroundCollision backgroundCollision,
        EnemyProjectileCollision.LinkState projectileLinkState,
        boolean swordCollisionActive, int swordX, int swordWidth,
        int swordY, int swordHeight) {
        return tickInternal(frameCounter, linkEntityX, linkEntityY, randomByteSupplier,
            backgroundCollision, null, null, projectileLinkState.z(),
            projectileLinkState.direction(), 0, projectileLinkState, false,
            swordCollisionActive, swordX, swordWidth, swordY, swordHeight);
    }

    List<EntityProjectileEvent> tickWithProjectileEvents(
        int frameCounter, int linkEntityX, int linkEntityY,
        IntSupplier randomByteSupplier, RoomEntityBackgroundCollision backgroundCollision,
        RoomEntityObjectCollision objectCollision,
        EnemyProjectileCollision.LinkState projectileLinkState,
        boolean swordCollisionActive, int swordX, int swordWidth,
        int swordY, int swordHeight) {
        return tickInternal(frameCounter, linkEntityX, linkEntityY, randomByteSupplier,
            backgroundCollision, objectCollision, null, projectileLinkState.z(),
            projectileLinkState.direction(), 0, projectileLinkState, false,
            swordCollisionActive, swordX, swordWidth, swordY, swordHeight);
    }

    /** Full live-runtime path with a per-frame Link collision byte. */
    List<EntityProjectileEvent> tickWithProjectileEvents(
        int frameCounter, int linkEntityX, int linkEntityY, int collisionType,
        IntSupplier randomByteSupplier, RoomEntityBackgroundCollision backgroundCollision,
        RoomEntityObjectCollision objectCollision,
        LinkPositionHistory linkPositionHistory,
        int linkZ, int linkDirection, int entityYOffset,
        EnemyProjectileCollision.LinkState projectileLinkState,
        boolean swordCollisionActive, int swordX, int swordWidth,
        int swordY, int swordHeight) {
        return tickInternal(frameCounter, linkEntityX, linkEntityY, collisionType,
            randomByteSupplier, backgroundCollision, objectCollision, linkPositionHistory,
            linkZ, linkDirection, entityYOffset, projectileLinkState, false,
            swordCollisionActive, swordX, swordWidth, swordY, swordHeight);
    }

    /** Full live-runtime path with the Link speed bytes visible to handlers. */
    List<EntityProjectileEvent> tickWithProjectileEvents(
        int frameCounter, int linkEntityX, int linkEntityY, int collisionType,
        IntSupplier randomByteSupplier, RoomEntityBackgroundCollision backgroundCollision,
        RoomEntityObjectCollision objectCollision,
        LinkPositionHistory linkPositionHistory,
        int linkZ, int linkDirection, int entityYOffset,
        EnemyProjectileCollision.LinkState projectileLinkState,
        boolean swordCollisionActive, int swordX, int swordWidth,
        int swordY, int swordHeight, int linkSpeedX, int linkSpeedY) {
        validateByte(linkSpeedX, "Link X speed");
        validateByte(linkSpeedY, "Link Y speed");
        currentLinkSpeedX = linkSpeedX;
        currentLinkSpeedY = linkSpeedY;
        try {
            return tickWithProjectileEvents(frameCounter, linkEntityX, linkEntityY,
                collisionType, randomByteSupplier, backgroundCollision, objectCollision,
                linkPositionHistory, linkZ, linkDirection, entityYOffset,
                projectileLinkState, swordCollisionActive, swordX, swordWidth,
                swordY, swordHeight);
        } finally {
            currentLinkSpeedX = 0;
            currentLinkSpeedY = 0;
        }
    }

    List<EntityProjectileEvent> tickWithProjectileEvents(
        int frameCounter, int linkEntityX, int linkEntityY,
        IntSupplier randomByteSupplier, RoomEntityBackgroundCollision backgroundCollision,
        LinkPositionHistory linkPositionHistory,
        int linkZ, int linkDirection, int entityYOffset,
        EnemyProjectileCollision.LinkState projectileLinkState) {
        return tickInternal(frameCounter, linkEntityX, linkEntityY, randomByteSupplier,
            backgroundCollision, linkPositionHistory, linkZ, linkDirection, entityYOffset,
            projectileLinkState, false);
    }

    List<EntityProjectileEvent> tickWithProjectileEvents(
        int frameCounter, int linkEntityX, int linkEntityY,
        IntSupplier randomByteSupplier, RoomEntityBackgroundCollision backgroundCollision,
        LinkPositionHistory linkPositionHistory,
        int linkZ, int linkDirection, int entityYOffset,
        EnemyProjectileCollision.LinkState projectileLinkState,
        boolean swordCollisionActive, int swordX, int swordWidth,
        int swordY, int swordHeight) {
        return tickInternal(frameCounter, linkEntityX, linkEntityY, randomByteSupplier,
            backgroundCollision, null, linkPositionHistory, linkZ, linkDirection,
            entityYOffset, projectileLinkState, false, swordCollisionActive,
            swordX, swordWidth, swordY, swordHeight);
    }

    List<EntityProjectileEvent> tickWithProjectileEvents(
        int frameCounter, int linkEntityX, int linkEntityY,
        IntSupplier randomByteSupplier, RoomEntityBackgroundCollision backgroundCollision,
        RoomEntityObjectCollision objectCollision,
        LinkPositionHistory linkPositionHistory,
        int linkZ, int linkDirection, int entityYOffset,
        EnemyProjectileCollision.LinkState projectileLinkState,
        boolean swordCollisionActive, int swordX, int swordWidth,
        int swordY, int swordHeight) {
        return tickInternal(frameCounter, linkEntityX, linkEntityY, randomByteSupplier,
            backgroundCollision, objectCollision, linkPositionHistory, linkZ,
            linkDirection, entityYOffset, projectileLinkState, false,
            swordCollisionActive, swordX, swordWidth, swordY, swordHeight);
    }

    private List<EntityProjectileEvent> tickInternal(int frameCounter, int linkEntityX,
                      int linkEntityY, IntSupplier randomByteSupplier,
                      RoomEntityBackgroundCollision backgroundCollision,
                      LinkPositionHistory linkPositionHistory,
                      int linkZ, int linkDirection, int entityYOffset,
                      EnemyProjectileCollision.LinkState projectileLinkState,
                      boolean creditsGameplay) {
        return tickInternal(frameCounter, linkEntityX, linkEntityY, 0, randomByteSupplier,
            backgroundCollision, null, linkPositionHistory, linkZ, linkDirection, entityYOffset,
            projectileLinkState, creditsGameplay, false, 0, 0, 0, 0);
    }

    private List<EntityProjectileEvent> tickInternal(int frameCounter, int linkEntityX,
                      int linkEntityY, int collisionType, IntSupplier randomByteSupplier,
                      RoomEntityBackgroundCollision backgroundCollision,
                      LinkPositionHistory linkPositionHistory,
                      int linkZ, int linkDirection, int entityYOffset,
                      EnemyProjectileCollision.LinkState projectileLinkState,
                      boolean creditsGameplay) {
        return tickInternal(frameCounter, linkEntityX, linkEntityY, collisionType,
            randomByteSupplier, backgroundCollision, null, linkPositionHistory, linkZ,
            linkDirection, entityYOffset, projectileLinkState, creditsGameplay,
            false, 0, 0, 0, 0);
    }

    private List<EntityProjectileEvent> tickInternal(int frameCounter, int linkEntityX,
                      int linkEntityY, IntSupplier randomByteSupplier,
                      RoomEntityBackgroundCollision backgroundCollision,
                      RoomEntityObjectCollision objectCollision,
                      LinkPositionHistory linkPositionHistory,
                      int linkZ, int linkDirection, int entityYOffset,
                      EnemyProjectileCollision.LinkState projectileLinkState,
                      boolean creditsGameplay,
                      boolean swordCollisionActive, int swordX, int swordWidth,
                      int swordY, int swordHeight) {
        return tickInternal(frameCounter, linkEntityX, linkEntityY, 0, randomByteSupplier,
            backgroundCollision, objectCollision, linkPositionHistory, linkZ, linkDirection,
            entityYOffset, projectileLinkState, creditsGameplay, swordCollisionActive,
            swordX, swordWidth, swordY, swordHeight);
    }

    private List<EntityProjectileEvent> tickInternal(int frameCounter, int linkEntityX,
                      int linkEntityY, int collisionType, IntSupplier randomByteSupplier,
                      RoomEntityBackgroundCollision backgroundCollision,
                      RoomEntityObjectCollision objectCollision,
                      LinkPositionHistory linkPositionHistory,
                      int linkZ, int linkDirection, int entityYOffset,
                      EnemyProjectileCollision.LinkState projectileLinkState,
                      boolean creditsGameplay,
                      boolean swordCollisionActive, int swordX, int swordWidth,
                      int swordY, int swordHeight) {
        Objects.requireNonNull(randomByteSupplier, "randomByteSupplier");
        Objects.requireNonNull(projectileLinkState, "projectileLinkState");
        finalizePendingBombPresentations();
        int frame = frameCounter & 0xFF;
        int romCollisionType = collisionType & 0xFF;
        if (swordMoblinAlertingSoundCounter > 0) {
            swordMoblinAlertingSoundCounter--;
        }
        if (bombArrowCooldown > 0) {
            bombArrowCooldown--;
        }
        if (backgroundInteraction != null) {
            backgroundCollision = (entity, direction, nextX, nextY) ->
                backgroundInteraction.probe(entity, direction, nextX, nextY,
                    enemyIgnoreHitsCountdown[entity.slot()], frame).blocked();
        }
        projectileLaunchRequests.clear();
        Arrays.fill(enemyProjectileSpawnedThisFrame, false);
        Arrays.fill(dynamicEntitySpawnedThisFrame, false);
        transientVfxRequests.clear();
        pendingLinkFinalPositionRequests.clear();
        pendingLinkMotionBlockRequests.clear();
        pendingScreenShakeRequests.clear();
        boomerangObjectRequests.clear();
        magicRodObjectRequests.clear();
        magicPowderObjectRequests.clear();
        pendingDialogRequests.clear();
        pendingEntityEvents.clear();
        pendingChestRewardEvents.clear();
        pendingKeyQuicksandEvents.clear();
        pendingLikeLikeEvents.clear();
        pendingBombExplosionEvents.clear();
        hookshotBridgeUpdates.clear();
        pendingSwitchBlockAnimationRequest = false;
        pendingMusicTrack = -1;
        if (linkPlayingOcarinaCountdown >= 0x10 && ocarinaAnimationCounter == 0x14) {
            spawnMusicalNote(linkEntityX, linkEntityY, ocarinaAnimationPhase);
        }
        List<EntityProjectileEvent> projectileEvents = new ArrayList<>();
        if (linkPositionHistory != null) {
            followingLinkPositionHistory = linkPositionHistory;
        }
        followingLinkZ = linkZ & 0xFF;
        followingLinkDirection = linkDirection & 0xFF;
        followingEntityYOffset = entityYOffset & 0xFF;
        int romLinkDirection = romDirectionForJavaDirection(linkDirection);
        lastRomLinkDirection = romLinkDirection;
        for (int index = slots.length - 1; index >= 0; index--) {
            RoomEntity entity = slots[index];
            if (!entity.loaded()) {
                continue;
            }
            if (enemyProjectileSpawnedThisFrame[entity.slot()]
                || dynamicEntitySpawnedThisFrame[entity.slot()]) {
                continue;
            }
            RoomEntity originalEntity = entity;
            EntityStatus initialStatus = entity.status();
            boolean wasInitializing = initialStatus == EntityStatus.INIT;
            boolean ignoreHitsDecrementedBeforeHandler =
                decrementEnemyCombatCountdowns(entity.slot(), !wasInitializing);
            decrementEnemyStatusCountdowns(entity.slot());
            if (entity.sourceLoadOrder() == -1 && isDisabledFollower(entity.type())) {
                clearEntity(entity.slot());
                continue;
            }

            EntityStatus status = entity.status();
            boolean preserveGhiniPresentation = false;
            boolean preserveMadBomberPresentation = false;
            boolean preserveBombitePresentation = false;
            boolean preserveIronMaskPresentation = false;
            boolean preserveSnakePresentation = false;
            boolean preserveWizrobePresentation = false;
            boolean preserveWizrobeProjectilePresentation = false;
            boolean preservePolsVoicePresentation = false;
            boolean preserveSpikedBeetlePresentation = false;
            boolean preserveArmosKnightPresentation = false;
            boolean keyDropTransitionActive = false;
            if (status == EntityStatus.ACTIVE) {
                decrementEnemyDropCountdowns(entity);
            }
            if (status == EntityStatus.ACTIVE && entity.type() == ENTITY_HOOKSHOT_BRIDGE) {
                advanceHookshotBridgeEntity(index, entity);
                continue;
            }
            if (status == EntityStatus.ACTIVE && entity.type() == ENTITY_HOOKSHOT_CHAIN) {
                HookshotChainMotion.State hookshotState = hookshotChainMotion.state(entity.slot());
                if (hookshotState == null) {
                    clearEntity(entity.slot());
                    continue;
                }

                if ((frame & 0x03) == 0) {
                    pendingEntityEvents.add(new EntityCombatEvent(
                        entity.slot(), entity.type(), 0, false,
                        EntityCombatEvent.SoundChannel.NOISE, 0x0B));
                }

                if (hookshotState.wallCollisionPending()) {
                    HookshotChainMotion.State poked = HookshotChainMotion.completeWallPoke(
                        hookshotState);
                    hookshotChainMotion.setState(entity.slot(), poked);
                    pendingEntityEvents.add(new EntityCombatEvent(
                        entity.slot(), entity.type(), 0, false,
                        EntityCombatEvent.SoundChannel.JINGLE, 0x07));
                    transientVfxRequests.add(new TransientVfxRequest(
                        TransientVfxType.SWORD_POKE, entity.x(),
                        (entity.y() - entity.z()) & 0xFF));
                    slots[index] = entity;
                    continue;
                }

                if (hookshotState.entityState() == HookshotChainMotion.PULLING_STATE) {
                    if (HookshotChainMotion.overlapsLink(hookshotState,
                        linkEntityX, linkEntityY)) {
                        clearEntity(entity.slot());
                        continue;
                    }
                    HookshotChainMotion.PullSpeed pullSpeed =
                        HookshotChainMotion.pullLinkSpeed(
                            hookshotState, linkEntityX, linkEntityY);
                    projectileEvents.add(EntityProjectileEvent.hookshotPull(
                        entity.slot(), pullSpeed.speedX(), pullSpeed.speedY()));
                    slots[index] = entity;
                    continue;
                }

                HookshotChainMotion.Position nextPosition =
                    HookshotChainMotion.nextPosition(hookshotState);
                HookshotChainMotion.Step step = HookshotChainMotion.advance(
                    hookshotState, linkEntityX, linkEntityY, false);
                EntityBackgroundCollisionResult collisionResult = null;
                boolean blocked = false;
                if (step.state().transitionCountdown() != 0) {
                    if (backgroundInteraction != null) {
                        collisionResult = backgroundInteraction.probe(
                            entity, hookshotState.direction(), nextPosition.x(), nextPosition.y(),
                            enemyIgnoreHitsCountdown[entity.slot()], frame);
                        blocked = collisionResult.blocked();
                    } else {
                        blocked = backgroundCollision != null
                            && backgroundCollision.blocks(entity, hookshotState.direction(),
                                nextPosition.x(), nextPosition.y());
                    }
                }
                if (blocked) {
                    HookshotChainMotion.State stopped = HookshotChainMotion.rollbackTo(
                        step.state(), hookshotState.x(), hookshotState.y());
                    if (collisionResult != null
                        && collisionResult.physicsFlag() == 0x60) {
                        if (HookshotChainMotion.shouldUnloadForHookshotable(
                            step.state().transitionCountdown())) {
                            clearEntity(entity.slot());
                            continue;
                        }
                        hookshotChainMotion.setState(entity.slot(),
                            HookshotChainMotion.enterPulling(stopped));
                    } else {
                        hookshotChainMotion.setState(entity.slot(),
                            HookshotChainMotion.deferWallPoke(stopped));
                    }
                    slots[index] = entity;
                    continue;
                }

                if (indoorRoom && step.state().transitionCountdown() != 0
                    && hookshotState.speedY() != 0 && objectQuery != null) {
                    RoomEntity movedChain = withPositionAndVariant(entity,
                        step.state().x(), step.state().y(), entity.spriteVariant());
                    RoomEntityObjectSample object = objectQuery.sample(movedChain);
                    if (object != null) {
                        int bridgeObject = (hookshotState.speedY() & 0x80) != 0
                            ? OBJECT_HOOKSHOT_BRIDGE_PULL_DOWN
                            : OBJECT_HOOKSHOT_BRIDGE_PULL_UP;
                        if (object.objectId() == bridgeObject
                            && spawnHookshotBridge(object.objectLeft() + 0x08,
                                object.objectTop() + 0x10,
                                bridgeObject == OBJECT_HOOKSHOT_BRIDGE_PULL_DOWN ? 0 : 1,
                                step.state().transitionCountdown())) {
                            hookshotBridgeUpdates.add(new HookshotBridgeUpdate(
                                object.objectLeft(), object.objectTop(),
                                bridgeObject == OBJECT_HOOKSHOT_BRIDGE_PULL_DOWN ? 0 : 1));
                        }
                    }
                }

                hookshotChainMotion.setState(entity.slot(), step.state());
                if (collideHookshotWithEntities(entity, step.state(), frame)) {
                    // func_003_75A2 reaches jr_003_779A for a successful
                    // Iron Mask unmask, clearing the chain transition byte
                    // while leaving the chain entity in place.
                    hookshotChainMotion.setState(entity.slot(),
                        HookshotChainMotion.completeWallPoke(step.state()));
                }
                if (step.reachedLink()) {
                    clearEntity(entity.slot());
                    continue;
                }
                slots[index] = withPositionAndVariant(entity,
                    step.state().x(), step.state().y(), entity.spriteVariant());
                continue;
            }
            if (status == EntityStatus.ACTIVE && entity.type() == ENTITY_BOOMERANG) {
                advanceBoomerangEntity(index, entity, frame, linkEntityX, linkEntityY,
                    linkZ, projectileLinkState.motionState());
                continue;
            }
            if (status == EntityStatus.ACTIVE && entity.type() == ENTITY_SWORD_BEAM) {
                advanceSwordBeamEntity(index, entity, frame, linkEntityX, linkEntityY,
                    romLinkDirection, projectileLinkState.motionState());
                continue;
            }
            if (status == EntityStatus.ACTIVE && entity.type() == ENTITY_MAGIC_ROD_FIREBALL) {
                advanceMagicRodFireballEntity(index, entity, frame,
                    projectileLinkState.motionState());
                continue;
            }
            if (status == EntityStatus.ACTIVE
                && entity.type() == ENTITY_MAGIC_POWDER_SPRINKLE) {
                advanceMagicPowderSprinkleEntity(index, entity, frame,
                    projectileLinkState.motionState());
                continue;
            }
            if (status == EntityStatus.ACTIVE && entity.type() == ENTITY_MUSICAL_NOTE) {
                RoomEntity note = advanceMusicalNoteEntity(entity);
                if (note == null) {
                    continue;
                }
                slots[index] = note;
                continue;
            }
            if (status == EntityStatus.ACTIVE && entity.type() == ENTITY_BOMB) {
                BombMotion.Decision bombDecision = advanceBombEntity(index, entity,
                    projectileEvents, linkEntityX, linkEntityY, projectileLinkState);
                entity = slots[index];
                if (bombDecision.phase() == BombMotion.Phase.NORMAL) {
                    slots[index] = bombPrivateState4[entity.slot()] != 0
                        ? advanceEnemyBombEntity(entity, backgroundCollision)
                        : advancePlacedBombEntity(entity, backgroundCollision);
                }
                tryLiftBombIfRequested(index, bombDecision, linkEntityX, linkEntityY,
                    linkZ, romLinkDirection);
                continue;
            }
            if (status == EntityStatus.ACTIVE && entity.type() == ENTITY_ARROW) {
                if (playerArrowBombArrow[entity.slot()]
                    && playerArrowMotion.transitionCountdown(entity.slot()) != 0) {
                    int explosionSlot = spawnBombFromBombArrow(entity);
                    if (explosionSlot >= 0) {
                        pendingEntityEvents.add(new EntityCombatEvent(
                            explosionSlot, ENTITY_BOMB, 0, false,
                            EntityCombatEvent.SoundChannel.NOISE, 0x0C));
                    }
                    disableEntityWithoutPersistence(entity.slot());
                    continue;
                }
                boolean hitEntity = collidePlayerArrowWithEntities(entity, frame);
                if (hitEntity && !playerArrowBombArrow[entity.slot()]) {
                    // func_003_75A2 reaches jr_003_7737 after an ordinary
                    // arrow target hit. With no wall-collision byte set on
                    // the active arrow, the state-0 arrow unloads there.
                    disableEntityWithoutPersistence(entity.slot());
                    continue;
                }
                PlayerArrowMotion.Update arrowUpdate = playerArrowMotion.advance(
                    entity, backgroundCollision);
                if (arrowUpdate.unloaded()) {
                    disableEntityWithoutPersistence(entity.slot());
                    continue;
                }
                if (arrowUpdate.collidedWithWall()) {
                    // AlertSwordMoblins is shared by player arrows, enemy
                    // projectiles, and bomb explosions.
                    swordMoblinAlertingSoundCounter = 0x04;
                }
                slots[index] = arrowUpdate.entity();
                continue;
            }
            if (status == EntityStatus.ACTIVE && entity.type() == ENTITY_LIFTABLE_ROCK
                && liftableRockSmashActive[entity.slot()]) {
                advanceLiftableRockSmash(index, entity, frame);
                continue;
            }
            if (status == EntityStatus.LIFTED) {
                RoomEntity lifted = renderLiftedBomb(entity);
                slots[index] = advanceLiftedEntity(lifted, linkEntityX, linkEntityY, linkZ,
                    romLinkDirection);
                continue;
            } else if (status == EntityStatus.THROWN) {
                if (entity.type() == ENTITY_BOMB) {
                    BombMotion.Decision bombDecision = advanceBombEntity(index, entity,
                        projectileEvents, linkEntityX, linkEntityY, projectileLinkState);
                    entity = slots[index];
                    if (bombDecision.unloadAfterPresentation()) {
                        continue;
                    }
                }
                RoomEntity thrown = advanceThrownEntity(entity, backgroundCollision,
                    groundInteractionSideScrolling, romLinkDirection);
                slots[index] = thrown;
                continue;
            } else if (status == EntityStatus.FALLING) {
                if (entityMapId == MAP_COLOR_DUNGEON
                    && ColorShellMotion.isColorShellType(entity.type())) {
                    colorShellMotion.enterState6(entity);
                    RoomEntity activeShell = refreshColorShellDisplay(
                        withStatus(entity, EntityStatus.ACTIVE), EntityStatus.ACTIVE);
                    slots[index] = activeShell;
                    continue;
                }
                RoomEntity falling = advanceFallingEntity(entity);
                if (falling == null) {
                    disableEntityWithoutPersistence(entity.slot());
                    continue;
                }
                int renderFlipAttribute = baseEntityFlipAttribute[entity.slot()];
                if (enemyFlashCountdown[entity.slot()] > 0) {
                    renderFlipAttribute ^= (enemyFlashCountdown[entity.slot()] << 2) & 0x10;
                }
                slots[index] = new RoomEntity(
                    falling.slot(), falling.sourceLoadOrder(), falling.type(), falling.x(),
                    falling.y(), EntityStatus.FALLING, falling.spriteDefinition(),
                    falling.spriteVariant(), renderFlipAttribute, falling.spriteTileOffset(),
                    falling.z());
                continue;
            } else if (status == EntityStatus.DYING) {
                if (isBossKeyDropProducer(entity.type())) {
                    advanceBossKeyDropProducer(entity);
                    continue;
                }
                if (dyingCountdown[entity.slot()] == 0) {
                    handleTerminalEnemyDeath(entity, randomByteSupplier);
                } else {
                    RoomEntity updated = refreshColorShellDisplay(entity, status);
                    slots[index] = withDeathPresentation(updated,
                        deathSpriteVariantForCountdown(dyingCountdown[entity.slot()]),
                        powerRecoilDeath[entity.slot()]);
                }
                continue;
            } else if (status == EntityStatus.BURNING) {
                if (enemyTransitionCountdown[entity.slot()] == 0) {
                    finishBurning(entity);
                } else {
                    slots[index] = refreshColorShellDisplay(entity, status);
                }
                continue;
            } else if (status == EntityStatus.STUNNED) {
                if (entity.type() == ENTITY_BOMB) {
                    BombMotion.Decision bombDecision = advanceBombEntity(index, entity,
                        projectileEvents, linkEntityX, linkEntityY, projectileLinkState);
                    entity = slots[index];
                    if (bombDecision.unloadAfterPresentation()) {
                        continue;
                    }
                    entity = applyBombStunnedPostActiveWork(entity, backgroundCollision);
                    slots[index] = entity;
                }
                if (powerBraceletButtonHeld
                    && isLiftableEntity(entity)
                    && (entity.type() == ENTITY_BOMB
                        || (enemyPhysicsFlags[entity.slot()] & ENTITY_PHYSICS_GRABBABLE) != 0)
                    && RoomEntityPickupRules.overlapsLink(entity, linkEntityX, linkEntityY)) {
                    if (beginLift(entity.slot(), romLinkDirection)) {
                        slots[index] = advanceLiftedEntity(renderLiftedBomb(slots[index]),
                            linkEntityX, linkEntityY, linkZ, romLinkDirection);
                    }
                    continue;
                }
                if (enemyStunnedCountdown[entity.slot()] == 0) {
                    slots[index] = refreshColorShellDisplay(
                        withStatus(entity, EntityStatus.ACTIVE), EntityStatus.ACTIVE);
                } else {
                    slots[index] = refreshColorShellDisplay(entity, status);
                }
                continue;
            } else if (status == EntityStatus.INIT) {
                status = EntityStatus.ACTIVE;
                initializeEntityTimers(entity);
                if (entity.type() == ENTITY_KEESE) {
                    keeseMotion.initialize(entity.slot(), randomByteSupplier);
                }
                if (entity.type() == ENTITY_OCTOROK || entity.type() == ENTITY_MOBLIN) {
                    roamingEnemyMotion.initialize(entity.slot());
                }
                if (entity.type() == ENTITY_IRON_MASK) {
                    roamingEnemyMotion.initialize(entity.slot());
                }
                if (entity.type() == ENTITY_MOBLIN_SWORD) {
                    moblinSwordMotion.initialize(entity.slot(), entity.x());
                }
                if (entity.type() == ENTITY_TEKTITE) {
                    tektiteMotion.initialize(entity.slot(), randomByteSupplier);
                }
                if (entity.type() == ENTITY_LEEVER) {
                    leeverMotion.initialize(entity.slot());
                }
                if (entity.type() == ENTITY_ANTI_FAIRY) {
                    antiFairyMotion.initialize(entity.slot(), randomByteSupplier);
                }
                if (isSparkType(entity.type())) {
                    sparkMotion.initialize(entity.slot(), entity.type());
                }
                if (isZolGelType(entity.type())) {
                    zolGelMotion.initialize(entity.slot(), entity.type(), randomByteSupplier);
                }
                if (entity.type() == ENTITY_HIDING_ZOL) {
                    hidingZolMotion.initialize(entity.slot());
                }
                if (entity.type() == ENTITY_SPIKE_TRAP) {
                    spikeTrapMotion.initialize(entity.slot(), randomByteSupplier);
                }
                if (entity.type() == ENTITY_PAIRODD) {
                    pairoddMotion.initialize(entity.slot(), randomByteSupplier);
                }
                if (entity.type() == ENTITY_PAIRODD_PROJECTILE) {
                    pairoddProjectileMotion.initialize(entity.slot(), randomByteSupplier);
                }
                if (entity.type() == ENTITY_WATER_TEKTITE) {
                    waterTektiteMotion.initialize(entity.slot());
                }
                if (entity.type() == ENTITY_FISH) {
                    fishMotion.initialize(entity.slot());
                }
                if (entity.type() == ENTITY_CROW) {
                    crowMotion.initialize(entity.slot());
                }
                if (entity.type() == ENTITY_BOO_BUDDY) {
                    booBuddyMotion.initialize(entity.slot());
                }
                if (entity.type() == ENTITY_DROPPABLE_FAIRY) {
                    fairyMotion.initialize(entity.slot());
                }
                if (entity.type() == ENTITY_STALFOS_AGGRESSIVE) {
                    stalfosAggressiveMotion.initialize(entity.slot(), randomByteSupplier);
                }
                if (entity.type() == ENTITY_STALFOS_EVASIVE) {
                    stalfosEvasiveMotion.initialize(entity.slot());
                }
                if (entity.type() == ENTITY_GIBDO) {
                    gibdoMotion.initialize(entity.slot());
                }
                if (entity.type() == ENTITY_LIKE_LIKE) {
                    likeLikeMotion.initialize(entity.slot());
                }
                if (entity.type() == ENTITY_POLS_VOICE) {
                    polsVoiceMotion.initialize(entity.slot());
                }
                if (entity.type() == ENTITY_GOOMBA) {
                    goombaMotion.initialize(entity.slot());
                }
                if (entity.type() == ENTITY_SNAKE) {
                    snakeMotion.initialize(entity.slot());
                }
                if (entity.type() == ENTITY_WIZROBE) {
                    wizrobeMotion.initializeFromRoom(entity.slot(), entity.spriteVariant());
                    enemyTransitionCountdown[entity.slot()] = 0x80;
                    preserveWizrobePresentation = true;
                }
                if (entity.type() == ENTITY_WIZROBE_PROJECTILE) {
                    wizrobeProjectileMotion.initialize(entity.slot(), randomByteSupplier);
                    preserveWizrobeProjectilePresentation = true;
                }
                if (entity.type() == ENTITY_PEAHAT) {
                    peaHatMotion.initialize(entity.slot());
                }
                if (entity.type() == ENTITY_ARMOS_STATUE) {
                    armosMotion.initialize(entity.slot());
                }
                if (entity.type() == ENTITY_ARMOS_KNIGHT) {
                    armosKnightMotion.initialize(entity.slot());
                }
                if (isGhiniType(entity.type())) {
                    ghiniMotion.initialize(entity.slot(), entity.type());
                    if (isHidingGhiniType(entity.type())) {
                        preserveGhiniPresentation = true;
                    }
                }
                if (entity.type() == ENTITY_HARDHAT_BEETLE) {
                    hardHatMotion.initialize(entity.slot());
                }
                if (entity.type() == ENTITY_SPIKED_BEETLE) {
                    spikedBeetleMotion.initialize(entity.slot());
                }
                if (entity.type() == ENTITY_BOMBER) {
                    bomberMotion.initialize(entity.slot());
                }
                if (entity.type() == ENTITY_MAD_BOMBER) {
                    madBomberMotion.initialize(entity.slot());
                }
                if (isBombiteType(entity.type())) {
                    bombiteMotion.initialize(entity.slot());
                    slowTimerInitialized[entity.slot()] = true;
                }
                if (isFollowingNpcType(entity.type())) {
                    if (entity.type() == ENTITY_BOW_WOW) {
                        bowWowMotion.initialize(entity.slot());
                    } else {
                        followingNpcMotion.initialize(entity.slot(), entity.type());
                    }
                }
                if (ColorShellMotion.isColorShellType(entity.type())) {
                    colorShellMotion.initialize(entity.slot());
                }
            } else if (status == EntityStatus.ACTIVE) {
                decrementSlowTransitionCountdown(entity.slot(), frame);
                if (shouldDisappear(entity)) {
                    if (slowTransitionCountdown[entity.slot()] == 0) {
                        clearEntity(entity.slot());
                        continue;
                    }
                }
            }
            RoomEntity updated = entity;
            if (wasInitializing && entity.type() == ENTITY_CHEST_WITH_ITEM) {
                initializeChestEntity(entity);
                updated = withPositionAndVariant(entity, entity.x(),
                    (entity.y() - 0x08) & 0xFF, entity.spriteVariant());
            }
            if (preserveGhiniPresentation) {
                updated = withVariant(entity, -1);
            }
            if (status == EntityStatus.ACTIVE && !wasInitializing
                && entity.type() == ENTITY_CHEST_WITH_ITEM) {
                if (advanceChestEntity(index, entity)) {
                    continue;
                }
                updated = slots[index];
            }
            if (status == EntityStatus.ACTIVE && !wasInitializing
                && entity.type() == ENTITY_KEY_DROP_POINT) {
                // decrementEnemyStatusCountdowns mirrors UpdateEntityTimers'
                // shared transition-byte decrement before bank-$03's handler
                // reads it.
                int slot = entity.slot();
                if (checkForKeyDropQuicksandHole(updated)) {
                    pendingKeyQuicksandEvents.add(new KeyQuicksandEvent(slot));
                    slots[index] = withStatus(updated, EntityStatus.FALLING);
                    enemyTransitionCountdown[slot] = 0x2F;
                    fallingTargetX[slot] = 0x50;
                    fallingTargetY[slot] = 0x48;
                    fallingSpeedX[slot] = 0;
                    fallingSpeedY[slot] = 0;
                    fallingSpeedXAccumulator[slot] = 0;
                    fallingSpeedYAccumulator[slot] = 0;
                    fallingVisualYOffset[slot] = 0;
                    pendingEntityEvents.add(new EntityCombatEvent(
                        slot, ENTITY_KEY_DROP_POINT, 0, false,
                        EntityCombatEvent.SoundChannel.JINGLE, 0x18));
                    continue;
                }
                boolean hookshotRoom = entityRoomId == 0x80;
                if (hookshotRoom) {
                    updated = withDefinition(updated, spriteDefinitionFor(ENTITY_HOOKSHOT_CHAIN), 0);
                }
                KeyDropPointMotion.Update keyUpdate = KeyDropPointMotion.advance(
                    enemyTransitionCountdown[slot], updated.spriteVariant(), hookshotRoom);
                enemyTransitionCountdown[slot] = keyUpdate.nextTransitionCountdown();
                if (keyUpdate.dialogLowId() >= 0) {
                    pendingDialogRequests.add(new DialogRequest(
                        KeyDropPointMotion.DIALOG_TABLE, keyUpdate.dialogLowId()));
                }
                if (keyUpdate.rewardItemType() >= 0) {
                    pendingKeyRewardEvents.add(new KeyRewardEvent(
                        slot, keyUpdate.rewardItemType()));
                }
                keyDropTransitionActive = keyUpdate.holdAboveLink();
                if (keyDropTransitionActive) {
                    updated = withPositionAndVariant(updated, linkEntityX,
                        linkEntityY - 0x0C, updated.spriteVariant());
                    updated = withZ(updated, linkZ);
                }
            }
            if (wasInitializing && entity.type() == ENTITY_WIZROBE) {
                // EntityInitWizrobe decrements the initial display-list variant
                // before the first active handler dispatch.
                updated = withVariant(entity, -1);
            }
            if (status == EntityStatus.ACTIVE && !wasInitializing
                && entity.type() == ENTITY_POLS_VOICE
                && polsVoiceBalladOcarinaTrigger()) {
                // The handler checks this before rendering, recoil, movement,
                // or the ordinary collision path. Its private countdown $1F
                // is the shared EntityDeathHandler's visible death timer.
                dyingCountdown[entity.slot()] = 0x1F;
                enemyPhysicsFlags[entity.slot()] = 0x04;
                status = EntityStatus.DYING;
                updated = withStatus(entity, EntityStatus.DYING);
                pendingEntityEvents.add(new EntityCombatEvent(
                    entity.slot(), entity.type(), 0, false,
                    EntityCombatEvent.SoundChannel.NOISE, 0x13));
            }
            if (status == EntityStatus.ACTIVE && !wasInitializing
                && usesSharedRecoil(entity.type())) {
                // Bank-$03 AnimateRoamingEnemy and the bank-$04/$06/$07
                // handlers apply the shared recoil before their own movement.
                EnemyRecoilMotion.Update recoil = applyEnemyRecoilIfNeeded(
                    entity, backgroundCollision);
                entity = recoil.entity();
                updated = entity;
            }
            if (status == EntityStatus.ACTIVE && !wasInitializing
                && entity.type() == ENTITY_CRYSTAL_SWITCH) {
                // CrystalSwitchEntityHandler writes $FF before dispatching
                // its normal collision helper, so sword damage can never
                // turn this entity into a dying state.
                enemyHealth[entity.slot()] = 0xFF;
                if (enemyFlashCountdown[entity.slot()] > 0) {
                    // The handler consumes the collision flash as its trigger.
                    enemyFlashCountdown[entity.slot()] = 0;
                    if (!switchBlockAnimationActive) {
                        enemyTransitionCountdown[entity.slot()] = 0x18;
                        pendingSwitchBlockAnimationRequest = true;
                        pendingEntityEvents.add(new EntityCombatEvent(
                            entity.slot(), entity.type(), 0, false,
                            EntityCombatEvent.SoundChannel.WAVE, 0x0E));
                    }
                }
            }
            if (wasInitializing && entity.type() == ENTITY_LEEVER) {
                // EntityInitLeever calls SetEntitySpriteVariant($FF) before
                // the first active handler dispatch.
                updated = withVariant(entity, -1);
            }
            if (wasInitializing && isSparkType(entity.type())) {
                updated = sparkMotion.applyInitializationOffset(entity);
            }
            if (status == EntityStatus.ACTIVE && !wasInitializing
                && entity.type() == ENTITY_BUTTERFLY) {
                updated = butterflyMotion.advance(entity, frame, linkEntityX, linkEntityY,
                    randomByteSupplier);
            }
            if (status == EntityStatus.ACTIVE && !wasInitializing
                && entity.type() == ENTITY_KEESE) {
                updated = keeseMotion.advance(entity, frame, linkEntityX, linkEntityY,
                    randomByteSupplier);
            }
            if (status == EntityStatus.ACTIVE && !wasInitializing
                && entity.type() == ENTITY_BOMBER) {
                BomberMotion.Update bomberUpdate = bomberMotion.advance(
                    entity, enemyTransitionCountdown[entity.slot()], linkEntityX, linkEntityY,
                    romLinkDirection, swordCollisionActive, randomByteSupplier,
                    backgroundCollision);
                updated = bomberUpdate.entity();
                enemyTransitionCountdown[entity.slot()] = bomberUpdate.transitionCountdown();
                if (bomberUpdate.bombSpawn() != null) {
                    BomberMotion.BombSpawn spawn = bomberUpdate.bombSpawn();
                    int bombSlot = spawnEnemyBomb(spawn.x(), spawn.y(), spawn.z(),
                        spawn.transitionCountdown(), spawn.speedX(), spawn.speedY(),
                        spawn.speedZ());
                    if (bombSlot >= 0 && bomberUpdate.playJingle()) {
                        pendingEntityEvents.add(new EntityCombatEvent(
                            bombSlot, ENTITY_BOMB, 0, false,
                            EntityCombatEvent.SoundChannel.JINGLE,
                            BOMBER_THROW_JINGLE_ID));
                    }
                }
            }
            if (status == EntityStatus.ACTIVE && !wasInitializing
                && entity.type() == ENTITY_MAD_BOMBER) {
                // MadBomberEntityHandler rewrites this handler-owned drop
                // byte on every interactive pass before its state dispatch.
                droppedItemBySlot[entity.slot()] = 0x3C;
                MadBomberMotion.Update madBomberUpdate = madBomberMotion.advance(
                    entity, enemyTransitionCountdown[entity.slot()], linkEntityX, linkEntityY,
                    enemyFlashCountdown[entity.slot()], randomByteSupplier);
                updated = withVariant(madBomberUpdate.entity(),
                    madBomberUpdate.spriteVariant());
                preserveMadBomberPresentation = true;
                enemyTransitionCountdown[entity.slot()] =
                    madBomberUpdate.transitionCountdown();
                if (madBomberUpdate.bombSpawn() != null) {
                    MadBomberMotion.BombSpawn spawn = madBomberUpdate.bombSpawn();
                    int bombSlot = spawnEnemyBomb(spawn.x(), spawn.y(), spawn.z(),
                        spawn.transitionCountdown(), spawn.speedX(), spawn.speedY(),
                        spawn.speedZ());
                    if (bombSlot >= 0 && madBomberUpdate.playJingle()) {
                        pendingEntityEvents.add(new EntityCombatEvent(
                            bombSlot, ENTITY_BOMB, 0, false,
                            EntityCombatEvent.SoundChannel.JINGLE,
                            BOMBER_THROW_JINGLE_ID));
                    }
                }
            }
            if (status == EntityStatus.ACTIVE && !wasInitializing
                && isBombiteType(entity.type())) {
                BombiteMotion.Update bombiteUpdate = bombiteMotion.advance(
                    entity, frame, linkEntityX, linkEntityY,
                    enemyTransitionCountdown[entity.slot()],
                    slowTransitionCountdown[entity.slot()],
                    bombPrivateCountdown1[entity.slot()],
                    enemyIgnoreHitsCountdown[entity.slot()],
                    runningWithPegasusBoots, randomByteSupplier, backgroundCollision);
                updated = withVariant(bombiteUpdate.entity(), bombiteUpdate.spriteVariant());
                preserveBombitePresentation = true;
                enemyTransitionCountdown[entity.slot()] =
                    bombiteUpdate.transitionCountdown();
                slowTransitionCountdown[entity.slot()] =
                    bombiteUpdate.slowTransitionCountdown();
                bombPrivateCountdown1[entity.slot()] = bombiteUpdate.privateCountdown1();
                if (bombiteUpdate.bumpJingle()) {
                    pendingEntityEvents.add(new EntityCombatEvent(
                        entity.slot(), entity.type(), 0, false,
                        EntityCombatEvent.SoundChannel.JINGLE, 0x09));
                }
                boolean entityCollisionExplosion = entity.type() == ENTITY_BOUNCING_BOMBITE
                    && bombiteMotion.state(entity.slot()) == 2
                    && collideBouncingBombiteWithEntities(updated, frame);
                if (bombiteUpdate.explode() || entityCollisionExplosion) {
                    // BombiteExplode writes the already-positioned source
                    // coordinates into a fresh type-$02 enemy bomb and then
                    // lets ConfigureNewEntity supply z=0 and ignore-hits=1.
                    int bombSlot = spawnEnemyBomb(updated.x(), updated.y(), 0,
                        BOMBITE_EXPLOSION_COUNTDOWN);
                    if (bombSlot >= 0) {
                        enemyIgnoreHitsCountdown[bombSlot] = 0x01;
                        pendingEntityEvents.add(new EntityCombatEvent(
                            bombSlot, ENTITY_BOMB, 0, false,
                            EntityCombatEvent.SoundChannel.NOISE,
                            BOMBITE_EXPLOSION_SOUND_ID));
                    }
                    disableEntityWithoutPersistence(entity.slot());
                    continue;
                }
            }
            if (status == EntityStatus.ACTIVE && !wasInitializing
                && isRoamingEnemyType(entity.type())) {
                if (entity.type() == ENTITY_IRON_MASK
                    && ironMaskPrivateState2[entity.slot()] != 0) {
                    UnmaskedIronMaskMotion.Update unmaskedUpdate =
                        unmaskedIronMaskMotion.advance(entity, frame, randomByteSupplier,
                            backgroundInteraction, backgroundCollision,
                            enemyIgnoreHitsCountdown[entity.slot()], frame);
                    updated = unmaskedUpdate.entity();
                    enemyTransitionCountdown[entity.slot()] =
                        unmaskedUpdate.transitionCountdown();
                } else {
                    RoamingEnemyMotion.Update roamingUpdate = backgroundInteraction == null
                        ? roamingEnemyMotion.advance(entity, linkEntityX, linkEntityY,
                            randomByteSupplier, backgroundCollision, creditsGameplay)
                        : roamingEnemyMotion.advanceWithInteraction(entity, linkEntityX,
                            linkEntityY, randomByteSupplier, backgroundInteraction,
                            enemyIgnoreHitsCountdown[entity.slot()], frame, creditsGameplay);
                    updated = roamingUpdate.entity();
                    if (roamingUpdate.launchRequest() != null) {
                        projectileLaunchRequests.add(roamingUpdate.launchRequest());
                        spawnEnemyProjectile(entity, roamingUpdate.launchRequest());
                    }
                }
            }
            if (status == EntityStatus.ACTIVE && !wasInitializing
                && entity.type() == ENTITY_IRON_MASK) {
                int privateState2 = ironMaskPrivateState2[entity.slot()];
                int variant = privateState2 == 0
                    ? updated.spriteVariant() : ((frame >>> 4) & 0x01);
                if (spriteHandlers != null) {
                    updated = withDefinition(updated,
                        spriteHandlers.forIronMaskState(privateState2), variant);
                }
                preserveIronMaskPresentation = true;
            }
            if (status == EntityStatus.ACTIVE && !wasInitializing
                && entity.type() == ENTITY_MOBLIN_SWORD) {
                RoomEntityBackgroundInteraction swordBackgroundInteraction = backgroundInteraction;
                if (swordBackgroundInteraction == null && backgroundCollision != null) {
                    swordBackgroundInteraction = RoomEntityBackgroundInteraction.fromBoolean(
                        backgroundCollision);
                }
                MoblinSwordMotion.Update moblinSwordUpdate = moblinSwordMotion.advance(
                    entity, linkEntityX, linkEntityY,
                    swordBackgroundInteraction, enemyIgnoreHitsCountdown[entity.slot()],
                    swordMoblinAlertingSoundCounter, entityMapId,
                    transitionSequenceCounter, frame);
                updated = moblinSwordUpdate.entity();
                if (moblinSwordUpdate.dialogRequested()) {
                    pendingDialogRequests.add(new DialogRequest(1, DIALOG_MOBLIN));
                }
            }
            if (status == EntityStatus.ACTIVE && !wasInitializing
                && entity.type() == ENTITY_TEKTITE) {
                updated = tektiteMotion.advance(entity, linkEntityX, linkEntityY,
                    randomByteSupplier, backgroundCollision);
            }
            if (status == EntityStatus.ACTIVE && !wasInitializing
                && entity.type() == ENTITY_LEEVER) {
                updated = leeverMotion.advance(entity, frame, linkEntityX, linkEntityY,
                    randomByteSupplier, backgroundCollision);
            }
            if (status == EntityStatus.ACTIVE && !wasInitializing
                && entity.type() == ENTITY_ANTI_FAIRY) {
                updated = antiFairyMotion.advance(entity, frame, backgroundCollision,
                    randomByteSupplier);
            }
            if (status == EntityStatus.ACTIVE && !wasInitializing && isSparkType(entity.type())) {
                updated = sparkMotion.advance(entity, frame, backgroundCollision);
            }
            if (status == EntityStatus.ACTIVE && !wasInitializing && isZolGelType(entity.type())) {
                ZolGelMotion.Update zolGelUpdate = zolGelMotion.advance(entity, linkEntityX,
                    linkEntityY, linkZ, randomByteSupplier, backgroundCollision);
                updated = zolGelUpdate.entity();
                if (zolGelUpdate.split() != null) {
                    updated = applyZolSplit(entity, updated, zolGelUpdate.split());
                }
            }
            if (status == EntityStatus.ACTIVE && !wasInitializing
                && entity.type() == ENTITY_HIDING_ZOL) {
                updated = hidingZolMotion.advance(entity, linkEntityX, linkEntityY,
                    randomByteSupplier, backgroundCollision).entity();
            }
            if (status == EntityStatus.ACTIVE && !wasInitializing
                && entity.type() == ENTITY_SPIKE_TRAP) {
                SpikeTrapMotion.Update spikeTrapUpdate = spikeTrapMotion.advance(
                    entity, linkEntityX, linkEntityY, backgroundCollision);
                updated = spikeTrapUpdate.entity();
                if (spikeTrapUpdate.soundChannel() != EntityCombatEvent.SoundChannel.NONE) {
                    pendingEntityEvents.add(new EntityCombatEvent(
                        entity.slot(), entity.type(), 0, false,
                        spikeTrapUpdate.soundChannel(), spikeTrapUpdate.soundId()));
                }
            }
            if (status == EntityStatus.ACTIVE && !wasInitializing
                && entity.type() == ENTITY_PAIRODD) {
                PairoddMotion.Update pairoddUpdate = pairoddMotion.advance(entity, frame,
                    linkEntityX, linkEntityY, randomByteSupplier,
                    enemyFlashCountdown[entity.slot()] > 0);
                updated = pairoddUpdate.entity();
                if (pairoddUpdate.spawnProjectile()) {
                    spawnPairoddProjectile(entity, linkEntityX, linkEntityY);
                }
            }
            if (status == EntityStatus.ACTIVE && !wasInitializing
                && entity.type() == ENTITY_PAIRODD_PROJECTILE) {
                updated = pairoddProjectileMotion.advance(entity, frame);
                boolean objectCollisionHit = objectCollision != null
                    && objectCollision.collides(updated);
                boolean removePairoddProjectile = false;
                boolean swordPokeVfxQueued = false;
                var collisionEvent = EnemyProjectileCollision.check(updated,
                    pairoddProjectileMotion.direction(entity.slot()), projectileLinkState);
                if (collisionEvent.isPresent()) {
                    EntityProjectileEvent event = collisionEvent.orElseThrow();
                    projectileEvents.add(event);
                    if (event.swordPokeVfx()) {
                        transientVfxRequests.add(new TransientVfxRequest(
                            TransientVfxType.SWORD_POKE, event.swordPokeX(), event.swordPokeY()));
                        swordPokeVfxQueued = true;
                    }
                    if (event.remove()) {
                        removePairoddProjectile = true;
                    }
                }
                var swordCollisionEvent = EnemyProjectileCollision.checkSwordCollision(
                    updated, swordCollisionActive, swordX, swordWidth, swordY, swordHeight);
                if (swordCollisionEvent.isPresent()) {
                    EntityProjectileEvent event = swordCollisionEvent.orElseThrow();
                    projectileEvents.add(event);
                    if (event.swordPokeVfx() && !swordPokeVfxQueued) {
                        transientVfxRequests.add(new TransientVfxRequest(
                            TransientVfxType.SWORD_POKE, event.swordPokeX(), event.swordPokeY()));
                        swordPokeVfxQueued = true;
                    }
                    removePairoddProjectile = true;
                }
                if (objectCollisionHit) {
                    if (!swordPokeVfxQueued) {
                        transientVfxRequests.add(new TransientVfxRequest(
                            TransientVfxType.SWORD_POKE, updated.x(),
                            (updated.y() - updated.z()) & 0xFF));
                    }
                    removePairoddProjectile = true;
                }
                if (removePairoddProjectile) {
                    disableEntityWithoutPersistence(entity.slot());
                    continue;
                }
            }
            if (status == EntityStatus.ACTIVE && !wasInitializing
                && entity.type() == ENTITY_WIZROBE_PROJECTILE) {
                var collisionEvent = EnemyProjectileCollision.check(entity,
                    wizrobeProjectileMotion.direction(entity.slot()), projectileLinkState);
                if (collisionEvent.isPresent()) {
                    projectileEvents.add(collisionEvent.orElseThrow());
                }
                updated = wizrobeProjectileMotion.advance(entity);
                updated = withFlipAttribute(updated,
                    wizrobeProjectileMotion.flipAttribute(frame));
                preserveWizrobeProjectilePresentation = true;
                boolean objectCollisionHit = objectCollision != null
                    && objectCollision.collides(updated);
                if (objectCollisionHit
                    || collisionEvent.map(EntityProjectileEvent::remove).orElse(false)) {
                    disableEntityWithoutPersistence(entity.slot());
                    continue;
                }
            }
            if (status == EntityStatus.ACTIVE && !wasInitializing
                && laserMotion.isParent(entity.slot())) {
                LaserMotion.ParentUpdate laserUpdate = laserMotion.advanceParent(entity);
                updated = laserUpdate.entity();
                if (laserUpdate.spawnSensor()) {
                    spawnLaserSensor(entity);
                }
                if (laserUpdate.spawnBeam()) {
                    if (spawnLaserBeam(entity)) {
                        pendingEntityEvents.add(new EntityCombatEvent(
                            entity.slot(), entity.type(), 0, false,
                            EntityCombatEvent.SoundChannel.NOISE, 0x08));
                    }
                }
            }
            if (status == EntityStatus.ACTIVE && !wasInitializing
                && laserMotion.isSensor(entity.slot())) {
                int ownerSlot = laserMotion.parentSlot(entity.slot());
                RoomEntity owner = ownerSlot >= 0 && ownerSlot < slots.length
                    ? slots[ownerSlot] : null;
                LaserMotion.SensorUpdate sensorUpdate = laserMotion.advanceSensor(
                    entity, owner, linkEntityX, linkEntityY,
                    projectileLinkState.invincibilityCounter());
                if (sensorUpdate.triggeredParent() && ownerSlot >= 0 && ownerSlot < slots.length) {
                    // LaserLinkSensorHandler starts the parent's visible
                    // pre-fire flash at the same time as its $20 countdown.
                    enemyFlashCountdown[ownerSlot] = 0x10;
                }
                if (sensorUpdate.unloaded()) {
                    disableEntityWithoutPersistence(entity.slot());
                    continue;
                }
                updated = sensorUpdate.entity();
            }
            if (status == EntityStatus.ACTIVE && !wasInitializing
                && laserMotion.isBeam(entity.slot())) {
                var collisionEvent = EnemyProjectileCollision.check(entity,
                    laserMotion.direction(entity.slot()), projectileLinkState);
                if (collisionEvent.isPresent()) {
                    EntityProjectileEvent event = collisionEvent.orElseThrow();
                    projectileEvents.add(event);
                    if (event.remove()) {
                        disableEntityWithoutPersistence(entity.slot());
                        continue;
                    }
                    // Bank-$15 moves the beam before consuming the collision
                    // byte.  A reflected beam therefore advances once in
                    // this frame, then reverses the selected speed axis.
                    LaserMotion.BeamUpdate beamUpdate = laserMotion.advanceBeam(
                        entity, backgroundCollision);
                    if (beamUpdate.unloaded()) {
                        disableEntityWithoutPersistence(entity.slot());
                        continue;
                    }
                    updated = beamUpdate.entity();
                    laserMotion.reflectBeam(entity.slot(), projectileLinkState.direction());
                    if (event.swordPokeVfx()) {
                        transientVfxRequests.add(new TransientVfxRequest(
                            TransientVfxType.SWORD_POKE, updated.x(), updated.y()));
                    }
                } else {
                    LaserMotion.BeamUpdate beamUpdate = laserMotion.advanceBeam(
                        entity, backgroundCollision);
                    if (beamUpdate.unloaded()) {
                        disableEntityWithoutPersistence(entity.slot());
                        continue;
                    }
                    updated = beamUpdate.entity();
                    transientVfxRequests.add(new TransientVfxRequest(
                        TransientVfxType.LASER_BEAM, updated.x() + 0x04, updated.y()));
                }
            }
            if (status == EntityStatus.ACTIVE && !wasInitializing
                && isEnemyProjectileType(entity.type())) {
                EnemyProjectileMotion.Update projectileUpdate;
                boolean hasLinkCollision = enemyProjectileMotion.transitionCountdown(entity.slot()) == 0;
                if (hasLinkCollision) {
                    var collisionEvent = EnemyProjectileCollision.check(entity,
                        enemyProjectileMotion.direction(entity.slot()), projectileLinkState);
                    if (collisionEvent.isPresent()) {
                        EntityProjectileEvent event = collisionEvent.orElseThrow();
                        projectileEvents.add(event);
                        if (event.remove()) {
                            disableEntityWithoutPersistence(entity.slot());
                            continue;
                        }
                        projectileUpdate = enemyProjectileMotion.advance(entity, backgroundCollision,
                            event.startsWallTransition());
                    } else {
                        projectileUpdate = enemyProjectileMotion.advance(entity, backgroundCollision);
                    }
                } else {
                    projectileUpdate = enemyProjectileMotion.advance(entity, backgroundCollision);
                }
                if (projectileUpdate.unloaded()) {
                    disableEntityWithoutPersistence(entity.slot());
                    continue;
                }
                if (projectileUpdate.collidedWithWall()) {
                    swordMoblinAlertingSoundCounter = 0x04;
                }
                updated = projectileUpdate.entity();
            }
            if (status == EntityStatus.ACTIVE && !wasInitializing
                && entity.type() == ENTITY_WATER_TEKTITE) {
                updated = waterTektiteMotion.advance(entity, frame, randomByteSupplier,
                    backgroundCollision);
            }
            if (status == EntityStatus.ACTIVE && !wasInitializing
                && entity.type() == ENTITY_FISH) {
                FishMotion.Update fishUpdate = fishMotion.advance(entity,
                    enemyTransitionCountdown[entity.slot()], randomByteSupplier,
                    backgroundCollision);
                updated = fishUpdate.entity();
                enemyTransitionCountdown[entity.slot()] = fishUpdate.transitionCountdown();
                enemyPhysicsFlags[entity.slot()] = fishUpdate.physicsFlags();
                if (fishUpdate.waterSplash()) {
                    transientVfxRequests.add(new TransientVfxRequest(
                        TransientVfxType.WATER_SPLASH, updated.x(), updated.y()));
                    pendingEntityEvents.add(new EntityCombatEvent(
                        updated.slot(), updated.type(), 0, false,
                        EntityCombatEvent.SoundChannel.JINGLE, 0x0E));
                }
            }
            if (status == EntityStatus.ACTIVE && !wasInitializing
                && entity.type() == ENTITY_CROW) {
                CrowMotion.Update crowUpdate = crowMotion.advance(entity,
                    enemyTransitionCountdown[entity.slot()], frame, linkEntityX, linkEntityY);
                updated = crowUpdate.entity();
                enemyTransitionCountdown[entity.slot()] = crowUpdate.transitionCountdown();
                enemyPhysicsFlags[entity.slot()] = crowUpdate.physicsFlags();
                if (crowUpdate.boomerangSound()) {
                    pendingEntityEvents.add(new EntityCombatEvent(
                        updated.slot(), updated.type(), 0, false,
                        EntityCombatEvent.SoundChannel.NOISE, BOOMERANG_SFX_ID));
                }
                if (crowUpdate.unloaded()) {
                    disableEntityWithoutPersistence(entity.slot());
                    continue;
                }
            }
            if (status == EntityStatus.ACTIVE && !wasInitializing
                && entity.type() == ENTITY_BOO_BUDDY) {
                BooBuddyMotion.Update booBuddyUpdate = booBuddyMotion.advance(entity, frame,
                    linkEntityX, linkEntityY, enemyTransitionCountdown[entity.slot()],
                    booBuddyTriggerCount, swordCollisionActive, randomByteSupplier);
                updated = booBuddyUpdate.entity();
                enemyTransitionCountdown[entity.slot()] = booBuddyUpdate.transitionCountdown();
                if (booBuddyUpdate.healthOverride() >= 0) {
                    enemyHealth[entity.slot()] = booBuddyUpdate.healthOverride();
                }
                if (booBuddyUpdate.unloaded()) {
                    disableEntityWithoutPersistence(entity.slot());
                    continue;
                }
            }
            if (status == EntityStatus.ACTIVE && !wasInitializing
                && entity.type() == ENTITY_DROPPABLE_FAIRY) {
                FairyMotion.Update fairyUpdate = fairyMotion.advance(entity, frame,
                    linkEntityX, linkEntityY, randomByteSupplier,
                    enemyTransitionCountdown[entity.slot()]);
                updated = fairyUpdate.entity();
                enemyTransitionCountdown[entity.slot()] = fairyUpdate.transitionCountdown();
            }
            if (status == EntityStatus.ACTIVE && !wasInitializing
                && entity.type() == ENTITY_STALFOS_AGGRESSIVE) {
                updated = stalfosAggressiveMotion.advance(entity, frame, linkEntityX, linkEntityY,
                    randomByteSupplier, backgroundCollision);
            }
            if (status == EntityStatus.ACTIVE && !wasInitializing
                && entity.type() == ENTITY_STALFOS_EVASIVE) {
                StalfosEvasiveMotion.Update evasiveUpdate = stalfosEvasiveMotion.advance(
                    entity, frame, linkEntityX, linkEntityY, actionButtonsHeld,
                    enemyTransitionCountdown[entity.slot()],
                    enemyIgnoreHitsCountdown[entity.slot()], randomByteSupplier,
                    backgroundCollision, entityMapId);
                updated = evasiveUpdate.entity();
                enemyTransitionCountdown[entity.slot()] = evasiveUpdate.transitionCountdown();
                if (evasiveUpdate.cloneRequest() != null) {
                    spawnEvasiveClone(entity, evasiveUpdate.cloneRequest());
                }
                if (evasiveUpdate.swordPokeRequested()) {
                    pendingEntityEvents.add(new EntityCombatEvent(
                        entity.slot(), entity.type(), 0, false,
                        EntityCombatEvent.SoundChannel.JINGLE, 0x07));
                    transientVfxRequests.add(new TransientVfxRequest(
                        TransientVfxType.SWORD_POKE, updated.x(),
                        (updated.y() - updated.z()) & 0xFF));
                }
                if (evasiveUpdate.unloadRequested()) {
                    disableEntityWithoutPersistence(entity.slot());
                    continue;
                }
                if (spriteHandlers != null) {
                    EntitySpriteDefinition evasiveDefinition =
                        spriteHandlers.forStalfosEvasiveState(
                            stalfosEvasiveMotion.privateState1(entity.slot()));
                    updated = withDefinition(updated, evasiveDefinition, updated.spriteVariant());
                }
            }
            if (status == EntityStatus.ACTIVE && !wasInitializing
                && entity.type() == ENTITY_GIBDO) {
                updated = gibdoMotion.advance(entity, randomByteSupplier, backgroundCollision);
            }
            if (status == EntityStatus.ACTIVE && !wasInitializing
                && entity.type() == ENTITY_LIKE_LIKE) {
                LikeLikeMotion.Update likeLikeUpdate = likeLikeMotion.advance(
                    entity, frame, linkEntityX, linkEntityY, linkZ,
                    projectileLinkState.motionState(), actionButtonsHeld,
                    linkItemA, linkItemB, projectileLinkState.shieldLevel(),
                    slowTransitionCountdown[entity.slot()], randomByteSupplier,
                    backgroundCollision);
                updated = likeLikeUpdate.entity();
                slowTransitionCountdown[entity.slot()] = likeLikeUpdate.slowTransitionCountdown();
                if (likeLikeUpdate.event() != null) {
                    pendingLikeLikeEvents.add(likeLikeUpdate.event());
                    if (likeLikeUpdate.event().kind()
                        == LikeLikeEvent.Kind.RELEASE) {
                        slowTimerInitialized[entity.slot()] = true;
                    }
                }
                if (likeLikeUpdate.spriteVariant() >= 0) {
                    updated = withVariant(updated, likeLikeUpdate.spriteVariant());
                }
            }
            if (status == EntityStatus.ACTIVE && !wasInitializing
                && entity.type() == ENTITY_GOOMBA) {
                GoombaMotion.Update goombaUpdate = goombaMotion.advance(
                    entity, frame, linkEntityX, linkEntityY, randomByteSupplier,
                    backgroundCollision, enemyTransitionCountdown[entity.slot()],
                    groundInteractionSideScrolling);
                updated = goombaUpdate.entity();
                enemyTransitionCountdown[entity.slot()] = goombaUpdate.transitionCountdown();
                if (goombaUpdate.startDying()) {
                    // func_007_666B writes ENTITY_DROPPABLE_HEART and the
                    // private-countdown-3 death window before setting status
                    // DYING and physics flags to 4. Keep both the source
                    // table and the runtime death countdown synchronized;
                    // the latter drives the shared Java death presentation.
                    droppedItemBySlot[entity.slot()] = 0x2D;
                    dropPrivateCountdown3[entity.slot()] = 0x0C;
                    dyingCountdown[entity.slot()] = 0x0C;
                    enemyPhysicsFlags[entity.slot()] = 0x04;
                    status = EntityStatus.DYING;
                }
            }
            if (status == EntityStatus.ACTIVE && !wasInitializing
                && entity.type() == ENTITY_SNAKE) {
                SnakeMotion.Update snakeUpdate = snakeMotion.advance(
                    entity, frame, linkEntityX, linkEntityY, randomByteSupplier,
                    backgroundCollision, enemyTransitionCountdown[entity.slot()]);
                updated = snakeUpdate.entity();
                enemyTransitionCountdown[entity.slot()] = snakeUpdate.transitionCountdown();
                preserveSnakePresentation = true;
            }
            if (status == EntityStatus.ACTIVE && !wasInitializing
                && entity.type() == ENTITY_WIZROBE) {
                WizrobeMotion.Update wizrobeUpdate = wizrobeMotion.advance(
                    entity, linkEntityX, linkEntityY, randomByteSupplier,
                    enemyTransitionCountdown[entity.slot()],
                    enemyPhysicsFlags[entity.slot()]);
                updated = wizrobeUpdate.entity();
                enemyTransitionCountdown[entity.slot()] = wizrobeUpdate.transitionCountdown();
                enemyPhysicsFlags[entity.slot()] = wizrobeUpdate.physicsFlags();
                preserveWizrobePresentation = true;
                if (wizrobeUpdate.projectileSpawn() != null) {
                    spawnWizrobeProjectile(updated, wizrobeUpdate.projectileSpawn());
                }
            }
            if (status == EntityStatus.ACTIVE && !wasInitializing
                && entity.type() == ENTITY_PEAHAT) {
                updated = peaHatMotion.advance(entity, frame, randomByteSupplier,
                    backgroundCollision);
            }
            if (status == EntityStatus.ACTIVE && !wasInitializing
                && entity.type() == ENTITY_ARMOS_STATUE) {
                ArmosMotion.Update armosUpdate = armosMotion.advance(
                    entity, frame, linkEntityX, linkEntityY, randomByteSupplier);
                updated = armosUpdate.entity();
                if (armosUpdate.woke()) {
                    // ArmosStatueEntityHandler's wake branch starts the same
                    // $18 visible flash used by the ROM's enemy hit handlers.
                    enemyFlashCountdown[entity.slot()] = 0x18;
                }
                if (armosUpdate.activated()) {
                    // State 1 clears the harmless physics bit and enables the
                    // normal hitbox/sword path for state 2.
                    enemyPhysicsFlags[entity.slot()] &= 0x7F;
                    enemyIgnoreHitsCountdown[entity.slot()] = 0;
                }
            }
            if (status == EntityStatus.ACTIVE && !wasInitializing
                && entity.type() == ENTITY_ARMOS_KNIGHT) {
                BossIntroMotion.Update bossIntro = bossIntroMotion.advance(
                    options1(entity.slot()), entity.type(), entityMapId,
                    transitionSequenceCounter);
                if (bossIntro.musicTrack() >= 0) {
                    pendingMusicTrack = bossIntro.musicTrack();
                }
                if (bossIntro.dialogLowId() >= 0) {
                    pendingDialogRequests.add(new DialogRequest(
                        bossIntro.dialogTableId(), bossIntro.dialogLowId()));
                }
                requestArmosLinkPush(entity, linkEntityX, linkEntityY,
                    projectileLinkState.motionState());
                ArmosKnightMotion.Update armosKnightUpdate = armosKnightMotion.advance(
                    entity, linkEntityX, linkEntityY, linkZ,
                    enemyTransitionCountdown[entity.slot()],
                    enemyPhysicsFlags[entity.slot()], enemyHitboxFlags[entity.slot()],
                    options1(entity.slot()), enemyHealth[entity.slot()]);
                updated = armosKnightUpdate.entity();
                enemyTransitionCountdown[entity.slot()] =
                    armosKnightUpdate.transitionCountdown();
                enemyPhysicsFlags[entity.slot()] = armosKnightUpdate.physicsFlags();
                enemyHitboxFlags[entity.slot()] = armosKnightUpdate.hitboxFlags();
                entityOptions1Override[entity.slot()] = armosKnightUpdate.options1();
                preserveArmosKnightPresentation = true;
                if (armosKnightUpdate.linkMotionBlocked()) {
                    pendingLinkMotionBlockRequests.add(
                        new LinkMotionBlockRequest(entity.slot()));
                }
                if (armosKnightUpdate.screenShakeRequest() != null) {
                    ArmosKnightMotion.ScreenShakeRequest shake =
                        armosKnightUpdate.screenShakeRequest();
                    pendingScreenShakeRequests.add(new ScreenShakeRequest(
                        entity.slot(), shake.countdown(), shake.phase()));
                }
                if (armosKnightUpdate.rubbleRequest() != null) {
                    ArmosKnightMotion.RubbleRequest rubble = armosKnightUpdate.rubbleRequest();
                    int rubbleSlot = spawnLiftableRockRubble(rubble.x(), rubble.y());
                    if (rubbleSlot >= 0) {
                        transientVfxRequests.add(new TransientVfxRequest(
                            TransientVfxType.POOF, rubble.x(), rubble.y()));
                        pendingEntityEvents.add(new EntityCombatEvent(
                            rubbleSlot, ENTITY_LIFTABLE_ROCK, 0, false,
                            EntityCombatEvent.SoundChannel.NOISE, 0x29));
                    }
                }
                if (armosKnightUpdate.jingleId() >= 0) {
                    pendingEntityEvents.add(new EntityCombatEvent(
                        entity.slot(), entity.type(), 0, false,
                        EntityCombatEvent.SoundChannel.JINGLE,
                        armosKnightUpdate.jingleId()));
                }
            }
            if (status == EntityStatus.ACTIVE && !wasInitializing
                && isGhiniType(entity.type())) {
                updated = ghiniMotion.advance(entity, frame, entity.type(),
                    linkEntityX, linkEntityY, romCollisionType, randomByteSupplier);
                preserveGhiniPresentation = true;
            }
            if (status == EntityStatus.ACTIVE && !wasInitializing
                && entity.type() == ENTITY_HARDHAT_BEETLE) {
                updated = hardHatMotion.advance(entity, frame, linkEntityX, linkEntityY,
                    randomByteSupplier, backgroundCollision);
            }
            if (status == EntityStatus.ACTIVE && !wasInitializing
                && entity.type() == ENTITY_POLS_VOICE) {
                // PolsVoiceEntityHandler deliberately writes the temporary
                // ignore-hits byte after moving, so the background helper sees
                // $01, then clears it with B (zero) before the default enemy
                // collision pass. The shared Java movement probe is the
                // handler's background-interaction seam, so bracket it here.
                enemyIgnoreHitsCountdown[entity.slot()] = 0x01;
                try {
                    PolsVoiceMotion.Update polsVoiceUpdate = polsVoiceMotion.advance(
                        entity, linkEntityX, linkEntityY, randomByteSupplier,
                        backgroundCollision, enemyTransitionCountdown[entity.slot()]);
                    updated = polsVoiceUpdate.entity();
                    enemyTransitionCountdown[entity.slot()] =
                        polsVoiceUpdate.transitionCountdown();
                } finally {
                    enemyIgnoreHitsCountdown[entity.slot()] = 0;
                }
                preservePolsVoicePresentation = true;
            }
            if (status == EntityStatus.ACTIVE && !wasInitializing
                && entity.type() == ENTITY_SPIKED_BEETLE) {
                SpikedBeetleMotion.Update spikedBeetleUpdate = spikedBeetleMotion.advance(
                    entity, frame, linkEntityX, linkEntityY, randomByteSupplier,
                    backgroundCollision, enemyTransitionCountdown[entity.slot()],
                    enemyIgnoreHitsCountdown[entity.slot()]);
                updated = spikedBeetleUpdate.entity();
                enemyTransitionCountdown[entity.slot()] =
                    spikedBeetleUpdate.transitionCountdown();
                entityOptions1Override[entity.slot()] = spikedBeetleUpdate.options1();
                enemyHitboxFlags[entity.slot()] = spikedBeetleUpdate.hitboxFlags();
                preserveSpikedBeetlePresentation = true;
            }
            if (status == EntityStatus.ACTIVE && !wasInitializing
                && isDynamicFollowingNpc(entity)) {
                if (entity.type() == ENTITY_BOW_WOW) {
                    updated = bowWowMotion.advance(entity, frame, linkEntityX, linkEntityY,
                        followingLinkZ, randomByteSupplier, backgroundCollision);
                } else {
                    updated = followingNpcMotion.advance(entity, frame, linkEntityX, linkEntityY,
                        followingLinkZ, followingLinkDirection, followingEntityYOffset,
                        followingLinkPositionHistory, backgroundCollision);
                }
            }
            if (status == EntityStatus.ACTIVE && !wasInitializing
                && ColorShellMotion.isColorShellType(entity.type())) {
                ColorShellMotion.Update shellUpdate = colorShellMotion.advance(
                    entity, frame, linkEntityX, linkEntityY, randomByteSupplier,
                    backgroundCollision, enemyIgnoreHitsCountdown[entity.slot()],
                    colorShellWorld, Arrays.asList(slots));
                updated = shellUpdate.entity();
                enemyIgnoreHitsCountdown[entity.slot()] = shellUpdate.nextIgnoreHitsCountdown();
                if (shellUpdate.unloadRequested()) {
                    pendingClearedEntityMask |= persistentClearMask(entity);
                    disableEntityWithoutPersistence(entity.slot());
                    continue;
                }
            }
            if (status == EntityStatus.ACTIVE && !wasInitializing
                && !keyDropTransitionActive && enemyDropActive[updated.slot()]) {
                updated = enemyDropMotion.advance(updated, frame,
                    entityGroundStatus[updated.slot()], groundInteractionSideScrolling);
            }
            if (status == EntityStatus.ACTIVE && !wasInitializing
                && !keyDropTransitionActive && !hasNoGroundInteraction(updated)) {
                // ApplyEntityInteractionWithBackground runs after each ROM
                // entity handler's movement and before the final display-list
                // presentation. The room session supplies terrain physics;
                // direct runtime users retain the no-op default.
                int previousGroundStatus = entityGroundStatus[updated.slot()];
                RoomEntityGroundInteraction.Result groundResult = Objects.requireNonNull(
                    groundInteraction.apply(updated, frame, previousGroundStatus,
                        verticalSpeedZ(updated), groundInteractionSideScrolling),
                    "Room entity ground interaction returned null");
                updated = Objects.requireNonNull(groundResult.entity(),
                    "Room entity ground interaction returned a null entity");
                entityGroundStatus[updated.slot()] = groundResult.groundStatus() & 0xFF;
                if (groundResult.pitTransition() != null
                    && beginFalling(updated, groundResult.pitTransition(),
                        ignoreHitsDecrementedBeforeHandler)) {
                    status = EntityStatus.FALLING;
                }
                if (groundResult.waterSplash()) {
                    transientVfxRequests.add(new TransientVfxRequest(
                        TransientVfxType.WATER_SPLASH, updated.x(), updated.y()));
                    pendingEntityEvents.add(new EntityCombatEvent(
                        updated.slot(), updated.type(), 0, false,
                        EntityCombatEvent.SoundChannel.JINGLE, 0x0E));
                }
                if (groundResult.unloaded()) {
                    disableEntityWithoutPersistence(updated.slot());
                    continue;
                }
                if (status == EntityStatus.ACTIVE && enemyDropActive[updated.slot()]) {
                    boolean dropGroundCollision = groundInteractionSideScrolling
                        && enemyDropMotion.speedY(updated.slot()) != 0
                        && (enemyDropMotion.speedY(updated.slot()) & 0x80) == 0
                        && backgroundCollision != null
                        && backgroundCollision.blocks(updated,
                            EntityBackgroundCollisionResult.DOWN,
                            updated.x(), updated.y());
                    updated = enemyDropMotion.bounce(updated, groundResult.groundStatus(),
                        groundInteractionSideScrolling, dropGroundCollision);
                }
            }
            if (ColorShellMotion.isColorShellType(updated.type())) {
                updated = refreshColorShellDisplay(updated, status);
            }
            if (status == EntityStatus.ACTIVE && isRuntimeFloatingItem(updated)) {
                updated = withZ(updated,
                    FloatingItemMotion.zForFrame(groundInteractionSideScrolling, frame));
            }
            int variant = preserveGhiniPresentation || preserveMadBomberPresentation
                || preserveBombitePresentation
                || preserveIronMaskPresentation || preserveSnakePresentation
                || preserveWizrobePresentation || preserveWizrobeProjectilePresentation
                || preservePolsVoicePresentation
                || preserveSpikedBeetlePresentation || preserveArmosKnightPresentation
                ? updated.spriteVariant() : variantFor(updated, frame);
            if (status == EntityStatus.ACTIVE && shouldDisappear(entity)) {
                variant = (slowTransitionCountdown[entity.slot()] & 0x01) != 0 ? 0 : -1;
            }
            int renderFlipAttribute = preserveGhiniPresentation || preserveMadBomberPresentation
                || preserveBombitePresentation
                || preserveIronMaskPresentation || preserveSnakePresentation
                || preserveWizrobePresentation || preserveWizrobeProjectilePresentation
                || preservePolsVoicePresentation
                || preserveSpikedBeetlePresentation || preserveArmosKnightPresentation
                ? updated.entityFlipAttribute() : baseEntityFlipAttribute[entity.slot()];
            if (preserveBombitePresentation && updated.type() == ENTITY_TIMER_BOMBITE) {
                renderFlipAttribute |= (bombPrivateCountdown1[updated.slot()] << 3) & 0x10;
            }
            if (enemyFlashCountdown[entity.slot()] > 0) {
                renderFlipAttribute ^= (enemyFlashCountdown[entity.slot()] << 2) & 0x10;
            }
            if (status != entity.status() || variant != entity.spriteVariant()
                || updated.type() != entity.type()
                || updated.spriteDefinition() != entity.spriteDefinition()
                || updated.x() != originalEntity.x() || updated.y() != originalEntity.y()
                || renderFlipAttribute != originalEntity.entityFlipAttribute()
                || updated.z() != originalEntity.z()) {
                slots[index] = new RoomEntity(
                    updated.slot(), updated.sourceLoadOrder(), updated.type(), updated.x(), updated.y(),
                    status, updated.spriteDefinition(), variant, renderFlipAttribute,
                    updated.spriteTileOffset(), updated.z());
            }
        }
        updateHookshotChainOam(linkEntityX, linkEntityY, frame);
        return List.copyOf(projectileEvents);
    }

    List<RoamingEnemyMotion.LaunchRequest> projectileLaunchRequests() {
        return List.copyOf(projectileLaunchRequests);
    }

    /**
     * Runs the room-entity portion of PickableCollectIfNeeded. A non-null
     * event means the entity completed its pickup collision and its first-eight
     * persistence bit is ready for the room session to record.
     */
    public EntityPickupEvent collectIfNeeded(int frameCounter,
                                             int linkPixelX,
                                             int linkPixelY,
                                             boolean linkAirborne,
                                             boolean linkInteractive) {
        return collectIfNeeded(frameCounter, linkPixelX, linkPixelY, linkAirborne,
            linkInteractive, 0, 0);
    }

    /** Pickup collision with Link's Java direction available for held items. */
    public EntityPickupEvent collectIfNeeded(int frameCounter,
                                             int linkPixelX,
                                             int linkPixelY,
                                             boolean linkAirborne,
                                             boolean linkInteractive,
                                             int linkDirection) {
        return collectIfNeeded(frameCounter, linkPixelX, linkPixelY, linkAirborne,
            linkInteractive, linkDirection, 0);
    }

    /** Pickup collision with Link's ROM-facing Z available for floating items. */
    public EntityPickupEvent collectIfNeeded(int frameCounter,
                                             int linkPixelX,
                                             int linkPixelY,
                                             boolean linkAirborne,
                                             boolean linkInteractive,
                                             int linkDirection,
                                             int linkZ) {
        if (!linkInteractive) {
            return null;
        }
        int romDirection = romDirectionForJavaDirection(linkDirection);

        for (int index = slots.length - 1; index >= 0; index--) {
            RoomEntity entity = slots[index];
            boolean floatingType = FloatingItemMotion.isFloatingItem(entity.type());
            boolean floating = isRuntimeFloatingItem(entity);
            if (!entity.loaded() || entity.status() != EntityStatus.ACTIVE
                || !RoomEntityPickupRules.isPickable(entity.type())
                || (floatingType && !floating)
                || dropPrivateCountdown1[entity.slot()] > 0
                || (!floating && !RoomEntityPickupRules.collisionCadenceMatches(
                    frameCounter, entity.slot()))
                || (!floating && linkAirborne)
                || (floating && !FloatingItemMotion.linkZAllowsCollection(
                    groundInteractionSideScrolling, linkZ))
                || !(floating
                    ? RoomEntityPickupRules.overlapsFloatingItem(entity, linkPixelX, linkPixelY)
                    : RoomEntityPickupRules.overlapsLink(entity, linkPixelX, linkPixelY))) {
                continue;
            }

            int persistentClearMask = persistentClearMask(entity);
            if (entity.type() == ENTITY_KEY_DROP_POINT) {
                if (entityRoomId == 0x80 || entity.spriteVariant() != 0) {
                    enemyTransitionCountdown[entity.slot()] =
                        KeyDropPointMotion.TRANSITION_COUNTDOWN;
                } else {
                    pendingKeyRewardEvents.add(new KeyRewardEvent(
                        entity.slot(), ChestContentsTable.CHEST_SMALL_KEY));
                    clearEntity(entity.slot());
                }
            } else if (requiresHeldPickupTransition(entity.type())) {
                beginLift(entity.slot(), romDirection);
            } else {
                clearEntity(entity.slot());
            }
            return new EntityPickupEvent(entity.slot(), entity.type(), persistentClearMask,
                floating ? entity.spriteVariant() : -1);
        }
        return null;
    }

    /**
     * Starts EntityGetLiftedUp's shared state transition. Directions are the
     * ROM order (right, left, up, down); RoomSession performs the Java-to-ROM
     * conversion at its boundary.
     */
    boolean beginLift(int slot, int romDirection) {
        validateEntitySlot(slot);
        validateRomDirection(romDirection);
        RoomEntity entity = slots[slot];
        if (!entity.loaded()) {
            return false;
        }
        if (entity.type() == ENTITY_BOMB && bombFinalPresentationPending[slot]) {
            return false;
        }
        if (liftedEntitySlot >= 0 && liftedEntitySlot != slot
            && slots[liftedEntitySlot].status() == EntityStatus.LIFTED) {
            return false;
        }

        liftedEntitySlot = slot;
        liftedPhase[slot] = 0;
        liftedSourceDirection[slot] = romDirection;
        liftedStateInitialized[slot] = true;
        liftedCarryState = 0;
        liftedEffectiveDirection = romDirection;
        enemyTransitionCountdown[slot] = 0x02;
        if (entity.type() == ENTITY_BOMB) {
            if (placedBombMotionInitialized[slot]) {
                thrownEntityMotion.clear(slot);
                placedBombMotionInitialized[slot] = false;
            }
            bombFinalPresentationPending[slot] = false;
            enemyFlashCountdown[slot] = 0;
            entity = withDefinition(entity, spriteDefinitionFor(ENTITY_BOMB), 0);
        }
        slots[slot] = withStatus(entity, EntityStatus.LIFTED);
        return true;
    }

    /** Hands the fully held entity to func_014_53A3's generic throw path. */
    boolean throwLiftedEntity(int romDirection) {
        validateRomDirection(romDirection);
        LiftedEntityState state = liftedEntityState();
        if (!state.active() || state.carryState() != 0x01) {
            return false;
        }

        int slot = state.slot();
        RoomEntity entity = slots[slot];
        thrownDirection[slot] = romDirection;
        thrownEntityMotion.start(slot, romDirection, entity.type(),
            groundInteractionSideScrolling);
        thrownMotionInitialized[slot] = true;
        liftedStateInitialized[slot] = false;
        liftedPhase[slot] = 0;
        liftedCarryState = 0;
        liftedEffectiveDirection = 0;
        liftedEntitySlot = -1;
        enemyTransitionCountdown[slot] = entity.type() == ENTITY_BOMB
            ? BombMotion.INITIAL_COUNTDOWN : 0;
        slots[slot] = withStatus(entity, EntityStatus.THROWN);
        return true;
    }

    LiftedEntityState liftedEntityState() {
        if (liftedEntitySlot < 0 || liftedEntitySlot >= slots.length) {
            return LiftedEntityState.none();
        }
        RoomEntity entity = slots[liftedEntitySlot];
        if (!entity.loaded() || entity.status() != EntityStatus.LIFTED) {
            return LiftedEntityState.none();
        }
        return new LiftedEntityState(liftedEntitySlot, entity.type(),
            liftedPhase[liftedEntitySlot], enemyTransitionCountdown[liftedEntitySlot],
            liftedCarryState, liftedSourceDirection[liftedEntitySlot],
            liftedEffectiveDirection);
    }

    private RoomEntity advanceLiftedEntity(RoomEntity entity, int linkEntityX,
                                            int linkEntityY, int linkZ,
                                            int romLinkDirection) {
        int slot = entity.slot();
        if (!liftedStateInitialized[slot]) {
            liftedPhase[slot] = 0;
            liftedSourceDirection[slot] = romLinkDirection;
            liftedStateInitialized[slot] = true;
            liftedEffectiveDirection = romLinkDirection;
            enemyTransitionCountdown[slot] = 0x02;
        }

        LiftedEntityMotion.Update update = LiftedEntityMotion.advance(
            liftedPhase[slot], enemyTransitionCountdown[slot],
            liftedSourceDirection[slot], romLinkDirection,
            byteValue(linkEntityX), byteValue(linkEntityY), byteValue(linkZ),
            liftedLinkC13B, entity.type(), groundInteractionSideScrolling,
            false, entity.z());
        liftedPhase[slot] = update.phase();
        enemyTransitionCountdown[slot] = update.transitionCountdown();
        liftedCarryState = update.carryState();
        liftedEffectiveDirection = update.effectiveDirection();
        return new RoomEntity(entity.slot(), entity.sourceLoadOrder(), entity.type(),
            update.x(), update.y(), EntityStatus.LIFTED, entity.spriteDefinition(),
            entity.spriteVariant(), entity.entityFlipAttribute(), entity.spriteTileOffset(),
            update.z());
    }

    private RoomEntity advanceThrownEntity(RoomEntity entity,
                                            RoomEntityBackgroundCollision backgroundCollision,
                                            boolean sideScrolling,
                                            int romLinkDirection) {
        int slot = entity.slot();
        if (!thrownMotionInitialized[slot]) {
            thrownDirection[slot] = romLinkDirection;
            thrownEntityMotion.start(slot, romLinkDirection, entity.type(), sideScrolling);
            thrownMotionInitialized[slot] = true;
        }
        ThrownEntityMotion.Update update = thrownEntityMotion.advance(
            entity, sideScrolling, backgroundCollision);
        RoomEntity updated = update.entity();
        if (thrownEntityMotion.speedX(slot) == 0 && thrownEntityMotion.speedY(slot) == 0) {
            thrownEntityMotion.clear(slot);
            thrownMotionInitialized[slot] = false;
            enemyStunnedCountdown[slot] = 0xFF;
            updated = withStatus(updated, EntityStatus.STUNNED);
        }
        return updated;
    }

    private RoomEntity advancePlacedBombEntity(
            RoomEntity entity, RoomEntityBackgroundCollision backgroundCollision) {
        int slot = entity.slot();
        if (!placedBombMotionInitialized[slot]) {
            int direction = bombDirection[slot];
            if (direction < ThrownEntityMotion.ROM_DIRECTION_RIGHT
                || direction > ThrownEntityMotion.ROM_DIRECTION_DOWN) {
                // A room-loaded type-$02 entity has no SpawnPlayerProjectile direction.
                // Preserve its static source state instead of inventing a table index.
                return entity;
            }
            thrownEntityMotion.startPlacedBomb(slot, bombDirection[slot]);
            placedBombMotionInitialized[slot] = true;
        }
        return thrownEntityMotion.advance(entity, groundInteractionSideScrolling,
            backgroundCollision).entity();
    }

    private RoomEntity advanceEnemyBombEntity(
            RoomEntity entity, RoomEntityBackgroundCollision backgroundCollision) {
        int slot = entity.slot();
        if (!enemyBombMotionInitialized[slot]) {
            // Room-loaded type-$02 entities and legacy test-created enemy
            // bombs have no source speed tables to advance.
            return entity;
        }
        return thrownEntityMotion.advance(entity, groundInteractionSideScrolling,
            backgroundCollision).entity();
    }

    private static boolean isLiftableEntity(RoomEntity entity) {
        return switch (entity.type()) {
            case ENTITY_BOMB, ENTITY_LIFTABLE_ROCK, ENTITY_LIFTABLE_STATUE,
                ENTITY_WRECKING_BALL, ENTITY_SIDE_VIEW_POT, ENTITY_ROOSTER,
                ENTITY_CUCCO, ENTITY_HORSE_PIECE -> true;
            default -> false;
        };
    }

    /**
     * Runs the ported portion of DefaultEnemyDamageCollisionHandler for the
     * currently supported enemy families. The four sword arguments are the
     * ROM's wC140..wC143 rectangle; non-positive width/height disables sword
     * collision for this pass.
     */
    public List<EntityCombatEvent> resolveCombat(int frameCounter,
                                                 int linkEntityX,
                                                 int linkEntityY,
                                                 boolean linkAirborne,
                                                 int linkVerticalVelocity,
                                                 boolean linkInteractive,
                                                 boolean swordCollisionActive,
                                                 int swordX,
                                                 int swordWidth,
                                                 int swordY,
                                                 int swordHeight) {
        return resolveCombat(frameCounter, linkEntityX, linkEntityY, linkAirborne,
            linkVerticalVelocity, linkInteractive, swordCollisionActive, swordX, swordWidth,
            swordY, swordHeight, EnemyAttackContext.standard());
    }

    public List<EntityCombatEvent> resolveCombat(int frameCounter,
                                                 int linkEntityX,
                                                 int linkEntityY,
                                                 boolean linkAirborne,
                                                 boolean linkInteractive,
                                                 boolean swordCollisionActive,
                                                 int swordX,
                                                 int swordWidth,
                                                 int swordY,
                                                 int swordHeight) {
        return resolveCombat(frameCounter, linkEntityX, linkEntityY, linkAirborne,
            0, linkInteractive, swordCollisionActive, swordX, swordWidth, swordY, swordHeight,
            EnemyAttackContext.standard());
    }

    public List<EntityCombatEvent> resolveCombat(int frameCounter,
                                                 int linkEntityX,
                                                 int linkEntityY,
                                                 boolean linkAirborne,
                                                 boolean linkInteractive,
                                                 boolean swordCollisionActive,
                                                 int swordX,
                                                 int swordWidth,
                                                 int swordY,
                                                 int swordHeight,
                                                 EnemyAttackContext attackContext) {
        return resolveCombat(frameCounter, linkEntityX, linkEntityY, linkAirborne,
            0, linkInteractive, swordCollisionActive, swordX, swordWidth, swordY, swordHeight,
            attackContext);
    }

    public List<EntityCombatEvent> resolveCombat(int frameCounter,
                                                 int linkEntityX,
                                                 int linkEntityY,
                                                 boolean linkAirborne,
                                                 int linkVerticalVelocity,
                                                 boolean linkInteractive,
                                                 boolean swordCollisionActive,
                                                 int swordX,
                                                 int swordWidth,
                                                 int swordY,
                                                 int swordHeight,
                                                 EnemyAttackContext attackContext) {
        validateByte(linkVerticalVelocity, "Link vertical velocity");
        if (attackContext == null) {
            throw new IllegalArgumentException("Enemy attack context cannot be null");
        }
        runningWithPegasusBoots = attackContext.pegasusBoots();
        List<EntityCombatEvent> events = new ArrayList<>();
        for (int index = slots.length - 1; index >= 0; index--) {
            RoomEntity entity = slots[index];
            if (!entity.loaded() || entity.status() != EntityStatus.ACTIVE
                || !RoomEntityCombatRules.supportsEnemyCollision(entity.type())) {
                continue;
            }
            if (entity.type() == ENTITY_FISH
                && !fishMotion.allowsEnemyCollision(entity.slot())) {
                continue;
            }
            if (entity.type() == ENTITY_CROW
                && !crowMotion.allowsEnemyCollision(entity.slot())) {
                continue;
            }
            boolean booBuddyAllowsLinkCollision = entity.type() != ENTITY_BOO_BUDDY
                || booBuddyMotion.allowsEnemyCollision(entity.slot(), booBuddyTriggerCount,
                    enemyTransitionCountdown[entity.slot()], swordCollisionActive);
            boolean booBuddyAllowsSwordCollision = entity.type() != ENTITY_BOO_BUDDY
                || booBuddyMotion.allowsSwordCollision(entity.slot(), booBuddyTriggerCount);
            if (!booBuddyAllowsLinkCollision && !booBuddyAllowsSwordCollision) {
                continue;
            }
            if (isGhiniType(entity.type())
                && (ghiniMotion.hidden(entity.slot()) || entity.spriteVariant() < 0)) {
                continue;
            }
            if (entity.type() == ENTITY_SPIKED_BEETLE
                && !spikedBeetleMotion.allowsCombat(entity.slot())) {
                continue;
            }
            if (entity.type() == ENTITY_ARMOS_STATUE
                && !armosMotion.isActive(entity.slot())) {
                continue;
            }
            if (entity.type() == ENTITY_STALFOS_EVASIVE
                && stalfosEvasiveMotion.isAirborne(entity.slot())) {
                continue;
            }
            if (entity.type() == ENTITY_LEEVER && !leeverMotion.isChasing(entity.slot())) {
                continue;
            }
            boolean peaHatGrounded = entity.type() != ENTITY_PEAHAT
                || peaHatMotion.isGrounded(entity);
            if (entity.type() == ENTITY_PAIRODD
                && !pairoddMotion.allowsEnemyCollision(entity.slot())) {
                continue;
            }
            if (entity.type() == ENTITY_WIZROBE && wizrobeMotion.state(entity.slot()) != 3) {
                continue;
            }
            if (isZolGelType(entity.type()) && zolGelMotion.skipsEnemyCollision(entity.slot())) {
                continue;
            }
            boolean hidingZolSwordCollision = entity.type() != ENTITY_HIDING_ZOL
                || hidingZolMotion.allowsSwordCollision(entity.slot());
            boolean hidingZolLinkCollision = entity.type() != ENTITY_HIDING_ZOL
                || hidingZolMotion.allowsLinkCollision(entity.slot());
            if (!hidingZolSwordCollision && !hidingZolLinkCollision) {
                continue;
            }
            if (enemyFlashCountdown[entity.slot()] > 0
                || enemyIgnoreHitsCountdown[entity.slot()] > 0) {
                continue;
            }
            if ((entity.type() == ENTITY_TIMER_BOMBITE
                    && bombiteMotion.state(entity.slot()) == 1)
                || (entity.type() == ENTITY_BOUNCING_BOMBITE
                    && bombPrivateCountdown1[entity.slot()] > 0)) {
                continue;
            }

            boolean goombaStomp = canGoombaStomp(entity, linkEntityX, linkEntityY,
                linkAirborne, linkVerticalVelocity, linkInteractive, frameCounter);
            boolean linkCollision = entity.type() != ENTITY_LIKE_LIKE
                && !linkAirborne && linkInteractive
                && booBuddyAllowsLinkCollision
                && RoomEntityCombatRules.collisionCadenceMatches(frameCounter, entity.slot())
                && peaHatGrounded
                && hidingZolLinkCollision
                && RoomEntityCombatRules.overlapsLink(entity, linkEntityX, linkEntityY);
            boolean swordHit = swordCollisionActive
                && booBuddyAllowsSwordCollision
                && (entity.type() != ENTITY_LIKE_LIKE
                    || likeLikeMotion.state(entity.slot()) == 0)
                && hidingZolSwordCollision
                && RoomEntityCombatRules.overlapsSword(
                    entity, swordX, swordWidth, swordY, swordHeight);
            if (!linkCollision && !swordHit) {
                if (!goombaStomp) {
                    continue;
                }
            }

            if (goombaStomp) {
                goombaMotion.enterStomp(entity.slot());
                enemyTransitionCountdown[entity.slot()] = 0x30;
                EntityCombatEvent.LinkAction linkAction = groundInteractionSideScrolling
                    ? EntityCombatEvent.LinkAction.GOOMBA_BOUNCE_SIDE_SCROLLING
                    : EntityCombatEvent.LinkAction.GOOMBA_BOUNCE_TOP_DOWN;
                events.add(new EntityCombatEvent(
                    entity.slot(), entity.type(), 0, false, 0, -1,
                    EntityCombatEvent.SoundChannel.WAVE, 0x0E,
                    EntityCombatEvent.SoundChannel.NONE, -1, null, linkAction));
                continue;
            }

            EntityCombatEvent.SoundChannel soundChannel = EntityCombatEvent.SoundChannel.NONE;
            int soundId = -1;
            EntityCombatEvent.SoundChannel secondarySoundChannel =
                EntityCombatEvent.SoundChannel.NONE;
            int secondarySoundId = -1;
            int enemyDamage = 0;
            int enemySpecialAction = -1;
            EntityCombatEvent.SwordPokeVfx swordPokeVfx = null;
            boolean peaHatSwordClink = entity.type() == ENTITY_PEAHAT && !peaHatGrounded;
            boolean spikedBeetleSwordClink = entity.type() == ENTITY_SPIKED_BEETLE
                && (options1(entity.slot()) & 0x40) != 0;
            if (entity.type() == ENTITY_CRYSTAL_SWITCH) {
                if (swordHit) {
                    // The crystal handler uses the normal enemy-hit path for
                    // the flash/ignore timers, but its preceding health write
                    // prevents the generic damage path from killing it.
                    enemyIgnoreHitsCountdown[entity.slot()] = attackContext.powerRecoil()
                        ? 0x20 : 0x0A;
                    enemyFlashCountdown[entity.slot()] = 0x18;
                    enemyRecoilMotion.clear(entity.slot());
                    soundChannel = EntityCombatEvent.SoundChannel.JINGLE;
                    soundId = 0x03;
                }
            } else if (swordHit && entity.type() == ENTITY_IRON_MASK
                && ironMaskPrivateState2[entity.slot()] == 0
                && lastRomLinkDirection != roamingEnemyMotion.direction(entity.slot())) {
                // DefaultEnemyDamageCollisionHandler's masked Iron Mask branch
                // only accepts a sword hit from the direction the mask faces.
                // A rear hit pushes Link and the mask apart, then uses the
                // shared sword-poke presentation without applying damage.
                swordPokeVfx = new EntityCombatEvent.SwordPokeVfx(
                    byteValue(swordX - 0x08), byteValue(swordY - 0x08));
                enemyIgnoreHitsCountdown[entity.slot()] = 0x10;
                enemyRecoilMotion.configure(
                    entity.slot(), entity.x(), entity.y(), entity.z(),
                    linkEntityX, linkEntityY, 0x10);
                soundChannel = EntityCombatEvent.SoundChannel.JINGLE;
                soundId = 0x07;
            } else if (swordHit && entity.type() == ENTITY_SPIKED_BEETLE
                && !spikedBeetleSwordClink) {
                // The initial static options byte reaches the Beetle-specific
                // EnemyCollidedWithSword branch and flips the shell without
                // applying damage. Later normal handler passes write the
                // sword-clink-off bit, which takes the shared poke path below.
                spikedBeetleMotion.flipFromSword(entity.slot(), lastRomLinkDirection);
                enemyTransitionCountdown[entity.slot()] = 0xFF;
                enemyIgnoreHitsCountdown[entity.slot()] = 0;
                enemyRecoilMotion.clear(entity.slot());
                soundChannel = EntityCombatEvent.SoundChannel.JINGLE;
                soundId = 0x09;
            } else if (swordHit && RoomEntityCombatRules.swordPokeForSwordCollision(
                entity.type(), peaHatSwordClink || spikedBeetleSwordClink)) {
                // EnemyCollidedWithSword's ENTITY_OPT1_SWORD_CLINK_OFF path
                // calls label_D07/label_D15: no damage or normal recoil,
                // sixteen ignored-hit frames, then the sword-poke VFX and
                // jingle writes.
                swordPokeVfx = new EntityCombatEvent.SwordPokeVfx(
                    byteValue(swordX - 0x08), byteValue(swordY - 0x08));
                enemyIgnoreHitsCountdown[entity.slot()] = 0x10;
                enemyRecoilMotion.clear(entity.slot());
                soundChannel = EntityCombatEvent.SoundChannel.JINGLE;
                soundId = 0x07;
            } else if (swordHit && entity.type() == ENTITY_BOUNCING_BOMBITE) {
                // EnemyCollidedWithSword's Bombite branch is not a damage hit:
                // it enters state-$02, reverses the vector returned by
                // GetVectorTowardsLink, and starts the private lit window.
                bombiteMotion.enterBouncingLitFromSword(
                    entity.slot(), entity, linkEntityX, linkEntityY);
                enemyTransitionCountdown[entity.slot()] = 0x40;
                bombPrivateCountdown1[entity.slot()] = 0x08;
                enemyRecoilMotion.clear(entity.slot());
            } else if (swordHit) {
                RomEnemyCombatTables.SwordDamageResult swordResult =
                    enemyCombatTables == null ? null
                        : enemyCombatTables.resolveSwordDamage(entity.type(), attackContext);
                int swordDamage = swordResult == null
                    ? RoomEntityCombatRules.basicSwordDamage(entity.type())
                    : swordResult.numericDamage();
                enemyDamage = swordDamage;
                if (swordResult != null) {
                    enemySpecialAction = swordResult.specialAction();
                }
                boolean swordResultApplied = swordResult == null
                    ? swordDamage > 0
                    : !swordResult.ignored();
                if (usesSharedRecoil(entity.type())) {
                    // EnemyCollidedWithSword applies the default `$30` recoil
                    // before ApplySwordDamagesToEnemy changes health.
                    enemyRecoilMotion.configure(
                        entity.slot(), entity.x(), entity.y(), entity.z(),
                        linkEntityX, linkEntityY, 0x30);
                }
                // ConfigureEntityRecoil reaches StartIgnoringHitsForEntity
                // before ApplySwordDamagesToEnemy, including lethal hits.
                enemyIgnoreHitsCountdown[entity.slot()] = attackContext.powerRecoil()
                    ? 0x20 : 0x0A;
                if (swordDamage > 0) {
                    enemyHealth[entity.slot()] = Math.max(0,
                        enemyHealth[entity.slot()] - swordDamage);
                }
                soundChannel = EntityCombatEvent.SoundChannel.JINGLE;
                soundId = swordResultApplied ? 0x03 : 0x09;
                if (enemySpecialAction == 0xFE) {
                    // ApplySwordDamagesToEnemy's burning branch starts the
                    // shared $60 transition and emits NOISE_SFX_BURSTING_FLAME.
                    enemyTransitionCountdown[entity.slot()] = 0x60;
                    enemyStunnedCountdown[entity.slot()] = 0;
                    enemyFlashCountdown[entity.slot()] = 0;
                    enemyIgnoreHitsCountdown[entity.slot()] = 0x0A;
                    enemyRecoilMotion.clear(entity.slot());
                    slots[entity.slot()] = withStatus(entity, EntityStatus.BURNING);
                    secondarySoundChannel = EntityCombatEvent.SoundChannel.NOISE;
                    secondarySoundId = 0x12;
                } else if (enemySpecialAction == 0xFF) {
                    // EntityBecomeStunned uses private countdown 2 and clears
                    // vertical speed; the runtime has no separate Z velocity
                    // for this status, so the countdown is the authoritative
                    // stun state here.
                    enemyTransitionCountdown[entity.slot()] = 0;
                    enemyStunnedCountdown[entity.slot()] = 0xFF;
                    enemyFlashCountdown[entity.slot()] = 0;
                    enemyIgnoreHitsCountdown[entity.slot()] = 0x0A;
                    enemyRecoilMotion.clear(entity.slot());
                    slots[entity.slot()] = withStatus(entity, EntityStatus.STUNNED);
                } else if (swordDamage > 0 && enemyHealth[entity.slot()] == 0) {
                    dyingCountdown[entity.slot()] = 0x40;
                    powerRecoilDeath[entity.slot()] = attackContext.powerRecoil();
                    slots[entity.slot()] = withDeathPresentation(
                        withStatus(entity, EntityStatus.DYING), -1,
                        powerRecoilDeath[entity.slot()]);
                } else if (swordDamage > 0) {
                    // jr_003_73B6 and StartIgnoringHitsForEntity: a normal
                    // sword hit flashes for $18 frames and suppresses the
                    // next $0A collision passes.
                    enemyFlashCountdown[entity.slot()] = 0x18;
                    enemyIgnoreHitsCountdown[entity.slot()] = attackContext.powerRecoil()
                        ? 0x20 : 0x0A;
                    if (isZolGelType(entity.type())) {
                        zolGelMotion.onSwordHit(entity.slot());
                    }
                    if (entity.type() == ENTITY_ZOL && spriteHandlers != null) {
                        entity = withDefinition(entity, spriteHandlers.forZolSlimeEye(),
                            entity.spriteVariant());
                        slots[entity.slot()] = entity;
                    }
                }
            }
            if (linkCollision && entity.type() == ENTITY_GEL) {
                zolGelMotion.onLinkCollision(entity.slot());
            }
            events.add(new EntityCombatEvent(
                entity.slot(), entity.type(),
                linkCollision ? contactDamage(entity.type()) : 0,
                swordHit, enemyDamage, enemySpecialAction, soundChannel, soundId,
                secondarySoundChannel, secondarySoundId, swordPokeVfx));
        }
        return List.copyOf(events);
    }

    /** Advances entity $01 through the ROM boomerang handler's two states. */
    private void advanceBoomerangEntity(int index, RoomEntity entity, int frame,
                                         int linkEntityX, int linkEntityY, int linkEntityZ,
                                         int linkMotionState) {
        int slot = entity.slot();
        boomerangMotion.decrementTransitionCountdown(slot);
        if (boomerangSfxCounter == 0) {
            pendingEntityEvents.add(new EntityCombatEvent(
                slot, ENTITY_BOOMERANG, 0, false,
                EntityCombatEvent.SoundChannel.NOISE, BOOMERANG_SFX_ID));
        }
        boomerangSfxCounter = (boomerangSfxCounter + 1) % BOOMERANG_SFX_COUNTER_PERIOD;

        int variant = entity.spriteVariant();
        if ((frame & 0x03) == 0) {
            variant = (variant + 1) & 0x03;
        }

        // label_3B7B runs before the position update and uses the launch
        // vector for recoil. This pass also clears the outbound countdown on
        // a successful enemy/object collision, as the ROM does.
        collideBoomerangWithEntities(entity, frame);
        RoomEntity moved = boomerangMotion.advancePosition(entity);
        RoomEntityObjectQuery intersectionQuery = objectIntersectionQuery != null
            ? objectIntersectionQuery : objectQuery;
        RoomEntityObjectSample object = intersectionQuery == null
            ? null : intersectionQuery.sample(moved);
        boolean objectCollision = object != null
            && boomerangObjectCollision(moved, object, frame, slot);
        boolean bushCollision = object != null && !indoorRoom
            && (object.objectId() == OBJECT_BUSH
                || object.objectId() == OBJECT_BUSH_GROUND_STAIRS);
        RoomEntity positioned = objectCollision
            && boomerangMotion.transitionCountdown(slot) != 0 ? entity : moved;
        if (bushCollision) {
            int location = (object.objectTop() | (object.objectLeft() >>> 4)) & 0xFF;
            boomerangObjectRequests.add(new BoomerangObjectRequest(
                slot, location, object.objectLeft(), object.objectTop()));
        }

        // BoomerangDestroyBushIfNeeded clears the source collision byte before
        // the state handler, so a bush does not also produce the wall-poke
        // branch in the same frame.
        boolean stateCollision = objectCollision && !bushCollision;
        if (boomerangMotion.state(slot) == 0) {
            if (boomerangMotion.transitionCountdown(slot) == 0) {
                boomerangMotion.setVectorTowardsLink(slot, positioned,
                    linkEntityX, linkEntityY, linkEntityZ, 0x08);
                boomerangMotion.setState(slot, 1);
            } else if (stateCollision) {
                boomerangMotion.setTransitionCountdown(slot, 0);
                transientVfxRequests.add(new TransientVfxRequest(
                    TransientVfxType.SWORD_POKE, positioned.x(),
                    (positioned.y() - positioned.z() + 0x03) & 0xFF));
                pendingEntityEvents.add(new EntityCombatEvent(
                    slot, ENTITY_BOOMERANG, 0, false,
                    EntityCombatEvent.SoundChannel.JINGLE, 0x07));
            }
        } else {
            if ((frame & 0x03) == 0) {
                boomerangMotion.setVectorTowardsLink(slot, positioned,
                    linkEntityX, linkEntityY, linkEntityZ, 0x20);
            }
            if (((frame ^ slot) & 0x01) != 0 && linkEntityZ == 0
                && linkMotionState < 0x02
                && BoomerangMotion.overlapsLink(positioned, linkEntityX, linkEntityY)) {
                clearEntity(slot);
                return;
            }
        }
        slots[index] = withPositionAndVariant(positioned, positioned.x(), positioned.y(),
            variant);
    }

    /** Advances entity {@code $DF} through the ROM sword-beam handler. */
    private void advanceSwordBeamEntity(int index, RoomEntity entity, int frame,
                                        int linkEntityX, int linkEntityY,
                                        int romLinkDirection, int linkMotionState) {
        int slot = entity.slot();
        if (swordBeamMotion.state(slot) == 0) {
            int x = (linkEntityX + SwordBeamMotion.firstHandlerOffsetX(romLinkDirection))
                & 0xFF;
            int y = (linkEntityY + SwordBeamMotion.firstHandlerOffsetY(romLinkDirection))
                & 0xFF;
            swordBeamMotion.initializeFirstHandler(slot, romLinkDirection);
            enemyFlashCountdown[slot] = 0xFF;
            pendingEntityEvents.add(new EntityCombatEvent(
                slot, ENTITY_SWORD_BEAM, 0, false,
                EntityCombatEvent.SoundChannel.JINGLE, SWORD_BEAM_JINGLE_ID));
            // The state-0 handler only applies the Link-relative launch
            // offset and enters state 1; it does not render the beam yet.
            slots[index] = withPositionAndVariant(entity, x, y, -1);
            return;
        }

        int variant = swordBeamMotion.direction(slot);
        if (linkMotionState >= 0x02) {
            // ReturnIfNonInteractive_19 renders state 1, then returns before
            // damage, movement, object intersection, or VFX creation.
            slots[index] = withPositionAndVariant(entity, entity.x(), entity.y(), variant);
            return;
        }

        boolean hitEntity = collideSwordBeamWithEntities(entity, frame);
        RoomEntity moved = swordBeamMotion.advancePosition(entity);
        RoomEntityObjectQuery intersectionQuery = objectIntersectionQuery != null
            ? objectIntersectionQuery : objectQuery;
        RoomEntityObjectSample object = intersectionQuery == null
            ? null : intersectionQuery.sample(moved);
        boolean hitObject = object != null
            && swordBeamObjectCollision(moved, object, frame, slot);
        if (hitEntity || hitObject) {
            clearEntity(slot);
            return;
        }

        if (((frame + 1) & 0x03) == 0) {
            transientVfxRequests.add(new TransientVfxRequest(
                TransientVfxType.SWORD_BEAM, moved.x(),
                (moved.y() - moved.z()) & 0xFF, variant));
        }
        slots[index] = withPositionAndVariant(moved, moved.x(), moved.y(), variant);
    }

    /** Advances entity {@code $04} through MagicRodFireballEntityHandler. */
    private void advanceMagicRodFireballEntity(int index, RoomEntity entity, int frame,
                                               int linkMotionState) {
        int slot = entity.slot();
        int variant = MagicRodFireballMotion.frameVariant(frame);

        // The source calls func_003_75A2 before either the wall-fire branch or
        // ReturnIfNonInteractive. A fireball can therefore still deliver the
        // common target collision while Link is being transitioned.
        boolean hitEntity = collideMagicRodFireballWithEntities(entity, frame);
        if (magicRodFireballMotion.privateCountdown1(slot) != 0) {
            if (magicRodFireballMotion.tickFireTransition(slot)) {
                clearEntity(slot);
                return;
            }
            EntitySpriteDefinition fireDefinition = spriteHandlers == null
                ? entity.spriteDefinition() : spriteHandlers.forMagicRodFireState();
            slots[index] = withDefinition(entity, fireDefinition, variant);
            return;
        }

        // ReturnIfNonInteractive_03 is after the normal sprite write and
        // before movement/object lookup in the ROM helper.
        if (linkMotionState >= 0x02) {
            slots[index] = withVariant(entity, variant);
            return;
        }

        RoomEntity moved = magicRodFireballMotion.advancePosition(entity);
        RoomEntityObjectQuery intersectionQuery = objectIntersectionQuery != null
            ? objectIntersectionQuery : objectQuery;
        RoomEntityObjectSample object = intersectionQuery == null
            ? null : intersectionQuery.sample(moved);
        if (isMagicRodBurnableObject(object)) {
            int location = (object.objectTop() | (object.objectLeft() >>> 4)) & 0xFF;
            magicRodObjectRequests.add(new MagicRodObjectRequest(
                slot, location, object.objectLeft(), object.objectTop()));
            slots[index] = withVariant(moved, variant);
            return;
        }

        boolean hitObject = object != null
            && swordBeamObjectCollision(moved, object, frame, slot);
        if (hitEntity || hitObject) {
            // ArrowRenderAndMove writes privateCountdown1 after the normal
            // fireball display has already been rendered. Keep the normal
            // definition for this frame; the next handler tick switches to
            // FireSpriteVariants and decrements the $30 countdown.
            magicRodFireballMotion.beginFireTransition(slot);
        }
        slots[index] = withVariant(moved, variant);
    }

    private boolean isMagicRodBurnableObject(RoomEntityObjectSample object) {
        if (object == null) {
            return false;
        }
        int objectId = object.objectId() & 0xFF;
        if (indoorRoom) {
            return objectId == 0x8A; // OBJECT_FROZEN_BLOCK
        }
        return objectId == OBJECT_BUSH || objectId == OBJECT_BUSH_GROUND_STAIRS;
    }

    /** Advances entity {@code $08} through MagicPowderSprinkleEntityHandler. */
    private void advanceMagicPowderSprinkleEntity(int index, RoomEntity entity, int frame,
                                                   int linkMotionState) {
        int slot = entity.slot();
        decrementSlowTransitionCountdown(slot, frame);

        if (magicPowderState[slot] != 0) {
            // State one has no ordinary sprinkle rectangle: the source emits
            // its short torch pair only at the state transition boundaries.
            if (linkMotionState >= 0x02) {
                slots[index] = withVariant(entity, -1);
                return;
            }
            if (slowTransitionCountdown[slot] != 0) {
                slots[index] = withVariant(entity, -1);
                return;
            }

            int objectLeft = entity.x() & 0xF0;
            int objectTop = entity.y() & 0xF0;
            int location = (objectTop | (objectLeft >>> 4)) & 0xFF;
            magicPowderObjectRequests.add(new MagicPowderObjectRequest(
                slot, location, objectLeft, objectTop,
                MagicPowderObjectAction.EXTINGUISH_TORCH));
            clearEntity(slot);
            return;
        }

        int transitionCountdown = enemyTransitionCountdown[slot];
        if (transitionCountdown == 0) {
            clearEntity(slot);
            return;
        }

        // func_018_7B02 runs before ReturnIfNonInteractive_18 and selects
        // ((transitionCountdown >> 2) & 7) from the ROM rectangle list.
        slots[index] = withVariant(entity, (transitionCountdown >>> 2) & 0x07);
        if (linkMotionState >= 0x02) {
            return;
        }

        if (transitionCountdown != 0x07) {
            if (transitionCountdown < 0x10) {
                collideMagicPowderWithEntities(entity, frame);
            }
            return;
        }

        RoomEntityObjectQuery intersectionQuery = objectIntersectionQuery != null
            ? objectIntersectionQuery : objectQuery;
        RoomEntityObjectSample object = intersectionQuery == null
            ? null : intersectionQuery.sample(entity);
        int objectLeft = magicPowderObjectLeft(entity);
        int objectTop = magicPowderObjectTop(entity);
        int location = (objectTop | (objectLeft >>> 4)) & 0xFF;
        int objectId = object == null ? 0xFF : object.objectId() & 0xFF;

        if (!indoorRoom && (objectId == OBJECT_BUSH
            || objectId == OBJECT_BUSH_GROUND_STAIRS)) {
            magicPowderObjectRequests.add(new MagicPowderObjectRequest(
                slot, location, objectLeft, objectTop, MagicPowderObjectAction.REVEAL));
            transientVfxRequests.add(new TransientVfxRequest(
                TransientVfxType.POOF, entity.x(), (entity.y() - entity.z()) & 0xFF));
            pendingEntityEvents.add(new EntityCombatEvent(
                slot, ENTITY_MAGIC_POWDER_SPRINKLE, 0, false,
                EntityCombatEvent.SoundChannel.JINGLE, MAGIC_POWDER_POOF_JINGLE_ID));
            clearEntity(slot);
            return;
        }

        if (indoorRoom && objectId == OBJECT_TORCH_UNLIT) {
            magicPowderState[slot] = 1;
            slowTransitionCountdown[slot] = MAGIC_POWDER_SLOW_TRANSITION_COUNTDOWN;
            slowTimerInitialized[slot] = true;
            enemyTransitionCountdown[slot] = 0;
            RoomEntity torch = withPositionAndVariant(entity, objectLeft, objectTop, 0);
            EntitySpriteDefinition torchDefinition = spriteHandlers == null
                ? entity.spriteDefinition() : spriteHandlers.forMagicPowderTorchState(true);
            slots[index] = withDefinition(torch, torchDefinition, 0);
            magicPowderObjectRequests.add(new MagicPowderObjectRequest(
                slot, location, objectLeft, objectTop,
                MagicPowderObjectAction.IGNITE_TORCH));
            pendingEntityEvents.add(new EntityCombatEvent(
                slot, ENTITY_MAGIC_POWDER_SPRINKLE, 0, false,
                EntityCombatEvent.SoundChannel.NOISE, 0x12));
            return;
        }

        // label_018_7A4B reaches the common collision helper only during the
        // final sixteen transition ticks and only when private state 4 is 0.
        if (transitionCountdown < 0x10 && magicPowderPrivateState4[slot] == 0) {
            collideMagicPowderWithEntities(entity, frame);
        }
    }

    private static int magicPowderObjectLeft(RoomEntity entity) {
        return (((entity.x() + 0x07 - 0x08) & 0xFF) & 0xF0);
    }

    private static int magicPowderObjectTop(RoomEntity entity) {
        return (((entity.y() + 0x07 - 0x10) & 0xFF) & 0xF0);
    }

    private boolean collideMagicPowderWithEntities(RoomEntity sprinkle, int frame) {
        int sourceSlot = sprinkle.slot();
        int sourceVisualY = (sprinkle.y() - sprinkle.z()) & 0xFF;
        boolean collided = false;
        for (int targetSlot = slots.length - 1; targetSlot >= 0; targetSlot--) {
            if (targetSlot == sourceSlot || ((frame ^ targetSlot) & 0x01) != 0) {
                continue;
            }

            RoomEntity target = slots[targetSlot];
            int targetPhysics = enemyPhysicsFlags[targetSlot];
            if (!target.loaded()
                || target.status().value() < EntityStatus.ACTIVE.value()
                || (targetPhysics & ENTITY_PHYSICS_GRABBABLE) != 0
                || !RoomEntityCombatRules.supportsEnemyCollision(target.type())
                || (targetPhysics & ENTITY_PHYSICS_PROJECTILE_NOCLIP) != 0
                || (enemyHitboxFlags[targetSlot] & HITFLAGS_IGNORE_HITS) != 0
                || enemyIgnoreHitsCountdown[targetSlot] != 0
                || target.spriteVariant() < 0
                || unsignedByteAbs(sprinkle.x() - target.x()) >= 0x0C
                || unsignedByteAbs(sourceVisualY
                    - ((target.y() - target.z()) & 0xFF)) >= 0x0C) {
                continue;
            }

            collided = true;
            applyPlayerProjectileDamage(target, DAMAGE_TYPE_MAGIC_POWDER, 0, 0);
        }
        return collided;
    }

    private boolean boomerangObjectCollision(RoomEntity entity,
                                              RoomEntityObjectSample object,
                                              int frame, int slot) {
        if (!BoomerangMotion.objectPhysicsCollides(object.physicsFlag())) {
            return false;
        }
        int physics = object.physicsFlag() & 0xFF;
        if (physics >= 0xD0 && physics <= 0xD3
            && thrownDirection[slot] == physics - 0xD0) {
            if (entity.z() != 0) {
                entityUnknownJ[slot] = (entityUnknownJ[slot] + 1) & 0xFF;
                return false;
            }
            return true;
        }
        if (entityUnknownJ[slot] != 0 && physics != 0xFF) {
            // func_003_7D6B's freshly spawned projectile guard: consume the
            // source's unknownTableJ on its cadence before accepting a solid
            // terrain collision.
            if ((frame & 0x03) == 0 || (!indoorRoom && (frame & 0x01) == 0)) {
                return false;
            }
            entityUnknownJ[slot] = 0;
            return false;
        }
        return true;
    }

    /** The non-boomerang result path of ApplySwordIntersectionWithObjects. */
    private boolean swordBeamObjectCollision(RoomEntity entity,
                                             RoomEntityObjectSample object,
                                             int frame, int slot) {
        int physics = object.physicsFlag() & 0xFF;
        if (physics >= 0xD0 && physics <= 0xD3) {
            if (thrownDirection[slot] == physics - 0xD0) {
                if (entity.z() != 0) {
                    entityUnknownJ[slot] = (entityUnknownJ[slot] + 1) & 0xFF;
                    return false;
                }
                return true;
            }
            if (entityUnknownJ[slot] != 0) {
                if ((frame & 0x03) == 0
                    || (!indoorRoom && (frame & 0x01) == 0)) {
                    return false;
                }
                entityUnknownJ[slot] = (entityUnknownJ[slot] - 1) & 0xFF;
            }
            return false;
        }
        return BoomerangMotion.objectPhysicsCollides(physics);
    }

    /** Common bank-$03 collision pass for entity {@code $DF}. */
    private boolean collideSwordBeamWithEntities(RoomEntity beam, int frame) {
        int sourceSlot = beam.slot();
        int sourceVisualY = (beam.y() - beam.z()) & 0xFF;
        boolean collided = false;
        for (int targetSlot = slots.length - 1; targetSlot >= 0; targetSlot--) {
            if (targetSlot == sourceSlot || ((frame ^ targetSlot) & 0x01) != 0) {
                continue;
            }

            RoomEntity target = slots[targetSlot];
            int targetPhysics = enemyPhysicsFlags[targetSlot];
            if (!target.loaded()
                || target.status().value() < EntityStatus.ACTIVE.value()
                || (targetPhysics & ENTITY_PHYSICS_GRABBABLE) != 0
                || !RoomEntityCombatRules.supportsEnemyCollision(target.type())
                || (targetPhysics & ENTITY_PHYSICS_PROJECTILE_NOCLIP) != 0
                || (enemyHitboxFlags[targetSlot] & HITFLAGS_IGNORE_HITS) != 0
                || enemyIgnoreHitsCountdown[targetSlot] != 0
                || target.spriteVariant() < 0
                || unsignedByteAbs(beam.x() - target.x()) >= 0x0C
                || unsignedByteAbs(sourceVisualY
                    - ((target.y() - target.z()) & 0xFF)) >= 0x0C) {
                continue;
            }

            collided = true;
            applyPlayerProjectileDamage(target, DAMAGE_TYPE_SWORD_BEAM,
                swordBeamMotion.speedX(sourceSlot), swordBeamMotion.speedY(sourceSlot));
        }
        return collided;
    }

    /** Common bank-$03 collision pass with DAMAGE_TYPE_MAGIC_ROD ($0A). */
    private boolean collideMagicRodFireballWithEntities(RoomEntity fireball, int frame) {
        int sourceSlot = fireball.slot();
        int sourceVisualY = (fireball.y() - fireball.z()) & 0xFF;
        boolean collided = false;
        for (int targetSlot = slots.length - 1; targetSlot >= 0; targetSlot--) {
            if (targetSlot == sourceSlot || ((frame ^ targetSlot) & 0x01) != 0) {
                continue;
            }

            RoomEntity target = slots[targetSlot];
            int targetPhysics = enemyPhysicsFlags[targetSlot];
            if (!target.loaded()
                || target.status().value() < EntityStatus.ACTIVE.value()
                || (targetPhysics & ENTITY_PHYSICS_GRABBABLE) != 0
                || !RoomEntityCombatRules.supportsEnemyCollision(target.type())
                || (targetPhysics & ENTITY_PHYSICS_PROJECTILE_NOCLIP) != 0
                || (enemyHitboxFlags[targetSlot] & HITFLAGS_IGNORE_HITS) != 0
                || enemyIgnoreHitsCountdown[targetSlot] != 0
                || target.spriteVariant() < 0
                || unsignedByteAbs(fireball.x() - target.x()) >= 0x0C
                || unsignedByteAbs(sourceVisualY
                    - ((target.y() - target.z()) & 0xFF)) >= 0x0C) {
                continue;
            }

            collided = true;
            applyPlayerProjectileDamage(target, DAMAGE_TYPE_MAGIC_ROD,
                magicRodFireballMotion.speedX(sourceSlot),
                magicRodFireballMotion.speedY(sourceSlot));
        }
        return collided;
    }

    /**
     * Port of the common bank-$03 func_003_75A2 pass for player arrows. The
     * active arrow checks targets in descending slot order and uses the same
     * alternating frame cadence and unsigned twelve-pixel windows as the ROM.
     */
    private boolean collidePlayerArrowWithEntities(RoomEntity arrow, int frame) {
        boolean collided = false;
        int arrowSlot = arrow.slot();
        int attackType = playerArrowBombArrow[arrowSlot]
            ? DAMAGE_TYPE_BOMB_ARROW : DAMAGE_TYPE_ARROW;
        int arrowVisualY = (arrow.y() - arrow.z()) & 0xFF;
        for (int targetSlot = slots.length - 1; targetSlot >= 0; targetSlot--) {
            if (targetSlot == arrowSlot
                || ((frame ^ targetSlot) & 0x01) != 0) {
                continue;
            }

            RoomEntity target = slots[targetSlot];
            if (!target.loaded()
                || target.status().value() < EntityStatus.ACTIVE.value()
                || !RoomEntityCombatRules.supportsEnemyCollision(target.type())
                || (enemyPhysicsFlags[targetSlot] & ENTITY_PHYSICS_PROJECTILE_NOCLIP) != 0
                || (enemyHitboxFlags[targetSlot] & HITFLAGS_IGNORE_HITS) != 0
                || enemyIgnoreHitsCountdown[targetSlot] != 0
                || target.spriteVariant() < 0) {
                continue;
            }

            if (unsignedByteAbs(arrow.x() - target.x()) >= 0x0C
                || unsignedByteAbs(
                    arrowVisualY - ((target.y() - target.z()) & 0xFF)) >= 0x0C) {
                continue;
            }

            collided = true;
            if (attackType == DAMAGE_TYPE_BOMB_ARROW) {
                // BombArrowHandler selects DAMAGE_TYPE_BOMB_ARROW. The
                // shared damage helper deliberately treats this active-arrow
                // state as a harmless target transition rather than calling
                // ApplySwordDamagesToEnemy.
                enemyTransitionCountdown[targetSlot] = 0x03;
            } else {
                applyPlayerArrowDamage(arrow, target);
            }
        }
        return collided;
    }

    /** Common bank-$03 collision pass for the active boomerang. */
    private boolean collideBoomerangWithEntities(RoomEntity boomerang, int frame) {
        int sourceSlot = boomerang.slot();
        int sourceVisualY = (boomerang.y() - boomerang.z()) & 0xFF;
        boolean collided = false;
        for (int targetSlot = slots.length - 1; targetSlot >= 0; targetSlot--) {
            if (targetSlot == sourceSlot || ((frame ^ targetSlot) & 0x01) != 0) {
                continue;
            }

            RoomEntity target = slots[targetSlot];
            int targetPhysics = enemyPhysicsFlags[targetSlot];
            boolean grabbable = (targetPhysics & ENTITY_PHYSICS_GRABBABLE) != 0;
            if (!target.loaded()
                || target.status().value() < EntityStatus.ACTIVE.value()
                || (!grabbable && !RoomEntityCombatRules.supportsEnemyCollision(target.type()))
                || (targetPhysics & ENTITY_PHYSICS_PROJECTILE_NOCLIP) != 0
                || (enemyHitboxFlags[targetSlot] & HITFLAGS_IGNORE_HITS) != 0
                || enemyIgnoreHitsCountdown[targetSlot] != 0
                || target.spriteVariant() < 0
                || unsignedByteAbs(boomerang.x() - target.x()) >= 0x0C
                || unsignedByteAbs(sourceVisualY
                    - ((target.y() - target.z()) & 0xFF)) >= 0x0C) {
                continue;
            }

            collided = true;
            boomerangMotion.setTransitionCountdown(sourceSlot, 0);
            if (grabbable) {
                // The source writes sourceSlot + 1 to the target's private
                // state five. The current runtime has no consumer for that
                // field, but the source-side transition is still observable.
                continue;
            }
            applyPlayerProjectileDamage(target, DAMAGE_TYPE_BOOMERANG,
                boomerangMotion.speedX(sourceSlot), boomerangMotion.speedY(sourceSlot));
        }
        return collided;
    }

    /**
     * Ports the BouncingBombite-specific portion of bank-$03
     * {@code func_003_75A2}. The ROM scans every target in descending slot
     * order, on alternating frames, and uses the active Bombite's transition
     * byte as the source-side explosion latch.
     */
    private boolean collideBouncingBombiteWithEntities(RoomEntity source, int frame) {
        int sourceSlot = source.slot();
        int sourceVisualY = (source.y() - source.z()) & 0xFF;
        int sourceSpeedX = bombiteMotion.speedX(sourceSlot);
        int sourceSpeedY = bombiteMotion.speedY(sourceSlot);
        boolean collided = false;
        for (int targetSlot = slots.length - 1; targetSlot >= 0; targetSlot--) {
            if (targetSlot == sourceSlot
                || ((frame ^ targetSlot) & 0x01) != 0) {
                continue;
            }

            RoomEntity target = slots[targetSlot];
            if (!target.loaded()
                || target.status().value() < EntityStatus.ACTIVE.value()
                || (enemyPhysicsFlags[targetSlot] & ENTITY_PHYSICS_PROJECTILE_NOCLIP) != 0
                || unsignedByteAbs(source.x() - target.x()) >= 0x0C
                || unsignedByteAbs(sourceVisualY
                    - ((target.y() - target.z()) & 0xFF)) >= 0x0C
                || target.spriteVariant() == -1) {
                continue;
            }

            collided = true;
            // GetEntityTransitionCountdown uses BC, which remains the active
            // source slot throughout func_003_75A2.
            enemyTransitionCountdown[sourceSlot] = 0;
            if (target.type() == ENTITY_BOUNCING_BOMBITE) {
                bombiteMotion.enterBouncingLitFromEntityCollision(
                    targetSlot, sourceSpeedX, sourceSpeedY);
                enemyTransitionCountdown[targetSlot] = 0x40;
                bombPrivateCountdown1[targetSlot] = 0x08;
            }
        }
        return collided;
    }

    /**
     * Ports the Iron Mask target branch of bank-$03 {@code func_003_75A2} for
     * the active hookshot chain. Other hookshot target damage branches remain
     * owned by their entity-specific runtimes; this branch is special because
     * it changes the target's private state and creates a visible mask entity.
     */
    private boolean collideHookshotWithEntities(
            RoomEntity hookshot, HookshotChainMotion.State hookshotState, int frame) {
        boolean unmasked = false;
        int sourceSlot = hookshot.slot();
        int sourceVisualY = (hookshotState.y() - hookshotState.z()) & 0xFF;
        for (int targetSlot = slots.length - 1; targetSlot >= 0; targetSlot--) {
            if (targetSlot == sourceSlot || ((frame ^ targetSlot) & 0x01) != 0) {
                continue;
            }

            RoomEntity target = slots[targetSlot];
            if (!target.loaded()
                || target.status().value() < EntityStatus.ACTIVE.value()
                || (enemyPhysicsFlags[targetSlot] & ENTITY_PHYSICS_PROJECTILE_NOCLIP) != 0
                || unsignedByteAbs(hookshotState.x() - target.x()) >= 0x0C
                || unsignedByteAbs(sourceVisualY
                    - ((target.y() - target.z()) & 0xFF)) >= 0x0C
                || target.spriteVariant() == -1
                || enemyIgnoreHitsCountdown[targetSlot] != 0) {
                continue;
            }

            if (target.type() != ENTITY_IRON_MASK
                || ((roamingEnemyMotion.direction(targetSlot) ^ 0x01)
                    != hookshotState.direction())
                || ironMaskPrivateState2[targetSlot] != 0) {
                continue;
            }

            ironMaskPrivateState2[targetSlot] = 1;
            unmaskedIronMaskMotion.initializeFromMasked(targetSlot,
                roamingEnemyMotion.direction(targetSlot),
                roamingEnemyMotion.speedX(targetSlot), roamingEnemyMotion.speedY(targetSlot));
            spawnIronMasksMask(hookshotState.x(), hookshotState.y(),
                hookshot.spriteVariant() & 0x01, sourceSlot);
            unmasked = true;
        }
        return unmasked;
    }

    private void spawnIronMasksMask(int x, int y, int variant, int sourceHookshotSlot) {
        int freeSlot = findFreeEntitySlot();
        if (freeSlot < 0) {
            return;
        }

        EntitySpriteDefinition definition = spriteDefinitionFor(ENTITY_IRON_MASKS_MASK);
        int selectedVariant = definition.supported()
            ? Math.min(variant & 0x01, definition.variantCount() - 1) : -1;
        RoomEntity mask = new RoomEntity(freeSlot, -1, ENTITY_IRON_MASKS_MASK,
            x & 0xFF, y & 0xFF, EntityStatus.ACTIVE, definition, selectedVariant,
            0, 0, 0);
        slots[freeSlot] = mask;
        baseEntityFlipAttribute[freeSlot] = 0;
        enemyTransitionCountdown[freeSlot] = 0;
        enemyStunnedCountdown[freeSlot] = 0;
        enemyHealth[freeSlot] = initialHealth(ENTITY_IRON_MASKS_MASK);
        enemyFlashCountdown[freeSlot] = 0;
        enemyIgnoreHitsCountdown[freeSlot] = 1;
        enemyPhysicsFlags[freeSlot] = IRON_MASKS_MASK_INITIAL_PHYSICS_FLAGS;
        entityOptions1Override[freeSlot] = IRON_MASKS_MASK_OPTIONS1;
        dyingCountdown[freeSlot] = 0;
        powerRecoilDeath[freeSlot] = false;
        enemyHitboxFlags[freeSlot] = 0;
        if (indoorRoom) {
            slowTransitionCountdown[freeSlot] = 0x80;
            slowTimerInitialized[freeSlot] = true;
        }
        enemyRecoilMotion.clear(freeSlot);
        dynamicEntitySpawnedThisFrame[freeSlot] = true;
        // The source writes sourceSlot + 1 to private state 5. The current
        // mask handler does not consume that byte, but retaining the value
        // keeps the dynamic entity's provenance ROM-shaped for later pickup
        // and item-interaction work.
        ironMasksMaskSourceHookshotSlot[freeSlot] = (sourceHookshotSlot + 1) & 0xFF;
    }

    private void applyPlayerArrowDamage(RoomEntity arrow, RoomEntity target) {
        applyPlayerProjectileDamage(target, playerArrowBombArrow[arrow.slot()]
            ? DAMAGE_TYPE_BOMB_ARROW : DAMAGE_TYPE_ARROW,
            playerArrowMotion.speedX(arrow.slot()), playerArrowMotion.speedY(arrow.slot()));
    }

    private void applyPlayerProjectileDamage(RoomEntity target, int damageType,
                                             int speedX, int speedY) {
        int targetSlot = target.slot();
        enemyRecoilMotion.configureFromSpeed(targetSlot, speedX, speedY);

        RomEnemyCombatTables.SwordDamageResult damageResult = enemyCombatTables == null
            ? null : enemyCombatTables.resolveAttackDamage(target.type(), damageType);
        int rawDamage = damageResult == null
            ? RoomEntityCombatRules.basicSwordDamage(target.type())
            : damageResult.rawValue();
        if (rawDamage == 0) {
            return;
        }

        int enemyDamage = rawDamage < 0xF0 ? rawDamage : 0;
        int specialAction = rawDamage >= 0xF0 ? rawDamage : -1;
        EntityCombatEvent.SoundChannel secondaryChannel = EntityCombatEvent.SoundChannel.NONE;
        int secondarySoundId = -1;

        if (rawDamage == 0xFE) {
            enemyTransitionCountdown[targetSlot] = 0x60;
            enemyStunnedCountdown[targetSlot] = 0;
            enemyFlashCountdown[targetSlot] = 0;
            enemyIgnoreHitsCountdown[targetSlot] = 0x0A;
            enemyPhysicsFlags[targetSlot] = (enemyPhysicsFlags[targetSlot] + 2) & 0xFF;
            enemyRecoilMotion.clear(targetSlot);
            slots[targetSlot] = withStatus(target, EntityStatus.BURNING);
            secondaryChannel = EntityCombatEvent.SoundChannel.NOISE;
            secondarySoundId = 0x12;
        } else if (rawDamage == 0xFF) {
            enemyTransitionCountdown[targetSlot] = 0;
            enemyStunnedCountdown[targetSlot] = 0xFF;
            enemyFlashCountdown[targetSlot] = 0;
            enemyIgnoreHitsCountdown[targetSlot] = 0x0A;
            enemyRecoilMotion.clear(targetSlot);
            slots[targetSlot] = withStatus(target, EntityStatus.STUNNED);
        } else if (rawDamage == 0xFD) {
            // The fairy-conversion branch is entity-specific in the ROM. The
            // generic table result is still surfaced so the pending special
            // action is not silently changed into numeric damage.
            enemyRecoilMotion.clear(targetSlot);
        } else {
            enemyHealth[targetSlot] = Math.max(0, enemyHealth[targetSlot] - enemyDamage);
            if (enemyHealth[targetSlot] == 0) {
                dyingCountdown[targetSlot] = 0x40;
                powerRecoilDeath[targetSlot] = false;
                slots[targetSlot] = withDeathPresentation(
                    withStatus(target, EntityStatus.DYING), -1, false);
            } else if (isZolGelType(target.type())) {
                zolGelMotion.onSwordHit(targetSlot);
                if (target.type() == ENTITY_ZOL && spriteHandlers != null) {
                    slots[targetSlot] = withDefinition(target,
                        spriteHandlers.forZolSlimeEye(), target.spriteVariant());
                }
            }
            enemyFlashCountdown[targetSlot] = 0x18;
            enemyIgnoreHitsCountdown[targetSlot] = 0x0A;
        }

        pendingEntityEvents.add(new EntityCombatEvent(
            targetSlot, target.type(), 0, false, enemyDamage, specialAction,
            EntityCombatEvent.SoundChannel.JINGLE, 0x03,
            secondaryChannel, secondarySoundId, null));
    }

    private int initialHealth(int type) {
        return enemyCombatTables == null
            ? RoomEntityCombatRules.initialHealth(type)
            : enemyCombatTables.initialHealth(type);
    }

    private int contactDamage(int type) {
        return enemyCombatTables == null
            ? RoomEntityCombatRules.contactDamage(type)
            : enemyCombatTables.contactDamage(type);
    }

    private void advanceHookshotBridgeEntity(int index, RoomEntity entity) {
        HookshotBridgeMotion.State bridgeState = hookshotBridgeMotion.state(entity.slot());
        if (bridgeState == null) {
            clearEntity(entity.slot());
            return;
        }

        HookshotBridgeMotion.ObjectCell cell = HookshotBridgeMotion.objectCell(bridgeState);
        HookshotBridgeMotion.Step step = hookshotBridgeMotion.advance(entity.slot());
        RoomEntity moved = withPositionAndVariant(entity, step.state().x(), step.state().y(),
            entity.spriteVariant());
        RoomEntityObjectSample object = objectQuery == null ? null : objectQuery.sample(moved);
        boolean clear = object != null && object.objectId() != OBJECT_HOOKSHOT_BRIDGE_REPLACEMENT
            && object.physicsFlag() == 0;
        hookshotBridgeUpdates.add(new HookshotBridgeUpdate(
            cell.objectLeft(), cell.objectTop(), bridgeState.direction(), !clear));
        if (clear) {
            clearEntity(entity.slot());
        } else {
            slots[index] = moved;
        }
    }

    /** Unloads a slot and returns the persistent first-eight load-order bit. */
    public int clearEntity(int slot) {
        if (slot < 0 || slot >= slots.length) {
            throw new IllegalArgumentException("Entity slot out of range: " + slot);
        }
        resetEnemyDropState(slot);
        RoomEntity entity = slots[slot];
        bombFinalPresentationPending[slot] = false;
        enemyHitboxFlags[slot] = 0;
        playerArrowBombArrow[slot] = false;
        bombPrivateCountdown1[slot] = 0;
        bombPrivateCountdown3[slot] = 0;
        magicPowderState[slot] = 0;
        magicPowderPrivateState4[slot] = 0;
        musicalNoteInertia[slot] = 0;
        musicalNoteSpeedX[slot] = 0;
        musicalNoteSpeedY[slot] = 0;
        entityUnknownJ[slot] = 0;
        ironMaskPrivateState2[slot] = 0;
        ironMasksMaskSourceHookshotSlot[slot] = 0;
        liftableRockSmashCountdown[slot] = 0;
        liftableRockSmashSourceVariant[slot] = 0;
        liftableRockSmashActive[slot] = false;
        if (latestDroppedBombEntityIndex == slot) {
            latestDroppedBombEntityIndex = -1;
        }
        if (latestShotArrowEntityIndex == slot) {
            latestShotArrowEntityIndex = -1;
        }
        bombiteMotion.clear(slot);
        if (!entity.loaded()) {
            return 0;
        }
        slowTransitionCountdown[slot] = 0;
        slowTimerInitialized[slot] = false;
        enemyTransitionCountdown[slot] = 0;
        enemyStunnedCountdown[slot] = 0;
        dyingCountdown[slot] = 0;
        powerRecoilDeath[slot] = false;
        enemyPhysicsFlags[slot] = 0;
        enemyHealth[slot] = 0;
        enemyFlashCountdown[slot] = 0;
        enemyIgnoreHitsCountdown[slot] = 0;
        chestSpeedY[slot] = 0;
        chestSpeedYAccumulator[slot] = 0;
        chestInertia[slot] = 0;
        chestItemBySlot[slot] = 0;
        entityGroundStatus[slot] = 0;
        fallingTargetX[slot] = 0;
        fallingTargetY[slot] = 0;
        fallingSpeedX[slot] = 0;
        fallingSpeedY[slot] = 0;
        fallingSpeedXAccumulator[slot] = 0;
        fallingSpeedYAccumulator[slot] = 0;
        fallingVisualYOffset[slot] = 0;
        liftedPhase[slot] = 0;
        liftedSourceDirection[slot] = 0;
        liftedStateInitialized[slot] = false;
        thrownDirection[slot] = 0xFF;
        bombDirection[slot] = 0xFF;
        bombPrivateState4[slot] = 0;
        magicPowderState[slot] = 0;
        magicPowderPrivateState4[slot] = 0;
        bombPrivateCountdown1[slot] = 0;
        bombPrivateCountdown3[slot] = 0;
        placedBombMotionInitialized[slot] = false;
        ledgeTransitionTimer[slot] = 0;
        thrownMotionInitialized[slot] = false;
        hookshotChainMotion.clear(slot);
        hookshotBridgeMotion.clear(slot);
        boomerangMotion.clear(slot);
        magicRodFireballMotion.clear(slot);
        swordBeamMotion.clear(slot);
        if (entity.type() == ENTITY_HOOKSHOT_CHAIN) {
            hookshotChainOam = List.of();
        }
        baseEntityFlipAttribute[slot] = 0;
        entityOptions1Override[slot] = -1;
        enemyRecoilMotion.clear(slot);
        colorShellMotion.clear(slot);
        butterflyMotion.clear(slot);
        keeseMotion.clear(slot);
        roamingEnemyMotion.clear(slot);
        unmaskedIronMaskMotion.clear(slot);
        moblinSwordMotion.clear(slot);
        playerArrowMotion.clear(slot);
        tektiteMotion.clear(slot);
        leeverMotion.clear(slot);
        antiFairyMotion.clear(slot);
        sparkMotion.clear(slot);
        zolGelMotion.clear(slot);
        hidingZolMotion.clear(slot);
        spikeTrapMotion.clear(slot);
        pairoddMotion.clear(slot);
        pairoddProjectileMotion.clear(slot);
        enemyProjectileMotion.clear(slot);
        laserMotion.clear(slot);
        waterTektiteMotion.clear(slot);
        fishMotion.clear(slot);
        crowMotion.clear(slot);
        booBuddyMotion.clear(slot);
        stalfosAggressiveMotion.clear(slot);
        stalfosEvasiveMotion.clear(slot);
        gibdoMotion.clear(slot);
        likeLikeMotion.clear(slot);
        goombaMotion.clear(slot);
        snakeMotion.clear(slot);
        wizrobeMotion.clear(slot);
        wizrobeProjectileMotion.clear(slot);
        peaHatMotion.clear(slot);
        armosMotion.clear(slot);
        ghiniMotion.clear(slot);
        hardHatMotion.clear(slot);
        madBomberMotion.clear(slot);
        followingNpcMotion.clear(slot);
        bowWowMotion.clear(slot);
        thrownEntityMotion.clear(slot);
        if (liftedEntitySlot == slot) {
            liftedEntitySlot = -1;
            liftedCarryState = 0;
            liftedEffectiveDirection = 0;
        }
        slots[slot] = RoomEntity.disabled(slot);
        return entity.sourceLoadOrder() >= 0 && entity.sourceLoadOrder() < 8
            ? 1 << entity.sourceLoadOrder() : 0;
    }

    /** Creates the ROM's player-projectile entity type {@code $03}. */
    int spawnHookshotChain(int linkEntityX, int linkEntityY, int linkEntityZ,
                           int romDirection) {
        if (hookshotActive()) {
            return -1;
        }
        int freeSlot = findFreeEntitySlot();
        if (freeSlot < 0) {
            return -1;
        }

        EntitySpriteDefinition definition = spriteDefinitionFor(ENTITY_HOOKSHOT_CHAIN);
        int variant = definition.supported() ? definition.initialVariant() : -1;
        RoomEntity chain = new RoomEntity(freeSlot, -1, ENTITY_HOOKSHOT_CHAIN,
            linkEntityX & 0xFF, linkEntityY & 0xFF, EntityStatus.ACTIVE,
            definition, variant, 0, 0, (linkEntityZ + 1) & 0xFF);
        slots[freeSlot] = chain;
        hookshotChainMotion.initializeSpawn(freeSlot, linkEntityX & 0xFF,
            linkEntityY & 0xFF, linkEntityZ & 0xFF, romDirection);
        enemyPhysicsFlags[freeSlot] = 2 | ENTITY_PHYSICS_HARMLESS
            | ENTITY_PHYSICS_PROJECTILE_NOCLIP;
        enemyHealth[freeSlot] = 0;
        enemyTransitionCountdown[freeSlot] = 0;
        enemyStunnedCountdown[freeSlot] = 0;
        enemyFlashCountdown[freeSlot] = 0;
        enemyIgnoreHitsCountdown[freeSlot] = 0;
        baseEntityFlipAttribute[freeSlot] = 0;
        entityOptions1Override[freeSlot] = ENTITY_OPT1_NO_GROUND_INTERACTION
            | ENTITY_OPT1_EXCLUDED_FROM_KILL_ALL;
        return freeSlot;
    }

    /** Creates the ROM's ordinary player-arrow entity type {@code $00}. */
    int spawnArrow(int linkEntityX, int linkEntityY, int linkEntityZ, int romDirection) {
        return spawnArrow(linkEntityX, linkEntityY, linkEntityZ, romDirection, false);
    }

    int spawnArrow(int linkEntityX, int linkEntityY, int linkEntityZ, int romDirection,
                   boolean pieceOfPower) {
        validateRomDirection(romDirection);
        lastArrowShotPlayedWhoosh = false;
        if (activeProjectileCount() >= 0x02) {
            return -1;
        }
        int freeSlot = findFreeEntitySlot();
        if (freeSlot < 0) {
            return -1;
        }

        EntitySpriteDefinition definition = spriteDefinitionFor(ENTITY_ARROW);
        int variant = romDirection;
        RoomEntity arrow = new RoomEntity(freeSlot, -1, ENTITY_ARROW,
            linkEntityX & 0xFF, linkEntityY & 0xFF, EntityStatus.ACTIVE,
            definition, variant, 0, 0, (linkEntityZ + 1) & 0xFF);
        slots[freeSlot] = arrow;
        playerArrowMotion.clear(freeSlot);
        playerArrowMotion.initializeSpawn(freeSlot, romDirection, pieceOfPower);
        baseEntityFlipAttribute[freeSlot] = 0;
        entityOptions1Override[freeSlot] = ENTITY_OPT1_NO_GROUND_INTERACTION
            | ENTITY_OPT1_EXCLUDED_FROM_KILL_ALL;
        enemyPhysicsFlags[freeSlot] = 2 | ENTITY_PHYSICS_PROJECTILE_NOCLIP;
        enemyHealth[freeSlot] = 0;
        enemyTransitionCountdown[freeSlot] = 0;
        enemyStunnedCountdown[freeSlot] = 0;
        enemyFlashCountdown[freeSlot] = 0;
        enemyIgnoreHitsCountdown[freeSlot] = 1;
        dyingCountdown[freeSlot] = 0;
        powerRecoilDeath[freeSlot] = false;
        latestShotArrowEntityIndex = freeSlot;
        if (bombArrowCooldown != 0) {
            int bombSlot = latestDroppedBombEntityIndex;
            bombArrowCooldown = 0;
            if (isLoadedEntityOfType(bombSlot, ENTITY_BOMB)) {
                disableEntityWithoutPersistence(bombSlot);
            }
            latestDroppedBombEntityIndex = -1;
            playerArrowBombArrow[freeSlot] = true;
            if (spriteHandlers != null) {
                slots[freeSlot] = withDefinition(slots[freeSlot],
                    spriteHandlers.forBombArrow(), romDirection);
            }
        } else {
            bombArrowCooldown = BOMB_ARROW_COOLDOWN;
            lastArrowShotPlayedWhoosh = true;
        }
        return freeSlot;
    }

    /** Creates the ROM's player-boomerang entity type {@code $01}. */
    int spawnBoomerang(int linkEntityX, int linkEntityY, int linkEntityZ,
                       int romDirection, int pressedButtonsMask) {
        validateRomDirection(romDirection);
        if (activeProjectileCount() != 0) {
            return -1;
        }
        int freeSlot = findFreeEntitySlot();
        if (freeSlot < 0) {
            return -1;
        }

        EntitySpriteDefinition definition = spriteDefinitionFor(ENTITY_BOOMERANG);
        RoomEntity boomerang = new RoomEntity(freeSlot, -1, ENTITY_BOOMERANG,
            linkEntityX & 0xFF, linkEntityY & 0xFF, EntityStatus.ACTIVE,
            definition, romDirection, 0, 0, (linkEntityZ + 1) & 0xFF);
        slots[freeSlot] = boomerang;
        boomerangMotion.clear(freeSlot);
        boomerangMotion.initializeSpawn(freeSlot, romDirection, pressedButtonsMask);
        thrownDirection[freeSlot] = romDirection;
        entityUnknownJ[freeSlot] = 1;
        baseEntityFlipAttribute[freeSlot] = 0;
        entityOptions1Override[freeSlot] = ENTITY_OPT1_NO_GROUND_INTERACTION
            | ENTITY_OPT1_EXCLUDED_FROM_KILL_ALL;
        enemyPhysicsFlags[freeSlot] = 2 | ENTITY_PHYSICS_PROJECTILE_NOCLIP;
        enemyHealth[freeSlot] = 0;
        enemyTransitionCountdown[freeSlot] = 0;
        enemyStunnedCountdown[freeSlot] = 0;
        enemyFlashCountdown[freeSlot] = 0;
        enemyIgnoreHitsCountdown[freeSlot] = 1;
        dyingCountdown[freeSlot] = 0;
        powerRecoilDeath[freeSlot] = false;
        return freeSlot;
    }

    /** Creates the ROM's level-two sword-beam entity type {@code $DF}. */
    int spawnSwordBeam(int linkEntityX, int linkEntityY, int linkEntityZ,
                       int romDirection) {
        validateRomDirection(romDirection);
        if (activeProjectileCount() != 0) {
            return -1;
        }
        int freeSlot = findFreeEntitySlot();
        if (freeSlot < 0) {
            return -1;
        }

        EntitySpriteDefinition definition = spriteDefinitionFor(ENTITY_SWORD_BEAM);
        int variant = definition.supported() ? romDirection : -1;
        RoomEntity beam = new RoomEntity(freeSlot, -1, ENTITY_SWORD_BEAM,
            linkEntityX & 0xFF, linkEntityY & 0xFF, EntityStatus.ACTIVE,
            definition, variant, 0, 0, (linkEntityZ + 1) & 0xFF);
        slots[freeSlot] = beam;
        swordBeamMotion.clear(freeSlot);
        swordBeamMotion.initializeSpawn(freeSlot, romDirection);
        thrownDirection[freeSlot] = romDirection;
        entityUnknownJ[freeSlot] = 1;
        baseEntityFlipAttribute[freeSlot] = 0;
        entityOptions1Override[freeSlot] = ENTITY_OPT1_NO_GROUND_INTERACTION
            | ENTITY_OPT1_EXCLUDED_FROM_KILL_ALL;
        enemyPhysicsFlags[freeSlot] = 2 | ENTITY_PHYSICS_PROJECTILE_NOCLIP;
        enemyHealth[freeSlot] = 0;
        enemyTransitionCountdown[freeSlot] = 0;
        enemyStunnedCountdown[freeSlot] = 0;
        enemyFlashCountdown[freeSlot] = 0;
        enemyIgnoreHitsCountdown[freeSlot] = 1;
        enemyHitboxFlags[freeSlot] = 0;
        dyingCountdown[freeSlot] = 0;
        powerRecoilDeath[freeSlot] = false;
        return freeSlot;
    }

    /** Creates the ROM's Magic Rod fireball entity type {@code $04}. */
    int spawnMagicRodFireball(int linkEntityX, int linkEntityY, int linkEntityZ,
                              int romDirection) {
        validateRomDirection(romDirection);
        if (activeProjectileCount() >= 0x02) {
            return -1;
        }
        int freeSlot = findFreeEntitySlot();
        if (freeSlot < 0) {
            return -1;
        }

        EntitySpriteDefinition definition = spriteDefinitionFor(ENTITY_MAGIC_ROD_FIREBALL);
        int variant = definition.supported() ? definition.initialVariant() : -1;
        RoomEntity fireball = new RoomEntity(freeSlot, -1, ENTITY_MAGIC_ROD_FIREBALL,
            linkEntityX & 0xFF, linkEntityY & 0xFF, EntityStatus.ACTIVE,
            definition, variant, 0, 0, (linkEntityZ + 1) & 0xFF);
        slots[freeSlot] = fireball;
        magicRodFireballMotion.clear(freeSlot);
        magicRodFireballMotion.initializeSpawn(freeSlot, romDirection);
        thrownDirection[freeSlot] = romDirection;
        entityUnknownJ[freeSlot] = 1;
        baseEntityFlipAttribute[freeSlot] = 0;
        entityOptions1Override[freeSlot] = ENTITY_OPT1_NO_GROUND_INTERACTION
            | ENTITY_OPT1_EXCLUDED_FROM_KILL_ALL;
        enemyPhysicsFlags[freeSlot] = 2 | ENTITY_PHYSICS_PROJECTILE_NOCLIP;
        enemyHealth[freeSlot] = 0;
        enemyTransitionCountdown[freeSlot] = 0;
        enemyStunnedCountdown[freeSlot] = 0;
        enemyFlashCountdown[freeSlot] = 0;
        enemyIgnoreHitsCountdown[freeSlot] = 1;
        enemyHitboxFlags[freeSlot] = 0;
        dyingCountdown[freeSlot] = 0;
        powerRecoilDeath[freeSlot] = false;
        return freeSlot;
    }

    /** Creates the ROM's type-$08 Magic Powder sprinkle entity. */
    int spawnMagicPowderSprinkle(int linkEntityX, int linkEntityY, int linkEntityZ,
                                 int romDirection) {
        validateRomDirection(romDirection);
        int freeSlot = findFreeEntitySlot();
        if (freeSlot < 0) {
            return -1;
        }

        int[] xOffsets = {0x0E, -0x0E, 0x00, 0x00};
        int[] yOffsets = {0x00, 0x00, -0x0C, 0x0C};
        EntitySpriteDefinition definition = spriteDefinitionFor(
            ENTITY_MAGIC_POWDER_SPRINKLE);
        int variant = definition.supported() ? definition.initialVariant() : -1;
        RoomEntity sprinkle = new RoomEntity(freeSlot, -1,
            ENTITY_MAGIC_POWDER_SPRINKLE,
            (linkEntityX + xOffsets[romDirection]) & 0xFF,
            (linkEntityY + yOffsets[romDirection]) & 0xFF,
            EntityStatus.ACTIVE, definition, variant, 0, 0, linkEntityZ & 0xFF);
        slots[freeSlot] = sprinkle;
        magicPowderState[freeSlot] = 0;
        enemyTransitionCountdown[freeSlot] = MAGIC_POWDER_TRANSITION_COUNTDOWN;
        slowTransitionCountdown[freeSlot] = 0;
        slowTimerInitialized[freeSlot] = false;
        enemyHitboxFlags[freeSlot] = 0;
        enemyPhysicsFlags[freeSlot] = 0x03 | ENTITY_PHYSICS_HARMLESS
            | ENTITY_PHYSICS_PROJECTILE_NOCLIP;
        enemyHealth[freeSlot] = 0;
        enemyStunnedCountdown[freeSlot] = 0;
        enemyFlashCountdown[freeSlot] = 0;
        enemyIgnoreHitsCountdown[freeSlot] = 0;
        dyingCountdown[freeSlot] = 0;
        powerRecoilDeath[freeSlot] = false;
        baseEntityFlipAttribute[freeSlot] = 0;
        entityOptions1Override[freeSlot] = ENTITY_OPT1_EXCLUDED_FROM_KILL_ALL;
        dynamicEntitySpawnedThisFrame[freeSlot] = true;
        return freeSlot;
    }

    private void spawnMusicalNote(int linkEntityX, int linkEntityY, int animationPhase) {
        int freeSlot = findFreeEntitySlot();
        if (freeSlot < 0) {
            return;
        }

        int phase = animationPhase & 0x01;
        EntitySpriteDefinition definition = spriteDefinitionFor(ENTITY_MUSICAL_NOTE);
        int variant = definition.supported() ? definition.initialVariant() : -1;
        int x = linkEntityX + (phase == 0 ? 0x08 : -0x08);
        RoomEntity note = new RoomEntity(freeSlot, -1, ENTITY_MUSICAL_NOTE,
            x & 0xFF, (linkEntityY - 0x08) & 0xFF, EntityStatus.ACTIVE,
            definition, variant, 0, 0, 0);
        slots[freeSlot] = note;
        musicalNoteInertia[freeSlot] = MUSICAL_NOTE_INITIAL_INERTIA;
        musicalNoteSpeedX[freeSlot] = phase == 0 ? 0x06 : 0x01;
        musicalNoteSpeedY[freeSlot] = MUSICAL_NOTE_SPEED_Y;
        enemyPhysicsFlags[freeSlot] = 0;
        enemyTransitionCountdown[freeSlot] = 0;
        enemyStunnedCountdown[freeSlot] = 0;
        enemyFlashCountdown[freeSlot] = 0;
        enemyIgnoreHitsCountdown[freeSlot] = 0;
        dyingCountdown[freeSlot] = 0;
        powerRecoilDeath[freeSlot] = false;
        baseEntityFlipAttribute[freeSlot] = 0;
        entityOptions1Override[freeSlot] = ENTITY_OPT1_NO_GROUND_INTERACTION;
        dynamicEntitySpawnedThisFrame[freeSlot] = true;
    }

    private RoomEntity advanceMusicalNoteEntity(RoomEntity entity) {
        int slot = entity.slot();
        int inertia = (musicalNoteInertia[slot] - 1) & 0xFF;
        musicalNoteInertia[slot] = inertia;
        if (inertia == 0) {
            clearEntity(slot);
            return null;
        }

        int speedXAdjustment = (inertia & 0x10) != 0 ? -1 : 1;
        if ((inertia & 0x01) == 0) {
            musicalNoteSpeedX[slot] = (musicalNoteSpeedX[slot] + speedXAdjustment) & 0xFF;
        }
        int x = (entity.x() + signedByte(musicalNoteSpeedX[slot])) & 0xFF;
        int y = (entity.y() + signedByte(musicalNoteSpeedY[slot])) & 0xFF;
        return withPositionAndVariant(entity, x, y, entity.spriteVariant());
    }

    int activePlayerArrowCount() {
        int count = 0;
        for (RoomEntity entity : slots) {
            if (entity.loaded() && entity.type() == ENTITY_ARROW) {
                count++;
            }
        }
        return count;
    }

    /** Mirrors the player projectile handlers that feed wActiveProjectileCount. */
    int activeProjectileCount() {
        int count = 0;
        for (RoomEntity entity : slots) {
            if (!entity.loaded()) {
                continue;
            }
            int type = entity.type() & 0xFF;
            if (type == ENTITY_ARROW || type == ENTITY_BOOMERANG
                || type == ENTITY_MAGIC_ROD_FIREBALL || type == ENTITY_SWORD_BEAM) {
                count++;
            }
        }
        return count;
    }

    boolean boomerangActive() {
        for (RoomEntity entity : slots) {
            if (entity.loaded() && entity.type() == ENTITY_BOOMERANG) {
                return true;
            }
        }
        return false;
    }

    int activeBoomerangSlot() {
        for (RoomEntity entity : slots) {
            if (entity.loaded() && entity.type() == ENTITY_BOOMERANG) {
                return entity.slot();
            }
        }
        return -1;
    }

    int boomerangDirection(int slot) {
        return boomerangMotion.direction(slot);
    }

    int boomerangSpeedX(int slot) {
        return boomerangMotion.speedX(slot);
    }

    int boomerangSpeedY(int slot) {
        return boomerangMotion.speedY(slot);
    }

    int boomerangTransitionCountdown(int slot) {
        return boomerangMotion.transitionCountdown(slot);
    }

    int boomerangState(int slot) {
        return boomerangMotion.state(slot);
    }

    int swordBeamState(int slot) {
        return swordBeamMotion.state(slot);
    }

    int magicRodFireballPrivateCountdown1(int slot) {
        return magicRodFireballMotion.privateCountdown1(slot);
    }

    int magicPowderTransitionCountdown(int slot) {
        validateEntitySlot(slot);
        return enemyTransitionCountdown[slot];
    }

    int magicPowderState(int slot) {
        validateEntitySlot(slot);
        return magicPowderState[slot];
    }

    int magicPowderSlowTransitionCountdown(int slot) {
        validateEntitySlot(slot);
        return slowTransitionCountdown[slot];
    }

    void setMagicPowderTransitionCountdownForTest(int slot, int value) {
        validateCountdownTestValue(slot, value);
        if (!isLoadedEntityOfType(slot, ENTITY_MAGIC_POWDER_SPRINKLE)) {
            throw new IllegalArgumentException("Entity slot does not contain Magic Powder: "
                + slot);
        }
        enemyTransitionCountdown[slot] = value;
    }

    void setMagicPowderSlowTransitionCountdownForTest(int slot, int value) {
        validateCountdownTestValue(slot, value);
        if (!isLoadedEntityOfType(slot, ENTITY_MAGIC_POWDER_SPRINKLE)) {
            throw new IllegalArgumentException("Entity slot does not contain Magic Powder: "
                + slot);
        }
        slowTransitionCountdown[slot] = value;
        slowTimerInitialized[slot] = true;
    }

    int swordBeamSpeedX(int slot) {
        return swordBeamMotion.speedX(slot);
    }

    int swordBeamSpeedY(int slot) {
        return swordBeamMotion.speedY(slot);
    }

    int playerArrowDirection(int slot) {
        return playerArrowMotion.direction(slot);
    }

    int playerArrowSpeedX(int slot) {
        return playerArrowMotion.speedX(slot);
    }

    int playerArrowSpeedY(int slot) {
        return playerArrowMotion.speedY(slot);
    }

    int playerArrowTransitionCountdown(int slot) {
        return playerArrowMotion.transitionCountdown(slot);
    }

    int bombArrowCooldownForTest() {
        return bombArrowCooldown;
    }

    boolean isBombArrowForTest(int slot) {
        validateEntitySlot(slot);
        return playerArrowBombArrow[slot];
    }

    boolean lastArrowShotPlayedWhoosh() {
        return lastArrowShotPlayedWhoosh;
    }

    boolean lastBombPlacementPlayedBump() {
        return lastBombPlacementPlayedBump;
    }

    /** Creates the ROM's ordinary player bomb entity type {@code $02}. */
    int spawnBomb(int linkEntityX, int linkEntityY, int linkEntityZ, int romDirection) {
        validateRomDirection(romDirection);
        lastBombPlacementPlayedBump = false;
        finalizePendingBombPresentations();
        if (bombActive()) {
            return -1;
        }
        int freeSlot = findFreeEntitySlot();
        if (freeSlot < 0) {
            return -1;
        }

        EntitySpriteDefinition definition = spriteDefinitionFor(ENTITY_BOMB);
        int variant = definition.supported() ? definition.initialVariant() : -1;
        RoomEntity bomb = new RoomEntity(freeSlot, -1, ENTITY_BOMB,
            linkEntityX & 0xFF, linkEntityY & 0xFF, EntityStatus.ACTIVE,
            definition, variant, 0, 0, (linkEntityZ + 1) & 0xFF);
        slots[freeSlot] = bomb;
        bombDirection[freeSlot] = romDirection;
        thrownEntityMotion.startPlacedBomb(freeSlot, romDirection);
        placedBombMotionInitialized[freeSlot] = true;
        playerArrowBombArrow[freeSlot] = false;
        bombFinalPresentationPending[freeSlot] = false;
        bombPrivateState4[freeSlot] = 0;
        bombPrivateCountdown1[freeSlot] = 0x10;
        bombPrivateCountdown3[freeSlot] = 0;
        enemyHitboxFlags[freeSlot] = 0;
        enemyTransitionCountdown[freeSlot] = BombMotion.INITIAL_COUNTDOWN;
        enemyStunnedCountdown[freeSlot] = 0;
        enemyHealth[freeSlot] = 0;
        enemyFlashCountdown[freeSlot] = 0;
        enemyIgnoreHitsCountdown[freeSlot] = 0;
        enemyPhysicsFlags[freeSlot] = BOMB_INITIAL_PHYSICS_FLAGS;
        entityGroundStatus[freeSlot] = 0;
        baseEntityFlipAttribute[freeSlot] = 0;
        entityOptions1Override[freeSlot] = BOMB_OPTIONS1;
        if (bombArrowCooldown != 0) {
            bombArrowCooldown = 0;
            int arrowSlot = latestShotArrowEntityIndex;
            if (isLoadedEntityOfType(arrowSlot, ENTITY_ARROW)) {
                markArrowAsBombArrow(arrowSlot);
            }
        } else {
            bombArrowCooldown = BOMB_ARROW_COOLDOWN;
            latestDroppedBombEntityIndex = freeSlot;
            applyInitialBombArrowCandidateState(freeSlot, romDirection);
        }
        return freeSlot;
    }

    /** Creates the source's type-$02 enemy bomb with privateState4 set to $01. */
    int spawnEnemyBomb(int entityX, int entityY, int entityZ, int transitionCountdown) {
        return spawnEnemyBomb(entityX, entityY, entityZ, transitionCountdown,
            0, 0, 0, false);
    }

    /** Creates a Bomber-spawned type-$02 enemy bomb with its source speeds. */
    private int spawnEnemyBomb(int entityX, int entityY, int entityZ,
                               int transitionCountdown, int speedX, int speedY,
                               int speedZ) {
        return spawnEnemyBomb(entityX, entityY, entityZ, transitionCountdown,
            speedX, speedY, speedZ, true);
    }

    private int spawnEnemyBomb(int entityX, int entityY, int entityZ,
                               int transitionCountdown, int speedX, int speedY,
                               int speedZ, boolean initializeMotion) {
        validateByte(entityX, "Enemy bomb X");
        validateByte(entityY, "Enemy bomb Y");
        validateByte(entityZ, "Enemy bomb Z");
        validateByte(transitionCountdown, "Enemy bomb transition countdown");
        validateByte(speedX, "Enemy bomb X speed");
        validateByte(speedY, "Enemy bomb Y speed");
        validateByte(speedZ, "Enemy bomb Z speed");
        int freeSlot = findFreeEntitySlot();
        if (freeSlot < 0) {
            return -1;
        }

        EntitySpriteDefinition definition = spriteDefinitionFor(ENTITY_BOMB);
        int variant = definition.supported() ? definition.initialVariant() : -1;
        RoomEntity bomb = new RoomEntity(freeSlot, -1, ENTITY_BOMB,
            entityX, entityY, EntityStatus.ACTIVE, definition, variant, 0, 0, entityZ);
        slots[freeSlot] = bomb;
        bombDirection[freeSlot] = 0xFF;
        playerArrowBombArrow[freeSlot] = false;
        bombFinalPresentationPending[freeSlot] = false;
        bombPrivateState4[freeSlot] = 0x01;
        bombPrivateCountdown1[freeSlot] = 0;
        bombPrivateCountdown3[freeSlot] = 0;
        placedBombMotionInitialized[freeSlot] = false;
        enemyBombMotionInitialized[freeSlot] = initializeMotion;
        if (initializeMotion) {
            thrownEntityMotion.startEnemyBomb(freeSlot, speedX, speedY, speedZ);
        } else {
            thrownEntityMotion.clear(freeSlot);
        }
        thrownMotionInitialized[freeSlot] = false;
        enemyHitboxFlags[freeSlot] = 0;
        enemyTransitionCountdown[freeSlot] = transitionCountdown;
        enemyStunnedCountdown[freeSlot] = 0;
        enemyHealth[freeSlot] = initialHealth(ENTITY_BOMB);
        enemyFlashCountdown[freeSlot] = 0;
        enemyIgnoreHitsCountdown[freeSlot] = initializeMotion ? 1 : 0;
        enemyPhysicsFlags[freeSlot] = BOMB_INITIAL_PHYSICS_FLAGS;
        entityGroundStatus[freeSlot] = 0;
        baseEntityFlipAttribute[freeSlot] = 0;
        entityOptions1Override[freeSlot] = BOMB_OPTIONS1;
        dynamicEntitySpawnedThisFrame[freeSlot] = true;
        return freeSlot;
    }

    /** Creates the temporary type-$05 entity used by bombed bushes, grass, and pots. */
    int spawnLiftableRockSmash(int entityX, int entityY, int sourceSpriteVariant) {
        if (sourceSpriteVariant != LIFTABLE_ROCK_SMASH_MODE_ROCK
            && sourceSpriteVariant != LIFTABLE_ROCK_SMASH_MODE_BUSH
            && sourceSpriteVariant != LIFTABLE_ROCK_SMASH_MODE_GRASS) {
            throw new IllegalArgumentException("Liftable-rock source variant must be 0, 1, or FF");
        }
        boolean rock = sourceSpriteVariant == LIFTABLE_ROCK_SMASH_MODE_ROCK;
        int initialCountdown = rock ? 0x0F : 0x1F;
        return spawnLiftableRockAnimation(entityX, entityY, sourceSpriteVariant,
            initialCountdown, LIFTABLE_ROCK_SMASH_PHYSICS_FLAGS, rock ? 0x09 : 0x05);
    }

    /** Creates one of the four silent type-$05 rubble entities from bank-$36. */
    int spawnLiftableRockRubble(int entityX, int entityY) {
        return spawnLiftableRockAnimation(entityX, entityY, LIFTABLE_ROCK_SMASH_MODE_ROCK,
            0x0F, LIFTABLE_ROCK_RUBBLE_PHYSICS_FLAGS, -1);
    }

    private int spawnLiftableRockAnimation(int entityX, int entityY, int sourceSpriteVariant,
                                           int initialCountdown, int physicsFlags, int soundId) {
        int freeSlot = findFreeEntitySlot();
        if (freeSlot < 0) {
            return -1;
        }

        EntitySpriteDefinition definition = spriteDefinitionFor(ENTITY_LIFTABLE_ROCK);
        boolean swampLeaves = !indoorRoom && spriteSelection != null
            && spriteSelection.roomTable() == EntityRoomLoader.RoomTable.OVERWORLD
            && spriteSelection.roomId() == 0x32;
        int variant = definition.supported()
            ? liftableRockSmashVariant(sourceSpriteVariant, swampLeaves, initialCountdown) : -1;
        RoomEntity smash = new RoomEntity(freeSlot, -1, ENTITY_LIFTABLE_ROCK,
            entityX & 0xFF, entityY & 0xFF, EntityStatus.ACTIVE,
            definition, variant, 0, 0, 0);
        slots[freeSlot] = smash;
        liftableRockSmashActive[freeSlot] = true;
        liftableRockSmashCountdown[freeSlot] = initialCountdown;
        liftableRockSmashSourceVariant[freeSlot] = sourceSpriteVariant;
        enemyPhysicsFlags[freeSlot] = physicsFlags;
        enemyHitboxFlags[freeSlot] = 0;
        enemyHealth[freeSlot] = initialHealth(ENTITY_LIFTABLE_ROCK);
        enemyIgnoreHitsCountdown[freeSlot] = 1;
        enemyTransitionCountdown[freeSlot] = 0;
        enemyStunnedCountdown[freeSlot] = 0;
        enemyFlashCountdown[freeSlot] = 0;
        enemyRecoilMotion.clear(freeSlot);
        entityOptions1Override[freeSlot] = BOMB_OPTIONS1;
        if (soundId >= 0) {
            pendingEntityEvents.add(new EntityCombatEvent(
                freeSlot, ENTITY_LIFTABLE_ROCK, 0, false,
                EntityCombatEvent.SoundChannel.NOISE, soundId));
        }
        dynamicEntitySpawnedThisFrame[freeSlot] = true;
        return freeSlot;
    }

    private int spawnBombFromBombArrow(RoomEntity arrow) {
        int freeSlot = findFreeEntitySlot();
        if (freeSlot < 0) {
            return -1;
        }

        EntitySpriteDefinition definition = spriteDefinitionFor(ENTITY_BOMB);
        int variant = definition.supported() ? definition.initialVariant() : -1;
        RoomEntity bomb = new RoomEntity(freeSlot, -1, ENTITY_BOMB,
            arrow.x(), arrow.y(), EntityStatus.ACTIVE, definition, variant,
            0, 0, arrow.z());
        slots[freeSlot] = bomb;
        bombDirection[freeSlot] = playerArrowMotion.direction(arrow.slot());
        bombFinalPresentationPending[freeSlot] = false;
        bombPrivateState4[freeSlot] = 0;
        bombPrivateCountdown1[freeSlot] = 0;
        bombPrivateCountdown3[freeSlot] = 0;
        enemyHitboxFlags[freeSlot] = 0;
        enemyTransitionCountdown[freeSlot] = BOMB_ARROW_EXPLOSION_COUNTDOWN;
        enemyStunnedCountdown[freeSlot] = 0;
        enemyHealth[freeSlot] = 0;
        enemyFlashCountdown[freeSlot] = 0;
        enemyIgnoreHitsCountdown[freeSlot] = 0;
        enemyPhysicsFlags[freeSlot] = BOMB_INITIAL_PHYSICS_FLAGS;
        entityGroundStatus[freeSlot] = 0;
        baseEntityFlipAttribute[freeSlot] = 0;
        entityOptions1Override[freeSlot] = BOMB_OPTIONS1;
        placedBombMotionInitialized[freeSlot] = true;
        thrownEntityMotion.clear(freeSlot);
        dynamicEntitySpawnedThisFrame[freeSlot] = true;
        return freeSlot;
    }

    private void applyInitialBombArrowCandidateState(int slot, int romDirection) {
        int x = slots[slot].x() + bombArrowXOffset(romDirection);
        int y = slots[slot].y() + bombArrowYOffset(romDirection);
        slots[slot] = withPositionAndVariant(slots[slot], x, y,
            slots[slot].spriteVariant());
        bombPrivateCountdown3[slot] = 0x03;
        lastBombPlacementPlayedBump = !groundInteractionSideScrolling;
        if (groundInteractionSideScrolling) {
            slots[slot] = withZ(slots[slot], 0);
        }
        thrownEntityMotion.clear(slot);
        // ConvertToBombArrowIfNeeded zeros the projectile speeds. Keep the
        // motion state initialized so the ordinary placed-bomb bridge does
        // not restart SpawnPlayerProjectile's directional speeds.
        placedBombMotionInitialized[slot] = true;
    }

    private void markArrowAsBombArrow(int slot) {
        playerArrowBombArrow[slot] = true;
        RoomEntity arrow = slots[slot];
        if (spriteHandlers != null && arrow.loaded() && arrow.type() == ENTITY_ARROW) {
            slots[slot] = withDefinition(arrow, spriteHandlers.forBombArrow(),
                playerArrowMotion.direction(slot));
        }
    }

    private static int bombArrowXOffset(int romDirection) {
        return switch (romDirection) {
            case 0 -> 8;
            case 1 -> -8;
            case 2, 3 -> 0;
            default -> throw new IllegalArgumentException("ROM direction must be between 0 and 3");
        };
    }

    private static int bombArrowYOffset(int romDirection) {
        return switch (romDirection) {
            case 0, 1 -> 0;
            case 2 -> -3;
            case 3 -> 4;
            default -> throw new IllegalArgumentException("ROM direction must be between 0 and 3");
        };
    }

    private boolean isLoadedEntityOfType(int slot, int type) {
        return slot >= 0 && slot < slots.length && slots[slot].loaded()
            && slots[slot].type() == type;
    }

    boolean bombActive() {
        for (RoomEntity entity : slots) {
            if (entity.loaded() && entity.type() == ENTITY_BOMB
                && !bombFinalPresentationPending[entity.slot()]) {
                return true;
            }
        }
        return false;
    }

    int bombDirection(int slot) {
        validateEntitySlot(slot);
        return bombDirection[slot] & 0xFF;
    }

    int bombPrivateCountdown1ForTest(int slot) {
        validateEntitySlot(slot);
        return bombPrivateCountdown1[slot] & 0xFF;
    }

    void setBombTransitionCountdownForTest(int slot, int value) {
        validateCountdownTestValue(slot, value);
        if (!slots[slot].loaded() || slots[slot].type() != ENTITY_BOMB) {
            throw new IllegalArgumentException("Entity slot does not contain a bomb: " + slot);
        }
        enemyTransitionCountdown[slot] = value;
    }

    private boolean spawnHookshotBridge(int bridgeX, int bridgeY, int direction,
                                        int transitionCountdown) {
        if (!indoorRoom) {
            return false;
        }
        int freeSlot = findFreeEntitySlot();
        if (freeSlot < 0) {
            return false;
        }
        EntitySpriteDefinition definition = spriteDefinitionFor(ENTITY_HOOKSHOT_BRIDGE);
        int variant = definition.supported() ? definition.initialVariant() : -1;
        RoomEntity bridge = new RoomEntity(freeSlot, -1, ENTITY_HOOKSHOT_BRIDGE,
            bridgeX & 0xFF, bridgeY & 0xFF, EntityStatus.ACTIVE,
            definition, variant, 0, 0, 0);
        slots[freeSlot] = bridge;
        hookshotBridgeMotion.initializeSpawn(freeSlot, bridge.x(), bridge.y(), direction);
        enemyPhysicsFlags[freeSlot] = ENTITY_PHYSICS_HARMLESS
            | ENTITY_PHYSICS_PROJECTILE_NOCLIP;
        enemyHealth[freeSlot] = 0;
        enemyTransitionCountdown[freeSlot] = transitionCountdown & 0xFF;
        enemyStunnedCountdown[freeSlot] = 0;
        enemyFlashCountdown[freeSlot] = 0;
        enemyIgnoreHitsCountdown[freeSlot] = 0;
        baseEntityFlipAttribute[freeSlot] = 0;
        entityOptions1Override[freeSlot] = ENTITY_OPT1_NO_GROUND_INTERACTION
            | ENTITY_OPT1_EXCLUDED_FROM_KILL_ALL;
        dynamicEntitySpawnedThisFrame[freeSlot] = true;
        return true;
    }

    boolean hookshotActive() {
        return hookshotSlot() >= 0;
    }

    int hookshotSlot() {
        for (int slot = 0; slot < slots.length; slot++) {
            if (hookshotChainMotion.active(slot)) {
                return slot;
            }
        }
        return -1;
    }

    int hookshotTransitionCountdown(int slot) {
        return hookshotChainMotion.transitionCountdown(slot);
    }

    int hookshotEntityState(int slot) {
        HookshotChainMotion.State state = hookshotChainMotion.state(slot);
        return state == null ? 0 : state.entityState();
    }

    boolean hookshotWallCollisionPending(int slot) {
        HookshotChainMotion.State state = hookshotChainMotion.state(slot);
        return state != null && state.wallCollisionPending();
    }

    public RoomEntitySnapshot snapshot() {
        return new RoomEntitySnapshot(Arrays.asList(slots), spriteSelection, spriteTiles,
            groundInteractionSideScrolling, hookshotChainOam, fallingVisualYOffset);
    }

    private void updateHookshotChainOam(int linkEntityX, int linkEntityY, int frameCounter) {
        int slot = hookshotSlot();
        if (slot < 0 || !slots[slot].loaded()) {
            hookshotChainOam = List.of();
            return;
        }
        RoomEntity chain = slots[slot];
        hookshotChainOam = HookshotChainOam.entries(
            chain.x(), chain.y(), linkEntityX, linkEntityY, frameCounter);
    }

    void setSpriteSelection(EntitySpriteSelection selection) {
        spriteSelection = selection;
    }

    void setGroundInteraction(RoomEntityGroundInteraction groundInteraction) {
        this.groundInteraction = groundInteraction == null
            ? (entity, frameCounter, previousGroundStatus, speedZ, sideScrolling) ->
                RoomEntityGroundInteraction.Result.unchanged(entity, 0)
            : groundInteraction;
    }

    void setBackgroundInteraction(RoomEntityBackgroundInteraction backgroundInteraction) {
        this.backgroundInteraction = backgroundInteraction;
    }

    void setObjectQuery(RoomEntityObjectQuery objectQuery) {
        this.objectQuery = objectQuery;
    }

    void setObjectIntersectionQuery(RoomEntityObjectQuery objectIntersectionQuery) {
        this.objectIntersectionQuery = objectIntersectionQuery;
    }

    int groundStatus(int slot) {
        if (slot < 0 || slot >= entityGroundStatus.length) {
            throw new IllegalArgumentException("Entity slot out of range: " + slot);
        }
        return entityGroundStatus[slot];
    }

    void setActionButtonsHeld(boolean actionButtonsHeld) {
        this.actionButtonsHeld = actionButtonsHeld;
    }

    void setLikeLikeLinkInventory(int itemA, int itemB) {
        validateByte(itemA, "Link A inventory slot");
        validateByte(itemB, "Link B inventory slot");
        linkItemA = itemA;
        linkItemB = itemB;
    }

    void setLikeLikeLinkInventoryForTest(int itemA, int itemB) {
        setLikeLikeLinkInventory(itemA, itemB);
    }

    void setPowerBraceletButtonHeld(boolean powerBraceletButtonHeld) {
        this.powerBraceletButtonHeld = powerBraceletButtonHeld;
    }

    /** Supplies the source-prioritized held A/B button for an equipped bomb. */
    void setBombButtonHeld(boolean bombButtonHeld) {
        this.bombButtonHeld = bombButtonHeld;
    }

    void setLiftedLinkC13B(int linkC13B) {
        if ((linkC13B & ~0xFF) != 0) {
            throw new IllegalArgumentException("Lifted Link C13B must be an unsigned byte: "
                + linkC13B);
        }
        this.liftedLinkC13B = linkC13B;
    }

    void setGroundInteractionSideScrolling(boolean sideScrolling) {
        groundInteractionSideScrolling = sideScrolling;
    }

    void setEnemyDropResolver(EnemyDropResolver resolver) {
        enemyDropResolver = resolver;
    }

    void setEnemyDropCounters(EnemyDropResolver.CounterState counters) {
        enemyDropCounters = Objects.requireNonNull(counters, "Drop counters cannot be null");
    }

    void setEnemyDropPlayerState(int maxHearts, int health, boolean activePowerUp) {
        validateEnemyDropByte(maxHearts, "Maximum hearts");
        validateEnemyDropByte(health, "Health");
        enemyDropMaxHearts = maxHearts;
        enemyDropHealth = health;
        enemyDropActivePowerUp = activePowerUp;
    }

    void setEnemyDropBossBattle(boolean bossBattle) {
        enemyDropBossBattle = bossBattle;
    }

    void setDroppedItemForTest(int slot, int itemType) {
        validateEntitySlot(slot);
        validateEnemyDropByte(itemType, "Dropped item");
        droppedItemBySlot[slot] = itemType;
    }

    void setEntityMapId(int mapId) {
        if (mapId < -1 || mapId > 0xFF) {
            throw new IllegalArgumentException("Entity map id must be -1 or an unsigned byte: "
                + mapId);
        }
        entityMapId = mapId;
    }

    void setEntityMapIdForTest(int mapId) {
        setEntityMapId(mapId);
    }

    void setEntityRoomId(int roomId) {
        validateByte(roomId, "Entity room id");
        entityRoomId = roomId;
    }

    void setEntityRoomIdForTest(int roomId) {
        setEntityRoomId(roomId);
    }

    /** Supplies the three player upgrade bytes read by ChestWithItemEntityHandler. */
    void setChestPlayerLevels(int shieldLevel, int swordLevel, int powerBraceletLevel) {
        validateByte(shieldLevel, "Shield level");
        validateByte(swordLevel, "Sword level");
        validateByte(powerBraceletLevel, "Power Bracelet level");
        chestShieldLevel = shieldLevel;
        chestSwordLevel = swordLevel;
        chestPowerBraceletLevel = powerBraceletLevel;
    }

    void setTransitionSequenceCounterForTest(int counter) {
        if (counter < 0 || counter > 0xFF) {
            throw new IllegalArgumentException(
                "Transition sequence counter must be an unsigned byte: " + counter);
        }
        transitionSequenceCounter = counter;
    }

    void setFollowingNpcState(FollowingNpcState state) {
        if (state == null) {
            throw new IllegalArgumentException("Follower state cannot be null");
        }
        followingNpcState = state;
    }

    void setColorShellWorld(ColorShellWorld world) {
        colorShellWorld = world == null ? ColorShellWorld.none() : world;
    }

    void setSwitchBlockAnimationActive(boolean active) {
        switchBlockAnimationActive = active;
    }

    void setSwitchBlockAnimationActiveForTest(boolean active) {
        setSwitchBlockAnimationActive(active);
    }

    boolean consumePendingSwitchBlockAnimationRequest() {
        boolean pending = pendingSwitchBlockAnimationRequest;
        pendingSwitchBlockAnimationRequest = false;
        return pending;
    }

    int consumePendingClearedEntityMask() {
        int pending = pendingClearedEntityMask;
        pendingClearedEntityMask = 0;
        return pending;
    }

    List<EntityCombatEvent> consumePendingEntityEvents() {
        List<EntityCombatEvent> pending = List.copyOf(pendingEntityEvents);
        pendingEntityEvents.clear();
        return pending;
    }

    List<LinkFinalPositionRequest> consumePendingLinkFinalPositionRequests() {
        List<LinkFinalPositionRequest> pending = List.copyOf(pendingLinkFinalPositionRequests);
        pendingLinkFinalPositionRequests.clear();
        return pending;
    }

    List<LinkMotionBlockRequest> consumePendingLinkMotionBlockRequests() {
        List<LinkMotionBlockRequest> pending = List.copyOf(pendingLinkMotionBlockRequests);
        pendingLinkMotionBlockRequests.clear();
        return pending;
    }

    List<ScreenShakeRequest> consumePendingScreenShakeRequests() {
        List<ScreenShakeRequest> pending = List.copyOf(pendingScreenShakeRequests);
        pendingScreenShakeRequests.clear();
        return pending;
    }

    List<ChestRewardEvent> consumePendingChestRewardEvents() {
        List<ChestRewardEvent> pending = List.copyOf(pendingChestRewardEvents);
        pendingChestRewardEvents.clear();
        return pending;
    }

    List<KeyRewardEvent> consumePendingKeyRewardEvents() {
        List<KeyRewardEvent> pending = List.copyOf(pendingKeyRewardEvents);
        pendingKeyRewardEvents.clear();
        return pending;
    }

    List<KeyQuicksandEvent> consumePendingKeyQuicksandEvents() {
        List<KeyQuicksandEvent> pending = List.copyOf(pendingKeyQuicksandEvents);
        pendingKeyQuicksandEvents.clear();
        return pending;
    }

    int consumePendingMusicTrack() {
        int pending = pendingMusicTrack;
        pendingMusicTrack = -1;
        return pending;
    }

    List<LikeLikeEvent> consumePendingLikeLikeEvents() {
        List<LikeLikeEvent> pending = List.copyOf(pendingLikeLikeEvents);
        pendingLikeLikeEvents.clear();
        return pending;
    }

    int likeLikeState(int slot) {
        return likeLikeMotion.state(slot);
    }

    int likeLikePrivateState1(int slot) {
        return likeLikeMotion.privateState1(slot);
    }

    int likeLikeInertia(int slot) {
        return likeLikeMotion.inertia(slot);
    }

    int likeLikeWalkState(int slot) {
        return likeLikeMotion.walkState(slot);
    }

    int likeLikeWalkSpeedY(int slot) {
        return likeLikeMotion.walkSpeedY(slot);
    }

    List<DialogRequest> consumePendingDialogRequests() {
        List<DialogRequest> pending = List.copyOf(pendingDialogRequests);
        pendingDialogRequests.clear();
        return pending;
    }

    List<BombExplosionEvent> consumeBombExplosionEvents() {
        List<BombExplosionEvent> pending = List.copyOf(pendingBombExplosionEvents);
        pendingBombExplosionEvents.clear();
        return pending;
    }

    List<HookshotBridgeUpdate> hookshotBridgeUpdates() {
        return List.copyOf(hookshotBridgeUpdates);
    }

    private static boolean isFollowingNpcType(int type) {
        return type == ENTITY_GHOST || type == ENTITY_ROOSTER
            || type == ENTITY_MARIN_AT_THE_SHORE || type == ENTITY_BOW_WOW;
    }

    private static boolean isGhiniType(int type) {
        return type == ENTITY_HIDING_GHINI || type == ENTITY_GIANT_GHINI
            || type == ENTITY_GHINI;
    }

    private static boolean isHidingGhiniType(int type) {
        return type == ENTITY_HIDING_GHINI || type == ENTITY_GIANT_GHINI;
    }

    private static boolean isSparkType(int type) {
        return type == ENTITY_SPARK_COUNTER_CLOCKWISE || type == ENTITY_SPARK_CLOCKWISE;
    }

    private static boolean isZolGelType(int type) {
        return type == ENTITY_ZOL || type == ENTITY_GEL;
    }

    private static boolean isRoamingEnemyType(int type) {
        return type == ENTITY_OCTOROK || type == ENTITY_MOBLIN || type == ENTITY_IRON_MASK;
    }

    private static boolean usesBank6Recoil(int type) {
        return type == ENTITY_KEESE || type == ENTITY_TEKTITE
            || type == ENTITY_ANTI_FAIRY || type == ENTITY_STALFOS_AGGRESSIVE
            || type == ENTITY_HARDHAT_BEETLE || type == ENTITY_ARMOS_STATUE
            || type == ENTITY_WIZROBE
            || type == ENTITY_CROW
            || type == ENTITY_BOO_BUDDY
            || type == ENTITY_SPARK_COUNTER_CLOCKWISE
            || type == ENTITY_SPARK_CLOCKWISE
            || type == ENTITY_POLS_VOICE
            || type == ENTITY_ZOL || type == ENTITY_GEL
            || type == ENTITY_LIKE_LIKE || type == ENTITY_ARMOS_KNIGHT;
    }

    private static boolean usesSharedRecoil(int type) {
        return type == ENTITY_LEEVER || type == ENTITY_PEAHAT
            || type == ENTITY_WATER_TEKTITE
            || type == ENTITY_STALFOS_EVASIVE
            || type == ENTITY_MOBLIN_SWORD
            || type == ENTITY_BOUNCING_BOMBITE || type == ENTITY_TIMER_BOMBITE
            || type == ENTITY_MAD_BOMBER
            || type == ENTITY_BOMBER
            || type == ENTITY_GOOMBA
            || type == ENTITY_SNAKE
            || isRoamingEnemyType(type) || usesBank6Recoil(type)
            || isGhiniType(type);
    }

    /** Mirrors bank-$03 ApplyLinkCollisionWithEnemy's Goomba stomp branch. */
    private boolean canGoombaStomp(RoomEntity entity, int linkEntityX, int linkEntityY,
                                   boolean linkAirborne, int linkVerticalVelocity,
                                   boolean linkInteractive, int frameCounter) {
        if (entity.type() != ENTITY_GOOMBA || !linkAirborne || !linkInteractive
            || !RoomEntityCombatRules.collisionCadenceMatches(frameCounter, entity.slot())
            || !RoomEntityCombatRules.overlapsLink(entity, linkEntityX, linkEntityY)) {
            return false;
        }

        // Top-down hLinkVelocityZ is negative while descending. The side-view
        // branch instead tests hLinkSpeedY, where a non-negative value means
        // Link is moving downward onto the Goomba.
        return groundInteractionSideScrolling
            ? (linkVerticalVelocity & 0x80) == 0
            : (linkVerticalVelocity & 0x80) != 0;
    }

    private static boolean isBombiteType(int type) {
        return type == ENTITY_BOUNCING_BOMBITE || type == ENTITY_TIMER_BOMBITE;
    }

    private static boolean isEnemyProjectileType(int type) {
        return type == ENTITY_OCTOROK_ROCK || type == ENTITY_MOBLIN_ARROW;
    }

    private static boolean isLaserType(int type) {
        return type == ENTITY_LASER || type == ENTITY_LASER_BEAM;
    }

    private static boolean isDynamicFollowingNpc(RoomEntity entity) {
        return entity.sourceLoadOrder() == -1 && isFollowingNpcType(entity.type());
    }

    private static boolean isBossKeyDropProducer(int entityType) {
        return entityType == ENTITY_MASTER_STALFOS || entityType == ENTITY_DESERT_LANMOLA
            || entityType == ENTITY_ARMOS_KNIGHT;
    }

    private boolean hasNoGroundInteractionOverride(int slot) {
        return entityOptions1Override[slot] >= 0
            && (entityOptions1Override[slot] & ENTITY_OPT1_NO_GROUND_INTERACTION) != 0;
    }

    private boolean hasNoGroundInteraction(RoomEntity entity) {
        return isGhiniType(entity.type()) || entity.type() == ENTITY_MAD_BOMBER
            || entity.type() == ENTITY_WIZROBE_PROJECTILE
            || entity.type() == ENTITY_BOO_BUDDY
            || entity.type() == ENTITY_DROPPABLE_FAIRY
            || hasNoGroundInteractionOverride(entity.slot());
    }

    private RoomEntity applyZolSplit(RoomEntity original, RoomEntity updated,
                                      ZolGelMotion.Split split) {
        EntitySpriteDefinition gelDefinition = spriteDefinitionFor(ENTITY_GEL);
        int gelVariant = gelDefinition.supported() ? 0 : -1;
        int slot = original.slot();
        RoomEntity gel = new RoomEntity(slot, original.sourceLoadOrder(), ENTITY_GEL,
            (split.originalX() - 4) & 0xFF, split.originalY(), EntityStatus.ACTIVE,
            gelDefinition, gelVariant, original.entityFlipAttribute(), original.spriteTileOffset(),
            split.originalZ());
        enemyTransitionCountdown[slot] = 0;
        enemyStunnedCountdown[slot] = 0;
        enemyHealth[slot] = initialHealth(ENTITY_GEL);
        enemyFlashCountdown[slot] = 0;
        enemyIgnoreHitsCountdown[slot] = 0;
        dyingCountdown[slot] = 0;
        powerRecoilDeath[slot] = false;
        enemyRecoilMotion.clear(slot);

        int freeSlot = findFreeEntitySlot();
        if (freeSlot >= 0) {
            baseEntityFlipAttribute[freeSlot] = baseEntityFlipAttribute[original.slot()];
            RoomEntity spawnedGel = new RoomEntity(freeSlot, original.sourceLoadOrder(), ENTITY_GEL,
                (split.originalX() + 8) & 0xFF, split.originalY(), EntityStatus.ACTIVE,
                gelDefinition, gelVariant, original.entityFlipAttribute(), original.spriteTileOffset(),
                split.originalZ());
            slots[freeSlot] = spawnedGel;
            enemyTransitionCountdown[freeSlot] = 0;
            enemyStunnedCountdown[freeSlot] = 0;
            dyingCountdown[freeSlot] = 0;
            powerRecoilDeath[freeSlot] = false;
            enemyHealth[freeSlot] = initialHealth(ENTITY_GEL);
            enemyFlashCountdown[freeSlot] = 0;
            // SpawnNewEntity sets the new entity's ignore-hits countdown to 1.
            enemyIgnoreHitsCountdown[freeSlot] = 1;
            enemyRecoilMotion.clear(freeSlot);
            zolGelMotion.prepareSpawnedGel(freeSlot);
        }
        return gel;
    }

    private void handleTerminalEnemyDeath(RoomEntity entity, IntSupplier randomByteSupplier) {
        boolean swallowedShield = entity.type() == ENTITY_LIKE_LIKE
            && likeLikeMotion.privateState1(entity.slot()) != 0;
        if (entity.sourceLoadOrder() < 0 || (enemyDropResolver == null && !swallowedShield)) {
            disableEntityWithoutPersistence(entity.slot());
            return;
        }

        EnemyDropResolver.Result result;
        if (swallowedShield) {
            result = new EnemyDropResolver.Result(
                ENTITY_SWORD_SHIELD_PICKUP, enemyDropCounters, 0);
        } else {
            EnemyDropResolver.Context context = new EnemyDropResolver.Context(
                entity.type(), enemyDropHealthGroup(entity.type()), droppedItemBySlot[entity.slot()],
                enemyDropMaxHearts, enemyDropHealth, enemyDropBossBattle,
                enemyDropActivePowerUp, groundInteractionSideScrolling, enemyDropCounters,
                randomByteSupplier);
            result = enemyDropResolver.resolve(context);
            enemyDropCounters = result.counters();
        }

        int killIndex = killCount & 0xFF;
        killOrder[killIndex] = entity.sourceLoadOrder() & 0xFF;
        killCount = (killCount + 1) & 0xFF;
        pendingClearedEntityMask |= persistentClearMask(entity);
        if (result.dropped()) {
            spawnEnemyDrop(entity, result.itemType());
        }
        disableEntityWithoutPersistence(entity.slot());
    }

    /**
     * Ports the boss-specific death handlers that eventually create entity
     * {@code $30}. Boss entities stay on their active handler while DYING, so
     * they must not enter the ordinary EnemyDropResolver path.
     */
    private void advanceBossKeyDropProducer(RoomEntity source) {
        int slot = source.slot();
        if (enemyTransitionCountdown[slot] != 0) {
            return;
        }

        int state = bossDeathProducerState[slot];
        if (source.type() == ENTITY_DESERT_LANMOLA) {
            switch (state) {
                case 0 -> {
                    // func_006_5629: transition $60, then private state 2++.
                    bossDeathProducerState[slot] = 1;
                    enemyTransitionCountdown[slot] = 0x60;
                    return;
                }
                case 1 -> {
                    // func_006_563A: transition $CF, then private state 2++.
                    bossDeathProducerState[slot] = 2;
                    enemyTransitionCountdown[slot] = 0xCF;
                    return;
                }
                default -> {
                    // func_006_564B: variant $02, z from hMultiPurpose3,
                    // speedZ $10, and private countdown 1 $10.
                    spawnBossKeyDrop(source, 0x02, source.z(), 0x10, 0x10);
                    disableEntityWithoutPersistence(slot);
                    pendingEntityEvents.add(new EntityCombatEvent(
                        slot, source.type(), 0, false,
                        EntityCombatEvent.SoundChannel.NOISE, 0x13));
                }
            }
            return;
        }

        if (source.type() == ENTITY_ARMOS_KNIGHT) {
            switch (state) {
                case 0 -> {
                    // ArmosKnightPrivateState0Handler: transition $A0 and
                    // the first full flash countdown.
                    bossDeathProducerState[slot] = 1;
                    enemyTransitionCountdown[slot] = 0xA0;
                    enemyFlashCountdown[slot] = 0xFF;
                    return;
                }
                case 1 -> {
                    // ArmosKnightPrivateState1Handler: the second flash
                    // begins only after the first countdown reaches zero.
                    bossDeathProducerState[slot] = 2;
                    enemyTransitionCountdown[slot] = 0xC0;
                    enemyFlashCountdown[slot] = 0xFF;
                    return;
                }
                default -> {
                    // ArmosKnightPrivateState2Handler calls DidKillEnemy;
                    // its handler has already forced the dropped item to the
                    // key drop point and the source load order to $FF.
                    spawnEnemyDrop(source, ENTITY_KEY_DROP_POINT);
                    disableEntityWithoutPersistence(slot);
                    pendingEntityEvents.add(new EntityCombatEvent(
                        slot, source.type(), 0, false,
                        EntityCombatEvent.SoundChannel.NOISE, 0x1A));
                }
            }
            return;
        }

        switch (state) {
            case 0 -> {
                // func_007_7EB6: transition $A0, then private state 2++.
                bossDeathProducerState[slot] = 1;
                enemyTransitionCountdown[slot] = 0xA0;
            }
            case 1 -> {
                // func_007_7EC7: transition $C0, then private state 2++.
                bossDeathProducerState[slot] = 2;
                enemyTransitionCountdown[slot] = 0xC0;
            }
            default -> {
                // func_007_7ED6: ConfigureNewEntity leaves z at zero;
                // the producer supplies speedZ $18 and countdown 1 $20.
                spawnBossKeyDrop(source, 0x00, 0x00, 0x18, 0x20);
                disableEntityWithoutPersistence(slot);
                pendingEntityEvents.add(new EntityCombatEvent(
                    slot, source.type(), 0, false,
                    EntityCombatEvent.SoundChannel.NOISE, 0x1A));
            }
        }
    }

    private void spawnBossKeyDrop(RoomEntity source, int spriteVariant, int z,
                                  int speedZ, int privateCountdown1) {
        int freeSlot = findFreeEntitySlot();
        if (freeSlot < 0) {
            return;
        }

        EntitySpriteDefinition definition = spriteDefinitionFor(ENTITY_KEY_DROP_POINT);
        int variant = definition.supported()
            ? Math.min(spriteVariant, definition.variantCount() - 1) : -1;
        RoomEntity drop = new RoomEntity(freeSlot, -1, ENTITY_KEY_DROP_POINT,
            source.x(), source.y(), EntityStatus.ACTIVE, definition, variant,
            0, 0, z);
        slots[freeSlot] = drop;
        resetEnemyDropState(freeSlot);
        enemyDropMotion.initialize(freeSlot, groundInteractionSideScrolling, speedZ);
        enemyDropActive[freeSlot] = true;
        slowTransitionCountdown[freeSlot] = 0;
        slowTimerInitialized[freeSlot] = false;
        dropPrivateCountdown1[freeSlot] = privateCountdown1;
        dropPrivateCountdown3[freeSlot] = 0;
        enemyTransitionCountdown[freeSlot] = 0;
        enemyStunnedCountdown[freeSlot] = 0;
        dyingCountdown[freeSlot] = 0;
        powerRecoilDeath[freeSlot] = false;
        enemyPhysicsFlags[freeSlot] = initialPhysicsFlags(ENTITY_KEY_DROP_POINT);
        enemyHealth[freeSlot] = initialHealth(ENTITY_KEY_DROP_POINT);
        enemyFlashCountdown[freeSlot] = 0;
        enemyIgnoreHitsCountdown[freeSlot] = 1;
        entityGroundStatus[freeSlot] = 0;
        baseEntityFlipAttribute[freeSlot] = 0;
        entityOptions1Override[freeSlot] = -1;
        enemyRecoilMotion.clear(freeSlot);
        dynamicEntitySpawnedThisFrame[freeSlot] = true;
    }

    private int enemyDropHealthGroup(int entityType) {
        if (enemyCombatTables == null) {
            throw new IllegalStateException(
                "ROM enemy combat tables are required for enemy drop resolution");
        }
        return enemyCombatTables.healthGroup(entityType);
    }

    private void spawnEnemyDrop(RoomEntity source, int itemType) {
        int freeSlot = findFreeEntitySlot();
        if (freeSlot < 0) {
            return;
        }

        EntitySpriteDefinition definition = spriteDefinitionFor(itemType);
        int variant = definition.supported() ? definition.initialVariant() : -1;
        if (itemType == ENTITY_KEY_DROP_POINT && source.type() == ENTITY_ARMOS_KNIGHT) {
            variant = 3;
        }
        RoomEntity drop = new RoomEntity(freeSlot, -1, itemType, source.x(), source.y(),
            EntityStatus.ACTIVE, definition, variant, 0, 0, source.z());
        slots[freeSlot] = drop;
        resetEnemyDropState(freeSlot);
        slowTransitionCountdown[freeSlot] = 0x80;
        slowTimerInitialized[freeSlot] = true;
        enemyTransitionCountdown[freeSlot] = 0;
        enemyStunnedCountdown[freeSlot] = 0;
        dyingCountdown[freeSlot] = 0;
        powerRecoilDeath[freeSlot] = false;
        enemyPhysicsFlags[freeSlot] = initialPhysicsFlags(itemType);
        enemyHealth[freeSlot] = initialHealth(itemType);
        enemyFlashCountdown[freeSlot] = 0;
        enemyIgnoreHitsCountdown[freeSlot] = 1;
        entityGroundStatus[freeSlot] = 0;
        baseEntityFlipAttribute[freeSlot] = 0;
        entityOptions1Override[freeSlot] = -1;
        enemyRecoilMotion.clear(freeSlot);
        dropPrivateCountdown1[freeSlot] = 0x18;
        dropPrivateCountdown3[freeSlot] = 0x03;
        if (itemType == ENTITY_DROPPABLE_FAIRY) {
            fairyMotion.initialize(freeSlot);
        } else {
            enemyDropMotion.initialize(freeSlot, groundInteractionSideScrolling);
            enemyDropActive[freeSlot] = true;
        }
        dynamicEntitySpawnedThisFrame[freeSlot] = true;
    }

    /**
     * Mirrors the shovel's delayed random reward branch in
     * {@code label_002_4C92}. The first random byte is consumed even when the
     * current room is not allowed to produce a reward; the caller supplies
     * that room-specific restriction explicitly.
     */
    int spawnShovelDrop(int objectLeft, int objectTop, int linkEntityX, int linkEntityY,
                        boolean allowDrop) {
        if (defaultRandomByteSupplier == null) {
            throw new IllegalStateException("Shovel drops require a ROM random-byte supplier");
        }
        if ((defaultRandomByteSupplier.getAsInt() & 0x07) != 0 || !allowDrop) {
            return -1;
        }

        int itemType = (defaultRandomByteSupplier.getAsInt() & 0x01) == 0 ? 0x2E : 0x2D;
        int freeSlot = findFreeEntitySlot();
        if (freeSlot < 0) {
            return -1;
        }

        int x = (objectLeft & 0xF0) + 0x08;
        int y = (objectTop & 0xF0) + 0x10;
        EntitySpriteDefinition definition = spriteDefinitionFor(itemType);
        int variant = definition.supported() ? definition.initialVariant() : -1;
        RoomEntity drop = new RoomEntity(freeSlot, -1, itemType, x & 0xFF, y & 0xFF,
            EntityStatus.ACTIVE, definition, variant, 0, 0, 0);
        slots[freeSlot] = drop;
        resetEnemyDropState(freeSlot);
        slowTransitionCountdown[freeSlot] = 0x80;
        slowTimerInitialized[freeSlot] = true;
        enemyTransitionCountdown[freeSlot] = 0;
        enemyStunnedCountdown[freeSlot] = 0;
        dyingCountdown[freeSlot] = 0;
        powerRecoilDeath[freeSlot] = false;
        enemyPhysicsFlags[freeSlot] = initialPhysicsFlags(itemType);
        enemyHealth[freeSlot] = initialHealth(itemType);
        enemyFlashCountdown[freeSlot] = 0;
        enemyIgnoreHitsCountdown[freeSlot] = 1;
        entityGroundStatus[freeSlot] = 0;
        baseEntityFlipAttribute[freeSlot] = 0;
        entityOptions1Override[freeSlot] = -1;
        enemyRecoilMotion.clear(freeSlot);
        dropPrivateCountdown1[freeSlot] = 0x18;
        dropPrivateCountdown3[freeSlot] = 0x03;
        // label_002_4C92 seeds this drop at $20, unlike the common enemy
        // death path's $18 seed.
        enemyDropMotion.initialize(freeSlot, groundInteractionSideScrolling, 0x20);
        enemyDropMotion.initializeAwayFromLink(freeSlot, x & 0xFF, y & 0xFF,
            linkEntityX & 0xFF, linkEntityY & 0xFF, 0x0C);
        enemyDropActive[freeSlot] = true;
        dynamicEntitySpawnedThisFrame[freeSlot] = true;
        return freeSlot;
    }

    /**
     * Mirrors SpawnChestWithItem. The arguments are the intersected object's
     * unaligned left/top coordinates; the source masks them to a room cell
     * and places the entity at (+8,+16).
     */
    int spawnChestWithItem(int objectLeft, int objectTop, int itemType) {
        if (chestContentsTable == null) {
            throw new IllegalStateException("ROM chest contents table is not configured");
        }
        validateByte(objectLeft, "Chest object left");
        validateByte(objectTop, "Chest object top");
        if (itemType < 0 || itemType > ChestContentsTable.CHEST_ZOL) {
            throw new IllegalArgumentException("Chest item variant out of range: " + itemType);
        }
        int freeSlot = findFreeEntitySlot();
        if (freeSlot < 0) {
            return -1;
        }
        EntitySpriteDefinition definition = spriteDefinitionForChest(itemType);
        int spriteVariant = definition.supported() ? itemType : -1;
        int x = (objectLeft & 0xF0) + 0x08;
        int y = (objectTop & 0xF0) + 0x10;
        RoomEntity chest = new RoomEntity(freeSlot, -1, ENTITY_CHEST_WITH_ITEM,
            x & 0xFF, y & 0xFF, EntityStatus.INIT, definition, spriteVariant,
            0, 0, 0);
        slots[freeSlot] = chest;
        baseEntityFlipAttribute[freeSlot] = 0;
        enemyPhysicsFlags[freeSlot] = CHEST_INITIAL_PHYSICS_FLAGS;
        enemyHitboxFlags[freeSlot] = 0;
        enemyHealth[freeSlot] = 0;
        enemyTransitionCountdown[freeSlot] = 0;
        enemyStunnedCountdown[freeSlot] = 0;
        enemyFlashCountdown[freeSlot] = 0;
        enemyIgnoreHitsCountdown[freeSlot] = 0;
        chestSpeedY[freeSlot] = 0;
        chestSpeedYAccumulator[freeSlot] = 0;
        chestInertia[freeSlot] = 0;
        chestItemBySlot[freeSlot] = itemType;
        entityOptions1Override[freeSlot] = -1;
        dynamicEntitySpawnedThisFrame[freeSlot] = false;
        return freeSlot;
    }

    private int findFreeEntitySlot() {
        for (int slot = slots.length - 1; slot >= 0; slot--) {
            if (!slots[slot].loaded()) {
                // Dynamic entity paths reuse this slot directly rather than
                // necessarily passing through clearEntity first.
                enemyHitboxFlags[slot] = 0;
                return slot;
            }
        }
        return -1;
    }

    private void spawnPairoddProjectile(RoomEntity source, int linkEntityX, int linkEntityY) {
        int freeSlot = findFreeEntitySlot();
        if (freeSlot < 0) {
            return;
        }

        EntitySpriteDefinition projectileDefinition =
            spriteDefinitionFor(ENTITY_PAIRODD_PROJECTILE);
        int projectileVariant = projectileDefinition.supported()
            ? projectileDefinition.initialVariant() : -1;
        RoomEntity projectile = new RoomEntity(freeSlot, -1, ENTITY_PAIRODD_PROJECTILE,
            source.x(), source.y(), EntityStatus.ACTIVE, projectileDefinition,
            projectileVariant, 0, 0, source.z());
        slots[freeSlot] = projectile;
        baseEntityFlipAttribute[freeSlot] = 0;
        enemyTransitionCountdown[freeSlot] = 0;
        enemyStunnedCountdown[freeSlot] = 0;
        enemyHealth[freeSlot] = initialHealth(ENTITY_PAIRODD_PROJECTILE);
        enemyFlashCountdown[freeSlot] = 0;
        enemyIgnoreHitsCountdown[freeSlot] = 1;
        enemyRecoilMotion.clear(freeSlot);
        dyingCountdown[freeSlot] = 0;
        powerRecoilDeath[freeSlot] = false;
        pairoddProjectileMotion.initializeSpawn(freeSlot, source, linkEntityX, linkEntityY);
    }

    private void spawnWizrobeProjectile(RoomEntity source,
                                         WizrobeMotion.ProjectileSpawn spawn) {
        int freeSlot = findFreeEntitySlot();
        if (freeSlot < 0) {
            return;
        }

        EntitySpriteDefinition projectileDefinition =
            spriteDefinitionFor(ENTITY_WIZROBE_PROJECTILE);
        int projectileVariant = projectileDefinition.supported()
            ? spawn.direction() : -1;
        RoomEntity projectile = new RoomEntity(freeSlot, -1, ENTITY_WIZROBE_PROJECTILE,
            spawn.x(), spawn.y(), EntityStatus.ACTIVE, projectileDefinition,
            projectileVariant, 0, 0, source.z());
        slots[freeSlot] = projectile;
        baseEntityFlipAttribute[freeSlot] = 0;
        entityOptions1Override[freeSlot] = WIZROBE_PROJECTILE_OPTIONS1;
        enemyTransitionCountdown[freeSlot] = 0;
        enemyStunnedCountdown[freeSlot] = 0;
        dyingCountdown[freeSlot] = 0;
        powerRecoilDeath[freeSlot] = false;
        enemyPhysicsFlags[freeSlot] = 0x42;
        enemyHealth[freeSlot] = initialHealth(ENTITY_WIZROBE_PROJECTILE);
        enemyFlashCountdown[freeSlot] = 0;
        enemyIgnoreHitsCountdown[freeSlot] = 1;
        enemyRecoilMotion.clear(freeSlot);
        wizrobeProjectileMotion.initializeSpawn(freeSlot, spawn.direction());
        dynamicEntitySpawnedThisFrame[freeSlot] = true;
    }

    private boolean spawnEvasiveClone(RoomEntity source,
                                      StalfosEvasiveMotion.CloneRequest request) {
        int freeSlot = findFreeEntitySlot();
        if (freeSlot < 0) {
            return false;
        }

        EntitySpriteDefinition cloneDefinition = spriteDefinitionForEvasiveState(
            source.spriteDefinition(), 1);
        int cloneVariant = cloneDefinition.supported() ? cloneDefinition.initialVariant() : -1;
        RoomEntity clone = new RoomEntity(freeSlot, -1, ENTITY_STALFOS_EVASIVE,
            request.x(), request.y(), EntityStatus.ACTIVE, cloneDefinition, cloneVariant,
            0, 0, request.z());
        slots[freeSlot] = clone;
        baseEntityFlipAttribute[freeSlot] = 0;
        entityOptions1Override[freeSlot] = EVASIVE_CLONE_OPTIONS;
        enemyTransitionCountdown[freeSlot] = 0;
        enemyStunnedCountdown[freeSlot] = 0;
        dyingCountdown[freeSlot] = 0;
        powerRecoilDeath[freeSlot] = false;
        enemyPhysicsFlags[freeSlot] = EVASIVE_CLONE_PHYSICS_FLAGS;
        enemyHealth[freeSlot] = initialHealth(ENTITY_STALFOS_EVASIVE);
        enemyFlashCountdown[freeSlot] = 0;
        // SpawnNewEntity writes one frame, but the Evasive handler immediately
        // stores register B over it. AnimateEntities keeps B at zero, so this
        // clone enters its fleeing handler with no ignore-hits countdown.
        enemyIgnoreHitsCountdown[freeSlot] = 0;
        enemyRecoilMotion.clear(freeSlot);
        stalfosEvasiveMotion.initializeClone(freeSlot, request.speedX(), request.speedY());
        dynamicEntitySpawnedThisFrame[freeSlot] = true;
        pendingEntityEvents.add(new EntityCombatEvent(
            source.slot(), source.type(), 0, false,
            EntityCombatEvent.SoundChannel.NOISE, 0x0A));
        return true;
    }

    private void spawnEnemyProjectile(RoomEntity source,
                                      RoamingEnemyMotion.LaunchRequest request) {
        // Dynamic projectiles must carry the ROM-decoded display definition;
        // a behavior-only runtime without a catalog can still expose the
        // launch request, but must not insert an unrenderable placeholder.
        if (spriteHandlers == null) {
            return;
        }
        int freeSlot = findFreeEntitySlot();
        if (freeSlot < 0) {
            return;
        }

        int direction = roamingEnemyMotion.direction(source.slot());
        EnemyProjectileMotion.SpawnData spawn = EnemyProjectileMotion.spawnData(
            request.projectileType(), direction);
        EntitySpriteDefinition projectileDefinition = spriteDefinitionFor(request.projectileType());
        int projectileVariant = projectileDefinition.supported()
            ? spawn.initialVariant() : -1;
        RoomEntity projectile = new RoomEntity(freeSlot, -1, request.projectileType(),
            byteValue(source.x() + signedByte(spawn.offsetX())),
            byteValue(source.y() + signedByte(spawn.offsetY())), EntityStatus.ACTIVE,
            projectileDefinition, projectileVariant, 0, 0, source.z());
        slots[freeSlot] = projectile;
        baseEntityFlipAttribute[freeSlot] = 0;
        enemyTransitionCountdown[freeSlot] = 0;
        enemyStunnedCountdown[freeSlot] = 0;
        enemyHealth[freeSlot] = initialHealth(request.projectileType());
        enemyFlashCountdown[freeSlot] = 0;
        enemyIgnoreHitsCountdown[freeSlot] = 1;
        enemyRecoilMotion.clear(freeSlot);
        dyingCountdown[freeSlot] = 0;
        powerRecoilDeath[freeSlot] = false;
        enemyProjectileMotion.initializeSpawn(freeSlot, request.projectileType(), direction);
        enemyProjectileSpawnedThisFrame[freeSlot] = true;
    }

    private void spawnLaserSensor(RoomEntity parent) {
        int freeSlot = findFreeEntitySlot();
        if (freeSlot < 0) {
            return;
        }

        RoomEntity sensor = new RoomEntity(freeSlot, -1, ENTITY_LASER,
            parent.x(), parent.y(), EntityStatus.ACTIVE,
            EntitySpriteDefinition.unsupported(ENTITY_LASER), -1,
            0, 0, parent.z());
        slots[freeSlot] = sensor;
        baseEntityFlipAttribute[freeSlot] = 0;
        enemyTransitionCountdown[freeSlot] = 0;
        enemyStunnedCountdown[freeSlot] = 0;
        enemyHealth[freeSlot] = 0;
        enemyFlashCountdown[freeSlot] = 0;
        enemyIgnoreHitsCountdown[freeSlot] = 1;
        dyingCountdown[freeSlot] = 0;
        powerRecoilDeath[freeSlot] = false;
        laserMotion.initializeSensor(freeSlot, parent.slot(), laserMotion.direction(parent.slot()));
        dynamicEntitySpawnedThisFrame[freeSlot] = true;
    }

    private boolean spawnLaserBeam(RoomEntity parent) {
        int freeSlot = findFreeEntitySlot();
        if (freeSlot < 0) {
            return false;
        }

        RoomEntity beam = new RoomEntity(freeSlot, -1, ENTITY_LASER_BEAM,
            parent.x(), parent.y(), EntityStatus.ACTIVE,
            EntitySpriteDefinition.unsupported(ENTITY_LASER_BEAM), -1,
            0, 0, parent.z());
        slots[freeSlot] = beam;
        baseEntityFlipAttribute[freeSlot] = 0;
        enemyTransitionCountdown[freeSlot] = 0;
        enemyStunnedCountdown[freeSlot] = 0;
        enemyHealth[freeSlot] = 0;
        enemyFlashCountdown[freeSlot] = 0;
        enemyIgnoreHitsCountdown[freeSlot] = 1;
        dyingCountdown[freeSlot] = 0;
        powerRecoilDeath[freeSlot] = false;
        laserMotion.initializeBeam(freeSlot, laserMotion.direction(parent.slot()),
            laserMotion.speedX(parent.slot()), laserMotion.speedY(parent.slot()));
        dynamicEntitySpawnedThisFrame[freeSlot] = true;
        return true;
    }

    private EntitySpriteDefinition spriteDefinitionFor(int entityType) {
        if (spriteHandlers == null) {
            return EntitySpriteDefinition.unsupported(entityType);
        }
        EntityRoomLoader.RoomTable table = spriteSelection == null
            ? (indoorRoom ? EntityRoomLoader.RoomTable.INDOORS_A
                : EntityRoomLoader.RoomTable.OVERWORLD)
            : spriteSelection.roomTable();
        int mapId = spriteSelection == null ? -1 : spriteSelection.roomId();
        return spriteHandlers.forEntityType(entityType, table, mapId);
    }

    private EntitySpriteDefinition spriteDefinitionForChest(int itemType) {
        if (itemType == ChestContentsTable.CHEST_ZOL) {
            return EntitySpriteDefinition.unsupported(ENTITY_CHEST_WITH_ITEM);
        }
        if (spriteHandlers == null) {
            return EntitySpriteDefinition.unsupported(ENTITY_CHEST_WITH_ITEM);
        }
        int roomId = spriteSelection == null ? -1 : spriteSelection.roomId();
        if (entityMapId >= 0 && roomId >= 0) {
            return spriteHandlers.forChestState(entityMapId, roomId, itemType);
        }
        return spriteHandlers.forEntityType(ENTITY_CHEST_WITH_ITEM,
            spriteSelection == null
                ? (indoorRoom ? EntityRoomLoader.RoomTable.INDOORS_A
                    : EntityRoomLoader.RoomTable.OVERWORLD)
                : spriteSelection.roomTable(), entityMapId);
    }

    private void initializeChestEntity(RoomEntity entity) {
        int slot = entity.slot();
        int itemType = chestItemBySlot[slot] & 0xFF;
        chestSpeedY[slot] = 0xFC;
        chestSpeedYAccumulator[slot] = 0;
        chestInertia[slot] = 0;
        pendingEntityEvents.add(new EntityCombatEvent(
            slot, ENTITY_CHEST_WITH_ITEM, 0, false,
            EntityCombatEvent.SoundChannel.NOISE, CHEST_OPEN_NOISE_ID));
        if (itemType < ChestContentsTable.CHEST_MESSAGE) {
            pendingChestRewardEvents.add(new ChestRewardEvent(slot, itemType));
        }
    }

    /** Returns true when the source handler unloaded the chest this frame. */
    private boolean advanceChestEntity(int index, RoomEntity entity) {
        int slot = entity.slot();
        int itemType = chestItemBySlot[slot] & 0xFF;
        if (itemType == ChestContentsTable.CHEST_ZOL) {
            spawnChestZol(entity);
            clearEntity(slot);
            return true;
        }

        int y = addFallingSpeedToPosition(entity.y(), chestSpeedY[slot],
            chestSpeedYAccumulator, slot);
        int inertia = (chestInertia[slot] + 1) & 0xFF;
        chestInertia[slot] = inertia;
        if (inertia == 0x10) {
            chestSpeedY[slot] = 0;
        }
        if (inertia == 0x08 && chestContentsTable != null) {
            int soundValue = chestContentsTable.presentationSoundValue(itemType);
            if (soundValue == CHEST_TREASURE_JINGLE_ID) {
                pendingEntityEvents.add(new EntityCombatEvent(
                    slot, ENTITY_CHEST_WITH_ITEM, 0, false,
                    EntityCombatEvent.SoundChannel.JINGLE, CHEST_TREASURE_JINGLE_ID));
            } else if (soundValue != 0) {
                pendingMusicTrack = soundValue;
            }
        }
        if (inertia == 0x26 && chestContentsTable != null) {
            int roomId = spriteSelection == null ? -1 : spriteSelection.roomId();
            int dialogLow = chestContentsTable.dialogLowIdFor(itemType,
                chestShieldLevel, chestSwordLevel, chestPowerBraceletLevel,
                entityMapId < 0 ? 0 : entityMapId, roomId < 0 ? 0 : roomId);
            int tableId = itemType == ChestContentsTable.CHEST_MESSAGE && roomId == 0x96
                ? 1 : 0;
            pendingDialogRequests.add(new DialogRequest(tableId, dialogLow));
        }
        if (inertia == 0x28) {
            clearEntity(slot);
            return true;
        }
        slots[index] = withPositionAndVariant(entity, entity.x(), y, itemType);
        return false;
    }

    private void spawnChestZol(RoomEntity chest) {
        int freeSlot = findFreeEntitySlot();
        if (freeSlot < 0) {
            return;
        }
        EntitySpriteDefinition definition = spriteDefinitionFor(ENTITY_ZOL);
        int variant = definition.supported() ? definition.initialVariant() : -1;
        RoomEntity zol = new RoomEntity(freeSlot, -1, ENTITY_ZOL, chest.x(), chest.y(),
            EntityStatus.ACTIVE, definition, variant, 0, 0, 0x06);
        slots[freeSlot] = zol;
        zolGelMotion.prepareChestSpawn(freeSlot);
        enemyPhysicsFlags[freeSlot] = initialPhysicsFlags(ENTITY_ZOL);
        enemyHitboxFlags[freeSlot] = 0;
        enemyHealth[freeSlot] = initialHealth(ENTITY_ZOL);
        enemyIgnoreHitsCountdown[freeSlot] = 1;
        dynamicEntitySpawnedThisFrame[freeSlot] = true;
        pendingEntityEvents.add(new EntityCombatEvent(
            freeSlot, ENTITY_ZOL, 0, false,
            EntityCombatEvent.SoundChannel.JINGLE, 0x1D));
    }

    private void finalizePendingBombPresentations() {
        for (int slot = 0; slot < slots.length; slot++) {
            if (bombFinalPresentationPending[slot]) {
                clearEntity(slot);
            }
        }
    }

    private BombMotion.Decision advanceBombEntity(
            int index, RoomEntity entity, List<EntityProjectileEvent> projectileEvents,
            int linkEntityX, int linkEntityY,
            EnemyProjectileCollision.LinkState projectileLinkState) {
        int slot = entity.slot();
        BombMotion.Decision decision = BombMotion.decide(enemyTransitionCountdown[slot] & 0xFF);
        if (decision.playExplosionSound()) {
            pendingEntityEvents.add(new EntityCombatEvent(
                slot, ENTITY_BOMB, 0, false,
                EntityCombatEvent.SoundChannel.NOISE, 0x0C));
        }
        if (decision.countdownOverride().isPresent()) {
            enemyTransitionCountdown[slot] = decision.countdownOverride().getAsInt();
        }
        if (decision.phase() == BombMotion.Phase.EXPLOSION) {
            enemyPhysicsFlags[slot] = (enemyPhysicsFlags[slot] & 0xF0)
                | BOMB_EXPLOSION_PHYSICS_LOW_BITS;
        }

        EntitySpriteDefinition definition = switch (decision.phase()) {
            case NORMAL -> spriteDefinitionFor(ENTITY_BOMB);
            case WARNING -> spriteHandlers == null
                ? EntitySpriteDefinition.unsupported(ENTITY_BOMB)
                : spriteHandlers.forBombRightBeforeExploding();
            case EXPLOSION -> spriteHandlers == null
                ? EntitySpriteDefinition.unsupported(ENTITY_BOMB)
                : spriteHandlers.forBombExplosion();
        };
        int variant = decision.explosionVariant().orElse(0);
        RoomEntity updated = withDefinition(entity, definition, variant);
        slots[index] = updated;
        queueBombExplosionInteractions(updated, decision, projectileEvents,
            linkEntityX, linkEntityY, projectileLinkState);
        if (decision.unloadAfterPresentation()) {
            bombFinalPresentationPending[slot] = true;
        }
        return decision;
    }

    private void advanceLiftableRockSmash(int index, RoomEntity entity, int frameCounter) {
        int slot = entity.slot();
        int countdown = liftableRockSmashCountdown[slot] & 0xFF;
        if (countdown <= 1) {
            // LiftableRockEntityHandler unloads on the countdown-$01 frame,
            // before the display-list helper gets a chance to render it.
            clearEntity(slot);
            return;
        }

        int sourceVariant = liftableRockSmashSourceVariant[slot];
        boolean swampLeaves = !indoorRoom && spriteSelection != null
            && spriteSelection.roomTable() == EntityRoomLoader.RoomTable.OVERWORLD
            && spriteSelection.roomId() == 0x32;
        int variant = liftableRockSmashVariant(sourceVariant, swampLeaves, countdown);
        if (sourceVariant == LIFTABLE_ROCK_SMASH_MODE_GRASS
            && ((frameCounter ^ slot) & 0x01) != 0) {
            // The $FF tall-grass path in func_019_7C50 only emits OAM on
            // alternating frames. -1 is the same hidden-display sentinel
            // consumed by RenderActiveEntitySpritesRect.
            variant = -1;
        }
        slots[index] = withVariant(entity, variant);
    }

    private static int liftableRockSmashVariant(int sourceVariant, boolean swampLeaves,
                                                int countdown) {
        if (sourceVariant == LIFTABLE_ROCK_SMASH_MODE_ROCK) {
            int frame = (countdown & 0x0C) >>> 2;
            return EntitySpriteHandlerCatalog.LIFTABLE_ROCK_SMASHED_ROCK_VARIANT_BASE + frame;
        }
        int frame = (((countdown & 0x1C) ^ 0x1C) >>> 2);
        int base = swampLeaves
            ? EntitySpriteHandlerCatalog.LIFTABLE_ROCK_CUT_LEAVES_SWAMP_VARIANT_BASE
            : EntitySpriteHandlerCatalog.LIFTABLE_ROCK_CUT_LEAVES_VARIANT_BASE;
        return base + frame;
    }

    private void queueBombExplosionInteractions(
            RoomEntity bomb, BombMotion.Decision decision,
            List<EntityProjectileEvent> projectileEvents,
            int linkEntityX, int linkEntityY,
            EnemyProjectileCollision.LinkState projectileLinkState) {
        if (decision.phase() != BombMotion.Phase.EXPLOSION) {
            return;
        }
        int bombSlot = bomb.slot();
        int countdown = enemyTransitionCountdown[bombSlot] & 0xFF;
        // BombExplosionHandler returns before every interaction for Tarin's
        // transformation explosion. All other bomb variants, including
        // enemy bombs, still run the shared destroyable-object checks.
        if (bombPrivateState4[bombSlot] == 0x4C) {
            return;
        }
        if (countdown < 0x0E || countdown > 0x16) {
            return;
        }

        int bombX = bomb.x() & 0xFF;
        int bombVisualY = (bomb.y() - bomb.z()) & 0xFF;
        pendingBombExplosionEvents.add(new BombExplosionEvent(bombSlot,
            BombExplosionEvent.OBJECT_TARGET, bombX, bombVisualY, countdown,
            BombExplosionEvent.DAMAGE_TYPE_BOMB));
        if (countdown != 0x12) {
            return;
        }
        if (bombPrivateState4[bombSlot] != 0) {
            queueEnemyBombLinkCollision(bomb, projectileEvents, linkEntityX, linkEntityY,
                projectileLinkState);
            return;
        }
        swordMoblinAlertingSoundCounter = 0x04;

        for (int targetSlot = slots.length - 1; targetSlot >= 0; targetSlot--) {
            RoomEntity target = slots[targetSlot];
            if (target.status().value() < EntityStatus.ACTIVE.value()
                || (enemyPhysicsFlags[targetSlot]
                    & (ENTITY_PHYSICS_PROJECTILE_NOCLIP | ENTITY_PHYSICS_GRABBABLE)) != 0
                || (enemyHitboxFlags[targetSlot] & HITFLAGS_IGNORE_HITS) != 0
                || !isWithinBombExplosionWindow(bombX, target.x())
                || !isWithinBombExplosionWindow(
                    (target.y() - target.z()) & 0xFF, bombVisualY)) {
                continue;
            }
            pendingBombExplosionEvents.add(new BombExplosionEvent(bombSlot, targetSlot,
                bombX, bombVisualY, countdown, BombExplosionEvent.DAMAGE_TYPE_BOMB));
            applyBombDamage(bomb, target);
        }
    }

    /** Mirrors BombExplosionHandler's separate privateState4 enemy-bomb path. */
    private void queueEnemyBombLinkCollision(
            RoomEntity bomb, List<EntityProjectileEvent> projectileEvents,
            int linkEntityX, int linkEntityY,
            EnemyProjectileCollision.LinkState projectileLinkState) {
        swordMoblinAlertingSoundCounter = 0x04;
        int bombX = bomb.x() & 0xFF;
        int bombY = bomb.y() & 0xFF;
        if (!isWithinBombExplosionWindow(bombX, linkEntityX)
            || !isWithinBombExplosionWindow(bombY, linkEntityY)) {
            return;
        }

        boolean collisionProtected = projectileLinkState.invincibilityCounter() != 0;
        int linkDamage = enemyCombatTables == null
            ? 0x08 : enemyCombatTables.contactDamage(ENTITY_BOMB);
        projectileEvents.add(new EntityProjectileEvent(
            bomb.slot(), ENTITY_BOMB, EntityProjectileEvent.Kind.LINK_DAMAGE, 0,
            collisionProtected ? 0 : linkDamage,
            collisionProtected
                ? EntityProjectileEvent.SoundChannel.NONE
                : EntityProjectileEvent.SoundChannel.WAVE,
            collisionProtected ? -1 : 0x03, false, false, 0, 0,
            (currentLinkSpeedX << 1) & 0xFF, (currentLinkSpeedY << 1) & 0xFF,
            collisionProtected ? 0 : 0x10));
    }

    /**
     * Ports the damage-table part of CheckExplosionInteractionWithEntities.
     * The ROM writes the source-relative length-$30 recoil after the generic
     * damage routine, even when the selected damage-table value is zero or a
     * source-specific special value.
     */
    private void applyBombDamage(RoomEntity bomb, RoomEntity target) {
        int targetSlot = target.slot();
        RomEnemyCombatTables.SwordDamageResult damageResult = enemyCombatTables == null
            ? null : enemyCombatTables.resolveAttackDamage(target.type(), DAMAGE_TYPE_BOMB);
        int rawDamage = damageResult == null
            ? RoomEntityCombatRules.basicSwordDamage(target.type())
            : damageResult.rawValue();

        if (rawDamage != 0) {
            int enemyDamage = rawDamage < 0xF0 ? rawDamage : 0;
            int specialAction = rawDamage >= 0xF0 ? rawDamage : -1;
            EntityCombatEvent.SoundChannel secondaryChannel =
                EntityCombatEvent.SoundChannel.NONE;
            int secondarySoundId = -1;

            if (rawDamage == 0xFE) {
                enemyTransitionCountdown[targetSlot] = 0x60;
                enemyStunnedCountdown[targetSlot] = 0;
                enemyFlashCountdown[targetSlot] = 0;
                enemyIgnoreHitsCountdown[targetSlot] = 0x0A;
                enemyPhysicsFlags[targetSlot] = (enemyPhysicsFlags[targetSlot] + 2) & 0xFF;
                entityOptions1Override[targetSlot] = options1(targetSlot) & 0xC2;
                slots[targetSlot] = withStatus(target, EntityStatus.BURNING);
                secondaryChannel = EntityCombatEvent.SoundChannel.NOISE;
                secondarySoundId = 0x12;
            } else if (rawDamage == 0xFF) {
                enemyTransitionCountdown[targetSlot] = 0;
                enemyStunnedCountdown[targetSlot] = 0xFF;
                enemyFlashCountdown[targetSlot] = 0;
                enemyIgnoreHitsCountdown[targetSlot] = 0x0A;
                slots[targetSlot] = withStatus(target, EntityStatus.STUNNED);
            } else if (rawDamage == 0xFD) {
                // The ROM's FD action is dispatched by the target's own
                // entity-specific branch. Preserve it as a special result;
                // the bomb vector below still applies unconditionally.
            } else if (rawDamage < 0xF0) {
                enemyHealth[targetSlot] = Math.max(0,
                    enemyHealth[targetSlot] - enemyDamage);
                if (enemyHealth[targetSlot] == 0) {
                    // ApplySwordDamagesToEnemy uses $2F for the ordinary
                    // dying countdown; $40 is the Piece-of-Power override.
                    dyingCountdown[targetSlot] = 0x2F;
                    powerRecoilDeath[targetSlot] = false;
                    slots[targetSlot] = withDeathPresentation(
                        withStatus(target, EntityStatus.DYING), -1, false);
                } else {
                    enemyFlashCountdown[targetSlot] = 0x18;
                    enemyIgnoreHitsCountdown[targetSlot] = 0x0A;
                }
            }

            pendingEntityEvents.add(new EntityCombatEvent(
                targetSlot, target.type(), 0, false, enemyDamage, specialAction,
                EntityCombatEvent.SoundChannel.JINGLE, 0x03,
                secondaryChannel, secondarySoundId, null));
        }

        enemyRecoilMotion.configureFromSource(targetSlot,
            bomb.x(), bomb.y(), bomb.z(), target.x(), target.y(), 0x30);
    }

    private static boolean isWithinBombExplosionWindow(int source, int target) {
        return (((((source - target) & 0xFF) + 0x18) & 0xFF) < 0x30);
    }

    private RoomEntity renderLiftedBomb(RoomEntity entity) {
        if (entity.type() != ENTITY_BOMB) {
            return entity;
        }
        enemyFlashCountdown[entity.slot()] = 0;
        return withDefinition(entity, spriteDefinitionFor(ENTITY_BOMB), 0);
    }

    private boolean tryLiftBombIfRequested(int index, BombMotion.Decision decision,
                                            int linkEntityX, int linkEntityY, int linkZ,
                                            int romLinkDirection) {
        if (decision.phase() != BombMotion.Phase.NORMAL || !bombButtonHeld) {
            return false;
        }
        RoomEntity entity = slots[index];
        if (!entity.loaded() || entity.type() != ENTITY_BOMB
            || bombPrivateCountdown1[entity.slot()] != 0
            || !RoomEntityPickupRules.overlapsLink(entity, linkEntityX, linkEntityY)) {
            return false;
        }
        if (!beginLift(entity.slot(), romLinkDirection)) {
            return false;
        }
        slots[index] = advanceLiftedEntity(renderLiftedBomb(slots[index]), linkEntityX,
            linkEntityY, linkZ, romLinkDirection);
        return true;
    }

    /** Mirrors EntityStunnedHandler's recoil/bounce/clear-speed tail for a bomb. */
    private RoomEntity applyBombStunnedPostActiveWork(
            RoomEntity stunned, RoomEntityBackgroundCollision backgroundCollision) {
        int slot = stunned.slot();
        RoomEntity moved = stunned;
        if (enemyRecoilMotion.isActive(slot)) {
            moved = applyEnemyRecoilIfNeeded(moved, backgroundCollision).entity();
        }
        moved = thrownEntityMotion.advance(
            moved, groundInteractionSideScrolling, backgroundCollision).entity();
        thrownEntityMotion.clear(slot);
        thrownMotionInitialized[slot] = false;
        return withStatus(moved, stunned.status());
    }

    private EntitySpriteDefinition spriteDefinitionForEvasiveState(
            EntitySpriteDefinition fallback, int privateState1) {
        if (spriteHandlers == null) {
            return fallback;
        }
        return spriteHandlers.forStalfosEvasiveState(privateState1);
    }

    private RoomEntity refreshColorShellDisplay(RoomEntity entity, EntityStatus status) {
        if (!ColorShellMotion.isColorShellType(entity.type()) || spriteHandlers == null) {
            return entity;
        }
        EntitySpriteDefinition definition = spriteHandlers.forColorShellState(
            entity.type(), colorShellMotion.state(entity.slot()), status);
        return withDefinition(entity, definition, colorShellMotion.spriteVariant(entity.slot()));
    }

    private void finishBurning(RoomEntity entity) {
        int slot = entity.slot();
        enemyTransitionCountdown[slot] = 0;
        if (entity.type() == ENTITY_GIBDO) {
            EntitySpriteDefinition definition = spriteDefinitionFor(ENTITY_STALFOS_EVASIVE);
            RoomEntity stalfos = withType(entity, ENTITY_STALFOS_EVASIVE, definition);
            slots[slot] = withStatus(stalfos, EntityStatus.ACTIVE);
            enemyHealth[slot] = initialHealth(ENTITY_STALFOS_EVASIVE);
            enemyFlashCountdown[slot] = 0;
            enemyIgnoreHitsCountdown[slot] = 0;
            entityOptions1Override[slot] = -1;
            enemyPhysicsFlags[slot] = EVASIVE_PHYSICS_FLAGS;
            dyingCountdown[slot] = 0;
            powerRecoilDeath[slot] = false;
            enemyRecoilMotion.clear(slot);
            stalfosEvasiveMotion.clear(slot);
            return;
        }

        enemyPhysicsFlags[slot] = 0x04;
        dyingCountdown[slot] = 0x1F;
        powerRecoilDeath[slot] = false;
        slots[slot] = withDeathPresentation(
            withStatus(entity, EntityStatus.DYING), 3, false);
        pendingEntityEvents.add(new EntityCombatEvent(
            slot, entity.type(), 0, false,
            EntityCombatEvent.SoundChannel.NOISE, 0x13));
    }

    int slowTransitionCountdown(int slot) {
        if (slot < 0 || slot >= slots.length) {
            throw new IllegalArgumentException("Entity slot out of range: " + slot);
        }
        return slowTransitionCountdown[slot];
    }

    int droppedItemForTest(int slot) {
        validateEntitySlot(slot);
        return droppedItemBySlot[slot];
    }

    int killCount() {
        return killCount;
    }

    int killOrderAt(int index) {
        if (index < 0 || index >= killOrder.length) {
            throw new IllegalArgumentException("Kill-order index out of range: " + index);
        }
        return killOrder[index];
    }

    EnemyDropResolver.CounterState enemyDropCounters() {
        return enemyDropCounters;
    }

    int dropPrivateCountdown1(int slot) {
        validateEntitySlot(slot);
        return dropPrivateCountdown1[slot];
    }

    int dropPrivateCountdown3(int slot) {
        validateEntitySlot(slot);
        return dropPrivateCountdown3[slot];
    }

    int dropSpeedY(int slot) {
        validateEntitySlot(slot);
        return enemyDropMotion.speedY(slot);
    }

    int dropSpeedX(int slot) {
        validateEntitySlot(slot);
        return enemyDropMotion.speedX(slot);
    }

    int dropSpeedZ(int slot) {
        validateEntitySlot(slot);
        return enemyDropMotion.speedZ(slot);
    }

    int fairySpeedX(int slot) {
        validateEntitySlot(slot);
        return fairyMotion.speedX(slot);
    }

    int fairySpeedY(int slot) {
        validateEntitySlot(slot);
        return fairyMotion.speedY(slot);
    }

    int keeseState(int slot) {
        return keeseMotion.state(slot);
    }

    int keeseTransitionCountdown(int slot) {
        return keeseMotion.transitionCountdown(slot);
    }

    int keeseAngle(int slot) {
        return keeseMotion.angle(slot);
    }

    int keeseSpeedX(int slot) {
        return keeseMotion.speedX(slot);
    }

    int keeseSpeedY(int slot) {
        return keeseMotion.speedY(slot);
    }

    int octorokState(int slot) {
        return roamingEnemyMotion.state(slot);
    }

    int octorokTransitionCountdown(int slot) {
        return roamingEnemyMotion.transitionCountdown(slot);
    }

    int octorokDirection(int slot) {
        return roamingEnemyMotion.direction(slot);
    }

    int octorokSpeedX(int slot) {
        return roamingEnemyMotion.speedX(slot);
    }

    int octorokSpeedY(int slot) {
        return roamingEnemyMotion.speedY(slot);
    }

    int ironMaskState(int slot) {
        return roamingEnemyMotion.state(slot);
    }

    int ironMaskTransitionCountdown(int slot) {
        return roamingEnemyMotion.transitionCountdown(slot);
    }

    int ironMaskDirection(int slot) {
        return roamingEnemyMotion.direction(slot);
    }

    int ironMaskSpeedX(int slot) {
        return roamingEnemyMotion.speedX(slot);
    }

    int ironMaskSpeedY(int slot) {
        return roamingEnemyMotion.speedY(slot);
    }

    int ironMaskPrivateState2(int slot) {
        validateEntitySlot(slot);
        return ironMaskPrivateState2[slot];
    }

    int moblinSwordState(int slot) {
        return moblinSwordMotion.state(slot);
    }

    int moblinSwordTransitionCountdown(int slot) {
        return moblinSwordMotion.transitionCountdown(slot);
    }

    int moblinSwordPrivateCountdown1(int slot) {
        return moblinSwordMotion.privateCountdown1(slot);
    }

    int moblinSwordPrivateState3(int slot) {
        return moblinSwordMotion.privateState3(slot);
    }

    int moblinSwordDirection(int slot) {
        return moblinSwordMotion.direction(slot);
    }

    int moblinSwordSpeedX(int slot) {
        return moblinSwordMotion.speedX(slot);
    }

    int swordMoblinAlertingSoundCounter() {
        return swordMoblinAlertingSoundCounter;
    }

    int tektiteState(int slot) {
        return tektiteMotion.state(slot);
    }

    int tektiteTransitionCountdown(int slot) {
        return tektiteMotion.transitionCountdown(slot);
    }

    int tektiteSpeedX(int slot) {
        return tektiteMotion.speedX(slot);
    }

    int tektiteSpeedY(int slot) {
        return tektiteMotion.speedY(slot);
    }

    int pairoddState(int slot) {
        return pairoddMotion.state(slot);
    }

    int pairoddTransitionCountdown(int slot) {
        return pairoddMotion.transitionCountdown(slot);
    }

    int pairoddDirection(int slot) {
        return pairoddMotion.direction(slot);
    }

    int pairoddProjectileSpeedX(int slot) {
        return pairoddProjectileMotion.speedX(slot);
    }

    int pairoddProjectileSpeedY(int slot) {
        return pairoddProjectileMotion.speedY(slot);
    }

    int pairoddProjectileDirection(int slot) {
        return pairoddProjectileMotion.direction(slot);
    }

    int enemyProjectileSpeedX(int slot) {
        return enemyProjectileMotion.speedX(slot);
    }

    int enemyProjectileSpeedY(int slot) {
        return enemyProjectileMotion.speedY(slot);
    }

    int enemyProjectileTransitionCountdown(int slot) {
        return enemyProjectileMotion.transitionCountdown(slot);
    }

    int laserDirection(int slot) {
        return laserMotion.direction(slot);
    }

    int laserParentTransitionCountdown(int slot) {
        return laserMotion.parentTransitionCountdown(slot);
    }

    int laserSpeedX(int slot) {
        return laserMotion.speedX(slot);
    }

    int laserSpeedY(int slot) {
        return laserMotion.speedY(slot);
    }

    void setLaserParentForTest(int slot, int countdown, int newSpeedX, int newSpeedY) {
        laserMotion.setParentForTest(slot, countdown, newSpeedX, newSpeedY);
    }

    void setPairoddProjectileForTest(int slot, int newSpeedX, int newSpeedY, int direction) {
        pairoddProjectileMotion.setForTest(slot, newSpeedX, newSpeedY, direction);
    }

    void setLaserBeamForTest(int slot, int newSpeedX, int newSpeedY, int direction) {
        laserMotion.setBeamForTest(slot, newSpeedX, newSpeedY, direction);
    }

    List<TransientVfxRequest> transientVfxRequests() {
        return List.copyOf(transientVfxRequests);
    }

    List<BoomerangObjectRequest> boomerangObjectRequests() {
        return List.copyOf(boomerangObjectRequests);
    }

    List<MagicRodObjectRequest> magicRodObjectRequests() {
        return List.copyOf(magicRodObjectRequests);
    }

    List<MagicPowderObjectRequest> magicPowderObjectRequests() {
        return List.copyOf(magicPowderObjectRequests);
    }

    int physicsFlags(int slot) {
        if (slot < 0 || slot >= slots.length) {
            throw new IllegalArgumentException("Entity slot out of range: " + slot);
        }
        return enemyPhysicsFlags[slot];
    }

    int tektiteSpeedZ(int slot) {
        return tektiteMotion.speedZ(slot);
    }

    int leeverState(int slot) {
        return leeverMotion.state(slot);
    }

    int leeverTransitionCountdown(int slot) {
        return leeverMotion.transitionCountdown(slot);
    }

    int leeverSpeedX(int slot) {
        return leeverMotion.speedX(slot);
    }

    int leeverSpeedY(int slot) {
        return leeverMotion.speedY(slot);
    }

    int antiFairySpeedX(int slot) {
        return antiFairyMotion.speedX(slot);
    }

    int antiFairySpeedY(int slot) {
        return antiFairyMotion.speedY(slot);
    }

    int sparkPrivateState1(int slot) {
        return sparkMotion.privateState1(slot);
    }

    int sparkPrivateState2(int slot) {
        return sparkMotion.privateState2(slot);
    }

    int sparkTransitionCountdown(int slot) {
        return sparkMotion.transitionCountdown(slot);
    }

    int sparkSpeedX(int slot) {
        return sparkMotion.speedX(slot);
    }

    int sparkSpeedY(int slot) {
        return sparkMotion.speedY(slot);
    }

    int zolState(int slot) {
        return zolGelMotion.state(slot);
    }

    int zolTransitionCountdown(int slot) {
        return zolGelMotion.transitionCountdown(slot);
    }

    int zolSpeedX(int slot) {
        return zolGelMotion.speedX(slot);
    }

    int zolSpeedY(int slot) {
        return zolGelMotion.speedY(slot);
    }

    int zolSpeedZ(int slot) {
        return zolGelMotion.speedZ(slot);
    }

    int zolPrivateCountdown1(int slot) {
        return zolGelMotion.privateCountdown1(slot);
    }

    int hidingZolState(int slot) {
        return hidingZolMotion.state(slot);
    }

    int hidingZolTransitionCountdown(int slot) {
        return hidingZolMotion.transitionCountdown(slot);
    }

    int hidingZolPrivateState1(int slot) {
        return hidingZolMotion.privateState1(slot);
    }

    int hidingZolSpeedX(int slot) {
        return hidingZolMotion.speedX(slot);
    }

    int hidingZolSpeedY(int slot) {
        return hidingZolMotion.speedY(slot);
    }

    int hidingZolSpeedZ(int slot) {
        return hidingZolMotion.speedZ(slot);
    }

    int spikeTrapState(int slot) {
        return spikeTrapMotion.state(slot);
    }

    int spikeTrapTransitionCountdown(int slot) {
        return spikeTrapMotion.transitionCountdown(slot);
    }

    int spikeTrapDirection(int slot) {
        return spikeTrapMotion.direction(slot);
    }

    int spikeTrapSpeedX(int slot) {
        return spikeTrapMotion.speedX(slot);
    }

    int spikeTrapSpeedY(int slot) {
        return spikeTrapMotion.speedY(slot);
    }

    int spikeTrapPrivateState1(int slot) {
        return spikeTrapMotion.privateState1(slot);
    }

    int spikeTrapPrivateState2(int slot) {
        return spikeTrapMotion.privateState2(slot);
    }

    int waterTektiteState(int slot) {
        return waterTektiteMotion.state(slot);
    }

    int waterTektiteTransitionCountdown(int slot) {
        return waterTektiteMotion.transitionCountdown(slot);
    }

    int waterTektiteSpeedX(int slot) {
        return waterTektiteMotion.speedX(slot);
    }

    int waterTektiteSpeedY(int slot) {
        return waterTektiteMotion.speedY(slot);
    }

    int waterTektitePrivateState1(int slot) {
        return waterTektiteMotion.privateState1(slot);
    }

    int waterTektitePrivateState2(int slot) {
        return waterTektiteMotion.privateState2(slot);
    }

    int stalfosState(int slot) {
        return stalfosAggressiveMotion.state(slot);
    }

    int stalfosTransitionCountdown(int slot) {
        return stalfosAggressiveMotion.transitionCountdown(slot);
    }

    int stalfosSpeedX(int slot) {
        return stalfosAggressiveMotion.speedX(slot);
    }

    int stalfosSpeedY(int slot) {
        return stalfosAggressiveMotion.speedY(slot);
    }

    int stalfosSpeedZ(int slot) {
        return stalfosAggressiveMotion.speedZ(slot);
    }

    int evasivePrivateState1(int slot) {
        return stalfosEvasiveMotion.privateState1(slot);
    }

    int evasivePrivateCountdown1(int slot) {
        return stalfosEvasiveMotion.privateCountdown1(slot);
    }

    int evasiveInertia(int slot) {
        return stalfosEvasiveMotion.inertia(slot);
    }

    int evasiveSpeedX(int slot) {
        return stalfosEvasiveMotion.speedX(slot);
    }

    int evasiveSpeedY(int slot) {
        return stalfosEvasiveMotion.speedY(slot);
    }

    int evasiveSpeedZ(int slot) {
        return stalfosEvasiveMotion.speedZ(slot);
    }

    void setEvasivePrivateCountdown1ForTest(int slot, int value) {
        stalfosEvasiveMotion.setPrivateCountdown1ForTest(slot, value);
    }

    void setEvasiveFleeingForTest(int slot, int newSpeedX, int newSpeedY) {
        stalfosEvasiveMotion.setFleeingForTest(slot, newSpeedX, newSpeedY);
    }

    int gibdoState(int slot) {
        return gibdoMotion.state(slot);
    }

    int gibdoSpeedX(int slot) {
        return gibdoMotion.speedX(slot);
    }

    int gibdoSpeedY(int slot) {
        return gibdoMotion.speedY(slot);
    }

    int goombaState(int slot) {
        return goombaMotion.state(slot);
    }

    int goombaTransitionCountdown(int slot) {
        return enemyTransitionCountdown[slot];
    }

    int goombaSpeedX(int slot) {
        return goombaMotion.speedX(slot);
    }

    int goombaSpeedY(int slot) {
        return goombaMotion.speedY(slot);
    }

    int snakeState(int slot) {
        return snakeMotion.state(slot);
    }

    int snakeTransitionCountdown(int slot) {
        return enemyTransitionCountdown[slot];
    }

    int snakePrivateCountdown1(int slot) {
        return snakeMotion.privateCountdown1(slot);
    }

    int snakeSpeedX(int slot) {
        return snakeMotion.speedX(slot);
    }

    int snakeSpeedY(int slot) {
        return snakeMotion.speedY(slot);
    }

    int polsVoiceState(int slot) {
        return polsVoiceMotion.state(slot);
    }

    int polsVoiceTransitionCountdown(int slot) {
        return enemyTransitionCountdown[slot];
    }

    int polsVoiceSpeedX(int slot) {
        return polsVoiceMotion.speedX(slot);
    }

    int polsVoiceSpeedY(int slot) {
        return polsVoiceMotion.speedY(slot);
    }

    int polsVoiceSpeedZ(int slot) {
        return polsVoiceMotion.speedZ(slot);
    }

    int spikedBeetleState(int slot) {
        return spikedBeetleMotion.state(slot);
    }

    int spikedBeetleTransitionCountdown(int slot) {
        return enemyTransitionCountdown[slot];
    }

    int spikedBeetleSpeedX(int slot) {
        return spikedBeetleMotion.speedX(slot);
    }

    int spikedBeetleSpeedY(int slot) {
        return spikedBeetleMotion.speedY(slot);
    }

    int wizrobeState(int slot) {
        return wizrobeMotion.state(slot);
    }

    int wizrobeDirection(int slot) {
        return wizrobeMotion.direction(slot);
    }

    int wizrobePrivateState1(int slot) {
        return wizrobeMotion.privateState1(slot);
    }

    int wizrobePrivateCountdown1(int slot) {
        return wizrobeMotion.privateCountdown1(slot);
    }

    int wizrobeNextSpriteVariant(int slot) {
        return wizrobeMotion.nextSpriteVariant(slot);
    }

    void setWizrobeStateForTest(int slot, int state, int transitionCountdown,
                                int privateState1, int privateCountdown1, int direction) {
        validateEntitySlot(slot);
        validateCountdownTestValue(slot, transitionCountdown);
        wizrobeMotion.setForTest(slot, state, transitionCountdown, privateState1,
            privateCountdown1, direction, slots[slot].spriteVariant());
        enemyTransitionCountdown[slot] = transitionCountdown;
        enemyPhysicsFlags[slot] = 0x42;
    }

    int wizrobeProjectileSpeedX(int slot) {
        return wizrobeProjectileMotion.speedX(slot);
    }

    int wizrobeProjectileSpeedY(int slot) {
        return wizrobeProjectileMotion.speedY(slot);
    }

    int wizrobeProjectileDirection(int slot) {
        return wizrobeProjectileMotion.direction(slot);
    }

    void setWizrobeProjectileForTest(int slot, int newSpeedX, int newSpeedY,
                                     int direction) {
        wizrobeProjectileMotion.setForTest(slot, newSpeedX, newSpeedY, direction);
    }

    int peaHatState(int slot) {
        return peaHatMotion.state(slot);
    }

    int peaHatSlowTransitionCountdown(int slot) {
        return peaHatMotion.slowTransitionCountdown(slot);
    }

    int peaHatPrivateState1(int slot) {
        return peaHatMotion.privateState1(slot);
    }

    int peaHatPrivateState2(int slot) {
        return peaHatMotion.privateState2(slot);
    }

    int peaHatPrivateState3(int slot) {
        return peaHatMotion.privateState3(slot);
    }

    int peaHatPrivateState4(int slot) {
        return peaHatMotion.privateState4(slot);
    }

    int peaHatSpeedX(int slot) {
        return peaHatMotion.speedX(slot);
    }

    int peaHatSpeedY(int slot) {
        return peaHatMotion.speedY(slot);
    }

    int armosState(int slot) {
        return armosMotion.state(slot);
    }

    int armosTransitionCountdown(int slot) {
        return armosMotion.transitionCountdown(slot);
    }

    int armosSpeedX(int slot) {
        return armosMotion.speedX(slot);
    }

    int armosSpeedY(int slot) {
        return armosMotion.speedY(slot);
    }

    int armosKnightState(int slot) {
        return armosKnightMotion.state(slot);
    }

    int armosKnightTransitionCountdown(int slot) {
        return transitionCountdown(slot);
    }

    int armosKnightSpeedX(int slot) {
        return armosKnightMotion.speedX(slot);
    }

    int armosKnightSpeedY(int slot) {
        return armosKnightMotion.speedY(slot);
    }

    int armosKnightSpeedZ(int slot) {
        return armosKnightMotion.speedZ(slot);
    }

    int armosKnightPrivateCountdown1(int slot) {
        return armosKnightMotion.privateCountdown1(slot);
    }

    int armosKnightInertia(int slot) {
        return armosKnightMotion.inertia(slot);
    }

    int ghiniTransitionCountdown(int slot) {
        return ghiniMotion.transitionCountdown(slot);
    }

    int ghiniPrivateCountdown1(int slot) {
        return ghiniMotion.privateCountdown1(slot);
    }

    int ghiniTargetXDirection(int slot) {
        return ghiniMotion.targetXDirection(slot);
    }

    int ghiniTargetYDirection(int slot) {
        return ghiniMotion.targetYDirection(slot);
    }

    int ghiniSpeedX(int slot) {
        return ghiniMotion.speedX(slot);
    }

    int ghiniSpeedY(int slot) {
        return ghiniMotion.speedY(slot);
    }

    int hardHatSpeedX(int slot) {
        return hardHatMotion.speedX(slot);
    }

    int hardHatSpeedY(int slot) {
        return hardHatMotion.speedY(slot);
    }

    int butterflyPrivateStateX(int slot) {
        return butterflyMotion.privateStateX(slot);
    }

    int butterflyPrivateStateY(int slot) {
        return butterflyMotion.privateStateY(slot);
    }

    int transitionCountdown(int slot) {
        if (slot < 0 || slot >= slots.length) {
            throw new IllegalArgumentException("Entity slot out of range: " + slot);
        }
        return enemyTransitionCountdown[slot];
    }

    int stunnedCountdown(int slot) {
        if (slot < 0 || slot >= slots.length) {
            throw new IllegalArgumentException("Entity slot out of range: " + slot);
        }
        return enemyStunnedCountdown[slot];
    }

    int dyingCountdown(int slot) {
        if (slot < 0 || slot >= slots.length) {
            throw new IllegalArgumentException("Entity slot out of range: " + slot);
        }
        return dyingCountdown[slot];
    }

    boolean powerRecoilDeath(int slot) {
        if (slot < 0 || slot >= slots.length) {
            throw new IllegalArgumentException("Entity slot out of range: " + slot);
        }
        return powerRecoilDeath[slot];
    }

    int enemyHealth(int slot) {
        if (slot < 0 || slot >= slots.length) {
            throw new IllegalArgumentException("Entity slot out of range: " + slot);
        }
        return enemyHealth[slot];
    }

    int enemyFlashCountdown(int slot) {
        if (slot < 0 || slot >= slots.length) {
            throw new IllegalArgumentException("Entity slot out of range: " + slot);
        }
        return enemyFlashCountdown[slot];
    }

    int enemyIgnoreHitsCountdown(int slot) {
        if (slot < 0 || slot >= slots.length) {
            throw new IllegalArgumentException("Entity slot out of range: " + slot);
        }
        return enemyIgnoreHitsCountdown[slot];
    }

    int enemyRecoilSpeedXForTest(int slot) {
        validateEntitySlot(slot);
        return enemyRecoilMotion.recoilSpeedX(slot);
    }

    int enemyRecoilSpeedYForTest(int slot) {
        validateEntitySlot(slot);
        return enemyRecoilMotion.recoilSpeedY(slot);
    }

    int thrownDirection(int slot) {
        validateEntitySlot(slot);
        return thrownDirection[slot] & 0xFF;
    }

    int ledgeTransitionTimer(int slot) {
        validateEntitySlot(slot);
        return ledgeTransitionTimer[slot] & 0xFF;
    }

    int fallingTargetX(int slot) {
        if (slot < 0 || slot >= slots.length) {
            throw new IllegalArgumentException("Entity slot out of range: " + slot);
        }
        return fallingTargetX[slot];
    }

    int fallingTargetY(int slot) {
        if (slot < 0 || slot >= slots.length) {
            throw new IllegalArgumentException("Entity slot out of range: " + slot);
        }
        return fallingTargetY[slot];
    }

    int fallingVisualYOffset(int slot) {
        if (slot < 0 || slot >= slots.length) {
            throw new IllegalArgumentException("Entity slot out of range: " + slot);
        }
        return fallingVisualYOffset[slot];
    }

    void setEnemyIgnoreHitsCountdownForTest(int slot, int value) {
        validateCountdownTestValue(slot, value);
        enemyIgnoreHitsCountdown[slot] = value;
    }

    void setOcarinaPlayback(int countdown, int songFlags, int selectedSong) {
        setOcarinaPlayback(countdown, songFlags, selectedSong, 0, 0);
    }

    void setOcarinaPlayback(int countdown, int songFlags, int selectedSong,
                            int animationCounter, int animationPhase) {
        validateByte(countdown, "Ocarina playback countdown");
        validateByte(songFlags, "Ocarina song flags");
        validateByte(selectedSong, "Selected Ocarina song");
        validateByte(animationCounter, "Ocarina animation counter");
        if (animationPhase < 0 || animationPhase > 1) {
            throw new IllegalArgumentException("Ocarina animation phase must be 0 or 1");
        }
        linkPlayingOcarinaCountdown = countdown;
        ocarinaSongFlags = songFlags;
        selectedSongIndex = selectedSong;
        ocarinaAnimationCounter = animationCounter;
        ocarinaAnimationPhase = animationPhase;
    }

    void setOcarinaPlaybackForTest(int countdown, int songFlags, int selectedSong) {
        setOcarinaPlayback(countdown, songFlags, selectedSong);
    }

    void setOcarinaAnimationForTest(int animationCounter, int animationPhase) {
        validateByte(animationCounter, "Ocarina animation counter");
        if (animationPhase < 0 || animationPhase > 1) {
            throw new IllegalArgumentException("Ocarina animation phase must be 0 or 1");
        }
        ocarinaAnimationCounter = animationCounter;
        ocarinaAnimationPhase = animationPhase;
    }

    int musicalNoteInertiaForTest(int slot) {
        validateEntitySlot(slot);
        return musicalNoteInertia[slot];
    }

    void setHitboxFlagsForTest(int slot, int value) {
        validateCountdownTestValue(slot, value);
        enemyHitboxFlags[slot] = value;
    }

    int hitboxFlagsForTest(int slot) {
        validateEntitySlot(slot);
        return enemyHitboxFlags[slot];
    }

    void setEnemyFlashCountdownForTest(int slot, int value) {
        validateCountdownTestValue(slot, value);
        enemyFlashCountdown[slot] = value;
    }

    void setPhysicsFlagsForTest(int slot, int value) {
        validateCountdownTestValue(slot, value);
        enemyPhysicsFlags[slot] = value;
    }

    void setThrownDirection(int slot, int value) {
        validateCountdownTestValue(slot, value);
        thrownDirection[slot] = value;
    }

    void setLedgeTransitionTimer(int slot, int value) {
        validateCountdownTestValue(slot, value);
        ledgeTransitionTimer[slot] = value;
    }

    void setThrownDirectionForTest(int slot, int value) {
        setThrownDirection(slot, value);
    }

    void setLedgeTransitionTimerForTest(int slot, int value) {
        setLedgeTransitionTimer(slot, value);
    }

    private static void validateCountdownTestValue(int slot, int value) {
        if (slot < 0 || slot >= EntityRoomLoader.MAX_ENTITIES) {
            throw new IllegalArgumentException("Entity slot out of range: " + slot);
        }
        if (value < 0 || value > 0xFF) {
            throw new IllegalArgumentException("Countdown must be an unsigned byte: " + value);
        }
    }

    int keyDropTransitionCountdown(int slot) {
        validateEntitySlot(slot);
        return enemyTransitionCountdown[slot];
    }

    void setKeyDropTransitionCountdownForTest(int slot, int value) {
        validateCountdownTestValue(slot, value);
        if (!isLoadedEntityOfType(slot, ENTITY_KEY_DROP_POINT)) {
            throw new IllegalArgumentException("Entity slot does not contain a key drop point: "
                + slot);
        }
        enemyTransitionCountdown[slot] = value;
    }

    int options1(int slot) {
        if (slot < 0 || slot >= slots.length) {
            throw new IllegalArgumentException("Entity slot out of range: " + slot);
        }
        if (entityOptions1Override[slot] >= 0) {
            return entityOptions1Override[slot];
        }
        if (isGhiniType(slots[slot].type())) {
            return GHINI_OPTIONS1;
        }
        if (slots[slot].type() == ENTITY_BOMB) {
            return BOMB_OPTIONS1;
        }
        if (slots[slot].type() == ENTITY_BOMBER) {
            return BOMBER_OPTIONS1;
        }
        if (slots[slot].type() == ENTITY_MAD_BOMBER) {
            return MAD_BOMBER_OPTIONS1;
        }
        if (slots[slot].type() == ENTITY_WIZROBE_PROJECTILE) {
            return WIZROBE_PROJECTILE_OPTIONS1;
        }
        if (slots[slot].type() == ENTITY_SWORD_SHIELD_PICKUP) {
            return ENTITY_OPT1_SPLASH_IN_WATER | ENTITY_OPT1_EXCLUDED_FROM_KILL_ALL;
        }
        if (slots[slot].type() == ENTITY_KEY_DROP_POINT) {
            return KEY_DROP_POINT_OPTIONS1;
        }
        if (slots[slot].type() == ENTITY_ARMOS_KNIGHT) {
            return ARMOS_KNIGHT_OPTIONS1;
        }
        if (slots[slot].type() == ENTITY_CHEST_WITH_ITEM) {
            return CHEST_OPTIONS1;
        }
        if (slots[slot].type() == ENTITY_FISH) {
            return ENTITY_OPT1_EXCLUDED_FROM_KILL_ALL;
        }
        if (slots[slot].type() == ENTITY_CROW) {
            return ENTITY_OPT1_ALLOW_OUT_OF_BOUNDS;
        }
        if (slots[slot].type() == ENTITY_BOO_BUDDY) {
            return BooBuddyMotion.OPTIONS1;
        }
        if (slots[slot].type() == ENTITY_DROPPABLE_FAIRY) {
            return FAIRY_OPTIONS1;
        }
        if (slots[slot].type() == ENTITY_SPIKED_BEETLE) {
            // The static table starts at splash-only. The handler writes the
            // sword-clink-off bit on its first normal active pass.
            return ENTITY_OPT1_SPLASH_IN_WATER;
        }
        if (slots[slot].type() == ENTITY_POLS_VOICE) {
            return ENTITY_OPT1_SPLASH_IN_WATER;
        }
        if (isBombiteType(slots[slot].type())) {
            return BOMBITE_OPTIONS1;
        }
        return slots[slot].type() == ENTITY_STALFOS_EVASIVE
            || slots[slot].type() == ENTITY_LIKE_LIKE
            ? ENTITY_OPT1_SPLASH_IN_WATER : 0;
    }

    int colorShellState(int slot) {
        return colorShellMotion.state(slot);
    }

    int bombiteState(int slot) {
        return bombiteMotion.state(slot);
    }

    int bombitePrivateCountdown1(int slot) {
        return bombPrivateCountdown1[slot];
    }

    int bombiteSpeedX(int slot) {
        return bombiteMotion.speedX(slot);
    }

    int bombiteSpeedY(int slot) {
        return bombiteMotion.speedY(slot);
    }

    int colorShellPhysicsFlags(int slot) {
        return colorShellMotion.physicsFlags(slot);
    }

    int fishState(int slot) {
        return fishMotion.state(slot);
    }

    int fishSpeedX(int slot) {
        return fishMotion.speedX(slot);
    }

    int fishSpeedZ(int slot) {
        return fishMotion.speedZ(slot);
    }

    int fishTransitionCountdown(int slot) {
        validateEntitySlot(slot);
        return enemyTransitionCountdown[slot];
    }

    int crowState(int slot) {
        return crowMotion.state(slot);
    }

    int crowSpeedX(int slot) {
        return crowMotion.speedX(slot);
    }

    int crowSpeedY(int slot) {
        return crowMotion.speedY(slot);
    }

    int crowSpeedZ(int slot) {
        return crowMotion.speedZ(slot);
    }

    int crowTransitionCountdown(int slot) {
        validateEntitySlot(slot);
        return enemyTransitionCountdown[slot];
    }

    int booBuddyState(int slot) {
        return booBuddyMotion.state(slot);
    }

    int booBuddySpeedX(int slot) {
        return booBuddyMotion.speedX(slot);
    }

    int booBuddySpeedY(int slot) {
        return booBuddyMotion.speedY(slot);
    }

    int booBuddyTransitionCountdown(int slot) {
        validateEntitySlot(slot);
        return enemyTransitionCountdown[slot];
    }

    int booBuddyTriggerCount() {
        return booBuddyTriggerCount;
    }

    void setBooBuddyTriggerCount(int value) {
        validateByte(value, "Boo Buddy trigger count");
        booBuddyTriggerCount = value;
    }

    void setBooBuddyTriggerCountForTest(int value) {
        setBooBuddyTriggerCount(value);
    }

    void setColorShellStateForTest(int slot, int state, int transitionCountdown,
                                   int direction, int speedX, int speedY) {
        colorShellMotion.setStateForTest(slot, state, transitionCountdown,
            direction, speedX, speedY);
    }

    int enemyRecoilSpeedX(int slot) {
        return enemyRecoilMotion.recoilSpeedX(slot);
    }

    int enemyRecoilSpeedY(int slot) {
        return enemyRecoilMotion.recoilSpeedY(slot);
    }

    void configureEnemyRecoilForTest(int slot, int linkEntityX, int linkEntityY, int length) {
        validateEntitySlot(slot);
        RoomEntity entity = slots[slot];
        if (!entity.loaded()) {
            throw new IllegalArgumentException("Entity slot is not loaded: " + slot);
        }
        enemyRecoilMotion.configure(slot, entity.x(), entity.y(), entity.z(),
            linkEntityX, linkEntityY, length);
    }

    boolean enemyRecoilActive(int slot) {
        return enemyRecoilMotion.isActive(slot);
    }

    private static int variantFor(RoomEntity entity, int frameCounter) {
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
            case ENTITY_SPARK_COUNTER_CLOCKWISE, ENTITY_SPARK_CLOCKWISE ->
                (frameCounter >>> 1) & 0x01;
            case 0x70, 0x73 -> (frameCounter >>> 4) & 0x01;
            default -> entity.spriteVariant();
        };
    }

    private void initializeEntityTimers(RoomEntity entity) {
        enemyPhysicsFlags[entity.slot()] = initialPhysicsFlags(entity.type());
        if (indoorRoom && RoomEntityPickupRules.usesIndoorDefaultSlowTimer(entity.type())) {
            slowTransitionCountdown[entity.slot()] = 0x80;
            slowTimerInitialized[entity.slot()] = true;
        }
    }

    private void resetEnemyDropState(int slot) {
        droppedItemBySlot[slot] = 0;
        dropPrivateCountdown1[slot] = 0;
        dropPrivateCountdown3[slot] = 0;
        enemyDropActive[slot] = false;
        enemyDropMotion.clear(slot);
        fairyMotion.clear(slot);
        hookshotChainMotion.clear(slot);
    }

    private void decrementEnemyDropCountdowns(RoomEntity entity) {
        int slot = entity.slot();
        if (dropPrivateCountdown1[slot] > 0) {
            dropPrivateCountdown1[slot]--;
        }
        if (dropPrivateCountdown3[slot] > 0) {
            dropPrivateCountdown3[slot]--;
        }
    }

    private boolean checkForKeyDropQuicksandHole(RoomEntity entity) {
        if (indoorRoom || entityRoomId != 0xCE || entity.z() != 0) {
            return false;
        }
        return withinUnsignedWindow(entity.y(), 0x48, 3)
            && withinUnsignedWindow(entity.x(), 0x50, 3);
    }

    private static boolean withinUnsignedWindow(int value, int center, int radius) {
        return (((value - center + radius) & 0xFF) < radius * 2);
    }

    private static int initialPhysicsFlags(int type) {
        return switch (type) {
            case ENTITY_ARMOS_STATUE -> ARMOS_INITIAL_PHYSICS_FLAGS;
            case ENTITY_ARMOS_KNIGHT -> ARMOS_KNIGHT_INITIAL_PHYSICS_FLAGS;
            case ENTITY_STALFOS_EVASIVE -> EVASIVE_PHYSICS_FLAGS;
            case ENTITY_IRON_MASK -> IRON_MASK_INITIAL_PHYSICS_FLAGS;
            case ENTITY_GOOMBA, ENTITY_SNAKE -> GOOMBA_INITIAL_PHYSICS_FLAGS;
            case ENTITY_WIZROBE -> 0x02;
            case ENTITY_SWORD_SHIELD_PICKUP -> SWORD_SHIELD_PICKUP_INITIAL_PHYSICS_FLAGS;
            case ENTITY_KEY_DROP_POINT -> KEY_DROP_POINT_INITIAL_PHYSICS_FLAGS;
            case ENTITY_LIKE_LIKE -> LIKE_LIKE_INITIAL_PHYSICS_FLAGS;
            case ENTITY_POLS_VOICE -> 0x12;
            case ENTITY_SPIKED_BEETLE -> 0x12;
            case ENTITY_WIZROBE_PROJECTILE -> 0x42;
            case ENTITY_IRON_MASKS_MASK -> IRON_MASKS_MASK_INITIAL_PHYSICS_FLAGS;
            case ENTITY_BOMB -> BOMB_INITIAL_PHYSICS_FLAGS;
            case ENTITY_CHEST_WITH_ITEM -> CHEST_INITIAL_PHYSICS_FLAGS;
            case ENTITY_BOUNCING_BOMBITE, ENTITY_TIMER_BOMBITE ->
                BOMBITE_INITIAL_PHYSICS_FLAGS;
            case ENTITY_FISH -> FishMotion.INITIAL_PHYSICS_FLAGS;
            case ENTITY_CROW -> CrowMotion.INITIAL_PHYSICS_FLAGS;
            case ENTITY_BOO_BUDDY -> BooBuddyMotion.INITIAL_PHYSICS_FLAGS;
            case ENTITY_DROPPABLE_FAIRY -> FAIRY_INITIAL_PHYSICS_FLAGS;
            case ENTITY_MAD_BOMBER -> MAD_BOMBER_INITIAL_PHYSICS_FLAGS;
            case ENTITY_BOMBER -> BOMBER_INITIAL_PHYSICS_FLAGS;
            case ENTITY_LIFTABLE_ROCK, ENTITY_LIFTABLE_STATUE,
                ENTITY_WRECKING_BALL, ENTITY_SIDE_VIEW_POT, ENTITY_ROOSTER,
                ENTITY_CUCCO, ENTITY_HORSE_PIECE -> ENTITY_PHYSICS_GRABBABLE;
            default -> 0;
        };
    }

    private static int initialHitboxFlags(int type) {
        return type == ENTITY_ARMOS_KNIGHT
            ? ARMOS_KNIGHT_INITIAL_HITBOX_FLAGS : 0;
    }

    private boolean polsVoiceBalladOcarinaTrigger() {
        return linkPlayingOcarinaCountdown == 0x01
            && (ocarinaSongFlags & 0x04) != 0
            && selectedSongIndex == 0;
    }

    private void decrementSlowTransitionCountdown(int slot, int frameCounter) {
        if ((frameCounter & 0x03) == 0 && slowTimerInitialized[slot]
            && slowTransitionCountdown[slot] > 0) {
            slowTransitionCountdown[slot]--;
        }
    }

    private boolean shouldDisappear(RoomEntity entity) {
        return slowTimerInitialized[entity.slot()]
            && switch (entity.type()) {
                case 0x2D, 0x2E, 0x2F, 0x37, 0x38, 0x3B -> true;
                default -> false;
            }
            && slowTransitionCountdown[entity.slot()] < 0x1C;
    }

    private static int persistentClearMask(RoomEntity entity) {
        return entity.sourceLoadOrder() >= 0 && entity.sourceLoadOrder() < 8
            ? 1 << entity.sourceLoadOrder() : 0;
    }

    private static boolean requiresHeldPickupTransition(int type) {
        return switch (type) {
            case 0x30, 0x31, 0x33, 0x34, 0x35, 0x36, 0x39, 0x3A, 0x3C -> true;
            default -> false;
        };
    }

    /**
     * The standard bank-$06 floating handler is available only where its
     * standard sprite definition was loaded. Color Dungeon's $86 handler is
     * a separate bank-$36 path and remains intentionally deferred.
     */
    private static boolean isRuntimeFloatingItem(RoomEntity entity) {
        return FloatingItemMotion.isFloatingItem(entity.type())
            && entity.spriteDefinition().supported();
    }

    private boolean isDisabledFollower(int type) {
        return switch (type) {
            case FollowingNpcEntitySpawner.ENTITY_ROOSTER -> !followingNpcState.roosterFollowing();
            case FollowingNpcEntitySpawner.ENTITY_GHOST ->
                followingNpcState.ghostFollowingState() != 1;
            case FollowingNpcEntitySpawner.ENTITY_MARIN_AT_THE_SHORE ->
                !followingNpcState.marinFollowing();
            case FollowingNpcEntitySpawner.ENTITY_BOW_WOW ->
                !followingNpcState.bowWowFollowing();
            default -> false;
        };
    }

    private static RoomEntity withStatus(RoomEntity entity, EntityStatus status) {
        return preserveDeathMetadata(entity, new RoomEntity(
            entity.slot(), entity.sourceLoadOrder(), entity.type(), entity.x(), entity.y(),
            status, entity.spriteDefinition(), entity.spriteVariant(), entity.entityFlipAttribute(),
            entity.spriteTileOffset(), entity.z()));
    }

    private static RoomEntity withType(RoomEntity entity, int type,
                                       EntitySpriteDefinition definition) {
        int variant = definition.supported() ? definition.initialVariant() : -1;
        return preserveDeathMetadata(entity, new RoomEntity(
            entity.slot(), entity.sourceLoadOrder(), type, entity.x(), entity.y(),
            entity.status(), definition, variant, entity.entityFlipAttribute(),
            entity.spriteTileOffset(), entity.z()));
    }

    private static RoomEntity withVariant(RoomEntity entity, int variant) {
        return preserveDeathMetadata(entity, new RoomEntity(
            entity.slot(), entity.sourceLoadOrder(), entity.type(), entity.x(), entity.y(),
            entity.status(), entity.spriteDefinition(), variant, entity.entityFlipAttribute(),
            entity.spriteTileOffset(), entity.z()));
    }

    private static RoomEntity withFlipAttribute(RoomEntity entity, int flipAttribute) {
        return preserveDeathMetadata(entity, new RoomEntity(
            entity.slot(), entity.sourceLoadOrder(), entity.type(), entity.x(), entity.y(),
            entity.status(), entity.spriteDefinition(), entity.spriteVariant(), flipAttribute,
            entity.spriteTileOffset(), entity.z()));
    }

    private static RoomEntity withZ(RoomEntity entity, int z) {
        return preserveDeathMetadata(entity, new RoomEntity(
            entity.slot(), entity.sourceLoadOrder(), entity.type(), entity.x(), entity.y(),
            entity.status(), entity.spriteDefinition(), entity.spriteVariant(),
            entity.entityFlipAttribute(), entity.spriteTileOffset(), z));
    }

    private static RoomEntity withDefinition(RoomEntity entity,
                                              EntitySpriteDefinition definition, int variant) {
        int selectedVariant = definition.supported()
            ? Math.min(Math.max(variant, 0), definition.variantCount() - 1) : -1;
        return preserveDeathMetadata(entity, new RoomEntity(
            entity.slot(), entity.sourceLoadOrder(), entity.type(), entity.x(), entity.y(),
            entity.status(), definition, selectedVariant, entity.entityFlipAttribute(),
            entity.spriteTileOffset(), entity.z()));
    }

    private static RoomEntity preserveDeathMetadata(RoomEntity source, RoomEntity rebuilt) {
        if (source.status() != EntityStatus.DYING || rebuilt.status() != EntityStatus.DYING) {
            return rebuilt;
        }
        return new RoomEntity(rebuilt.slot(), rebuilt.sourceLoadOrder(), rebuilt.type(),
            rebuilt.x(), rebuilt.y(), rebuilt.status(), rebuilt.spriteDefinition(),
            rebuilt.spriteVariant(), rebuilt.entityFlipAttribute(), rebuilt.spriteTileOffset(),
            rebuilt.z(), source.deathSpriteVariant(), source.powerRecoilDeath());
    }

    private static RoomEntity withDeathPresentation(RoomEntity entity, int variant,
                                                     boolean powerRecoil) {
        return new RoomEntity(entity.slot(), entity.sourceLoadOrder(), entity.type(),
            entity.x(), entity.y(), entity.status(), entity.spriteDefinition(),
            entity.spriteVariant(), entity.entityFlipAttribute(), entity.spriteTileOffset(),
            entity.z(), variant, powerRecoil);
    }

    private static int deathSpriteVariantForCountdown(int countdown) {
        if (countdown >= 0x20 || countdown <= 0) {
            return -1;
        }
        return ((countdown << 1) & 0x30) >>> 4;
    }

    private boolean beginFalling(RoomEntity entity,
                                 RoomEntityGroundInteraction.PitTransition transition,
                                 boolean ignoreHitsDecrementedBeforeHandler) {
        int slot = entity.slot();
        int ignoreHitsCountdown = enemyIgnoreHitsCountdown[slot];
        if (ignoreHitsDecrementedBeforeHandler) {
            // The Java runtime keeps a shared combat countdown pass for the
            // already-ported handlers.  In the ROM, the pit branch reads the
            // value before that branch's own decrement; reconstruct that
            // value here so INIT and ACTIVE entities both get one decrement
            // at the same source-level point.
            ignoreHitsCountdown = (ignoreHitsCountdown + 1) & 0xFF;
        }
        if (ignoreHitsCountdown == 0) {
            return false;
        }

        ignoreHitsCountdown--;
        enemyIgnoreHitsCountdown[slot] = ignoreHitsCountdown;
        enemyFlashCountdown[slot] = 0;
        fallingTargetX[slot] = transition.targetX() & 0xFF;
        fallingTargetY[slot] = transition.targetY() & 0xFF;
        fallingSpeedX[slot] = 0;
        fallingSpeedY[slot] = 0;
        fallingSpeedXAccumulator[slot] = 0;
        fallingSpeedYAccumulator[slot] = 0;
        fallingVisualYOffset[slot] = 0;

        boolean longFallingTransition = entity.type() == ENTITY_MOBLIN_SWORD
            || entity.type() == ENTITY_MOBLIN || entity.type() == ENTITY_OCTOROK;
        enemyTransitionCountdown[slot] = longFallingTransition ? 0x6F : 0x48;
        if (!longFallingTransition && ignoreHitsCountdown == 0) {
            enemyTransitionCountdown[slot] = 0x2F;
            pendingEntityEvents.add(new EntityCombatEvent(
                slot, entity.type(), 0, false,
                EntityCombatEvent.SoundChannel.JINGLE, FALLING_JINGLE_ID));
        }
        return true;
    }

    /** Port of bank-$03 EntityFallHandler's pre-impact falling branch. */
    private RoomEntity advanceFallingEntity(RoomEntity entity) {
        int slot = entity.slot();
        int transition = enemyTransitionCountdown[slot] & 0xFF;
        if (transition == 0) {
            return null;
        }

        if (transition >= 0x40) {
            // EntityFallHandler calls SetEntityVariantForDirection_03 three
            // times before the family handler. ReturnIfNonInteractive then
            // stops that handler after presentation because the status is
            // still FALLING, so movement and wall collision must not run here.
            fallingVisualYOffset[slot] = 0;
            int variant = entity.spriteVariant();
            if (entity.type() == ENTITY_OCTOROK || entity.type() == ENTITY_MOBLIN) {
                variant = roamingEnemyMotion.advancePresentationVariant(slot, 3);
            } else if (entity.type() == ENTITY_MOBLIN_SWORD) {
                variant = moblinSwordMotion.advancePresentationVariant(slot, entity.x(), 3);
            }
            return withPositionAndVariant(entity, entity.x(), entity.y(), variant);
        }

        int phase = (transition >>> 4) & 0x03;
        fallingVisualYOffset[slot] = FALLING_VISUAL_Y_OFFSETS[phase];
        if (transition == 0x3F) {
            pendingEntityEvents.add(new EntityCombatEvent(
                slot, entity.type(), 0, false,
                EntityCombatEvent.SoundChannel.JINGLE, FALLING_JINGLE_ID));
        }

        FallingVector vector = vectorTowardsTarget(entity.x(), entity.y(), entity.z(),
            fallingTargetX[slot], fallingTargetY[slot], FALLING_VECTOR_LENGTHS[phase]);
        fallingSpeedX[slot] = vector.x();
        fallingSpeedY[slot] = vector.y();
        int x = addFallingSpeedToPosition(entity.x(), fallingSpeedX[slot],
            fallingSpeedXAccumulator, slot);
        int y = addFallingSpeedToPosition(entity.y(), fallingSpeedY[slot],
            fallingSpeedYAccumulator, slot);
        return withPositionAndVariant(entity, x, y, phase);
    }

    private static FallingVector vectorTowardsTarget(int entityX, int entityY, int entityZ,
                                                      int targetX, int targetY, int length) {
        int distanceX = signedByte(targetX - entityX);
        int distanceY = signedByte(targetY - entityY + entityZ);
        int absoluteX = Math.abs(distanceX);
        int absoluteY = Math.abs(distanceY);
        boolean swapped = absoluteX < absoluteY;
        int smaller = Math.min(absoluteX, absoluteY);
        int larger = Math.max(absoluteX, absoluteY);
        int result = romDivide(length, smaller, larger);
        int x = swapped ? result : length;
        int y = swapped ? length : result;
        if (distanceX < 0) {
            x = -x;
        }
        if (distanceY < 0) {
            y = -y;
        }
        return new FallingVector(x & 0xFF, y & 0xFF);
    }

    private static int romDivide(int length, int smallerDistance, int largerDistance) {
        if (length == 0) {
            return 0;
        }
        if (largerDistance == 0) {
            return length;
        }
        int result = 0;
        int remainder = 0;
        for (int count = 0; count < length; count++) {
            int sum = remainder + smallerDistance;
            if (sum >= largerDistance) {
                sum -= largerDistance;
                result++;
            }
            remainder = sum & 0xFF;
        }
        return result;
    }

    private static int addFallingSpeedToPosition(int position, int speed, int[] accumulator,
                                                  int slot) {
        speed &= 0xFF;
        if (speed == 0) {
            return position & 0xFF;
        }
        int fractionalSum = accumulator[slot] + ((speed << 4) & 0xF0);
        accumulator[slot] = fractionalSum & 0xFF;
        int delta = signedByte(speed) >> 4;
        if (fractionalSum > 0xFF) {
            delta++;
        }
        return (position + delta) & 0xFF;
    }

    private static RoomEntity withPositionAndVariant(RoomEntity entity, int x, int y,
                                                       int variant) {
        return preserveDeathMetadata(entity, new RoomEntity(
            entity.slot(), entity.sourceLoadOrder(), entity.type(),
            x & 0xFF, y & 0xFF, entity.status(), entity.spriteDefinition(), variant,
            entity.entityFlipAttribute(), entity.spriteTileOffset(), entity.z()));
    }

    private record FallingVector(int x, int y) {
    }

    private void disableEntityWithoutPersistence(int slot) {
        resetEnemyDropState(slot);
        slowTransitionCountdown[slot] = 0;
        slowTimerInitialized[slot] = false;
        enemyTransitionCountdown[slot] = 0;
        bossDeathProducerState[slot] = 0;
        enemyStunnedCountdown[slot] = 0;
        dyingCountdown[slot] = 0;
        powerRecoilDeath[slot] = false;
        enemyPhysicsFlags[slot] = 0;
        enemyHealth[slot] = 0;
        enemyFlashCountdown[slot] = 0;
        enemyIgnoreHitsCountdown[slot] = 0;
        chestSpeedY[slot] = 0;
        chestSpeedYAccumulator[slot] = 0;
        chestInertia[slot] = 0;
        chestItemBySlot[slot] = 0;
        enemyHitboxFlags[slot] = 0;
        armosKnightMotion.clear(slot);
        entityGroundStatus[slot] = 0;
        fallingTargetX[slot] = 0;
        fallingTargetY[slot] = 0;
        fallingSpeedX[slot] = 0;
        fallingSpeedY[slot] = 0;
        fallingSpeedXAccumulator[slot] = 0;
        fallingSpeedYAccumulator[slot] = 0;
        fallingVisualYOffset[slot] = 0;
        liftedPhase[slot] = 0;
        liftedSourceDirection[slot] = 0;
        liftedStateInitialized[slot] = false;
        thrownDirection[slot] = 0xFF;
        bombDirection[slot] = 0xFF;
        bombPrivateState4[slot] = 0;
        magicPowderState[slot] = 0;
        magicPowderPrivateState4[slot] = 0;
        bombPrivateCountdown1[slot] = 0;
        bombPrivateCountdown3[slot] = 0;
        ironMaskPrivateState2[slot] = 0;
        ironMasksMaskSourceHookshotSlot[slot] = 0;
        liftableRockSmashCountdown[slot] = 0;
        liftableRockSmashSourceVariant[slot] = 0;
        liftableRockSmashActive[slot] = false;
        bombFinalPresentationPending[slot] = false;
        placedBombMotionInitialized[slot] = false;
        enemyBombMotionInitialized[slot] = false;
        ledgeTransitionTimer[slot] = 0;
        thrownMotionInitialized[slot] = false;
        baseEntityFlipAttribute[slot] = 0;
        entityOptions1Override[slot] = -1;
        enemyRecoilMotion.clear(slot);
        bomberMotion.clear(slot);
        madBomberMotion.clear(slot);
        bombiteMotion.clear(slot);
        colorShellMotion.clear(slot);
        butterflyMotion.clear(slot);
        keeseMotion.clear(slot);
        roamingEnemyMotion.clear(slot);
        unmaskedIronMaskMotion.clear(slot);
        moblinSwordMotion.clear(slot);
        tektiteMotion.clear(slot);
        leeverMotion.clear(slot);
        antiFairyMotion.clear(slot);
        sparkMotion.clear(slot);
        zolGelMotion.clear(slot);
        hidingZolMotion.clear(slot);
        spikeTrapMotion.clear(slot);
        pairoddMotion.clear(slot);
        pairoddProjectileMotion.clear(slot);
        playerArrowMotion.clear(slot);
        magicRodFireballMotion.clear(slot);
        playerArrowBombArrow[slot] = false;
        if (latestDroppedBombEntityIndex == slot) {
            latestDroppedBombEntityIndex = -1;
        }
        if (latestShotArrowEntityIndex == slot) {
            latestShotArrowEntityIndex = -1;
        }
        enemyProjectileMotion.clear(slot);
        laserMotion.clear(slot);
        waterTektiteMotion.clear(slot);
        fishMotion.clear(slot);
        crowMotion.clear(slot);
        booBuddyMotion.clear(slot);
        stalfosAggressiveMotion.clear(slot);
        stalfosEvasiveMotion.clear(slot);
        gibdoMotion.clear(slot);
        goombaMotion.clear(slot);
        snakeMotion.clear(slot);
        wizrobeMotion.clear(slot);
        wizrobeProjectileMotion.clear(slot);
        peaHatMotion.clear(slot);
        armosMotion.clear(slot);
        followingNpcMotion.clear(slot);
        ghiniMotion.clear(slot);
        hardHatMotion.clear(slot);
        polsVoiceMotion.clear(slot);
        spikedBeetleMotion.clear(slot);
        bowWowMotion.clear(slot);
        thrownEntityMotion.clear(slot);
        if (liftedEntitySlot == slot) {
            liftedEntitySlot = -1;
            liftedCarryState = 0;
            liftedEffectiveDirection = 0;
        }
        slots[slot] = RoomEntity.disabled(slot);
    }

    private int verticalSpeedZ(RoomEntity entity) {
        int slot = entity.slot();
        if (enemyDropActive[slot]) {
            return enemyDropMotion.speedZ(slot);
        }
        if (entity.type() == ENTITY_TEKTITE) {
            return tektiteMotion.speedZ(slot);
        }
        if (entity.type() == ENTITY_FISH) {
            return fishMotion.speedZ(slot);
        }
        if (entity.type() == ENTITY_CROW) {
            return crowMotion.speedZ(slot);
        }
        if (entity.type() == ENTITY_STALFOS_AGGRESSIVE) {
            return stalfosAggressiveMotion.speedZ(slot);
        }
        if (entity.type() == ENTITY_STALFOS_EVASIVE) {
            return stalfosEvasiveMotion.speedZ(slot);
        }
        if (isZolGelType(entity.type())) {
            return zolGelMotion.speedZ(slot);
        }
        if (entity.type() == ENTITY_HIDING_ZOL) {
            return hidingZolMotion.speedZ(slot);
        }
        if (entity.type() == ENTITY_POLS_VOICE) {
            return polsVoiceMotion.speedZ(slot);
        }
        if (entity.type() == ENTITY_SPIKED_BEETLE) {
            return spikedBeetleMotion.speedZ(slot);
        }
        if (isEnemyProjectileType(entity.type())) {
            return enemyProjectileMotion.speedZ(slot);
        }
        if (entity.type() == ENTITY_ARROW) {
            return playerArrowMotion.speedZ(slot);
        }
        if (ColorShellMotion.isColorShellType(entity.type())) {
            return colorShellMotion.speedZ(slot);
        }
        return 0;
    }

    private boolean decrementEnemyCombatCountdowns(int slot, boolean decrementIgnoreHits) {
        if (enemyFlashCountdown[slot] > 0) {
            enemyFlashCountdown[slot]--;
        }
        if (decrementIgnoreHits && !enemyRecoilMotion.isActive(slot)
            && enemyIgnoreHitsCountdown[slot] > 0) {
            enemyIgnoreHitsCountdown[slot]--;
            return true;
        }
        return false;
    }

    private void decrementEnemyStatusCountdowns(int slot) {
        if (enemyTransitionCountdown[slot] > 0) {
            enemyTransitionCountdown[slot]--;
        }
        if (enemyStunnedCountdown[slot] > 0) {
            enemyStunnedCountdown[slot]--;
        }
        if (dyingCountdown[slot] > 0) {
            dyingCountdown[slot]--;
        }
        if (bombPrivateCountdown1[slot] > 0) {
            bombPrivateCountdown1[slot]--;
        }
        if (bombPrivateCountdown3[slot] > 0) {
            bombPrivateCountdown3[slot]--;
        }
        if (liftableRockSmashActive[slot] && liftableRockSmashCountdown[slot] > 0) {
            liftableRockSmashCountdown[slot]--;
        }
    }

    private void requestArmosLinkPush(RoomEntity entity, int linkEntityX, int linkEntityY,
                                      int linkMotionState) {
        if (linkMotionState >= EnemyProjectileCollision.LINK_MOTION_NON_INTERACTIVE
            || (enemyPhysicsFlags[entity.slot()] & 0x80) != 0
            || !RoomEntityCombatRules.overlapsLink(entity, linkEntityX, linkEntityY)) {
            return;
        }
        pendingLinkFinalPositionRequests.add(new LinkFinalPositionRequest(entity.slot()));
        resetHookshotChainAfterLinkPush();
    }

    private void resetHookshotChainAfterLinkPush() {
        for (int slot = 0; slot < slots.length; slot++) {
            if (!slots[slot].loaded() || slots[slot].type() != ENTITY_HOOKSHOT_CHAIN
                || !hookshotChainMotion.active(slot)) {
                continue;
            }
            HookshotChainMotion.State state = hookshotChainMotion.state(slot);
            hookshotChainMotion.setState(slot, new HookshotChainMotion.State(
                state.x(), state.y(), state.z(), state.direction(), state.speedX(),
                state.speedY(), state.transitionCountdown(), state.speedXAccumulator(),
                state.speedYAccumulator(), 0, state.wallCollisionPending()));
            return;
        }
    }

    private EnemyRecoilMotion.Update applyEnemyRecoilIfNeeded(
            RoomEntity entity, RoomEntityBackgroundCollision backgroundCollision) {
        int slot = entity.slot();
        if (!enemyRecoilMotion.isActive(slot)) {
            return new EnemyRecoilMotion.Update(entity, false);
        }
        if (enemyIgnoreHitsCountdown[slot] == 0) {
            enemyRecoilMotion.clear(slot);
            return new EnemyRecoilMotion.Update(entity, false);
        }

        if (isRoamingEnemyType(entity.type())) {
            roamingEnemyMotion.beginRecoil(slot);
        }
        // ApplyRecoilIfNeeded_03/06 decrements the shared countdown
        // immediately before applying one fixed-point recoil step.
        enemyIgnoreHitsCountdown[slot]--;
        EnemyRecoilMotion.Update update = enemyRecoilMotion.advance(
            entity, backgroundCollision, isRoamingEnemyType(entity.type()));
        if (update.blocked() && isRoamingEnemyType(entity.type())) {
            // StopEntityRecoilOnCollision clears the ignore-hits countdown.
            enemyIgnoreHitsCountdown[slot] = 0;
        }
        return update;
    }

    private static int byteValue(int value) {
        return value & 0xFF;
    }

    private static int unsignedByteAbs(int value) {
        int difference = value & 0xFF;
        return difference < 0x80 ? difference : 0x100 - difference;
    }

    private static int signedByte(int value) {
        int unsigned = value & 0xFF;
        return unsigned < 0x80 ? unsigned : unsigned - 0x100;
    }

    private static int romDirectionForJavaDirection(int javaDirection) {
        if (javaDirection < 0 || javaDirection > 3) {
            throw new IllegalArgumentException("Link direction out of range: " + javaDirection);
        }
        return switch (javaDirection) {
            case 0 -> LiftedEntityMotion.ROM_DIRECTION_DOWN;
            case 1 -> LiftedEntityMotion.ROM_DIRECTION_UP;
            case 2 -> LiftedEntityMotion.ROM_DIRECTION_LEFT;
            case 3 -> LiftedEntityMotion.ROM_DIRECTION_RIGHT;
            default -> throw new AssertionError(javaDirection);
        };
    }

    private static void validateEntitySlot(int slot) {
        if (slot < 0 || slot >= EntityRoomLoader.MAX_ENTITIES) {
            throw new IllegalArgumentException("Entity slot out of range: " + slot);
        }
    }

    private static void validateEnemyDropByte(int value, String label) {
        if (value < 0 || value > 0xFF) {
            throw new IllegalArgumentException(label + " must be an unsigned byte: " + value);
        }
    }

    private static void validateByte(int value, String label) {
        if (value < 0 || value > 0xFF) {
            throw new IllegalArgumentException(label + " must be an unsigned byte: " + value);
        }
    }

    private static void validateRomDirection(int direction) {
        if (direction < LiftedEntityMotion.ROM_DIRECTION_RIGHT
            || direction > LiftedEntityMotion.ROM_DIRECTION_DOWN) {
            throw new IllegalArgumentException("ROM direction must be between 0 and 3");
        }
    }
}
