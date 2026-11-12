package linksawakening.world.entity;

import static linksawakening.world.entity.EntityMotionMath.*;
import static linksawakening.world.entity.EntityRomConstants.*;
import static linksawakening.world.entity.EntitySnapshots.*;
import static linksawakening.world.entity.EntityTypeRules.*;

import linksawakening.entity.EntitySpriteDefinition;
import linksawakening.world.entity.RoomEntityRuntime.CommonDroppableUpdate;
import linksawakening.world.entity.RoomEntityRuntime.SlimeKeyTransitionUpdate;
import linksawakening.world.entity.event.ChestRewardEvent;
import linksawakening.world.entity.event.DialogRequest;
import linksawakening.world.entity.event.EntityCombatEvent;
import linksawakening.world.entity.event.LinkHeldItemPoseRequest;
import linksawakening.world.entity.event.LinkMotionBlockRequest;
import linksawakening.world.entity.event.SlimeKeyRewardEvent;
import linksawakening.world.room.ChestContentsTable;

/** ROM pickup, reveal and drop mechanics, sharing the owning room slot table. */
public final class EntityPickupController {
    private final RoomEntityRuntime runtime;

    EntityPickupController(RoomEntityRuntime runtime) {
        this.runtime = runtime;
    }

    void initializeCommonDroppable(RoomEntity entity) {
        int slot = entity.slot();
        // EntityInitDiggableBushOrPotDroppable is used by hearts and magic
        // powder directly. The other ordinary drops use the tree/pot init
        // handler, which selects the Pegasus branch outdoors and the buried
        // branch indoors.
        runtime.entityState.droppablePrivateState3[slot] = entity.type() == EntityRomConstants.ENTITY_DROPPABLE_HEART
                        || entity.type() == EntityRomConstants.ENTITY_DROPPABLE_MAGIC_POWDER
                        || entity.type() == EntityRomConstants.ENTITY_HIDING_SLIME_KEY
                        || runtime.indoorRoom
                ? EntityRomConstants.DROPPABLE_HIDDEN_STATE_BURIED
                : EntityRomConstants.DROPPABLE_HIDDEN_STATE_PEGASUS;
        runtime.entityState.droppablePrivateState4[slot] = 0;
        runtime.entityState.entityOptions1Override[slot] = entity.type() == EntityRomConstants.ENTITY_HIDING_SLIME_KEY
                ? EntityRomConstants.HIDING_SLIME_KEY_HIDDEN_OPTIONS1
                : EntityRomConstants.DROPPABLE_HIDDEN_OPTIONS1;
    }

