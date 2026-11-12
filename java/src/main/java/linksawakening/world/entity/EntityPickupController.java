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
import linksawakening.world.entity.motion.HeartPiecePickupMotion;
import linksawakening.world.room.ChestContentsTable;

/** ROM pickup, reveal and drop mechanics, sharing the owning room slot table. */
public final class EntityPickupController {
    private final int[] chestSpeedY = new int[EntityRoomLoader.MAX_ENTITIES];
    private final int[] chestSpeedYAccumulator = new int[EntityRoomLoader.MAX_ENTITIES];
    private final int[] chestInertia = new int[EntityRoomLoader.MAX_ENTITIES];
    private final int[] chestItemBySlot = new int[EntityRoomLoader.MAX_ENTITIES];
    private int chestShieldLevel = 1;
    private int chestSwordLevel = 1;
    private boolean chestPlayerLevelsKnown;
    private int chestPowerBraceletLevel = 1;
    private final HeartPiecePickupMotion[] heartPiecePickup = new HeartPiecePickupMotion[EntityRoomLoader.MAX_ENTITIES];
    private int heartPiecesCount;
    private final int[] heartPieceMeterPairs = new int[20];
    private final EntityPickupControllerServices runtime;

    EntityPickupController(EntityPickupControllerServices runtime) {
        this.runtime = runtime;
    }

    void initializeCommonDroppable(RoomEntity entity) {
        int slot = entity.slot();
        // EntityInitDiggableBushOrPotDroppable is used by hearts and magic
        // powder directly. The other ordinary drops use the tree/pot init
        // handler, which selects the Pegasus branch outdoors and the buried
        // branch indoors.
        runtime.bossRewards()
                .writeDroppablePrivateState3(
                        slot,
                        entity.type() == EntityRomConstants.ENTITY_DROPPABLE_HEART
                                        || entity.type() == EntityRomConstants.ENTITY_DROPPABLE_MAGIC_POWDER
                                        || entity.type() == EntityRomConstants.ENTITY_HIDING_SLIME_KEY
                                        || runtime.indoorRoom()
                                ? EntityRomConstants.DROPPABLE_HIDDEN_STATE_BURIED
                                : EntityRomConstants.DROPPABLE_HIDDEN_STATE_PEGASUS);
        runtime.bossRewards().writeDroppablePrivateState4(slot, 0);
        runtime.physicsTables()
                .writeEntityOptions1Override(
                        slot,
                        entity.type() == EntityRomConstants.ENTITY_HIDING_SLIME_KEY
                                ? EntityRomConstants.HIDING_SLIME_KEY_HIDDEN_OPTIONS1
                                : EntityRomConstants.DROPPABLE_HIDDEN_OPTIONS1);
    }

