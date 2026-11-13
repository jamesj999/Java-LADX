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
import linksawakening.world.entity.motion.*;
import linksawakening.world.room.ChestContentsTable;

/** ROM pickup, reveal and drop mechanics, sharing the owning room slot table. */
public final class EntityPickupController {
    private final RoomEntityRuntime.CommonDroppableUpdate[] commonUpdates =
            new RoomEntityRuntime.CommonDroppableUpdate[RoomEntity.SLOT_COUNT];

    RoomEntityRuntime.CommonDroppableUpdate commonUpdate(RoomEntity entity, boolean hidden) {
        var previous = commonUpdates[entity.slot()];
        if (previous == null || previous.entity() != entity || previous.hidden() != hidden) {
            previous = new RoomEntityRuntime.CommonDroppableUpdate(entity, hidden);
            commonUpdates[entity.slot()] = previous;
        }
        return previous;
    }

    final EntityPickupState state;
    final boolean indoorRoom;
    final EntityBossState bossRewards;
    final EntityCombatTables combatTables;
    final EntityEventChannels events;
    final EntityLinkState linkState;
    final EntityMotionRegistry motions;
    final EntityPhysicsTables physicsTables;
    final EntityPresentationState presentationState;
    final EntityRoomVariables roomVariables;
    final EntitySlotLifecycle lifecycle;
    final EntitySlotProperties slotProperties;
    final EntityStoryState story;
    final RoomEntity[] slots;
    // Optional ROM data, absent in ROM-free runtimes.
    final linksawakening.world.room.ChestContentsTable chestContentsTable;

    EntityPickupController(
            EntityPickupState state,
            boolean indoorRoom,
            EntityBossState bossRewards,
            EntityCombatTables combatTables,
            EntityEventChannels events,
            EntityLinkState linkState,
            EntityMotionRegistry motions,
            EntityPhysicsTables physicsTables,
            EntityPresentationState presentationState,
            EntityRoomVariables roomVariables,
            EntitySlotLifecycle lifecycle,
            EntitySlotProperties slotProperties,
            EntityStoryState story,
            RoomEntity[] slots,
            linksawakening.world.room.ChestContentsTable chestContentsTable) {
        this.state = java.util.Objects.requireNonNull(state, "state");
        this.indoorRoom = indoorRoom;
        this.bossRewards = java.util.Objects.requireNonNull(bossRewards, "bossRewards");
        this.combatTables = java.util.Objects.requireNonNull(combatTables, "combatTables");
        this.events = java.util.Objects.requireNonNull(events, "events");
        this.linkState = java.util.Objects.requireNonNull(linkState, "linkState");
        this.motions = java.util.Objects.requireNonNull(motions, "motions");
        this.physicsTables = java.util.Objects.requireNonNull(physicsTables, "physicsTables");
        this.presentationState = java.util.Objects.requireNonNull(presentationState, "presentationState");
        this.roomVariables = java.util.Objects.requireNonNull(roomVariables, "roomVariables");
        this.lifecycle = java.util.Objects.requireNonNull(lifecycle, "lifecycle");
        this.slotProperties = java.util.Objects.requireNonNull(slotProperties, "slotProperties");
        this.story = java.util.Objects.requireNonNull(story, "story");
        this.slots = java.util.Objects.requireNonNull(slots, "slots");
        this.chestContentsTable = chestContentsTable;
    }

    void initializeCommonDroppable(RoomEntity entity) {
        EntityPickupController.droppablePickupBehaviorInitializeCommonDroppable(this, entity);
    }

    public CommonDroppableUpdate advanceCommonDroppable(
            RoomEntity entity, int linkEntityX, int linkEntityY, RoomEntityObjectSample objectUnderEntity) {
        return EntityPickupController.droppablePickupBehaviorAdvanceCommonDroppable(
                this, entity, linkEntityX, linkEntityY, objectUnderEntity);
    }

    void revealCommonDroppable(RoomEntity entity, int linkEntityX, int linkEntityY) {
        EntityPickupController.droppablePickupBehaviorRevealCommonDroppable(this, entity, linkEntityX, linkEntityY);
    }

