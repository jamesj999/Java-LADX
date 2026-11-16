package linksawakening.world.entity;

import static linksawakening.world.entity.EntityMotionMath.romDirectionForJavaDirection;
import static linksawakening.world.entity.EntitySnapshots.withDeathPresentation;
import static linksawakening.world.entity.EntitySnapshots.withPresentation;
import static linksawakening.world.entity.EntitySnapshots.withSourceLoadOrder;
import static linksawakening.world.entity.EntitySnapshots.withStatus;
import static linksawakening.world.entity.EntitySnapshots.withVariant;
import static linksawakening.world.entity.EntitySnapshots.withZ;
import static linksawakening.world.entity.EntityTypeRules.isBossKeyDropProducer;
import static linksawakening.world.entity.EntityTypeRules.isDynamicFollowingNpc;
import static linksawakening.world.entity.EntityTypeRules.isLiftableEntity;
import static linksawakening.world.entity.EntityTypeRules.isRuntimeFloatingItem;
import static linksawakening.world.entity.EntityTypeRules.usesSharedRecoil;

import java.util.Arrays;
import java.util.List;
import java.util.Objects;
import java.util.function.IntSupplier;
import linksawakening.vfx.TransientVfxType;
import linksawakening.world.entity.event.EntityCombatEvent;
import linksawakening.world.entity.event.EntityProjectileEvent;
import linksawakening.world.entity.event.TransientVfxRequest;
import linksawakening.world.entity.motion.BowWowMotion;
import linksawakening.world.entity.motion.BowWowMotion.BowWowCollisionFrame;
import linksawakening.world.entity.motion.ColorShellMotion;
import linksawakening.world.entity.motion.EnemyDropMotion;
import linksawakening.world.entity.motion.EnemyRecoilMotion;
import linksawakening.world.entity.motion.FloatingItemMotion;
import linksawakening.world.entity.motion.FollowingNpcMotion;
import linksawakening.world.entity.motion.FollowingNpcMotion.FollowingNpcFrame;
import linksawakening.world.entity.motion.LaserMotion;
import linksawakening.world.entity.motion.MoblinSwordMotion;
import linksawakening.world.entity.motion.PokeyMotion;
import linksawakening.world.entity.motion.RollingBonesMotion;
import linksawakening.world.entity.motion.TarinRaccoonMotion;
import linksawakening.world.entity.motion.ZolGelMotion;

/** ExecuteEntities: prepare one frame, visit slots in descending order, then commit room presentation. */
final class EntitySlotPipeline {
    private final EntityFrameExecution frameExecution;
    private final EntityFrameRandom frameRandom;

    EntitySlotPipeline(EntityFrameExecution frameExecution, EntityFrameRandom frameRandom) {
        this.frameExecution = java.util.Objects.requireNonNull(frameExecution);
        this.frameRandom = java.util.Objects.requireNonNull(frameRandom);
    }

    EntityFrameContext frameContext(int frameCounter) {
        IntSupplier randomByteSupplier = frameRandom.forFrame(frameCounter);
        return EntityFrameContext.builder()
                .frameCounter(frameCounter)
                .randomByteSupplier(randomByteSupplier)
                .projectileLinkState(EnemyProjectileCollision.LinkState.nonInteractive())
                .handlerLinkCollisionEnabled(true)
                .build();
    }

    List<EntityProjectileEvent> tick(EntityFrameContext context) {
        EntitySlotValidation.validateByte(context.linkSpeedX(), "Link X speed");
        EntitySlotValidation.validateByte(context.linkSpeedY(), "Link Y speed");
        frameExecution.linkState().writeCurrentLinkSpeedX(context.linkSpeedX());
        frameExecution.linkState().writeCurrentLinkSpeedY(context.linkSpeedY());
        try {
            return advanceFrame(frameExecution, context);
        } finally {
            frameExecution.combat().clearDeferredContactRecoil();
            frameExecution.linkState().writeCurrentLinkSpeedX(0);
            frameExecution.linkState().writeCurrentLinkSpeedY(0);
        }
    }

    static List<EntityProjectileEvent> advanceFrame(EntityFrameExecution frameExecution, EntityFrameContext context) {
        EntityFrameExecution state = frameExecution.acquire(context);
        try {
            return advancePreparedFrame(state);
        } finally {
            state.release();
        }
    }

    private static List<EntityProjectileEvent> advancePreparedFrame(EntityFrameExecution state) {
        copyInputs(state);
        prepareInteractionGates(state);
        advanceRoomClocks(state);
        prepareBackgroundCollision(state);
        resetSlotAndLinkEffects(state);
        resetRewardsAndObjects(state);
        prepareLateFrameEvents(state);
        prepareFollowerState(state);

        for (state.slotFrame().index(state.slots().size() - 1);
                state.slotFrame().index() >= 0;
                state.slotFrame().postIndex(-1)) {
            if (!prepareSlotAndTimers(state)) continue;
            if (!advanceSlotStatus(state)) continue;
            if (!advanceLaserSite(state)) continue;
            if (!advanceFollowersAndMotion(state)) continue;
            applyGroundAndCommitSlot(state);
        }
        if (state.gates().threeOfAKindInteractive()) {
            state.slotProperties().resolveThreeOfAKindPuzzle();
        }
        state.slotProperties()
                .updateHookshotChainOam(
                        state.inputs().linkEntityX(),
                        state.inputs().linkEntityY(),
                        state.inputs().frame());
        state.slotProperties().updatePincerBodyOam();
        state.slotProperties().updateWingedOctorokOam();
        state.slotProperties().updateBowWowChain(state.inputs().frame());
        return state.events().outbox.drain(EntityProjectileEvent.class, "projectileEvents");
    }

