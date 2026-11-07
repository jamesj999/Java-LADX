package linksawakening.world.entity;

import linksawakening.world.entity.event.EntityCombatEvent;
import linksawakening.world.entity.event.EntityProjectileEvent;

import linksawakening.world.entity.event.WitchRewardEvent;

import linksawakening.world.entity.event.WitchExchangeEvent;

import linksawakening.world.entity.event.WarpLinkStateRequest;

import linksawakening.world.entity.event.TransientVfxRequest;

import linksawakening.world.entity.event.SlimeKeyRewardEvent;

import linksawakening.world.entity.event.SideViewPlatformLinkRequest;

import linksawakening.world.entity.event.ScreenShakeRequest;

import linksawakening.world.entity.event.RoosterLinkStateRequest;

import linksawakening.world.entity.event.RoomStatusPersistenceRequest;

import linksawakening.world.entity.event.LinkMotionBlockRequest;

import linksawakening.world.entity.event.LinkFallenPoseRequest;

import linksawakening.world.entity.event.LinkFacingRequest;

import linksawakening.world.entity.event.LinkAttackClearRequest;

import linksawakening.world.entity.event.LikeLikeEvent;

import linksawakening.world.entity.event.KeyRewardEvent;

import linksawakening.world.entity.event.KeyQuicksandEvent;

import linksawakening.world.entity.event.HinoxLinkEffectRequest;

import linksawakening.world.entity.event.DungeonWarpRequest;

import linksawakening.world.entity.event.DialogRequest;

import static linksawakening.world.entity.EntityMotionMath.*;
import static linksawakening.world.entity.EntitySnapshots.*;
import static linksawakening.world.entity.EntityTypeRules.*;
import static linksawakening.world.entity.RoomEntityRuntime.*;

import java.util.List;
import java.util.function.IntSupplier;

/** Mutable execution locals for one immutable entity frame input. */
public final class EntityFrameExecution {
  public final RoomEntityRuntime runtime;
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

  public linksawakening.entity.EntitySpriteHandlerCatalog sprites() { return runtime.spriteHandlers; }

  public EntityBackgroundMechanics background() { return runtime.backgroundMechanics; }

  public EntityLiftThrowController liftThrow() { return runtime.liftThrow; }

  public EntityLinkView link() { return runtime.linkView; }

  public EntitySlotView slot() { return runtime.slotView; }

  public void replaceEntity(RoomEntity replacement) { runtime.slots[index] = replacement; }

  public List<RoomEntity> entities() { return runtime.slotEntities; }

  public RoomEntity entityAt(int slotIndex) { return runtime.slots[slotIndex]; }

  public void emit(DialogRequest event) { runtime.events.pendingDialogRequests.add(event); }

  public void emit(DungeonWarpRequest event) { runtime.events.pendingDungeonWarpRequests.add(event); }

  public void emit(EntityCombatEvent event) { runtime.events.pendingEntityEvents.add(event); }

  public void emit(HinoxLinkEffectRequest event) { runtime.events.pendingHinoxLinkEffectRequests.add(event); }

  public void emit(KeyQuicksandEvent event) { runtime.events.pendingKeyQuicksandEvents.add(event); }

  public void emit(KeyRewardEvent event) { runtime.events.pendingKeyRewardEvents.add(event); }

  public void emit(LikeLikeEvent event) { runtime.events.pendingLikeLikeEvents.add(event); }

  public void emit(LinkAttackClearRequest event) { runtime.events.pendingLinkAttackClearRequests.add(event); }

  public void emit(LinkFacingRequest event) { runtime.events.pendingLinkFacingRequests.add(event); }

  public void emit(LinkFallenPoseRequest event) { runtime.events.pendingLinkFallenPoseRequests.add(event); }

  public void emit(LinkMotionBlockRequest event) { runtime.events.pendingLinkMotionBlockRequests.add(event); }

  public void emit(RoomStatusPersistenceRequest event) { runtime.events.pendingRoomStatusPersistenceRequests.add(event); }

  public void emit(RoosterLinkStateRequest event) { runtime.events.pendingRoosterLinkStateRequests.add(event); }

  public void emit(ScreenShakeRequest event) { runtime.events.pendingScreenShakeRequests.add(event); }

  public void emit(SideViewPlatformLinkRequest event) { runtime.events.pendingSideViewPlatformLinkRequests.add(event); }

  public void emit(SlimeKeyRewardEvent event) { runtime.events.pendingSlimeKeyRewardEvents.add(event); }

  public void emit(WarpLinkStateRequest event) { runtime.events.pendingWarpLinkStateRequests.add(event); }

  public void emit(WitchExchangeEvent event) { runtime.events.pendingWitchExchangeEvents.add(event); }

  public void emit(WitchRewardEvent event) { runtime.events.pendingWitchRewardEvents.add(event); }

  public void emit(TransientVfxRequest event) { runtime.events.transientVfxRequests.add(event); }

