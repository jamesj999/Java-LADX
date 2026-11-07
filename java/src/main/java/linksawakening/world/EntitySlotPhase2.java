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

/** Ordered slot dispatch phase 2; false preserves the ROM handler early return. */
final class EntitySlotPhase2 {
    static boolean run(EntityFrameExecution state) {

            if (state.status == EntityStatus.LIFTED) {
                RoomEntity lifted = state.runtime.renderLiftedEntity(state.entity, state.frame);
                // Side-view pot pickup enters status LIFTED with the ROM's
                // transition countdown set to $02. UpdateEntityTimers has
                // already decremented it for this frame; EntityLiftedHandler
                // does not advance the pot until that countdown reaches zero.
                // Other lifted entities retain their established generic
                // presentation timing here.
                if (state.entity.type() == state.runtime.ENTITY_SIDE_VIEW_POT
                    && state.runtime.enemyTransitionCountdown[state.entity.slot()] != 0) {
                    state.runtime.slots[state.index] = lifted;
                    return false;
                }
                int liftedRomDirection = state.romLinkDirection;
                if (state.entity.type() == state.runtime.ENTITY_ROOSTER) {
                    RoosterMotion.Update roosterUpdate = RoosterMotion.advanceLifted(
                        state.frame, state.linkZ & 0xFF, state.runtime.linkPressedButtonsMask, state.romCollisionType,
                        state.runtime.currentLinkSpeedX, state.runtime.currentLinkSpeedY, state.romLinkDirection);
                    lifted = withVariant(lifted, roosterUpdate.spriteVariant());
                    state.runtime.liftedSourceDirection[state.entity.slot()] = roosterUpdate.romDirection();
                    liftedRomDirection = roosterUpdate.romDirection();
                    state.runtime.pendingRoosterLinkStateRequests.add(new RoosterLinkStateRequest(
                        state.entity.slot(), roosterUpdate.positionZ(), roosterUpdate.velocityZ(),
                        roosterUpdate.speedX(), roosterUpdate.speedY(),
                        roosterUpdate.romDirection()));
                    state.runtime.playBoomerangSoundIfDue(state.entity);
                }
                state.runtime.slots[state.index] = state.runtime.advanceLiftedEntity(lifted, state.linkEntityX, state.linkEntityY, state.linkZ,
                    liftedRomDirection);
                return false;
            } else if (state.status == EntityStatus.THROWN) {
                if (state.entity.type() == state.runtime.ENTITY_BOMB) {
                    BombMotion.Decision bombDecision = state.runtime.advanceBombEntity(state.index, state.entity,
                        state.projectileEvents, state.linkEntityX, state.linkEntityY, state.projectileLinkState);
                    state.entity = state.runtime.slots[state.index];
                    if (bombDecision.unloadAfterPresentation()) {
                        return false;
                    }
                }
                RoomEntity thrown = state.runtime.advanceThrownEntity(state.entity, state.backgroundCollision,
                    state.runtime.groundInteractionSideScrolling, state.romLinkDirection, state.frame);
                state.runtime.slots[state.index] = thrown;
                if (thrown.status() == EntityStatus.THROWN) {
                    state.runtime.collideThrownEntityWithEntities(thrown, state.frame);
                }
                return false;
            } else if (state.status == EntityStatus.FALLING) {
                if (state.runtime.entityMapId == state.runtime.MAP_COLOR_DUNGEON
                    && ColorShellMotion.isColorShellType(state.entity.type())) {
                    state.runtime.colorShellMotion.enterState6(state.entity);
                    RoomEntity activeShell = state.runtime.refreshColorShellDisplay(
                        withStatus(state.entity, EntityStatus.ACTIVE), EntityStatus.ACTIVE);
                    state.runtime.slots[state.index] = activeShell;
                    return false;
                }
                RoomEntity falling = state.runtime.advanceFallingEntity(state.entity);
                if (falling == null) {
                    state.runtime.disableEntityWithoutPersistence(state.entity.slot());
                    return false;
                }
                int renderFlipAttribute = state.runtime.baseEntityFlipAttribute[state.entity.slot()];
                if (state.runtime.enemyFlashCountdown[state.entity.slot()] > 0) {
                    renderFlipAttribute ^= (state.runtime.enemyFlashCountdown[state.entity.slot()] << 2) & 0x10;
                }
                state.runtime.slots[state.index] = new RoomEntity(
                    falling.slot(), falling.sourceLoadOrder(), falling.type(), falling.x(),
                    falling.y(), EntityStatus.FALLING, falling.spriteDefinition(),
                    falling.spriteVariant(), renderFlipAttribute, falling.spriteTileOffset(),
                    falling.z(), -1, false);
                return false;
            } else if (state.status == EntityStatus.DYING) {
                if (state.entity.type() == state.runtime.ENTITY_MOBLIN_KING) {
                    state.runtime.advanceMoblinKingDestruction(state.entity, state.frame, state.randomByteSupplier);
                    return false;
                }
                if (state.entity.type() == state.runtime.ENTITY_GENIE
                    && state.runtime.geniePrivateState1[state.entity.slot()] == 1) {
                    state.runtime.advanceGenieDestruction(state.entity, state.frame, state.randomByteSupplier);
                    return false;
                }
                if (state.entity.type() == state.runtime.ENTITY_ROLLING_BONES) {
                    state.runtime.advanceRollingBonesDestruction(state.entity, state.randomByteSupplier);
                    return false;
                }
                if (state.entity.type() == state.runtime.ENTITY_MOLDORM) {
                    state.runtime.advanceMoldormDestruction(state.entity, state.frame);
                    return false;
                }
                if (isBossKeyDropProducer(state.entity.type())) {
                    state.runtime.advanceBossKeyDropProducer(state.entity, state.randomByteSupplier);
                    return false;
                }
                if (state.runtime.dyingCountdown[state.entity.slot()] == 0) {
                    state.runtime.handleTerminalEnemyDeath(state.entity, state.randomByteSupplier);
                } else {
                    int slot = state.entity.slot();
                    int presentationCountdown = state.runtime.dyingCountdown[slot];
                    // EntityDeathHandler ($03:557D): the initial counter is a
                    // ceiling, not a fixed wait. Begin destruction once the
                    // hit recoil has expired; bosses use their handlers above.
                    if (presentationCountdown >= 0x20 && state.runtime.enemyIgnoreHitsCountdown[slot] == 0) {
                        state.runtime.dyingCountdown[slot] = 0x1F;
                        state.runtime.pendingEntityEvents.add(new EntityCombatEvent(
                            slot, state.entity.type(), 0, false,
                            EntityCombatEvent.SoundChannel.NOISE, 0x13));
                    }
                    RoomEntity updated = state.runtime.enemyIgnoreHitsCountdown[slot] > 0
                        ? state.runtime.applyEnemyRecoilIfNeeded(state.entity, state.backgroundCollision).entity() : state.entity;
                    updated = state.runtime.refreshColorShellDisplay(updated, state.status);
                    state.runtime.slots[state.index] = withDeathPresentation(updated,
                        state.runtime.deathSpriteVariantForCountdown(presentationCountdown),
                        state.runtime.powerRecoilDeath[slot]);
                }
                return false;
            } else if (state.status == EntityStatus.BURNING) {
                if (state.runtime.enemyTransitionCountdown[state.entity.slot()] == 0) {
                    state.runtime.finishBurning(state.entity);
                } else {
                    state.runtime.slots[state.index] = state.runtime.refreshColorShellDisplay(state.entity, state.status);
                }
                return false;
            } else if (state.status == EntityStatus.STUNNED) {
                if (state.entity.type() == state.runtime.ENTITY_BOMB) {
                    BombMotion.Decision bombDecision = state.runtime.advanceBombEntity(state.index, state.entity,
                        state.projectileEvents, state.linkEntityX, state.linkEntityY, state.projectileLinkState);
                    state.entity = state.runtime.slots[state.index];
                    if (bombDecision.unloadAfterPresentation()) {
                        return false;
                    }
                    state.entity = state.runtime.applyBombStunnedPostActiveWork(state.entity, state.backgroundCollision, state.frame);
                    state.runtime.slots[state.index] = state.entity;
                }
                if (state.runtime.powerBraceletButtonHeld
                    && isLiftableEntity(state.entity)
                    && (state.entity.type() == state.runtime.ENTITY_BOMB
                        || (state.runtime.enemyPhysicsFlags[state.entity.slot()] & state.runtime.ENTITY_PHYSICS_GRABBABLE) != 0)
                    && RoomEntityPickupRules.overlapsLink(state.entity, state.linkEntityX, state.linkEntityY)) {
                    if (state.runtime.beginLift(state.entity.slot(), state.romLinkDirection)) {
                        state.runtime.slots[state.index] = state.runtime.advanceLiftedEntity(state.runtime.renderLiftedBomb(state.runtime.slots[state.index]),
                            state.linkEntityX, state.linkEntityY, state.linkZ, state.romLinkDirection);
                    }
                    return false;
                }
                if (state.runtime.enemyStunnedCountdown[state.entity.slot()] == 0) {
                    state.runtime.slots[state.index] = state.runtime.refreshColorShellDisplay(
                        withStatus(state.entity, EntityStatus.ACTIVE), EntityStatus.ACTIVE);
                } else {
                    state.runtime.slots[state.index] = state.runtime.refreshColorShellDisplay(state.entity, state.status);
                }
                return false;
            } else if (state.status == EntityStatus.INIT) {
                if (state.entity.type() == state.runtime.ENTITY_HIDING_SLIME_KEY
                    && (state.runtime.entityRoomStatus & 0x10) != 0) {
                    // The handler's first instruction is the completed-room
                    // gate, including the INIT dispatch path.
                    state.runtime.disableEntityWithoutPersistence(state.entity.slot());
                    return false;
                }
                state.status = EntityStatus.ACTIVE;
                state.runtime.initializeEntityTimers(state.entity);
                state.runtime.entityHandlers.initialize(state.entity, state.randomByteSupplier);

                if (state.entity.type() == state.runtime.ENTITY_OCTOROK || state.entity.type() == state.runtime.ENTITY_MOBLIN) {
                    state.runtime.roamingEnemyMotion.initialize(state.entity.slot());
                }

                if (state.entity.type() == state.runtime.ENTITY_MOBLIN_SWORD) {
                    state.runtime.moblinSwordMotion.initialize(state.entity.slot(), state.entity.x());
                }



                if (isSparkType(state.entity.type())) {
                    state.runtime.sparkMotion.initialize(state.entity.slot(), state.entity.type());
                }
                if (isZolGelType(state.entity.type())) {
                    state.runtime.zolGelMotion.initialize(state.entity.slot(), state.entity.type(), state.randomByteSupplier);
                }




                if (state.entity.type() == state.runtime.ENTITY_MINI_MOLDORM) {
                    state.runtime.miniMoldormMotion.initialize(state.entity);
                }
                if (state.entity.type() == state.runtime.ENTITY_MOLDORM) {
                    state.runtime.moldormMotion.initialize(state.entity);
                }


























                if (state.entity.type() == state.runtime.ENTITY_WIZROBE) {
                    state.runtime.wizrobeMotion.initializeFromRoom(state.entity.slot(), state.entity.spriteVariant());
                    state.runtime.enemyTransitionCountdown[state.entity.slot()] = 0x80;
                    state.preserveWizrobePresentation = true;
                }
                if (state.entity.type() == state.runtime.ENTITY_WIZROBE_PROJECTILE) {
                    state.runtime.wizrobeProjectileMotion.initialize(state.entity.slot(), state.randomByteSupplier);
                    state.preserveWizrobeProjectilePresentation = true;
                }




                if (isGhiniType(state.entity.type())) {
                    state.runtime.ghiniMotion.initialize(state.entity.slot(), state.entity.type());
                    if (isHidingGhiniType(state.entity.type())) {
                        state.preserveGhiniPresentation = true;
                    }
                }




                if (isBombiteType(state.entity.type())) {
                    state.runtime.bombiteMotion.initialize(state.entity.slot());
                    state.runtime.slowTimerInitialized[state.entity.slot()] = true;
                }
                if (isFollowingNpcType(state.entity.type())) {
                    if (state.entity.type() == state.runtime.ENTITY_BOW_WOW) {
                        state.runtime.bowWowMotion.initialize(state.entity.slot());
                    } else {
                        state.runtime.followingNpcMotion.initialize(state.entity.slot(), state.entity.type());
                    }
                }
                if (ColorShellMotion.isColorShellType(state.entity.type())) {
                    state.runtime.colorShellMotion.initialize(state.entity.slot());
                }
            } else if (state.status == EntityStatus.ACTIVE) {
                if (!state.freezeTarinTimers && !state.freezeMoblinKingTimers) {
                    state.runtime.decrementSlowTransitionCountdown(state.entity.slot(), state.frame);
                }
                if (state.runtime.shouldDisappear(state.entity)) {
                    if (state.runtime.slowTransitionCountdown[state.entity.slot()] == 0) {
                        state.runtime.clearEntity(state.entity.slot());
                        return false;
                    }
                }
            }
            state.updated = state.entity;
            if (state.status == EntityStatus.ACTIVE && !state.wasInitializing
                && state.entity.type() == SideViewPlatformMotion.ENTITY_TYPE
                && state.handlerLinkCollisionEnabled && state.runtime.entityTimersInteractive()
                && !state.creditsGameplay && state.runtime.transitionSequenceCounter == 0x04) {
                SideViewPlatformMotion.Frame platformFrame =
                    state.runtime.sideViewPlatformMotion.beginFrame(state.entity);
                RoomEntity movedPlatform = platformFrame.entity();
                if (platformFrame.horizontalDelta() != 0 && state.backgroundCollision != null
                    && state.backgroundCollision.blocks(state.entity,
                        platformFrame.horizontalDelta() < 0 ? 1 : 0,
                        movedPlatform.x(), movedPlatform.y())) {
                    platformFrame = state.runtime.sideViewPlatformMotion.restoreHorizontalPosition(state.entity,
                        platformFrame);
                    movedPlatform = platformFrame.entity();
                }
                if (platformFrame.verticalDelta() != 0 && state.backgroundCollision != null) {
                    int direction = platformFrame.verticalDelta() < 0 ? 2 : 3;
                    if (state.backgroundCollision.blocks(state.entity, direction, movedPlatform.x(),
                        movedPlatform.y())) {
                        platformFrame = state.runtime.sideViewPlatformMotion.restoreVerticalPosition(state.entity,
                            platformFrame);
                        movedPlatform = platformFrame.entity();
                    }
                }
                boolean standing = state.runtime.groundInteractionSideScrolling
                    && (state.linkZ & 0xFF) == 0
                    && (state.runtime.currentLinkSpeedY & 0x80) == 0
                    && sideViewPlatformOverlapsLink(movedPlatform, state.linkEntityX, state.linkEntityY)
                    && (((state.linkEntityY - movedPlatform.y() + 0x08) & 0x80) != 0);
                boolean activationAllowed = state.runtime.entityRoomId == 0x3B || state.carryingEntity;
                if (standing) {
                    state.runtime.pendingSideViewPlatformLinkRequests.add(new SideViewPlatformLinkRequest(
                        state.entity.slot(), platformFrame.horizontalDelta(),
                        (movedPlatform.y() - 0x10) & 0xFF, 0x02, true, activationAllowed));
                }
                SideViewPlatformMotion.Update platformUpdate = state.runtime.sideViewPlatformMotion.finishFrame(
                    movedPlatform, platformFrame, state.frame, standing, activationAllowed);
                state.updated = platformUpdate.entity();
                if (platformUpdate.rumble()) {
                    state.runtime.pendingEntityEvents.add(new EntityCombatEvent(
                        state.entity.slot(), state.entity.type(), 0, false,
                        EntityCombatEvent.SoundChannel.NOISE, 0x11));
                }
            }
            if (state.wasInitializing && state.entity.type() == state.runtime.ENTITY_CHEST_WITH_ITEM) {
                state.runtime.initializeChestEntity(state.entity);
                state.updated = withPositionAndVariant(state.entity, state.entity.x(),
                    (state.entity.y() - 0x08) & 0xFF, state.entity.spriteVariant());
            }
            if (state.wasInitializing && isCommonDroppableType(state.entity.type())
                && state.runtime.droppablePrivateState3[state.entity.slot()] != 0) {
                // The init handler has already set the hidden options. The
                // corresponding active handler will be entered on the next
                // entity tick, so suppress this first display-list pass too.
                state.updated = withVariant(state.entity, -1);
                state.preserveDroppablePresentation = true;
            }
            if (state.preserveGhiniPresentation) {
                state.updated = withVariant(state.entity, -1);
            }
            if (state.wasInitializing && state.entity.type() == state.runtime.ENTITY_ZOMBIE) {
                // The room-loaded Zombie is only a hidden spawn marker; its
                // bank-$18 handler never renders the parent entity.
                state.updated = withVariant(state.entity, -1);
                state.preserveZombiePresentation = true;
            }
            if (state.status == EntityStatus.ACTIVE && !state.wasInitializing
                && state.entity.type() == state.runtime.ENTITY_CHEST_WITH_ITEM) {
                if (state.runtime.advanceChestEntity(state.index, state.entity)) {
                    return false;
                }
                state.updated = state.runtime.slots[state.index];
            }
            if (state.status == EntityStatus.ACTIVE && !state.wasInitializing
                && state.entity.type() == state.runtime.ENTITY_DROPPABLE_SECRET_SEASHELL) {
                if (state.runtime.chestSwordLevel >= 0x02
                    || (state.runtime.entityRoomStatus & 0x10) != 0
                    || (state.runtime.entityRoomId == 0xE3 && (state.runtime.entityRoomStatus & 0x40) == 0)) {
                    // DroppableSeashellEntityHandler uses UnloadEntityAndReturn
                    // for these gates, which deliberately does not mark the
                    // source load-order bit as completed.
                    state.runtime.disableEntityWithoutPersistence(state.entity.slot());
                    return false;
                }
                RoomEntityObjectSample objectUnderEntity = state.runtime.objectQuery == null
                    ? null : state.runtime.objectQuery.sample(state.entity);
                SecretSeashellMotion.Update shellUpdate = state.runtime.secretSeashellMotion.advance(
                    state.entity, state.runtime.entityRoomId, state.frame,
                    false, state.runtime.secretSeashellScreenShakeActive,
                    state.runtime.secretSeashellPegasusCollisionActive,
                    state.runtime.secretSeashellPegasusCollisionCoordinatesKnown,
                    state.runtime.secretSeashellPegasusCollisionX,
                    state.runtime.secretSeashellPegasusCollisionY,
                    state.linkEntityX, state.linkEntityY, objectUnderEntity);
                state.updated = shellUpdate.entity();
                state.runtime.entityOptions1Override[state.entity.slot()] =
                    state.runtime.secretSeashellMotion.privateState3(state.entity.slot()) != 0
                        ? state.runtime.SECRET_SEASHELL_HIDDEN_OPTIONS1
                        : state.runtime.SECRET_SEASHELL_REVEALED_OPTIONS1;
                state.preserveSecretSeashellPresentation = true;
            }
            if (state.status == EntityStatus.ACTIVE && !state.wasInitializing
                && state.entity.type() == state.runtime.ENTITY_HIDING_SLIME_KEY
                && (state.runtime.entityRoomStatus & 0x10) != 0) {
                // HidingSlimeKeyEntityHandler checks ROOM_STATUS_EVENT_1 before
                // calling the shared reveal helper and unloads without touching
                // the source persistence mask.
                state.runtime.disableEntityWithoutPersistence(state.entity.slot());
                return false;
            }
            if (state.status == EntityStatus.ACTIVE && !state.wasInitializing
                && isCommonDroppableType(state.entity.type())) {
                CommonDroppableUpdate droppableUpdate = state.runtime.advanceCommonDroppable(
                    state.entity, state.linkEntityX, state.linkEntityY,
                    state.runtime.objectQuery == null ? null : state.runtime.objectQuery.sample(state.entity));
                state.updated = droppableUpdate.entity();
                if (droppableUpdate.hidden()) {
                    // DroppableRevealOrReturnIfNeeded returns past the caller's
                    // remaining handler. In particular, no rendering, pickup,
                    // movement, or background interaction may run while hidden.
                    state.runtime.slots[state.index] = withStatus(withVariant(state.updated, -1), state.status);
                    return false;
                }
            }
            if (state.status == EntityStatus.ACTIVE && !state.wasInitializing
                && state.entity.type() == state.runtime.ENTITY_HIDING_SLIME_KEY) {
                int slot = state.entity.slot();
                SlimeKeyTransitionUpdate keyUpdate = state.runtime.advanceHidingSlimeKey(
                    state.updated, slot, state.linkEntityX, state.linkEntityY, state.linkZ);
                state.runtime.enemyTransitionCountdown[slot] = keyUpdate.nextTransitionCountdown();
                if (keyUpdate.reward() != null) {
                    state.runtime.pendingSlimeKeyRewardEvents.add(keyUpdate.reward());
                    state.runtime.pendingDialogRequests.add(new DialogRequest(
                        0, keyUpdate.reward().dialogLowId()));
                }
                if (keyUpdate.unload()) {
                    state.runtime.disableEntityWithoutPersistence(slot);
                    return false;
                }
                state.keyDropTransitionActive = keyUpdate.holdAboveLink();
                state.updated = keyUpdate.entity();
            }
            if (state.status == EntityStatus.ACTIVE && !state.wasInitializing
                && state.entity.type() == state.runtime.ENTITY_KEY_DROP_POINT) {
                // decrementEnemyStatusCountdowns mirrors UpdateEntityTimers'
                // shared transition-byte decrement before bank-$03's handler
                // reads it.
                int slot = state.entity.slot();
                if (state.runtime.checkForKeyDropQuicksandHole(state.updated)) {
                    state.runtime.pendingKeyQuicksandEvents.add(new KeyQuicksandEvent(slot));
                    state.runtime.slots[state.index] = withStatus(state.updated, EntityStatus.FALLING);
                    state.runtime.enemyTransitionCountdown[slot] = 0x2F;
                    state.runtime.fallingTargetX[slot] = 0x50;
                    state.runtime.fallingTargetY[slot] = 0x48;
                    state.runtime.fallingSpeedX[slot] = 0;
                    state.runtime.fallingSpeedY[slot] = 0;
                    state.runtime.fallingSpeedXAccumulator[slot] = 0;
                    state.runtime.fallingSpeedYAccumulator[slot] = 0;
                    state.runtime.fallingVisualYOffset[slot] = 0;
                    state.runtime.pendingEntityEvents.add(new EntityCombatEvent(
                        slot, state.runtime.ENTITY_KEY_DROP_POINT, 0, false,
                        EntityCombatEvent.SoundChannel.JINGLE, 0x18));
                    return false;
                }
                boolean hookshotRoom = state.runtime.entityRoomId == 0x80;
                if (hookshotRoom) {
                    state.updated = withDefinition(state.updated, state.runtime.spriteDefinitionFor(state.runtime.ENTITY_HOOKSHOT_CHAIN), 0);
                }
                KeyDropPointMotion.Update keyUpdate = KeyDropPointMotion.advance(
                    state.runtime.enemyTransitionCountdown[slot], state.updated.spriteVariant(), hookshotRoom);
                state.runtime.enemyTransitionCountdown[slot] = keyUpdate.nextTransitionCountdown();
                if (keyUpdate.dialogLowId() >= 0) {
                    state.runtime.pendingDialogRequests.add(new DialogRequest(
                        KeyDropPointMotion.DIALOG_TABLE, keyUpdate.dialogLowId()));
                }
                if (keyUpdate.rewardItemType() >= 0) {
                    state.runtime.pendingKeyRewardEvents.add(new KeyRewardEvent(
                        slot, keyUpdate.rewardItemType()));
                }
                state.keyDropTransitionActive = keyUpdate.holdAboveLink();
                if (state.keyDropTransitionActive) {
                    state.updated = withPositionAndVariant(state.updated, state.linkEntityX,
                        state.linkEntityY - 0x0C, state.updated.spriteVariant());
                    state.updated = withZ(state.updated, state.linkZ);
                }
            }
            if (state.wasInitializing && state.entity.type() == state.runtime.ENTITY_WIZROBE) {
                // EntityInitWizrobe decrements the initial display-list variant
                // before the first active handler dispatch.
                state.updated = withVariant(state.entity, -1);
            }
            if (state.status == EntityStatus.ACTIVE && !state.wasInitializing
                && state.entity.type() == state.runtime.ENTITY_POLS_VOICE
                && state.runtime.polsVoiceBalladOcarinaTrigger()) {
                // The handler checks this before rendering, recoil, movement,
                // or the ordinary collision path. Its private countdown $1F
                // is the shared EntityDeathHandler's visible death timer.
                state.runtime.dyingCountdown[state.entity.slot()] = 0x1F;
                state.runtime.enemyPhysicsFlags[state.entity.slot()] = 0x04;
                state.status = EntityStatus.DYING;
                state.updated = withStatus(state.entity, EntityStatus.DYING);
                state.runtime.pendingEntityEvents.add(new EntityCombatEvent(
                    state.entity.slot(), state.entity.type(), 0, false,
                    EntityCombatEvent.SoundChannel.NOISE, 0x13));
            }
            if (state.status == EntityStatus.ACTIVE && !state.wasInitializing
                && (state.entity.type() == state.runtime.ENTITY_ROLLING_BONES
                    || state.entity.type() == state.runtime.ENTITY_ROLLING_BONES_BAR)) {
                // BossIntro runs before ReturnIfNonInteractive_06.
                if (state.entity.type() == state.runtime.ENTITY_ROLLING_BONES
                    && !state.runtime.roomTransitionActive && !state.runtime.inventoryAppearing) {
                    BossIntroMotion.Update intro = state.runtime.bossIntroMotion.advance(
                        state.runtime.options1(state.entity.slot()), state.entity.type(), state.runtime.entityMapId,
                        state.runtime.transitionSequenceCounter);
                    if (intro.musicTrack() >= 0) state.runtime.pendingMusicTrack = intro.musicTrack();
                }
                // The bar's room-clear check precedes its interaction guard.
                if (state.entity.type() == state.runtime.ENTITY_ROLLING_BONES_BAR && (state.runtime.entityRoomStatus & 0x20) != 0) {
                    state.runtime.disableEntityWithoutPersistence(state.entity.slot());
                    return false;
                }
                // ReturnIfNonInteractive_06 reads the pre-decrement wC1A8.
                if (!state.handlerInteractive || state.countdownBlocksInteraction) return false;
                if (state.entity.type() == state.runtime.ENTITY_ROLLING_BONES
                    && !state.rollingBonesHitThisFrame && state.runtime.enemyIgnoreHitsCountdown[state.entity.slot()] != 0) {
                    // ApplyRecoilIfNeeded_06 pops the handler return address:
                    // no jump physics or AI movement occurs during recoil.
                    state.runtime.slots[state.index] = state.runtime.applyEnemyRecoilIfNeeded(state.entity, state.backgroundCollision).entity();
                    return false;
                }
            }
            if (state.status == EntityStatus.ACTIVE && !state.wasInitializing
                && (usesSharedRecoil(state.entity.type())
                    || (state.entity.type() == state.runtime.ENTITY_POKEY
                        && state.runtime.pokeyMotion.isSegment(state.entity.slot())
                        && state.handlerLinkCollisionEnabled))
                && state.entity.type() != state.runtime.ENTITY_ROLLING_BONES
                && state.entity.type() != state.runtime.ENTITY_MINI_MOLDORM
                && state.entity.type() != state.runtime.ENTITY_BOO_BUDDY
                && (state.entity.type() != state.runtime.ENTITY_MASKED_MIMIC_GORIYA || state.runtime.entityMapId != 0x1F)
                && ((state.entity.type() != state.runtime.ENTITY_STAR && state.entity.type() != state.runtime.ENTITY_BLOOPER)
                    || state.handlerLinkCollisionEnabled)
                && ((state.entity.type() != state.runtime.ENTITY_ZOMBIE && state.entity.type() != state.runtime.ENTITY_BUZZ_BLOB
                    && state.entity.type() != state.runtime.ENTITY_SAND_CRAB)
                    || state.handlerLinkCollisionEnabled)) {
                // Bank-$03 AnimateRoamingEnemy and the bank-$04/$06/$07
                // handlers apply the shared recoil before their own movement.
                EnemyRecoilMotion.Update recoil = state.runtime.applyEnemyRecoilIfNeeded(
                    state.entity, state.backgroundCollision);
                state.entity = recoil.entity();
                state.updated = state.entity;
            }
            if (state.status == EntityStatus.ACTIVE && !state.wasInitializing
                && state.entity.type() == state.runtime.ENTITY_MINI_MOLDORM) {
                int slot = state.entity.slot();
                // RenderMiniMoldorm records the history before the bank-$04
                // recoil helper runs, so keep this ordering explicit.
                state.runtime.miniMoldormMotion.beginFrame(state.entity, state.runtime.enemyIgnoreHitsCountdown[slot] != 0);
                EnemyRecoilMotion.Update recoil = state.runtime.applyEnemyRecoilIfNeeded(
                    state.entity, state.backgroundCollision);
                state.entity = recoil.entity();
                state.updated = state.entity;
                RoomEntityBackgroundInteraction miniBackgroundInteraction = state.runtime.backgroundInteraction;
                if (miniBackgroundInteraction == null && state.backgroundCollision != null) {
                    miniBackgroundInteraction = RoomEntityBackgroundInteraction.fromBoolean(
                        state.backgroundCollision);
                }
                int transitionForMotion = state.runtime.enemyTransitionCountdown[slot];
                if (transitionForMotion != 0) {
                    // The common runtime timer has already performed the ROM
                    // pre-handler decrement; MiniMoldormMotion mirrors that
                    // decrement for its direct source-derived API as well.
                    transitionForMotion = (transitionForMotion + 1) & 0xFF;
                }
                MiniMoldormMotion.Update miniUpdate = state.runtime.miniMoldormMotion.advanceAfterHistory(
                    state.entity, transitionForMotion, state.runtime.enemyIgnoreHitsCountdown[slot],
                    state.runtime.enemyFlashCountdown[slot], miniBackgroundInteraction,
                    state.randomByteSupplier, state.frame);
                state.updated = miniUpdate.entity();
                state.runtime.enemyTransitionCountdown[slot] = miniUpdate.transitionCountdown();
                if (state.runtime.spriteHandlers != null) {
                    state.updated = withDefinition(state.updated,
                        state.runtime.spriteHandlers.forMiniMoldormState(
                            miniUpdate.headSpriteVariant(), state.updated.x(),
                            (state.updated.y() - state.updated.z()) & 0xFF,
                            miniUpdate.segment1X(), miniUpdate.segment1Y(),
                            miniUpdate.segment2X(), miniUpdate.segment2Y()), 0);
                } else if (state.updated.spriteDefinition().supported()
                    && miniUpdate.headSpriteVariant() < state.updated.spriteDefinition().variantCount()) {
                    state.updated = withVariant(state.updated, miniUpdate.headSpriteVariant());
                }
                state.preserveMiniMoldormPresentation = true;
            }
            if (state.status == EntityStatus.ACTIVE && !state.wasInitializing
                && state.entity.type() == state.runtime.ENTITY_MOLDORM) {
                int slot = state.entity.slot();
                if (!state.handlerInteractive || state.countdownBlocksInteraction || state.moldormIntroOpenedThisFrame) {
                    if (state.runtime.spriteHandlers != null) {
                        state.runtime.slots[state.index] = withDefinition(state.entity, state.runtime.spriteHandlers.forMoldormState(
                            state.runtime.moldormMotion.headSpriteVariant(slot), state.entity.x(),
                            (state.entity.y() - state.entity.z()) & 0xFF,
                            state.runtime.moldormMotion.currentTailSegments(slot), 0, state.frame), 0);
                    }
                    if (state.runtime.roomTransitionActive) state.runtime.moldormMotion.resetHistory(slot);
                    return false;
                }
                RoomEntityBackgroundInteraction moldormBackgroundInteraction =
                    state.runtime.backgroundInteraction;
                if (moldormBackgroundInteraction == null && state.backgroundCollision != null) {
                    moldormBackgroundInteraction = RoomEntityBackgroundInteraction.fromBoolean(
                        state.backgroundCollision);
                }
                int transitionForMotion = state.runtime.enemyTransitionCountdown[slot];
                if (transitionForMotion != 0) {
                    // Common countdowns have already ticked before dispatch;
                    // the direct motion API performs the same decrement.
                    transitionForMotion = (transitionForMotion + 1) & 0xFF;
                }
                MoldormMotion.Update moldormUpdate = state.runtime.moldormMotion.advance(
                    state.entity, state.frame, transitionForMotion, state.runtime.enemyIgnoreHitsCountdown[slot],
                    state.runtime.enemyFlashCountdown[slot], state.runtime.enemyHealth[slot], state.randomByteSupplier,
                    moldormBackgroundInteraction);
                state.updated = moldormUpdate.entity();
                state.runtime.enemyTransitionCountdown[slot] = moldormUpdate.transitionCountdown();
                if (moldormUpdate.noiseRequested()) {
                    state.runtime.pendingEntityEvents.add(new EntityCombatEvent(
                        slot, state.entity.type(), 0, false,
                        EntityCombatEvent.SoundChannel.NOISE, 0x1B));
                }
                if (state.runtime.spriteHandlers != null) {
                    MoldormMotion.Presentation presentation = moldormUpdate.presentation();
                    state.updated = withDefinition(state.updated, state.runtime.spriteHandlers.forMoldormPresentation(
                        presentation.headSpriteVariant(), presentation.headX(),
                        presentation.headY(), state.updated.x(),
                        (state.updated.y() - state.updated.z()) & 0xFF,
                        presentation.segments(), 0, state.frame), 0);
                }
                state.runtime.enemyPhysicsFlags[slot] = 0x02;
                state.preserveMoldormPresentation = true;
            }
            if (state.status == EntityStatus.ACTIVE && !state.wasInitializing
                && state.entity.type() == state.runtime.ENTITY_MIMIC) {
                int slot = state.entity.slot();
                RoomEntityBackgroundInteraction mimicBackgroundInteraction = state.runtime.backgroundInteraction;
                if (mimicBackgroundInteraction == null && state.backgroundCollision != null) {
                    mimicBackgroundInteraction = RoomEntityBackgroundInteraction.fromBoolean(
                        state.backgroundCollision);
                }
                MimicMotion.Update mimicUpdate = state.runtime.mimicMotion.advance(
                    state.entity, state.frame, state.runtime.linkPressedButtonsMask, state.romCollisionType,
                    mimicBackgroundInteraction, state.runtime.enemyIgnoreHitsCountdown[slot], state.frame);
                state.updated = mimicUpdate.entity();
                state.preserveMimicPresentation = true;
            }
            if (state.status == EntityStatus.ACTIVE && !state.wasInitializing
                && state.entity.type() == state.runtime.ENTITY_MASKED_MIMIC_GORIYA
                && state.runtime.entityMapId != 0x1F) {
                int slot = state.entity.slot();
                RoomEntityBackgroundInteraction maskedMimicBackgroundInteraction =
                    state.runtime.backgroundInteraction;
                if (maskedMimicBackgroundInteraction == null && state.backgroundCollision != null) {
                    maskedMimicBackgroundInteraction = RoomEntityBackgroundInteraction.fromBoolean(
                        state.backgroundCollision);
                }
                MaskedMimicMotion.Update maskedMimicUpdate = state.runtime.maskedMimicMotion.advance(
                    state.entity, state.linkEntityX, state.linkEntityY, state.romLinkDirection,
                    state.runtime.linkPressedButtonsMask, state.romCollisionType,
                    maskedMimicBackgroundInteraction, state.runtime.enemyIgnoreHitsCountdown[slot], state.frame);
                state.updated = maskedMimicUpdate.entity();
                state.runtime.entityOptions1Override[slot] = maskedMimicUpdate.options1();
                state.preserveMaskedMimicPresentation = true;
            }
            if (state.status == EntityStatus.ACTIVE && !state.wasInitializing
                && state.entity.type() == state.runtime.ENTITY_CRYSTAL_SWITCH) {
                // CrystalSwitchEntityHandler writes $FF before dispatching
                // its normal collision helper, so sword damage can never
                // turn this entity into a dying state.
                state.runtime.enemyHealth[state.entity.slot()] = 0xFF;
                if (state.runtime.enemyFlashCountdown[state.entity.slot()] > 0) {
                    // The handler consumes the collision flash as its trigger.
                    state.runtime.enemyFlashCountdown[state.entity.slot()] = 0;
                    if (!state.runtime.switchBlockAnimationActive) {
                        state.runtime.enemyTransitionCountdown[state.entity.slot()] = 0x18;
                        state.runtime.pendingSwitchBlockAnimationRequest = true;
                        state.runtime.pendingEntityEvents.add(new EntityCombatEvent(
                            state.entity.slot(), state.entity.type(), 0, false,
                            EntityCombatEvent.SoundChannel.WAVE, 0x0E));
                    }
                }
            }
            if (state.wasInitializing && state.entity.type() == state.runtime.ENTITY_LEEVER) {
                // EntityInitLeever calls SetEntitySpriteVariant($FF) before
                // the first active handler dispatch.
                state.updated = withVariant(state.entity, -1);
            }
            if (state.wasInitializing && isSparkType(state.entity.type())) {
                state.updated = state.runtime.sparkMotion.applyInitializationOffset(state.entity);
            }
            if (state.status == EntityStatus.ACTIVE && !state.wasInitializing
                && state.runtime.entityHandlers.hasAdvance(state.entity.type())) {
                state.updated = state.runtime.entityHandlers.advance(state.entity, state.context.withRuntimeInputs(
                    state.frame, state.linkEntityX, state.linkEntityY, state.backgroundCollision));
            }

            if (state.status == EntityStatus.ACTIVE && !state.wasInitializing
                && state.entity.type() == state.runtime.ENTITY_BOMBER) {
                BomberMotion.Update bomberUpdate = state.runtime.bomberMotion.advance(
                    state.entity, state.runtime.enemyTransitionCountdown[state.entity.slot()], state.linkEntityX, state.linkEntityY,
                    state.romLinkDirection, state.swordCollisionActive, state.randomByteSupplier,
                    state.backgroundCollision);
                state.updated = bomberUpdate.entity();
                state.runtime.enemyTransitionCountdown[state.entity.slot()] = bomberUpdate.transitionCountdown();
                if (bomberUpdate.bombSpawn() != null) {
                    BomberMotion.BombSpawn spawn = bomberUpdate.bombSpawn();
                    int bombSlot = state.runtime.spawnEnemyBomb(spawn.x(), spawn.y(), spawn.z(),
                        spawn.transitionCountdown(), spawn.speedX(), spawn.speedY(),
                        spawn.speedZ());
                    if (bombSlot >= 0 && bomberUpdate.playJingle()) {
                        state.runtime.pendingEntityEvents.add(new EntityCombatEvent(
                            bombSlot, state.runtime.ENTITY_BOMB, 0, false,
                            EntityCombatEvent.SoundChannel.JINGLE,
                            state.runtime.BOMBER_THROW_JINGLE_ID));
                    }
                }
            }
            if (state.status == EntityStatus.ACTIVE && !state.wasInitializing
                && state.entity.type() == state.runtime.ENTITY_MAD_BOMBER) {
                // MadBomberEntityHandler rewrites this handler-owned drop
                // byte on every interactive pass before its state dispatch.
                state.runtime.droppedItemBySlot[state.entity.slot()] = 0x3C;
                MadBomberMotion.Update madBomberUpdate = state.runtime.madBomberMotion.advance(
                    state.entity, state.runtime.enemyTransitionCountdown[state.entity.slot()], state.linkEntityX, state.linkEntityY,
                    state.runtime.enemyFlashCountdown[state.entity.slot()], state.randomByteSupplier);
                state.updated = withVariant(madBomberUpdate.entity(),
                    madBomberUpdate.spriteVariant());
                state.preserveMadBomberPresentation = true;
                state.runtime.enemyTransitionCountdown[state.entity.slot()] =
                    madBomberUpdate.transitionCountdown();
                if (madBomberUpdate.bombSpawn() != null) {
                    MadBomberMotion.BombSpawn spawn = madBomberUpdate.bombSpawn();
                    int bombSlot = state.runtime.spawnEnemyBomb(spawn.x(), spawn.y(), spawn.z(),
                        spawn.transitionCountdown(), spawn.speedX(), spawn.speedY(),
                        spawn.speedZ());
                    if (bombSlot >= 0 && madBomberUpdate.playJingle()) {
                        state.runtime.pendingEntityEvents.add(new EntityCombatEvent(
                            bombSlot, state.runtime.ENTITY_BOMB, 0, false,
                            EntityCombatEvent.SoundChannel.JINGLE,
                            state.runtime.BOMBER_THROW_JINGLE_ID));
                    }
                }
            }
        return true;
    }
}
