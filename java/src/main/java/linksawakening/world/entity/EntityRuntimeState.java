package linksawakening.world.entity;

import linksawakening.world.entity.motion.AntiFairyMotion;
import linksawakening.world.entity.motion.ArmosKnightMotion;
import linksawakening.world.entity.motion.ArmosMotion;
import linksawakening.world.entity.motion.BlooperMotion;
import linksawakening.world.entity.motion.BomberMotion;
import linksawakening.world.entity.motion.BombiteMotion;
import linksawakening.world.entity.motion.BooBuddyMotion;
import linksawakening.world.entity.motion.BoomerangMotion;
import linksawakening.world.entity.motion.BossIntroMotion;
import linksawakening.world.entity.motion.BowWowMotion;
import linksawakening.world.entity.motion.BushCrawlerMotion;
import linksawakening.world.entity.motion.ButterflyMotion;
import linksawakening.world.entity.motion.BuzzBlobMotion;
import linksawakening.world.entity.motion.ColorShellMotion;
import linksawakening.world.entity.motion.CrowMotion;
import linksawakening.world.entity.motion.CuccoMotion;
import linksawakening.world.entity.motion.DogMotion;
import linksawakening.world.entity.motion.EnemyDropMotion;
import linksawakening.world.entity.motion.EnemyProjectileMotion;
import linksawakening.world.entity.motion.EnemyRecoilMotion;
import linksawakening.world.entity.motion.FairyMotion;
import linksawakening.world.entity.motion.FishMotion;
import linksawakening.world.entity.motion.FollowingNpcMotion;
import linksawakening.world.entity.motion.GenieMotion;
import linksawakening.world.entity.motion.GhiniMotion;
import linksawakening.world.entity.motion.GiantGopongaMotion;
import linksawakening.world.entity.motion.GibdoMotion;
import linksawakening.world.entity.motion.GoombaMotion;
import linksawakening.world.entity.motion.GopongaProjectileMotion;
import linksawakening.world.entity.motion.HardHatMotion;
import linksawakening.world.entity.motion.HeartPiecePickupMotion;
import linksawakening.world.entity.motion.HidingZolMotion;
import linksawakening.world.entity.motion.HinoxMotion;
import linksawakening.world.entity.motion.HookshotBridgeMotion;
import linksawakening.world.entity.motion.HookshotChainMotion;
import linksawakening.world.entity.motion.KeeseMotion;
import linksawakening.world.entity.motion.LaserMotion;
import linksawakening.world.entity.motion.LeeverMotion;
import linksawakening.world.entity.motion.LikeLikeMotion;
import linksawakening.world.entity.motion.MadBomberMotion;
import linksawakening.world.entity.motion.MagicRodFireballMotion;
import linksawakening.world.entity.motion.MaskedMimicMotion;
import linksawakening.world.entity.motion.MimicMotion;
import linksawakening.world.entity.motion.MiniMoldormMotion;
import linksawakening.world.entity.motion.MoblinKingMotion;
import linksawakening.world.entity.motion.MoblinSwordMotion;
import linksawakening.world.entity.motion.MoldormMotion;
import linksawakening.world.entity.motion.PairoddMotion;
import linksawakening.world.entity.motion.PairoddProjectileMotion;
import linksawakening.world.entity.motion.PeaHatMotion;
import linksawakening.world.entity.motion.PincerMotion;
import linksawakening.world.entity.motion.PiranhaMotion;
import linksawakening.world.entity.motion.PlayerArrowMotion;
import linksawakening.world.entity.motion.PokeyMotion;
import linksawakening.world.entity.motion.PolsVoiceMotion;
import linksawakening.world.entity.motion.RoamingEnemyMotion;
import linksawakening.world.entity.motion.RollingBonesMotion;
import linksawakening.world.entity.motion.SandCrabMotion;
import linksawakening.world.entity.motion.SecretSeashellMotion;
import linksawakening.world.entity.motion.SideViewPlatformMotion;
import linksawakening.world.entity.motion.SideViewPotMotion;
import linksawakening.world.entity.motion.SnakeMotion;
import linksawakening.world.entity.motion.SparkMotion;
import linksawakening.world.entity.motion.SpikeTrapMotion;
import linksawakening.world.entity.motion.SpikedBeetleMotion;
import linksawakening.world.entity.motion.StalfosAggressiveMotion;
import linksawakening.world.entity.motion.StalfosEvasiveMotion;
import linksawakening.world.entity.motion.StarMotion;
import linksawakening.world.entity.motion.SwordBeamMotion;
import linksawakening.world.entity.motion.TarinRaccoonMotion;
import linksawakening.world.entity.motion.TektiteMotion;
import linksawakening.world.entity.motion.ThreeOfAKindMotion;
import linksawakening.world.entity.motion.ThrownEntityMotion;
import linksawakening.world.entity.motion.UnmaskedIronMaskMotion;
import linksawakening.world.entity.motion.UrchinMotion;
import linksawakening.world.entity.motion.WaterTektiteMotion;
import linksawakening.world.entity.motion.WingedOctorokMotion;
import linksawakening.world.entity.motion.WitchMotion;
import linksawakening.world.entity.motion.WitchRatMotion;
import linksawakening.world.entity.motion.WizrobeMotion;
import linksawakening.world.entity.motion.WizrobeProjectileMotion;
import linksawakening.world.entity.motion.ZolGelMotion;
import linksawakening.world.entity.motion.ZombieMotion;
import linksawakening.world.entity.motion.ZoraMotion;

