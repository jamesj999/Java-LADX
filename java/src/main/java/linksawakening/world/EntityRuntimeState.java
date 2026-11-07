package linksawakening.world;

import linksawakening.world.entity.event.BoomerangObjectRequest;
import linksawakening.world.entity.event.ChestRewardEvent;
import linksawakening.world.entity.event.DialogRequest;
import linksawakening.world.entity.event.DungeonWarpRequest;
import linksawakening.world.entity.event.HeartContainerRewardEvent;
import linksawakening.world.entity.event.HinoxLinkEffectRequest;
import linksawakening.world.entity.event.HookshotBridgeUpdate;
import linksawakening.world.entity.event.InstrumentCompletionEvent;
import linksawakening.world.entity.event.InstrumentRewardEvent;
import linksawakening.world.entity.event.KeyQuicksandEvent;
import linksawakening.world.entity.event.KeyRewardEvent;
import linksawakening.world.entity.event.LikeLikeEvent;
import linksawakening.world.entity.event.LinkAttackClearRequest;
import linksawakening.world.entity.event.LinkAttackStepRequest;
import linksawakening.world.entity.event.LinkFacingRequest;
import linksawakening.world.entity.event.LinkFallenPoseRequest;
import linksawakening.world.entity.event.LinkFinalPositionRequest;
import linksawakening.world.entity.event.LinkHeldItemPoseRequest;
import linksawakening.world.entity.event.LinkMotionBlockRequest;
import linksawakening.world.entity.event.LinkSwordFinalPoseRequest;
import linksawakening.world.entity.event.LinkSwordSpinPoseRequest;
import linksawakening.world.entity.event.MadamMeowMeowFullHealRequest;
import linksawakening.world.entity.event.MagicPowderObjectRequest;
import linksawakening.world.entity.event.MagicRodObjectRequest;
import linksawakening.world.entity.event.OwlEventCompletion;
import linksawakening.world.entity.event.RichardProgressEvent;
import linksawakening.world.entity.event.RoomStatusPersistenceRequest;
import linksawakening.world.entity.event.RoosterLinkStateRequest;
import linksawakening.world.entity.event.ScreenShakeRequest;
import linksawakening.world.entity.event.SideViewPlatformLinkRequest;
import linksawakening.world.entity.event.SideViewPotLinkRequest;
import linksawakening.world.entity.event.SlimeKeyRewardEvent;
import linksawakening.world.entity.event.SwordPickupRewardEvent;
import linksawakening.world.entity.event.ThrownBackgroundCollisionEvent;
import linksawakening.world.entity.event.ToadstoolRewardEvent;
import linksawakening.world.entity.event.TransientVfxRequest;
import linksawakening.world.entity.event.WarpLinkStateRequest;
import linksawakening.world.entity.event.WitchExchangeEvent;
import linksawakening.world.entity.event.WitchRewardEvent;

import static linksawakening.world.EntityMotionMath.*;
import static linksawakening.world.EntitySnapshots.*;
import static linksawakening.world.EntityTypeRules.*;
import linksawakening.entity.EntitySpriteSelection;
import linksawakening.entity.EntitySpriteDefinition;
import linksawakening.entity.EntitySpriteHandlerCatalog;
import linksawakening.gpu.EntitySpriteTileSnapshot;
import linksawakening.rom.RomTables;
import linksawakening.vfx.TransientVfxType;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Objects;
import java.util.function.IntSupplier;
import java.util.function.IntUnaryOperator;
import static linksawakening.world.RoomEntityRuntime.*;

/** ROM slot arrays and room handler state; orchestration remains in RoomEntityRuntime. */
abstract class EntityRuntimeState extends EntityRomConstants {
    final EntityOutbox outbox = new EntityOutbox();

    EntitySpriteSelection spriteSelection;

    RoomEntityGroundInteraction groundInteraction =
        (entity, frameCounter, previousGroundStatus, speedZ, sideScrolling) ->
            RoomEntityGroundInteraction.Result.unchanged(entity, 0);

    RoomEntityBackgroundInteraction backgroundInteraction;

    FollowingNpcState followingNpcState = FollowingNpcState.none();

    LinkPositionHistory followingLinkPositionHistory = new LinkPositionHistory();

    int followingLinkZ;

    int followingLinkDirection;

    int lastRomLinkDirection;

    int followingEntityYOffset;

    int linkAttackStepAnimationCountdown;

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

