package linksawakening.world.entity;

import linksawakening.world.room.ChestContentsTable;
import linksawakening.world.entity.event.EntityCombatEvent;

import linksawakening.world.entity.event.ChestRewardEvent;
import linksawakening.world.entity.event.DialogRequest;
import linksawakening.world.entity.event.LinkHeldItemPoseRequest;
import linksawakening.world.entity.event.LinkMotionBlockRequest;
import linksawakening.world.entity.event.SlimeKeyRewardEvent;

import static linksawakening.world.entity.EntityMotionMath.*;
import static linksawakening.world.entity.EntitySnapshots.*;
import static linksawakening.world.entity.EntityTypeRules.*;
import linksawakening.entity.EntitySpriteDefinition;
import static linksawakening.world.entity.RoomEntityRuntime.*;

/** ROM pickup, reveal and drop mechanics, sharing the owning room slot table. */
public final class EntityPickupController {
    private final RoomEntityRuntime runtime;
    EntityPickupController(RoomEntityRuntime runtime) { this.runtime = runtime; }

    void initializeCommonDroppable(RoomEntity entity) {
        int slot = entity.slot();
        // EntityInitDiggableBushOrPotDroppable is used by hearts and magic
        // powder directly. The other ordinary drops use the tree/pot init
        // handler, which selects the Pegasus branch outdoors and the buried
        // branch indoors.
        runtime.droppablePrivateState3[slot] = entity.type() == runtime.ENTITY_DROPPABLE_HEART
            || entity.type() == runtime.ENTITY_DROPPABLE_MAGIC_POWDER
            || entity.type() == runtime.ENTITY_HIDING_SLIME_KEY
            || runtime.indoorRoom
            ? runtime.DROPPABLE_HIDDEN_STATE_BURIED
            : runtime.DROPPABLE_HIDDEN_STATE_PEGASUS;
        runtime.droppablePrivateState4[slot] = 0;
        runtime.entityOptions1Override[slot] = entity.type() == runtime.ENTITY_HIDING_SLIME_KEY
            ? runtime.HIDING_SLIME_KEY_HIDDEN_OPTIONS1 : runtime.DROPPABLE_HIDDEN_OPTIONS1;
        }

    public CommonDroppableUpdate advanceCommonDroppable(
            RoomEntity entity, int linkEntityX, int linkEntityY,
            RoomEntityObjectSample objectUnderEntity) {
        int slot = entity.slot();
        int hiddenState = runtime.droppablePrivateState3[slot];
        if (hiddenState != 0) {
            boolean reveal = false;
            if (hiddenState == runtime.DROPPABLE_HIDDEN_STATE_BURIED) {
                // DroppableRevealOrReturnIfNeeded deliberately keeps ordinary
                // buried drops invisible indoors. Outdoors, hearts reveal on
                // short grass or a shovel hole; the remaining shared drops
                // require the shovel-hole path and set privateState4 first.
                if (!runtime.indoorRoom) {
                    int objectId = objectUnderEntity == null
                        ? 0xFF : objectUnderEntity.objectId() & 0xFF;
                    if (entity.type() == runtime.ENTITY_DROPPABLE_HEART) {
                        reveal = objectId == runtime.DROPPABLE_OBJECT_SHORT_GRASS
                            || objectId == runtime.DROPPABLE_OBJECT_SHOVEL_HOLE;
                    } else {
                        runtime.droppablePrivateState4[slot] = 1;
                        reveal = objectId == runtime.DROPPABLE_OBJECT_SHOVEL_HOLE;
                    }
                }
            } else if (hiddenState == runtime.DROPPABLE_HIDDEN_STATE_PEGASUS) {
                reveal = runtime.secretSeashellScreenShakeActive
                    && runtime.secretSeashellPegasusCollisionActive
                    && (!runtime.secretSeashellPegasusCollisionCoordinatesKnown
                        || withinUnsignedWindow(entity.x() + 0x08,
                            runtime.secretSeashellPegasusCollisionX, 0x10)
                        && withinUnsignedWindow(entity.y() + 0x08,
                            runtime.secretSeashellPegasusCollisionY, 0x10));
            }

            if (!reveal) {
                runtime.entityOptions1Override[slot] = entity.type() == runtime.ENTITY_HIDING_SLIME_KEY
                    ? runtime.HIDING_SLIME_KEY_HIDDEN_OPTIONS1 : runtime.DROPPABLE_HIDDEN_OPTIONS1;
                return new CommonDroppableUpdate(withVariant(entity, -1), true);
            }
            revealCommonDroppable(entity, linkEntityX, linkEntityY);
        }

        runtime.entityOptions1Override[slot] = hiddenState == 0
            ? runtime.entityOptions1Override[slot] : runtime.DROPPABLE_REVEALED_OPTIONS1;
        if (entity.type() != runtime.ENTITY_DROPPABLE_FAIRY && !runtime.enemyDropActive[slot]) {
            runtime.enemyDropMotion.initialize(slot, runtime.groundInteractionSideScrolling);
            runtime.enemyDropActive[slot] = true;
        }
        return new CommonDroppableUpdate(
            withVariant(entity, entity.spriteDefinition().supported() ? 0 : -1), false);
        }

