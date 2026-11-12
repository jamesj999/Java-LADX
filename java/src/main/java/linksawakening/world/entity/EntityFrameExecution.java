package linksawakening.world.entity;

import static linksawakening.world.entity.EntityMotionMath.*;
import static linksawakening.world.entity.EntityRomConstants.*;
import static linksawakening.world.entity.EntitySnapshots.*;
import static linksawakening.world.entity.EntityTypeRules.*;

import java.util.List;
import java.util.function.IntSupplier;
import linksawakening.world.entity.event.DialogRequest;
import linksawakening.world.entity.event.DungeonWarpRequest;
import linksawakening.world.entity.event.EntityCombatEvent;
import linksawakening.world.entity.event.EntityProjectileEvent;
import linksawakening.world.entity.event.HinoxLinkEffectRequest;
import linksawakening.world.entity.event.KeyQuicksandEvent;
import linksawakening.world.entity.event.KeyRewardEvent;
import linksawakening.world.entity.event.LikeLikeEvent;
import linksawakening.world.entity.event.LinkAttackClearRequest;
import linksawakening.world.entity.event.LinkFacingRequest;
import linksawakening.world.entity.event.LinkFallenPoseRequest;
import linksawakening.world.entity.event.LinkMotionBlockRequest;
import linksawakening.world.entity.event.RoomStatusPersistenceRequest;
import linksawakening.world.entity.event.RoosterLinkStateRequest;
import linksawakening.world.entity.event.ScreenShakeRequest;
import linksawakening.world.entity.event.SideViewPlatformLinkRequest;
import linksawakening.world.entity.event.SlimeKeyRewardEvent;
import linksawakening.world.entity.event.TransientVfxRequest;
import linksawakening.world.entity.event.WarpLinkStateRequest;
import linksawakening.world.entity.event.WitchExchangeEvent;
import linksawakening.world.entity.event.WitchRewardEvent;
import linksawakening.world.entity.motion.*;

/** Mutable execution locals for one immutable entity frame input. */
public final class EntityFrameExecution {
    final RoomEntityRuntime runtime;
    public RoomEntity lifted;
    public int liftedRomDirection;
    public int frameCounter;
    public int linkEntityX;
    public int linkEntityY;
    int collisionType;
    public IntSupplier randomByteSupplier;
    public RoomEntityBackgroundCollision backgroundCollision;
    public RoomEntityObjectCollision objectCollision;
    LinkPositionHistory linkPositionHistory;
    public int linkZ;
    public boolean linkAirborne;
    int linkDirection;
    int entityYOffset;
    public EnemyProjectileCollision.LinkState projectileLinkState;
    public boolean creditsGameplay;
    public boolean swordCollisionActive;
    public int swordX;
    public int swordWidth;
    public int swordY;
    public int swordHeight;
    public boolean handlerLinkCollisionEnabled;
    public boolean countdownBlocksInteraction;
    public boolean threeOfAKindInteractive;
    public boolean handlerInteractive;
    public boolean bowWowHandlerInteractive;
    public boolean projectileTimersInteractive;
    public boolean sideViewPotInteractive;
    public boolean sideViewPotLinkCollisionInteractive;
    public int frame;
    public int romCollisionType;
    public RoomEntityBackgroundInteraction hinoxBackgroundInteraction;
    public List<EntityProjectileEvent> projectileEvents;
    public boolean carryingEntity;
    public int romLinkDirection;
    public int index;
    public RoomEntity entity;
    public boolean moldormIntroOpenedThisFrame;
    public boolean rollingBonesHitThisFrame;
    boolean kanaletLeafProducer;
    RoomEntity originalEntity;
    public EntityStatus initialStatus;
    public boolean wasInitializing;
    boolean freezeTarinTimers;
    boolean freezeThreeOfAKindTimers;
    boolean freezeHinoxTimers;
    boolean freezeGenieTimers;
    boolean freezeWitchTimers;
    boolean freezePowderTimers;
    boolean freezeBooBuddyTimers;
    boolean freezeBowWowTimers;
    boolean freezeMoblinKingTimers;
    boolean ignoreHitsDecrementedBeforeHandler;
    public EntityStatus status;
    public boolean preserveGhiniPresentation;
    public boolean preserveMadBomberPresentation;
    public boolean preserveBombitePresentation;
    public boolean preserveIronMaskPresentation;
    public boolean preserveSnakePresentation;
    public boolean preserveStarPresentation;
    public boolean preserveBlooperPresentation;
    public boolean preserveWingedOctorokPresentation;
    public boolean preservePincerPresentation;
    public boolean preserveBushCrawlerPresentation;
    public boolean preserveMimicPresentation;
    public boolean preserveMaskedMimicPresentation;
    public boolean preserveMiniMoldormPresentation;
    public boolean preserveMoldormPresentation;
    public boolean preserveCuccoPresentation;
    public boolean preserveGopongaFlowerPresentation;
    public boolean preserveGiantGopongaPresentation;
    public boolean preserveGopongaProjectilePresentation;
    public boolean preservePokeyPresentation;
    public boolean preservePiranhaPresentation;
    public boolean preserveZoraPresentation;
    public boolean preserveZombiePresentation;
    public boolean preserveBuzzBlobPresentation;
    public boolean preserveSandCrabPresentation;
    public boolean preserveDogPresentation;
    public boolean preserveUrchinPresentation;
    public boolean preserveWitchRatPresentation;
    public boolean preserveWitchPresentation;
    public boolean preserveRoosterPresentation;
    public boolean preserveWizrobePresentation;
    public boolean preserveWizrobeProjectilePresentation;
    public boolean preservePolsVoicePresentation;
    public boolean preserveSpikedBeetlePresentation;
    public boolean preserveArmosKnightPresentation;
    public boolean preserveHinoxPresentation;
    public boolean preserveRollingBonesPresentation;
    public boolean preserveThreeOfAKindPresentation;
    public boolean preserveMoblinKingPresentation;
    public boolean preserveSecretSeashellPresentation;
    public boolean preserveDroppablePresentation;
    public boolean preserveTarinPresentation;
    public boolean keyDropTransitionActive;
    public RoomEntityGroundInteraction.Result preAppliedGroundResult;
    public boolean applyGenericGroundInteraction;
    public RoomEntity updated;
    EntityLinkCollisionRules.LinkPushPolicy genericLinkPushPolicy;
    int variant;
    int renderFlipAttribute;
    EntityFrameContext context;