    public CommonDroppableUpdate advanceCommonDroppable(
            RoomEntity entity, int linkEntityX, int linkEntityY, RoomEntityObjectSample objectUnderEntity) {
        int slot = entity.slot();
        int hiddenState = runtime.bossRewards().readDroppablePrivateState3(slot);
        if (hiddenState != 0) {
            boolean reveal = false;
            if (hiddenState == EntityRomConstants.DROPPABLE_HIDDEN_STATE_BURIED) {
                // DroppableRevealOrReturnIfNeeded deliberately keeps ordinary
                // buried drops invisible indoors. Outdoors, hearts reveal on
                // short grass or a shovel hole; the remaining shared drops
                // require the shovel-hole path and set privateState4 first.
                if (!runtime.indoorRoom()) {
                    int objectId = objectUnderEntity == null ? 0xFF : objectUnderEntity.objectId() & 0xFF;
                    if (entity.type() == EntityRomConstants.ENTITY_DROPPABLE_HEART) {
                        reveal = objectId == EntityRomConstants.DROPPABLE_OBJECT_SHORT_GRASS
                                || objectId == EntityRomConstants.DROPPABLE_OBJECT_SHOVEL_HOLE;
                    } else {
                        runtime.bossRewards().writeDroppablePrivateState4(slot, 1);
                        reveal = objectId == EntityRomConstants.DROPPABLE_OBJECT_SHOVEL_HOLE;
                    }
                }
            } else if (hiddenState == EntityRomConstants.DROPPABLE_HIDDEN_STATE_PEGASUS) {
                reveal = runtime.motionSet().secretSeashellMotion.readSecretSeashellScreenShakeActive()
                        && runtime.motionSet().secretSeashellMotion.readSecretSeashellPegasusCollisionActive()
                        && (!runtime.motionSet()
                                        .secretSeashellMotion
                                        .readSecretSeashellPegasusCollisionCoordinatesKnown()
                                || withinUnsignedWindow(
                                                entity.x() + 0x08,
                                                runtime.motionSet()
                                                        .secretSeashellMotion
                                                        .readSecretSeashellPegasusCollisionX(),
                                                0x10)
                                        && withinUnsignedWindow(
                                                entity.y() + 0x08,
                                                runtime.motionSet()
                                                        .secretSeashellMotion
                                                        .readSecretSeashellPegasusCollisionY(),
                                                0x10));
            }

            if (!reveal) {
                runtime.physicsTables()
                        .writeEntityOptions1Override(
                                slot,
                                entity.type() == EntityRomConstants.ENTITY_HIDING_SLIME_KEY
                                        ? EntityRomConstants.HIDING_SLIME_KEY_HIDDEN_OPTIONS1
                                        : EntityRomConstants.DROPPABLE_HIDDEN_OPTIONS1);
                return new CommonDroppableUpdate(withVariant(entity, -1), true);
            }
            revealCommonDroppable(entity, linkEntityX, linkEntityY);
            // $629A falls through to .remainInvisible ($629C): POP AF removes
            // the caller return address even on the reveal pass. Drawing,
            // collection and BouncingEntityPhysics start next frame.
            return new CommonDroppableUpdate(withVariant(entity, -1), true);
        }

        runtime.physicsTables()
                .writeEntityOptions1Override(
                        slot,
                        hiddenState == 0
                                ? runtime.physicsTables().readEntityOptions1Override(slot)
                                : EntityRomConstants.DROPPABLE_REVEALED_OPTIONS1);
        if (entity.type() != EntityRomConstants.ENTITY_DROPPABLE_FAIRY
                && !runtime.bossRewards().readEnemyDropActive(slot)) {
            runtime.motionSet()
                    .enemyDropMotion
                    .initialize(slot, runtime.physicsTables().readGroundInteractionSideScrolling());
            runtime.bossRewards().writeEnemyDropActive(slot, true);
        }
        return new CommonDroppableUpdate(
                withVariant(entity, entity.spriteDefinition().supported() ? 0 : -1), false);
    }

    void revealCommonDroppable(RoomEntity entity, int linkEntityX, int linkEntityY) {
        int slot = entity.slot();
        runtime.bossRewards().writeDroppablePrivateState3(slot, 0);
        runtime.bossRewards().writeDroppablePrivateState4(slot, 0);
        runtime.bossRewards().writeDropPrivateCountdown1(slot, EntityRomConstants.DROPPABLE_REVEAL_COUNTDOWN);
        runtime.physicsTables().writeSlowTransitionCountdown(slot, EntityRomConstants.DROPPABLE_REVEAL_SLOW_COUNTDOWN);
        runtime.physicsTables().writeSlowTimerInitialized(slot, true);

        runtime.physicsTables().writeEntityOptions1Override(slot, EntityRomConstants.DROPPABLE_REVEALED_OPTIONS1);
        if (entity.type() == EntityRomConstants.ENTITY_DROPPABLE_FAIRY) {
            Vector vector = vectorAwayFromLink(
                    entity.x(),
                    entity.y(),
                    linkEntityX,
                    linkEntityY,
                    EntityRomConstants.DROPPABLE_REVEAL_VECTOR_LENGTH);
            runtime.motionSet().fairyMotion.setSpeed(slot, vector.x(), vector.y());
            return;
        }

        runtime.motionSet()
                .enemyDropMotion
                .initialize(
                        slot,
                        runtime.physicsTables().readGroundInteractionSideScrolling(),
                        EntityRomConstants.DROPPABLE_REVEAL_SPEED_Z);
        runtime.motionSet()
                .enemyDropMotion
                .initializeAwayFromLink(
                        slot,
                        entity.x(),
                        entity.y(),
                        linkEntityX,
                        linkEntityY,
                        EntityRomConstants.DROPPABLE_REVEAL_VECTOR_LENGTH);
        runtime.bossRewards().writeEnemyDropActive(slot, true);
        runtime.physicsTables().writeEntityOptions1Override(slot, EntityRomConstants.DROPPABLE_REVEALED_OPTIONS1);
    }

