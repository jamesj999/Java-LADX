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
            if (state.entity.type() == state.runtime.ENTITY_KANALET_BOMBABLE_WALL) {
                if ((state.runtime.entityRoomStatus & 0x10) != 0) {
                    state.runtime.disableEntityWithoutPersistence(state.entity.slot());
                    return false;
                }
                state.runtime.droppedItemBySlot[state.entity.slot()] = state.runtime.ENTITY_HIDING_SLIME_KEY;
                if (state.runtime.enemyIgnoreHitsCountdown[state.entity.slot()] != 0) {
                    if (state.runtime.spawnKanaletWallMoblin(state.entity)) {
                        state.runtime.disableEntityWithoutPersistence(state.entity.slot());
                        return false;
                    }
                }
            }
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
            if (!state.runtime.indoorRoom && state.entity.type() == state.runtime.ENTITY_TARIN
                && (state.runtime.entityRoomStatus & 0x10) != 0) {
                state.runtime.disableEntityWithoutPersistence(state.entity.slot());
                return false;
            }
            if (state.runtime.indoorRoom && state.entity.type() == state.runtime.ENTITY_MARIN_INDOOR && state.runtime.chestPlayerLevelsKnown
                && state.runtime.chestSwordLevel != 0) {
                state.runtime.disableEntityWithoutPersistence(state.entity.slot());
                return false;
            }
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
            if (state.entity.type() == state.runtime.ENTITY_BOO_BUDDY && state.initialStatus == EntityStatus.ACTIVE) {
                int slot = state.entity.slot();
                // Rendering precedes ReturnIfNonInteractive_06 and recoil.
                state.entity = withVariant(state.entity, state.runtime.booBuddyMotion.selectedVariant(slot));
                state.runtime.slots[state.index] = state.entity;
                if (state.projectileTimersInteractive) {
                    state.runtime.booBuddyMotion.tickTimers(slot);
                }
                if (!state.handlerInteractive) {
                    return false;
                }
                if (state.runtime.enemyIgnoreHitsCountdown[slot] != 0) {
                    // $06:64F7 consumes one ignore-hits tick, moves with recoil
                    // speeds, and pops the handler return address. OPTIONS1
                    // bit $20 skips background collision for Boo Buddy.
                    state.runtime.enemyIgnoreHitsCountdown[slot]--;
                    state.runtime.slots[state.index] = state.runtime.booBuddyMotion.advanceRecoil(state.entity,
                        state.runtime.enemyRecoilMotion.recoilSpeedX(slot),
                        state.runtime.enemyRecoilMotion.recoilSpeedY(slot));
                    return false;
                }
            }
            if (state.entity.type() == state.runtime.ENTITY_BOW_WOW && state.initialStatus == EntityStatus.ACTIVE) {
                int slot = state.entity.slot();
                if (state.projectileTimersInteractive) {
                    state.runtime.bowWowMotion.tickTimers(slot);
                }
                // $05:4054 selects the following tiles for every nonzero
                // quest state, including kidnapped ($80), before any gates.
                int spriteAddress = state.runtime.bowWowState != 0 ? 0x401C : 0x4000;
                if (state.runtime.spriteHandlers != null
                    && (state.entity.spriteDefinition().bank() != 0x05
                        || state.entity.spriteDefinition().address() != spriteAddress)) {
                    state.entity = withDefinition(state.entity, state.runtime.bowWowState != 0
                        ? state.runtime.spriteHandlers.forFollowerEntityType(state.runtime.ENTITY_BOW_WOW)
                        : state.runtime.spriteHandlers.forEntityType(state.runtime.ENTITY_BOW_WOW,
                            EntityRoomLoader.RoomTable.OVERWORLD),
                        state.entity.spriteVariant());
                }
                state.entity = withVariant(state.entity, state.runtime.bowWowMotion.selectedVariant(slot));
                state.runtime.slots[state.index] = state.entity;
                if (!state.bowWowHandlerInteractive) {
                    return false;
                }
                if (state.runtime.enemyIgnoreHitsCountdown[slot] != 0) {
                    state.runtime.enemyIgnoreHitsCountdown[slot]--;
                }
            }
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
            if (!state.wasInitializing && state.entity.type() == state.runtime.ENTITY_MOLDORM
                && !state.runtime.roomTransitionActive && !state.runtime.inventoryAppearing) {
                BossIntroMotion.Update intro = state.runtime.bossIntroMotion.advance(
                    state.runtime.options1(state.entity.slot()), state.entity.type(), state.runtime.entityMapId, state.runtime.transitionSequenceCounter);
                if (intro.musicTrack() >= 0) state.runtime.pendingMusicTrack = intro.musicTrack();
                if (intro.dialogLowId() >= 0) {
                    state.runtime.pendingDialogRequests.add(new DialogRequest(intro.dialogTableId(), intro.dialogLowId()));
                    state.moldormIntroOpenedThisFrame = true;
                }
            }

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
            if (state.status == EntityStatus.ACTIVE
            && state.entity.type() == state.runtime.ENTITY_SIDE_VIEW_POT
            && !state.runtime.sideViewPotMotion.active(state.entity.slot())) {
                SideViewPotContact.Result contact = SideViewPotContact.Result.none();
                int effectiveLinkX = state.linkEntityX;
                int effectiveLinkY = state.linkEntityY;
                if (state.sideViewPotLinkCollisionInteractive) {
                    contact = SideViewPotContact.resolve(
                        state.entity, state.linkEntityX, state.linkEntityY, state.linkAirborne, state.runtime.currentLinkSpeedY);
                    if (contact.collided()) {
                        state.runtime.pendingLinkPotContact(state.entity.slot(), contact);
                        if (contact.restoreFinalPositionX()) {
                            effectiveLinkX = state.runtime.currentLinkFinalPositionXConfigured
                                ? state.runtime.currentLinkFinalPositionX : state.linkEntityX;
                        }
                        if (contact.snapTop()) {
                            effectiveLinkY = contact.positionY();
                        }
                    }
                }
                if (state.runtime.groundInteractionSideScrolling
                    && state.sideViewPotInteractive
                    && SideViewPotContact.liftWindow(state.entity, effectiveLinkX, effectiveLinkY)
                    && state.runtime.sideViewPotPowerBraceletHeld()
                    && state.runtime.linkAttackStepAnimationCountdown == 0
                    && state.runtime.beginLift(state.entity.slot(), state.romLinkDirection)) {
                    state.runtime.pendingEntityEvents.add(new EntityCombatEvent(
                        state.entity.slot(), state.entity.type(), 0, false,
                        EntityCombatEvent.SoundChannel.WAVE, 0x02));
                }
                return false;
            }
            if (state.status == EntityStatus.ACTIVE
                && state.entity.type() == state.runtime.ENTITY_SIDE_VIEW_POT
                && state.runtime.sideViewPotMotion.active(state.entity.slot())) {
                if (state.sideViewPotInteractive) {
                    SideViewPotMotion.Update potUpdate = state.runtime.sideViewPotMotion.advance(state.entity);
                    RoomEntity movedPot = potUpdate.entity();
                    boolean collision = state.runtime.sideViewPotBackgroundCollision(state.entity, movedPot,
                        state.backgroundCollision, state.frame);
                    if (collision) {
                        state.runtime.spawnLiftableRockSmash(movedPot.x(),
                            (movedPot.y() - movedPot.z()) & 0xFF,
                            state.runtime.LIFTABLE_ROCK_SMASH_MODE_ROCK);
                        state.runtime.clearEntity(state.entity.slot());
                    } else {
                        state.runtime.slots[state.index] = movedPot;
                    }
                }
                // ReturnIfNonInteractive_19 leaves the rendered pot and all
                // dedicated motion state untouched while Link is gated.
                return false;
            }
            if (state.status == EntityStatus.ACTIVE && state.entity.type() == state.runtime.ENTITY_DOG) {
                // DogEntityHandler writes this before ReturnIfNonInteractive_19.
                state.runtime.enemyHealth[state.entity.slot()] = DogMotion.HANDLER_HEALTH;
            }
            if (state.status == EntityStatus.ACTIVE && state.entity.type() == state.runtime.ENTITY_HOOKSHOT_BRIDGE) {
                state.runtime.advanceHookshotBridgeEntity(state.index, state.entity);
                return false;
            }
            if (state.status == EntityStatus.ACTIVE && state.entity.type() == state.runtime.ENTITY_HOOKSHOT_CHAIN) {
                HookshotChainMotion.State hookshotState = state.runtime.hookshotChainMotion.state(state.entity.slot());
                if (hookshotState == null) {
                    state.runtime.clearEntity(state.entity.slot());
                    return false;
                }
                // $7BE3 cancels the chain before rendering or the pause gate.
                if (state.runtime.linkPlayingOcarinaCountdown != 0) {
                    state.runtime.clearEntity(state.entity.slot());
                    return false;
                }
                if (state.projectileTimersInteractive) {
                    hookshotState = HookshotChainMotion.decrementTransitionCountdown(hookshotState);
                    state.runtime.hookshotChainMotion.setState(state.entity.slot(), hookshotState);
                }
                // Bank $18 has no credits exception in ReturnIfNonInteractive.
                if (!state.handlerInteractive || state.creditsGameplay) {
                    return false;
                }

                if ((state.frame & 0x03) == 0) {
                    state.runtime.pendingEntityEvents.add(new EntityCombatEvent(
                        state.entity.slot(), state.entity.type(), 0, false,
                        EntityCombatEvent.SoundChannel.NOISE, 0x0B));
                }

                if (hookshotState.entityState() == HookshotChainMotion.PULLING_STATE) {
                    if (state.handlerLinkCollisionEnabled && HookshotChainMotion.overlapsLink(hookshotState,
                        state.linkEntityX, state.linkEntityY)) {
                        state.runtime.clearEntity(state.entity.slot());
                        return false;
                    }
                    HookshotChainMotion.PullSpeed pullSpeed =
                        HookshotChainMotion.pullLinkSpeed(
                            hookshotState, state.linkEntityX, state.linkEntityY);
                    state.projectileEvents.add(EntityProjectileEvent.hookshotPull(
                        state.entity.slot(), pullSpeed.speedX(), pullSpeed.speedY()));
                    state.runtime.slots[state.index] = state.entity;
                    return false;
                }

                HookshotChainMotion.Position nextPosition =
                    HookshotChainMotion.nextPosition(hookshotState);
                HookshotChainMotion.Step step = HookshotChainMotion.advance(
                    hookshotState, state.linkEntityX, state.linkEntityY, false, false);
                if (step.returning()) {
                    state.runtime.hookshotChainMotion.setState(state.entity.slot(), step.state());
                    if (state.handlerLinkCollisionEnabled && step.reachedLink()) {
                        state.runtime.clearEntity(state.entity.slot());
                    } else {
                        state.runtime.slots[state.index] = withPositionAndVariant(state.entity,
                            step.state().x(), step.state().y(), state.entity.spriteVariant());
                    }
                    return false;
                }
                // $7C59 checks enemies before background interaction. The
                // collision pass may clear the timer, but this pass still
                // follows the already-selected outgoing handler branch.
                boolean unmasked = state.runtime.collideHookshotWithEntities(state.entity, step.state(), state.frame);
                if (unmasked) {
                    step = new HookshotChainMotion.Step(
                        HookshotChainMotion.completeWallPoke(step.state()),
                        step.collided(), step.reachedLink(), step.returning());
                }
                if (hookshotState.wallCollisionPending()) {
                    state.runtime.hookshotChainMotion.setState(state.entity.slot(),
                        HookshotChainMotion.completeWallPoke(step.state()));
                    state.runtime.pendingEntityEvents.add(new EntityCombatEvent(
                        state.entity.slot(), state.entity.type(), 0, false,
                        EntityCombatEvent.SoundChannel.JINGLE, 0x07));
                    state.runtime.transientVfxRequests.add(new TransientVfxRequest(
                        TransientVfxType.SWORD_POKE, state.entity.x(),
                        (state.entity.y() - state.entity.z()) & 0xFF));
                    state.runtime.slots[state.index] = withPositionAndVariant(state.entity,
                        step.state().x(), step.state().y(), state.entity.spriteVariant());
                    return false;
                }
                EntityBackgroundCollisionResult collisionResult = null;
                boolean blocked;
                if (state.runtime.backgroundInteraction != null) {
                    collisionResult = state.runtime.backgroundInteraction.probe(
                        state.entity, hookshotState.direction(), nextPosition.x(), nextPosition.y(),
                        state.runtime.enemyIgnoreHitsCountdown[state.entity.slot()], state.frame);
                    blocked = collisionResult.blocked();
                } else {
                    blocked = state.backgroundCollision != null
                        && state.backgroundCollision.blocks(state.entity, hookshotState.direction(),
                            nextPosition.x(), nextPosition.y());
                }
                if (blocked) {
                    HookshotChainMotion.State stopped = HookshotChainMotion.rollbackTo(
                        step.state(), hookshotState.x(), hookshotState.y());
                    if (collisionResult != null
                        && collisionResult.physicsFlag() == 0x60) {
                        if (HookshotChainMotion.shouldUnloadForHookshotable(
                            step.state().transitionCountdown())) {
                            state.runtime.clearEntity(state.entity.slot());
                            return false;
                        }
                        state.runtime.hookshotChainMotion.setState(state.entity.slot(),
                            HookshotChainMotion.enterPulling(stopped));
                    } else {
                        state.runtime.hookshotChainMotion.setState(state.entity.slot(),
                            HookshotChainMotion.deferWallPoke(stopped));
                    }
                    state.runtime.slots[state.index] = state.entity;
                    return false;
                }

                if (state.runtime.indoorRoom
                    && hookshotState.speedY() != 0 && state.runtime.objectQuery != null) {
                    RoomEntity movedChain = withPositionAndVariant(state.entity,
                        step.state().x(), step.state().y(), state.entity.spriteVariant());
                    RoomEntityObjectSample object = state.runtime.objectQuery.sample(movedChain);
                    if (object != null) {
                        int bridgeObject = (hookshotState.speedY() & 0x80) != 0
                            ? state.runtime.OBJECT_HOOKSHOT_BRIDGE_PULL_DOWN
                            : state.runtime.OBJECT_HOOKSHOT_BRIDGE_PULL_UP;
                        if (object.objectId() == bridgeObject
                            && state.runtime.spawnHookshotBridge(object.objectLeft() + 0x08,
                                object.objectTop() + 0x10,
                                bridgeObject == state.runtime.OBJECT_HOOKSHOT_BRIDGE_PULL_DOWN ? 0 : 1,
                                step.state().transitionCountdown())) {
                            state.runtime.hookshotBridgeUpdates.add(new HookshotBridgeUpdate(
                                object.objectLeft(), object.objectTop(),
                                bridgeObject == state.runtime.OBJECT_HOOKSHOT_BRIDGE_PULL_DOWN ? 0 : 1));
                        }
                    }
                }

                state.runtime.hookshotChainMotion.setState(state.entity.slot(), step.state());
                state.runtime.slots[state.index] = withPositionAndVariant(state.entity,
                    step.state().x(), step.state().y(), state.entity.spriteVariant());
                return false;
            }
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
            if (state.status == EntityStatus.ACTIVE && state.entity.type() == state.runtime.ENTITY_BOOMERANG) {
                state.runtime.advanceBoomerangEntity(state.index, state.entity, state.frame, state.linkEntityX, state.linkEntityY,
                    state.linkZ, state.projectileLinkState.motionState(), state.handlerInteractive,
                    state.projectileTimersInteractive);
                return false;
            }
            if (state.status == EntityStatus.ACTIVE && state.entity.type() == state.runtime.ENTITY_SWORD_BEAM) {
                state.runtime.advanceSwordBeamEntity(state.index, state.entity, state.frame, state.linkEntityX, state.linkEntityY,
                    state.romLinkDirection, state.handlerInteractive);
                return false;
            }
            if (state.status == EntityStatus.ACTIVE && state.entity.type() == state.runtime.ENTITY_MAGIC_ROD_FIREBALL) {
                state.runtime.advanceMagicRodFireballEntity(state.index, state.entity, state.frame,
                    state.handlerInteractive, state.projectileTimersInteractive);
                return false;
            }
            if (state.status == EntityStatus.ACTIVE
                && state.entity.type() == state.runtime.ENTITY_MAGIC_POWDER_SPRINKLE) {
                state.runtime.advanceMagicPowderSprinkleEntity(state.index, state.entity, state.frame,
                    state.projectileLinkState.motionState(),
                    state.handlerInteractive && !state.creditsGameplay, state.projectileTimersInteractive);
                return false;
            }
            if (state.status == EntityStatus.ACTIVE && state.entity.type() == state.runtime.ENTITY_MUSICAL_NOTE) {
                RoomEntity note = state.runtime.advanceMusicalNoteEntity(state.entity);
                if (note == null) {
                    return false;
                }
                state.runtime.slots[state.index] = note;
                return false;
            }
            if (state.status == EntityStatus.ACTIVE && state.entity.type() == state.runtime.ENTITY_BOMB) {
                BombMotion.Decision bombDecision = state.runtime.advanceBombEntity(state.index, state.entity,
                    state.projectileEvents, state.linkEntityX, state.linkEntityY, state.projectileLinkState);
                state.entity = state.runtime.slots[state.index];
                if (bombDecision.phase() == BombMotion.Phase.NORMAL) {
                    state.runtime.slots[state.index] = state.runtime.bombPrivateState4[state.entity.slot()] != 0
                        ? state.runtime.advanceEnemyBombEntity(state.entity, state.backgroundCollision, state.frame)
                        : state.runtime.advancePlacedBombEntity(state.entity, state.backgroundCollision, state.frame);
                }
                state.runtime.tryLiftBombIfRequested(state.index, bombDecision, state.linkEntityX, state.linkEntityY,
                    state.linkZ, state.romLinkDirection);
                return false;
            }
            if (state.status == EntityStatus.ACTIVE && state.entity.type() == state.runtime.ENTITY_ARROW) {
                if (state.runtime.playerArrowBombArrow[state.entity.slot()]
                    && state.runtime.playerArrowMotion.transitionCountdown(state.entity.slot()) != 0) {
                    int explosionSlot = state.runtime.spawnBombFromBombArrow(state.entity);
                    if (explosionSlot >= 0) {
                        state.runtime.pendingEntityEvents.add(new EntityCombatEvent(
                            explosionSlot, state.runtime.ENTITY_BOMB, 0, false,
                            EntityCombatEvent.SoundChannel.NOISE, 0x0C));
                    }
                    state.runtime.disableEntityWithoutPersistence(state.entity.slot());
                    return false;
                }
                // ArrowEntityHandler jumps straight to rendering/rebound
                // while its transition countdown is nonzero ($6A3D-$6A40).
                boolean hitEntity = state.runtime.playerArrowMotion.transitionCountdown(state.entity.slot()) == 0
                    && state.runtime.collidePlayerArrowWithEntities(state.entity, state.frame);
                if (hitEntity && !state.runtime.playerArrowBombArrow[state.entity.slot()]) {
                    // func_003_75A2 reaches jr_003_7737 after an ordinary
                    // arrow target hit. With no wall-collision byte set on
                    // the active arrow, the state-0 arrow unloads there.
                    state.runtime.disableEntityWithoutPersistence(state.entity.slot());
                    return false;
                }
                PlayerArrowMotion.Update arrowUpdate = state.runtime.playerArrowMotion.advance(
                    state.entity, state.backgroundCollision, state.frame, state.runtime.entityGroundStatus[state.entity.slot()],
                    state.runtime.groundInteractionSideScrolling, state.handlerInteractive, state.projectileTimersInteractive);
                if (arrowUpdate.unloaded()) {
                    state.runtime.disableEntityWithoutPersistence(state.entity.slot());
                    return false;
                }
                if (arrowUpdate.collidedWithWall()) {
                    state.runtime.pendingEntityEvents.add(new EntityCombatEvent(
                        state.entity.slot(), state.runtime.ENTITY_ARROW, 0, false,
                        EntityCombatEvent.SoundChannel.JINGLE, 0x07));
                    // AlertSwordMoblins is shared by player arrows, enemy
                    // projectiles, and bomb explosions.
                    state.runtime.swordMoblinAlertingSoundCounter = 0x04;
                }
                state.runtime.slots[state.index] = arrowUpdate.entity();
                return false;
            }
            if (state.status == EntityStatus.ACTIVE && state.entity.type() == state.runtime.ENTITY_LIFTABLE_ROCK
                && state.runtime.liftableRockSmashActive[state.entity.slot()]) {
                state.runtime.advanceLiftableRockSmash(state.index, state.entity, state.frame);
                return false;
            }
            if (state.status == EntityStatus.ACTIVE && state.entity.type() == state.runtime.ENTITY_HEART_CONTAINER
                && state.runtime.enemyTransitionCountdown[state.entity.slot()] != 0) {
                state.runtime.advanceCollectedHeartContainer(state.entity, state.linkEntityX, state.linkEntityY, state.linkZ);
                return false;
            }
            if (state.status == EntityStatus.ACTIVE
                && state.entity.type() == state.runtime.ENTITY_MADAM_MEOWMEOW) {
                state.runtime.advanceMadamMeowMeow(state.entity, state.frame, state.linkEntityX, state.linkEntityY, state.linkAirborne);
                return false;
            }
            if (state.status == EntityStatus.ACTIVE && state.entity.type() == state.runtime.ENTITY_RICHARD) {
                state.runtime.advanceRichard(state.entity, state.frame, state.linkEntityX, state.linkEntityY, state.linkAirborne);
                return false;
            }
            if (state.status == EntityStatus.ACTIVE && state.entity.type() == 0x42
                && state.runtime.indoorRoom && state.runtime.entityMapId != 0x16) {
                state.runtime.advanceOwlStatue(state.entity, state.linkEntityX, state.linkEntityY, state.linkZ,
                    state.projectileLinkState.motionState(), state.linkAirborne);
                return false;
            }
            if (state.status == EntityStatus.ACTIVE && state.entity.type() == state.runtime.ENTITY_BOW_WOW
                && state.entity.sourceLoadOrder() >= 0 && state.runtime.bowWowState == 0x80
                && state.handlerLinkCollisionEnabled && state.romCollisionType != 0
                && withinUnsignedWindow(state.linkEntityX, 0x88, 0x10)
                && withinUnsignedWindow(state.linkEntityY, 0x40, 0x10)) {
                // BowWowEntityHandler's kidnapped branch checks the handler's
                // collision byte only after Link enters the $88/$40 pen.
                state.runtime.bowWowState = 0x01;
                if (state.runtime.spriteHandlers != null) {
                    state.entity = withDefinition(state.entity,
                        state.runtime.spriteHandlers.forFollowerEntityType(state.runtime.ENTITY_BOW_WOW),
                        state.entity.spriteVariant());
                    state.runtime.slots[state.index] = state.entity;
                }
                state.runtime.followingNpcState = new FollowingNpcState(
                    state.runtime.followingNpcState.roosterFollowing(),
                    state.runtime.followingNpcState.ghostFollowingState(),
                    state.runtime.followingNpcState.marinFollowing(), true,
                    state.runtime.followingNpcState.instrument4Flags(),
                    state.runtime.followingNpcState.powerBraceletLevel(), state.runtime.followingNpcState.db10());
                state.runtime.pendingMusicTrack = 0x10;
                state.runtime.pendingDialogRequests.add(new DialogRequest(1, 0x6C));
                state.runtime.pendingEntityEvents.add(new EntityCombatEvent(
                    state.entity.slot(), state.entity.type(), 0, false,
                    EntityCombatEvent.SoundChannel.WAVE, 0x18));
            }
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
            if (state.status == EntityStatus.ACTIVE
                && state.entity.type() == state.runtime.ENTITY_SWORD_SHIELD_PICKUP) {
                if (state.runtime.chestSwordLevel == 0 && state.runtime.spriteHandlers != null) {
                    state.entity = withDefinition(state.entity,
                        state.runtime.spriteHandlers.forSwordShieldPickup(true), state.entity.spriteVariant());
                    state.runtime.slots[state.index] = state.entity;
                }
                if (state.runtime.swordPickupSequenceActive[state.entity.slot()]) {
                    state.runtime.advanceSwordPickup(state.entity, state.frame, state.linkEntityX, state.linkEntityY, state.linkZ);
                    return false;
                }
            }
            if (state.status == EntityStatus.ACTIVE && state.runtime.powerUpPickupCountdown[state.entity.slot()] != 0) {
                state.runtime.advancePowerUpPickup(state.entity, state.frame, state.linkEntityX, state.linkEntityY, state.linkZ);
                return false;
            }
            if (state.status == EntityStatus.ACTIVE && state.entity.type() == state.runtime.ENTITY_SLEEPY_TOADSTOOL) {
                if (!state.runtime.toadstoolPickupSequenceActive[state.entity.slot()]
                    && (state.runtime.playerHasToadstool || state.runtime.playerMagicPowderCount != 0)) {
                    state.runtime.disableEntityWithoutPersistence(state.entity.slot());
                    return false;
                }
                if (state.runtime.toadstoolPickupSequenceActive[state.entity.slot()]) {
                    state.runtime.advanceToadstoolPickup(state.entity, state.linkEntityX, state.linkEntityY, state.linkZ);
                    return false;
                }
            }
            if (state.status == EntityStatus.ACTIVE && state.entity.type() == state.runtime.ENTITY_OWL_EVENT) {
                state.runtime.advanceOwlEvent(state.entity, state.frame, state.linkEntityX, state.linkEntityY,
                    state.handlerLinkCollisionEnabled);
                return false;
            }
        return true;
    }
}
