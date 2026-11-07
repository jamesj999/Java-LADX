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

/** Registered ROM type callbacks for ordered slot phase 1. */
final class EntityPhase1Handlers {
    static void install(EntityHandlerRegistry registry) {
        registry.registerTick(RoomEntityRuntime.ENTITY_KANALET_BOMBABLE_WALL, 1, state -> {
if (true) {
                if ((state.runtime.entityRoomStatus & 0x10) != 0) {
                    state.runtime.lifecycle.disableEntityWithoutPersistence(state.entity.slot());
                    return false;
                }
                state.runtime.droppedItemBySlot[state.entity.slot()] = state.runtime.ENTITY_HIDING_SLIME_KEY;
                if (state.runtime.enemyIgnoreHitsCountdown[state.entity.slot()] != 0) {
                    if (state.runtime.entityEnemySpawns.spawnKanaletWallMoblin(state.entity)) {
                        state.runtime.lifecycle.disableEntityWithoutPersistence(state.entity.slot());
                        return false;
                    }
                }
            }
            return true;
        });
        registry.registerTick(RoomEntityRuntime.ENTITY_TARIN, 2, state -> {
if (!state.runtime.indoorRoom && true
                && (state.runtime.entityRoomStatus & 0x10) != 0) {
                state.runtime.lifecycle.disableEntityWithoutPersistence(state.entity.slot());
                return false;
            }
            return true;
        });
        registry.registerTick(RoomEntityRuntime.ENTITY_MARIN_INDOOR, 3, state -> {
if (state.runtime.indoorRoom && true && state.runtime.chestPlayerLevelsKnown
                && state.runtime.chestSwordLevel != 0) {
                state.runtime.lifecycle.disableEntityWithoutPersistence(state.entity.slot());
                return false;
            }
            return true;
        });
        registry.registerTick(RoomEntityRuntime.ENTITY_BOO_BUDDY, 4, state -> {
if (true && state.initialStatus == EntityStatus.ACTIVE) {
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
            return true;
        });
        registry.registerTick(RoomEntityRuntime.ENTITY_BOW_WOW, 5, state -> {
if (true && state.initialStatus == EntityStatus.ACTIVE) {
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
            return true;
        });
        registry.registerTick(RoomEntityRuntime.ENTITY_MOLDORM, 6, state -> {
if (!state.wasInitializing && true
                && !state.runtime.roomTransitionActive && !state.runtime.inventoryAppearing) {
                BossIntroMotion.Update intro = state.runtime.bossIntroMotion.advance(
                    state.runtime.options1(state.entity.slot()), state.entity.type(), state.runtime.entityMapId, state.runtime.transitionSequenceCounter);
                if (intro.musicTrack() >= 0) state.runtime.pendingMusicTrack = intro.musicTrack();
                if (intro.dialogLowId() >= 0) {
                    state.runtime.pendingDialogRequests.add(new DialogRequest(intro.dialogTableId(), intro.dialogLowId()));
                    state.moldormIntroOpenedThisFrame = true;
                }
            }
            return true;
        });
        registry.registerTick(RoomEntityRuntime.ENTITY_SIDE_VIEW_POT, 7, state -> {
if (state.status == EntityStatus.ACTIVE
            && true
            && !state.runtime.sideViewPotMotion.active(state.entity.slot())) {
                SideViewPotContact.Result contact = SideViewPotContact.Result.none();
                int effectiveLinkX = state.linkEntityX;
                int effectiveLinkY = state.linkEntityY;
                if (state.sideViewPotLinkCollisionInteractive) {
                    contact = SideViewPotContact.resolve(
                        state.entity, state.linkEntityX, state.linkEntityY, state.linkAirborne, state.runtime.currentLinkSpeedY);
                    if (contact.collided()) {
                        state.runtime.liftThrow.pendingLinkPotContact(state.entity.slot(), contact);
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
                    && state.runtime.liftThrow.sideViewPotPowerBraceletHeld()
                    && state.runtime.linkAttackStepAnimationCountdown == 0
                    && state.runtime.beginLift(state.entity.slot(), state.romLinkDirection)) {
                    state.runtime.pendingEntityEvents.add(new EntityCombatEvent(
                        state.entity.slot(), state.entity.type(), 0, false,
                        EntityCombatEvent.SoundChannel.WAVE, 0x02));
                }
                return false;
            }
            return true;
        });
        registry.registerTick(RoomEntityRuntime.ENTITY_SIDE_VIEW_POT, 8, state -> {
if (state.status == EntityStatus.ACTIVE
                && true
                && state.runtime.sideViewPotMotion.active(state.entity.slot())) {
                if (state.sideViewPotInteractive) {
                    SideViewPotMotion.Update potUpdate = state.runtime.sideViewPotMotion.advance(state.entity);
                    RoomEntity movedPot = potUpdate.entity();
                    boolean collision = state.runtime.liftThrow.sideViewPotBackgroundCollision(state.entity, movedPot,
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
            return true;
        });
        registry.registerTick(RoomEntityRuntime.ENTITY_DOG, 9, state -> {
if (state.status == EntityStatus.ACTIVE && true) {
                // DogEntityHandler writes this before ReturnIfNonInteractive_19.
                state.runtime.enemyHealth[state.entity.slot()] = DogMotion.HANDLER_HEALTH;
            }
            return true;
        });
        registry.registerTick(RoomEntityRuntime.ENTITY_HOOKSHOT_BRIDGE, 10, state -> {
if (state.status == EntityStatus.ACTIVE && true) {
                state.runtime.backgroundMechanics.advanceHookshotBridgeEntity(state.index, state.entity);
                return false;
            }
            return true;
        });
        registry.registerTick(RoomEntityRuntime.ENTITY_HOOKSHOT_CHAIN, 11, state -> {
if (state.status == EntityStatus.ACTIVE && true) {
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
                boolean unmasked = state.runtime.projectiles.collideHookshotWithEntities(state.entity, step.state(), state.frame);
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
                            && state.runtime.entityPlayerSpawns.spawnHookshotBridge(object.objectLeft() + 0x08,
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
            return true;
        });
        registry.registerTick(RoomEntityRuntime.ENTITY_BOOMERANG, 12, state -> {
if (state.status == EntityStatus.ACTIVE && true) {
                state.runtime.projectiles.advanceBoomerangEntity(state.index, state.entity, state.frame, state.linkEntityX, state.linkEntityY,
                    state.linkZ, state.projectileLinkState.motionState(), state.handlerInteractive,
                    state.projectileTimersInteractive);
                return false;
            }
            return true;
        });
        registry.registerTick(RoomEntityRuntime.ENTITY_SWORD_BEAM, 13, state -> {
if (state.status == EntityStatus.ACTIVE && true) {
                state.runtime.projectiles.advanceSwordBeamEntity(state.index, state.entity, state.frame, state.linkEntityX, state.linkEntityY,
                    state.romLinkDirection, state.handlerInteractive);
                return false;
            }
            return true;
        });
        registry.registerTick(RoomEntityRuntime.ENTITY_MAGIC_ROD_FIREBALL, 14, state -> {
if (state.status == EntityStatus.ACTIVE && true) {
                state.runtime.projectiles.advanceMagicRodFireballEntity(state.index, state.entity, state.frame,
                    state.handlerInteractive, state.projectileTimersInteractive);
                return false;
            }
            return true;
        });
        registry.registerTick(RoomEntityRuntime.ENTITY_MAGIC_POWDER_SPRINKLE, 15, state -> {
if (state.status == EntityStatus.ACTIVE
                && true) {
                state.runtime.projectiles.advanceMagicPowderSprinkleEntity(state.index, state.entity, state.frame,
                    state.projectileLinkState.motionState(),
                    state.handlerInteractive && !state.creditsGameplay, state.projectileTimersInteractive);
                return false;
            }
            return true;
        });
        registry.registerTick(RoomEntityRuntime.ENTITY_MUSICAL_NOTE, 16, state -> {
if (state.status == EntityStatus.ACTIVE && true) {
                RoomEntity note = state.runtime.slotProperties.advanceMusicalNoteEntity(state.entity);
                if (note == null) {
                    return false;
                }
                state.runtime.slots[state.index] = note;
                return false;
            }
            return true;
        });
        registry.registerTick(RoomEntityRuntime.ENTITY_BOMB, 17, state -> {
if (state.status == EntityStatus.ACTIVE && true) {
                BombMotion.Decision bombDecision = state.runtime.bombInteractions.advanceBombEntity(state.index, state.entity,
                    state.projectileEvents, state.linkEntityX, state.linkEntityY, state.projectileLinkState);
                state.entity = state.runtime.slots[state.index];
                if (bombDecision.phase() == BombMotion.Phase.NORMAL) {
                    state.runtime.slots[state.index] = state.runtime.bombPrivateState4[state.entity.slot()] != 0
                        ? state.runtime.liftThrow.advanceEnemyBombEntity(state.entity, state.backgroundCollision, state.frame)
                        : state.runtime.liftThrow.advancePlacedBombEntity(state.entity, state.backgroundCollision, state.frame);
                }
                state.runtime.liftThrow.tryLiftBombIfRequested(state.index, bombDecision, state.linkEntityX, state.linkEntityY,
                    state.linkZ, state.romLinkDirection);
                return false;
            }
            return true;
        });
        registry.registerTick(RoomEntityRuntime.ENTITY_ARROW, 18, state -> {
if (state.status == EntityStatus.ACTIVE && true) {
                if (state.runtime.playerArrowBombArrow[state.entity.slot()]
                    && state.runtime.playerArrowMotion.transitionCountdown(state.entity.slot()) != 0) {
                    int explosionSlot = state.runtime.entityPlayerSpawns.spawnBombFromBombArrow(state.entity);
                    if (explosionSlot >= 0) {
                        state.runtime.pendingEntityEvents.add(new EntityCombatEvent(
                            explosionSlot, state.runtime.ENTITY_BOMB, 0, false,
                            EntityCombatEvent.SoundChannel.NOISE, 0x0C));
                    }
                    state.runtime.lifecycle.disableEntityWithoutPersistence(state.entity.slot());
                    return false;
                }
                // ArrowEntityHandler jumps straight to rendering/rebound
                // while its transition countdown is nonzero ($6A3D-$6A40).
                boolean hitEntity = state.runtime.playerArrowMotion.transitionCountdown(state.entity.slot()) == 0
                    && state.runtime.projectiles.collidePlayerArrowWithEntities(state.entity, state.frame);
                if (hitEntity && !state.runtime.playerArrowBombArrow[state.entity.slot()]) {
                    // func_003_75A2 reaches jr_003_7737 after an ordinary
                    // arrow target hit. With no wall-collision byte set on
                    // the active arrow, the state-0 arrow unloads there.
                    state.runtime.lifecycle.disableEntityWithoutPersistence(state.entity.slot());
                    return false;
                }
                PlayerArrowMotion.Update arrowUpdate = state.runtime.playerArrowMotion.advance(
                    state.entity, state.backgroundCollision, state.frame, state.runtime.entityGroundStatus[state.entity.slot()],
                    state.runtime.groundInteractionSideScrolling, state.handlerInteractive, state.projectileTimersInteractive);
                if (arrowUpdate.unloaded()) {
                    state.runtime.lifecycle.disableEntityWithoutPersistence(state.entity.slot());
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
            return true;
        });
        registry.registerTick(RoomEntityRuntime.ENTITY_LIFTABLE_ROCK, 19, state -> {
if (state.status == EntityStatus.ACTIVE && true
                && state.runtime.liftableRockSmashActive[state.entity.slot()]) {
                state.runtime.liftThrow.advanceLiftableRockSmash(state.index, state.entity, state.frame);
                return false;
            }
            return true;
        });
        registry.registerTick(RoomEntityRuntime.ENTITY_HEART_CONTAINER, 20, state -> {
if (state.status == EntityStatus.ACTIVE && true
                && state.runtime.enemyTransitionCountdown[state.entity.slot()] != 0) {
                state.runtime.bossRewards.advanceCollectedHeartContainer(state.entity, state.linkEntityX, state.linkEntityY, state.linkZ);
                return false;
            }
            return true;
        });
        registry.registerTick(RoomEntityRuntime.ENTITY_MADAM_MEOWMEOW, 21, state -> {
if (state.status == EntityStatus.ACTIVE
                && true) {
                state.runtime.story.advanceMadamMeowMeow(state.entity, state.frame, state.linkEntityX, state.linkEntityY, state.linkAirborne);
                return false;
            }
            return true;
        });
        registry.registerTick(RoomEntityRuntime.ENTITY_RICHARD, 22, state -> {
if (state.status == EntityStatus.ACTIVE && true) {
                state.runtime.story.advanceRichard(state.entity, state.frame, state.linkEntityX, state.linkEntityY, state.linkAirborne);
                return false;
            }
            return true;
        });
        registry.registerTick(RoomEntityRuntime.ENTITY_BOW_WOW, 23, state -> {
if (state.status == EntityStatus.ACTIVE && true
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
            return true;
        });
        registry.registerTick(RoomEntityRuntime.ENTITY_SWORD_SHIELD_PICKUP, 24, state -> {
if (state.status == EntityStatus.ACTIVE
                && true) {
                if (state.runtime.chestSwordLevel == 0 && state.runtime.spriteHandlers != null) {
                    state.entity = withDefinition(state.entity,
                        state.runtime.spriteHandlers.forSwordShieldPickup(true), state.entity.spriteVariant());
                    state.runtime.slots[state.index] = state.entity;
                }
                if (state.runtime.swordPickupSequenceActive[state.entity.slot()]) {
                    state.runtime.rewards.advanceSwordPickup(state.entity, state.frame, state.linkEntityX, state.linkEntityY, state.linkZ);
                    return false;
                }
            }
            return true;
        });
        registry.registerTick(RoomEntityRuntime.ENTITY_SLEEPY_TOADSTOOL, 25, state -> {
if (state.status == EntityStatus.ACTIVE && true) {
                if (!state.runtime.toadstoolPickupSequenceActive[state.entity.slot()]
                    && (state.runtime.playerHasToadstool || state.runtime.playerMagicPowderCount != 0)) {
                    state.runtime.lifecycle.disableEntityWithoutPersistence(state.entity.slot());
                    return false;
                }
                if (state.runtime.toadstoolPickupSequenceActive[state.entity.slot()]) {
                    state.runtime.rewards.advanceToadstoolPickup(state.entity, state.linkEntityX, state.linkEntityY, state.linkZ);
                    return false;
                }
            }
            return true;
        });
        registry.registerTick(RoomEntityRuntime.ENTITY_OWL_EVENT, 26, state -> {
if (state.status == EntityStatus.ACTIVE && true) {
                state.runtime.story.advanceOwlEvent(state.entity, state.frame, state.linkEntityX, state.linkEntityY,
                    state.handlerLinkCollisionEnabled);
                return false;
            }
            return true;
        });
    }
}