    void revealCommonDroppable(RoomEntity entity, int linkEntityX, int linkEntityY) {
        int slot = entity.slot();
        runtime.droppablePrivateState3[slot] = 0;
        runtime.droppablePrivateState4[slot] = 0;
        runtime.dropPrivateCountdown1[slot] = runtime.DROPPABLE_REVEAL_COUNTDOWN;
        runtime.slowTransitionCountdown[slot] = runtime.DROPPABLE_REVEAL_SLOW_COUNTDOWN;
        runtime.slowTimerInitialized[slot] = true;

        if (entity.type() == runtime.ENTITY_DROPPABLE_FAIRY) {
            Vector vector = vectorAwayFromLink(entity.x(), entity.y(),
                linkEntityX, linkEntityY, runtime.DROPPABLE_REVEAL_VECTOR_LENGTH);
            runtime.fairyMotion.setSpeed(slot, vector.x(), vector.y());
            return;
        }

        runtime.enemyDropMotion.initialize(slot, runtime.groundInteractionSideScrolling,
            runtime.DROPPABLE_REVEAL_SPEED_Z);
        runtime.enemyDropMotion.initializeAwayFromLink(slot, entity.x(), entity.y(),
            linkEntityX, linkEntityY, runtime.DROPPABLE_REVEAL_VECTOR_LENGTH);
        runtime.enemyDropActive[slot] = true;
        runtime.entityOptions1Override[slot] = runtime.DROPPABLE_REVEALED_OPTIONS1;
        }

    public SlimeKeyTransitionUpdate advanceHidingSlimeKey(
            RoomEntity entity, int slot, int linkEntityX, int linkEntityY, int linkZ) {
        int transition = runtime.enemyTransitionCountdown[slot];
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
            int leaves = Math.min(0x63, runtime.entityGoldenLeavesCount & 0xFF);
            if (!runtime.indoorRoom && runtime.entityRoomId == runtime.ROOM_OW_POTHOLE_FIELD_SLIME_KEY) {
                leaves = runtime.GOLDEN_LEAVES_FIVE_COUNT;
            }
            leaves = Math.min(0x63, leaves + 1);
            runtime.entityGoldenLeavesCount = leaves;
            int dialogLowId = leaves == runtime.GOLDEN_LEAVES_FINAL_COUNT
                ? runtime.DIALOG_SLIME_KEY
                : leaves == runtime.GOLDEN_LEAVES_FIVE_COUNT
                    ? runtime.DIALOG_FINAL_GOLDEN_LEAF : runtime.DIALOG_GOLDEN_LEAF;
            reward = new SlimeKeyRewardEvent(slot, leaves, dialogLowId);
        }