import linksawakening.entity.EntitySpriteSelection;
import java.util.List;
import java.util.function.IntUnaryOperator;

/** ROM slot arrays and room handler state; orchestration remains in RoomEntityRuntime. */
abstract class EntityRuntimeState extends EntityRomConstants {
    final EntityEventChannels events = new EntityEventChannels();
    final EntityLinkState linkState = new EntityLinkState();
    final EntityLinkView linkView = new EntityLinkView(linkState);

    public EntitySpriteSelection spriteSelection;

    RoomEntityGroundInteraction groundInteraction =
        (entity, frameCounter, previousGroundStatus, speedZ, sideScrolling) ->
            RoomEntityGroundInteraction.Result.unchanged(entity, 0);

    RoomEntityBackgroundInteraction backgroundInteraction;

    public FollowingNpcState followingNpcState = FollowingNpcState.none();

    LinkPositionHistory followingLinkPositionHistory = new LinkPositionHistory();

    final ButterflyMotion butterflyMotion = new ButterflyMotion();

    final SideViewPlatformMotion sideViewPlatformMotion = new SideViewPlatformMotion();

    final SideViewPotMotion sideViewPotMotion = new SideViewPotMotion();

    final KeeseMotion keeseMotion = new KeeseMotion();

    final RoamingEnemyMotion roamingEnemyMotion = new RoamingEnemyMotion();

    final UnmaskedIronMaskMotion unmaskedIronMaskMotion =
        new UnmaskedIronMaskMotion();

    final MoblinSwordMotion moblinSwordMotion = new MoblinSwordMotion();

    final PlayerArrowMotion playerArrowMotion = new PlayerArrowMotion();

    final EnemyProjectileMotion enemyProjectileMotion = new EnemyProjectileMotion();

    final HookshotChainMotion hookshotChainMotion = new HookshotChainMotion();

    final HookshotBridgeMotion hookshotBridgeMotion = new HookshotBridgeMotion();

    final LaserMotion laserMotion = new LaserMotion();

    final TektiteMotion tektiteMotion = new TektiteMotion();

    final LeeverMotion leeverMotion = new LeeverMotion();

    final AntiFairyMotion antiFairyMotion = new AntiFairyMotion();

    final SparkMotion sparkMotion = new SparkMotion();

    final ZolGelMotion zolGelMotion = new ZolGelMotion();

