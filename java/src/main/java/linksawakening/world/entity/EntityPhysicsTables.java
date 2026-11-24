package linksawakening.world.entity;

import java.util.Arrays;
import java.util.Objects;
import java.util.function.IntUnaryOperator;

/** Owned PhysicsTables storage; preserves ROM slot indexing. */
final class EntityPhysicsTables {
    private final IntUnaryOperator initialPhysicsFlagsLookup;
    private RoomEntityGroundInteraction groundInteraction = RoomEntityGroundInteraction.NONE;
    private RoomEntityBackgroundInteraction backgroundInteraction = RoomEntityBackgroundInteraction.NONE;
    private final int[] slowTransitionCountdown = new int[EntityRoomLoader.MAX_ENTITIES];
    private final boolean[] slowTimerInitialized = new boolean[EntityRoomLoader.MAX_ENTITIES];
    private final int[] enemyTransitionCountdown = new int[EntityRoomLoader.MAX_ENTITIES];
    private final int[] enemyPhysicsFlags = new int[EntityRoomLoader.MAX_ENTITIES];
    private final int[] entityGroundStatus = new int[EntityRoomLoader.MAX_ENTITIES];
    private final int[] fallingTargetX = new int[EntityRoomLoader.MAX_ENTITIES];
    private final int[] fallingTargetY = new int[EntityRoomLoader.MAX_ENTITIES];
    private final int[] fallingSpeedX = new int[EntityRoomLoader.MAX_ENTITIES];
    private final int[] fallingSpeedY = new int[EntityRoomLoader.MAX_ENTITIES];
    private final int[] fallingSpeedXAccumulator = new int[EntityRoomLoader.MAX_ENTITIES];
    private final int[] fallingSpeedYAccumulator = new int[EntityRoomLoader.MAX_ENTITIES];
    private final int[] fallingVisualYOffset = new int[EntityRoomLoader.MAX_ENTITIES];
    private final int[] baseEntityFlipAttribute = new int[EntityRoomLoader.MAX_ENTITIES];
    private final int[] entityOptions1Override = new int[EntityRoomLoader.MAX_ENTITIES];
    private final boolean[] enemyProjectileSpawnedThisFrame = new boolean[EntityRoomLoader.MAX_ENTITIES];
    private final boolean[] dynamicEntitySpawnedThisFrame = new boolean[EntityRoomLoader.MAX_ENTITIES];
    // ROM wEntitiesPrivateState4Table: handler-dependent register; a narrower meaning has not been established.
    private final int[] entityPrivateState4 = new int[EntityRoomLoader.MAX_ENTITIES];
    private final int[] entityInertia = new int[EntityRoomLoader.MAX_ENTITIES];
    private final int[] entityUnknownJ = new int[EntityRoomLoader.MAX_ENTITIES];
    private boolean groundInteractionSideScrolling;
    private RoomEntityObjectQuery objectQuery = RoomEntityObjectQuery.NONE;
    private RoomEntityObjectQuery objectIntersectionQuery = RoomEntityObjectQuery.NONE;

    EntityPhysicsTables(IntUnaryOperator initialPhysicsFlagsLookup) {
        this.initialPhysicsFlagsLookup = initialPhysicsFlagsLookup;
    }

    void initializeEnemyPhysicsFlags(int slot, int type) {
        writeEnemyPhysicsFlags(slot, EntityTypeRules.initialPhysicsFlags(type, initialPhysicsFlagsLookup));
    }

    RoomEntityGroundInteraction readGroundInteraction() {
        return groundInteraction;
    }

    RoomEntityGroundInteraction writeGroundInteraction(RoomEntityGroundInteraction value) {
        groundInteraction = Objects.requireNonNullElse(value, RoomEntityGroundInteraction.NONE);
        return groundInteraction;
    }

    boolean hasRichBackgroundInteraction() {
        return backgroundInteraction != RoomEntityBackgroundInteraction.NONE;
    }

    RoomEntityBackgroundInteraction readBackgroundInteraction() {
        return backgroundInteraction;
    }

    RoomEntityBackgroundInteraction writeBackgroundInteraction(RoomEntityBackgroundInteraction value) {
        backgroundInteraction = RoomEntityBackgroundInteraction.orNone(value);
        return backgroundInteraction;
    }

    int readSlowTransitionCountdown(int slot) {
        return slowTransitionCountdown[slot];
    }

    int writeSlowTransitionCountdown(int slot, int value) {
        slowTransitionCountdown[slot] = value;
        return slowTransitionCountdown[slot];
    }

    int postSlowTransitionCountdown(int slot, int delta) {
        int previous = slowTransitionCountdown[slot];
        slowTransitionCountdown[slot] += delta;
        return previous;
    }

    boolean readSlowTimerInitialized(int slot) {
        return slowTimerInitialized[slot];
    }

    boolean writeSlowTimerInitialized(int slot, boolean value) {
        slowTimerInitialized[slot] = value;
        return slowTimerInitialized[slot];
    }

    int readEnemyTransitionCountdown(int slot) {
        return enemyTransitionCountdown[slot];
    }

    int writeEnemyTransitionCountdown(int slot, int value) {
        enemyTransitionCountdown[slot] = value;
        return enemyTransitionCountdown[slot];
    }

    int postEnemyTransitionCountdown(int slot, int delta) {
        int previous = enemyTransitionCountdown[slot];
        enemyTransitionCountdown[slot] += delta;
        return previous;
    }

    int readEnemyPhysicsFlags(int slot) {
        return enemyPhysicsFlags[slot];
    }

    int writeEnemyPhysicsFlags(int slot, int value) {
        enemyPhysicsFlags[slot] = value;
        return enemyPhysicsFlags[slot];
    }

