package linksawakening.world;

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

import static linksawakening.world.EntityMotionMath.*;
import static linksawakening.world.EntitySnapshots.*;
import static linksawakening.world.EntityTypeRules.*;
import static linksawakening.world.RoomEntityRuntime.*;

import java.util.Arrays;
import java.util.List;
import java.util.Objects;
import java.util.function.IntSupplier;

/** Mutable execution locals for one immutable entity frame input. */
final class EntityFrameExecution {
  final RoomEntityRuntime runtime;
  RoomEntity lifted;
  int liftedRomDirection;
  int frameCounter;
  int linkEntityX;
  int linkEntityY;
  int collisionType;
  IntSupplier randomByteSupplier;
  RoomEntityBackgroundCollision backgroundCollision;
  RoomEntityObjectCollision objectCollision;
  LinkPositionHistory linkPositionHistory;
  int linkZ;
  boolean linkAirborne;
  int linkDirection;
  int entityYOffset;
  EnemyProjectileCollision.LinkState projectileLinkState;
  boolean creditsGameplay;
  boolean swordCollisionActive;
  int swordX;
  int swordWidth;
  int swordY;
  int swordHeight;
  boolean handlerLinkCollisionEnabled;
  boolean countdownBlocksInteraction;
  boolean threeOfAKindInteractive;
  boolean handlerInteractive;
  boolean bowWowHandlerInteractive;
  boolean projectileTimersInteractive;
  boolean sideViewPotInteractive;
  boolean sideViewPotLinkCollisionInteractive;
  int frame;
  int romCollisionType;
  RoomEntityBackgroundInteraction hinoxBackgroundInteraction;
  List<EntityProjectileEvent> projectileEvents;
  boolean carryingEntity;
  int romLinkDirection;
  int index;
  RoomEntity entity;
  boolean moldormIntroOpenedThisFrame;
  boolean rollingBonesHitThisFrame;
  boolean kanaletLeafProducer;
  RoomEntity originalEntity;
  EntityStatus initialStatus;
  boolean wasInitializing;
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
  EntityStatus status;
  boolean preserveGhiniPresentation;
  boolean preserveMadBomberPresentation;
  boolean preserveBombitePresentation;
  boolean preserveIronMaskPresentation;
  boolean preserveSnakePresentation;
  boolean preserveStarPresentation;
  boolean preserveBlooperPresentation;
  boolean preserveWingedOctorokPresentation;
  boolean preservePincerPresentation;
  boolean preserveBushCrawlerPresentation;
  boolean preserveMimicPresentation;
  boolean preserveMaskedMimicPresentation;
  boolean preserveMiniMoldormPresentation;
  boolean preserveMoldormPresentation;
  boolean preserveCuccoPresentation;
  boolean preserveGopongaFlowerPresentation;
  boolean preserveGiantGopongaPresentation;
  boolean preserveGopongaProjectilePresentation;
  boolean preservePokeyPresentation;
  boolean preservePiranhaPresentation;
  boolean preserveZoraPresentation;
  boolean preserveZombiePresentation;
  boolean preserveBuzzBlobPresentation;
  boolean preserveSandCrabPresentation;
  boolean preserveDogPresentation;
  boolean preserveUrchinPresentation;
  boolean preserveWitchRatPresentation;
  boolean preserveWitchPresentation;
  boolean preserveRoosterPresentation;
  boolean preserveWizrobePresentation;
  boolean preserveWizrobeProjectilePresentation;
  boolean preservePolsVoicePresentation;
  boolean preserveSpikedBeetlePresentation;
  boolean preserveArmosKnightPresentation;
  boolean preserveHinoxPresentation;
  boolean preserveRollingBonesPresentation;
  boolean preserveThreeOfAKindPresentation;
  boolean preserveMoblinKingPresentation;
  boolean preserveSecretSeashellPresentation;
  boolean preserveDroppablePresentation;
  boolean preserveTarinPresentation;
  boolean keyDropTransitionActive;
  RoomEntityGroundInteraction.Result preAppliedGroundResult;
  boolean applyGenericGroundInteraction;
  RoomEntity updated;
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
            && !state.runtime.witchGotItemPresentationActive();
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
    state.carryingEntity = state.runtime.liftedEntityState().carryState() != 0;
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

  void replaceEntity(RoomEntity replacement) { runtime.slots[index] = replacement; }

  RoomEntity entityAt(int slotIndex) { return runtime.slots[slotIndex]; }

  void emit(DialogRequest event) { runtime.pendingDialogRequests.add(event); }

  void emit(DungeonWarpRequest event) { runtime.pendingDungeonWarpRequests.add(event); }

  void emit(EntityCombatEvent event) { runtime.pendingEntityEvents.add(event); }

  void emit(HinoxLinkEffectRequest event) { runtime.pendingHinoxLinkEffectRequests.add(event); }

  void emit(KeyQuicksandEvent event) { runtime.pendingKeyQuicksandEvents.add(event); }

  void emit(KeyRewardEvent event) { runtime.pendingKeyRewardEvents.add(event); }

  void emit(LikeLikeEvent event) { runtime.pendingLikeLikeEvents.add(event); }

  void emit(LinkAttackClearRequest event) { runtime.pendingLinkAttackClearRequests.add(event); }

  void emit(LinkFacingRequest event) { runtime.pendingLinkFacingRequests.add(event); }

  void emit(LinkFallenPoseRequest event) { runtime.pendingLinkFallenPoseRequests.add(event); }

  void emit(LinkMotionBlockRequest event) { runtime.pendingLinkMotionBlockRequests.add(event); }

  void emit(RoomStatusPersistenceRequest event) { runtime.pendingRoomStatusPersistenceRequests.add(event); }

  void emit(RoosterLinkStateRequest event) { runtime.pendingRoosterLinkStateRequests.add(event); }

  void emit(ScreenShakeRequest event) { runtime.pendingScreenShakeRequests.add(event); }

  void emit(SideViewPlatformLinkRequest event) { runtime.pendingSideViewPlatformLinkRequests.add(event); }

  void emit(SlimeKeyRewardEvent event) { runtime.pendingSlimeKeyRewardEvents.add(event); }

  void emit(WarpLinkStateRequest event) { runtime.pendingWarpLinkStateRequests.add(event); }

  void emit(WitchExchangeEvent event) { runtime.pendingWitchExchangeEvents.add(event); }

  void emit(WitchRewardEvent event) { runtime.pendingWitchRewardEvents.add(event); }

  void emit(TransientVfxRequest event) { runtime.transientVfxRequests.add(event); }

  EntitySlotLifecycle lifecycle() { return runtime.lifecycle; }

  EntityEnemySpawns enemySpawns() { return runtime.entityEnemySpawns; }

  EntityPlayerSpawns playerSpawns() { return runtime.entityPlayerSpawns; }
}