    final HidingZolMotion hidingZolMotion = new HidingZolMotion();

    final StarMotion starMotion = new StarMotion();

    final BlooperMotion blooperMotion = new BlooperMotion();

    final WingedOctorokMotion wingedOctorokMotion = new WingedOctorokMotion();

    final PincerMotion pincerMotion = new PincerMotion();

    final BushCrawlerMotion bushCrawlerMotion = new BushCrawlerMotion();

    final MimicMotion mimicMotion = new MimicMotion();

    final MaskedMimicMotion maskedMimicMotion = new MaskedMimicMotion();

    final MiniMoldormMotion miniMoldormMotion = new MiniMoldormMotion();

    final MoldormMotion moldormMotion = new MoldormMotion();

    final CuccoMotion cuccoMotion = new CuccoMotion();

    final GiantGopongaMotion giantGopongaMotion = new GiantGopongaMotion();

    final GopongaProjectileMotion gopongaProjectileMotion =
        new GopongaProjectileMotion();

    final PokeyMotion pokeyMotion = new PokeyMotion();

    final PiranhaMotion piranhaMotion = new PiranhaMotion();

    final ZoraMotion zoraMotion = new ZoraMotion();

    final ZombieMotion zombieMotion = new ZombieMotion();

    final BuzzBlobMotion buzzBlobMotion = new BuzzBlobMotion();

    final SandCrabMotion sandCrabMotion = new SandCrabMotion();

    final DogMotion dogMotion = new DogMotion();

    final UrchinMotion urchinMotion = new UrchinMotion();

    final WitchRatMotion witchRatMotion = new WitchRatMotion();

    final WitchMotion witchMotion = new WitchMotion();

    public final int[] witchGotItemCountdown = new int[EntityRoomLoader.MAX_ENTITIES];

    final TarinRaccoonMotion tarinRaccoonMotion = new TarinRaccoonMotion();

    final SpikeTrapMotion spikeTrapMotion = new SpikeTrapMotion();

    final PairoddMotion pairoddMotion = new PairoddMotion();

    final PairoddProjectileMotion pairoddProjectileMotion =
        new PairoddProjectileMotion();

    final WaterTektiteMotion waterTektiteMotion = new WaterTektiteMotion();

    final FishMotion fishMotion = new FishMotion();

    final CrowMotion crowMotion = new CrowMotion();

    final BooBuddyMotion booBuddyMotion = new BooBuddyMotion();

    final FairyMotion fairyMotion = new FairyMotion();

    final StalfosAggressiveMotion stalfosAggressiveMotion =
        new StalfosAggressiveMotion();

    final StalfosEvasiveMotion stalfosEvasiveMotion = new StalfosEvasiveMotion();

    final GibdoMotion gibdoMotion = new GibdoMotion();

    final LikeLikeMotion likeLikeMotion = new LikeLikeMotion();

    final GoombaMotion goombaMotion = new GoombaMotion();

    final SnakeMotion snakeMotion = new SnakeMotion();

    final WizrobeMotion wizrobeMotion = new WizrobeMotion();

    final WizrobeProjectileMotion wizrobeProjectileMotion =
        new WizrobeProjectileMotion();

    final PeaHatMotion peaHatMotion = new PeaHatMotion();

    final ArmosMotion armosMotion = new ArmosMotion();

    final ArmosKnightMotion armosKnightMotion = new ArmosKnightMotion();

    final HinoxMotion hinoxMotion = new HinoxMotion();

    final RollingBonesMotion rollingBonesMotion = new RollingBonesMotion();

    final ThreeOfAKindMotion threeOfAKindMotion = new ThreeOfAKindMotion();

    final MoblinKingMotion moblinKingMotion = new MoblinKingMotion();

    final BossIntroMotion bossIntroMotion = new BossIntroMotion();

    final GenieMotion genieMotion = new GenieMotion();

    final int[] genieActiveState = new int[EntityRoomLoader.MAX_ENTITIES];