    EntityFrameExecution(RoomEntityRuntime runtime, EntityFrameContext context) {
        this.runtime = runtime;
        this.context = context;
    }

    public linksawakening.entity.EntitySpriteHandlerCatalog sprites() {
        return runtime.spriteHandlers;
    }

    public EntityBackgroundMechanics background() {
        return runtime.backgroundMechanics;
    }

    public EntityLiftThrowController liftThrow() {
        return runtime.liftThrow;
    }

    public EntityLinkView link() {
        return runtime.entityState.linkView;
    }

    public EntitySlotView slot() {
        return runtime.entityState.slotView;
    }

    public void replaceEntity(RoomEntity replacement) {
        runtime.slots[index] = replacement;
    }

    public List<RoomEntity> entities() {
        return runtime.slotEntities;
    }

    public RoomEntity entityAt(int slotIndex) {
        return runtime.slots[slotIndex];
    }

    public void emit(DialogRequest event) {
        runtime.entityState.events.pendingDialogRequests.add(event);
    }

    public void emit(DungeonWarpRequest event) {
        runtime.entityState.events.pendingDungeonWarpRequests.add(event);
    }

    public void emit(EntityCombatEvent event) {
        runtime.entityState.events.pendingEntityEvents.add(event);
    }

    public void emit(HinoxLinkEffectRequest event) {
        runtime.entityState.events.pendingHinoxLinkEffectRequests.add(event);
    }

    public void emit(KeyQuicksandEvent event) {
        runtime.entityState.events.pendingKeyQuicksandEvents.add(event);
    }

    public void emit(KeyRewardEvent event) {
        runtime.entityState.events.pendingKeyRewardEvents.add(event);
    }

    public void emit(LikeLikeEvent event) {
        runtime.entityState.events.pendingLikeLikeEvents.add(event);
    }

    public void emit(LinkAttackClearRequest event) {
        runtime.entityState.events.pendingLinkAttackClearRequests.add(event);
    }

