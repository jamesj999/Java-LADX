package linksawakening.world.entity;

import linksawakening.world.entity.PincerBodyOam;
import linksawakening.world.entity.WingedOctorokOam;
import linksawakening.world.entity.LinkPositionHistory;
import linksawakening.world.entity.event.BombExplosionEvent;
import linksawakening.world.entity.event.EntityCombatEvent;
import linksawakening.world.entity.HookshotChainOam;
import linksawakening.world.entity.BowWowChain;

import linksawakening.world.entity.event.HeartPiecePickupUpdate;

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

import static linksawakening.world.entity.EntityMotionMath.*;
import static linksawakening.world.entity.EntitySnapshots.*;
import static linksawakening.world.entity.EntityTypeRules.*;
import linksawakening.entity.EntitySpriteSelection;
import java.util.List;
import java.util.function.IntUnaryOperator;
import static linksawakening.world.entity.RoomEntityRuntime.*;

/** ROM slot arrays and room handler state; orchestration remains in RoomEntityRuntime. */
abstract class EntityRuntimeState extends EntityRomConstants {
    final EntityOutbox outbox = new EntityOutbox();

    public EntitySpriteSelection spriteSelection;

    public RoomEntityGroundInteraction groundInteraction =
        (entity, frameCounter, previousGroundStatus, speedZ, sideScrolling) ->
            RoomEntityGroundInteraction.Result.unchanged(entity, 0);

    public RoomEntityBackgroundInteraction backgroundInteraction;

    public FollowingNpcState followingNpcState = FollowingNpcState.none();

    LinkPositionHistory followingLinkPositionHistory = new LinkPositionHistory();

    int followingLinkZ;

    int followingLinkDirection;

    int lastRomLinkDirection;

    int followingEntityYOffset;

    public int linkAttackStepAnimationCountdown;

    final ButterflyMotion butterflyMotion = new ButterflyMotion();

    public final SideViewPlatformMotion sideViewPlatformMotion = new SideViewPlatformMotion();

    public final SideViewPotMotion sideViewPotMotion = new SideViewPotMotion();

    final KeeseMotion keeseMotion = new KeeseMotion();

    public final RoamingEnemyMotion roamingEnemyMotion = new RoamingEnemyMotion();

    public final UnmaskedIronMaskMotion unmaskedIronMaskMotion =
        new UnmaskedIronMaskMotion();

    public final MoblinSwordMotion moblinSwordMotion = new MoblinSwordMotion();

    public final PlayerArrowMotion playerArrowMotion = new PlayerArrowMotion();

    public final EnemyProjectileMotion enemyProjectileMotion = new EnemyProjectileMotion();

    public final HookshotChainMotion hookshotChainMotion = new HookshotChainMotion();

    final HookshotBridgeMotion hookshotBridgeMotion = new HookshotBridgeMotion();

    final LaserMotion laserMotion = new LaserMotion();

    final TektiteMotion tektiteMotion = new TektiteMotion();

    final LeeverMotion leeverMotion = new LeeverMotion();

    final AntiFairyMotion antiFairyMotion = new AntiFairyMotion();

    public final SparkMotion sparkMotion = new SparkMotion();

    public final ZolGelMotion zolGelMotion = new ZolGelMotion();

    public final HidingZolMotion hidingZolMotion = new HidingZolMotion();

    public final StarMotion starMotion = new StarMotion();

    public final BlooperMotion blooperMotion = new BlooperMotion();

    public final WingedOctorokMotion wingedOctorokMotion = new WingedOctorokMotion();

    public final PincerMotion pincerMotion = new PincerMotion();

    public final BushCrawlerMotion bushCrawlerMotion = new BushCrawlerMotion();

    public final MimicMotion mimicMotion = new MimicMotion();

    public final MaskedMimicMotion maskedMimicMotion = new MaskedMimicMotion();

    public final MiniMoldormMotion miniMoldormMotion = new MiniMoldormMotion();