    final int[] geniePrivateState1 = new int[EntityRoomLoader.MAX_ENTITIES];

    final int[] geniePrivateState2 = new int[EntityRoomLoader.MAX_ENTITIES];

    final int[] geniePrivateState3 = new int[EntityRoomLoader.MAX_ENTITIES];

    final int[] geniePrivateState4 = new int[EntityRoomLoader.MAX_ENTITIES];

    final int[] geniePrivateCountdown1 = new int[EntityRoomLoader.MAX_ENTITIES];

    final int[] geniePrivateCountdown2 = new int[EntityRoomLoader.MAX_ENTITIES];

    final int[] genieDirection = new int[EntityRoomLoader.MAX_ENTITIES];

    final boolean[] genieDeathInitialized = new boolean[EntityRoomLoader.MAX_ENTITIES];

    final int[] genieSpeedX = new int[EntityRoomLoader.MAX_ENTITIES];

    final int[] genieSpeedY = new int[EntityRoomLoader.MAX_ENTITIES];

    final int[] genieSpeedZ = new int[EntityRoomLoader.MAX_ENTITIES];

    final int[] genieSpeedXAccumulator = new int[EntityRoomLoader.MAX_ENTITIES];

    final int[] genieSpeedYAccumulator = new int[EntityRoomLoader.MAX_ENTITIES];

    final int[] genieSpeedZAccumulator = new int[EntityRoomLoader.MAX_ENTITIES];

    final boolean[] genieImpactRegisteredThisFrame =
        new boolean[EntityRoomLoader.MAX_ENTITIES];

    int genieControllerSlot = -1;

    int genieSharedBodySpeedX;

    int genieSharedBodySpeedY;

    int bossAgonySfxCountdown;

    int bossAgonySourceSlot;

    final GhiniMotion ghiniMotion = new GhiniMotion();

    final HardHatMotion hardHatMotion = new HardHatMotion();

    final PolsVoiceMotion polsVoiceMotion = new PolsVoiceMotion();

    final SpikedBeetleMotion spikedBeetleMotion = new SpikedBeetleMotion();

    final MadBomberMotion madBomberMotion = new MadBomberMotion();

    final BomberMotion bomberMotion = new BomberMotion();

    final BombiteMotion bombiteMotion = new BombiteMotion();

    final EnemyRecoilMotion enemyRecoilMotion = new EnemyRecoilMotion();

    final FollowingNpcMotion followingNpcMotion = new FollowingNpcMotion();

    final BowWowMotion bowWowMotion = new BowWowMotion();

    final ColorShellMotion colorShellMotion = new ColorShellMotion();

    final SecretSeashellMotion secretSeashellMotion =
        new SecretSeashellMotion();

    final ThrownEntityMotion thrownEntityMotion = new ThrownEntityMotion();

    final int[] slowTransitionCountdown = new int[EntityRoomLoader.MAX_ENTITIES];

    final boolean[] slowTimerInitialized = new boolean[EntityRoomLoader.MAX_ENTITIES];

    final int[] droppedItemBySlot = new int[EntityRoomLoader.MAX_ENTITIES];

    final int[] dropPrivateCountdown1 = new int[EntityRoomLoader.MAX_ENTITIES];

    final int[] dropPrivateCountdown3 = new int[EntityRoomLoader.MAX_ENTITIES];

    public final int[] droppablePrivateState3 = new int[EntityRoomLoader.MAX_ENTITIES];

    final int[] droppablePrivateState4 = new int[EntityRoomLoader.MAX_ENTITIES];

    final boolean[] enemyDropActive = new boolean[EntityRoomLoader.MAX_ENTITIES];

    final EnemyDropMotion enemyDropMotion = new EnemyDropMotion();

    final EntitySlotView slotView = new EntitySlotView(this);
    final int[] enemyTransitionCountdown = new int[EntityRoomLoader.MAX_ENTITIES];

    final int[] bossDeathProducerState = new int[EntityRoomLoader.MAX_ENTITIES];