    public SlimeKeyTransitionUpdate advanceHidingSlimeKey(
            RoomEntity entity, int slot, int linkEntityX, int linkEntityY, int linkZ) {
        int transition = runtime.physicsTables().readEnemyTransitionCountdown(slot);
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
            int leaves = Math.min(0x63, runtime.roomVariables().readEntityGoldenLeavesCount() & 0xFF);
            if (!runtime.indoorRoom()
                    && runtime.roomVariables().readEntityRoomId()
                            == EntityRomConstants.ROOM_OW_POTHOLE_FIELD_SLIME_KEY) {
                leaves = EntityRomConstants.GOLDEN_LEAVES_FIVE_COUNT;
            }
            leaves = Math.min(0x63, leaves + 1);
            runtime.roomVariables().writeEntityGoldenLeavesCount(leaves);
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
        runtime.physicsTables().writeEnemyPhysicsFlags(slot, initialPhysicsFlags(entity.type()));
        if (isCommonDroppableType(entity.type())) {
            initializeCommonDroppable(entity);
        }
        if (entity.type() == EntityRomConstants.ENTITY_DROPPABLE_SECRET_SEASHELL) {
            runtime.motionSet()
                    .secretSeashellMotion
                    .ensureInitialized(slot, runtime.roomVariables().readEntityRoomId());
            runtime.physicsTables()
                    .writeEntityOptions1Override(slot, EntityRomConstants.SECRET_SEASHELL_HIDDEN_OPTIONS1);
        }
        if (runtime.indoorRoom() && RoomEntityPickupRules.usesIndoorDefaultSlowTimer(entity.type())) {
            runtime.physicsTables().writeSlowTransitionCountdown(slot, 0x80);
            runtime.physicsTables().writeSlowTimerInitialized(slot, true);
        }
    }

    void resetEnemyDropState(int slot) {
        runtime.bossRewards().writeDroppedItemBySlot(slot, 0);
        runtime.bossRewards().writeDropPrivateCountdown1(slot, 0);
        runtime.bossRewards().writeDropPrivateCountdown3(slot, 0);
        runtime.bossRewards().writeDroppablePrivateState3(slot, 0);
        runtime.bossRewards().writeDroppablePrivateState4(slot, 0);
        runtime.bossRewards().writeEnemyDropActive(slot, false);
        runtime.motionSet().enemyDropMotion.clear(slot);
        runtime.motionSet().fairyMotion.clear(slot);
        runtime.motionSet().hookshotChainMotion.clear(slot);
    }

    void decrementEnemyDropCountdowns(RoomEntity entity) {
        int slot = entity.slot();
        if (runtime.bossRewards().readDropPrivateCountdown1(slot) > 0) {
            runtime.bossRewards().postDropPrivateCountdown1(slot, -1);
        }
        if (runtime.bossRewards().readDropPrivateCountdown3(slot) > 0) {
            runtime.bossRewards().postDropPrivateCountdown3(slot, -1);
        }
    }

    public boolean checkForKeyDropQuicksandHole(RoomEntity entity) {
        if (runtime.indoorRoom() || runtime.roomVariables().readEntityRoomId() != 0xCE || entity.z() != 0) {
            return false;
        }
        return withinUnsignedWindow(entity.y(), 0x48, 3) && withinUnsignedWindow(entity.x(), 0x50, 3);
    }

    public void advanceHeartPiecePickup(RoomEntity entity, int x, int y, int z) {
        int slot = entity.slot();
        var update = this.readHeartPiecePickup(slot)
                .advance(
                        runtime.physicsTables().readEnemyTransitionCountdown(slot),
                        runtime.linkState().dialogActive,
                        this.readHeartPiecesCount());
        runtime.events().heartPieceUpdates.add(update);
        if (update.incrementPiece()) this.postHeartPiecesCount(1);
        if (update.newContainer()) this.writeHeartPiecesCount(0);
        if (update.dialogId() >= 0) runtime.events().pendingDialogRequests.add(new DialogRequest(0, update.dialogId()));
        if (update.newContainer())
            runtime.events()
                    .pendingEntityEvents
                    .add(new EntityCombatEvent(
                            slot, entity.type(), 0, false, EntityCombatEvent.SoundChannel.JINGLE, 0x19));
        if (update.complete()) {
            runtime.lifecycle().disableEntityWithoutPersistence(slot);
            return;
        }
        runtime.events().pendingLinkMotionBlockRequests.add(new LinkMotionBlockRequest(slot));
        runtime.events().pendingLinkHeldItemPoseRequests.add(new LinkHeldItemPoseRequest(slot));
        runtime.slots()[slot] = withZ(withPositionAndVariant(entity, x, (y - 12) & 255, entity.spriteVariant()), z);
    }