        RoomEntity held = withPositionAndVariant(entity, linkEntityX,
            linkEntityY - 0x0C, entity.spriteVariant());
        held = withZ(held, linkZ);
        return new SlimeKeyTransitionUpdate(held, nextTransition, true, false, reward);
        }

    void initializeEntityTimers(RoomEntity entity) {
        int slot = entity.slot();
        runtime.enemyPhysicsFlags[slot] = initialPhysicsFlags(entity.type());
        if (isCommonDroppableType(entity.type())) {
            initializeCommonDroppable(entity);
        }
        if (entity.type() == runtime.ENTITY_DROPPABLE_SECRET_SEASHELL) {
            runtime.secretSeashellMotion.ensureInitialized(slot, runtime.entityRoomId);
            runtime.entityOptions1Override[slot] = runtime.SECRET_SEASHELL_HIDDEN_OPTIONS1;
        }
        if (runtime.indoorRoom && RoomEntityPickupRules.usesIndoorDefaultSlowTimer(entity.type())) {
            runtime.slowTransitionCountdown[slot] = 0x80;
            runtime.slowTimerInitialized[slot] = true;
        }
        }

    void resetEnemyDropState(int slot) {
        runtime.droppedItemBySlot[slot] = 0;
        runtime.dropPrivateCountdown1[slot] = 0;
        runtime.dropPrivateCountdown3[slot] = 0;
        runtime.droppablePrivateState3[slot] = 0;
        runtime.droppablePrivateState4[slot] = 0;
        runtime.enemyDropActive[slot] = false;
        runtime.enemyDropMotion.clear(slot);
        runtime.fairyMotion.clear(slot);
        runtime.hookshotChainMotion.clear(slot);
        }

    void decrementEnemyDropCountdowns(RoomEntity entity) {
        int slot = entity.slot();
        if (runtime.dropPrivateCountdown1[slot] > 0) {
            runtime.dropPrivateCountdown1[slot]--;
        }
        if (runtime.dropPrivateCountdown3[slot] > 0) {
            runtime.dropPrivateCountdown3[slot]--;
        }
        }

    public boolean checkForKeyDropQuicksandHole(RoomEntity entity) {
        if (runtime.indoorRoom || runtime.entityRoomId != 0xCE || entity.z() != 0) {
            return false;
        }
        return withinUnsignedWindow(entity.y(), 0x48, 3)
            && withinUnsignedWindow(entity.x(), 0x50, 3);
        }

    public void advanceHeartPiecePickup(RoomEntity entity, int x, int y, int z) {
        int slot = entity.slot();
        var update = runtime.heartPiecePickup[slot].advance(runtime.enemyTransitionCountdown[slot], runtime.linkState.dialogActive, runtime.heartPiecesCount);
        runtime.events.heartPieceUpdates.add(update);
        if (update.incrementPiece()) runtime.heartPiecesCount++;
        if (update.newContainer()) runtime.heartPiecesCount = 0;
        if (update.dialogId() >= 0) runtime.events.pendingDialogRequests.add(new DialogRequest(0, update.dialogId()));
        if (update.newContainer()) runtime.events.pendingEntityEvents.add(new EntityCombatEvent(slot, entity.type(), 0, false,
            EntityCombatEvent.SoundChannel.JINGLE, 0x19));
        if (update.complete()) {
            runtime.lifecycle.disableEntityWithoutPersistence(slot);
            return;
        }
        runtime.events.pendingLinkMotionBlockRequests.add(new LinkMotionBlockRequest(slot));
        runtime.events.pendingLinkHeldItemPoseRequests.add(new LinkHeldItemPoseRequest(slot));
        runtime.slots[slot] = withZ(withPositionAndVariant(entity, x, (y - 12) & 255, entity.spriteVariant()), z);
        }

    public void initializeChestEntity(RoomEntity entity) {
        int slot = entity.slot();
        int itemType = runtime.chestItemBySlot[slot] & 0xFF;
        runtime.chestSpeedY[slot] = 0xFC;
        runtime.chestSpeedYAccumulator[slot] = 0;
        runtime.chestInertia[slot] = 0;
        runtime.events.pendingEntityEvents.add(new EntityCombatEvent(
            slot, runtime.ENTITY_CHEST_WITH_ITEM, 0, false,
            EntityCombatEvent.SoundChannel.NOISE, runtime.CHEST_OPEN_NOISE_ID));
        if (itemType == ChestContentsTable.CHEST_TAIL_KEY) {
            for (RoomEntity candidate : runtime.slots) {
                if (candidate.loaded() && candidate.type() == runtime.ENTITY_OWL_EVENT) {
                    runtime.owlPrivateCountdown1[candidate.slot()] = 0x38;
                    break;
                }
            }
        }
        if (itemType < ChestContentsTable.CHEST_MESSAGE) {
            runtime.events.pendingChestRewardEvents.add(new ChestRewardEvent(slot, itemType));
        }
        }

    public boolean advanceChestEntity(int index, RoomEntity entity) {
        int slot = entity.slot();
        int itemType = runtime.chestItemBySlot[slot] & 0xFF;
        if (itemType == ChestContentsTable.CHEST_ZOL) {
            spawnChestZol(entity);
            runtime.clearEntity(slot);
            return true;
        }

        // ChestWithItemEntityHandler ($07:7C5E) renders before this gate,
        // but does not move, advance inertia, or unload while text is open.
        if (runtime.linkState.dialogActive) {
            return false;
        }

        int y = addFallingSpeedToPosition(entity.y(), runtime.chestSpeedY[slot],
            runtime.chestSpeedYAccumulator, slot);
        int inertia = (runtime.chestInertia[slot] + 1) & 0xFF;
        runtime.chestInertia[slot] = inertia;
        if (inertia == 0x10) {
            runtime.chestSpeedY[slot] = 0;
        }
        if (inertia == 0x08 && runtime.chestContentsTable != null) {
            int soundValue = runtime.chestContentsTable.presentationSoundValue(itemType);
            if (soundValue == runtime.CHEST_TREASURE_JINGLE_ID) {
                runtime.events.pendingEntityEvents.add(new EntityCombatEvent(
                    slot, runtime.ENTITY_CHEST_WITH_ITEM, 0, false,
                    EntityCombatEvent.SoundChannel.JINGLE, runtime.CHEST_TREASURE_JINGLE_ID));
            } else if (soundValue != 0) {
                runtime.pendingMusicTrack = soundValue;
            }
        }
        if (inertia == 0x26 && runtime.chestContentsTable != null) {
            int roomId = runtime.spriteSelection == null ? -1 : runtime.spriteSelection.roomId();
            int dialogLow = runtime.chestContentsTable.dialogLowIdFor(itemType,
                runtime.chestShieldLevel, runtime.chestSwordLevel, runtime.chestPowerBraceletLevel,
                runtime.entityMapId < 0 ? 0 : runtime.entityMapId, roomId < 0 ? 0 : roomId);
            int tableId = itemType == ChestContentsTable.CHEST_MESSAGE && roomId == 0x96
                ? 1 : 0;
            runtime.events.pendingDialogRequests.add(new DialogRequest(tableId, dialogLow));
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
        EntitySpriteDefinition definition = runtime.slotProperties.spriteDefinitionFor(runtime.ENTITY_ZOL);
        int variant = definition.supported() ? definition.initialVariant() : -1;
        RoomEntity zol = new RoomEntity(freeSlot, -1, runtime.ENTITY_ZOL, chest.x(), chest.y(),
            EntityStatus.ACTIVE, definition, variant, 0, 0, 0x06, -1, false);
        runtime.slots[freeSlot] = zol;
        runtime.initializeHealthGroup(freeSlot);
        runtime.zolGelMotion.prepareChestSpawn(freeSlot);
        runtime.enemyPhysicsFlags[freeSlot] = initialPhysicsFlags(runtime.ENTITY_ZOL);
        runtime.enemyHitboxFlags[freeSlot] = 0;
        runtime.enemyHealth[freeSlot] = runtime.initialHealth(runtime.ENTITY_ZOL);
        runtime.enemyIgnoreHitsCountdown[freeSlot] = 1;
        runtime.dynamicEntitySpawnedThisFrame[freeSlot] = true;
        runtime.events.pendingEntityEvents.add(new EntityCombatEvent(
            freeSlot, runtime.ENTITY_ZOL, 0, false,
            EntityCombatEvent.SoundChannel.JINGLE, 0x1D));
        }

    void spawnEnemyDrop(RoomEntity source, int itemType) {
        int freeSlot = runtime.findFreeEntitySlot();
        if (freeSlot < 0) {
            return;
        }

        EntitySpriteDefinition definition = runtime.slotProperties.spriteDefinitionFor(itemType);
        int variant = definition.supported() ? definition.initialVariant() : -1;
        if (itemType == runtime.ENTITY_KEY_DROP_POINT && source.type() == runtime.ENTITY_ARMOS_KNIGHT) {
            variant = 3;
        }
        RoomEntity drop = new RoomEntity(freeSlot, -1, itemType, source.x(), source.y(),
            EntityStatus.ACTIVE, definition, variant, 0, 0, source.z(), -1, false);
        runtime.slots[freeSlot] = drop;
        runtime.initializeHealthGroup(freeSlot);
        resetEnemyDropState(freeSlot);
        runtime.slowTransitionCountdown[freeSlot] = 0x80;
        runtime.slowTimerInitialized[freeSlot] = true;
        runtime.enemyTransitionCountdown[freeSlot] = 0;
        runtime.enemyStunnedCountdown[freeSlot] = 0;
        runtime.dyingCountdown[freeSlot] = 0;
        runtime.powerRecoilDeath[freeSlot] = false;
        runtime.enemyPhysicsFlags[freeSlot] = initialPhysicsFlags(itemType);
        runtime.enemyHealth[freeSlot] = runtime.initialHealth(itemType);
        runtime.enemyFlashCountdown[freeSlot] = 0;
        runtime.enemyIgnoreHitsCountdown[freeSlot] = 1;
        runtime.entityGroundStatus[freeSlot] = 0;
        runtime.baseEntityFlipAttribute[freeSlot] = 0;
        runtime.entityOptions1Override[freeSlot] = -1;
        runtime.enemyRecoilMotion.clear(freeSlot);
        runtime.dropPrivateCountdown1[freeSlot] = 0x18;
        runtime.dropPrivateCountdown3[freeSlot] = 0x03;
        if (itemType == runtime.ENTITY_DROPPABLE_FAIRY) {
            runtime.fairyMotion.initialize(freeSlot);
        } else {
            runtime.enemyDropMotion.initialize(freeSlot, runtime.groundInteractionSideScrolling);
            runtime.enemyDropActive[freeSlot] = true;
        }
        runtime.dynamicEntitySpawnedThisFrame[freeSlot] = true;
        }

    void spawnBossKeyDrop(RoomEntity source, int spriteVariant, int z,
                                  int speedZ, int privateCountdown1) {
        spawnKeyDropAt(runtime.ENTITY_KEY_DROP_POINT, source.x(), source.y(), spriteVariant, z, speedZ,
            privateCountdown1);
        }

    int spawnKeyDropAt(int itemType, int x, int y, int spriteVariant, int z,
                               int speedZ, int privateCountdown1) {
        int freeSlot = runtime.findFreeEntitySlot();
        if (freeSlot < 0) {
            return -1;
        }

        EntitySpriteDefinition definition = runtime.slotProperties.spriteDefinitionFor(itemType);
        int variant = definition.supported()
            ? Math.min(spriteVariant, definition.variantCount() - 1) : -1;
        RoomEntity drop = new RoomEntity(freeSlot, -1, itemType,
            x & 0xFF, y & 0xFF, EntityStatus.ACTIVE, definition, variant,
            0, 0, z, -1, false);
        runtime.slots[freeSlot] = drop;
        runtime.initializeHealthGroup(freeSlot);
        resetEnemyDropState(freeSlot);
        runtime.enemyDropMotion.initialize(freeSlot, runtime.groundInteractionSideScrolling, speedZ);
        runtime.enemyDropActive[freeSlot] = true;
        runtime.slowTransitionCountdown[freeSlot] = 0;
        runtime.slowTimerInitialized[freeSlot] = false;
        runtime.dropPrivateCountdown1[freeSlot] = privateCountdown1;
        runtime.dropPrivateCountdown3[freeSlot] = 0;
        runtime.enemyTransitionCountdown[freeSlot] = 0;
        runtime.enemyStunnedCountdown[freeSlot] = 0;
        runtime.dyingCountdown[freeSlot] = 0;
        runtime.powerRecoilDeath[freeSlot] = false;
        runtime.enemyPhysicsFlags[freeSlot] = initialPhysicsFlags(itemType);
        runtime.enemyHealth[freeSlot] = runtime.initialHealth(itemType);
        runtime.enemyFlashCountdown[freeSlot] = 0;
        runtime.enemyIgnoreHitsCountdown[freeSlot] = 1;
        runtime.entityGroundStatus[freeSlot] = 0;
        runtime.baseEntityFlipAttribute[freeSlot] = 0;
        runtime.entityOptions1Override[freeSlot] = -1;
        runtime.enemyRecoilMotion.clear(freeSlot);
        runtime.dynamicEntitySpawnedThisFrame[freeSlot] = true;
        return freeSlot;
        }
  public boolean hasHeartPiecePickup(int slot) { return runtime.heartPiecePickup[slot] != null; }

}