    public CommonDroppableUpdate advanceCommonDroppable(
            RoomEntity entity, int linkEntityX, int linkEntityY, RoomEntityObjectSample objectUnderEntity) {
        int slot = entity.slot();
        int hiddenState = runtime.entityState.droppablePrivateState3[slot];
        if (hiddenState != 0) {
            boolean reveal = false;
            if (hiddenState == EntityRomConstants.DROPPABLE_HIDDEN_STATE_BURIED) {
                // DroppableRevealOrReturnIfNeeded deliberately keeps ordinary
                // buried drops invisible indoors. Outdoors, hearts reveal on
                // short grass or a shovel hole; the remaining shared drops
                // require the shovel-hole path and set privateState4 first.
                if (!runtime.indoorRoom) {
                    int objectId = objectUnderEntity == null ? 0xFF : objectUnderEntity.objectId() & 0xFF;
                    if (entity.type() == EntityRomConstants.ENTITY_DROPPABLE_HEART) {
                        reveal = objectId == EntityRomConstants.DROPPABLE_OBJECT_SHORT_GRASS
                                || objectId == EntityRomConstants.DROPPABLE_OBJECT_SHOVEL_HOLE;
                    } else {
                        runtime.entityState.droppablePrivateState4[slot] = 1;
                        reveal = objectId == EntityRomConstants.DROPPABLE_OBJECT_SHOVEL_HOLE;
                    }
                }
            } else if (hiddenState == EntityRomConstants.DROPPABLE_HIDDEN_STATE_PEGASUS) {
                reveal = runtime.entityState.secretSeashellScreenShakeActive
                        && runtime.entityState.secretSeashellPegasusCollisionActive
                        && (!runtime.entityState.secretSeashellPegasusCollisionCoordinatesKnown
                                || withinUnsignedWindow(
                                                entity.x() + 0x08,
                                                runtime.entityState.secretSeashellPegasusCollisionX,
                                                0x10)
                                        && withinUnsignedWindow(
                                                entity.y() + 0x08,
                                                runtime.entityState.secretSeashellPegasusCollisionY,
                                                0x10));
            }

            if (!reveal) {
                runtime.entityState.entityOptions1Override[slot] = entity.type()
                                == EntityRomConstants.ENTITY_HIDING_SLIME_KEY
                        ? EntityRomConstants.HIDING_SLIME_KEY_HIDDEN_OPTIONS1
                        : EntityRomConstants.DROPPABLE_HIDDEN_OPTIONS1;
                return new CommonDroppableUpdate(withVariant(entity, -1), true);
            }
            revealCommonDroppable(entity, linkEntityX, linkEntityY);
            // $629A falls through to .remainInvisible ($629C): POP AF removes
            // the caller return address even on the reveal pass. Drawing,
            // collection and BouncingEntityPhysics start next frame.
            return new CommonDroppableUpdate(withVariant(entity, -1), true);
        }

        runtime.entityState.entityOptions1Override[slot] = hiddenState == 0
                ? runtime.entityState.entityOptions1Override[slot]
                : EntityRomConstants.DROPPABLE_REVEALED_OPTIONS1;
        if (entity.type() != EntityRomConstants.ENTITY_DROPPABLE_FAIRY && !runtime.entityState.enemyDropActive[slot]) {
            runtime.entityState.enemyDropMotion.initialize(slot, runtime.entityState.groundInteractionSideScrolling);
            runtime.entityState.enemyDropActive[slot] = true;
        }
        return new CommonDroppableUpdate(
                withVariant(entity, entity.spriteDefinition().supported() ? 0 : -1), false);
    }

    void revealCommonDroppable(RoomEntity entity, int linkEntityX, int linkEntityY) {
        int slot = entity.slot();
        runtime.entityState.droppablePrivateState3[slot] = 0;
        runtime.entityState.droppablePrivateState4[slot] = 0;
        runtime.entityState.dropPrivateCountdown1[slot] = EntityRomConstants.DROPPABLE_REVEAL_COUNTDOWN;
        runtime.entityState.slowTransitionCountdown[slot] = EntityRomConstants.DROPPABLE_REVEAL_SLOW_COUNTDOWN;
        runtime.entityState.slowTimerInitialized[slot] = true;

        runtime.entityState.entityOptions1Override[slot] = EntityRomConstants.DROPPABLE_REVEALED_OPTIONS1;
        if (entity.type() == EntityRomConstants.ENTITY_DROPPABLE_FAIRY) {
            Vector vector = vectorAwayFromLink(
                    entity.x(),
                    entity.y(),
                    linkEntityX,
                    linkEntityY,
                    EntityRomConstants.DROPPABLE_REVEAL_VECTOR_LENGTH);
            runtime.entityState.fairyMotion.setSpeed(slot, vector.x(), vector.y());
            return;
        }

        runtime.entityState.enemyDropMotion.initialize(
                slot, runtime.entityState.groundInteractionSideScrolling, EntityRomConstants.DROPPABLE_REVEAL_SPEED_Z);
        runtime.entityState.enemyDropMotion.initializeAwayFromLink(
                slot,
                entity.x(),
                entity.y(),
                linkEntityX,
                linkEntityY,
                EntityRomConstants.DROPPABLE_REVEAL_VECTOR_LENGTH);
        runtime.entityState.enemyDropActive[slot] = true;
        runtime.entityState.entityOptions1Override[slot] = EntityRomConstants.DROPPABLE_REVEALED_OPTIONS1;
    }