    public void initializeChestEntity(RoomEntity entity) {
        int slot = entity.slot();
        int itemType = this.readChestItemBySlot(slot) & 0xFF;
        this.writeChestSpeedY(slot, 0xFC);
        this.writeChestSpeedYAccumulator(slot, 0);
        this.writeChestInertia(slot, 0);
        runtime.events()
                .pendingEntityEvents
                .add(new EntityCombatEvent(
                        slot,
                        EntityRomConstants.ENTITY_CHEST_WITH_ITEM,
                        0,
                        false,
                        EntityCombatEvent.SoundChannel.NOISE,
                        EntityRomConstants.CHEST_OPEN_NOISE_ID));
        if (itemType == ChestContentsTable.CHEST_TAIL_KEY) {
            for (RoomEntity candidate : runtime.slots()) {
                if (candidate.loaded() && candidate.type() == EntityRomConstants.ENTITY_OWL_EVENT) {
                    runtime.story().writeOwlPrivateCountdown1(candidate.slot(), 0x38);
                    break;
                }
            }
        }
        if (itemType < ChestContentsTable.CHEST_MESSAGE) {
            runtime.events().pendingChestRewardEvents.add(new ChestRewardEvent(slot, itemType));
        }
    }

    public boolean advanceChestEntity(int index, RoomEntity entity) {
        int slot = entity.slot();
        int itemType = this.readChestItemBySlot(slot) & 0xFF;
        if (itemType == ChestContentsTable.CHEST_ZOL) {
            spawnChestZol(entity);
            runtime.clearEntity(slot);
            return true;
        }

        // ChestWithItemEntityHandler ($07:7C5E) renders before this gate,
        // but does not move, advance inertia, or unload while text is open.
        if (runtime.linkState().dialogActive) {
            return false;
        }

        int y = addFallingSpeedToPosition(
                entity.y(), this.readChestSpeedY(slot), this.storageChestSpeedYAccumulator(), slot);
        int inertia = (this.readChestInertia(slot) + 1) & 0xFF;
        this.writeChestInertia(slot, inertia);
        if (inertia == 0x10) {
            this.writeChestSpeedY(slot, 0);
        }
        if (inertia == 0x08 && runtime.chestContentsTable() != null) {
            int soundValue = runtime.chestContentsTable().presentationSoundValue(itemType);
            if (soundValue == EntityRomConstants.CHEST_TREASURE_JINGLE_ID) {
                runtime.events()
                        .pendingEntityEvents
                        .add(new EntityCombatEvent(
                                slot,
                                EntityRomConstants.ENTITY_CHEST_WITH_ITEM,
                                0,
                                false,
                                EntityCombatEvent.SoundChannel.JINGLE,
                                EntityRomConstants.CHEST_TREASURE_JINGLE_ID));
            } else if (soundValue != 0) {
                runtime.presentationState().writePendingMusicTrack(soundValue);
            }
        }
        if (inertia == 0x26 && runtime.chestContentsTable() != null) {
            int roomId = runtime.presentationState().readSpriteSelection() == null
                    ? -1
                    : runtime.presentationState().readSpriteSelection().roomId();
            int dialogLow = runtime.chestContentsTable()
                    .dialogLowIdFor(
                            itemType,
                            this.readChestShieldLevel(),
                            this.readChestSwordLevel(),
                            this.readChestPowerBraceletLevel(),
                            runtime.roomVariables().readEntityMapId() < 0
                                    ? 0
                                    : runtime.roomVariables().readEntityMapId(),
                            roomId < 0 ? 0 : roomId);
            int tableId = itemType == ChestContentsTable.CHEST_MESSAGE && roomId == 0x96 ? 1 : 0;
            runtime.events().pendingDialogRequests.add(new DialogRequest(tableId, dialogLow));
        }
        if (inertia == 0x28) {
            runtime.clearEntity(slot);
            return true;
        }
        runtime.slots()[index] = withPositionAndVariant(entity, entity.x(), y, itemType);
        return false;
    }

