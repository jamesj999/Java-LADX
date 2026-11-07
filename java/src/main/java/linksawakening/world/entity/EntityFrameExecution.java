package linksawakening.world.entity;

import linksawakening.world.entity.LinkPositionHistory;
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

import linksawakening.world.entity.event.ThrownBackgroundCollisionEvent;

import static linksawakening.world.entity.EntityMotionMath.*;
import static linksawakening.world.entity.EntitySnapshots.*;
import static linksawakening.world.entity.EntityTypeRules.*;
import static linksawakening.world.entity.RoomEntityRuntime.*;

import java.util.Arrays;
import java.util.List;
import java.util.Objects;
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

  List<EntityProjectileEvent> advance() {
    EntityFrameExecution state = this;

    state.frameCounter = state.context.frameCounter();
    state.linkEntityX = state.context.linkEntityX();
    state.linkEntityY = state.context.linkEntityY();
    state.collisionType = state.context.collisionType();
    state.randomByteSupplier = state.context.randomByteSupplier();
    state.backgroundCollision = state.context.backgroundCollision();
    state.objectCollision = state.context.objectCollision();
    state.linkPositionHistory = state.context.linkPositionHistory();
    state.linkZ = state.context.linkZ();
    state.linkAirborne = state.context.linkAirborne();
    state.linkDirection = state.context.linkDirection();
    state.entityYOffset = state.context.entityYOffset();
    state.projectileLinkState = state.context.projectileLinkState();
    state.creditsGameplay = state.context.creditsGameplay();
    state.swordCollisionActive = state.context.swordCollisionActive();
    state.swordX = state.context.swordX();
    state.swordWidth = state.context.swordWidth();
    state.swordY = state.context.swordY();
    state.swordHeight = state.context.swordHeight();
    state.handlerLinkCollisionEnabled = state.context.handlerLinkCollisionEnabled();
    Objects.requireNonNull(state.randomByteSupplier, "randomByteSupplier");
    Objects.requireNonNull(state.projectileLinkState, "projectileLinkState");
    state.runtime.gelClingingToLink =
        false; // AnimateEntities clears wIsGelClingingToLink each pass.
    state.countdownBlocksInteraction = state.runtime.entityInteractionCountdown != 0;
    if (!state.runtime.dialogActive && state.runtime.entityInteractionCountdown > 0)
      state.runtime.entityInteractionCountdown--;
    state.handlerLinkCollisionEnabled =
        state.handlerLinkCollisionEnabled
            && !state.runtime.inventoryAppearing
            && !state.countdownBlocksInteraction;
    state.threeOfAKindInteractive =
        state.handlerLinkCollisionEnabled
            && state.runtime.backgroundMechanics.entityTimersInteractive();
    state.handlerInteractive =
        state.runtime.backgroundMechanics.entityHandlerInteractive(state.creditsGameplay);
    state.bowWowHandlerInteractive =
        state.runtime.transitionSequenceCounter == 0x04
            && !state.runtime.roomTransitionActive
            && !state.runtime.dialogActive
            && !state.runtime.inventoryAppearing
            && !state.runtime.inspection().witchGotItemPresentationActive();
    state.projectileTimersInteractive =
        state.runtime.backgroundMechanics.gameplayEntityTimersInteractive(state.creditsGameplay);
    state.sideViewPotInteractive = state.handlerInteractive;
    state.sideViewPotLinkCollisionInteractive =
        state.sideViewPotInteractive
            && state.handlerLinkCollisionEnabled
            && state.projectileLinkState.motionState()
                < EnemyProjectileCollision.LINK_MOTION_NON_INTERACTIVE;
    state.runtime.shouldGetLostInMysteriousWoods = false;
    state.runtime.bombInteractions.finalizePendingBombPresentations();
    state.frame = state.frameCounter & 0xFF;
    state.romCollisionType = state.collisionType & 0xFF;
    // WorldHandler runs $14:4B21 before AnimateEntities, even when no
    // boomerang exists or dialogs/room transitions suspend its handler.
    if (!state.runtime.boomerangSoundClockProvided
        && state.runtime.gameplayWorld
        && !state.creditsGameplay) {
      state.runtime.boomerangSfxCounter =
          (state.runtime.boomerangSfxCounter + 1) % state.runtime.BOOMERANG_SFX_COUNTER_PERIOD;
    }
    state.runtime.boomerangSoundClockProvided = false;
    if (state.runtime.swordMoblinAlertingSoundCounter > 0) {
      state.runtime.swordMoblinAlertingSoundCounter--;
    }
    if (state.runtime.bombArrowCooldown > 0) {
      state.runtime.bombArrowCooldown--;
    }
    if (state.runtime.backgroundInteraction != null) {
      state.backgroundCollision =
          (entity, direction, nextX, nextY) -> {
            EntityBackgroundCollisionResult result =
                state.runtime.backgroundInteraction.probe(
                    entity,
                    direction,
                    nextX,
                    nextY,
                    state.runtime.enemyIgnoreHitsCountdown[entity.slot()],
                    state.frame);
            if (result.blocked()
                && entity.status() == EntityStatus.THROWN
                && checksThrownRoomEventTriggers(entity.type())) {
              state.runtime.pendingThrownBackgroundCollisions.add(
                  new ThrownBackgroundCollisionEvent(
                      entity.slot(), entity.type(), direction, result.objectId()));
            }
            return result.blocked();
          };
    }
    state.hinoxBackgroundInteraction = state.runtime.backgroundInteraction;
    if (state.hinoxBackgroundInteraction == null && state.backgroundCollision != null) {
      state.hinoxBackgroundInteraction =
          RoomEntityBackgroundInteraction.fromBoolean(state.backgroundCollision);
    }
    state.runtime.projectileLaunchRequests.clear();
    Arrays.fill(state.runtime.enemyProjectileSpawnedThisFrame, false);
    Arrays.fill(state.runtime.dynamicEntitySpawnedThisFrame, false);
    Arrays.fill(state.runtime.genieImpactRegisteredThisFrame, false);
    Arrays.fill(state.runtime.wingedSwordAttackThisFrame, false);
    Arrays.fill(state.runtime.wingedStateTwoThisFrame, false);
    state.runtime.transientVfxRequests.clear();
    state.runtime.pendingLinkFinalPositionRequests.clear();
    state.runtime.pendingRoosterLinkStateRequests.clear();
    state.runtime.pendingLinkMotionBlockRequests.clear();
    state.runtime.pendingSideViewPlatformLinkRequests.clear();
    state.runtime.pendingSideViewPotLinkRequests.clear();
    state.runtime.pendingHinoxLinkEffectRequests.clear();
    state.runtime.pendingLinkFallenPoseRequests.clear();
    state.runtime.pendingLinkFacingRequests.clear();
    state.runtime.pendingLinkAttackStepRequests.clear();
    state.runtime.pendingLinkAttackStepRequests.addAll(
        state.runtime.pendingPreTickLinkAttackStepRequests);
    state.runtime.pendingPreTickLinkAttackStepRequests.clear();
    state.runtime.pendingLinkAttackClearRequests.clear();
    state.runtime.pendingRoomStatusPersistenceRequests.clear();
    state.runtime.pendingLinkHeldItemPoseRequests.clear();
    state.runtime.pendingLinkSwordSpinPoseRequests.clear();
    state.runtime.pendingLinkSwordFinalPoseRequests.clear();
    state.runtime.pendingScreenShakeRequests.clear();
    state.runtime.boomerangObjectRequests.clear();
    state.runtime.magicRodObjectRequests.clear();
    state.runtime.magicPowderObjectRequests.clear();
    state.runtime.pendingDialogRequests.clear();
    state.runtime.pendingEntityEvents.clear();
    state.runtime.pendingThrownBackgroundCollisions.clear();
    state.runtime.pendingEntityEvents.addAll(state.runtime.pendingPreTickEntityEvents);
    state.runtime.pendingPreTickEntityEvents.clear();
    state.runtime.pendingWarpLinkStateRequests.clear();
    state.runtime.pendingChestRewardEvents.clear();
    state.runtime.pendingHeartContainerRewards.clear();
    state.runtime.pendingMadamMeowMeowFullHealRequests.clear();
    state.runtime.pendingSwordPickupRewards.clear();
    state.runtime.pendingToadstoolRewards.clear();
    state.runtime.pendingInstrumentRewards.clear();
    state.runtime.pendingInstrumentCompletions.clear();
    state.runtime.pendingRichardProgressEvents.clear();
    if (state.runtime.bossAgonySfxCountdown > 0) {
      state.runtime.bossAgonySfxCountdown--;
      if (state.runtime.bossAgonySfxCountdown == 0) {
        state.runtime.pendingEntityEvents.add(
            new EntityCombatEvent(
                state.runtime.bossAgonySourceSlot,
                state.runtime.ENTITY_GENIE,
                0,
                false,
                EntityCombatEvent.SoundChannel.WAVE,
                0x10));
      }
    }
    state.runtime.pendingWitchExchangeEvents.clear();
    state.runtime.pendingWitchRewardEvents.clear();
    state.runtime.pendingOwlEventCompletions.clear();
    state.runtime.pendingSlimeKeyRewardEvents.clear();
    state.runtime.pendingKeyQuicksandEvents.clear();
    state.runtime.pendingLikeLikeEvents.clear();
    state.runtime.pendingBombExplosionEvents.clear();
    state.runtime.hookshotBridgeUpdates.clear();
    state.runtime.pendingSwitchBlockAnimationRequest = false;
    if (state.runtime.linkPlayingOcarinaCountdown >= 0x10
        && state.runtime.ocarinaAnimationCounter == 0x14) {
      state.runtime.entityPlayerSpawns.spawnMusicalNote(
          state.linkEntityX, state.linkEntityY, state.runtime.ocarinaAnimationPhase);
    }
    state.projectileEvents =
        state.runtime.outbox.channel(EntityProjectileEvent.class, "projectileEvents");
    state.projectileEvents.clear();
    if (state.linkPositionHistory != null) {
      state.runtime.followingLinkPositionHistory = state.linkPositionHistory;
    }
    state.runtime.followingLinkZ = state.linkZ & 0xFF;
    state.runtime.followingLinkDirection = state.linkDirection & 0xFF;
    state.runtime.followingEntityYOffset = state.entityYOffset & 0xFF;
    state.carryingEntity = state.runtime.inspection().liftedEntityState().carryState() != 0;
    state.romLinkDirection = romDirectionForJavaDirection(state.linkDirection);
    state.runtime.lastRomLinkDirection = state.romLinkDirection;
    for (state.index = state.runtime.slots.length - 1; state.index >= 0; state.index--) {
      if (!EntitySlotPhase1.run(state)) continue;
      if (!EntitySlotPhase2.run(state)) continue;
      if (!EntitySlotPhase3.run(state)) continue;
      if (!EntitySlotPhase4.run(state)) continue;
      if (!EntitySlotPhase5.run(state)) continue;
    }
    if (state.threeOfAKindInteractive) {
      state.runtime.slotProperties.resolveThreeOfAKindPuzzle();
    }
    state.runtime.slotProperties.updateHookshotChainOam(
        state.linkEntityX, state.linkEntityY, state.frame);
    state.runtime.slotProperties.updatePincerBodyOam();
    state.runtime.slotProperties.updateWingedOctorokOam();
    state.runtime.slotProperties.updateBowWowChain(state.frame);
    return state.runtime.outbox.drain(EntityProjectileEvent.class, "projectileEvents");
  }

  public void replaceEntity(RoomEntity replacement) { runtime.slots[index] = replacement; }

  public RoomEntity entityAt(int slotIndex) { return runtime.slots[slotIndex]; }

  public void emit(DialogRequest event) { runtime.pendingDialogRequests.add(event); }

  public void emit(DungeonWarpRequest event) { runtime.pendingDungeonWarpRequests.add(event); }

  public void emit(EntityCombatEvent event) { runtime.pendingEntityEvents.add(event); }

  public void emit(HinoxLinkEffectRequest event) { runtime.pendingHinoxLinkEffectRequests.add(event); }

  public void emit(KeyQuicksandEvent event) { runtime.pendingKeyQuicksandEvents.add(event); }

  public void emit(KeyRewardEvent event) { runtime.pendingKeyRewardEvents.add(event); }

  public void emit(LikeLikeEvent event) { runtime.pendingLikeLikeEvents.add(event); }

  public void emit(LinkAttackClearRequest event) { runtime.pendingLinkAttackClearRequests.add(event); }

  public void emit(LinkFacingRequest event) { runtime.pendingLinkFacingRequests.add(event); }

  public void emit(LinkFallenPoseRequest event) { runtime.pendingLinkFallenPoseRequests.add(event); }

  public void emit(LinkMotionBlockRequest event) { runtime.pendingLinkMotionBlockRequests.add(event); }

  public void emit(RoomStatusPersistenceRequest event) { runtime.pendingRoomStatusPersistenceRequests.add(event); }

  public void emit(RoosterLinkStateRequest event) { runtime.pendingRoosterLinkStateRequests.add(event); }

  public void emit(ScreenShakeRequest event) { runtime.pendingScreenShakeRequests.add(event); }

  public void emit(SideViewPlatformLinkRequest event) { runtime.pendingSideViewPlatformLinkRequests.add(event); }

  public void emit(SlimeKeyRewardEvent event) { runtime.pendingSlimeKeyRewardEvents.add(event); }

  public void emit(WarpLinkStateRequest event) { runtime.pendingWarpLinkStateRequests.add(event); }

  public void emit(WitchExchangeEvent event) { runtime.pendingWitchExchangeEvents.add(event); }

  public void emit(WitchRewardEvent event) { runtime.pendingWitchRewardEvents.add(event); }

  public void emit(TransientVfxRequest event) { runtime.transientVfxRequests.add(event); }

  public EntitySlotLifecycle lifecycle() { return runtime.lifecycle; }

  public EntityEnemySpawns enemySpawns() { return runtime.entityEnemySpawns; }

  public EntityPlayerSpawns playerSpawns() { return runtime.entityPlayerSpawns; }
}