    int readEntityGroundStatus(int slot) {
        return entityGroundStatus[slot];
    }

    int writeEntityGroundStatus(int slot, int value) {
        entityGroundStatus[slot] = value;
        return entityGroundStatus[slot];
    }

    int slotCount() {
        return entityGroundStatus.length;
    }

    int readFallingTargetX(int slot) {
        return fallingTargetX[slot];
    }

    int writeFallingTargetX(int slot, int value) {
        fallingTargetX[slot] = value;
        return fallingTargetX[slot];
    }

    int readFallingTargetY(int slot) {
        return fallingTargetY[slot];
    }

    int writeFallingTargetY(int slot, int value) {
        fallingTargetY[slot] = value;
        return fallingTargetY[slot];
    }

    int readFallingSpeedX(int slot) {
        return fallingSpeedX[slot];
    }

    int writeFallingSpeedX(int slot, int value) {
        fallingSpeedX[slot] = value;
        return fallingSpeedX[slot];
    }

    int readFallingSpeedY(int slot) {
        return fallingSpeedY[slot];
    }

    int writeFallingSpeedY(int slot, int value) {
        fallingSpeedY[slot] = value;
        return fallingSpeedY[slot];
    }

    int writeFallingSpeedXAccumulator(int slot, int value) {
        fallingSpeedXAccumulator[slot] = value;
        return fallingSpeedXAccumulator[slot];
    }

    int advanceFallingX(int position, int speed, int slot) {
        return EntityMotionMath.addFallingSpeedToPosition(position, speed, fallingSpeedXAccumulator, slot);
    }

    int writeFallingSpeedYAccumulator(int slot, int value) {
        fallingSpeedYAccumulator[slot] = value;
        return fallingSpeedYAccumulator[slot];
    }

    int advanceFallingY(int position, int speed, int slot) {
        return EntityMotionMath.addFallingSpeedToPosition(position, speed, fallingSpeedYAccumulator, slot);
    }

    int readFallingVisualYOffset(int slot) {
        return fallingVisualYOffset[slot];
    }

    int writeFallingVisualYOffset(int slot, int value) {
        fallingVisualYOffset[slot] = value;
        return fallingVisualYOffset[slot];
    }

    int[] copyFallingVisualYOffset() {
        return fallingVisualYOffset.clone();
    }

    int readBaseEntityFlipAttribute(int slot) {
        return baseEntityFlipAttribute[slot];
    }

    int writeBaseEntityFlipAttribute(int slot, int value) {
        baseEntityFlipAttribute[slot] = value;
        return baseEntityFlipAttribute[slot];
    }

    int readEntityOptions1Override(int slot) {
        return entityOptions1Override[slot];
    }

    int writeEntityOptions1Override(int slot, int value) {
        entityOptions1Override[slot] = value;
        return entityOptions1Override[slot];
    }

    void resetOptionsOverrides() {
        Arrays.fill(entityOptions1Override, -1);
    }

    boolean readEnemyProjectileSpawnedThisFrame(int slot) {
        return enemyProjectileSpawnedThisFrame[slot];
    }

    boolean writeEnemyProjectileSpawnedThisFrame(int slot, boolean value) {
        enemyProjectileSpawnedThisFrame[slot] = value;
        return enemyProjectileSpawnedThisFrame[slot];
    }

    void clearEnemyProjectileSpawnedThisFrame() {
        Arrays.fill(enemyProjectileSpawnedThisFrame, false);
    }

    boolean readDynamicEntitySpawnedThisFrame(int slot) {
        return dynamicEntitySpawnedThisFrame[slot];
    }

    boolean writeDynamicEntitySpawnedThisFrame(int slot, boolean value) {
        dynamicEntitySpawnedThisFrame[slot] = value;
        return dynamicEntitySpawnedThisFrame[slot];
    }

    void clearDynamicEntitySpawnedThisFrame() {
        Arrays.fill(dynamicEntitySpawnedThisFrame, false);
    }

    int readEntityPrivateState4(int slot) {
        return entityPrivateState4[slot];
    }

    int writeEntityPrivateState4(int slot, int value) {
        entityPrivateState4[slot] = value;
        return entityPrivateState4[slot];
    }

    int readEntityInertia(int slot) {
        return entityInertia[slot];
    }

    int writeEntityInertia(int slot, int value) {
        entityInertia[slot] = value;
        return entityInertia[slot];
    }

    int readEntityUnknownJ(int slot) {
        return entityUnknownJ[slot];
    }

    int writeEntityUnknownJ(int slot, int value) {
        entityUnknownJ[slot] = value;
        return entityUnknownJ[slot];
    }

    boolean readGroundInteractionSideScrolling() {
        return groundInteractionSideScrolling;
    }

    boolean writeGroundInteractionSideScrolling(boolean value) {
        groundInteractionSideScrolling = value;
        return groundInteractionSideScrolling;
    }

    RoomEntityObjectQuery readObjectQuery() {
        return objectQuery;
    }

    RoomEntityObjectQuery writeObjectQuery(RoomEntityObjectQuery value) {
        objectQuery = RoomEntityObjectQuery.orNone(value);
        return objectQuery;
    }

    RoomEntityObjectQuery readObjectIntersectionQuery() {
        return objectIntersectionQuery == RoomEntityObjectQuery.NONE ? objectQuery : objectIntersectionQuery;
    }

    RoomEntityObjectQuery writeObjectIntersectionQuery(RoomEntityObjectQuery value) {
        objectIntersectionQuery = RoomEntityObjectQuery.orNone(value);
        return objectIntersectionQuery;
    }
}