    void spawnChestZol(RoomEntity chest) {
        int freeSlot = runtime.findFreeEntitySlot();
        if (freeSlot < 0) {
            return;
        }
        EntitySpriteDefinition definition = runtime.slotProperties().spriteDefinitionFor(EntityRomConstants.ENTITY_ZOL);
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
        runtime.slots()[freeSlot] = zol;
        runtime.initializeHealthGroup(freeSlot);
        runtime.motionSet().zolGelMotion.prepareChestSpawn(freeSlot);
        runtime.physicsTables().writeEnemyPhysicsFlags(freeSlot, initialPhysicsFlags(EntityRomConstants.ENTITY_ZOL));
        runtime.combatTables().writeEnemyHitboxFlags(freeSlot, 0);
        runtime.combatTables().writeEnemyHealth(freeSlot, runtime.initialHealth(EntityRomConstants.ENTITY_ZOL));
        runtime.combatTables().writeEnemyIgnoreHitsCountdown(freeSlot, 1);
        runtime.physicsTables().writeDynamicEntitySpawnedThisFrame(freeSlot, true);
        runtime.events()
                .pendingEntityEvents
                .add(new EntityCombatEvent(
                        freeSlot,
                        EntityRomConstants.ENTITY_ZOL,
                        0,
                        false,
                        EntityCombatEvent.SoundChannel.JINGLE,
                        0x1D));
    }

    void spawnEnemyDrop(RoomEntity source, int itemType) {
        int freeSlot = runtime.findFreeEntitySlot();
        if (freeSlot < 0) {
            return;
        }

        EntitySpriteDefinition definition = runtime.slotProperties().spriteDefinitionFor(itemType);
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
        runtime.slots()[freeSlot] = drop;
        runtime.initializeHealthGroup(freeSlot);
        resetEnemyDropState(freeSlot);
        runtime.physicsTables().writeSlowTransitionCountdown(freeSlot, 0x80);
        runtime.physicsTables().writeSlowTimerInitialized(freeSlot, true);
        runtime.physicsTables().writeEnemyTransitionCountdown(freeSlot, 0);
        runtime.combatTables().writeEnemyStunnedCountdown(freeSlot, 0);
        runtime.combatTables().writeDyingCountdown(freeSlot, 0);
        runtime.combatTables().writePowerRecoilDeath(freeSlot, false);
        runtime.physicsTables().writeEnemyPhysicsFlags(freeSlot, initialPhysicsFlags(itemType));
        runtime.combatTables().writeEnemyHealth(freeSlot, runtime.initialHealth(itemType));
        runtime.combatTables().writeEnemyFlashCountdown(freeSlot, 0);
        runtime.combatTables().writeEnemyIgnoreHitsCountdown(freeSlot, 1);
        runtime.physicsTables().writeEntityGroundStatus(freeSlot, 0);
        runtime.physicsTables().writeBaseEntityFlipAttribute(freeSlot, 0);
        runtime.physicsTables().writeEntityOptions1Override(freeSlot, -1);
        runtime.motionSet().enemyRecoilMotion.clear(freeSlot);
        runtime.bossRewards().writeDropPrivateCountdown1(freeSlot, 0x18);
        runtime.bossRewards().writeDropPrivateCountdown3(freeSlot, 0x03);
        if (itemType == EntityRomConstants.ENTITY_DROPPABLE_FAIRY) {
            runtime.motionSet().fairyMotion.initialize(freeSlot);
        } else {
            runtime.motionSet()
                    .enemyDropMotion
                    .initialize(freeSlot, runtime.physicsTables().readGroundInteractionSideScrolling());
            runtime.bossRewards().writeEnemyDropActive(freeSlot, true);
        }
        runtime.physicsTables().writeDynamicEntitySpawnedThisFrame(freeSlot, true);
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

        EntitySpriteDefinition definition = runtime.slotProperties().spriteDefinitionFor(itemType);
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
        runtime.slots()[freeSlot] = drop;
        runtime.initializeHealthGroup(freeSlot);
        resetEnemyDropState(freeSlot);
        runtime.motionSet()
                .enemyDropMotion
                .initialize(freeSlot, runtime.physicsTables().readGroundInteractionSideScrolling(), speedZ);
        runtime.bossRewards().writeEnemyDropActive(freeSlot, true);
        runtime.physicsTables().writeSlowTransitionCountdown(freeSlot, 0);
        runtime.physicsTables().writeSlowTimerInitialized(freeSlot, false);
        runtime.bossRewards().writeDropPrivateCountdown1(freeSlot, privateCountdown1);
        runtime.bossRewards().writeDropPrivateCountdown3(freeSlot, 0);
        runtime.physicsTables().writeEnemyTransitionCountdown(freeSlot, 0);
        runtime.combatTables().writeEnemyStunnedCountdown(freeSlot, 0);
        runtime.combatTables().writeDyingCountdown(freeSlot, 0);
        runtime.combatTables().writePowerRecoilDeath(freeSlot, false);
        runtime.physicsTables().writeEnemyPhysicsFlags(freeSlot, initialPhysicsFlags(itemType));
        runtime.combatTables().writeEnemyHealth(freeSlot, runtime.initialHealth(itemType));
        runtime.combatTables().writeEnemyFlashCountdown(freeSlot, 0);
        runtime.combatTables().writeEnemyIgnoreHitsCountdown(freeSlot, 1);
        runtime.physicsTables().writeEntityGroundStatus(freeSlot, 0);
        runtime.physicsTables().writeBaseEntityFlipAttribute(freeSlot, 0);
        runtime.physicsTables().writeEntityOptions1Override(freeSlot, -1);
        runtime.motionSet().enemyRecoilMotion.clear(freeSlot);
        runtime.physicsTables().writeDynamicEntitySpawnedThisFrame(freeSlot, true);
        return freeSlot;
    }

