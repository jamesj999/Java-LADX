package linksawakening.world.entity;

/** Owned PhysicsTables storage; preserves ROM slot indexing. */
final class EntityPhysicsTables {
    private RoomEntityGroundInteraction groundInteraction =
            (entity, frameCounter, previousGroundStatus, speedZ, sideScrolling) ->
                    RoomEntityGroundInteraction.Result.unchanged(entity, 0);
    private RoomEntityBackgroundInteraction backgroundInteraction;
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
    private final int[] entityPrivateState4 = new int[EntityRoomLoader.MAX_ENTITIES];
    private final int[] entityInertia = new int[EntityRoomLoader.MAX_ENTITIES];
    private final int[] entityUnknownJ = new int[EntityRoomLoader.MAX_ENTITIES];
    private boolean groundInteractionSideScrolling;
    private RoomEntityObjectQuery objectQuery;
    private RoomEntityObjectQuery objectIntersectionQuery;

    RoomEntityGroundInteraction readGroundInteraction() {
        return groundInteraction;
    }

    RoomEntityGroundInteraction writeGroundInteraction(RoomEntityGroundInteraction value) {
        return groundInteraction = value;
    }

    RoomEntityBackgroundInteraction readBackgroundInteraction() {
        return backgroundInteraction;
    }

    RoomEntityBackgroundInteraction writeBackgroundInteraction(RoomEntityBackgroundInteraction value) {
        return backgroundInteraction = value;
    }

    int readSlowTransitionCountdown(int slot) {
        return slowTransitionCountdown[slot];
    }

    int writeSlowTransitionCountdown(int slot, int value) {
        return slowTransitionCountdown[slot] = value;
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
        return slowTimerInitialized[slot] = value;
    }

    int readEnemyTransitionCountdown(int slot) {
        return enemyTransitionCountdown[slot];
    }

    int writeEnemyTransitionCountdown(int slot, int value) {
        return enemyTransitionCountdown[slot] = value;
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
        return enemyPhysicsFlags[slot] = value;
    }

    int readEntityGroundStatus(int slot) {
        return entityGroundStatus[slot];
    }

    int writeEntityGroundStatus(int slot, int value) {
        return entityGroundStatus[slot] = value;
    }

    int slotCount() {
        return entityGroundStatus.length;
    }

    int readFallingTargetX(int slot) {
        return fallingTargetX[slot];
    }

    int writeFallingTargetX(int slot, int value) {
        return fallingTargetX[slot] = value;
    }

    int readFallingTargetY(int slot) {
        return fallingTargetY[slot];
    }

    int writeFallingTargetY(int slot, int value) {
        return fallingTargetY[slot] = value;
    }

    int readFallingSpeedX(int slot) {
        return fallingSpeedX[slot];
    }

    int writeFallingSpeedX(int slot, int value) {
        return fallingSpeedX[slot] = value;
    }

    int readFallingSpeedY(int slot) {
        return fallingSpeedY[slot];
    }

    int writeFallingSpeedY(int slot, int value) {
        return fallingSpeedY[slot] = value;
    }

    int writeFallingSpeedXAccumulator(int slot, int value) {
        return fallingSpeedXAccumulator[slot] = value;
    }

    int advanceFallingX(int position, int speed, int slot) {
        return EntityMotionMath.addFallingSpeedToPosition(position, speed, fallingSpeedXAccumulator, slot);
    }

    int writeFallingSpeedYAccumulator(int slot, int value) {
        return fallingSpeedYAccumulator[slot] = value;
    }

    int advanceFallingY(int position, int speed, int slot) {
        return EntityMotionMath.addFallingSpeedToPosition(position, speed, fallingSpeedYAccumulator, slot);
    }

    int readFallingVisualYOffset(int slot) {
        return fallingVisualYOffset[slot];
    }

    int writeFallingVisualYOffset(int slot, int value) {
        return fallingVisualYOffset[slot] = value;
    }

    int[] copyFallingVisualYOffset() {
        return fallingVisualYOffset.clone();
    }

    int readBaseEntityFlipAttribute(int slot) {
        return baseEntityFlipAttribute[slot];
    }

    int writeBaseEntityFlipAttribute(int slot, int value) {
        return baseEntityFlipAttribute[slot] = value;
    }

    int readEntityOptions1Override(int slot) {
        return entityOptions1Override[slot];
    }

    int writeEntityOptions1Override(int slot, int value) {
        return entityOptions1Override[slot] = value;
    }

    void resetOptionsOverrides() {
        java.util.Arrays.fill(entityOptions1Override, -1);
    }

    boolean readEnemyProjectileSpawnedThisFrame(int slot) {
        return enemyProjectileSpawnedThisFrame[slot];
    }

    boolean writeEnemyProjectileSpawnedThisFrame(int slot, boolean value) {
        return enemyProjectileSpawnedThisFrame[slot] = value;
    }

    void clearEnemyProjectileSpawnedThisFrame() {
        java.util.Arrays.fill(enemyProjectileSpawnedThisFrame, false);
    }

    boolean readDynamicEntitySpawnedThisFrame(int slot) {
        return dynamicEntitySpawnedThisFrame[slot];
    }

    boolean writeDynamicEntitySpawnedThisFrame(int slot, boolean value) {
        return dynamicEntitySpawnedThisFrame[slot] = value;
    }

    void clearDynamicEntitySpawnedThisFrame() {
        java.util.Arrays.fill(dynamicEntitySpawnedThisFrame, false);
    }

    int readEntityPrivateState4(int slot) {
        return entityPrivateState4[slot];
    }

    int writeEntityPrivateState4(int slot, int value) {
        return entityPrivateState4[slot] = value;
    }

    int readEntityInertia(int slot) {
        return entityInertia[slot];
    }

    int writeEntityInertia(int slot, int value) {
        return entityInertia[slot] = value;
    }

    int readEntityUnknownJ(int slot) {
        return entityUnknownJ[slot];
    }

    int writeEntityUnknownJ(int slot, int value) {
        return entityUnknownJ[slot] = value;
    }

    boolean readGroundInteractionSideScrolling() {
        return groundInteractionSideScrolling;
    }

    boolean writeGroundInteractionSideScrolling(boolean value) {
        return groundInteractionSideScrolling = value;
    }

    RoomEntityObjectQuery readObjectQuery() {
        return objectQuery;
    }

    RoomEntityObjectQuery writeObjectQuery(RoomEntityObjectQuery value) {
        return objectQuery = value;
    }

    RoomEntityObjectQuery readObjectIntersectionQuery() {
        return objectIntersectionQuery;
    }

    RoomEntityObjectQuery writeObjectIntersectionQuery(RoomEntityObjectQuery value) {
        return objectIntersectionQuery = value;
    }
}