    public SlimeKeyTransitionUpdate advanceHidingSlimeKey(
            RoomEntity entity, int slot, int linkEntityX, int linkEntityY, int linkZ) {
        int transition = runtime.entityState.enemyTransitionCountdown[slot];
        if (transition == 0) {
            return new SlimeKeyTransitionUpdate(entity, 0, false, false, null);
        }
        if (transition == 1) {
            // The handler decrements A locally for this branch and then takes
            // UnloadEntityAndReturn; the transition table byte itself is not
            // observed again because the entity is gone.
            return new SlimeKeyTransitionUpdate(entity, transition, false, true, null);
        }

        SlimeKeyRewardEvent reward = null;
        int nextTransition = transition;
        if (transition == 0x10) {
            nextTransition = 0x0F;
            int leaves = Math.min(0x63, runtime.entityState.entityGoldenLeavesCount & 0xFF);
            if (!runtime.indoorRoom
                    && runtime.entityState.entityRoomId == EntityRomConstants.ROOM_OW_POTHOLE_FIELD_SLIME_KEY) {
                leaves = EntityRomConstants.GOLDEN_LEAVES_FIVE_COUNT;
            }
            leaves = Math.min(0x63, leaves + 1);
            runtime.entityState.entityGoldenLeavesCount = leaves;
            int dialogLowId = leaves == EntityRomConstants.GOLDEN_LEAVES_FINAL_COUNT
                    ? EntityRomConstants.DIALOG_SLIME_KEY
                    : leaves == EntityRomConstants.GOLDEN_LEAVES_FIVE_COUNT
                            ? EntityRomConstants.DIALOG_FINAL_GOLDEN_LEAF
                            : EntityRomConstants.DIALOG_GOLDEN_LEAF;
            reward = new SlimeKeyRewardEvent(slot, leaves, dialogLowId);
        }

        RoomEntity held = withPositionAndVariant(entity, linkEntityX, linkEntityY - 0x0C, entity.spriteVariant());
        held = withZ(held, linkZ);
        return new SlimeKeyTransitionUpdate(held, nextTransition, true, false, reward);
    }

    void initializeEntityTimers(RoomEntity entity) {
        int slot = entity.slot();
        runtime.entityState.enemyPhysicsFlags[slot] = initialPhysicsFlags(entity.type());
        if (isCommonDroppableType(entity.type())) {
            initializeCommonDroppable(entity);
        }
        if (entity.type() == EntityRomConstants.ENTITY_DROPPABLE_SECRET_SEASHELL) {
            runtime.entityState.secretSeashellMotion.ensureInitialized(slot, runtime.entityState.entityRoomId);
            runtime.entityState.entityOptions1Override[slot] = EntityRomConstants.SECRET_SEASHELL_HIDDEN_OPTIONS1;
        }
        if (runtime.indoorRoom && RoomEntityPickupRules.usesIndoorDefaultSlowTimer(entity.type())) {
            runtime.entityState.slowTransitionCountdown[slot] = 0x80;
            runtime.entityState.slowTimerInitialized[slot] = true;
        }
    }

    void resetEnemyDropState(int slot) {
        runtime.entityState.droppedItemBySlot[slot] = 0;
        runtime.entityState.dropPrivateCountdown1[slot] = 0;
        runtime.entityState.dropPrivateCountdown3[slot] = 0;
        runtime.entityState.droppablePrivateState3[slot] = 0;
        runtime.entityState.droppablePrivateState4[slot] = 0;
        runtime.entityState.enemyDropActive[slot] = false;
        runtime.entityState.enemyDropMotion.clear(slot);
        runtime.entityState.fairyMotion.clear(slot);
        runtime.entityState.hookshotChainMotion.clear(slot);
    }

    void decrementEnemyDropCountdowns(RoomEntity entity) {
        int slot = entity.slot();
        if (runtime.entityState.dropPrivateCountdown1[slot] > 0) {
            runtime.entityState.dropPrivateCountdown1[slot]--;
        }
        if (runtime.entityState.dropPrivateCountdown3[slot] > 0) {
            runtime.entityState.dropPrivateCountdown3[slot]--;
        }
    }

    public boolean checkForKeyDropQuicksandHole(RoomEntity entity) {
        if (runtime.indoorRoom || runtime.entityState.entityRoomId != 0xCE || entity.z() != 0) {
            return false;
        }
        return withinUnsignedWindow(entity.y(), 0x48, 3) && withinUnsignedWindow(entity.x(), 0x50, 3);
    }