    public void emit(LinkFacingRequest event) {
        runtime.entityState.events.pendingLinkFacingRequests.add(event);
    }

    public void emit(LinkFallenPoseRequest event) {
        runtime.entityState.events.pendingLinkFallenPoseRequests.add(event);
    }

    public void emit(LinkMotionBlockRequest event) {
        runtime.entityState.events.pendingLinkMotionBlockRequests.add(event);
    }

    public void emit(RoomStatusPersistenceRequest event) {
        runtime.entityState.events.pendingRoomStatusPersistenceRequests.add(event);
    }

    public void emit(RoosterLinkStateRequest event) {
        runtime.entityState.events.pendingRoosterLinkStateRequests.add(event);
    }

    public void emit(ScreenShakeRequest event) {
        runtime.entityState.events.pendingScreenShakeRequests.add(event);
    }

    public void emit(SideViewPlatformLinkRequest event) {
        runtime.entityState.events.pendingSideViewPlatformLinkRequests.add(event);
    }

    public void emit(SlimeKeyRewardEvent event) {
        runtime.entityState.events.pendingSlimeKeyRewardEvents.add(event);
    }

    public void emit(WarpLinkStateRequest event) {
        runtime.entityState.events.pendingWarpLinkStateRequests.add(event);
    }

    public void emit(WitchExchangeEvent event) {
        runtime.entityState.events.pendingWitchExchangeEvents.add(event);
    }

    public void emit(WitchRewardEvent event) {
        runtime.entityState.events.pendingWitchRewardEvents.add(event);
    }

    public void emit(TransientVfxRequest event) {
        runtime.entityState.events.transientVfxRequests.add(event);
    }

    public EntitySlotLifecycle lifecycle() {
        return runtime.lifecycle;
    }

    public EntityEnemySpawns enemySpawns() {
        return runtime.entityEnemySpawns;
    }

    public EntityPlayerSpawns playerSpawns() {
        return runtime.entityPlayerSpawns;
    }

    public EntityProjectileController projectiles() {
        return runtime.projectiles;
    }

    public EntitySlotProperties slotProperties() {
        return runtime.slotProperties;
    }

    public EntityBombInteractions bombInteractions() {
        return runtime.bombInteractions;
    }

    public EntityRewardController rewards() {
        return runtime.rewards;
    }

    public EntityStoryController story() {
        return runtime.story;
    }

    public SlimeEyeController slimeEye() {
        return runtime.slimeEye;
    }

    public GenieEntityController genie() {
        return runtime.genie;
    }

    public EntityPickupController pickups() {
        return runtime.pickups;
    }

    public EntityBossRewards bossRewards() {
        return runtime.bossRewards;
    }

    public EntityRoomInspection roomInspection() {
        return runtime.roomInspection;
    }

    public EntityEnemyInspection enemyInspection() {
        return runtime.enemyInspection;
    }

    public linksawakening.world.entity.motion.SideViewPlatformMotion sideViewPlatformMotion() {
        return runtime.entityState.motions.get(SideViewPlatformMotion.class, SideViewPlatformMotion::new);
    }

    public linksawakening.world.entity.motion.SideViewPotMotion sideViewPotMotion() {
        return runtime.entityState.motions.get(SideViewPotMotion.class, SideViewPotMotion::new);
    }

    public linksawakening.world.entity.motion.RoamingEnemyMotion roamingEnemyMotion() {
        return runtime.entityState.motions.get(RoamingEnemyMotion.class, RoamingEnemyMotion::new);
    }

    public linksawakening.world.entity.motion.UnmaskedIronMaskMotion unmaskedIronMaskMotion() {
        return runtime.entityState.motions.get(UnmaskedIronMaskMotion.class, UnmaskedIronMaskMotion::new);
    }

    public linksawakening.world.entity.motion.MoblinSwordMotion moblinSwordMotion() {
        return runtime.entityState.motions.get(MoblinSwordMotion.class, MoblinSwordMotion::new);
    }

    public linksawakening.world.entity.motion.PlayerArrowMotion playerArrowMotion() {
        return runtime.entityState.motions.get(PlayerArrowMotion.class, PlayerArrowMotion::new);
    }

