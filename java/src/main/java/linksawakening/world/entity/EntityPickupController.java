package linksawakening.world.entity;

import static linksawakening.world.entity.EntityMotionMath.Vector;
import static linksawakening.world.entity.EntityMotionMath.addFallingSpeedToPosition;
import static linksawakening.world.entity.EntityMotionMath.vectorAwayFromLink;
import static linksawakening.world.entity.EntityMotionMath.withinUnsignedWindow;
import static linksawakening.world.entity.EntitySnapshots.withPositionAndVariant;
import static linksawakening.world.entity.EntitySnapshots.withVariant;
import static linksawakening.world.entity.EntitySnapshots.withZ;
import static linksawakening.world.entity.EntityTypeRules.initialPhysicsFlags;
import static linksawakening.world.entity.EntityTypeRules.isCommonDroppableType;

import linksawakening.entity.EntitySpriteDefinition;
import linksawakening.world.entity.event.ChestRewardEvent;
import linksawakening.world.entity.event.DialogRequest;
import linksawakening.world.entity.event.EntityCombatEvent;
import linksawakening.world.entity.event.LinkHeldItemPoseRequest;
import linksawakening.world.entity.event.LinkMotionBlockRequest;
import linksawakening.world.entity.event.SlimeKeyRewardEvent;
import linksawakening.world.entity.motion.EnemyDropMotion;
import linksawakening.world.entity.motion.EnemyRecoilMotion;
import linksawakening.world.entity.motion.FairyMotion;
import linksawakening.world.entity.motion.SecretSeashellMotion;
import linksawakening.world.entity.motion.ZolGelMotion;
import linksawakening.world.room.ChestContentsTable;

/** ROM pickup, reveal and drop mechanics, sharing the owning room slot table. */
public final class EntityPickupController {
    public record SlimeKeyTransitionUpdate(
            RoomEntity entity,
            int nextTransitionCountdown,
            boolean holdAboveLink,
            boolean unload,
            SlimeKeyRewardEvent reward) {}

    public record CommonDroppableUpdate(RoomEntity entity, boolean hidden) {}

    private final EntityPickupController.CommonDroppableUpdate[] commonUpdates =
            new EntityPickupController.CommonDroppableUpdate[RoomEntity.SLOT_COUNT];

    {
        java.util.Arrays.fill(
                commonUpdates, new EntityPickupController.CommonDroppableUpdate(RoomEntity.disabled(0), false));
    }