    public void advanceHeartPiecePickup(RoomEntity entity, int x, int y, int z) {
        int slot = entity.slot();
        var update = runtime.entityState.heartPiecePickup[slot].advance(
                runtime.entityState.enemyTransitionCountdown[slot],
                runtime.entityState.linkState.dialogActive,
                runtime.entityState.heartPiecesCount);
        runtime.entityState.events.heartPieceUpdates.add(update);
        if (update.incrementPiece()) runtime.entityState.heartPiecesCount++;
        if (update.newContainer()) runtime.entityState.heartPiecesCount = 0;
        if (update.dialogId() >= 0)
            runtime.entityState.events.pendingDialogRequests.add(new DialogRequest(0, update.dialogId()));
        if (update.newContainer())
            runtime.entityState.events.pendingEntityEvents.add(
                    new EntityCombatEvent(slot, entity.type(), 0, false, EntityCombatEvent.SoundChannel.JINGLE, 0x19));
        if (update.complete()) {
            runtime.lifecycle.disableEntityWithoutPersistence(slot);
            return;
        }
        runtime.entityState.events.pendingLinkMotionBlockRequests.add(new LinkMotionBlockRequest(slot));
        runtime.entityState.events.pendingLinkHeldItemPoseRequests.add(new LinkHeldItemPoseRequest(slot));
        runtime.slots[slot] = withZ(withPositionAndVariant(entity, x, (y - 12) & 255, entity.spriteVariant()), z);
    }

    public void initializeChestEntity(RoomEntity entity) {
        int slot = entity.slot();
        int itemType = runtime.entityState.chestItemBySlot[slot] & 0xFF;
        runtime.entityState.chestSpeedY[slot] = 0xFC;
        runtime.entityState.chestSpeedYAccumulator[slot] = 0;
        runtime.entityState.chestInertia[slot] = 0;
        runtime.entityState.events.pendingEntityEvents.add(new EntityCombatEvent(
                slot,
                EntityRomConstants.ENTITY_CHEST_WITH_ITEM,
                0,
                false,
                EntityCombatEvent.SoundChannel.NOISE,
                EntityRomConstants.CHEST_OPEN_NOISE_ID));
        if (itemType == ChestContentsTable.CHEST_TAIL_KEY) {
            for (RoomEntity candidate : runtime.slots) {
                if (candidate.loaded() && candidate.type() == EntityRomConstants.ENTITY_OWL_EVENT) {
                    runtime.entityState.owlPrivateCountdown1[candidate.slot()] = 0x38;
                    break;
                }
            }
        }
        if (itemType < ChestContentsTable.CHEST_MESSAGE) {
            runtime.entityState.events.pendingChestRewardEvents.add(new ChestRewardEvent(slot, itemType));
        }
    }

    public boolean advanceChestEntity(int index, RoomEntity entity) {
        int slot = entity.slot();
        int itemType = runtime.entityState.chestItemBySlot[slot] & 0xFF;
        if (itemType == ChestContentsTable.CHEST_ZOL) {
            spawnChestZol(entity);
            runtime.clearEntity(slot);
            return true;
        }

        // ChestWithItemEntityHandler ($07:7C5E) renders before this gate,
        // but does not move, advance inertia, or unload while text is open.
        if (runtime.entityState.linkState.dialogActive) {
            return false;
        }

        int y = addFallingSpeedToPosition(
                entity.y(), runtime.entityState.chestSpeedY[slot], runtime.entityState.chestSpeedYAccumulator, slot);
        int inertia = (runtime.entityState.chestInertia[slot] + 1) & 0xFF;
        runtime.entityState.chestInertia[slot] = inertia;
        if (inertia == 0x10) {
            runtime.entityState.chestSpeedY[slot] = 0;
        }
        if (inertia == 0x08 && runtime.chestContentsTable != null) {
            int soundValue = runtime.chestContentsTable.presentationSoundValue(itemType);
            if (soundValue == EntityRomConstants.CHEST_TREASURE_JINGLE_ID) {
                runtime.entityState.events.pendingEntityEvents.add(new EntityCombatEvent(
                        slot,
                        EntityRomConstants.ENTITY_CHEST_WITH_ITEM,
                        0,
                        false,
                        EntityCombatEvent.SoundChannel.JINGLE,
                        EntityRomConstants.CHEST_TREASURE_JINGLE_ID));
            } else if (soundValue != 0) {
                runtime.entityState.pendingMusicTrack = soundValue;
            }
        }
        if (inertia == 0x26 && runtime.chestContentsTable != null) {
            int roomId =
                    runtime.entityState.spriteSelection == null ? -1 : runtime.entityState.spriteSelection.roomId();
            int dialogLow = runtime.chestContentsTable.dialogLowIdFor(
                    itemType,
                    runtime.entityState.chestShieldLevel,
                    runtime.entityState.chestSwordLevel,
                    runtime.entityState.chestPowerBraceletLevel,
                    runtime.entityState.entityMapId < 0 ? 0 : runtime.entityState.entityMapId,
                    roomId < 0 ? 0 : roomId);
            int tableId = itemType == ChestContentsTable.CHEST_MESSAGE && roomId == 0x96 ? 1 : 0;
            runtime.entityState.events.pendingDialogRequests.add(new DialogRequest(tableId, dialogLow));
        }
        if (inertia == 0x28) {
            runtime.clearEntity(slot);
            return true;
        }
        runtime.slots[index] = withPositionAndVariant(entity, entity.x(), y, itemType);
        return false;
    }