    public linksawakening.world.entity.motion.EnemyProjectileMotion enemyProjectileMotion() {
        return runtime.entityState.motions.get(EnemyProjectileMotion.class, EnemyProjectileMotion::new);
    }

    public linksawakening.world.entity.motion.HookshotChainMotion hookshotChainMotion() {
        return runtime.entityState.motions.get(HookshotChainMotion.class, HookshotChainMotion::new);
    }

    public linksawakening.world.entity.motion.SparkMotion sparkMotion() {
        return runtime.entityState.motions.get(SparkMotion.class, SparkMotion::new);
    }

    public linksawakening.world.entity.motion.ZolGelMotion zolGelMotion() {
        return runtime.entityState.motions.get(ZolGelMotion.class, ZolGelMotion::new);
    }

    public linksawakening.world.entity.motion.HidingZolMotion hidingZolMotion() {
        return runtime.entityState.motions.get(HidingZolMotion.class, HidingZolMotion::new);
    }

    public linksawakening.world.entity.motion.StarMotion starMotion() {
        return runtime.entityState.motions.get(StarMotion.class, StarMotion::new);
    }

    public linksawakening.world.entity.motion.BlooperMotion blooperMotion() {
        return runtime.entityState.motions.get(BlooperMotion.class, BlooperMotion::new);
    }

    public linksawakening.world.entity.motion.WingedOctorokMotion wingedOctorokMotion() {
        return runtime.entityState.motions.get(WingedOctorokMotion.class, WingedOctorokMotion::new);
    }

    public linksawakening.world.entity.motion.PincerMotion pincerMotion() {
        return runtime.entityState.motions.get(PincerMotion.class, PincerMotion::new);
    }

    public linksawakening.world.entity.motion.BushCrawlerMotion bushCrawlerMotion() {
        return runtime.entityState.motions.get(BushCrawlerMotion.class, BushCrawlerMotion::new);
    }

    public linksawakening.world.entity.motion.MimicMotion mimicMotion() {
        return runtime.entityState.motions.get(MimicMotion.class, MimicMotion::new);
    }

    public linksawakening.world.entity.motion.MaskedMimicMotion maskedMimicMotion() {
        return runtime.entityState.motions.get(MaskedMimicMotion.class, MaskedMimicMotion::new);
    }

    public linksawakening.world.entity.motion.MiniMoldormMotion miniMoldormMotion() {
        return runtime.entityState.motions.get(MiniMoldormMotion.class, MiniMoldormMotion::new);
    }

    public linksawakening.world.entity.motion.MoldormMotion moldormMotion() {
        return runtime.entityState.motions.get(MoldormMotion.class, MoldormMotion::new);
    }

    public linksawakening.world.entity.motion.CuccoMotion cuccoMotion() {
        return runtime.entityState.motions.get(CuccoMotion.class, CuccoMotion::new);
    }

    public linksawakening.world.entity.motion.GiantGopongaMotion giantGopongaMotion() {
        return runtime.entityState.motions.get(GiantGopongaMotion.class, GiantGopongaMotion::new);
    }

    public linksawakening.world.entity.motion.GopongaProjectileMotion gopongaProjectileMotion() {
        return runtime.entityState.motions.get(GopongaProjectileMotion.class, GopongaProjectileMotion::new);
    }

    public linksawakening.world.entity.motion.PokeyMotion pokeyMotion() {
        return runtime.entityState.motions.get(PokeyMotion.class, PokeyMotion::new);
    }

    public linksawakening.world.entity.motion.PiranhaMotion piranhaMotion() {
        return runtime.entityState.motions.get(PiranhaMotion.class, PiranhaMotion::new);
    }

    public linksawakening.world.entity.motion.ZoraMotion zoraMotion() {
        return runtime.entityState.motions.get(ZoraMotion.class, ZoraMotion::new);
    }

    public linksawakening.world.entity.motion.ZombieMotion zombieMotion() {
        return runtime.entityState.motions.get(ZombieMotion.class, ZombieMotion::new);
    }

    public linksawakening.world.entity.motion.BuzzBlobMotion buzzBlobMotion() {
        return runtime.entityState.motions.get(BuzzBlobMotion.class, BuzzBlobMotion::new);
    }

    public linksawakening.world.entity.motion.SandCrabMotion sandCrabMotion() {
        return runtime.entityState.motions.get(SandCrabMotion.class, SandCrabMotion::new);
    }

