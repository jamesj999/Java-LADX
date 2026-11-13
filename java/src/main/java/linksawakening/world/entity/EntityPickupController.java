package linksawakening.world.entity;

import static linksawakening.world.entity.EntityMotionMath.*;
import static linksawakening.world.entity.EntityRomConstants.*;
import static linksawakening.world.entity.EntitySnapshots.*;
import static linksawakening.world.entity.EntityTypeRules.*;

import linksawakening.world.entity.RoomEntityRuntime.CommonDroppableUpdate;
import linksawakening.world.entity.RoomEntityRuntime.SlimeKeyTransitionUpdate;
import linksawakening.world.entity.motion.*;

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
        DroppablePickupBehavior.initializeCommonDroppable(this, entity);
    }

    public CommonDroppableUpdate advanceCommonDroppable(
            RoomEntity entity, int linkEntityX, int linkEntityY, RoomEntityObjectSample objectUnderEntity) {
        return DroppablePickupBehavior.advanceCommonDroppable(
                this, entity, linkEntityX, linkEntityY, objectUnderEntity);
    }

    void revealCommonDroppable(RoomEntity entity, int linkEntityX, int linkEntityY) {
        DroppablePickupBehavior.revealCommonDroppable(this, entity, linkEntityX, linkEntityY);
    }

    public SlimeKeyTransitionUpdate advanceHidingSlimeKey(
            RoomEntity entity, int slot, int linkEntityX, int linkEntityY, int linkZ) {
        return DroppablePickupBehavior.advanceHidingSlimeKey(this, entity, slot, linkEntityX, linkEntityY, linkZ);
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
        DroppablePickupBehavior.advanceHeartPiecePickup(this, entity, x, y, z);
    }

    public void initializeChestEntity(RoomEntity entity) {
        ChestPickupBehavior.initializeChestEntity(this, entity);
    }

    public boolean advanceChestEntity(int index, RoomEntity entity) {
        return ChestPickupBehavior.advanceChestEntity(this, index, entity);
    }

    void spawnChestZol(RoomEntity chest) {
        ChestPickupBehavior.spawnChestZol(this, chest);
    }

    void spawnEnemyDrop(RoomEntity source, int itemType) {
        EnemyDropSpawns.spawnEnemyDrop(this, source, itemType);
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
        return EnemyDropSpawns.spawnKeyDropAt(this, itemType, x, y, spriteVariant, z, speedZ, privateCountdown1);
    }

    public boolean hasHeartPiecePickup(int slot) {
        return state.readHeartPiecePickup(slot) != null;
    }

    final DroppablePickupFrameStep droppablePickupFrameStep = new DroppablePickupFrameStep();
}
