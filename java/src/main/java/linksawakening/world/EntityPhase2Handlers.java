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

/** Registered ROM type callbacks for ordered slot phase 2. */
final class EntityPhase2Handlers {
    static void install(EntityHandlerRegistry registry) {
        registry.registerTick(RoomEntityRuntime.ENTITY_CHEST_WITH_ITEM, 27, state -> {
if (state.wasInitializing && true) {
                state.runtime.pickups.initializeChestEntity(state.entity);
                state.updated = withPositionAndVariant(state.entity, state.entity.x(),
                    (state.entity.y() - 0x08) & 0xFF, state.entity.spriteVariant());
            }
            return true;
        });
        registry.registerTick(RoomEntityRuntime.ENTITY_ZOMBIE, 28, state -> {
if (state.wasInitializing && true) {
                // The room-loaded Zombie is only a hidden spawn marker; its
                // bank-$18 handler never renders the parent entity.
                state.updated = withVariant(state.entity, -1);
                state.preserveZombiePresentation = true;
            }
            return true;
        });
        registry.registerTick(RoomEntityRuntime.ENTITY_CHEST_WITH_ITEM, 29, state -> {
if (state.status == EntityStatus.ACTIVE && !state.wasInitializing
                && true) {
                if (state.runtime.pickups.advanceChestEntity(state.index, state.entity)) {
                    return false;
                }
                state.updated = state.runtime.slots[state.index];
            }
            return true;
        });
        registry.registerTick(RoomEntityRuntime.ENTITY_DROPPABLE_SECRET_SEASHELL, 30, state -> {
if (state.status == EntityStatus.ACTIVE && !state.wasInitializing
                && true) {
                if (state.runtime.chestSwordLevel >= 0x02
                    || (state.runtime.entityRoomStatus & 0x10) != 0
                    || (state.runtime.entityRoomId == 0xE3 && (state.runtime.entityRoomStatus & 0x40) == 0)) {
                    // DroppableSeashellEntityHandler uses UnloadEntityAndReturn
                    // for these gates, which deliberately does not mark the
                    // source load-order bit as completed.
                    state.runtime.lifecycle.disableEntityWithoutPersistence(state.entity.slot());
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
            return true;
        });
        registry.registerTick(RoomEntityRuntime.ENTITY_HIDING_SLIME_KEY, 31, state -> {
if (state.status == EntityStatus.ACTIVE && !state.wasInitializing
                && true
                && (state.runtime.entityRoomStatus & 0x10) != 0) {
                // HidingSlimeKeyEntityHandler checks ROOM_STATUS_EVENT_1 before
                // calling the shared reveal helper and unloads without touching
                // the source persistence mask.
                state.runtime.lifecycle.disableEntityWithoutPersistence(state.entity.slot());
                return false;
            }
            return true;
        });
        registry.registerTick(RoomEntityRuntime.ENTITY_HIDING_SLIME_KEY, 32, state -> {
if (state.status == EntityStatus.ACTIVE && !state.wasInitializing
                && true) {
                int slot = state.entity.slot();
                SlimeKeyTransitionUpdate keyUpdate = state.runtime.pickups.advanceHidingSlimeKey(
                    state.updated, slot, state.linkEntityX, state.linkEntityY, state.linkZ);
                state.runtime.enemyTransitionCountdown[slot] = keyUpdate.nextTransitionCountdown();
                if (keyUpdate.reward() != null) {
                    state.runtime.pendingSlimeKeyRewardEvents.add(keyUpdate.reward());
                    state.runtime.pendingDialogRequests.add(new DialogRequest(
                        0, keyUpdate.reward().dialogLowId()));
                }
                if (keyUpdate.unload()) {
                    state.runtime.lifecycle.disableEntityWithoutPersistence(slot);
                    return false;
                }
                state.keyDropTransitionActive = keyUpdate.holdAboveLink();
                state.updated = keyUpdate.entity();
            }
            return true;
        });
        registry.registerTick(RoomEntityRuntime.ENTITY_KEY_DROP_POINT, 33, state -> {
if (state.status == EntityStatus.ACTIVE && !state.wasInitializing
                && true) {
                // decrementEnemyStatusCountdowns mirrors UpdateEntityTimers'
                // shared transition-byte decrement before bank-$03's handler
                // reads it.
                int slot = state.entity.slot();
                if (state.runtime.pickups.checkForKeyDropQuicksandHole(state.updated)) {
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
                    state.updated = withDefinition(state.updated, state.runtime.slotProperties.spriteDefinitionFor(state.runtime.ENTITY_HOOKSHOT_CHAIN), 0);
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
            return true;
        });
        registry.registerTick(RoomEntityRuntime.ENTITY_WIZROBE, 34, state -> {
if (state.wasInitializing && true) {
                // EntityInitWizrobe decrements the initial display-list variant
                // before the first active handler dispatch.
                state.updated = withVariant(state.entity, -1);
            }
            return true;
        });
        registry.registerTick(RoomEntityRuntime.ENTITY_POLS_VOICE, 35, state -> {
if (state.status == EntityStatus.ACTIVE && !state.wasInitializing
                && true
                && state.runtime.enemyInspection.polsVoiceBalladOcarinaTrigger()) {
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
            return true;
        });
        registry.registerTick(RoomEntityRuntime.ENTITY_MINI_MOLDORM, 36, state -> {
if (state.status == EntityStatus.ACTIVE && !state.wasInitializing
                && true) {
                int slot = state.entity.slot();
                // RenderMiniMoldorm records the history before the bank-$04
                // recoil helper runs, so keep this ordering explicit.
                state.runtime.miniMoldormMotion.beginFrame(state.entity, state.runtime.enemyIgnoreHitsCountdown[slot] != 0);
                EnemyRecoilMotion.Update recoil = state.runtime.backgroundMechanics.applyEnemyRecoilIfNeeded(
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
            return true;
        });
        registry.registerTick(RoomEntityRuntime.ENTITY_MOLDORM, 37, state -> {
if (state.status == EntityStatus.ACTIVE && !state.wasInitializing
                && true) {
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
            return true;
        });
        registry.registerTick(RoomEntityRuntime.ENTITY_MIMIC, 38, state -> {
if (state.status == EntityStatus.ACTIVE && !state.wasInitializing
                && true) {
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
            return true;
        });
        registry.registerTick(RoomEntityRuntime.ENTITY_MASKED_MIMIC_GORIYA, 39, state -> {
if (state.status == EntityStatus.ACTIVE && !state.wasInitializing
                && true
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
            return true;
        });
        registry.registerTick(RoomEntityRuntime.ENTITY_CRYSTAL_SWITCH, 40, state -> {
if (state.status == EntityStatus.ACTIVE && !state.wasInitializing
                && true) {
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
            return true;
        });
        registry.registerTick(RoomEntityRuntime.ENTITY_LEEVER, 41, state -> {
if (state.wasInitializing && true) {
                // EntityInitLeever calls SetEntitySpriteVariant($FF) before
                // the first active handler dispatch.
                state.updated = withVariant(state.entity, -1);
            }
            return true;
        });
        registry.registerTick(RoomEntityRuntime.ENTITY_BOMBER, 42, state -> {
if (state.status == EntityStatus.ACTIVE && !state.wasInitializing
                && true) {
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
            return true;
        });
        registry.registerTick(RoomEntityRuntime.ENTITY_MAD_BOMBER, 43, state -> {
if (state.status == EntityStatus.ACTIVE && !state.wasInitializing
                && true) {
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
        });
    }
}