    public final MoldormMotion moldormMotion = new MoldormMotion();

    public final CuccoMotion cuccoMotion = new CuccoMotion();

    public final GiantGopongaMotion giantGopongaMotion = new GiantGopongaMotion();

    public final GopongaProjectileMotion gopongaProjectileMotion =
        new GopongaProjectileMotion();

    public final PokeyMotion pokeyMotion = new PokeyMotion();

    public final PiranhaMotion piranhaMotion = new PiranhaMotion();

    public final ZoraMotion zoraMotion = new ZoraMotion();

    public final ZombieMotion zombieMotion = new ZombieMotion();

    public final BuzzBlobMotion buzzBlobMotion = new BuzzBlobMotion();

    public final SandCrabMotion sandCrabMotion = new SandCrabMotion();

    public final DogMotion dogMotion = new DogMotion();

    public final UrchinMotion urchinMotion = new UrchinMotion();

    public final WitchRatMotion witchRatMotion = new WitchRatMotion();

    public final WitchMotion witchMotion = new WitchMotion();

    public final int[] witchGotItemCountdown = new int[EntityRoomLoader.MAX_ENTITIES];

    public final TarinRaccoonMotion tarinRaccoonMotion = new TarinRaccoonMotion();

    public final SpikeTrapMotion spikeTrapMotion = new SpikeTrapMotion();

    public final PairoddMotion pairoddMotion = new PairoddMotion();

    public final PairoddProjectileMotion pairoddProjectileMotion =
        new PairoddProjectileMotion();

    public final WaterTektiteMotion waterTektiteMotion = new WaterTektiteMotion();

    public final FishMotion fishMotion = new FishMotion();

    public final CrowMotion crowMotion = new CrowMotion();

    public final BooBuddyMotion booBuddyMotion = new BooBuddyMotion();

    public final FairyMotion fairyMotion = new FairyMotion();

    public final StalfosAggressiveMotion stalfosAggressiveMotion =
        new StalfosAggressiveMotion();

    public final StalfosEvasiveMotion stalfosEvasiveMotion = new StalfosEvasiveMotion();

    final GibdoMotion gibdoMotion = new GibdoMotion();

    public final LikeLikeMotion likeLikeMotion = new LikeLikeMotion();

    public final GoombaMotion goombaMotion = new GoombaMotion();

    public final SnakeMotion snakeMotion = new SnakeMotion();

    public final WizrobeMotion wizrobeMotion = new WizrobeMotion();

    public final WizrobeProjectileMotion wizrobeProjectileMotion =
        new WizrobeProjectileMotion();

    public final PeaHatMotion peaHatMotion = new PeaHatMotion();

    public final ArmosMotion armosMotion = new ArmosMotion();

    public final ArmosKnightMotion armosKnightMotion = new ArmosKnightMotion();

    public final HinoxMotion hinoxMotion = new HinoxMotion();

    public final RollingBonesMotion rollingBonesMotion = new RollingBonesMotion();

    public final ThreeOfAKindMotion threeOfAKindMotion = new ThreeOfAKindMotion();

    public final MoblinKingMotion moblinKingMotion = new MoblinKingMotion();

    public final BossIntroMotion bossIntroMotion = new BossIntroMotion();

    final GenieMotion genieMotion = new GenieMotion();

    public final int[] genieActiveState = new int[EntityRoomLoader.MAX_ENTITIES];

    public final int[] geniePrivateState1 = new int[EntityRoomLoader.MAX_ENTITIES];

    final int[] geniePrivateState2 = new int[EntityRoomLoader.MAX_ENTITIES];

    final int[] geniePrivateState3 = new int[EntityRoomLoader.MAX_ENTITIES];

    public final int[] geniePrivateState4 = new int[EntityRoomLoader.MAX_ENTITIES];

    public final int[] geniePrivateCountdown1 = new int[EntityRoomLoader.MAX_ENTITIES];

