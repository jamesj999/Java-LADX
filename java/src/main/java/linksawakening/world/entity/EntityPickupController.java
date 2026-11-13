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
    final EntityPickupState state;
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

    final EntityPickupControllerServices runtime;

    EntityPickupController(EntityPickupState state, EntityPickupControllerServices runtime) {
        this.state = java.util.Objects.requireNonNull(state, "state");
        this.runtime = runtime;
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
        runtime.physicsTables().writeEnemyPhysicsFlags(slot, initialPhysicsFlags(entity.type()));
        if (isCommonDroppableType(entity.type())) {
            initializeCommonDroppable(entity);
        }
        if (entity.type() == EntityRomConstants.ENTITY_DROPPABLE_SECRET_SEASHELL) {
            runtime.motions()
                    .get(SecretSeashellMotion.class, SecretSeashellMotion::new)
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
        runtime.bossRewards().state.writeDroppedItemBySlot(slot, 0);
        runtime.bossRewards().state.writeDropPrivateCountdown1(slot, 0);
        runtime.bossRewards().state.writeDropPrivateCountdown3(slot, 0);
        runtime.bossRewards().state.writeDroppablePrivateState3(slot, 0);
        runtime.bossRewards().state.writeDroppablePrivateState4(slot, 0);
        runtime.bossRewards().state.writeEnemyDropActive(slot, false);
        runtime.motions().get(EnemyDropMotion.class, EnemyDropMotion::new).clear(slot);
        runtime.motions().get(FairyMotion.class, FairyMotion::new).clear(slot);
        runtime.motions()
                .get(HookshotChainMotion.class, HookshotChainMotion::new)
                .clear(slot);
    }

    void decrementEnemyDropCountdowns(RoomEntity entity) {
        int slot = entity.slot();
        if (runtime.bossRewards().state.readDropPrivateCountdown1(slot) > 0) {
            runtime.bossRewards().state.postDropPrivateCountdown1(slot, -1);
        }
        if (runtime.bossRewards().state.readDropPrivateCountdown3(slot) > 0) {
            runtime.bossRewards().state.postDropPrivateCountdown3(slot, -1);
        }
    }

    public boolean checkForKeyDropQuicksandHole(RoomEntity entity) {
        if (runtime.indoorRoom() || runtime.roomVariables().readEntityRoomId() != 0xCE || entity.z() != 0) {
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