    final boolean[] rollingBonesDeathInitialized =
        new boolean[EntityRoomLoader.MAX_ENTITIES];

    final int[] moldormDestructionTailState =
        new int[EntityRoomLoader.MAX_ENTITIES];

    final int[] enemyStunnedCountdown = new int[EntityRoomLoader.MAX_ENTITIES];

    boolean gelClingingToLink;

    final int[] dyingCountdown = new int[EntityRoomLoader.MAX_ENTITIES];

    final boolean[] powerRecoilDeath = new boolean[EntityRoomLoader.MAX_ENTITIES];

    final int[] enemyPhysicsFlags = new int[EntityRoomLoader.MAX_ENTITIES];

    final int[] enemyHealth = new int[EntityRoomLoader.MAX_ENTITIES];

    final int[] ironMaskPrivateState2 = new int[EntityRoomLoader.MAX_ENTITIES];

    final int[] ironMasksMaskSourceHookshotSlot =
        new int[EntityRoomLoader.MAX_ENTITIES];

    final int[] rollingBonesSwordHitFrame = new int[EntityRoomLoader.MAX_ENTITIES];

    final int[] moblinKingLethalHitFrame = new int[EntityRoomLoader.MAX_ENTITIES];

    final int[] enemyFlashCountdown = new int[EntityRoomLoader.MAX_ENTITIES];

    final int[] enemyIgnoreHitsCountdown = new int[EntityRoomLoader.MAX_ENTITIES];

    final int[] enemyHitboxFlags = new int[EntityRoomLoader.MAX_ENTITIES];

    final int[] warpState = new int[EntityRoomLoader.MAX_ENTITIES];

    final int[] warpTransitionCountdown = new int[EntityRoomLoader.MAX_ENTITIES];

    final int[] entityGroundStatus = new int[EntityRoomLoader.MAX_ENTITIES];

    final int[] fallingTargetX = new int[EntityRoomLoader.MAX_ENTITIES];

    final int[] fallingTargetY = new int[EntityRoomLoader.MAX_ENTITIES];

    final int[] fallingSpeedX = new int[EntityRoomLoader.MAX_ENTITIES];

    final int[] fallingSpeedY = new int[EntityRoomLoader.MAX_ENTITIES];

    final int[] fallingSpeedXAccumulator = new int[EntityRoomLoader.MAX_ENTITIES];

    final int[] fallingSpeedYAccumulator = new int[EntityRoomLoader.MAX_ENTITIES];

    final int[] fallingVisualYOffset = new int[EntityRoomLoader.MAX_ENTITIES];

    final int[] baseEntityFlipAttribute = new int[EntityRoomLoader.MAX_ENTITIES];

    final int[] entityOptions1Override = new int[EntityRoomLoader.MAX_ENTITIES];

    final boolean[] enemyProjectileSpawnedThisFrame =
        new boolean[EntityRoomLoader.MAX_ENTITIES];

    final boolean[] dynamicEntitySpawnedThisFrame =
        new boolean[EntityRoomLoader.MAX_ENTITIES];

    List<HookshotChainOam.Entry> hookshotChainOam = List.of();

    List<PincerBodyOam.Entry> pincerBodyOam = List.of();

    List<WingedOctorokOam.Entry> wingedOctorokOam = List.of();

    final boolean[] wingedSwordAttackThisFrame =
        new boolean[EntityRoomLoader.MAX_ENTITIES];

    final boolean[] wingedStateTwoThisFrame =
        new boolean[EntityRoomLoader.MAX_ENTITIES];

    final int[] liftedPhase = new int[EntityRoomLoader.MAX_ENTITIES];

    final int[] liftedSourceDirection = new int[EntityRoomLoader.MAX_ENTITIES];

    final boolean[] liftedStateInitialized = new boolean[EntityRoomLoader.MAX_ENTITIES];

    final int[] bombDirection = new int[EntityRoomLoader.MAX_ENTITIES];