    public linksawakening.world.entity.motion.DogMotion dogMotion() {
        return runtime.entityState.motions.get(DogMotion.class, DogMotion::new);
    }

    public linksawakening.world.entity.motion.UrchinMotion urchinMotion() {
        return runtime.entityState.motions.get(UrchinMotion.class, UrchinMotion::new);
    }

    public linksawakening.world.entity.motion.WitchRatMotion witchRatMotion() {
        return runtime.entityState.motions.get(WitchRatMotion.class, WitchRatMotion::new);
    }

    public linksawakening.world.entity.motion.WitchMotion witchMotion() {
        return runtime.entityState.motions.get(WitchMotion.class, WitchMotion::new);
    }

    public linksawakening.world.entity.motion.TarinRaccoonMotion tarinRaccoonMotion() {
        return runtime.entityState.motions.get(TarinRaccoonMotion.class, TarinRaccoonMotion::new);
    }

    public linksawakening.world.entity.motion.SpikeTrapMotion spikeTrapMotion() {
        return runtime.entityState.motions.get(SpikeTrapMotion.class, SpikeTrapMotion::new);
    }

    public linksawakening.world.entity.motion.PairoddMotion pairoddMotion() {
        return runtime.entityState.motions.get(PairoddMotion.class, PairoddMotion::new);
    }

    public linksawakening.world.entity.motion.PairoddProjectileMotion pairoddProjectileMotion() {
        return runtime.entityState.motions.get(PairoddProjectileMotion.class, PairoddProjectileMotion::new);
    }

    public linksawakening.world.entity.motion.WaterTektiteMotion waterTektiteMotion() {
        return runtime.entityState.motions.get(WaterTektiteMotion.class, WaterTektiteMotion::new);
    }

    public linksawakening.world.entity.motion.FishMotion fishMotion() {
        return runtime.entityState.motions.get(FishMotion.class, FishMotion::new);
    }

    public linksawakening.world.entity.motion.CrowMotion crowMotion() {
        return runtime.entityState.motions.get(CrowMotion.class, CrowMotion::new);
    }

    public linksawakening.world.entity.motion.BooBuddyMotion booBuddyMotion() {
        return runtime.entityState.motions.get(BooBuddyMotion.class, BooBuddyMotion::new);
    }

    public linksawakening.world.entity.motion.FairyMotion fairyMotion() {
        return runtime.entityState.motions.get(FairyMotion.class, FairyMotion::new);
    }

    public linksawakening.world.entity.motion.StalfosAggressiveMotion stalfosAggressiveMotion() {
        return runtime.entityState.motions.get(StalfosAggressiveMotion.class, StalfosAggressiveMotion::new);
    }

    public linksawakening.world.entity.motion.StalfosEvasiveMotion stalfosEvasiveMotion() {
        return runtime.entityState.motions.get(StalfosEvasiveMotion.class, StalfosEvasiveMotion::new);
    }

    public linksawakening.world.entity.motion.LikeLikeMotion likeLikeMotion() {
        return runtime.entityState.motions.get(LikeLikeMotion.class, LikeLikeMotion::new);
    }

    public linksawakening.world.entity.motion.GoombaMotion goombaMotion() {
        return runtime.entityState.motions.get(GoombaMotion.class, GoombaMotion::new);
    }

    public linksawakening.world.entity.motion.SnakeMotion snakeMotion() {
        return runtime.entityState.motions.get(SnakeMotion.class, SnakeMotion::new);
    }

    public linksawakening.world.entity.motion.WizrobeMotion wizrobeMotion() {
        return runtime.entityState.motions.get(WizrobeMotion.class, WizrobeMotion::new);
    }

    public linksawakening.world.entity.motion.WizrobeProjectileMotion wizrobeProjectileMotion() {
        return runtime.entityState.motions.get(WizrobeProjectileMotion.class, WizrobeProjectileMotion::new);
    }

    public linksawakening.world.entity.motion.PeaHatMotion peaHatMotion() {
        return runtime.entityState.motions.get(PeaHatMotion.class, PeaHatMotion::new);
    }