    final int[] witchGotItemCountdown = new int[EntityRoomLoader.MAX_ENTITIES];

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

    final int[] droppablePrivateState3 = new int[EntityRoomLoader.MAX_ENTITIES];

    final int[] droppablePrivateState4 = new int[EntityRoomLoader.MAX_ENTITIES];

    final boolean[] enemyDropActive = new boolean[EntityRoomLoader.MAX_ENTITIES];

    final EnemyDropMotion enemyDropMotion = new EnemyDropMotion();

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

    final List<RoamingEnemyMotion.LaunchRequest> projectileLaunchRequests =
        outbox.channel(RoamingEnemyMotion.LaunchRequest.class, "projectileLaunchRequests");

    final List<TransientVfxRequest> transientVfxRequests = outbox.channel(TransientVfxRequest.class, "transientVfxRequests");

    final List<HeartContainerRewardEvent> pendingHeartContainerRewards = outbox.channel(HeartContainerRewardEvent.class, "pendingHeartContainerRewards");

    final List<MadamMeowMeowFullHealRequest> pendingMadamMeowMeowFullHealRequests = outbox.channel(MadamMeowMeowFullHealRequest.class, "pendingMadamMeowMeowFullHealRequests");

    final List<SwordPickupRewardEvent> pendingSwordPickupRewards = outbox.channel(SwordPickupRewardEvent.class, "pendingSwordPickupRewards");

    final List<ToadstoolRewardEvent> pendingToadstoolRewards = outbox.channel(ToadstoolRewardEvent.class, "pendingToadstoolRewards");

    final List<InstrumentRewardEvent> pendingInstrumentRewards = outbox.channel(InstrumentRewardEvent.class, "pendingInstrumentRewards");

    final List<InstrumentCompletionEvent> pendingInstrumentCompletions = outbox.channel(InstrumentCompletionEvent.class, "pendingInstrumentCompletions");

    final List<RichardProgressEvent> pendingRichardProgressEvents = outbox.channel(RichardProgressEvent.class, "pendingRichardProgressEvents");

    final List<OwlEventCompletion> pendingOwlEventCompletions = outbox.channel(OwlEventCompletion.class, "pendingOwlEventCompletions");

    final List<LinkFinalPositionRequest> pendingLinkFinalPositionRequests = outbox.channel(LinkFinalPositionRequest.class, "pendingLinkFinalPositionRequests");

    final List<RoosterLinkStateRequest> pendingRoosterLinkStateRequests = outbox.channel(RoosterLinkStateRequest.class, "pendingRoosterLinkStateRequests");

    final List<SideViewPlatformLinkRequest> pendingSideViewPlatformLinkRequests = outbox.channel(SideViewPlatformLinkRequest.class, "pendingSideViewPlatformLinkRequests");

    final List<SideViewPotLinkRequest> pendingSideViewPotLinkRequests = outbox.channel(SideViewPotLinkRequest.class, "pendingSideViewPotLinkRequests");

    final List<LinkMotionBlockRequest> pendingLinkMotionBlockRequests = outbox.channel(LinkMotionBlockRequest.class, "pendingLinkMotionBlockRequests");

    final List<HinoxLinkEffectRequest> pendingHinoxLinkEffectRequests = outbox.channel(HinoxLinkEffectRequest.class, "pendingHinoxLinkEffectRequests");

    final List<LinkFallenPoseRequest> pendingLinkFallenPoseRequests = outbox.channel(LinkFallenPoseRequest.class, "pendingLinkFallenPoseRequests");

    final List<LinkFacingRequest> pendingLinkFacingRequests = outbox.channel(LinkFacingRequest.class, "pendingLinkFacingRequests");

    final List<LinkAttackStepRequest> pendingLinkAttackStepRequests = outbox.channel(LinkAttackStepRequest.class, "pendingLinkAttackStepRequests");

    final List<LinkAttackStepRequest> pendingPreTickLinkAttackStepRequests = outbox.channel(LinkAttackStepRequest.class, "pendingPreTickLinkAttackStepRequests");

    final List<LinkAttackClearRequest> pendingLinkAttackClearRequests = outbox.channel(LinkAttackClearRequest.class, "pendingLinkAttackClearRequests");

    final List<RoomStatusPersistenceRequest> pendingRoomStatusPersistenceRequests = outbox.channel(RoomStatusPersistenceRequest.class, "pendingRoomStatusPersistenceRequests");