    public SlimeKeyTransitionUpdate advanceHidingSlimeKey(
            RoomEntity entity, int slot, int linkEntityX, int linkEntityY, int linkZ) {
        return EntityPickupController.droppablePickupBehaviorAdvanceHidingSlimeKey(
                this, entity, slot, linkEntityX, linkEntityY, linkZ);
    }

    void initializeEntityTimers(RoomEntity entity) {
        int slot = entity.slot();
        this.physicsTables.writeEnemyPhysicsFlags(slot, initialPhysicsFlags(entity.type()));
        if (isCommonDroppableType(entity.type())) {
            initializeCommonDroppable(entity);
        }
        if (entity.type() == EntityRomConstants.ENTITY_DROPPABLE_SECRET_SEASHELL) {
            this.motions
                    .get(SecretSeashellMotion.class, SecretSeashellMotion::new)
                    .ensureInitialized(slot, this.roomVariables.readEntityRoomId());
            this.physicsTables.writeEntityOptions1Override(slot, EntityRomConstants.SECRET_SEASHELL_HIDDEN_OPTIONS1);
        }
        if (this.indoorRoom && RoomEntityPickupRules.usesIndoorDefaultSlowTimer(entity.type())) {
            this.physicsTables.writeSlowTransitionCountdown(slot, 0x80);
            this.physicsTables.writeSlowTimerInitialized(slot, true);
        }
    }

    void resetEnemyDropState(int slot) {
        bossRewards.resetEnemyDropState(slot, motions);
    }

    void decrementEnemyDropCountdowns(RoomEntity entity) {
        int slot = entity.slot();
        if (this.bossRewards.readDropPrivateCountdown1(slot) > 0) {
            this.bossRewards.postDropPrivateCountdown1(slot, -1);
        }
        if (this.bossRewards.readDropPrivateCountdown3(slot) > 0) {
            this.bossRewards.postDropPrivateCountdown3(slot, -1);
        }
    }

    public boolean checkForKeyDropQuicksandHole(RoomEntity entity) {
        if (this.indoorRoom || this.roomVariables.readEntityRoomId() != 0xCE || entity.z() != 0) {
            return false;
        }
        return withinUnsignedWindow(entity.y(), 0x48, 3) && withinUnsignedWindow(entity.x(), 0x50, 3);
    }

    public void advanceHeartPiecePickup(RoomEntity entity, int x, int y, int z) {
        EntityPickupController.droppablePickupBehaviorAdvanceHeartPiecePickup(this, entity, x, y, z);
    }

    public void initializeChestEntity(RoomEntity entity) {
        EntityPickupController.chestPickupBehaviorInitializeChestEntity(this, entity);
    }

    public boolean advanceChestEntity(int index, RoomEntity entity) {
        return EntityPickupController.chestPickupBehaviorAdvanceChestEntity(this, index, entity);
    }

    void spawnChestZol(RoomEntity chest) {
        EntityPickupController.chestPickupBehaviorSpawnChestZol(this, chest);
    }

    void spawnEnemyDrop(RoomEntity source, int itemType) {
        EntityPickupController.enemyDropSpawnsSpawnEnemyDrop(this, source, itemType);
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
        return EntityPickupController.enemyDropSpawnsSpawnKeyDropAt(
                this, itemType, x, y, spriteVariant, z, speedZ, privateCountdown1);
    }

    public boolean hasHeartPiecePickup(int slot) {
        return state.readHeartPiecePickup(slot) != null;
    }

    final DroppablePickupFrameStep droppablePickupFrameStep = new DroppablePickupFrameStep();

    static void chestPickupBehaviorInitializeChestEntity(EntityPickupController owner, RoomEntity entity) {
        var runtime = owner;
        int slot = entity.slot();
        int itemType = owner.state.readChestItemBySlot(slot) & 0xFF;
        owner.state.writeChestSpeedY(slot, 0xFC);
        owner.state.writeChestSpeedYAccumulator(slot, 0);
        owner.state.writeChestInertia(slot, 0);
        runtime.events.pendingEntityEvents.add(new EntityCombatEvent(
                slot,
                EntityRomConstants.ENTITY_CHEST_WITH_ITEM,
                0,
                false,
                EntityCombatEvent.SoundChannel.NOISE,
                EntityRomConstants.CHEST_OPEN_NOISE_ID));
        if (itemType == ChestContentsTable.CHEST_TAIL_KEY) {
            for (RoomEntity candidate : runtime.slots) {
                if (candidate.loaded() && candidate.type() == EntityRomConstants.ENTITY_OWL_EVENT) {
                    runtime.story.owlState().writeOwlPrivateCountdown1(candidate.slot(), 0x38);
                    break;
                }
            }
        }
        if (itemType < ChestContentsTable.CHEST_MESSAGE) {
            runtime.events.pendingChestRewardEvents.add(new ChestRewardEvent(slot, itemType));
        }
    }