    private static void copyInputs(EntityFrameExecution state) {

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

    private static void prepareInteractionGates(EntityFrameExecution state) {

        state.motion(ZolGelMotion.class, ZolGelMotion::new)
                .writeGelClingingToLink(false); // AnimateEntities clears wIsGelClingingToLink each pass.
        state.gates().countdownBlocksInteraction(state.roomVariables().readEntityInteractionCountdown() != 0);
        if (!state.linkState().readDialogActive() && state.roomVariables().readEntityInteractionCountdown() > 0)
            state.roomVariables().postEntityInteractionCountdown(-1);
        state.gates()
                .handlerLinkCollisionEnabled(state.gates().handlerLinkCollisionEnabled()
                        && !state.linkState().readInventoryAppearing()
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
                        && !state.linkState().readDialogActive()
                        && !state.linkState().readInventoryAppearing()
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
        state.story().state.writeShouldGetLostInMysteriousWoods(false);
        state.bombInteractions().finalizePendingBombPresentations();
        state.inputs().frame(state.inputs().frameCounter() & 0xFF);
        state.inputs().romCollisionType(state.inputs().collisionType() & 0xFF);
    }

    private static void advanceRoomClocks(EntityFrameExecution state) {

        // WorldHandler runs $14:4B21 before AnimateEntities, even when no
        // boomerang exists or dialogs/room transitions suspend its handler.
        if (!state.projectiles().state.readBoomerangSoundClockProvided()
                && state.roomVariables().readGameplayWorld()
                && !state.inputs().creditsGameplay()) {
            state.projectiles()
                    .state
                    .writeBoomerangSfxCounter((state.projectiles().state.readBoomerangSfxCounter() + 1)
                            % EntityRomConstants.BOOMERANG_SFX_COUNTER_PERIOD);
        }
        state.projectiles().state.writeBoomerangSoundClockProvided(false);
        if (state.motion(MoblinSwordMotion.class, MoblinSwordMotion::new).readSwordMoblinAlertingSoundCounter() > 0) {
            state.motion(MoblinSwordMotion.class, MoblinSwordMotion::new).postSwordMoblinAlertingSoundCounter(-1);
        }
        if (state.bombInteractions().state.readBombArrowCooldown() > 0) {
            state.bombInteractions().state.postBombArrowCooldown(-1);
        }
    }

    private static void prepareBackgroundCollision(EntityFrameExecution state) {

        if (state.physicsTables().hasRichBackgroundInteraction()) {
            state.inputs().backgroundCollision(state.collisionAdapters().booleanProbe());
            state.inputs().hinoxBackgroundInteraction(state.physicsTables().readBackgroundInteraction());
        } else {
            state.inputs()
                    .hinoxBackgroundInteraction(
                            state.inputs().backgroundCollision() == RoomEntityBackgroundCollision.OPEN
                                    ? RoomEntityBackgroundInteraction.NONE
                                    : state.collisionAdapters().richProbe());
        }
    }

    private static void resetSlotAndLinkEffects(EntityFrameExecution state) {

        state.events().clearProjectileLaunchRequests();
        state.physicsTables().clearEnemyProjectileSpawnedThisFrame();
        state.physicsTables().clearDynamicEntitySpawnedThisFrame();
        state.genie().beginFrame();
        Arrays.fill(state.slotProperties().state.storageWingedSwordAttackThisFrame(), false);
        Arrays.fill(state.slotProperties().state.storageWingedStateTwoThisFrame(), false);
        state.events().clearTransientVfxRequests();
        state.events().clearPendingLinkFinalPositionRequests();
        state.events().clearPendingRoosterLinkStateRequests();
        state.events().clearPendingLinkMotionBlockRequests();
        state.events().clearPendingSideViewPlatformLinkRequests();
        state.events().clearPendingSideViewPotLinkRequests();
        state.events().clearPendingHinoxLinkEffectRequests();
        state.events().clearPendingLinkFallenPoseRequests();
        state.events().clearPendingLinkFacingRequests();
        state.events().clearPendingLinkAttackStepRequests();
        state.events().linkAttackStep(state.events().pendingPreTickLinkAttackStepRequests());
        state.events().clearPendingPreTickLinkAttackStepRequests();
        state.events().clearPendingLinkAttackClearRequests();
        state.events().clearPendingRoomStatusPersistenceRequests();
        state.events().clearPendingLinkHeldItemPoseRequests();
        state.events().clearPendingLinkSwordSpinPoseRequests();
        state.events().clearPendingLinkSwordFinalPoseRequests();
        state.events().clearPendingScreenShakeRequests();
    }

    private static void resetRewardsAndObjects(EntityFrameExecution state) {

        state.events().clearBoomerangObjectRequests();
        state.events().clearMagicRodObjectRequests();
        state.events().clearMagicPowderObjectRequests();
        state.events().clearPendingDialogRequests();
        state.events().clearPendingEntityEvents();
        state.events().clearPendingThrownBackgroundCollisions();
        state.events().combat(state.events().pendingPreTickEntityEvents());
        state.events().clearPendingPreTickEntityEvents();
        state.events().clearPendingWarpLinkStateRequests();
        state.events().clearPendingChestRewardEvents();
        state.events().clearPendingHeartContainerRewards();
        state.events().clearPendingMadamMeowMeowFullHealRequests();
        state.events().clearPendingSwordPickupRewards();
        state.events().clearPendingToadstoolRewards();
        state.events().clearPendingInstrumentRewards();
        state.events().clearPendingInstrumentCompletions();
        state.events().clearPendingRichardProgressEvents();
    }

    private static void prepareLateFrameEvents(EntityFrameExecution state) {

        if (state.bossRewards().state.readBossAgonySfxCountdown() > 0) {
            state.bossRewards().state.postBossAgonySfxCountdown(-1);
            if (state.bossRewards().state.readBossAgonySfxCountdown() == 0) {
                state.events()
                        .sound(
                                state.bossRewards().state.readBossAgonySourceSlot(),
                                EntityRomConstants.ENTITY_GENIE,
                                EntityCombatEvent.SoundChannel.WAVE,
                                0x10);
            }
        }
        state.events().clearPendingWitchExchangeEvents();
        state.events().clearPendingWitchRewardEvents();
        state.events().clearPendingOwlEventCompletions();
        state.events().clearPendingSlimeKeyRewardEvents();
        state.events().clearPendingKeyQuicksandEvents();
        state.events().clearPendingLikeLikeEvents();
        state.events().clearPendingBombExplosionEvents();
        state.events().clearHookshotBridgeUpdates();
        state.roomVariables().writePendingSwitchBlockAnimationRequest(false);
        if (state.linkState().readLinkPlayingOcarinaCountdown() >= 0x10
                && state.linkState().readOcarinaAnimationCounter() == 0x14) {
            state.playerSpawns()
                    .spawnMusicalNote(
                            state.inputs().linkEntityX(),
                            state.inputs().linkEntityY(),
                            state.linkState().readOcarinaAnimationPhase());
        }
        state.inputs().projectileEvents(state.events().outbox.channel(EntityProjectileEvent.class, "projectileEvents"));
        state.inputs().projectileEvents().clear();
    }

    private static void prepareFollowerState(EntityFrameExecution state) {

        if (state.inputs().linkPositionHistory() != null) {
            state.motion(FollowingNpcMotion.class, FollowingNpcMotion::new)
                    .writeFollowingLinkPositionHistory(state.inputs().linkPositionHistory());
        }
        state.linkState().writeFollowingLinkZ(state.inputs().linkZ() & 0xFF);
        state.linkState().writeFollowingLinkDirection(state.inputs().linkDirection() & 0xFF);
        state.linkState().writeFollowingEntityYOffset(state.inputs().entityYOffset() & 0xFF);
        state.slotFrame()
                .carryingEntity(state.roomInspection().liftedEntityState().carryState() != 0);
        state.inputs()
                .romLinkDirection(romDirectionForJavaDirection(state.inputs().linkDirection()));
        state.linkState().writeLastRomLinkDirection(state.inputs().romLinkDirection());
    }

    private static boolean prepareSlotAndTimers(EntityFrameExecution state) {

        if (!prepareSlot(state)) return false;
        if (!freezeTimers(state)) return false;
        if (!advanceTimersAndBossIntro(state)) return false;
        if (!resetPresentation(state)) return false;
        if (!advancePickupSequence(state)) return false;
        return true;
    }

    private static boolean prepareSlot(EntityFrameExecution state) {
        var slotState = state.slotFrame();
        var frameInput = state.inputs();
        var timerGates = state.gates();
        slotState.entity(state.slots().get(slotState.index()));
        if (state.liftThrow().advanceStatueRubble(slotState.entity(), timerGates.sideViewPotInteractive()))
            return false;
        slotState.moldormIntroOpenedThisFrame(false);
        slotState.rollingBonesHitThisFrame(slotState.entity().type() == EntityRomConstants.ENTITY_ROLLING_BONES
                && state.motion(RollingBonesMotion.class, RollingBonesMotion::new)
                                .swordHitFrame(slotState.index())
                        == frameInput.frame() + 1);
        state.motion(RollingBonesMotion.class, RollingBonesMotion::new).setSwordHitFrame(slotState.index(), 0);
        if (!slotState.entity().loaded()) {
            return false;
        }
        if (!state.handlers().tick(EntityTickStage.PHASE_1_ENTRY, state)) return false;
        if (state.physicsTables()
                        .readEnemyProjectileSpawnedThisFrame(slotState.entity().slot())
                || state.physicsTables()
                        .readDynamicEntitySpawnedThisFrame(slotState.entity().slot())) {
            return false;
        }
        if (!state.handlers().tick(EntityTickStage.PHASE_1_AFTER_SPAWN_GUARD, state)) return false;
        slotState.kanaletLeafProducer((slotState.entity().type() == EntityRomConstants.ENTITY_CROW
                        && !state.indoorRoom()
                        && state.roomVariables().readEntityRoomId() == 0x58)
                || slotState.entity().type() == EntityRomConstants.ENTITY_MAD_BOMBER
                || (slotState.entity().type() == EntityRomConstants.ENTITY_KNIGHT
                        && state.roomVariables().readEntityMapId() >= 0x14));
        if (slotState.kanaletLeafProducer()) {
            if (!state.handlers().tick(EntityTickStage.PHASE_1_LEAF_PRODUCER, state)) return false;
            if ((state.roomVariables().readEntityRoomStatus() & 0x10) != 0) {
                state.lifecycle()
                        .disableEntityWithoutPersistence(slotState.entity().slot());
                return false;
            }
            state.bossRewards()
                    .state
                    .writeDroppedItemBySlot(slotState.entity().slot(), EntityRomConstants.ENTITY_HIDING_SLIME_KEY);
            if (slotState.entity().sourceLoadOrder() != 0xFF) {
                slotState.entity(withSourceLoadOrder(slotState.entity(), 0xFF));
                state.slots().replace(slotState.index(), slotState.entity());
            }
        }
        if (!state.handlers().tick(EntityTickStage.PHASE_1_BEFORE_STATUS_TIMERS, state)) return false;
        slotState.originalEntity(slotState.entity());
        slotState.initialStatus(slotState.entity().status());
        slotState.wasInitializing(slotState.initialStatus() == EntityStatus.INIT);
        return true;
    }

    private static boolean freezeTimers(EntityFrameExecution state) {
        var slotState = state.slotFrame();
        var frameInput = state.inputs();
        var timerGates = state.gates();
        timerGates.freezeTarinTimers(!state.indoorRoom()
                && slotState.entity().type() == EntityRomConstants.ENTITY_TARIN
                && state.motion(TarinRaccoonMotion.class, TarinRaccoonMotion::new)
                                .state(slotState.entity().slot())
                        != 0
                && !state.roomInspection().tarinTransformationTimersInteractive());
        timerGates.freezeThreeOfAKindTimers(slotState.entity().type() == EntityRomConstants.ENTITY_THREE_OF_A_KIND
                && !timerGates.threeOfAKindInteractive());
        timerGates.freezeHinoxTimers(slotState.entity().type() == EntityRomConstants.ENTITY_HINOX
                && slotState.initialStatus() == EntityStatus.ACTIVE
                && (frameInput.creditsGameplay()
                        || !timerGates.handlerLinkCollisionEnabled()
                        || !state.background().entityTimersInteractive()
                        || state.roomVariables().readTransitionSequenceCounter() != 0x04));
        timerGates.freezeGenieTimers(slotState.entity().type() == EntityRomConstants.ENTITY_GENIE
                && (!state.background().gameplayEntityTimersInteractive(frameInput.creditsGameplay())
                        || state.roomInspection().witchGotItemPresentationActive()));
        timerGates.freezeWitchTimers(slotState.entity().type() == EntityRomConstants.ENTITY_WITCH
                && !timerGates.projectileTimersInteractive());
        timerGates.freezePowderTimers(slotState.entity().type() == EntityRomConstants.ENTITY_MAGIC_POWDER_SPRINKLE
                && !timerGates.projectileTimersInteractive());
        timerGates.freezeBooBuddyTimers(slotState.entity().type() == EntityRomConstants.ENTITY_BOO_BUDDY
                && !timerGates.projectileTimersInteractive());
        timerGates.freezeBowWowTimers(slotState.entity().type() == EntityRomConstants.ENTITY_BOW_WOW
                && !timerGates.projectileTimersInteractive());
        timerGates.freezeMoblinKingTimers(slotState.entity().type() == EntityRomConstants.ENTITY_MOBLIN_KING
                && !timerGates.projectileTimersInteractive());
        return true;
    }

    private static boolean advanceTimersAndBossIntro(EntityFrameExecution state) {
        var slotState = state.slotFrame();
        var timerGates = state.gates();
        timerGates.ignoreHitsDecrementedBeforeHandler(!timerGates.freezeTarinTimers()
                && !timerGates.freezeThreeOfAKindTimers()
                && !timerGates.freezeHinoxTimers()
                && !timerGates.freezeGenieTimers()
                && !timerGates.freezeWitchTimers()
                && !timerGates.freezePowderTimers()
                && !timerGates.freezeBooBuddyTimers()
                && !timerGates.freezeBowWowTimers()
                && !timerGates.freezeMoblinKingTimers()
                && !slotState.rollingBonesHitThisFrame()
                && state.lifecycle()
                        .decrementEnemyCombatCountdowns(
                                slotState.entity().slot(),
                                !slotState.wasInitializing()
                                        && slotState.entity().type() != EntityRomConstants.ENTITY_BOO_BUDDY
                                        && slotState.entity().type() != EntityRomConstants.ENTITY_BOW_WOW
                                        && slotState.entity().type() != EntityRomConstants.ENTITY_SLIME_EYE));
        if (!timerGates.freezeTarinTimers()
                && !timerGates.freezeThreeOfAKindTimers()
                && !timerGates.freezeHinoxTimers()
                && !timerGates.freezeGenieTimers()
                && !timerGates.freezePowderTimers()
                && !timerGates.freezeBooBuddyTimers()
                && !timerGates.freezeBowWowTimers()
                && !timerGates.freezeWitchTimers()
                && !timerGates.freezeMoblinKingTimers()) {
            state.lifecycle().decrementEnemyStatusCountdowns(slotState.entity().slot());
            if (!state.handlers().tick(EntityTickStage.PHASE_1_AFTER_STATUS_TIMERS, state)) return false;
        }
        if (!state.handlers().tick(EntityTickStage.PHASE_1_BEFORE_BOSS_INTRO, state)) return false;
        if (slotState.entity().sourceLoadOrder() == -1
                && state.enemyInspection().isDisabledFollower(slotState.entity().type())) {
            state.lifecycle().clearEntity(slotState.entity().slot());
            return false;
        }

        // label_394D -> $14:54AC runs before these bosses' interaction
        // guards, including during their death handlers. It directly
        // nudges Link into the arena, then blocks his next input frame.
        if (!state.handlers().tick(EntityTickStage.PHASE_1_BOSS_DOORWAY, state)) return false;

        // Moldorm calls BossIntro before dispatching active/death states ($04:56C0).
        if (!state.handlers().tick(EntityTickStage.PHASE_1_BOSS_INTRO, state)) return false;

        slotState.status(slotState.entity().status());
        return true;
    }

    private static boolean resetPresentation(EntityFrameExecution state) {
        var slotState = state.slotFrame();
        var presentationFlags = state.presentation();
        presentationFlags.reset();
        slotState.keyDropTransitionActive(false);
        slotState.preAppliedGroundResult(null);
        slotState.applyGenericGroundInteraction(slotState.entity().type() != EntityRomConstants.ENTITY_ZOMBIE
                && slotState.entity().type() != EntityRomConstants.ENTITY_BUZZ_BLOB
                && slotState.entity().type() != EntityRomConstants.ENTITY_SAND_CRAB
                && slotState.entity().type() != EntityRomConstants.ENTITY_DOG
                && slotState.entity().type() != EntityRomConstants.ENTITY_URCHIN);
        return true;
    }

    private static boolean advancePickupSequence(EntityFrameExecution state) {
        var slotState = state.slotFrame();
        var frameInput = state.inputs();
        if (slotState.status() == EntityStatus.ACTIVE) {
            state.pickups().decrementEnemyDropCountdowns(slotState.entity());
        }
        if (!state.handlers().tick(EntityTickStage.PHASE_1_BEFORE_PICKUP_SEQUENCE, state)) return false;
        if (slotState.status() == EntityStatus.ACTIVE
                && state.rewards()
                                .state
                                .readPowerUpPickupCountdown(slotState.entity().slot())
                        != 0) {
            state.rewards()
                    .advancePowerUpPickup(
                            slotState.entity(),
                            frameInput.frame(),
                            frameInput.linkEntityX(),
                            frameInput.linkEntityY(),
                            frameInput.linkZ());
            return false;
        }
        if (!state.handlers().tick(EntityTickStage.PHASE_1_AFTER_INSTRUMENT_PICKUP, state)) return false;
        return true;
    }

    private static boolean advanceSlotStatus(EntityFrameExecution state) {
        var slotState = state.slotFrame();
        var frameInput = state.inputs();
        var timerGates = state.gates();
        var presentationFlags = state.presentation();

        if (!state.handlers().tick(EntityTickStage.PHASE_2_BEFORE_STATUS_BRANCHES, state)) return false;

        if (!advanceStatus(state)) return false;
        slotState.updated(slotState.entity());
        if (!state.handlers().tick(EntityTickStage.PHASE_2_BEFORE_RECOIL, state)) return false;
        if (presentationFlags.preserveGhiniPresentation()) {
            slotState.updated(withVariant(slotState.entity(), -1));
        }
        if (!state.handlers().tick(EntityTickStage.PHASE_2_BEFORE_RECOIL_APPLICATION, state)) return false;
        if (slotState.status() == EntityStatus.ACTIVE
                && !slotState.wasInitializing()
                && (usesSharedRecoil(slotState.entity().type())
                        || (slotState.entity().type() == EntityRomConstants.ENTITY_POKEY
                                && state.motion(PokeyMotion.class, PokeyMotion::new)
                                        .isSegment(slotState.entity().slot())
                                && timerGates.handlerLinkCollisionEnabled()))
                && slotState.entity().type() != EntityRomConstants.ENTITY_ROLLING_BONES
                && slotState.entity().type() != EntityRomConstants.ENTITY_MINI_MOLDORM
                && slotState.entity().type() != EntityRomConstants.ENTITY_BOO_BUDDY
                && (slotState.entity().type() != EntityRomConstants.ENTITY_MASKED_MIMIC_GORIYA
                        || state.roomVariables().readEntityMapId() != 0x1F)
                && ((slotState.entity().type() != EntityRomConstants.ENTITY_STAR
                                && slotState.entity().type() != EntityRomConstants.ENTITY_BLOOPER)
                        || timerGates.handlerLinkCollisionEnabled())
                && ((slotState.entity().type() != EntityRomConstants.ENTITY_ZOMBIE
                                && slotState.entity().type() != EntityRomConstants.ENTITY_BUZZ_BLOB
                                && slotState.entity().type() != EntityRomConstants.ENTITY_SAND_CRAB)
                        || timerGates.handlerLinkCollisionEnabled())) {
            // Bank-$03 AnimateRoamingEnemy and the bank-$04/$06/$07
            // handlers apply the shared recoil before their own movement.
            EnemyRecoilMotion.Update recoil =
                    state.background().applyEnemyRecoilIfNeeded(slotState.entity(), frameInput.backgroundCollision());
            slotState.entity(recoil.entity());
            slotState.updated(slotState.entity());
        }
        if (!state.handlers().tick(EntityTickStage.PHASE_2_AFTER_RECOIL_APPLICATION, state)) return false;
        if (slotState.status() == EntityStatus.ACTIVE
                && !slotState.wasInitializing()
                && state.handlers().hasAdvance(slotState.entity().type())) {
            slotState.updated(state.handlers()
                    .advance(
                            slotState.entity(),
                            state.context()
                                    .withRuntimeInputs(
                                            frameInput.frame(),
                                            frameInput.linkEntityX(),
                                            frameInput.linkEntityY(),
                                            frameInput.backgroundCollision())));
            if (slotState.entity().type() == EntityRomConstants.ENTITY_LEEVER
                    && timerGates.handlerInteractive()
                    && timerGates.handlerLinkCollisionEnabled()) {
                state.combat().finishLeeverContactRecoil(slotState.updated());
            }
        }

        if (!state.handlers().tick(EntityTickStage.PHASE_2_AFTER_LEEVER_CONTACT, state)) return false;
        return true;
    }

    private static boolean advanceLifted(EntityFrameExecution state) {
        var slotState = state.slotFrame();
        var frameInput = state.inputs();

        slotState.lifted(state.liftThrow().renderLiftedEntity(slotState.entity(), frameInput.frame()));
        // Side-view pot pickup enters status LIFTED with the ROM's
        // transition countdown set to $02. UpdateEntityTimers has
        // already decremented it for this frame; EntityLiftedHandler
        // does not advance the pot until that countdown reaches zero.
        // Other state.lifted entities retain their established generic
        // presentation timing here.
        if (!state.handlers().tick(EntityTickStage.PHASE_2_LIFTED, state)) return false;
        slotState.liftedRomDirection(frameInput.romLinkDirection());
        if (!state.handlers().tick(EntityTickStage.PHASE_2_LIFTED_FOLLOWER, state)) return false;
        state.slots()
                .replace(
                        slotState.index(),
                        state.liftThrow()
                                .advanceLiftedEntity(
                                        slotState.lifted(),
                                        frameInput.linkEntityX(),
                                        frameInput.linkEntityY(),
                                        frameInput.linkZ(),
                                        slotState.liftedRomDirection()));
        return false;
    }

    private static boolean advanceThrown(EntityFrameExecution state) {
        var slotState = state.slotFrame();
        var frameInput = state.inputs();

        if (!state.handlers().tick(EntityTickStage.PHASE_2_THROWN, state)) return false;
        RoomEntity thrown = state.liftThrow()
                .advanceThrownEntity(
                        slotState.entity(),
                        frameInput.backgroundCollision(),
                        state.physicsTables().readGroundInteractionSideScrolling(),
                        frameInput.romLinkDirection(),
                        frameInput.frame());
        state.slots().replace(slotState.index(), thrown);
        if (thrown.status() == EntityStatus.THROWN) {
            state.liftThrow().collideThrownEntityWithEntities(thrown, frameInput.frame());
        }
        return false;
    }

    private static boolean advanceFalling(EntityFrameExecution state) {
        var slotState = state.slotFrame();

        if (!state.handlers().tick(EntityTickStage.PHASE_2_FALLING, state)) return false;
        RoomEntity falling = state.background().advanceFallingEntity(slotState.entity());
        if (!falling.loaded()) {
            state.lifecycle().disableEntityWithoutPersistence(slotState.entity().slot());
            return false;
        }
        int renderFlipAttribute = state.physicsTables()
                .readBaseEntityFlipAttribute(slotState.entity().slot());
        if (state.combatTables().readEnemyFlashCountdown(slotState.entity().slot()) > 0) {
            renderFlipAttribute ^= (state.combatTables()
                                    .readEnemyFlashCountdown(slotState.entity().slot())
                            << 2)
                    & 0x10;
        }
        state.slots()
                .replace(
                        slotState.index(),
                        new RoomEntity(
                                falling.slot(),
                                falling.sourceLoadOrder(),
                                falling.type(),
                                falling.x(),
                                falling.y(),
                                EntityStatus.FALLING,
                                falling.spriteDefinition(),
                                falling.spriteVariant(),
                                renderFlipAttribute,
                                falling.spriteTileOffset(),
                                falling.z(),
                                -1,
                                false));
        return false;
    }

    private static boolean advanceDying(EntityFrameExecution state) {
        var slotState = state.slotFrame();
        var frameInput = state.inputs();

        if (!state.handlers().tick(EntityTickStage.PHASE_2_DYING, state)) return false;
        if (isBossKeyDropProducer(slotState.entity().type())) {
            state.bossRewards().advanceBossKeyDropProducer(slotState.entity(), frameInput.randomByteSupplier());
            return false;
        }
        if (state.combatTables().readDyingCountdown(slotState.entity().slot()) == 0) {
            state.bossRewards().handleTerminalEnemyDeath(slotState.entity(), frameInput.randomByteSupplier());
        } else {
            int slot = slotState.entity().slot();
            int presentationCountdown = state.combatTables().readDyingCountdown(slot);
            // EntityDeathHandler ($03:557D): the initial counter is a
            // ceiling, not a fixed wait. Begin destruction once the
            // hit recoil has expired; bosses use their handlers above.
            if (presentationCountdown >= 0x20 && state.combatTables().readEnemyIgnoreHitsCountdown(slot) == 0) {
                state.combatTables().writeDyingCountdown(slot, 0x1F);
                state.events().sound(slot, slotState.entity().type(), EntityCombatEvent.SoundChannel.NOISE, 0x13);
            }
            RoomEntity updated = state.combatTables().readEnemyIgnoreHitsCountdown(slot) > 0
                    ? state.background()
                            .applyEnemyRecoilIfNeeded(slotState.entity(), frameInput.backgroundCollision())
                            .entity()
                    : slotState.entity();
            updated = state.slotProperties().refreshColorShellDisplay(updated, slotState.status());
            state.slots()
                    .replace(
                            slotState.index(),
                            withDeathPresentation(
                                    updated,
                                    EntityMotionMath.deathSpriteVariantForCountdown(presentationCountdown),
                                    state.combatTables().readPowerRecoilDeath(slot)));
        }
        return false;
    }

    private static boolean advanceBurning(EntityFrameExecution state) {
        var slotState = state.slotFrame();

        if (state.physicsTables()
                        .readEnemyTransitionCountdown(slotState.entity().slot())
                == 0) {
            state.lifecycle().finishBurning(slotState.entity());
        } else {
            state.slots()
                    .replace(
                            slotState.index(),
                            state.slotProperties().refreshColorShellDisplay(slotState.entity(), slotState.status()));
        }
        return false;
    }

    private static boolean advanceStunned(EntityFrameExecution state) {
        var slotState = state.slotFrame();
        var frameInput = state.inputs();

        if (!state.handlers().tick(EntityTickStage.PHASE_2_STUNNED, state)) return false;
        if (state.linkState().readPowerBraceletButtonHeld()
                && isLiftableEntity(slotState.entity())
                && (slotState.entity().type() == EntityRomConstants.ENTITY_BOMB
                        || (state.physicsTables()
                                                .readEnemyPhysicsFlags(
                                                        slotState.entity().slot())
                                        & EntityRomConstants.ENTITY_PHYSICS_GRABBABLE)
                                != 0)
                && RoomEntityPickupRules.overlapsLink(
                        slotState.entity(), frameInput.linkEntityX(), frameInput.linkEntityY())) {
            if (state.playerCommands().beginLift(slotState.entity().slot(), frameInput.romLinkDirection())) {
                state.slots()
                        .replace(
                                slotState.index(),
                                state.liftThrow()
                                        .advanceLiftedEntity(
                                                state.liftThrow()
                                                        .renderLiftedBomb(
                                                                state.slots().get(slotState.index())),
                                                frameInput.linkEntityX(),
                                                frameInput.linkEntityY(),
                                                frameInput.linkZ(),
                                                frameInput.romLinkDirection()));
            }
            return false;
        }
        if (state.combatTables().readEnemyStunnedCountdown(slotState.entity().slot()) == 0) {
            state.slots()
                    .replace(
                            slotState.index(),
                            state.slotProperties()
                                    .refreshColorShellDisplay(
                                            withStatus(slotState.entity(), EntityStatus.ACTIVE), EntityStatus.ACTIVE));
        } else {
            state.slots()
                    .replace(
                            slotState.index(),
                            state.slotProperties().refreshColorShellDisplay(slotState.entity(), slotState.status()));
        }
        return false;
    }

    private static boolean advanceInit(EntityFrameExecution state) {
        var slotState = state.slotFrame();
        var frameInput = state.inputs();

        if (!state.handlers().tick(EntityTickStage.PHASE_2_BEFORE_INITIALIZE, state)) return false;
        if (!state.lifecycle().initializeBossBattle(slotState.entity())) return false;
        slotState.status(EntityStatus.ACTIVE);
        state.pickups().initializeEntityTimers(slotState.entity());
        state.handlers().initialize(slotState.entity(), frameInput.randomByteSupplier());

        if (!state.handlers().tick(EntityTickStage.PHASE_2_AFTER_INITIALIZE, state)) return false;

        return true;
    }

    private static boolean advanceActive(EntityFrameExecution state) {
        var slotState = state.slotFrame();
        var frameInput = state.inputs();
        var timerGates = state.gates();

        if (!timerGates.freezeTarinTimers() && !timerGates.freezeMoblinKingTimers()) {
            state.lifecycle()
                    .decrementSlowTransitionCountdown(slotState.entity().slot(), frameInput.frame());
        }
        if (state.lifecycle().shouldDisappear(slotState.entity())) {
            if (state.physicsTables()
                            .readSlowTransitionCountdown(slotState.entity().slot())
                    == 0) {
                state.lifecycle().clearEntity(slotState.entity().slot());
                return false;
            }
        }

        return true;
    }

    private static boolean advanceStatus(EntityFrameExecution state) {
        var slotState = state.slotFrame();
        if (slotState.status() == EntityStatus.LIFTED) {
            return advanceLifted(state);
        } else if (slotState.status() == EntityStatus.THROWN) {
            return advanceThrown(state);
        } else if (slotState.status() == EntityStatus.FALLING) {
            return advanceFalling(state);
        } else if (slotState.status() == EntityStatus.DYING) {
            return advanceDying(state);
        } else if (slotState.status() == EntityStatus.BURNING) {
            return advanceBurning(state);
        } else if (slotState.status() == EntityStatus.STUNNED) {
            return advanceStunned(state);
        } else if (slotState.status() == EntityStatus.INIT) {
            return advanceInit(state);
        } else if (slotState.status() == EntityStatus.ACTIVE) {
            return advanceActive(state);
        }
        return true;
    }

    private static boolean advanceLaserSite(EntityFrameExecution state) {

        if (!state.handlers().tick(EntityTickStage.PHASE_3_BEFORE_LASER, state)) return false;
        if (!advanceLaserParent(state)) return false;
        if (!advanceLaserSensor(state)) return false;
        if (!advanceLaserBeam(state)) return false;
        if (!state.handlers().tick(EntityTickStage.PHASE_3_AFTER_LASER, state)) return false;
        return true;
    }

    private static boolean advanceLaserParent(EntityFrameExecution state) {
        var slotState = state.slotFrame();
        if (slotState.status() == EntityStatus.ACTIVE
                && !slotState.wasInitializing()
                && state.motion(LaserMotion.class, LaserMotion::new)
                        .isParent(slotState.entity().slot())) {
            LaserMotion.ParentUpdate laserUpdate =
                    state.motion(LaserMotion.class, LaserMotion::new).advanceParent(slotState.entity());
            slotState.updated(laserUpdate.entity());
            if (laserUpdate.spawnSensor()) {
                state.enemySpawns().spawnLaserSensor(slotState.entity());
            }
            if (laserUpdate.spawnBeam()) {
                if (state.enemySpawns().spawnLaserBeam(slotState.entity())) {
                    state.events()
                            .sound(
                                    slotState.entity().slot(),
                                    slotState.entity().type(),
                                    EntityCombatEvent.SoundChannel.NOISE,
                                    0x08);
                }
            }
        }
        return true;
    }

    private static boolean advanceLaserSensor(EntityFrameExecution state) {
        var slotState = state.slotFrame();
        var frameInput = state.inputs();
        if (slotState.status() == EntityStatus.ACTIVE
                && !slotState.wasInitializing()
                && state.motion(LaserMotion.class, LaserMotion::new)
                        .isSensor(slotState.entity().slot())) {
            int ownerSlot = state.motion(LaserMotion.class, LaserMotion::new)
                    .parentSlot(slotState.entity().slot());
            RoomEntity owner = ownerSlot >= 0 && ownerSlot < state.slots().size()
                    ? state.slots().get(ownerSlot)
                    : null;
            LaserMotion.SensorUpdate sensorUpdate = state.motion(LaserMotion.class, LaserMotion::new)
                    .advanceSensor(
                            slotState.entity(),
                            owner,
                            frameInput.linkEntityX(),
                            frameInput.linkEntityY(),
                            frameInput.projectileLinkState().invincibilityCounter());
            if (sensorUpdate.triggeredParent()
                    && ownerSlot >= 0
                    && ownerSlot < state.slots().size()) {
                // LaserLinkSensorHandler starts the parent's visible
                // pre-fire flash at the same time as its $20 countdown.
                state.combatTables().writeEnemyFlashCountdown(ownerSlot, 0x10);
            }
            if (sensorUpdate.unloaded()) {
                state.lifecycle()
                        .disableEntityWithoutPersistence(slotState.entity().slot());
                return false;
            }
            slotState.updated(sensorUpdate.entity());
        }
        return true;
    }

    private static boolean advanceLaserBeam(EntityFrameExecution state) {
        var slotState = state.slotFrame();
        var frameInput = state.inputs();
        if (slotState.status() == EntityStatus.ACTIVE
                && !slotState.wasInitializing()
                && state.motion(LaserMotion.class, LaserMotion::new)
                        .isBeam(slotState.entity().slot())) {
            var collisionEvent = EnemyProjectileCollision.check(
                    slotState.entity(),
                    state.motion(LaserMotion.class, LaserMotion::new)
                            .direction(slotState.entity().slot()),
                    frameInput.projectileLinkState());
            if (collisionEvent.isPresent()) {
                EntityProjectileEvent event = collisionEvent.orElseThrow();
                frameInput.projectileEvents().add(event);
                if (event.remove()) {
                    state.lifecycle()
                            .disableEntityWithoutPersistence(slotState.entity().slot());
                    return false;
                }
                // Bank-$15 moves the beam before consuming the collision
                // byte.  A reflected beam therefore advances once in
                // this frame, then reverses the selected speed axis.
                LaserMotion.BeamUpdate beamUpdate = state.motion(LaserMotion.class, LaserMotion::new)
                        .advanceBeam(slotState.entity(), frameInput.backgroundCollision());
                if (beamUpdate.unloaded()) {
                    state.lifecycle()
                            .disableEntityWithoutPersistence(slotState.entity().slot());
                    return false;
                }
                slotState.updated(beamUpdate.entity());
                state.motion(LaserMotion.class, LaserMotion::new)
                        .reflectBeam(
                                slotState.entity().slot(),
                                frameInput.projectileLinkState().direction());
                if (event.swordPokeVfx()) {
                    state.events()
                            .vfx(new TransientVfxRequest(
                                    TransientVfxType.SWORD_POKE,
                                    slotState.updated().x(),
                                    slotState.updated().y()));
                }
            } else {
                LaserMotion.BeamUpdate beamUpdate = state.motion(LaserMotion.class, LaserMotion::new)
                        .advanceBeam(slotState.entity(), frameInput.backgroundCollision());
                if (beamUpdate.unloaded()) {
                    state.lifecycle()
                            .disableEntityWithoutPersistence(slotState.entity().slot());
                    return false;
                }
                slotState.updated(beamUpdate.entity());
                state.events()
                        .vfx(new TransientVfxRequest(
                                TransientVfxType.LASER_BEAM,
                                slotState.updated().x() + 0x04,
                                slotState.updated().y()));
            }
        }
        return true;
    }

    private static boolean advanceFollowersAndMotion(EntityFrameExecution state) {
        var slotState = state.slotFrame();
        var frameInput = state.inputs();

        if (!state.handlers().tick(EntityTickStage.PHASE_4_BEFORE_FOLLOWER, state)) return false;
        if (slotState.status() == EntityStatus.ACTIVE
                && !slotState.wasInitializing()
                && (isDynamicFollowingNpc(slotState.entity())
                        || slotState.entity().type() == EntityRomConstants.ENTITY_BOW_WOW)) {
            if (slotState.entity().type() == EntityRomConstants.ENTITY_BOW_WOW) {
                BowWowMotion.Update bowWowUpdate = state.motion(BowWowMotion.class, BowWowMotion::new)
                        .advanceAfterTimers(new BowWowCollisionFrame(
                                slotState.entity(),
                                frameInput.frame(),
                                frameInput.linkEntityX(),
                                frameInput.linkEntityY(),
                                state.linkState().readFollowingLinkZ(),
                                frameInput.randomByteSupplier(),
                                frameInput.backgroundCollision(),
                                state.slots().view(),
                                state.enemyCombatTables()::canBowWowEatEntity,
                                state.motion(BowWowMotion.class, BowWowMotion::new)
                                        .readBowWowState()));
                slotState.updated(bowWowUpdate.entity());
                if (bowWowUpdate.targetContact() != null) {
                    state.bossRewards()
                            .handleBowWowTargetContact(
                                    slotState.entity(), bowWowUpdate.targetContact(), frameInput.randomByteSupplier());
                }
            } else {
                slotState.updated(state.motion(FollowingNpcMotion.class, FollowingNpcMotion::new)
                        .advance(new FollowingNpcFrame(
                                slotState.entity(),
                                frameInput.frame(),
                                frameInput.linkEntityX(),
                                frameInput.linkEntityY(),
                                state.linkState().readFollowingLinkZ(),
                                state.linkState().readFollowingLinkDirection(),
                                state.linkState().readFollowingEntityYOffset(),
                                state.motion(FollowingNpcMotion.class, FollowingNpcMotion::new)
                                        .readFollowingLinkPositionHistory(),
                                frameInput.backgroundCollision())));
            }
        }
        return true;
    }

    private static boolean applyGroundAndCommitSlot(EntityFrameExecution state) {
        var slotState = state.slotFrame();
        var frameInput = state.inputs();

        if (!state.handlers().tick(EntityTickStage.PHASE_5_BEFORE_COMMIT, state)) return false;
        RoomEntity beforeDropPhysics = slotState.updated();
        if (slotState.status() == EntityStatus.ACTIVE
                && !slotState.wasInitializing()
                && !slotState.keyDropTransitionActive()
                && state.bossRewards()
                        .state
                        .readEnemyDropActive(slotState.updated().slot())) {
            slotState.updated(state.motion(EnemyDropMotion.class, EnemyDropMotion::new)
                    .advance(
                            slotState.updated(),
                            frameInput.frame(),
                            state.physicsTables()
                                    .readEntityGroundStatus(slotState.updated().slot()),
                            state.physicsTables().readGroundInteractionSideScrolling()));
        }
        if (!applyGroundInteraction(state, beforeDropPhysics)) return false;
        if (!applyFinalPresentation(state)) return false;

        return true;
    }

    private static void bounceDroppedItem(
            EntityFrameExecution state, RoomEntity beforeDropPhysics, RoomEntityGroundInteraction.Result groundResult) {
        var slotState = state.slotFrame();
        var frameInput = state.inputs();
        if (slotState.status() == EntityStatus.ACTIVE
                && state.bossRewards()
                        .state
                        .readEnemyDropActive(slotState.updated().slot())) {
            // BouncingEntityPhysics calls the complete background helper before
            // bouncing. Ground effects alone omit the neighbouring bush walls.
            RoomEntityBackgroundInteraction dropBackground = frameInput.hinoxBackgroundInteraction();
            slotState.updated(state.motion(EnemyDropMotion.class, EnemyDropMotion::new)
                    .resolveWalls(
                            beforeDropPhysics,
                            slotState.updated(),
                            dropBackground,
                            state.combatTables()
                                    .readEnemyIgnoreHitsCountdown(
                                            slotState.updated().slot()),
                            frameInput.frame()));
            boolean dropGroundCollision = state.physicsTables().readGroundInteractionSideScrolling()
                    && state.motion(EnemyDropMotion.class, EnemyDropMotion::new)
                                    .speedY(slotState.updated().slot())
                            != 0
                    && (state.motion(EnemyDropMotion.class, EnemyDropMotion::new)
                                            .speedY(slotState.updated().slot())
                                    & 0x80)
                            == 0
                    && frameInput
                            .backgroundCollision()
                            .blocks(
                                    slotState.updated(),
                                    EntityBackgroundCollisionResult.DOWN,
                                    slotState.updated().x(),
                                    slotState.updated().y());
            slotState.updated(state.motion(EnemyDropMotion.class, EnemyDropMotion::new)
                    .bounce(
                            slotState.updated(),
                            groundResult.groundStatus(),
                            state.physicsTables().readGroundInteractionSideScrolling(),
                            dropGroundCollision));
        }
    }

    private static boolean applyGroundInteraction(EntityFrameExecution state, RoomEntity beforeDropPhysics) {
        var slotState = state.slotFrame();
        var frameInput = state.inputs();
        var timerGates = state.gates();
        if (slotState.status() == EntityStatus.ACTIVE
                && !slotState.wasInitializing()
                && !slotState.keyDropTransitionActive()
                && slotState.applyGenericGroundInteraction()
                && !state.background().hasNoGroundInteraction(slotState.updated())
                && (timerGates.handlerLinkCollisionEnabled()
                        || slotState.updated().type() != EntityRomConstants.ENTITY_BLOOPER)) {
            // ApplyEntityInteractionWithBackground runs after each ROM
            // entity handler's movement and before the final display-list
            // presentation. The room session supplies terrain physics;
            // direct runtime users retain the no-op default.
            int previousGroundStatus = state.physicsTables()
                    .readEntityGroundStatus(slotState.updated().slot());
            RoomEntityGroundInteraction.Result groundResult = slotState.preAppliedGroundResult() != null
                    ? slotState.preAppliedGroundResult()
                    : Objects.requireNonNull(
                            state.physicsTables()
                                    .readGroundInteraction()
                                    .apply(
                                            slotState.updated(),
                                            frameInput.frame(),
                                            previousGroundStatus,
                                            state.background().verticalSpeedZ(slotState.updated()),
                                            state.physicsTables().readGroundInteractionSideScrolling()),
                            "Room entity ground interaction returned null");
            slotState.updated(Objects.requireNonNull(
                    groundResult.entity(), "Room entity ground interaction returned a null entity"));
            state.physicsTables()
                    .writeEntityGroundStatus(slotState.updated().slot(), groundResult.groundStatus() & 0xFF);
            if (groundResult.pitTransition() != null
                    && state.background()
                            .beginFalling(
                                    slotState.updated(),
                                    groundResult.pitTransition(),
                                    timerGates.ignoreHitsDecrementedBeforeHandler())) {
                slotState.status(EntityStatus.FALLING);
            }
            if (groundResult.waterSplash()) {
                state.events()
                        .vfx(new TransientVfxRequest(
                                TransientVfxType.WATER_SPLASH,
                                slotState.updated().x(),
                                slotState.updated().y()));
                state.events()
                        .sound(
                                slotState.updated().slot(),
                                slotState.updated().type(),
                                EntityCombatEvent.SoundChannel.JINGLE,
                                0x0E);
            }
            if (groundResult.unloaded()) {
                state.lifecycle()
                        .disableEntityWithoutPersistence(slotState.updated().slot());
                return false;
            }
            bounceDroppedItem(state, beforeDropPhysics, groundResult);
        }
        return true;
    }

    private static boolean applyFinalPresentation(EntityFrameExecution state) {
        var slotState = state.slotFrame();
        var frameInput = state.inputs();
        var timerGates = state.gates();
        var presentationFlags = state.presentation();
        slotState.genericLinkPushPolicy(
                EntityLinkCollisionRules.genericPolicyFor(slotState.updated().type()));
        if (slotState.status() == EntityStatus.ACTIVE
                && !slotState.wasInitializing()
                && timerGates.handlerLinkCollisionEnabled()
                && slotState.updated().type() != EntityRomConstants.ENTITY_GENIE
                && slotState.genericLinkPushPolicy().restoreFinalPosition()
                && RoomEntityCombatRules.overlapsLink(
                        slotState.updated(), frameInput.linkEntityX(), frameInput.linkEntityY())) {
            // The shared PushLinkOutOfEntity_* helpers run after their
            // handler-specific work. Restore the pre-entity final
            // position and carry the helper's other side effects across
            // the room/runtime boundary.
            state.background().requestLinkPush(slotState.updated(), slotState.genericLinkPushPolicy());
        }
        if (ColorShellMotion.isColorShellType(slotState.updated().type())) {
            slotState.updated(state.slotProperties().refreshColorShellDisplay(slotState.updated(), slotState.status()));
        }
        if (slotState.status() == EntityStatus.ACTIVE && isRuntimeFloatingItem(slotState.updated())) {
            slotState.updated(withZ(
                    slotState.updated(),
                    FloatingItemMotion.zForFrame(
                            state.physicsTables().readGroundInteractionSideScrolling(), frameInput.frame())));
        }
        slotState.variant(
                presentationFlags.preservesHandlerPresentation()
                        ? slotState.updated().spriteVariant()
                        : EntityTypeRules.variantFor(slotState.updated(), frameInput.frame()));
        if (slotState.status() == EntityStatus.ACTIVE && state.lifecycle().shouldDisappear(slotState.entity())) {
            slotState.variant(
                    (state.physicsTables()
                                                    .readSlowTransitionCountdown(
                                                            slotState.entity().slot())
                                            & 0x01)
                                    != 0
                            ? 0
                            : -1);
        }
        commitPresentation(state);

        return true;
    }

    private static void commitPresentation(EntityFrameExecution state) {
        var slotState = state.slotFrame();
        var presentationFlags = state.presentation();
        slotState.renderFlipAttribute(
                presentationFlags.preservesHandlerPresentation()
                        ? slotState.updated().entityFlipAttribute()
                        : state.physicsTables()
                                .readBaseEntityFlipAttribute(slotState.entity().slot()));
        if (presentationFlags.preserveBombitePresentation()
                && slotState.updated().type() == EntityRomConstants.ENTITY_TIMER_BOMBITE) {
            slotState.renderFlipAttribute(slotState.renderFlipAttribute()
                    | ((state.bombInteractions()
                                            .state
                                            .readBombPrivateCountdown1(
                                                    slotState.updated().slot())
                                    << 3)
                            & 0x10));
        }
        if (state.combatTables().readEnemyFlashCountdown(slotState.entity().slot()) > 0) {
            slotState.renderFlipAttribute(slotState.renderFlipAttribute()
                    ^ ((state.combatTables()
                                            .readEnemyFlashCountdown(
                                                    slotState.entity().slot())
                                    << 2)
                            & 0x10));
        }
        if (slotState.status() != slotState.entity().status()
                || slotState.variant() != slotState.entity().spriteVariant()
                || slotState.updated().type() != slotState.entity().type()
                || slotState.updated().spriteDefinition() != slotState.entity().spriteDefinition()
                || slotState.updated().x() != slotState.originalEntity().x()
                || slotState.updated().y() != slotState.originalEntity().y()
                || slotState.renderFlipAttribute() != slotState.originalEntity().entityFlipAttribute()
                || slotState.updated().z() != slotState.originalEntity().z()) {
            state.slots()
                    .replace(
                            slotState.index(),
                            withPresentation(
                                    slotState.updated(),
                                    slotState.status(),
                                    slotState.variant(),
                                    slotState.renderFlipAttribute()));
        }
    }
}
