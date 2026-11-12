package linksawakening.world.entity;

import static linksawakening.world.entity.EntityMotionMath.*;
import static linksawakening.world.entity.EntityRomConstants.*;
import static linksawakening.world.entity.EntitySnapshots.*;
import static linksawakening.world.entity.EntityTypeRules.*;

import linksawakening.world.entity.motion.*;

/** Ordered slot dispatch phase 1; false preserves the ROM handler early return. */
final class EntitySlotPhase1 {
    static boolean run(EntityFrameExecution state) {

        state.entity = state.runtime.slots[state.index];
        if (state.runtime.liftThrow.advanceStatueRubble(state.entity, state.sideViewPotInteractive)) return false;
        state.moldormIntroOpenedThisFrame = false;
        state.rollingBonesHitThisFrame = state.entity.type() == EntityRomConstants.ENTITY_ROLLING_BONES
                && state.runtime
                                .entityState
                                .motions
                                .get(RollingBonesMotion.class, RollingBonesMotion::new)
                                .swordHitFrame(state.index)
                        == state.frame + 1;
        state.runtime
                .entityState
                .motions
                .get(RollingBonesMotion.class, RollingBonesMotion::new)
                .setSwordHitFrame(state.index, 0);
        if (!state.entity.loaded()) {
            return false;
        }
        if (!state.runtime.entityHandlers.tick(EntityTickStage.PHASE_1_TYPE_0X35, state)) return false;
        if (state.runtime.entityState.physicsTables.readEnemyProjectileSpawnedThisFrame(state.entity.slot())
                || state.runtime.entityState.physicsTables.readDynamicEntitySpawnedThisFrame(state.entity.slot())) {
            return false;
        }
        if (!state.runtime.entityHandlers.tick(EntityTickStage.PHASE_1_KANALET_BOMBABLE_WALL, state)) return false;
        state.kanaletLeafProducer = state.entity.type() == EntityRomConstants.ENTITY_CROW
                        && !state.runtime.indoorRoom
                        && state.runtime.entityState.roomVariables.readEntityRoomId() == 0x58
                || state.entity.type() == EntityRomConstants.ENTITY_MAD_BOMBER
                || state.entity.type() == EntityRomConstants.ENTITY_KNIGHT
                        && state.runtime.entityState.roomVariables.readEntityMapId() >= 0x14;
        if (state.kanaletLeafProducer) {
            if (!state.runtime.entityHandlers.tick(EntityTickStage.PHASE_1_CROW, state)) return false;
            if ((state.runtime.entityState.roomVariables.readEntityRoomStatus() & 0x10) != 0) {
                state.runtime.lifecycle.disableEntityWithoutPersistence(state.entity.slot());
                return false;
            }
            state.runtime.bossRewards.writeDroppedItemBySlot(
                    state.entity.slot(), EntityRomConstants.ENTITY_HIDING_SLIME_KEY);
            if (state.entity.sourceLoadOrder() != 0xFF) {
                state.entity = withSourceLoadOrder(state.entity, 0xFF);
                state.runtime.slots[state.index] = state.entity;
            }
        }
        if (!state.runtime.entityHandlers.tick(EntityTickStage.PHASE_1_TARIN, state)) return false;
        if (!state.runtime.entityHandlers.tick(EntityTickStage.PHASE_1_MARIN_INDOOR, state)) return false;
        state.originalEntity = state.entity;
        state.initialStatus = state.entity.status();
        state.wasInitializing = state.initialStatus == EntityStatus.INIT;
        state.freezeTarinTimers = !state.runtime.indoorRoom
                && state.entity.type() == EntityRomConstants.ENTITY_TARIN
                && state.runtime
                                .entityState
                                .motions
                                .get(TarinRaccoonMotion.class, TarinRaccoonMotion::new)
                                .state(state.entity.slot())
                        != 0
                && !state.runtime.roomInspection.tarinTransformationTimersInteractive();
        state.freezeThreeOfAKindTimers =
                state.entity.type() == EntityRomConstants.ENTITY_THREE_OF_A_KIND && !state.threeOfAKindInteractive;
        state.freezeHinoxTimers = state.entity.type() == EntityRomConstants.ENTITY_HINOX
                && state.initialStatus == EntityStatus.ACTIVE
                && (state.creditsGameplay
                        || !state.handlerLinkCollisionEnabled
                        || !state.runtime.backgroundMechanics.entityTimersInteractive()
                        || state.runtime.entityState.roomVariables.readTransitionSequenceCounter() != 0x04);
        state.freezeGenieTimers = state.entity.type() == EntityRomConstants.ENTITY_GENIE
                && (!state.runtime.backgroundMechanics.gameplayEntityTimersInteractive(state.creditsGameplay)
                        || state.runtime.inspection().witchGotItemPresentationActive());
        state.freezeWitchTimers =
                state.entity.type() == EntityRomConstants.ENTITY_WITCH && !state.projectileTimersInteractive;
        state.freezePowderTimers = state.entity.type() == EntityRomConstants.ENTITY_MAGIC_POWDER_SPRINKLE
                && !state.projectileTimersInteractive;
        state.freezeBooBuddyTimers =
                state.entity.type() == EntityRomConstants.ENTITY_BOO_BUDDY && !state.projectileTimersInteractive;
        state.freezeBowWowTimers =
                state.entity.type() == EntityRomConstants.ENTITY_BOW_WOW && !state.projectileTimersInteractive;
        state.freezeMoblinKingTimers =
                state.entity.type() == EntityRomConstants.ENTITY_MOBLIN_KING && !state.projectileTimersInteractive;
        state.ignoreHitsDecrementedBeforeHandler = !state.freezeTarinTimers
                && !state.freezeThreeOfAKindTimers
                && !state.freezeHinoxTimers
                && !state.freezeGenieTimers
                && !state.freezeWitchTimers
                && !state.freezePowderTimers
                && !state.freezeBooBuddyTimers
                && !state.freezeBowWowTimers
                && !state.freezeMoblinKingTimers
                && !state.rollingBonesHitThisFrame
                && state.runtime.lifecycle.decrementEnemyCombatCountdowns(
                        state.entity.slot(),
                        !state.wasInitializing
                                && state.entity.type() != EntityRomConstants.ENTITY_BOO_BUDDY
                                && state.entity.type() != EntityRomConstants.ENTITY_BOW_WOW
                                && state.entity.type() != 0x5B);
        if (!state.freezeTarinTimers
                && !state.freezeThreeOfAKindTimers
                && !state.freezeHinoxTimers
                && !state.freezeGenieTimers
                && !state.freezePowderTimers
                && !state.freezeBooBuddyTimers
                && !state.freezeBowWowTimers
                && !state.freezeWitchTimers
                && !state.freezeMoblinKingTimers) {
            state.runtime.lifecycle.decrementEnemyStatusCountdowns(state.entity.slot());
            if (!state.runtime.entityHandlers.tick(EntityTickStage.PHASE_1_GENIE, state)) return false;
            if (!state.runtime.entityHandlers.tick(EntityTickStage.PHASE_1_GENIE_2, state)) return false;
        }
        if (!state.runtime.entityHandlers.tick(EntityTickStage.PHASE_1_BOO_BUDDY, state)) return false;
        if (!state.runtime.entityHandlers.tick(EntityTickStage.PHASE_1_BOW_WOW, state)) return false;
        if (state.entity.sourceLoadOrder() == -1
                && state.runtime.enemyInspection.isDisabledFollower(state.entity.type())) {
            state.runtime.clearEntity(state.entity.slot());
            return false;
        }

        // label_394D -> $14:54AC runs before these bosses' interaction
        // guards, including during their death handlers. It directly
        // nudges Link into the arena, then blocks his next input frame.
        if (!state.runtime.entityHandlers.tick(EntityTickStage.PHASE_1_MOLDORM_2, state)) return false;

        // Moldorm calls BossIntro before dispatching active/death states ($04:56C0).
        if (!state.runtime.entityHandlers.tick(EntityTickStage.PHASE_1_MOLDORM, state)) return false;

        state.status = state.entity.status();
        state.preserveGhiniPresentation = false;
        state.preserveMadBomberPresentation = false;
        state.preserveBombitePresentation = false;
        state.preserveIronMaskPresentation = false;
        state.preserveSnakePresentation = false;
        state.preserveStarPresentation = false;
        state.preserveBlooperPresentation = false;
        state.preserveWingedOctorokPresentation = false;
        state.preservePincerPresentation = false;
        state.preserveBushCrawlerPresentation = false;
        state.preserveMimicPresentation = false;
        state.preserveMaskedMimicPresentation = false;
        state.preserveMiniMoldormPresentation = false;
        state.preserveMoldormPresentation = false;
        state.preserveCuccoPresentation = false;
        state.preserveGopongaFlowerPresentation = false;
        state.preserveGiantGopongaPresentation = false;
        state.preserveGopongaProjectilePresentation = false;
        state.preservePokeyPresentation = false;
        state.preservePiranhaPresentation = false;
        state.preserveZoraPresentation = false;
        state.preserveZombiePresentation = false;
        state.preserveBuzzBlobPresentation = false;
        state.preserveSandCrabPresentation = false;
        state.preserveDogPresentation = false;
        state.preserveUrchinPresentation = false;
        state.preserveWitchRatPresentation = false;
        state.preserveWitchPresentation = false;
        state.preserveRoosterPresentation = false;
        state.preserveWizrobePresentation = false;
        state.preserveWizrobeProjectilePresentation = false;
        state.preservePolsVoicePresentation = false;
        state.preserveSpikedBeetlePresentation = false;
        state.preserveArmosKnightPresentation = false;
        state.preserveHinoxPresentation = false;
        state.preserveRollingBonesPresentation = false;
        state.preserveThreeOfAKindPresentation = false;
        state.preserveMoblinKingPresentation = false;
        state.preserveSecretSeashellPresentation = false;
        state.preserveDroppablePresentation = false;
        state.preserveTarinPresentation = false;
        state.keyDropTransitionActive = false;
        state.preAppliedGroundResult = null;
        state.applyGenericGroundInteraction = state.entity.type() != EntityRomConstants.ENTITY_ZOMBIE
                && state.entity.type() != EntityRomConstants.ENTITY_BUZZ_BLOB
                && state.entity.type() != EntityRomConstants.ENTITY_SAND_CRAB
                && state.entity.type() != EntityRomConstants.ENTITY_DOG
                && state.entity.type() != EntityRomConstants.ENTITY_URCHIN;
        if (state.status == EntityStatus.ACTIVE) {
            state.runtime.pickups.decrementEnemyDropCountdowns(state.entity);
        }
        if (!state.runtime.entityHandlers.tick(EntityTickStage.PHASE_1_SIDE_VIEW_POT, state)) return false;
        if (!state.runtime.entityHandlers.tick(EntityTickStage.PHASE_1_SIDE_VIEW_POT_2, state)) return false;
        if (!state.runtime.entityHandlers.tick(EntityTickStage.PHASE_1_DOG, state)) return false;
        if (!state.runtime.entityHandlers.tick(EntityTickStage.PHASE_1_HOOKSHOT_BRIDGE, state)) return false;
        if (!state.runtime.entityHandlers.tick(EntityTickStage.PHASE_1_HOOKSHOT_CHAIN, state)) return false;
        if (!state.runtime.entityHandlers.tick(EntityTickStage.PHASE_1_TYPE, state)) return false;
        if (!state.runtime.entityHandlers.tick(EntityTickStage.PHASE_1_BOOMERANG, state)) return false;
        if (!state.runtime.entityHandlers.tick(EntityTickStage.PHASE_1_SWORD_BEAM, state)) return false;
        if (!state.runtime.entityHandlers.tick(EntityTickStage.PHASE_1_MAGIC_ROD_FIREBALL, state)) return false;
        if (!state.runtime.entityHandlers.tick(EntityTickStage.PHASE_1_MAGIC_POWDER_SPRINKLE, state)) return false;
        if (!state.runtime.entityHandlers.tick(EntityTickStage.PHASE_1_MUSICAL_NOTE, state)) return false;
        if (!state.runtime.entityHandlers.tick(EntityTickStage.PHASE_1_BOMB, state)) return false;
        if (!state.runtime.entityHandlers.tick(EntityTickStage.PHASE_1_ARROW, state)) return false;
        if (!state.runtime.entityHandlers.tick(EntityTickStage.PHASE_1_LIFTABLE_ROCK, state)) return false;
        if (!state.runtime.entityHandlers.tick(EntityTickStage.PHASE_1_HEART_CONTAINER, state)) return false;
        if (!state.runtime.entityHandlers.tick(EntityTickStage.PHASE_1_MADAM_MEOWMEOW, state)) return false;
        if (!state.runtime.entityHandlers.tick(EntityTickStage.PHASE_1_RICHARD, state)) return false;
        if (!state.runtime.entityHandlers.tick(EntityTickStage.PHASE_1_TYPE_0X42, state)) return false;
        if (!state.runtime.entityHandlers.tick(EntityTickStage.PHASE_1_BOW_WOW_2, state)) return false;
        if (!state.runtime.entityHandlers.tick(EntityTickStage.PHASE_1_KID_72, state)) return false;
        if (!state.runtime.entityHandlers.tick(EntityTickStage.PHASE_1_TYPE_0X35_2, state)) return false;
        if (!state.runtime.entityHandlers.tick(EntityTickStage.PHASE_1_TYPE_0X39, state)) return false;
        if (!state.runtime.entityHandlers.tick(EntityTickStage.PHASE_1_INSTRUMENT_OF_THE_SIRENS, state)) return false;
        if (!state.runtime.entityHandlers.tick(EntityTickStage.PHASE_1_INSTRUMENT_OF_THE_SIRENS_2, state)) return false;
        if (!state.runtime.entityHandlers.tick(EntityTickStage.PHASE_1_SWORD_SHIELD_PICKUP, state)) return false;
        if (state.status == EntityStatus.ACTIVE
                && state.runtime.rewards.readPowerUpPickupCountdown(state.entity.slot()) != 0) {
            state.runtime.rewards.advancePowerUpPickup(
                    state.entity, state.frame, state.linkEntityX, state.linkEntityY, state.linkZ);
            return false;
        }
        if (!state.runtime.entityHandlers.tick(EntityTickStage.PHASE_1_SLEEPY_TOADSTOOL, state)) return false;
        if (!state.runtime.entityHandlers.tick(EntityTickStage.PHASE_1_OWL_EVENT, state)) return false;
        return true;
    }
}