    static boolean chestPickupBehaviorAdvanceChestEntity(EntityPickupController owner, int index, RoomEntity entity) {
        var runtime = owner;
        int slot = entity.slot();
        int itemType = owner.state.readChestItemBySlot(slot) & 0xFF;
        if (itemType == ChestContentsTable.CHEST_ZOL) {
            owner.spawnChestZol(entity);
            runtime.lifecycle.clearEntity(slot);
            return true;
        }

        // ChestWithItemEntityHandler ($07:7C5E) renders before this gate,
        // but does not move, advance inertia, or unload while text is open.
        if (runtime.linkState.dialogActive) {
            return false;
        }

        int y = addFallingSpeedToPosition(
                entity.y(), owner.state.readChestSpeedY(slot), owner.state.storageChestSpeedYAccumulator(), slot);
        int inertia = (owner.state.readChestInertia(slot) + 1) & 0xFF;
        owner.state.writeChestInertia(slot, inertia);
        if (inertia == 0x10) {
            owner.state.writeChestSpeedY(slot, 0);
        }
        if (inertia == 0x08 && runtime.chestContentsTable != null) {
            int soundValue = runtime.chestContentsTable.presentationSoundValue(itemType);
            if (soundValue == EntityRomConstants.CHEST_TREASURE_JINGLE_ID) {
                runtime.events.pendingEntityEvents.add(new EntityCombatEvent(
                        slot,
                        EntityRomConstants.ENTITY_CHEST_WITH_ITEM,
                        0,
                        false,
                        EntityCombatEvent.SoundChannel.JINGLE,
                        EntityRomConstants.CHEST_TREASURE_JINGLE_ID));
            } else if (soundValue != 0) {
                runtime.presentationState.writePendingMusicTrack(soundValue);
            }
        }
        if (inertia == 0x26 && runtime.chestContentsTable != null) {
            int roomId = runtime.presentationState.readSpriteSelection() == null
                    ? -1
                    : runtime.presentationState.readSpriteSelection().roomId();
            int dialogLow = runtime.chestContentsTable.dialogLowIdFor(
                    itemType,
                    owner.state.readChestShieldLevel(),
                    owner.state.readChestSwordLevel(),
                    owner.state.readChestPowerBraceletLevel(),
                    runtime.roomVariables.readEntityMapId() < 0 ? 0 : runtime.roomVariables.readEntityMapId(),
                    roomId < 0 ? 0 : roomId);
            int tableId = itemType == ChestContentsTable.CHEST_MESSAGE && roomId == 0x96 ? 1 : 0;
            runtime.events.pendingDialogRequests.add(new DialogRequest(tableId, dialogLow));
        }
        if (inertia == 0x28) {
            runtime.lifecycle.clearEntity(slot);
            return true;
        }
        runtime.slots[index] = withPositionAndVariant(entity, entity.x(), y, itemType);
        return false;
    }

    static void chestPickupBehaviorSpawnChestZol(EntityPickupController owner, RoomEntity chest) {
        var runtime = owner;
        int freeSlot = EntitySlotAllocator.findFreeSlot(runtime.slots, runtime.combatTables, runtime.slots.length - 1);
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
        runtime.lifecycle.initializeHealthGroup(freeSlot);
        runtime.motions.get(ZolGelMotion.class, ZolGelMotion::new).prepareChestSpawn(freeSlot);
        runtime.physicsTables.writeEnemyPhysicsFlags(freeSlot, initialPhysicsFlags(EntityRomConstants.ENTITY_ZOL));
        runtime.combatTables.writeEnemyHitboxFlags(freeSlot, 0);
        runtime.combatTables.writeEnemyHealth(
                freeSlot, runtime.slotProperties.initialHealth(EntityRomConstants.ENTITY_ZOL));
        runtime.combatTables.writeEnemyIgnoreHitsCountdown(freeSlot, 1);
        runtime.physicsTables.writeDynamicEntitySpawnedThisFrame(freeSlot, true);
        runtime.events.pendingEntityEvents.add(new EntityCombatEvent(
                freeSlot, EntityRomConstants.ENTITY_ZOL, 0, false, EntityCombatEvent.SoundChannel.JINGLE, 0x1D));
    }