    // Identity distinguishes cache generations or explicit sentinels; equal payloads must not coalesce.
    @SuppressWarnings("ReferenceEquality")
    EntityPickupController.CommonDroppableUpdate commonUpdate(RoomEntity entity, boolean hidden) {
        var previous = commonUpdates[entity.slot()];
        if (previous.entity() != entity || previous.hidden() != hidden) {
            previous = new EntityPickupController.CommonDroppableUpdate(entity, hidden);
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
    final EntitySlots slots;
    final EntityChestContents chestContentsTable;

    EntityPickupController(
            EntityPickupState state,
            boolean indoorRoom,
            EntityBossState bossRewards,
            EntityEventChannels events,
            EntityLinkState linkState,
            EntityMotionRegistry motions,
            EntityPresentationState presentationState,
            EntityRoomVariables roomVariables,
            EntitySlotLifecycle lifecycle,
            EntitySlotProperties slotProperties,
            EntityStoryState story,
            EntitySlots slots,
            EntityChestContents chestContentsTable) {
        EntityCombatTables combatTables = slots.combat();
        EntityPhysicsTables physicsTables = slots.physics();
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
        this.chestContentsTable = java.util.Objects.requireNonNull(chestContentsTable, "chest contents");
    }

    void initializeCommonDroppable(RoomEntity entity) {
        int slot = entity.slot();
        // EntityInitDiggableBushOrPotDroppable is used by hearts and magic
        // powder directly. The other ordinary drops use the tree/pot init
        // handler, which selects the Pegasus branch outdoors and the buried
        // branch indoors.
        this.bossRewards.writeDroppablePrivateState3(
                slot,
                entity.type() == EntityRomConstants.ENTITY_DROPPABLE_HEART
                                || entity.type() == EntityRomConstants.ENTITY_DROPPABLE_MAGIC_POWDER
                                || entity.type() == EntityRomConstants.ENTITY_HIDING_SLIME_KEY
                                || this.indoorRoom
                        ? EntityRomConstants.DROPPABLE_HIDDEN_STATE_BURIED
                        : EntityRomConstants.DROPPABLE_HIDDEN_STATE_PEGASUS);
        this.bossRewards.writeDroppablePrivateState4(slot, 0);
        this.physicsTables.writeEntityOptions1Override(
                slot,
                entity.type() == EntityRomConstants.ENTITY_HIDING_SLIME_KEY
                        ? EntityRomConstants.HIDING_SLIME_KEY_HIDDEN_OPTIONS1
                        : EntityRomConstants.DROPPABLE_HIDDEN_OPTIONS1);
    }

    public CommonDroppableUpdate advanceCommonDroppable(
            RoomEntity entity, int linkEntityX, int linkEntityY, RoomEntityObjectSample objectUnderEntity) {
        objectUnderEntity = RoomEntityObjectSample.orAbsent(objectUnderEntity);
        return this.droppablePickupFrameStep.run(entity, linkEntityX, linkEntityY, objectUnderEntity);
    }

    void revealCommonDroppable(RoomEntity entity, int linkEntityX, int linkEntityY) {
        var runtime = this;
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

    public SlimeKeyTransitionUpdate advanceHidingSlimeKey(
            RoomEntity entity, int slot, int linkEntityX, int linkEntityY, int linkZ) {
        int transition = this.physicsTables.readEnemyTransitionCountdown(slot);
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
            int leaves = Math.min(0x63, this.roomVariables.readEntityGoldenLeavesCount() & 0xFF);
            if (!this.indoorRoom
                    && this.roomVariables.readEntityRoomId() == EntityRomConstants.ROOM_OW_POTHOLE_FIELD_SLIME_KEY) {
                leaves = EntityRomConstants.GOLDEN_LEAVES_FIVE_COUNT;
            }
            leaves = Math.min(0x63, leaves + 1);
            this.roomVariables.writeEntityGoldenLeavesCount(leaves);
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
        int slot = entity.slot();
        var update = this.state
                .readHeartPiecePickup(slot)
                .advance(
                        this.physicsTables.readEnemyTransitionCountdown(slot),
                        this.linkState.dialogActive,
                        this.state.readHeartPiecesCount());
        this.events.heartPieceUpdates.add(update);
        if (update.incrementPiece()) this.state.postHeartPiecesCount(1);
        if (update.newContainer()) this.state.writeHeartPiecesCount(0);
        if (update.dialogId() >= 0) this.events.pendingDialogRequests.add(new DialogRequest(0, update.dialogId()));
        if (update.newContainer())
            this.events.pendingEntityEvents.add(
                    new EntityCombatEvent(slot, entity.type(), 0, false, EntityCombatEvent.SoundChannel.JINGLE, 0x19));
        if (update.complete()) {
            this.lifecycle.disableEntityWithoutPersistence(slot);
            return;
        }
        this.events.pendingLinkMotionBlockRequests.add(new LinkMotionBlockRequest(slot));
        this.events.pendingLinkHeldItemPoseRequests.add(new LinkHeldItemPoseRequest(slot));
        this.slots.replace(slot, withZ(withPositionAndVariant(entity, x, (y - 12) & 255, entity.spriteVariant()), z));
    }

    public void initializeChestEntity(RoomEntity entity) {
        int slot = entity.slot();
        int itemType = this.state.readChestItemBySlot(slot) & 0xFF;
        this.state.writeChestSpeedY(slot, 0xFC);
        this.state.writeChestSpeedYAccumulator(slot, 0);
        this.state.writeChestInertia(slot, 0);
        this.events.pendingEntityEvents.add(new EntityCombatEvent(
                slot,
                EntityRomConstants.ENTITY_CHEST_WITH_ITEM,
                0,
                false,
                EntityCombatEvent.SoundChannel.NOISE,
                EntityRomConstants.CHEST_OPEN_NOISE_ID));
        if (itemType == ChestContentsTable.CHEST_TAIL_KEY) {
            for (int candidateSlotIndex = 0; candidateSlotIndex < this.slots.size(); candidateSlotIndex++) {
                RoomEntity candidate = this.slots.get(candidateSlotIndex);

                if (candidate.loaded() && candidate.type() == EntityRomConstants.ENTITY_OWL_EVENT) {
                    this.story.owlState().writeOwlPrivateCountdown1(candidate.slot(), 0x38);
                    break;
                }
            }
        }
        if (itemType < ChestContentsTable.CHEST_MESSAGE) {
            this.events.pendingChestRewardEvents.add(new ChestRewardEvent(slot, itemType));
        }
    }

    public boolean advanceChestEntity(int index, RoomEntity entity) {
        int slot = entity.slot();
        int itemType = this.state.readChestItemBySlot(slot) & 0xFF;
        if (itemType == ChestContentsTable.CHEST_ZOL) {
            this.spawnChestZol(entity);
            this.lifecycle.clearEntity(slot);
            return true;
        }

        // ChestWithItemEntityHandler ($07:7C5E) renders before this gate,
        // but does not move, advance inertia, or unload while text is open.
        if (this.linkState.dialogActive) {
            return false;
        }

        int y = addFallingSpeedToPosition(
                entity.y(), this.state.readChestSpeedY(slot), this.state.storageChestSpeedYAccumulator(), slot);
        int inertia = (this.state.readChestInertia(slot) + 1) & 0xFF;
        this.state.writeChestInertia(slot, inertia);
        if (inertia == 0x10) {
            this.state.writeChestSpeedY(slot, 0);
        }
        if (inertia == 0x08) {
            int soundValue = this.chestContentsTable.presentationSoundValue(itemType);
            if (soundValue == EntityRomConstants.CHEST_TREASURE_JINGLE_ID) {
                this.events.pendingEntityEvents.add(new EntityCombatEvent(
                        slot,
                        EntityRomConstants.ENTITY_CHEST_WITH_ITEM,
                        0,
                        false,
                        EntityCombatEvent.SoundChannel.JINGLE,
                        EntityRomConstants.CHEST_TREASURE_JINGLE_ID));
            } else if (soundValue != 0) {
                this.presentationState.writePendingMusicTrack(soundValue);
            }
        }
        if (inertia == 0x26) {
            int roomId = this.presentationState.readSpriteSelection().roomId();
            int dialogLow = this.chestContentsTable.dialogLowIdFor(
                    itemType,
                    this.state.readChestShieldLevel(),
                    this.state.readChestSwordLevel(),
                    this.state.readChestPowerBraceletLevel(),
                    this.roomVariables.readEntityMapId() < 0 ? 0 : this.roomVariables.readEntityMapId(),
                    roomId < 0 ? 0 : roomId);
            int tableId = itemType == ChestContentsTable.CHEST_MESSAGE && roomId == 0x96 ? 1 : 0;
            if (dialogLow != EntityChestContents.NO_DIALOG)
                this.events.pendingDialogRequests.add(new DialogRequest(tableId, dialogLow));
        }
        if (inertia == 0x28) {
            this.lifecycle.clearEntity(slot);
            return true;
        }
        this.slots.replace(index, withPositionAndVariant(entity, entity.x(), y, itemType));
        return false;
    }

    void spawnChestZol(RoomEntity chest) {
        var runtime = this;
        int freeSlot = runtime.slots.findFree(runtime.slots.size() - 1);
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
        runtime.slots.replace(freeSlot, zol);
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

    void spawnEnemyDrop(RoomEntity source, int itemType) {
        var runtime = this;
        int freeSlot = runtime.slots.findFree(runtime.slots.size() - 1);
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
        runtime.slots.replace(freeSlot, drop);
        runtime.lifecycle.initializeHealthGroup(freeSlot);
        this.resetEnemyDropState(freeSlot);
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
        var runtime = this;
        int freeSlot = runtime.slots.findFree(runtime.slots.size() - 1);
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
        runtime.slots.replace(freeSlot, drop);
        runtime.lifecycle.initializeHealthGroup(freeSlot);
        this.resetEnemyDropState(freeSlot);
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

    public boolean hasHeartPiecePickup(int slot) {
        return state.readHeartPiecePickup(slot).started();
    }

    final DroppablePickupFrameStep droppablePickupFrameStep = new DroppablePickupFrameStep(this);

    static final class DroppablePickupFrameStep {
        private final linksawakening.world.entity.EntityPickupController owner;
        // Absent before initialization or while this reusable execution scratch is inactive.
        private linksawakening.world.entity.RoomEntity entity;
        private int linkEntityX;
        private int linkEntityY;
        // Absent before initialization or while this reusable execution scratch is inactive.
        private linksawakening.world.entity.RoomEntityObjectSample objectUnderEntity;
        private int slot;
        private int hiddenState;

        DroppablePickupFrameStep(linksawakening.world.entity.EntityPickupController owner) {
            this.owner = java.util.Objects.requireNonNull(owner, "owner");
        }

        linksawakening.world.entity.EntityPickupController.CommonDroppableUpdate run() {

            slot = entity.slot();
            hiddenState = owner.bossRewards.readDroppablePrivateState3(slot);
            if (hiddenState != 0) {
                boolean reveal = false;
                if (hiddenState == EntityRomConstants.DROPPABLE_HIDDEN_STATE_BURIED) {
                    // DroppableRevealOrReturnIfNeeded deliberately keeps ordinary
                    // buried drops invisible indoors. Outdoors, hearts reveal on
                    // short grass or a shovel hole; the remaining shared drops
                    // require the shovel-hole path and set privateState4 first.
                    if (!owner.indoorRoom) {
                        int objectId = !objectUnderEntity.sampled() ? 0xFF : objectUnderEntity.objectId() & 0xFF;
                        if (entity.type() == EntityRomConstants.ENTITY_DROPPABLE_HEART) {
                            reveal = objectId == EntityRomConstants.DROPPABLE_OBJECT_SHORT_GRASS
                                    || objectId == EntityRomConstants.DROPPABLE_OBJECT_SHOVEL_HOLE;
                        } else {
                            owner.bossRewards.writeDroppablePrivateState4(slot, 1);
                            reveal = objectId == EntityRomConstants.DROPPABLE_OBJECT_SHOVEL_HOLE;
                        }
                    }
                } else if (hiddenState == EntityRomConstants.DROPPABLE_HIDDEN_STATE_PEGASUS) {
                    reveal = owner.motions
                                    .get(SecretSeashellMotion.class, SecretSeashellMotion::new)
                                    .readSecretSeashellScreenShakeActive()
                            && owner.motions
                                    .get(SecretSeashellMotion.class, SecretSeashellMotion::new)
                                    .readSecretSeashellPegasusCollisionActive()
                            && (!owner.motions
                                            .get(SecretSeashellMotion.class, SecretSeashellMotion::new)
                                            .readSecretSeashellPegasusCollisionCoordinatesKnown()
                                    || (withinUnsignedWindow(
                                                    entity.x() + 0x08,
                                                    owner.motions
                                                            .get(SecretSeashellMotion.class, SecretSeashellMotion::new)
                                                            .readSecretSeashellPegasusCollisionX(),
                                                    0x10)
                                            && withinUnsignedWindow(
                                                    entity.y() + 0x08,
                                                    owner.motions
                                                            .get(SecretSeashellMotion.class, SecretSeashellMotion::new)
                                                            .readSecretSeashellPegasusCollisionY(),
                                                    0x10)));
                }

                if (!reveal) {
                    owner.physicsTables.writeEntityOptions1Override(
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

            owner.physicsTables.writeEntityOptions1Override(
                    slot,
                    hiddenState == 0
                            ? owner.physicsTables.readEntityOptions1Override(slot)
                            : EntityRomConstants.DROPPABLE_REVEALED_OPTIONS1);
            if (entity.type() != EntityRomConstants.ENTITY_DROPPABLE_FAIRY
                    && !owner.bossRewards.readEnemyDropActive(slot)) {
                owner.motions
                        .get(EnemyDropMotion.class, EnemyDropMotion::new)
                        .initialize(slot, owner.physicsTables.readGroundInteractionSideScrolling());
                owner.bossRewards.writeEnemyDropActive(slot, true);
            }
            return owner.commonUpdate(
                    withVariant(entity, entity.spriteDefinition().supported() ? 0 : -1), false);
        }

        private boolean inUse;

        linksawakening.world.entity.EntityPickupController.CommonDroppableUpdate run(
                linksawakening.world.entity.RoomEntity entity,
                int linkEntityX,
                int linkEntityY,
                linksawakening.world.entity.RoomEntityObjectSample objectUnderEntity) {
            if (inUse) {
                return new DroppablePickupFrameStep(owner).run(entity, linkEntityX, linkEntityY, objectUnderEntity);
            }
            inUse = true;

            slot = 0;
            hiddenState = 0;

            this.entity = entity;
            this.linkEntityX = linkEntityX;
            this.linkEntityY = linkEntityY;
            this.objectUnderEntity = objectUnderEntity;
            try {
                return run();
            } finally {
                inUse = false;

                this.entity = null;
                this.objectUnderEntity = null;
            }
        }
    }
}
