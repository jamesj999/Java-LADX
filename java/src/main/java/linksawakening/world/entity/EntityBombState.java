package linksawakening.world.entity;

import java.util.Arrays;

/** Shared bombInteractions slot state, independent of behavior dispatch and construction order. */
final class EntityBombState {
    private final boolean[] bombFinalPresentationPending = new boolean[EntityRoomLoader.MAX_ENTITIES];

    private final int[] bombDirection = new int[EntityRoomLoader.MAX_ENTITIES];
    private final boolean[] playerArrowBombArrow = new boolean[EntityRoomLoader.MAX_ENTITIES];
    // ROM wEntitiesPrivateCountdown1Table.
    private final int[] bombCollisionGraceCountdown = new int[EntityRoomLoader.MAX_ENTITIES];
    // ROM wEntitiesPrivateCountdown3Table.
    private final int[] bombLiftDelay = new int[EntityRoomLoader.MAX_ENTITIES];
    // ROM wEntitiesPrivateState4Table.
    private final int[] bombOrigin = new int[EntityRoomLoader.MAX_ENTITIES];
    private final boolean[] placedBombMotionInitialized = new boolean[EntityRoomLoader.MAX_ENTITIES];
    private final boolean[] enemyBombMotionInitialized = new boolean[EntityRoomLoader.MAX_ENTITIES];
    private int bombArrowCooldown;
    private int latestDroppedBombEntityIndex = -1;
    private boolean lastBombPlacementPlayedBump;

    int readBombDirection(int slot) {
        return bombDirection[slot];
    }

    int writeBombDirection(int slot, int value) {
        bombDirection[slot] = value;
        return bombDirection[slot];
    }

    int[] storageBombDirection() {
        return bombDirection;
    }

    boolean readPlayerArrowBombArrow(int slot) {
        return playerArrowBombArrow[slot];
    }

    boolean writePlayerArrowBombArrow(int slot, boolean value) {
        playerArrowBombArrow[slot] = value;
        return playerArrowBombArrow[slot];
    }

    int readBombCollisionGraceCountdown(int slot) {
        return bombCollisionGraceCountdown[slot];
    }

    int writeBombCollisionGraceCountdown(int slot, int value) {
        bombCollisionGraceCountdown[slot] = value;
        return bombCollisionGraceCountdown[slot];
    }

    int postBombCollisionGraceCountdown(int slot, int delta) {
        int previous = bombCollisionGraceCountdown[slot];
        bombCollisionGraceCountdown[slot] += delta;
        return previous;
    }

    int readBombLiftDelay(int slot) {
        return bombLiftDelay[slot];
    }

    int writeBombLiftDelay(int slot, int value) {
        bombLiftDelay[slot] = value;
        return bombLiftDelay[slot];
    }

    int postBombLiftDelay(int slot, int delta) {
        int previous = bombLiftDelay[slot];
        bombLiftDelay[slot] += delta;
        return previous;
    }

    int readBombOrigin(int slot) {
        return bombOrigin[slot];
    }

    int writeBombOrigin(int slot, int value) {
        bombOrigin[slot] = value;
        return bombOrigin[slot];
    }

    boolean readPlacedBombMotionInitialized(int slot) {
        return placedBombMotionInitialized[slot];
    }

    boolean writePlacedBombMotionInitialized(int slot, boolean value) {
        placedBombMotionInitialized[slot] = value;
        return placedBombMotionInitialized[slot];
    }

    boolean readEnemyBombMotionInitialized(int slot) {
        return enemyBombMotionInitialized[slot];
    }

    boolean writeEnemyBombMotionInitialized(int slot, boolean value) {
        enemyBombMotionInitialized[slot] = value;
        return enemyBombMotionInitialized[slot];
    }

    int readBombArrowCooldown() {
        return bombArrowCooldown;
    }

    int writeBombArrowCooldown(int value) {
        bombArrowCooldown = value;
        return bombArrowCooldown;
    }

    int postBombArrowCooldown(int delta) {
        int previous = bombArrowCooldown;
        bombArrowCooldown += delta;
        return previous;
    }

    int readLatestDroppedBombEntityIndex() {
        return latestDroppedBombEntityIndex;
    }

    int writeLatestDroppedBombEntityIndex(int value) {
        latestDroppedBombEntityIndex = value;
        return latestDroppedBombEntityIndex;
    }

    boolean readLastBombPlacementPlayedBump() {
        return lastBombPlacementPlayedBump;
    }

    boolean writeLastBombPlacementPlayedBump(boolean value) {
        lastBombPlacementPlayedBump = value;
        return lastBombPlacementPlayedBump;
    }

    boolean bombFinalPresentationPending(int slot) {
        return bombFinalPresentationPending[slot];
    }

    void setBombFinalPresentationPending(int slot, boolean value) {
        bombFinalPresentationPending[slot] = value;
    }

    void clearBombFinalPresentations() {
        Arrays.fill(bombFinalPresentationPending, false);
    }
}