  public EntitySlotLifecycle lifecycle() { return runtime.lifecycle; }

  public EntityEnemySpawns enemySpawns() { return runtime.entityEnemySpawns; }

  public EntityPlayerSpawns playerSpawns() { return runtime.entityPlayerSpawns; }
  public EntityProjectileController projectiles() { return runtime.projectiles; }
  public EntitySlotProperties slotProperties() { return runtime.slotProperties; }
  public EntityBombInteractions bombInteractions() { return runtime.bombInteractions; }
  public EntityRewardController rewards() { return runtime.rewards; }
  public EntityStoryController story() { return runtime.story; }
  public GenieEntityController genie() { return runtime.genie; }
  public EntityPickupController pickups() { return runtime.pickups; }
  public EntityBossRewards bossRewards() { return runtime.bossRewards; }
  public EntityRoomInspection roomInspection() { return runtime.roomInspection; }
  public EntityEnemyInspection enemyInspection() { return runtime.enemyInspection; }
  public linksawakening.world.entity.motion.SideViewPlatformMotion sideViewPlatformMotion() { return runtime.sideViewPlatformMotion; }
  public linksawakening.world.entity.motion.SideViewPotMotion sideViewPotMotion() { return runtime.sideViewPotMotion; }
  public linksawakening.world.entity.motion.RoamingEnemyMotion roamingEnemyMotion() { return runtime.roamingEnemyMotion; }
  public linksawakening.world.entity.motion.UnmaskedIronMaskMotion unmaskedIronMaskMotion() { return runtime.unmaskedIronMaskMotion; }
  public linksawakening.world.entity.motion.MoblinSwordMotion moblinSwordMotion() { return runtime.moblinSwordMotion; }
  public linksawakening.world.entity.motion.PlayerArrowMotion playerArrowMotion() { return runtime.playerArrowMotion; }
  public linksawakening.world.entity.motion.EnemyProjectileMotion enemyProjectileMotion() { return runtime.enemyProjectileMotion; }
  public linksawakening.world.entity.motion.HookshotChainMotion hookshotChainMotion() { return runtime.hookshotChainMotion; }
  public linksawakening.world.entity.motion.SparkMotion sparkMotion() { return runtime.sparkMotion; }
  public linksawakening.world.entity.motion.ZolGelMotion zolGelMotion() { return runtime.zolGelMotion; }
  public linksawakening.world.entity.motion.HidingZolMotion hidingZolMotion() { return runtime.hidingZolMotion; }
  public linksawakening.world.entity.motion.StarMotion starMotion() { return runtime.starMotion; }
  public linksawakening.world.entity.motion.BlooperMotion blooperMotion() { return runtime.blooperMotion; }
  public linksawakening.world.entity.motion.WingedOctorokMotion wingedOctorokMotion() { return runtime.wingedOctorokMotion; }
  public linksawakening.world.entity.motion.PincerMotion pincerMotion() { return runtime.pincerMotion; }
  public linksawakening.world.entity.motion.BushCrawlerMotion bushCrawlerMotion() { return runtime.bushCrawlerMotion; }
  public linksawakening.world.entity.motion.MimicMotion mimicMotion() { return runtime.mimicMotion; }
  public linksawakening.world.entity.motion.MaskedMimicMotion maskedMimicMotion() { return runtime.maskedMimicMotion; }
  public linksawakening.world.entity.motion.MiniMoldormMotion miniMoldormMotion() { return runtime.miniMoldormMotion; }
  public linksawakening.world.entity.motion.MoldormMotion moldormMotion() { return runtime.moldormMotion; }
  public linksawakening.world.entity.motion.CuccoMotion cuccoMotion() { return runtime.cuccoMotion; }
  public linksawakening.world.entity.motion.GiantGopongaMotion giantGopongaMotion() { return runtime.giantGopongaMotion; }
  public linksawakening.world.entity.motion.GopongaProjectileMotion gopongaProjectileMotion() { return runtime.gopongaProjectileMotion; }
  public linksawakening.world.entity.motion.PokeyMotion pokeyMotion() { return runtime.pokeyMotion; }
  public linksawakening.world.entity.motion.PiranhaMotion piranhaMotion() { return runtime.piranhaMotion; }
  public linksawakening.world.entity.motion.ZoraMotion zoraMotion() { return runtime.zoraMotion; }
  public linksawakening.world.entity.motion.ZombieMotion zombieMotion() { return runtime.zombieMotion; }
  public linksawakening.world.entity.motion.BuzzBlobMotion buzzBlobMotion() { return runtime.buzzBlobMotion; }
  public linksawakening.world.entity.motion.SandCrabMotion sandCrabMotion() { return runtime.sandCrabMotion; }
  public linksawakening.world.entity.motion.DogMotion dogMotion() { return runtime.dogMotion; }
  public linksawakening.world.entity.motion.UrchinMotion urchinMotion() { return runtime.urchinMotion; }
  public linksawakening.world.entity.motion.WitchRatMotion witchRatMotion() { return runtime.witchRatMotion; }
  public linksawakening.world.entity.motion.WitchMotion witchMotion() { return runtime.witchMotion; }
  public linksawakening.world.entity.motion.TarinRaccoonMotion tarinRaccoonMotion() { return runtime.tarinRaccoonMotion; }
  public linksawakening.world.entity.motion.SpikeTrapMotion spikeTrapMotion() { return runtime.spikeTrapMotion; }
  public linksawakening.world.entity.motion.PairoddMotion pairoddMotion() { return runtime.pairoddMotion; }
  public linksawakening.world.entity.motion.PairoddProjectileMotion pairoddProjectileMotion() { return runtime.pairoddProjectileMotion; }
  public linksawakening.world.entity.motion.WaterTektiteMotion waterTektiteMotion() { return runtime.waterTektiteMotion; }
  public linksawakening.world.entity.motion.FishMotion fishMotion() { return runtime.fishMotion; }
  public linksawakening.world.entity.motion.CrowMotion crowMotion() { return runtime.crowMotion; }
  public linksawakening.world.entity.motion.BooBuddyMotion booBuddyMotion() { return runtime.booBuddyMotion; }
  public linksawakening.world.entity.motion.FairyMotion fairyMotion() { return runtime.fairyMotion; }
  public linksawakening.world.entity.motion.StalfosAggressiveMotion stalfosAggressiveMotion() { return runtime.stalfosAggressiveMotion; }
  public linksawakening.world.entity.motion.StalfosEvasiveMotion stalfosEvasiveMotion() { return runtime.stalfosEvasiveMotion; }
  public linksawakening.world.entity.motion.LikeLikeMotion likeLikeMotion() { return runtime.likeLikeMotion; }
  public linksawakening.world.entity.motion.GoombaMotion goombaMotion() { return runtime.goombaMotion; }
  public linksawakening.world.entity.motion.SnakeMotion snakeMotion() { return runtime.snakeMotion; }
  public linksawakening.world.entity.motion.WizrobeMotion wizrobeMotion() { return runtime.wizrobeMotion; }
  public linksawakening.world.entity.motion.WizrobeProjectileMotion wizrobeProjectileMotion() { return runtime.wizrobeProjectileMotion; }
  public linksawakening.world.entity.motion.PeaHatMotion peaHatMotion() { return runtime.peaHatMotion; }
  public linksawakening.world.entity.motion.ArmosMotion armosMotion() { return runtime.armosMotion; }
  public linksawakening.world.entity.motion.ArmosKnightMotion armosKnightMotion() { return runtime.armosKnightMotion; }
  public linksawakening.world.entity.motion.HinoxMotion hinoxMotion() { return runtime.hinoxMotion; }
  public linksawakening.world.entity.motion.RollingBonesMotion rollingBonesMotion() { return runtime.rollingBonesMotion; }
  public linksawakening.world.entity.motion.ThreeOfAKindMotion threeOfAKindMotion() { return runtime.threeOfAKindMotion; }
  public linksawakening.world.entity.motion.MoblinKingMotion moblinKingMotion() { return runtime.moblinKingMotion; }
  public linksawakening.world.entity.motion.BossIntroMotion bossIntroMotion() { return runtime.bossIntroMotion; }
  public linksawakening.world.entity.motion.GhiniMotion ghiniMotion() { return runtime.ghiniMotion; }
  public linksawakening.world.entity.motion.HardHatMotion hardHatMotion() { return runtime.hardHatMotion; }
  public linksawakening.world.entity.motion.PolsVoiceMotion polsVoiceMotion() { return runtime.polsVoiceMotion; }
  public linksawakening.world.entity.motion.SpikedBeetleMotion spikedBeetleMotion() { return runtime.spikedBeetleMotion; }
  public linksawakening.world.entity.motion.MadBomberMotion madBomberMotion() { return runtime.madBomberMotion; }
  public linksawakening.world.entity.motion.BomberMotion bomberMotion() { return runtime.bomberMotion; }
  public linksawakening.world.entity.motion.BombiteMotion bombiteMotion() { return runtime.bombiteMotion; }
  public linksawakening.world.entity.motion.EnemyRecoilMotion enemyRecoilMotion() { return runtime.enemyRecoilMotion; }
  public linksawakening.world.entity.motion.FollowingNpcMotion followingNpcMotion() { return runtime.followingNpcMotion; }
  public linksawakening.world.entity.motion.BowWowMotion bowWowMotion() { return runtime.bowWowMotion; }
  public linksawakening.world.entity.motion.ColorShellMotion colorShellMotion() { return runtime.colorShellMotion; }
  public linksawakening.world.entity.motion.SecretSeashellMotion secretSeashellMotion() { return runtime.secretSeashellMotion; }
  public linksawakening.world.entity.motion.WarpMotion warpMotion() { return runtime.warpMotion; }
}
