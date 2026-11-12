package linksawakening.world.entity;

import static linksawakening.world.entity.EntityMotionMath.*;
import static linksawakening.world.entity.EntityRomConstants.*;
import static linksawakening.world.entity.EntitySlotValidation.*;
import static linksawakening.world.entity.EntitySnapshots.*;
import static linksawakening.world.entity.EntityTypeRules.*;

import java.util.Arrays;
import java.util.Objects;
import linksawakening.world.entity.event.EntityCombatEvent;
import linksawakening.world.entity.event.EntityProjectileEvent;
import linksawakening.world.entity.motion.*;

/** Frame setup in the same order as AnimateEntities, before the descending slot loop. */
final class EntityFramePreparation {
    static void copyInputs(EntityFrameExecution state) {

        state.inputs().frameCounter(state.context().frameCounter());
        state.inputs().linkEntityX(state.context().linkEntityX());
        state.inputs().linkEntityY(state.context().linkEntityY());
        state.inputs().collisionType(state.context().collisionType());
        state.inputs().randomByteSupplier(state.context().randomByteSupplier());
        state.inputs().backgroundCollision(state.context().backgroundCollision());
        state.inputs().objectCollision(state.context().objectCollision());
        state.inputs().linkPositionHistory(state.context().linkPositionHistory());
        state.inputs().linkZ(state.context().linkZ());
        state.inputs().linkAirborne(state.context().linkAirborne());
        state.inputs().linkDirection(state.context().linkDirection());
        state.inputs().entityYOffset(state.context().entityYOffset());
        state.inputs().projectileLinkState(state.context().projectileLinkState());
        state.inputs().creditsGameplay(state.context().creditsGameplay());
        state.inputs().swordCollisionActive(state.context().swordCollisionActive());
        state.inputs().swordX(state.context().swordX());
        state.inputs().swordWidth(state.context().swordWidth());
        state.inputs().swordY(state.context().swordY());
        state.inputs().swordHeight(state.context().swordHeight());
        state.gates().handlerLinkCollisionEnabled(state.context().handlerLinkCollisionEnabled());
        Objects.requireNonNull(state.inputs().randomByteSupplier(), "randomByteSupplier");
        Objects.requireNonNull(state.inputs().projectileLinkState(), "projectileLinkState");
    }

    static void prepareInteractionGates(EntityFrameExecution state) {

        state.motion(ZolGelMotion.class, ZolGelMotion::new)
                .writeGelClingingToLink(false); // AnimateEntities clears wIsGelClingingToLink each pass.
        state.gates().countdownBlocksInteraction(state.roomVariables().readEntityInteractionCountdown() != 0);
        if (!state.linkState().dialogActive && state.roomVariables().readEntityInteractionCountdown() > 0)
            state.roomVariables().postEntityInteractionCountdown(-1);
        state.gates()
                .handlerLinkCollisionEnabled(state.gates().handlerLinkCollisionEnabled()
                        && !state.linkState().inventoryAppearing
                        && !state.gates().countdownBlocksInteraction());
        state.gates()
                .threeOfAKindInteractive(state.gates().handlerLinkCollisionEnabled()
                        && state.background().entityTimersInteractive());
        state.gates()
                .handlerInteractive(state.background()
                        .entityHandlerInteractive(state.inputs().creditsGameplay()));
        state.gates()
                .bowWowHandlerInteractive(state.roomVariables().readTransitionSequenceCounter() == 0x04
                        && !state.roomVariables().readRoomTransitionActive()
                        && !state.linkState().dialogActive
                        && !state.linkState().inventoryAppearing
                        && !state.roomInspection().witchGotItemPresentationActive());
        state.gates()
                .projectileTimersInteractive(state.background()
                        .gameplayEntityTimersInteractive(state.inputs().creditsGameplay()));
        state.gates().sideViewPotInteractive(state.gates().handlerInteractive());
        state.gates()
                .sideViewPotLinkCollisionInteractive(state.gates().sideViewPotInteractive()
                        && state.gates().handlerLinkCollisionEnabled()
                        && state.inputs().projectileLinkState().motionState()
                                < EnemyProjectileCollision.LINK_MOTION_NON_INTERACTIVE);
        state.story().writeShouldGetLostInMysteriousWoods(false);
        state.bombInteractions().finalizePendingBombPresentations();
        state.inputs().frame(state.inputs().frameCounter() & 0xFF);
        state.inputs().romCollisionType(state.inputs().collisionType() & 0xFF);
    }

    static void advanceRoomClocks(EntityFrameExecution state) {

        // WorldHandler runs $14:4B21 before AnimateEntities, even when no
        // boomerang exists or dialogs/room transitions suspend its handler.
        if (!state.projectiles().readBoomerangSoundClockProvided()
                && state.roomVariables().readGameplayWorld()
                && !state.inputs().creditsGameplay()) {
            state.projectiles()
                    .writeBoomerangSfxCounter((state.projectiles().readBoomerangSfxCounter() + 1)
                            % EntityRomConstants.BOOMERANG_SFX_COUNTER_PERIOD);
        }
        state.projectiles().writeBoomerangSoundClockProvided(false);
        if (state.motion(MoblinSwordMotion.class, MoblinSwordMotion::new).readSwordMoblinAlertingSoundCounter() > 0) {
            state.motion(MoblinSwordMotion.class, MoblinSwordMotion::new).postSwordMoblinAlertingSoundCounter(-1);
        }
        if (state.bombInteractions().readBombArrowCooldown() > 0) {
            state.bombInteractions().postBombArrowCooldown(-1);
        }
    }