    static void droppablePickupBehaviorInitializeCommonDroppable(EntityPickupController owner, RoomEntity entity) {
        var runtime = owner;
        int slot = entity.slot();
        // EntityInitDiggableBushOrPotDroppable is used by hearts and magic
        // powder directly. The other ordinary drops use the tree/pot init
        // handler, which selects the Pegasus branch outdoors and the buried
        // branch indoors.
        runtime.bossRewards.writeDroppablePrivateState3(
                slot,
                entity.type() == EntityRomConstants.ENTITY_DROPPABLE_HEART
                                || entity.type() == EntityRomConstants.ENTITY_DROPPABLE_MAGIC_POWDER
                                || entity.type() == EntityRomConstants.ENTITY_HIDING_SLIME_KEY
                                || runtime.indoorRoom
                        ? EntityRomConstants.DROPPABLE_HIDDEN_STATE_BURIED
                        : EntityRomConstants.DROPPABLE_HIDDEN_STATE_PEGASUS);
        runtime.bossRewards.writeDroppablePrivateState4(slot, 0);
        runtime.physicsTables.writeEntityOptions1Override(
                slot,
                entity.type() == EntityRomConstants.ENTITY_HIDING_SLIME_KEY
                        ? EntityRomConstants.HIDING_SLIME_KEY_HIDDEN_OPTIONS1
                        : EntityRomConstants.DROPPABLE_HIDDEN_OPTIONS1);
    }

    static CommonDroppableUpdate droppablePickupBehaviorAdvanceCommonDroppable(
            EntityPickupController owner,
            RoomEntity entity,
            int linkEntityX,
            int linkEntityY,
            RoomEntityObjectSample objectUnderEntity) {
        return owner.droppablePickupFrameStep.run(owner, entity, linkEntityX, linkEntityY, objectUnderEntity);
    }

    static void droppablePickupBehaviorRevealCommonDroppable(
            EntityPickupController owner, RoomEntity entity, int linkEntityX, int linkEntityY) {
        var runtime = owner;
        int slot = entity.slot();
        runtime.bossRewards.writeDroppablePrivateState3(slot, 0);
        runtime.bossRewards.writeDroppablePrivateState4(slot, 0);
        runtime.bossRewards.writeDropPrivateCountdown1(slot, EntityRomConstants.DROPPABLE_REVEAL_COUNTDOWN);
        runtime.physicsTables.writeSlowTransitionCountdown(slot, EntityRomConstants.DROPPABLE_REVEAL_SLOW_COUNTDOWN);
        runtime.physicsTables.writeSlowTimerInitialized(slot, true);

        runtime.physicsTables.writeEntityOptions1Override(slot, EntityRomConstants.DROPPABLE_REVEALED_OPTIONS1);
        if (entity.type() == EntityRomConstants.ENTITY_DROPPABLE_FAIRY) {
            Vector vector = vectorAwayFromLink(
                    entity.x(),
                    entity.y(),
                    linkEntityX,
                    linkEntityY,
                    EntityRomConstants.DROPPABLE_REVEAL_VECTOR_LENGTH);
            runtime.motions.get(FairyMotion.class, FairyMotion::new).setSpeed(slot, vector.x(), vector.y());
            return;
        }

        runtime.motions
                .get(EnemyDropMotion.class, EnemyDropMotion::new)
                .initialize(
                        slot,
                        runtime.physicsTables.readGroundInteractionSideScrolling(),
                        EntityRomConstants.DROPPABLE_REVEAL_SPEED_Z);
        runtime.motions
                .get(EnemyDropMotion.class, EnemyDropMotion::new)
                .initializeAwayFromLink(
                        slot,
                        entity.x(),
                        entity.y(),
                        linkEntityX,
                        linkEntityY,
                        EntityRomConstants.DROPPABLE_REVEAL_VECTOR_LENGTH);
        runtime.bossRewards.writeEnemyDropActive(slot, true);
        runtime.physicsTables.writeEntityOptions1Override(slot, EntityRomConstants.DROPPABLE_REVEALED_OPTIONS1);
    }

