package linksawakening.world.entity;

import static linksawakening.world.entity.EntityMotionMath.*;
import static linksawakening.world.entity.EntityRomConstants.*;
import static linksawakening.world.entity.EntitySnapshots.*;
import static linksawakening.world.entity.EntityTypeRules.*;

import linksawakening.world.entity.motion.*;

/** Ordered slot dispatch phase 1; false preserves the ROM handler early return. */
final class EntitySlotPhase1 {
    static boolean run(EntityFrameExecution state) {

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
        slotState.entity(state.runtime().slots[slotState.index()]);
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
        slotState.kanaletLeafProducer(slotState.entity().type() == EntityRomConstants.ENTITY_CROW
                        && !state.runtime().indoorRoom
                        && state.roomVariables().readEntityRoomId() == 0x58
                || slotState.entity().type() == EntityRomConstants.ENTITY_MAD_BOMBER
                || slotState.entity().type() == EntityRomConstants.ENTITY_KNIGHT
                        && state.roomVariables().readEntityMapId() >= 0x14);
        if (slotState.kanaletLeafProducer()) {
            if (!state.handlers().tick(EntityTickStage.PHASE_1_LEAF_PRODUCER, state)) return false;
            if ((state.roomVariables().readEntityRoomStatus() & 0x10) != 0) {
                state.lifecycle()
                        .disableEntityWithoutPersistence(slotState.entity().slot());
                return false;
            }
            state.bossRewards()
                    .writeDroppedItemBySlot(slotState.entity().slot(), EntityRomConstants.ENTITY_HIDING_SLIME_KEY);
            if (slotState.entity().sourceLoadOrder() != 0xFF) {
                slotState.entity(withSourceLoadOrder(slotState.entity(), 0xFF));
                state.runtime().slots[slotState.index()] = slotState.entity();
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
        timerGates.freezeTarinTimers(!state.runtime().indoorRoom
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
                                        && slotState.entity().type() != 0x5B));
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
                && state.runtime()
                        .enemyInspection
                        .isDisabledFollower(slotState.entity().type())) {
            state.runtime().clearEntity(slotState.entity().slot());
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
        presentationFlags.preserveGhiniPresentation(false);
        presentationFlags.preserveMadBomberPresentation(false);
        presentationFlags.preserveBombitePresentation(false);
        presentationFlags.preserveIronMaskPresentation(false);
        presentationFlags.preserveSnakePresentation(false);
        presentationFlags.preserveStarPresentation(false);
        presentationFlags.preserveBlooperPresentation(false);
        presentationFlags.preserveWingedOctorokPresentation(false);
        presentationFlags.preservePincerPresentation(false);
        presentationFlags.preserveBushCrawlerPresentation(false);
        presentationFlags.preserveMimicPresentation(false);
        presentationFlags.preserveMaskedMimicPresentation(false);
        presentationFlags.preserveMiniMoldormPresentation(false);
        presentationFlags.preserveMoldormPresentation(false);
        presentationFlags.preserveCuccoPresentation(false);
        presentationFlags.preserveGopongaFlowerPresentation(false);
        presentationFlags.preserveGiantGopongaPresentation(false);
        presentationFlags.preserveGopongaProjectilePresentation(false);
        presentationFlags.preservePokeyPresentation(false);
        presentationFlags.preservePiranhaPresentation(false);
        presentationFlags.preserveZoraPresentation(false);
        presentationFlags.preserveZombiePresentation(false);
        presentationFlags.preserveBuzzBlobPresentation(false);
        presentationFlags.preserveSandCrabPresentation(false);
        presentationFlags.preserveDogPresentation(false);
        presentationFlags.preserveUrchinPresentation(false);
        presentationFlags.preserveWitchRatPresentation(false);
        presentationFlags.preserveWitchPresentation(false);
        presentationFlags.preserveRoosterPresentation(false);
        presentationFlags.preserveWizrobePresentation(false);
        presentationFlags.preserveWizrobeProjectilePresentation(false);
        presentationFlags.preservePolsVoicePresentation(false);
        presentationFlags.preserveSpikedBeetlePresentation(false);
        presentationFlags.preserveArmosKnightPresentation(false);
        presentationFlags.preserveHinoxPresentation(false);
        presentationFlags.preserveRollingBonesPresentation(false);
        presentationFlags.preserveThreeOfAKindPresentation(false);
        presentationFlags.preserveMoblinKingPresentation(false);
        presentationFlags.preserveSecretSeashellPresentation(false);
        presentationFlags.preserveDroppablePresentation(false);
        presentationFlags.preserveTarinPresentation(false);
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
                && state.rewards().readPowerUpPickupCountdown(slotState.entity().slot()) != 0) {
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
}
