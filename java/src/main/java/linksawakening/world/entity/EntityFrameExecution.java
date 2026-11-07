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

  public EntityLinkView link() { return runtime.linkView; }

  public EntitySlotView slot() { return runtime.slotView; }

  public void replaceEntity(RoomEntity replacement) { runtime.slots[index] = replacement; }

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
}