    static SlimeKeyTransitionUpdate droppablePickupBehaviorAdvanceHidingSlimeKey(
            EntityPickupController owner, RoomEntity entity, int slot, int linkEntityX, int linkEntityY, int linkZ) {
        var runtime = owner;
        int transition = runtime.physicsTables.readEnemyTransitionCountdown(slot);
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
            int leaves = Math.min(0x63, runtime.roomVariables.readEntityGoldenLeavesCount() & 0xFF);
            if (!runtime.indoorRoom
                    && runtime.roomVariables.readEntityRoomId() == EntityRomConstants.ROOM_OW_POTHOLE_FIELD_SLIME_KEY) {
                leaves = EntityRomConstants.GOLDEN_LEAVES_FIVE_COUNT;
            }
            leaves = Math.min(0x63, leaves + 1);
            runtime.roomVariables.writeEntityGoldenLeavesCount(leaves);
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

    static void droppablePickupBehaviorAdvanceHeartPiecePickup(
            EntityPickupController owner, RoomEntity entity, int x, int y, int z) {
        var runtime = owner;
        int slot = entity.slot();
        var update = owner.state
                .readHeartPiecePickup(slot)
                .advance(
                        runtime.physicsTables.readEnemyTransitionCountdown(slot),
                        runtime.linkState.dialogActive,
                        owner.state.readHeartPiecesCount());
        runtime.events.heartPieceUpdates.add(update);
        if (update.incrementPiece()) owner.state.postHeartPiecesCount(1);
        if (update.newContainer()) owner.state.writeHeartPiecesCount(0);
        if (update.dialogId() >= 0) runtime.events.pendingDialogRequests.add(new DialogRequest(0, update.dialogId()));
        if (update.newContainer())
            runtime.events.pendingEntityEvents.add(
                    new EntityCombatEvent(slot, entity.type(), 0, false, EntityCombatEvent.SoundChannel.JINGLE, 0x19));
        if (update.complete()) {
            runtime.lifecycle.disableEntityWithoutPersistence(slot);
            return;
        }
        runtime.events.pendingLinkMotionBlockRequests.add(new LinkMotionBlockRequest(slot));
        runtime.events.pendingLinkHeldItemPoseRequests.add(new LinkHeldItemPoseRequest(slot));
        runtime.slots[slot] = withZ(withPositionAndVariant(entity, x, (y - 12) & 255, entity.spriteVariant()), z);
    }

    static void enemyDropSpawnsSpawnEnemyDrop(EntityPickupController owner, RoomEntity source, int itemType) {
        var runtime = owner;
        int freeSlot = EntitySlotAllocator.findFreeSlot(runtime.slots, runtime.combatTables, runtime.slots.length - 1);
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
        runtime.lifecycle.initializeHealthGroup(freeSlot);
        owner.resetEnemyDropState(freeSlot);
        runtime.physicsTables.writeSlowTransitionCountdown(freeSlot, 0x80);
        runtime.physicsTables.writeSlowTimerInitialized(freeSlot, true);
        runtime.physicsTables.writeEnemyTransitionCountdown(freeSlot, 0);
        runtime.combatTables.writeEnemyStunnedCountdown(freeSlot, 0);
        runtime.combatTables.writeDyingCountdown(freeSlot, 0);
        runtime.combatTables.writePowerRecoilDeath(freeSlot, false);
        runtime.physicsTables.writeEnemyPhysicsFlags(freeSlot, initialPhysicsFlags(itemType));
        runtime.combatTables.writeEnemyHealth(freeSlot, runtime.slotProperties.initialHealth(itemType));
        runtime.combatTables.writeEnemyFlashCountdown(freeSlot, 0);
        runtime.combatTables.writeEnemyIgnoreHitsCountdown(freeSlot, 1);
        runtime.physicsTables.writeEntityGroundStatus(freeSlot, 0);
        runtime.physicsTables.writeBaseEntityFlipAttribute(freeSlot, 0);
        runtime.physicsTables.writeEntityOptions1Override(freeSlot, -1);
        runtime.motions.get(EnemyRecoilMotion.class, EnemyRecoilMotion::new).clear(freeSlot);
        runtime.bossRewards.writeDropPrivateCountdown1(freeSlot, 0x18);
        runtime.bossRewards.writeDropPrivateCountdown3(freeSlot, 0x03);
        if (itemType == EntityRomConstants.ENTITY_DROPPABLE_FAIRY) {
            runtime.motions.get(FairyMotion.class, FairyMotion::new).initialize(freeSlot);
        } else {
            runtime.motions
                    .get(EnemyDropMotion.class, EnemyDropMotion::new)
                    .initialize(freeSlot, runtime.physicsTables.readGroundInteractionSideScrolling());
            runtime.bossRewards.writeEnemyDropActive(freeSlot, true);
        }
        runtime.physicsTables.writeDynamicEntitySpawnedThisFrame(freeSlot, true);
    }

    static int enemyDropSpawnsSpawnKeyDropAt(
            EntityPickupController owner,
            int itemType,
            int x,
            int y,
            int spriteVariant,
            int z,
            int speedZ,
            int privateCountdown1) {
        var runtime = owner;
        int freeSlot = EntitySlotAllocator.findFreeSlot(runtime.slots, runtime.combatTables, runtime.slots.length - 1);
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
        runtime.lifecycle.initializeHealthGroup(freeSlot);
        owner.resetEnemyDropState(freeSlot);
        runtime.motions
                .get(EnemyDropMotion.class, EnemyDropMotion::new)
                .initialize(freeSlot, runtime.physicsTables.readGroundInteractionSideScrolling(), speedZ);
        runtime.bossRewards.writeEnemyDropActive(freeSlot, true);
        runtime.physicsTables.writeSlowTransitionCountdown(freeSlot, 0);
        runtime.physicsTables.writeSlowTimerInitialized(freeSlot, false);
        runtime.bossRewards.writeDropPrivateCountdown1(freeSlot, privateCountdown1);
        runtime.bossRewards.writeDropPrivateCountdown3(freeSlot, 0);
        runtime.physicsTables.writeEnemyTransitionCountdown(freeSlot, 0);
        runtime.combatTables.writeEnemyStunnedCountdown(freeSlot, 0);
        runtime.combatTables.writeDyingCountdown(freeSlot, 0);
        runtime.combatTables.writePowerRecoilDeath(freeSlot, false);
        runtime.physicsTables.writeEnemyPhysicsFlags(freeSlot, initialPhysicsFlags(itemType));
        runtime.combatTables.writeEnemyHealth(freeSlot, runtime.slotProperties.initialHealth(itemType));
        runtime.combatTables.writeEnemyFlashCountdown(freeSlot, 0);
        runtime.combatTables.writeEnemyIgnoreHitsCountdown(freeSlot, 1);
        runtime.physicsTables.writeEntityGroundStatus(freeSlot, 0);
        runtime.physicsTables.writeBaseEntityFlipAttribute(freeSlot, 0);
        runtime.physicsTables.writeEntityOptions1Override(freeSlot, -1);
        runtime.motions.get(EnemyRecoilMotion.class, EnemyRecoilMotion::new).clear(freeSlot);
        runtime.physicsTables.writeDynamicEntitySpawnedThisFrame(freeSlot, true);
        return freeSlot;
    }

    static final class DroppablePickupFrameStep {
        private linksawakening.world.entity.EntityPickupController owner;
        private linksawakening.world.entity.RoomEntity entity;
        private int linkEntityX;
        private int linkEntityY;
        private linksawakening.world.entity.RoomEntityObjectSample objectUnderEntity;
        private linksawakening.world.entity.EntityPickupController runtime;
        private int slot;
        private int hiddenState;

        DroppablePickupFrameStep() {}

        linksawakening.world.entity.RoomEntityRuntime.CommonDroppableUpdate run() {
            runtime = owner;
            slot = entity.slot();
            hiddenState = runtime.bossRewards.readDroppablePrivateState3(slot);
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
                            runtime.bossRewards.writeDroppablePrivateState4(slot, 1);
                            reveal = objectId == EntityRomConstants.DROPPABLE_OBJECT_SHOVEL_HOLE;
                        }
                    }
                } else if (hiddenState == EntityRomConstants.DROPPABLE_HIDDEN_STATE_PEGASUS) {
                    reveal = runtime.motions
                                    .get(SecretSeashellMotion.class, SecretSeashellMotion::new)
                                    .readSecretSeashellScreenShakeActive()
                            && runtime.motions
                                    .get(SecretSeashellMotion.class, SecretSeashellMotion::new)
                                    .readSecretSeashellPegasusCollisionActive()
                            && (!runtime.motions
                                            .get(SecretSeashellMotion.class, SecretSeashellMotion::new)
                                            .readSecretSeashellPegasusCollisionCoordinatesKnown()
                                    || withinUnsignedWindow(
                                                    entity.x() + 0x08,
                                                    runtime.motions
                                                            .get(SecretSeashellMotion.class, SecretSeashellMotion::new)
                                                            .readSecretSeashellPegasusCollisionX(),
                                                    0x10)
                                            && withinUnsignedWindow(
                                                    entity.y() + 0x08,
                                                    runtime.motions
                                                            .get(SecretSeashellMotion.class, SecretSeashellMotion::new)
                                                            .readSecretSeashellPegasusCollisionY(),
                                                    0x10));
                }