    public final int[] geniePrivateCountdown2 = new int[EntityRoomLoader.MAX_ENTITIES];

    final int[] genieDirection = new int[EntityRoomLoader.MAX_ENTITIES];

    final boolean[] genieDeathInitialized = new boolean[EntityRoomLoader.MAX_ENTITIES];

    public final int[] genieSpeedX = new int[EntityRoomLoader.MAX_ENTITIES];

    public final int[] genieSpeedY = new int[EntityRoomLoader.MAX_ENTITIES];

    final int[] genieSpeedZ = new int[EntityRoomLoader.MAX_ENTITIES];

    final int[] genieSpeedXAccumulator = new int[EntityRoomLoader.MAX_ENTITIES];

    final int[] genieSpeedYAccumulator = new int[EntityRoomLoader.MAX_ENTITIES];

    final int[] genieSpeedZAccumulator = new int[EntityRoomLoader.MAX_ENTITIES];

    final boolean[] genieImpactRegisteredThisFrame =
        new boolean[EntityRoomLoader.MAX_ENTITIES];

    public int genieControllerSlot = -1;

    public int genieSharedBodySpeedX;

    public int genieSharedBodySpeedY;

    int bossAgonySfxCountdown;

    int bossAgonySourceSlot;

    public final GhiniMotion ghiniMotion = new GhiniMotion();

    public final HardHatMotion hardHatMotion = new HardHatMotion();

    public final PolsVoiceMotion polsVoiceMotion = new PolsVoiceMotion();

    public final SpikedBeetleMotion spikedBeetleMotion = new SpikedBeetleMotion();

    public final MadBomberMotion madBomberMotion = new MadBomberMotion();

    public final BomberMotion bomberMotion = new BomberMotion();

    public final BombiteMotion bombiteMotion = new BombiteMotion();

    public final EnemyRecoilMotion enemyRecoilMotion = new EnemyRecoilMotion();

    public final FollowingNpcMotion followingNpcMotion = new FollowingNpcMotion();

    public final BowWowMotion bowWowMotion = new BowWowMotion();

    public final ColorShellMotion colorShellMotion = new ColorShellMotion();

    public final SecretSeashellMotion secretSeashellMotion =
        new SecretSeashellMotion();

    final ThrownEntityMotion thrownEntityMotion = new ThrownEntityMotion();

    public final int[] slowTransitionCountdown = new int[EntityRoomLoader.MAX_ENTITIES];

    public final boolean[] slowTimerInitialized = new boolean[EntityRoomLoader.MAX_ENTITIES];

    public final int[] droppedItemBySlot = new int[EntityRoomLoader.MAX_ENTITIES];

    final int[] dropPrivateCountdown1 = new int[EntityRoomLoader.MAX_ENTITIES];

    public final int[] dropPrivateCountdown3 = new int[EntityRoomLoader.MAX_ENTITIES];

    public final int[] droppablePrivateState3 = new int[EntityRoomLoader.MAX_ENTITIES];

    final int[] droppablePrivateState4 = new int[EntityRoomLoader.MAX_ENTITIES];

    final boolean[] enemyDropActive = new boolean[EntityRoomLoader.MAX_ENTITIES];

    final EnemyDropMotion enemyDropMotion = new EnemyDropMotion();

    public final int[] enemyTransitionCountdown = new int[EntityRoomLoader.MAX_ENTITIES];

    final int[] bossDeathProducerState = new int[EntityRoomLoader.MAX_ENTITIES];

    final boolean[] rollingBonesDeathInitialized =
        new boolean[EntityRoomLoader.MAX_ENTITIES];

    final int[] moldormDestructionTailState =
        new int[EntityRoomLoader.MAX_ENTITIES];

    final int[] enemyStunnedCountdown = new int[EntityRoomLoader.MAX_ENTITIES];

    public boolean gelClingingToLink;

    public final int[] dyingCountdown = new int[EntityRoomLoader.MAX_ENTITIES];

