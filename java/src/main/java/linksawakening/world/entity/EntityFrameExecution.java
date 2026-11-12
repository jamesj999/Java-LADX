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
        return runtime.entityState.motionSet.sideViewPlatformMotion;
    }

    public linksawakening.world.entity.motion.SideViewPotMotion sideViewPotMotion() {
        return runtime.entityState.motionSet.sideViewPotMotion;
    }

    public linksawakening.world.entity.motion.RoamingEnemyMotion roamingEnemyMotion() {
        return runtime.entityState.motionSet.roamingEnemyMotion;
    }

    public linksawakening.world.entity.motion.UnmaskedIronMaskMotion unmaskedIronMaskMotion() {
        return runtime.entityState.motionSet.unmaskedIronMaskMotion;
    }

    public linksawakening.world.entity.motion.MoblinSwordMotion moblinSwordMotion() {
        return runtime.entityState.motionSet.moblinSwordMotion;
    }

    public linksawakening.world.entity.motion.PlayerArrowMotion playerArrowMotion() {
        return runtime.entityState.motionSet.playerArrowMotion;
    }

    public linksawakening.world.entity.motion.EnemyProjectileMotion enemyProjectileMotion() {
        return runtime.entityState.motionSet.enemyProjectileMotion;
    }

    public linksawakening.world.entity.motion.HookshotChainMotion hookshotChainMotion() {
        return runtime.entityState.motionSet.hookshotChainMotion;
    }

    public linksawakening.world.entity.motion.SparkMotion sparkMotion() {
        return runtime.entityState.motionSet.sparkMotion;
    }

    public linksawakening.world.entity.motion.ZolGelMotion zolGelMotion() {
        return runtime.entityState.motionSet.zolGelMotion;
    }

    public linksawakening.world.entity.motion.HidingZolMotion hidingZolMotion() {
        return runtime.entityState.motionSet.hidingZolMotion;
    }

    public linksawakening.world.entity.motion.StarMotion starMotion() {
        return runtime.entityState.motionSet.starMotion;
    }

    public linksawakening.world.entity.motion.BlooperMotion blooperMotion() {
        return runtime.entityState.motionSet.blooperMotion;
    }

    public linksawakening.world.entity.motion.WingedOctorokMotion wingedOctorokMotion() {
        return runtime.entityState.motionSet.wingedOctorokMotion;
    }

    public linksawakening.world.entity.motion.PincerMotion pincerMotion() {
        return runtime.entityState.motionSet.pincerMotion;
    }

    public linksawakening.world.entity.motion.BushCrawlerMotion bushCrawlerMotion() {
        return runtime.entityState.motionSet.bushCrawlerMotion;
    }

    public linksawakening.world.entity.motion.MimicMotion mimicMotion() {
        return runtime.entityState.motionSet.mimicMotion;
    }

    public linksawakening.world.entity.motion.MaskedMimicMotion maskedMimicMotion() {
        return runtime.entityState.motionSet.maskedMimicMotion;
    }

    public linksawakening.world.entity.motion.MiniMoldormMotion miniMoldormMotion() {
        return runtime.entityState.motionSet.miniMoldormMotion;
    }

    public linksawakening.world.entity.motion.MoldormMotion moldormMotion() {
        return runtime.entityState.motionSet.moldormMotion;
    }

    public linksawakening.world.entity.motion.CuccoMotion cuccoMotion() {
        return runtime.entityState.motionSet.cuccoMotion;
    }

    public linksawakening.world.entity.motion.GiantGopongaMotion giantGopongaMotion() {
        return runtime.entityState.motionSet.giantGopongaMotion;
    }

    public linksawakening.world.entity.motion.GopongaProjectileMotion gopongaProjectileMotion() {
        return runtime.entityState.motionSet.gopongaProjectileMotion;
    }

    public linksawakening.world.entity.motion.PokeyMotion pokeyMotion() {
        return runtime.entityState.motionSet.pokeyMotion;
    }

    public linksawakening.world.entity.motion.PiranhaMotion piranhaMotion() {
        return runtime.entityState.motionSet.piranhaMotion;
    }

    public linksawakening.world.entity.motion.ZoraMotion zoraMotion() {
        return runtime.entityState.motionSet.zoraMotion;
    }

    public linksawakening.world.entity.motion.ZombieMotion zombieMotion() {
        return runtime.entityState.motionSet.zombieMotion;
    }

    public linksawakening.world.entity.motion.BuzzBlobMotion buzzBlobMotion() {
        return runtime.entityState.motionSet.buzzBlobMotion;
    }

    public linksawakening.world.entity.motion.SandCrabMotion sandCrabMotion() {
        return runtime.entityState.motionSet.sandCrabMotion;
    }

    public linksawakening.world.entity.motion.DogMotion dogMotion() {
        return runtime.entityState.motionSet.dogMotion;
    }

    public linksawakening.world.entity.motion.UrchinMotion urchinMotion() {
        return runtime.entityState.motionSet.urchinMotion;
    }

    public linksawakening.world.entity.motion.WitchRatMotion witchRatMotion() {
        return runtime.entityState.motionSet.witchRatMotion;
    }

    public linksawakening.world.entity.motion.WitchMotion witchMotion() {
        return runtime.entityState.motionSet.witchMotion;
    }

    public linksawakening.world.entity.motion.TarinRaccoonMotion tarinRaccoonMotion() {
        return runtime.entityState.motionSet.tarinRaccoonMotion;
    }

    public linksawakening.world.entity.motion.SpikeTrapMotion spikeTrapMotion() {
        return runtime.entityState.motionSet.spikeTrapMotion;
    }

    public linksawakening.world.entity.motion.PairoddMotion pairoddMotion() {
        return runtime.entityState.motionSet.pairoddMotion;
    }

    public linksawakening.world.entity.motion.PairoddProjectileMotion pairoddProjectileMotion() {
        return runtime.entityState.motionSet.pairoddProjectileMotion;
    }

    public linksawakening.world.entity.motion.WaterTektiteMotion waterTektiteMotion() {
        return runtime.entityState.motionSet.waterTektiteMotion;
    }

    public linksawakening.world.entity.motion.FishMotion fishMotion() {
        return runtime.entityState.motionSet.fishMotion;
    }

    public linksawakening.world.entity.motion.CrowMotion crowMotion() {
        return runtime.entityState.motionSet.crowMotion;
    }

    public linksawakening.world.entity.motion.BooBuddyMotion booBuddyMotion() {
        return runtime.entityState.motionSet.booBuddyMotion;
    }

    public linksawakening.world.entity.motion.FairyMotion fairyMotion() {
        return runtime.entityState.motionSet.fairyMotion;
    }

    public linksawakening.world.entity.motion.StalfosAggressiveMotion stalfosAggressiveMotion() {
        return runtime.entityState.motionSet.stalfosAggressiveMotion;
    }

    public linksawakening.world.entity.motion.StalfosEvasiveMotion stalfosEvasiveMotion() {
        return runtime.entityState.motionSet.stalfosEvasiveMotion;
    }

    public linksawakening.world.entity.motion.LikeLikeMotion likeLikeMotion() {
        return runtime.entityState.motionSet.likeLikeMotion;
    }

    public linksawakening.world.entity.motion.GoombaMotion goombaMotion() {
        return runtime.entityState.motionSet.goombaMotion;
    }

    public linksawakening.world.entity.motion.SnakeMotion snakeMotion() {
        return runtime.entityState.motionSet.snakeMotion;
    }

    public linksawakening.world.entity.motion.WizrobeMotion wizrobeMotion() {
        return runtime.entityState.motionSet.wizrobeMotion;
    }

    public linksawakening.world.entity.motion.WizrobeProjectileMotion wizrobeProjectileMotion() {
        return runtime.entityState.motionSet.wizrobeProjectileMotion;
    }

    public linksawakening.world.entity.motion.PeaHatMotion peaHatMotion() {
        return runtime.entityState.motionSet.peaHatMotion;
    }

    public linksawakening.world.entity.motion.ArmosMotion armosMotion() {
        return runtime.entityState.motionSet.armosMotion;
    }

    public linksawakening.world.entity.motion.ArmosKnightMotion armosKnightMotion() {
        return runtime.entityState.motionSet.armosKnightMotion;
    }

    public linksawakening.world.entity.motion.HinoxMotion hinoxMotion() {
        return runtime.entityState.motionSet.hinoxMotion;
    }

    public linksawakening.world.entity.motion.RollingBonesMotion rollingBonesMotion() {
        return runtime.entityState.motionSet.rollingBonesMotion;
    }

    public linksawakening.world.entity.motion.ThreeOfAKindMotion threeOfAKindMotion() {
        return runtime.entityState.motionSet.threeOfAKindMotion;
    }

    public linksawakening.world.entity.motion.MoblinKingMotion moblinKingMotion() {
        return runtime.entityState.motionSet.moblinKingMotion;
    }

    public linksawakening.world.entity.motion.BossIntroMotion bossIntroMotion() {
        return runtime.entityState.motionSet.bossIntroMotion;
    }

    public linksawakening.world.entity.motion.GhiniMotion ghiniMotion() {
        return runtime.entityState.motionSet.ghiniMotion;
    }

    public linksawakening.world.entity.motion.HardHatMotion hardHatMotion() {
        return runtime.entityState.motionSet.hardHatMotion;
    }

    public linksawakening.world.entity.motion.PolsVoiceMotion polsVoiceMotion() {
        return runtime.entityState.motionSet.polsVoiceMotion;
    }

    public linksawakening.world.entity.motion.SpikedBeetleMotion spikedBeetleMotion() {
        return runtime.entityState.motionSet.spikedBeetleMotion;
    }

    public linksawakening.world.entity.motion.MadBomberMotion madBomberMotion() {
        return runtime.entityState.motionSet.madBomberMotion;
    }

    public linksawakening.world.entity.motion.BomberMotion bomberMotion() {
        return runtime.entityState.motionSet.bomberMotion;
    }

    public linksawakening.world.entity.motion.BombiteMotion bombiteMotion() {
        return runtime.entityState.motionSet.bombiteMotion;
    }

    public linksawakening.world.entity.motion.EnemyRecoilMotion enemyRecoilMotion() {
        return runtime.entityState.motionSet.enemyRecoilMotion;
    }

    public linksawakening.world.entity.motion.FollowingNpcMotion followingNpcMotion() {
        return runtime.entityState.motionSet.followingNpcMotion;
    }

    public linksawakening.world.entity.motion.BowWowMotion bowWowMotion() {
        return runtime.entityState.motionSet.bowWowMotion;
    }

    public linksawakening.world.entity.motion.ColorShellMotion colorShellMotion() {
        return runtime.entityState.motionSet.colorShellMotion;
    }

    public linksawakening.world.entity.motion.SecretSeashellMotion secretSeashellMotion() {
        return runtime.entityState.motionSet.secretSeashellMotion;
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