    static void prepareBackgroundCollision(EntityFrameExecution state) {

        if (state.physicsTables().readBackgroundInteraction() != null) {
            state.inputs().backgroundCollision(state.collisionAdapters().booleanProbe());
        }
        state.inputs().hinoxBackgroundInteraction(state.physicsTables().readBackgroundInteraction());
        if (state.inputs().hinoxBackgroundInteraction() == null
                && state.inputs().backgroundCollision() != null) {
            state.inputs().hinoxBackgroundInteraction(state.collisionAdapters().richProbe());
        }
    }

    static void resetSlotAndLinkEffects(EntityFrameExecution state) {

        state.events().projectileLaunchRequests.clear();
        state.physicsTables().clearEnemyProjectileSpawnedThisFrame();
        state.physicsTables().clearDynamicEntitySpawnedThisFrame();
        state.genie().beginFrame();
        Arrays.fill(state.slotProperties().storageWingedSwordAttackThisFrame(), false);
        Arrays.fill(state.slotProperties().storageWingedStateTwoThisFrame(), false);
        state.events().transientVfxRequests.clear();
        state.events().pendingLinkFinalPositionRequests.clear();
        state.events().pendingRoosterLinkStateRequests.clear();
        state.events().pendingLinkMotionBlockRequests.clear();
        state.events().pendingSideViewPlatformLinkRequests.clear();
        state.events().pendingSideViewPotLinkRequests.clear();
        state.events().pendingHinoxLinkEffectRequests.clear();
        state.events().pendingLinkFallenPoseRequests.clear();
        state.events().pendingLinkFacingRequests.clear();
        state.events().pendingLinkAttackStepRequests.clear();
        state.events().pendingLinkAttackStepRequests.addAll(state.events().pendingPreTickLinkAttackStepRequests);
        state.events().pendingPreTickLinkAttackStepRequests.clear();
        state.events().pendingLinkAttackClearRequests.clear();
        state.events().pendingRoomStatusPersistenceRequests.clear();
        state.events().pendingLinkHeldItemPoseRequests.clear();
        state.events().pendingLinkSwordSpinPoseRequests.clear();
        state.events().pendingLinkSwordFinalPoseRequests.clear();
        state.events().pendingScreenShakeRequests.clear();
    }

    static void resetRewardsAndObjects(EntityFrameExecution state) {

        state.events().boomerangObjectRequests.clear();
        state.events().magicRodObjectRequests.clear();
        state.events().magicPowderObjectRequests.clear();
        state.events().pendingDialogRequests.clear();
        state.events().pendingEntityEvents.clear();
        state.events().pendingThrownBackgroundCollisions.clear();
        state.events().pendingEntityEvents.addAll(state.events().pendingPreTickEntityEvents);
        state.events().pendingPreTickEntityEvents.clear();
        state.events().pendingWarpLinkStateRequests.clear();
        state.events().pendingChestRewardEvents.clear();
        state.events().pendingHeartContainerRewards.clear();
        state.events().pendingMadamMeowMeowFullHealRequests.clear();
        state.events().pendingSwordPickupRewards.clear();
        state.events().pendingToadstoolRewards.clear();
        state.events().pendingInstrumentRewards.clear();
        state.events().pendingInstrumentCompletions.clear();
        state.events().pendingRichardProgressEvents.clear();
    }

    static void prepareLateFrameEvents(EntityFrameExecution state) {

        if (state.bossRewards().readBossAgonySfxCountdown() > 0) {
            state.bossRewards().postBossAgonySfxCountdown(-1);
            if (state.bossRewards().readBossAgonySfxCountdown() == 0) {
                state.events()
                        .pendingEntityEvents
                        .add(new EntityCombatEvent(
                                state.bossRewards().readBossAgonySourceSlot(),
                                EntityRomConstants.ENTITY_GENIE,
                                0,
                                false,
                                EntityCombatEvent.SoundChannel.WAVE,
                                0x10));
            }
        }
        state.events().pendingWitchExchangeEvents.clear();
        state.events().pendingWitchRewardEvents.clear();
        state.events().pendingOwlEventCompletions.clear();
        state.events().pendingSlimeKeyRewardEvents.clear();
        state.events().pendingKeyQuicksandEvents.clear();
        state.events().pendingLikeLikeEvents.clear();
        state.events().pendingBombExplosionEvents.clear();
        state.events().hookshotBridgeUpdates.clear();
        state.roomVariables().writePendingSwitchBlockAnimationRequest(false);
        if (state.linkState().linkPlayingOcarinaCountdown >= 0x10
                && state.linkState().ocarinaAnimationCounter == 0x14) {
            state.playerSpawns()
                    .spawnMusicalNote(
                            state.inputs().linkEntityX(),
                            state.inputs().linkEntityY(),
                            state.linkState().ocarinaAnimationPhase);
        }
        state.inputs().projectileEvents(state.events().outbox.channel(EntityProjectileEvent.class, "projectileEvents"));
        state.inputs().projectileEvents().clear();
    }

    static void prepareFollowerState(EntityFrameExecution state) {

        if (state.inputs().linkPositionHistory() != null) {
            state.motion(FollowingNpcMotion.class, FollowingNpcMotion::new)
                    .writeFollowingLinkPositionHistory(state.inputs().linkPositionHistory());
        }
        state.linkState().followingLinkZ = state.inputs().linkZ() & 0xFF;
        state.linkState().followingLinkDirection = state.inputs().linkDirection() & 0xFF;
        state.linkState().followingEntityYOffset = state.inputs().entityYOffset() & 0xFF;
        state.slotFrame()
                .carryingEntity(state.roomInspection().liftedEntityState().carryState() != 0);
        state.inputs()
                .romLinkDirection(romDirectionForJavaDirection(state.inputs().linkDirection()));
        state.linkState().lastRomLinkDirection = state.inputs().romLinkDirection();
    }
}