                if (!reveal) {
                    runtime.physicsTables.writeEntityOptions1Override(
                            slot,
                            entity.type() == EntityRomConstants.ENTITY_HIDING_SLIME_KEY
                                    ? EntityRomConstants.HIDING_SLIME_KEY_HIDDEN_OPTIONS1
                                    : EntityRomConstants.DROPPABLE_HIDDEN_OPTIONS1);
                    return owner.commonUpdate(withVariant(entity, -1), true);
                }
                owner.revealCommonDroppable(entity, linkEntityX, linkEntityY);
                // $629A falls through to .remainInvisible ($629C): POP AF removes
                // the caller return address even on the reveal pass. Drawing,
                // collection and BouncingEntityPhysics start next frame.
                return owner.commonUpdate(withVariant(entity, -1), true);
            }

            runtime.physicsTables.writeEntityOptions1Override(
                    slot,
                    hiddenState == 0
                            ? runtime.physicsTables.readEntityOptions1Override(slot)
                            : EntityRomConstants.DROPPABLE_REVEALED_OPTIONS1);
            if (entity.type() != EntityRomConstants.ENTITY_DROPPABLE_FAIRY
                    && !runtime.bossRewards.readEnemyDropActive(slot)) {
                runtime.motions
                        .get(EnemyDropMotion.class, EnemyDropMotion::new)
                        .initialize(slot, runtime.physicsTables.readGroundInteractionSideScrolling());
                runtime.bossRewards.writeEnemyDropActive(slot, true);
            }
            return owner.commonUpdate(
                    withVariant(entity, entity.spriteDefinition().supported() ? 0 : -1), false);
        }

        private boolean inUse;

        linksawakening.world.entity.RoomEntityRuntime.CommonDroppableUpdate run(
                linksawakening.world.entity.EntityPickupController owner,
                linksawakening.world.entity.RoomEntity entity,
                int linkEntityX,
                int linkEntityY,
                linksawakening.world.entity.RoomEntityObjectSample objectUnderEntity) {
            if (inUse) {
                return new DroppablePickupFrameStep().run(owner, entity, linkEntityX, linkEntityY, objectUnderEntity);
            }
            inUse = true;
            runtime = null;
            slot = 0;
            hiddenState = 0;
            this.owner = owner;
            this.entity = entity;
            this.linkEntityX = linkEntityX;
            this.linkEntityY = linkEntityY;
            this.objectUnderEntity = objectUnderEntity;
            try {
                return run();
            } finally {
                inUse = false;
                this.owner = null;
                this.entity = null;
                this.objectUnderEntity = null;
                this.runtime = null;
            }
        }
    }
}