    final boolean[] playerArrowBombArrow =
        new boolean[EntityRoomLoader.MAX_ENTITIES];

    final int[] bombPrivateCountdown1 = new int[EntityRoomLoader.MAX_ENTITIES];

    final int[] bombPrivateCountdown3 = new int[EntityRoomLoader.MAX_ENTITIES];

    final int[] liftableRockSmashCountdown =
        new int[EntityRoomLoader.MAX_ENTITIES];

    final int[] liftableRockSmashSourceVariant =
        new int[EntityRoomLoader.MAX_ENTITIES];

    final boolean[] liftableRockSmashActive =
        new boolean[EntityRoomLoader.MAX_ENTITIES];

    final int[] bombPrivateState4 = new int[EntityRoomLoader.MAX_ENTITIES];

    final int[] magicPowderPrivateState4 = new int[EntityRoomLoader.MAX_ENTITIES];

    final int[] entityPrivateState4 = new int[EntityRoomLoader.MAX_ENTITIES];

    final int[] entityInertia = new int[EntityRoomLoader.MAX_ENTITIES];

    final int[] entityUnknownJ = new int[EntityRoomLoader.MAX_ENTITIES];

    final boolean[] placedBombMotionInitialized =
        new boolean[EntityRoomLoader.MAX_ENTITIES];

    final boolean[] enemyBombMotionInitialized =
        new boolean[EntityRoomLoader.MAX_ENTITIES];

    final int[] thrownDirection = new int[EntityRoomLoader.MAX_ENTITIES];

    final int[] ledgeTransitionTimer = new int[EntityRoomLoader.MAX_ENTITIES];

    final boolean[] thrownMotionInitialized = new boolean[EntityRoomLoader.MAX_ENTITIES];

    final int[] chestSpeedY = new int[EntityRoomLoader.MAX_ENTITIES];

    final int[] chestSpeedYAccumulator = new int[EntityRoomLoader.MAX_ENTITIES];

    final int[] chestInertia = new int[EntityRoomLoader.MAX_ENTITIES];

    final int[] chestItemBySlot = new int[EntityRoomLoader.MAX_ENTITIES];

    boolean switchBlockAnimationActive;

    boolean pendingSwitchBlockAnimationRequest;

    ColorShellWorld colorShellWorld = ColorShellWorld.none();

    int pendingClearedEntityMask;

    int pendingRoomStatusMask;

    EnemyDropResolver enemyDropResolver;

    EnemyDropResolver.CounterState enemyDropCounters =
        new EnemyDropResolver.CounterState(0, 0);

    int enemyDropMaxHearts = 3;

    int enemyDropHealth = 6;

    boolean enemyDropActivePowerUp;

    boolean enemyDropBossBattle;

    int killCount;

    public int booBuddyTriggerCount;

    public int bgPaletteEffectAddress;

    public int paletteDataFlags;

    int defaultMusicTrack = 0x05;

    final int[] killOrder = new int[0x100];

    public boolean shouldGetLostInMysteriousWoods;

    boolean activeMusic;

    public int bowWowState;

    // The three WRAM values read by PolsVoiceEntityHandler's Ocarina preamble.
    // RoomSession supplies these from the active Ocarina item state once per
    // entity tick; the zero values preserve the ordinary no-song path.

    // LinkMotionMapFadeInHandler leaves wTransitionSequenceCounter at $04 once
    // the active room is ready for interaction. Entity ticks are gated during
    // the host transition, so this is the source value visible to gameplay.
    int transitionSequenceCounter = 0x04;

    // ReturnIfNonInteractive_19's gameplay selector and room-transition flag.
    // Live room sessions default to the ordinary active world path; tests can
    // select the world-map/transition branches explicitly.
    boolean gameplayWorld = true;

    boolean roomTransitionActive;

    int swordMoblinAlertingSoundCounter;

    int bombArrowCooldown;

    int latestDroppedBombEntityIndex = -1;

    int latestShotArrowEntityIndex = -1;