    final boolean[] powerRecoilDeath = new boolean[EntityRoomLoader.MAX_ENTITIES];

    public final int[] enemyPhysicsFlags = new int[EntityRoomLoader.MAX_ENTITIES];

    public final int[] enemyHealth = new int[EntityRoomLoader.MAX_ENTITIES];

    public final int[] ironMaskPrivateState2 = new int[EntityRoomLoader.MAX_ENTITIES];

    final int[] ironMasksMaskSourceHookshotSlot =
        new int[EntityRoomLoader.MAX_ENTITIES];

    final int[] rollingBonesSwordHitFrame = new int[EntityRoomLoader.MAX_ENTITIES];

    final int[] moblinKingLethalHitFrame = new int[EntityRoomLoader.MAX_ENTITIES];

    public final int[] enemyFlashCountdown = new int[EntityRoomLoader.MAX_ENTITIES];

    public final int[] enemyIgnoreHitsCountdown = new int[EntityRoomLoader.MAX_ENTITIES];

    public final int[] enemyHitboxFlags = new int[EntityRoomLoader.MAX_ENTITIES];

    public final int[] warpState = new int[EntityRoomLoader.MAX_ENTITIES];

    public final int[] warpTransitionCountdown = new int[EntityRoomLoader.MAX_ENTITIES];

    public final int[] entityGroundStatus = new int[EntityRoomLoader.MAX_ENTITIES];

    public final int[] fallingTargetX = new int[EntityRoomLoader.MAX_ENTITIES];

    public final int[] fallingTargetY = new int[EntityRoomLoader.MAX_ENTITIES];

    public final int[] fallingSpeedX = new int[EntityRoomLoader.MAX_ENTITIES];

    public final int[] fallingSpeedY = new int[EntityRoomLoader.MAX_ENTITIES];

    public final int[] fallingSpeedXAccumulator = new int[EntityRoomLoader.MAX_ENTITIES];

    public final int[] fallingSpeedYAccumulator = new int[EntityRoomLoader.MAX_ENTITIES];

    public final int[] fallingVisualYOffset = new int[EntityRoomLoader.MAX_ENTITIES];

    final int[] baseEntityFlipAttribute = new int[EntityRoomLoader.MAX_ENTITIES];

    public final int[] entityOptions1Override = new int[EntityRoomLoader.MAX_ENTITIES];

    final boolean[] enemyProjectileSpawnedThisFrame =
        new boolean[EntityRoomLoader.MAX_ENTITIES];

    final boolean[] dynamicEntitySpawnedThisFrame =
        new boolean[EntityRoomLoader.MAX_ENTITIES];

    List<HookshotChainOam.Entry> hookshotChainOam = List.of();

    List<PincerBodyOam.Entry> pincerBodyOam = List.of();

    List<WingedOctorokOam.Entry> wingedOctorokOam = List.of();

    public final boolean[] wingedSwordAttackThisFrame =
        new boolean[EntityRoomLoader.MAX_ENTITIES];

    public final boolean[] wingedStateTwoThisFrame =
        new boolean[EntityRoomLoader.MAX_ENTITIES];

    final int[] liftedPhase = new int[EntityRoomLoader.MAX_ENTITIES];

    public final int[] liftedSourceDirection = new int[EntityRoomLoader.MAX_ENTITIES];

    final boolean[] liftedStateInitialized = new boolean[EntityRoomLoader.MAX_ENTITIES];

    final int[] bombDirection = new int[EntityRoomLoader.MAX_ENTITIES];

    public final boolean[] playerArrowBombArrow =
        new boolean[EntityRoomLoader.MAX_ENTITIES];

    public final int[] bombPrivateCountdown1 = new int[EntityRoomLoader.MAX_ENTITIES];

    final int[] bombPrivateCountdown3 = new int[EntityRoomLoader.MAX_ENTITIES];

    final int[] liftableRockSmashCountdown =
        new int[EntityRoomLoader.MAX_ENTITIES];

