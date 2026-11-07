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
            if (!state.runtime.entityHandlers.tick(27, state)) return false;
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
            if (!state.runtime.entityHandlers.tick(28, state)) return false;
            if (!state.runtime.entityHandlers.tick(29, state)) return false;
            if (!state.runtime.entityHandlers.tick(30, state)) return false;
            if (!state.runtime.entityHandlers.tick(31, state)) return false;
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
            if (!state.runtime.entityHandlers.tick(32, state)) return false;
            if (!state.runtime.entityHandlers.tick(33, state)) return false;
            if (!state.runtime.entityHandlers.tick(34, state)) return false;
            if (!state.runtime.entityHandlers.tick(35, state)) return false;
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
            if (!state.runtime.entityHandlers.tick(36, state)) return false;
            if (!state.runtime.entityHandlers.tick(37, state)) return false;
            if (!state.runtime.entityHandlers.tick(38, state)) return false;
            if (!state.runtime.entityHandlers.tick(39, state)) return false;
            if (!state.runtime.entityHandlers.tick(40, state)) return false;
            if (!state.runtime.entityHandlers.tick(41, state)) return false;
            if (state.wasInitializing && isSparkType(state.entity.type())) {
                state.updated = state.runtime.sparkMotion.applyInitializationOffset(state.entity);
            }
            if (state.status == EntityStatus.ACTIVE && !state.wasInitializing
                && state.runtime.entityHandlers.hasAdvance(state.entity.type())) {
                state.updated = state.runtime.entityHandlers.advance(state.entity, state.context.withRuntimeInputs(
                    state.frame, state.linkEntityX, state.linkEntityY, state.backgroundCollision));
            }

            if (!state.runtime.entityHandlers.tick(42, state)) return false;
            if (!state.runtime.entityHandlers.tick(43, state)) return false;
        return true;
    }
}