    boolean lastArrowShotPlayedWhoosh;

    boolean lastBombPlacementPlayedBump;

    // hLinkSpeedX/Y are written by Link before AnimateEntities. These are set
    // only around the live projectile tick so the enemy-bomb branch can mirror
    // its post-collision SLA writes without making Link state part of the
    // entity snapshot.

    List<BowWowChain.Entry> bowWowChainOam = List.of();

    int chestShieldLevel = 1;

    int chestSwordLevel = 1;

    boolean chestPlayerLevelsKnown;

    int chestPowerBraceletLevel = 1;

    int entityGoldenLeavesCount;

    int pendingMusicTrack = -1;

    int pendingMusicFadeOutCountdown;

    final int[] swordPickupState = new int[EntityRoomLoader.MAX_ENTITIES];

    public final boolean[] swordPickupSequenceActive =
        new boolean[EntityRoomLoader.MAX_ENTITIES];

    final int[] powerUpPickupCountdown = new int[EntityRoomLoader.MAX_ENTITIES];

    public final boolean[] toadstoolPickupSequenceActive =
        new boolean[EntityRoomLoader.MAX_ENTITIES];

    public final boolean[] instrumentPickupSequenceActive =
        new boolean[EntityRoomLoader.MAX_ENTITIES];

    public final int[] instrumentPickupState =
        new int[EntityRoomLoader.MAX_ENTITIES];

    final int[] instrumentBackgroundFade = new int[EntityRoomLoader.MAX_ENTITIES];

    final int[] instrumentObjectFade = new int[EntityRoomLoader.MAX_ENTITIES];

    final int[] instrumentHintBatches = new int[EntityRoomLoader.MAX_ENTITIES];

    final int[] instrumentWarpX = new int[EntityRoomLoader.MAX_ENTITIES];

    final int[] instrumentWarpY = new int[EntityRoomLoader.MAX_ENTITIES];

    public final HeartPiecePickupMotion[] heartPiecePickup = new HeartPiecePickupMotion[EntityRoomLoader.MAX_ENTITIES];

    int heartPiecesCount;

    final int[] heartPieceMeterPairs = new int[20];

    public final int[] instrumentPaletteCycle = new int[32];

    final int[] instrumentWarpSprites = new int[256];

    final int[] instrumentHintTiles = new int[20 * 18];

    final int[] instrumentHintAttributes = new int[20 * 18];

    final int[] instrumentWarpFadeStep = new int[EntityRoomLoader.MAX_ENTITIES];

    final int[] instrumentHintCountdown = new int[EntityRoomLoader.MAX_ENTITIES];

    final boolean[] instrumentHintOpened = new boolean[EntityRoomLoader.MAX_ENTITIES];

    final boolean[] instrumentPickupSparklesPending =
        new boolean[EntityRoomLoader.MAX_ENTITIES];

    public final int[] instrumentParticleKind =
        new int[EntityRoomLoader.MAX_ENTITIES];

    final int[] instrumentParticleSpawnCountdown =
        new int[EntityRoomLoader.MAX_ENTITIES];

    final int[] instrumentParticleSpawnCounter =
        new int[EntityRoomLoader.MAX_ENTITIES];

    final int[] instrumentParticleSpeedX =
        new int[EntityRoomLoader.MAX_ENTITIES];

    final int[] instrumentParticleSpeedY =
        new int[EntityRoomLoader.MAX_ENTITIES];

    final int[] instrumentParticleXAccumulator =
        new int[EntityRoomLoader.MAX_ENTITIES];

    final int[] instrumentParticleYAccumulator =
        new int[EntityRoomLoader.MAX_ENTITIES];

    final int[] kidKidnapState = new int[EntityRoomLoader.MAX_ENTITIES];

    final int[] kidKidnapDelay = new int[EntityRoomLoader.MAX_ENTITIES];

    final int[] kidSpeedXAccumulator = new int[EntityRoomLoader.MAX_ENTITIES];

