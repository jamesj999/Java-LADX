package linksawakening.world;

import static linksawakening.world.EntityMotionMath.*;
import static linksawakening.world.EntitySnapshots.*;
import static linksawakening.world.EntityTypeRules.*;
import linksawakening.entity.EntitySpriteSelection;
import linksawakening.entity.EntitySpriteDefinition;
import linksawakening.entity.EntitySpriteHandlerCatalog;
import linksawakening.gpu.EntitySpriteTileSnapshot;
import linksawakening.rom.RomTables;
import linksawakening.vfx.TransientVfxType;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Objects;
import java.util.function.IntSupplier;
import java.util.function.IntUnaryOperator;
import static linksawakening.world.RoomEntityRuntime.*;

/** Ordered slot dispatch phase 1; false preserves the ROM handler early return. */
final class EntitySlotPhase1 {
    static boolean run(EntityFrameExecution state) {

            state.entity = state.runtime.slots[state.index];
            state.moldormIntroOpenedThisFrame = false;
            state.rollingBonesHitThisFrame = state.entity.type() == state.runtime.ENTITY_ROLLING_BONES
                && state.runtime.rollingBonesSwordHitFrame[state.index] == state.frame + 1;
            state.runtime.rollingBonesSwordHitFrame[state.index] = 0;
            if (!state.entity.loaded()) {
                return false;
            }
            if (!state.runtime.entityHandlers.tick(94, state)) return false;
            if (state.runtime.enemyProjectileSpawnedThisFrame[state.entity.slot()]
                || state.runtime.dynamicEntitySpawnedThisFrame[state.entity.slot()]) {
                return false;
            }
            if (!state.runtime.entityHandlers.tick(1, state)) return false;
            state.kanaletLeafProducer =
                state.entity.type() == state.runtime.ENTITY_CROW && !state.runtime.indoorRoom && state.runtime.entityRoomId == 0x58
                || state.entity.type() == state.runtime.ENTITY_MAD_BOMBER
                || state.entity.type() == state.runtime.ENTITY_KNIGHT && state.runtime.entityMapId >= 0x14;
            if (state.kanaletLeafProducer) {
                if (!state.runtime.entityHandlers.tick(95, state)) return false;
                if ((state.runtime.entityRoomStatus & 0x10) != 0) {
                    state.runtime.lifecycle.disableEntityWithoutPersistence(state.entity.slot());
                    return false;
                }
                state.runtime.droppedItemBySlot[state.entity.slot()] = state.runtime.ENTITY_HIDING_SLIME_KEY;
                if (state.entity.sourceLoadOrder() != 0xFF) {
                    state.entity = withSourceLoadOrder(state.entity, 0xFF);
                    state.runtime.slots[state.index] = state.entity;
                }
            }
            if (!state.runtime.entityHandlers.tick(2, state)) return false;
            if (!state.runtime.entityHandlers.tick(3, state)) return false;
            state.originalEntity = state.entity;
            state.initialStatus = state.entity.status();
            state.wasInitializing = state.initialStatus == EntityStatus.INIT;
            state.freezeTarinTimers = !state.runtime.indoorRoom && state.entity.type() == state.runtime.ENTITY_TARIN
                && state.runtime.tarinRaccoonMotion.state(state.entity.slot()) != 0
                && !state.runtime.roomInspection.tarinTransformationTimersInteractive();
            state.freezeThreeOfAKindTimers = state.entity.type() == state.runtime.ENTITY_THREE_OF_A_KIND
                && !state.threeOfAKindInteractive;
            state.freezeHinoxTimers = state.entity.type() == state.runtime.ENTITY_HINOX
                && state.initialStatus == EntityStatus.ACTIVE
                && (state.creditsGameplay || !state.handlerLinkCollisionEnabled || !state.runtime.backgroundMechanics.entityTimersInteractive()
                    || state.runtime.transitionSequenceCounter != 0x04);
            state.freezeGenieTimers = state.entity.type() == state.runtime.ENTITY_GENIE
                && (!state.runtime.backgroundMechanics.gameplayEntityTimersInteractive(state.creditsGameplay)
                    || state.runtime.witchGotItemPresentationActive());
            state.freezeWitchTimers = state.entity.type() == state.runtime.ENTITY_WITCH
                && !state.projectileTimersInteractive;
            state.freezePowderTimers = state.entity.type() == state.runtime.ENTITY_MAGIC_POWDER_SPRINKLE
                && !state.projectileTimersInteractive;
            state.freezeBooBuddyTimers = state.entity.type() == state.runtime.ENTITY_BOO_BUDDY
                && !state.projectileTimersInteractive;
            state.freezeBowWowTimers = state.entity.type() == state.runtime.ENTITY_BOW_WOW
                && !state.projectileTimersInteractive;
            state.freezeMoblinKingTimers = state.entity.type() == state.runtime.ENTITY_MOBLIN_KING
                && !state.projectileTimersInteractive;
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
                && state.runtime.lifecycle.decrementEnemyCombatCountdowns(state.entity.slot(), !state.wasInitializing
                    && state.entity.type() != state.runtime.ENTITY_BOO_BUDDY && state.entity.type() != state.runtime.ENTITY_BOW_WOW);
            if (!state.freezeTarinTimers && !state.freezeThreeOfAKindTimers && !state.freezeHinoxTimers
                && !state.freezeGenieTimers && !state.freezePowderTimers && !state.freezeBooBuddyTimers
                && !state.freezeBowWowTimers && !state.freezeWitchTimers && !state.freezeMoblinKingTimers) {
                state.runtime.lifecycle.decrementEnemyStatusCountdowns(state.entity.slot());
                if (!state.runtime.entityHandlers.tick(96, state)) return false;
                if (!state.runtime.entityHandlers.tick(97, state)) return false;
            }
            if (!state.runtime.entityHandlers.tick(4, state)) return false;
            if (!state.runtime.entityHandlers.tick(5, state)) return false;
            if (state.entity.sourceLoadOrder() == -1 && state.runtime.enemyInspection.isDisabledFollower(state.entity.type())) {
                state.runtime.clearEntity(state.entity.slot());
                return false;
            }

            // label_394D -> $14:54AC runs before these bosses' interaction
            // guards, including during their death handlers. It directly
            // nudges Link into the arena, then blocks his next input frame.
            if (!state.runtime.entityHandlers.tick(98, state)) return false;

            // Moldorm calls BossIntro before dispatching active/death states ($04:56C0).
            if (!state.runtime.entityHandlers.tick(6, state)) return false;

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
            state.applyGenericGroundInteraction = state.entity.type() != state.runtime.ENTITY_ZOMBIE
                && state.entity.type() != state.runtime.ENTITY_BUZZ_BLOB
                && state.entity.type() != state.runtime.ENTITY_SAND_CRAB
                && state.entity.type() != state.runtime.ENTITY_DOG
                && state.entity.type() != state.runtime.ENTITY_URCHIN;
            if (state.status == EntityStatus.ACTIVE) {
                state.runtime.pickups.decrementEnemyDropCountdowns(state.entity);
            }
            if (!state.runtime.entityHandlers.tick(7, state)) return false;
            if (!state.runtime.entityHandlers.tick(8, state)) return false;
            if (!state.runtime.entityHandlers.tick(9, state)) return false;
            if (!state.runtime.entityHandlers.tick(10, state)) return false;
            if (!state.runtime.entityHandlers.tick(11, state)) return false;
            if (!state.runtime.entityHandlers.tick(99, state)) return false;
            if (!state.runtime.entityHandlers.tick(12, state)) return false;
            if (!state.runtime.entityHandlers.tick(13, state)) return false;
            if (!state.runtime.entityHandlers.tick(14, state)) return false;
            if (!state.runtime.entityHandlers.tick(15, state)) return false;
            if (!state.runtime.entityHandlers.tick(16, state)) return false;
            if (!state.runtime.entityHandlers.tick(17, state)) return false;
            if (!state.runtime.entityHandlers.tick(18, state)) return false;
            if (!state.runtime.entityHandlers.tick(19, state)) return false;
            if (!state.runtime.entityHandlers.tick(20, state)) return false;
            if (!state.runtime.entityHandlers.tick(21, state)) return false;
            if (!state.runtime.entityHandlers.tick(22, state)) return false;
            if (!state.runtime.entityHandlers.tick(100, state)) return false;
            if (!state.runtime.entityHandlers.tick(23, state)) return false;
            if (!state.runtime.entityHandlers.tick(101, state)) return false;
            if (!state.runtime.entityHandlers.tick(102, state)) return false;
            if (!state.runtime.entityHandlers.tick(103, state)) return false;
            if (!state.runtime.entityHandlers.tick(104, state)) return false;
            if (!state.runtime.entityHandlers.tick(105, state)) return false;
            if (!state.runtime.entityHandlers.tick(24, state)) return false;
            if (state.status == EntityStatus.ACTIVE && state.runtime.powerUpPickupCountdown[state.entity.slot()] != 0) {
                state.runtime.rewards.advancePowerUpPickup(state.entity, state.frame, state.linkEntityX, state.linkEntityY, state.linkZ);
                return false;
            }
            if (!state.runtime.entityHandlers.tick(25, state)) return false;
            if (!state.runtime.entityHandlers.tick(26, state)) return false;
        return true;
    }
}