    void spawnChestZol(RoomEntity chest) {
        int freeSlot = runtime.findFreeEntitySlot();
        if (freeSlot < 0) {
            return;
        }
        EntitySpriteDefinition definition = runtime.slotProperties.spriteDefinitionFor(EntityRomConstants.ENTITY_ZOL);
        int variant = definition.supported() ? definition.initialVariant() : -1;
        RoomEntity zol = new RoomEntity(
                freeSlot,
                -1,
                EntityRomConstants.ENTITY_ZOL,
                chest.x(),
                chest.y(),
                EntityStatus.ACTIVE,
                definition,
                variant,
                0,
                0,
                0x06,
                -1,
                false);
        runtime.slots[freeSlot] = zol;
        runtime.initializeHealthGroup(freeSlot);
        runtime.entityState.zolGelMotion.prepareChestSpawn(freeSlot);
        runtime.entityState.enemyPhysicsFlags[freeSlot] = initialPhysicsFlags(EntityRomConstants.ENTITY_ZOL);
        runtime.entityState.enemyHitboxFlags[freeSlot] = 0;
        runtime.entityState.enemyHealth[freeSlot] = runtime.initialHealth(EntityRomConstants.ENTITY_ZOL);
        runtime.entityState.enemyIgnoreHitsCountdown[freeSlot] = 1;
        runtime.entityState.dynamicEntitySpawnedThisFrame[freeSlot] = true;
        runtime.entityState.events.pendingEntityEvents.add(new EntityCombatEvent(
                freeSlot, EntityRomConstants.ENTITY_ZOL, 0, false, EntityCombatEvent.SoundChannel.JINGLE, 0x1D));
    }

    void spawnEnemyDrop(RoomEntity source, int itemType) {
        int freeSlot = runtime.findFreeEntitySlot();
        if (freeSlot < 0) {
            return;
        }

        EntitySpriteDefinition definition = runtime.slotProperties.spriteDefinitionFor(itemType);
        int variant = definition.supported() ? definition.initialVariant() : -1;
        if (itemType == EntityRomConstants.ENTITY_KEY_DROP_POINT
                && source.type() == EntityRomConstants.ENTITY_ARMOS_KNIGHT) {
            variant = 3;
        }
        RoomEntity drop = new RoomEntity(
                freeSlot,
                -1,
                itemType,
                source.x(),
                source.y(),
                EntityStatus.ACTIVE,
                definition,
                variant,
                0,
                0,
                source.z(),
                -1,
                false);
        runtime.slots[freeSlot] = drop;
        runtime.initializeHealthGroup(freeSlot);
        resetEnemyDropState(freeSlot);
        runtime.entityState.slowTransitionCountdown[freeSlot] = 0x80;
        runtime.entityState.slowTimerInitialized[freeSlot] = true;
        runtime.entityState.enemyTransitionCountdown[freeSlot] = 0;
        runtime.entityState.enemyStunnedCountdown[freeSlot] = 0;
        runtime.entityState.dyingCountdown[freeSlot] = 0;
        runtime.entityState.powerRecoilDeath[freeSlot] = false;
        runtime.entityState.enemyPhysicsFlags[freeSlot] = initialPhysicsFlags(itemType);
        runtime.entityState.enemyHealth[freeSlot] = runtime.initialHealth(itemType);
        runtime.entityState.enemyFlashCountdown[freeSlot] = 0;
        runtime.entityState.enemyIgnoreHitsCountdown[freeSlot] = 1;
        runtime.entityState.entityGroundStatus[freeSlot] = 0;
        runtime.entityState.baseEntityFlipAttribute[freeSlot] = 0;
        runtime.entityState.entityOptions1Override[freeSlot] = -1;
        runtime.entityState.enemyRecoilMotion.clear(freeSlot);
        runtime.entityState.dropPrivateCountdown1[freeSlot] = 0x18;
        runtime.entityState.dropPrivateCountdown3[freeSlot] = 0x03;
        if (itemType == EntityRomConstants.ENTITY_DROPPABLE_FAIRY) {
            runtime.entityState.fairyMotion.initialize(freeSlot);
        } else {
            runtime.entityState.enemyDropMotion.initialize(
                    freeSlot, runtime.entityState.groundInteractionSideScrolling);
            runtime.entityState.enemyDropActive[freeSlot] = true;
        }
        runtime.entityState.dynamicEntitySpawnedThisFrame[freeSlot] = true;
    }