    final int[] kidSpeedYAccumulator = new int[EntityRoomLoader.MAX_ENTITIES];

    final int[] richardState = new int[EntityRoomLoader.MAX_ENTITIES];

    final int[] richardDirection = new int[EntityRoomLoader.MAX_ENTITIES];

    final int[] richardXAccumulator = new int[EntityRoomLoader.MAX_ENTITIES];

    final int[] richardInertia = new int[EntityRoomLoader.MAX_ENTITIES];

    public boolean playerHasToadstool;

    public int playerMagicPowderCount;

    boolean playerHasTailKey;

    int instrument1Flags;

    int instrument2Flags;

    /** AnimateEntities copies wC111 to wC1A8 before decrementing it. */
    int entityInteractionCountdown;

    boolean freeMovementMode;

    final int[] owlEventState = new int[EntityRoomLoader.MAX_ENTITIES];

    /** wEntitiesPrivateCountdown1Table for the owl-event handler. */
    final int[] owlPrivateCountdown1 =
        new int[EntityRoomLoader.MAX_ENTITIES];

    final int[] owlSpeedX = new int[EntityRoomLoader.MAX_ENTITIES];

    final int[] owlSpeedY = new int[EntityRoomLoader.MAX_ENTITIES];

    final int[] owlSpeedZ = new int[EntityRoomLoader.MAX_ENTITIES];

    final int[] owlXAccumulator = new int[EntityRoomLoader.MAX_ENTITIES];

    final int[] owlYAccumulator = new int[EntityRoomLoader.MAX_ENTITIES];

    final int[] owlZAccumulator = new int[EntityRoomLoader.MAX_ENTITIES];

    final int[] owlBoomerangSfxCounter = new int[EntityRoomLoader.MAX_ENTITIES];

    final int[] owlPreviousMusicTrack = new int[EntityRoomLoader.MAX_ENTITIES];

    final boolean[] owlCompletionEmitted =
        new boolean[EntityRoomLoader.MAX_ENTITIES];

    IntUnaryOperator owlDialogResolver = roomId -> 0x0D9;

    int owlStatueDialogId = -1;

    IntUnaryOperator owlDefaultMusicResolver = roomId -> 0x05;

    boolean groundInteractionSideScrolling;

    int entityMapId = -1;

    int entityRoomId = -1;

    boolean dungeonMinibossReady;

    int entityRoomStatus;

    // These latches represent the HRAM values read by
    // DroppableRevealOrReturnIfNeeded's tree-shell branch. RoomSession feeds
    // them from Link's live Pegasus-Boots collision record before dispatch.
    public boolean secretSeashellScreenShakeActive;

    public boolean secretSeashellPegasusCollisionActive;

    public boolean secretSeashellPegasusCollisionCoordinatesKnown;

    public int secretSeashellPegasusCollisionX;

    public int secretSeashellPegasusCollisionY;

    int liftedEntitySlot = -1;

    int liftedCarryState;

    int liftedEffectiveDirection;

    int liftedLinkC13B;

    RoomEntityObjectQuery objectQuery;

    RoomEntityObjectQuery objectIntersectionQuery;

    final BoomerangMotion boomerangMotion = new BoomerangMotion();

    final MagicRodFireballMotion magicRodFireballMotion =
        new MagicRodFireballMotion();

    final SwordBeamMotion swordBeamMotion = new SwordBeamMotion();

    final int[] magicPowderState = new int[EntityRoomLoader.MAX_ENTITIES];

    int torchPaletteTargetAddress;

    boolean torchPaletteTransitionActive;

    final int[] musicalNoteInertia = new int[EntityRoomLoader.MAX_ENTITIES];

    final int[] musicalNoteSpeedX = new int[EntityRoomLoader.MAX_ENTITIES];

    final int[] musicalNoteSpeedY = new int[EntityRoomLoader.MAX_ENTITIES];

    int boomerangSfxCounter;

    boolean boomerangSoundClockProvided;
}
