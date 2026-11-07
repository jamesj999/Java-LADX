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
            if (state.entity.type() == 0x35 && (state.runtime.entityRoomStatus & 0x10) != 0) {
                state.runtime.disableEntityWithoutPersistence(state.entity.slot());
                return false;
            }
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
                if (state.entity.type() == state.runtime.ENTITY_CROW
                    && state.runtime.enemyIgnoreHitsCountdown[state.entity.slot()] >= 0x10) {
                    state.runtime.enemyIgnoreHitsCountdown[state.entity.slot()] >>>= 2;
                }
                if ((state.runtime.entityRoomStatus & 0x10) != 0) {
                    state.runtime.disableEntityWithoutPersistence(state.entity.slot());
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
                && !state.runtime.tarinTransformationTimersInteractive();
            state.freezeThreeOfAKindTimers = state.entity.type() == state.runtime.ENTITY_THREE_OF_A_KIND
                && !state.threeOfAKindInteractive;
            state.freezeHinoxTimers = state.entity.type() == state.runtime.ENTITY_HINOX
                && state.initialStatus == EntityStatus.ACTIVE
                && (state.creditsGameplay || !state.handlerLinkCollisionEnabled || !state.runtime.entityTimersInteractive()
                    || state.runtime.transitionSequenceCounter != 0x04);
            state.freezeGenieTimers = state.entity.type() == state.runtime.ENTITY_GENIE
                && (!state.runtime.gameplayEntityTimersInteractive(state.creditsGameplay)
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
                && state.runtime.decrementEnemyCombatCountdowns(state.entity.slot(), !state.wasInitializing
                    && state.entity.type() != state.runtime.ENTITY_BOO_BUDDY && state.entity.type() != state.runtime.ENTITY_BOW_WOW);
            if (!state.freezeTarinTimers && !state.freezeThreeOfAKindTimers && !state.freezeHinoxTimers
                && !state.freezeGenieTimers && !state.freezePowderTimers && !state.freezeBooBuddyTimers
                && !state.freezeBowWowTimers && !state.freezeWitchTimers && !state.freezeMoblinKingTimers) {
                state.runtime.decrementEnemyStatusCountdowns(state.entity.slot());
                if (state.entity.type() == state.runtime.ENTITY_GENIE
                    && state.runtime.geniePrivateCountdown1[state.entity.slot()] > 0) {
                    state.runtime.geniePrivateCountdown1[state.entity.slot()]--;
                }
                if (state.entity.type() == state.runtime.ENTITY_GENIE
                    && state.runtime.geniePrivateCountdown2[state.entity.slot()] > 0) {
                    state.runtime.geniePrivateCountdown2[state.entity.slot()]--;
                }
            }
            if (!state.runtime.entityHandlers.tick(4, state)) return false;
            if (!state.runtime.entityHandlers.tick(5, state)) return false;
            if (state.entity.sourceLoadOrder() == -1 && state.runtime.isDisabledFollower(state.entity.type())) {
                state.runtime.clearEntity(state.entity.slot());
                return false;
            }

            // label_394D -> $14:54AC runs before these bosses' interaction
            // guards, including during their death handlers. It directly
            // nudges Link into the arena, then blocks his next input frame.
            if (!state.wasInitializing && !state.runtime.roomTransitionActive && !state.runtime.freeMovementMode
                && (state.entity.type() == state.runtime.ENTITY_ROLLING_BONES || state.entity.type() == state.runtime.ENTITY_MOLDORM)) {
                int dx = 0;
                int dy = 0;
                if (((state.linkEntityX - 0x11) & 0xFF) >= 0x7E) {
                    dx = state.linkEntityX < 0x50 ? 1 : -1;
                } else if (((state.linkEntityY - 0x16) & 0xFF) >= 0x5E) {
                    dy = -1;
                }
                if (dx != 0 || dy != 0) {
                    state.runtime.pendingLinkMotionBlockRequests.add(
                        new LinkMotionBlockRequest(state.entity.slot(), dx, dy));
                    state.linkEntityX = (state.linkEntityX + dx) & 0xFF;
                    state.linkEntityY = (state.linkEntityY + dy) & 0xFF;
                    state.runtime.entityInteractionCountdown = 2;
                }
            }

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
                state.runtime.decrementEnemyDropCountdowns(state.entity);
            }
            if (!state.runtime.entityHandlers.tick(7, state)) return false;
            if (!state.runtime.entityHandlers.tick(8, state)) return false;
            if (!state.runtime.entityHandlers.tick(9, state)) return false;
            if (!state.runtime.entityHandlers.tick(10, state)) return false;
            if (!state.runtime.entityHandlers.tick(11, state)) return false;
            if (state.status == EntityStatus.ACTIVE && state.runtime.indoorRoom
                && state.entity.type() == WarpMotion.ENTITY_TYPE && state.runtime.warpMotion != null) {
                int slot = state.entity.slot();
                state.runtime.slots[state.index] = withPositionAndVariant(state.entity, state.entity.x(), state.entity.y(),
                    state.runtime.dungeonMinibossReady ? (state.frame >> 3) & 7 : -1);
                WarpMotion.Update warpUpdate = state.runtime.warpMotion.advance(
                    state.entity, state.runtime.warpState[slot], state.runtime.warpTransitionCountdown[slot], state.runtime.entityMapId,
                    state.runtime.entityRoomId, state.runtime.dungeonMinibossReady, state.linkEntityX, state.linkEntityY, state.linkZ);
                state.runtime.warpState[slot] = warpUpdate.state();
                state.runtime.warpTransitionCountdown[slot] = warpUpdate.countdown();
                if (warpUpdate.musicTrack() >= 0) {
                    state.runtime.pendingMusicTrack = warpUpdate.musicTrack();
                }
                if (warpUpdate.jingleId() >= 0) {
                    state.runtime.pendingEntityEvents.add(new EntityCombatEvent(
                        slot, state.entity.type(), 0, false,
                        EntityCombatEvent.SoundChannel.JINGLE, warpUpdate.jingleId()));
                }
                if (warpUpdate.motionBlocked()) {
                    state.runtime.pendingLinkMotionBlockRequests.add(new LinkMotionBlockRequest(slot));
                }
                if (warpUpdate.state() == WarpMotion.STATE_TRANSITION) {
                    state.runtime.pendingWarpLinkStateRequests.add(new WarpLinkStateRequest(
                        slot, state.entity.x(), state.entity.y(), warpUpdate.immunityCountdown()));
                }
                if (warpUpdate.destinationReady()) {
                    state.runtime.pendingDungeonWarpRequests.add(new DungeonWarpRequest(
                        slot, warpUpdate.destination()));
                    state.runtime.disableEntityWithoutPersistence(slot);
                }
                return false;
            }
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
            if (state.status == EntityStatus.ACTIVE && state.entity.type() == 0x42
                && state.runtime.indoorRoom && state.runtime.entityMapId != 0x16) {
                state.runtime.advanceOwlStatue(state.entity, state.linkEntityX, state.linkEntityY, state.linkZ,
                    state.projectileLinkState.motionState(), state.linkAirborne);
                return false;
            }
            if (!state.runtime.entityHandlers.tick(23, state)) return false;
            if (state.status == EntityStatus.ACTIVE
                && (state.entity.type() == state.runtime.ENTITY_KID_71 || state.entity.type() == state.runtime.ENTITY_KID_72)
                && state.runtime.bowWowState == 0x80) {
                state.runtime.advanceKidnapWarningKid(state.entity, state.frame, state.linkEntityX, state.linkEntityY);
                return false;
            }
            if (state.status == EntityStatus.ACTIVE && state.entity.type() == 0x35
                && state.runtime.heartPiecePickup[state.entity.slot()] != null) {
                state.runtime.advanceHeartPiecePickup(state.entity, state.linkEntityX, state.linkEntityY, state.linkZ);
                return false;
            }
            if (state.status == EntityStatus.ACTIVE && state.entity.type() == 0x39
                && state.runtime.instrumentParticleKind[state.entity.slot()] == 0
                && state.runtime.instrumentPickupState[state.entity.slot()] < 3
                && state.runtime.transitionSequenceCounter == 4 && state.runtime.spriteSelection != null) {
                int[][] palettes = state.runtime.spriteSelection.objectPalettes();
                int offset = ((state.frame >> 1) & 0x3C) / 2;
                palettes[1][2] = state.runtime.instrumentPaletteCycle[offset];
                palettes[1][3] = state.runtime.instrumentPaletteCycle[offset + 1];
                state.runtime.spriteSelection = state.runtime.spriteSelection.withObjectPalettes(palettes);
            }
            if (state.status == EntityStatus.ACTIVE
                && state.entity.type() == EntitySpriteHandlerCatalog.ENTITY_INSTRUMENT_OF_THE_SIRENS
                && state.runtime.instrumentParticleKind[state.entity.slot()] == 1) {
                state.runtime.advanceInstrumentParticle(state.entity);
                return false;
            }
            if (state.status == EntityStatus.ACTIVE
                && state.entity.type() == EntitySpriteHandlerCatalog.ENTITY_INSTRUMENT_OF_THE_SIRENS
                && state.runtime.instrumentPickupSequenceActive[state.entity.slot()]) {
                state.runtime.advanceInstrumentPickup(state.entity, state.frame, state.linkEntityX, state.linkEntityY, state.linkZ,
                    state.randomByteSupplier);
                return false;
            }
            if (!state.runtime.entityHandlers.tick(24, state)) return false;
            if (state.status == EntityStatus.ACTIVE && state.runtime.powerUpPickupCountdown[state.entity.slot()] != 0) {
                state.runtime.advancePowerUpPickup(state.entity, state.frame, state.linkEntityX, state.linkEntityY, state.linkZ);
                return false;
            }
            if (!state.runtime.entityHandlers.tick(25, state)) return false;
            if (!state.runtime.entityHandlers.tick(26, state)) return false;
        return true;
    }
}