    void spawnBossKeyDrop(RoomEntity source, int spriteVariant, int z, int speedZ, int privateCountdown1) {
        spawnKeyDropAt(
                EntityRomConstants.ENTITY_KEY_DROP_POINT,
                source.x(),
                source.y(),
                spriteVariant,
                z,
                speedZ,
                privateCountdown1);
    }

    int spawnKeyDropAt(int itemType, int x, int y, int spriteVariant, int z, int speedZ, int privateCountdown1) {
        int freeSlot = runtime.findFreeEntitySlot();
        if (freeSlot < 0) {
            return -1;
        }

        EntitySpriteDefinition definition = runtime.slotProperties.spriteDefinitionFor(itemType);
        int variant = definition.supported() ? Math.min(spriteVariant, definition.variantCount() - 1) : -1;
        RoomEntity drop = new RoomEntity(
                freeSlot,
                -1,
                itemType,
                x & 0xFF,
                y & 0xFF,
                EntityStatus.ACTIVE,
                definition,
                variant,
                0,
                0,
                z,
                -1,
                false);
        runtime.slots[freeSlot] = drop;
        runtime.initializeHealthGroup(freeSlot);
        resetEnemyDropState(freeSlot);
        runtime.entityState.enemyDropMotion.initialize(
                freeSlot, runtime.entityState.groundInteractionSideScrolling, speedZ);
        runtime.entityState.enemyDropActive[freeSlot] = true;
        runtime.entityState.slowTransitionCountdown[freeSlot] = 0;
        runtime.entityState.slowTimerInitialized[freeSlot] = false;
        runtime.entityState.dropPrivateCountdown1[freeSlot] = privateCountdown1;
        runtime.entityState.dropPrivateCountdown3[freeSlot] = 0;
        runtime.entityState.enemyTransitionCountdown[freeSlot] = 0;
        runtime.entityState.enemyStunnedCountdown[freeSlot] = 0;
        runtime.entityState.dyingCountdown[freeSlot] = 0;
        runtime.entityState.powerRecoilDeath[freeSlot] = false;
        runtime.entityState.enemyPhysicsFlags[freeSlot] = initialPhysicsFlags(itemType);
        runtime.entityState.enemyHealth[freeSlot] = runtime.initialHealth(itemType);
        runtime.entityState.enemyFlashCountdown[freeSlot] = 0;
        runtime.entityState.enemyIgnoreHitsCountdown[freeSlot] = 1;
        runtime.entityState.entityGroundStatus[freeSlot] = 0;
        runtime.entityState.baseEntityFlipAttribute[freeSlot] = 0;
        runtime.entityState.entityOptions1Override[freeSlot] = -1;
        runtime.entityState.enemyRecoilMotion.clear(freeSlot);
        runtime.entityState.dynamicEntitySpawnedThisFrame[freeSlot] = true;
        return freeSlot;
    }

    public boolean hasHeartPiecePickup(int slot) {
        return runtime.entityState.heartPiecePickup[slot] != null;
    }
}