    public boolean hasHeartPiecePickup(int slot) {
        return this.readHeartPiecePickup(slot) != null;
    }

    int readChestSpeedY(int slot) {
        return chestSpeedY[slot];
    }

    int writeChestSpeedY(int slot, int value) {
        return chestSpeedY[slot] = value;
    }

    int writeChestSpeedYAccumulator(int slot, int value) {
        return chestSpeedYAccumulator[slot] = value;
    }

    int[] storageChestSpeedYAccumulator() {
        return chestSpeedYAccumulator;
    }

    int readChestInertia(int slot) {
        return chestInertia[slot];
    }

    int writeChestInertia(int slot, int value) {
        return chestInertia[slot] = value;
    }

    int readChestItemBySlot(int slot) {
        return chestItemBySlot[slot];
    }

    int writeChestItemBySlot(int slot, int value) {
        return chestItemBySlot[slot] = value;
    }

    int readChestShieldLevel() {
        return chestShieldLevel;
    }

    int writeChestShieldLevel(int value) {
        return chestShieldLevel = value;
    }

    int readChestSwordLevel() {
        return chestSwordLevel;
    }

    int writeChestSwordLevel(int value) {
        return chestSwordLevel = value;
    }

    boolean readChestPlayerLevelsKnown() {
        return chestPlayerLevelsKnown;
    }

    boolean writeChestPlayerLevelsKnown(boolean value) {
        return chestPlayerLevelsKnown = value;
    }

    int readChestPowerBraceletLevel() {
        return chestPowerBraceletLevel;
    }

    int writeChestPowerBraceletLevel(int value) {
        return chestPowerBraceletLevel = value;
    }

    HeartPiecePickupMotion readHeartPiecePickup(int slot) {
        return heartPiecePickup[slot];
    }

    HeartPiecePickupMotion writeHeartPiecePickup(int slot, HeartPiecePickupMotion value) {
        return heartPiecePickup[slot] = value;
    }

    HeartPiecePickupMotion[] storageHeartPiecePickup() {
        return heartPiecePickup;
    }

    int readHeartPiecesCount() {
        return heartPiecesCount;
    }

    int writeHeartPiecesCount(int value) {
        return heartPiecesCount = value;
    }

    int postHeartPiecesCount(int delta) {
        int previous = heartPiecesCount;
        heartPiecesCount += delta;
        return previous;
    }

    int writeHeartPieceMeterPairs(int slot, int value) {
        return heartPieceMeterPairs[slot] = value;
    }

    int[] storageHeartPieceMeterPairs() {
        return heartPieceMeterPairs;
    }
}