    public linksawakening.world.entity.motion.ArmosMotion armosMotion() {
        return runtime.entityState.motions.get(ArmosMotion.class, ArmosMotion::new);
    }

    public linksawakening.world.entity.motion.ArmosKnightMotion armosKnightMotion() {
        return runtime.entityState.motions.get(ArmosKnightMotion.class, ArmosKnightMotion::new);
    }

    public linksawakening.world.entity.motion.HinoxMotion hinoxMotion() {
        return runtime.entityState.motions.get(HinoxMotion.class, HinoxMotion::new);
    }

    public linksawakening.world.entity.motion.RollingBonesMotion rollingBonesMotion() {
        return runtime.entityState.motions.get(RollingBonesMotion.class, RollingBonesMotion::new);
    }

    public linksawakening.world.entity.motion.ThreeOfAKindMotion threeOfAKindMotion() {
        return runtime.entityState.motions.get(ThreeOfAKindMotion.class, ThreeOfAKindMotion::new);
    }

    public linksawakening.world.entity.motion.MoblinKingMotion moblinKingMotion() {
        return runtime.entityState.motions.get(MoblinKingMotion.class, MoblinKingMotion::new);
    }

    public linksawakening.world.entity.motion.BossIntroMotion bossIntroMotion() {
        return runtime.entityState.motions.get(BossIntroMotion.class, BossIntroMotion::new);
    }

    public linksawakening.world.entity.motion.GhiniMotion ghiniMotion() {
        return runtime.entityState.motions.get(GhiniMotion.class, GhiniMotion::new);
    }

    public linksawakening.world.entity.motion.HardHatMotion hardHatMotion() {
        return runtime.entityState.motions.get(HardHatMotion.class, HardHatMotion::new);
    }

    public linksawakening.world.entity.motion.PolsVoiceMotion polsVoiceMotion() {
        return runtime.entityState.motions.get(PolsVoiceMotion.class, PolsVoiceMotion::new);
    }

    public linksawakening.world.entity.motion.SpikedBeetleMotion spikedBeetleMotion() {
        return runtime.entityState.motions.get(SpikedBeetleMotion.class, SpikedBeetleMotion::new);
    }

    public linksawakening.world.entity.motion.MadBomberMotion madBomberMotion() {
        return runtime.entityState.motions.get(MadBomberMotion.class, MadBomberMotion::new);
    }

    public linksawakening.world.entity.motion.BomberMotion bomberMotion() {
        return runtime.entityState.motions.get(BomberMotion.class, BomberMotion::new);
    }

    public linksawakening.world.entity.motion.BombiteMotion bombiteMotion() {
        return runtime.entityState.motions.get(BombiteMotion.class, BombiteMotion::new);
    }

    public linksawakening.world.entity.motion.EnemyRecoilMotion enemyRecoilMotion() {
        return runtime.entityState.motions.get(EnemyRecoilMotion.class, EnemyRecoilMotion::new);
    }

    public linksawakening.world.entity.motion.FollowingNpcMotion followingNpcMotion() {
        return runtime.entityState.motions.get(FollowingNpcMotion.class, FollowingNpcMotion::new);
    }

    public linksawakening.world.entity.motion.BowWowMotion bowWowMotion() {
        return runtime.entityState.motions.get(BowWowMotion.class, BowWowMotion::new);
    }

    public linksawakening.world.entity.motion.ColorShellMotion colorShellMotion() {
        return runtime.entityState.motions.get(ColorShellMotion.class, ColorShellMotion::new);
    }

    public linksawakening.world.entity.motion.SecretSeashellMotion secretSeashellMotion() {
        return runtime.entityState.motions.get(SecretSeashellMotion.class, SecretSeashellMotion::new);
    }

    public linksawakening.world.entity.motion.WarpMotion warpMotion() {
        return runtime.warpMotion;
    }

    public void requestMusic(int track) {
        runtime.entityState.presentationState.writePendingMusicTrack(track);
    }

    public void emit(linksawakening.world.entity.event.HookshotBridgeUpdate update) {
        runtime.entityState.events.hookshotBridgeUpdates.add(update);
    }

    public void emit(linksawakening.world.entity.motion.RoamingEnemyMotion.LaunchRequest request) {
        runtime.entityState.events.projectileLaunchRequests.add(request);
    }
}