    final List<LinkHeldItemPoseRequest> pendingLinkHeldItemPoseRequests = outbox.channel(LinkHeldItemPoseRequest.class, "pendingLinkHeldItemPoseRequests");

    final List<LinkSwordSpinPoseRequest> pendingLinkSwordSpinPoseRequests = outbox.channel(LinkSwordSpinPoseRequest.class, "pendingLinkSwordSpinPoseRequests");

    final List<LinkSwordFinalPoseRequest> pendingLinkSwordFinalPoseRequests = outbox.channel(LinkSwordFinalPoseRequest.class, "pendingLinkSwordFinalPoseRequests");

    final List<ScreenShakeRequest> pendingScreenShakeRequests = outbox.channel(ScreenShakeRequest.class, "pendingScreenShakeRequests");

    final List<DialogRequest> pendingDialogRequests = outbox.channel(DialogRequest.class, "pendingDialogRequests");

    final List<EntityCombatEvent> pendingEntityEvents = outbox.channel(EntityCombatEvent.class, "pendingEntityEvents");

    final List<EntityCombatEvent> pendingPreTickEntityEvents = outbox.channel(EntityCombatEvent.class, "pendingPreTickEntityEvents");

    final List<ThrownBackgroundCollisionEvent> pendingThrownBackgroundCollisions = outbox.channel(ThrownBackgroundCollisionEvent.class, "pendingThrownBackgroundCollisions");

    final List<DungeonWarpRequest> pendingDungeonWarpRequests = outbox.channel(DungeonWarpRequest.class, "pendingDungeonWarpRequests");

    final List<WarpLinkStateRequest> pendingWarpLinkStateRequests = outbox.channel(WarpLinkStateRequest.class, "pendingWarpLinkStateRequests");

    final List<ChestRewardEvent> pendingChestRewardEvents = outbox.channel(ChestRewardEvent.class, "pendingChestRewardEvents");

    final List<KeyRewardEvent> pendingKeyRewardEvents = outbox.channel(KeyRewardEvent.class, "pendingKeyRewardEvents");

    final List<SlimeKeyRewardEvent> pendingSlimeKeyRewardEvents = outbox.channel(SlimeKeyRewardEvent.class, "pendingSlimeKeyRewardEvents");

    final List<WitchExchangeEvent> pendingWitchExchangeEvents = outbox.channel(WitchExchangeEvent.class, "pendingWitchExchangeEvents");

    final List<WitchRewardEvent> pendingWitchRewardEvents = outbox.channel(WitchRewardEvent.class, "pendingWitchRewardEvents");

    final List<KeyQuicksandEvent> pendingKeyQuicksandEvents = outbox.channel(KeyQuicksandEvent.class, "pendingKeyQuicksandEvents");

    final List<LikeLikeEvent> pendingLikeLikeEvents = outbox.channel(LikeLikeEvent.class, "pendingLikeLikeEvents");

    final List<BombExplosionEvent> pendingBombExplosionEvents = outbox.channel(BombExplosionEvent.class, "pendingBombExplosionEvents");

    final List<HookshotBridgeUpdate> hookshotBridgeUpdates = outbox.channel(HookshotBridgeUpdate.class, "hookshotBridgeUpdates");

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

    int booBuddyTriggerCount;

    int bgPaletteEffectAddress;

    int paletteDataFlags;

    int defaultMusicTrack = 0x05;

    final int[] killOrder = new int[0x100];

    boolean actionButtonsHeld;

    boolean actionButtonAHeld;

    boolean actionButtonBHeld;

    boolean joypadHeld;

    boolean dialogActive;

    boolean inventoryAppearing;

    int dialogCooldown;

    int windowY = 0x80;

    int dialogAskSelectionIndex;

    boolean shouldGetLostInMysteriousWoods;

    boolean activeMusic;

    int bowWowState;

    int linkPressedButtonsMask;

    int linkItemA;

    int linkItemB;

    boolean linkInventoryConfigured;

    boolean powerBraceletButtonHeld;

    boolean bombButtonHeld;

    boolean runningWithPegasusBoots;

    // The three WRAM values read by PolsVoiceEntityHandler's Ocarina preamble.
    // RoomSession supplies these from the active Ocarina item state once per
    // entity tick; the zero values preserve the ordinary no-song path.
    int linkPlayingOcarinaCountdown;

    int ocarinaSongFlags;

    int selectedSongIndex;