    final int[] liftableRockSmashSourceVariant =
        new int[EntityRoomLoader.MAX_ENTITIES];

    public final boolean[] liftableRockSmashActive =
        new boolean[EntityRoomLoader.MAX_ENTITIES];

    public final int[] bombPrivateState4 = new int[EntityRoomLoader.MAX_ENTITIES];

    final int[] magicPowderPrivateState4 = new int[EntityRoomLoader.MAX_ENTITIES];

    final int[] entityPrivateState4 = new int[EntityRoomLoader.MAX_ENTITIES];

    public final int[] entityInertia = new int[EntityRoomLoader.MAX_ENTITIES];

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

    public final List<RoamingEnemyMotion.LaunchRequest> projectileLaunchRequests =
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

    public final List<HookshotBridgeUpdate> hookshotBridgeUpdates = outbox.channel(HookshotBridgeUpdate.class, "hookshotBridgeUpdates");

    public boolean switchBlockAnimationActive;

    public boolean pendingSwitchBlockAnimationRequest;

    public ColorShellWorld colorShellWorld = ColorShellWorld.none();

    public int pendingClearedEntityMask;

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

    public int defaultMusicTrack = 0x05;

    final int[] killOrder = new int[0x100];

    public boolean actionButtonsHeld;

    public boolean actionButtonAHeld;

    public boolean actionButtonBHeld;

    public boolean joypadHeld;

    public boolean dialogActive;

    public boolean inventoryAppearing;

    public int dialogCooldown;

    public int windowY = 0x80;

    int dialogAskSelectionIndex;

    public boolean shouldGetLostInMysteriousWoods;

    boolean activeMusic;

    public int bowWowState;

    public int linkPressedButtonsMask;

    public int linkItemA;

    public int linkItemB;

    boolean linkInventoryConfigured;

    boolean powerBraceletButtonHeld;

    boolean bombButtonHeld;

    public boolean runningWithPegasusBoots;

    // The three WRAM values read by PolsVoiceEntityHandler's Ocarina preamble.
    // RoomSession supplies these from the active Ocarina item state once per
    // entity tick; the zero values preserve the ordinary no-song path.
    public int linkPlayingOcarinaCountdown;

    int ocarinaSongFlags;

    int selectedSongIndex;

    int ocarinaAnimationCounter;

    int ocarinaAnimationPhase;

    // LinkMotionMapFadeInHandler leaves wTransitionSequenceCounter at $04 once
    // the active room is ready for interaction. Entity ticks are gated during
    // the host transition, so this is the source value visible to gameplay.
    public int transitionSequenceCounter = 0x04;

    // ReturnIfNonInteractive_19's gameplay selector and room-transition flag.
    // Live room sessions default to the ordinary active world path; tests can
    // select the world-map/transition branches explicitly.
    public boolean gameplayWorld = true;

    public boolean roomTransitionActive;

    public int swordMoblinAlertingSoundCounter;

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

    public int currentLinkSpeedX;

    public int currentLinkSpeedY;

    public int currentLinkFinalPositionX;

    public boolean currentLinkFinalPositionXConfigured;

    int chestShieldLevel = 1;

    public int chestSwordLevel = 1;

    public boolean chestPlayerLevelsKnown;

    int chestPowerBraceletLevel = 1;

    int entityGoldenLeavesCount;

    public int pendingMusicTrack = -1;

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

    final List<HeartPiecePickupUpdate> heartPieceUpdates =
        outbox.channel(HeartPiecePickupUpdate.class, "heartPieceUpdates");

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
    public int entityInteractionCountdown;

    public boolean freeMovementMode;

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

    public boolean groundInteractionSideScrolling;

    public int entityMapId = -1;

    public int entityRoomId = -1;

    public boolean dungeonMinibossReady;

    public int entityRoomStatus;

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

    public RoomEntityObjectQuery objectQuery;

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
