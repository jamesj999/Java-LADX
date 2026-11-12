package linksawakening.world.entity;

import static linksawakening.world.entity.EntityMotionMath.*;
import static linksawakening.world.entity.EntityRomConstants.*;
import static linksawakening.world.entity.EntitySnapshots.*;
import static linksawakening.world.entity.EntityTypeRules.*;

import linksawakening.world.entity.RoomEntityRuntime.CommonDroppableUpdate;
import linksawakening.world.entity.RoomEntityRuntime.SlimeKeyTransitionUpdate;
import linksawakening.world.entity.motion.*;
import linksawakening.world.entity.motion.HeartPiecePickupMotion;

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
    final EntityPickupControllerServices runtime;

    EntityPickupController(EntityPickupControllerServices runtime) {
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
        runtime.bossRewards().writeDroppedItemBySlot(slot, 0);
        runtime.bossRewards().writeDropPrivateCountdown1(slot, 0);
        runtime.bossRewards().writeDropPrivateCountdown3(slot, 0);
        runtime.bossRewards().writeDroppablePrivateState3(slot, 0);
        runtime.bossRewards().writeDroppablePrivateState4(slot, 0);
        runtime.bossRewards().writeEnemyDropActive(slot, false);
        runtime.motions().get(EnemyDropMotion.class, EnemyDropMotion::new).clear(slot);
        runtime.motions().get(FairyMotion.class, FairyMotion::new).clear(slot);
        runtime.motions()
                .get(HookshotChainMotion.class, HookshotChainMotion::new)
                .clear(slot);
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

    final DroppablePickupFrameStep droppablePickupFrameStep = new DroppablePickupFrameStep();
}