    int ocarinaAnimationCounter;

    int ocarinaAnimationPhase;

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
    int currentLinkVelocityZ;

    List<BowWowChain.Entry> bowWowChainOam = List.of();

    int currentLinkSpeedX;

    int currentLinkSpeedY;

    int currentLinkFinalPositionX;

    boolean currentLinkFinalPositionXConfigured;

    int chestShieldLevel = 1;

    int chestSwordLevel = 1;

    boolean chestPlayerLevelsKnown;

    int chestPowerBraceletLevel = 1;

    int entityGoldenLeavesCount;

    int pendingMusicTrack = -1;

    int pendingMusicFadeOutCountdown;

    final int[] swordPickupState = new int[EntityRoomLoader.MAX_ENTITIES];

    final boolean[] swordPickupSequenceActive =
        new boolean[EntityRoomLoader.MAX_ENTITIES];

    final int[] powerUpPickupCountdown = new int[EntityRoomLoader.MAX_ENTITIES];

    final boolean[] toadstoolPickupSequenceActive =
        new boolean[EntityRoomLoader.MAX_ENTITIES];

    final boolean[] instrumentPickupSequenceActive =
        new boolean[EntityRoomLoader.MAX_ENTITIES];

    final int[] instrumentPickupState =
        new int[EntityRoomLoader.MAX_ENTITIES];

    final int[] instrumentBackgroundFade = new int[EntityRoomLoader.MAX_ENTITIES];

    final int[] instrumentObjectFade = new int[EntityRoomLoader.MAX_ENTITIES];

    final int[] instrumentHintBatches = new int[EntityRoomLoader.MAX_ENTITIES];

    final int[] instrumentWarpX = new int[EntityRoomLoader.MAX_ENTITIES];

    final int[] instrumentWarpY = new int[EntityRoomLoader.MAX_ENTITIES];

    final HeartPiecePickupMotion[] heartPiecePickup = new HeartPiecePickupMotion[EntityRoomLoader.MAX_ENTITIES];

    final List<HeartPiecePickupMotion.Update> heartPieceUpdates =
        outbox.channel(HeartPiecePickupMotion.Update.class, "heartPieceUpdates");

    int heartPiecesCount;

    final int[] heartPieceMeterPairs = new int[20];

    final int[] instrumentPaletteCycle = new int[32];

    final int[] instrumentWarpSprites = new int[256];

    final int[] instrumentHintTiles = new int[20 * 18];

    final int[] instrumentHintAttributes = new int[20 * 18];

    final int[] instrumentWarpFadeStep = new int[EntityRoomLoader.MAX_ENTITIES];

    final int[] instrumentHintCountdown = new int[EntityRoomLoader.MAX_ENTITIES];

    final boolean[] instrumentHintOpened = new boolean[EntityRoomLoader.MAX_ENTITIES];

    final boolean[] instrumentPickupSparklesPending =
        new boolean[EntityRoomLoader.MAX_ENTITIES];

    final int[] instrumentParticleKind =
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

    boolean playerHasToadstool;

    int playerMagicPowderCount;

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
    boolean secretSeashellScreenShakeActive;

    boolean secretSeashellPegasusCollisionActive;

    boolean secretSeashellPegasusCollisionCoordinatesKnown;

    int secretSeashellPegasusCollisionX;

    int secretSeashellPegasusCollisionY;

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

    final List<BoomerangObjectRequest> boomerangObjectRequests = outbox.channel(BoomerangObjectRequest.class, "boomerangObjectRequests");

    final List<MagicRodObjectRequest> magicRodObjectRequests = outbox.channel(MagicRodObjectRequest.class, "magicRodObjectRequests");

    final int[] magicPowderState = new int[EntityRoomLoader.MAX_ENTITIES];

    int torchPaletteTargetAddress;

    boolean torchPaletteTransitionActive;

    final List<MagicPowderObjectRequest> magicPowderObjectRequests = outbox.channel(MagicPowderObjectRequest.class, "magicPowderObjectRequests");

    final int[] musicalNoteInertia = new int[EntityRoomLoader.MAX_ENTITIES];

    final int[] musicalNoteSpeedX = new int[EntityRoomLoader.MAX_ENTITIES];

    final int[] musicalNoteSpeedY = new int[EntityRoomLoader.MAX_ENTITIES];

    int boomerangSfxCounter;

    boolean boomerangSoundClockProvided;
}
