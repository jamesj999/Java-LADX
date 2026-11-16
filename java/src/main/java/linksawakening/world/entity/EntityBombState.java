package linksawakening.world.entity;

import java.util.Arrays;

/** Shared bombInteractions slot state, independent of behavior dispatch and construction order. */
final class EntityBombState {
    private final boolean[] bombFinalPresentationPending = new boolean[EntityRoomLoader.MAX_ENTITIES];

    private final int[] bombDirection = new int[EntityRoomLoader.MAX_ENTITIES];
    private final boolean[] playerArrowBombArrow = new boolean[EntityRoomLoader.MAX_ENTITIES];
    private final int[] bombPrivateCountdown1 = new int[EntityRoomLoader.MAX_ENTITIES];
    private final int[] bombPrivateCountdown3 = new int[EntityRoomLoader.MAX_ENTITIES];
    private final int[] bombPrivateState4 = new int[EntityRoomLoader.MAX_ENTITIES];
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

    int readBombPrivateCountdown1(int slot) {
        return bombPrivateCountdown1[slot];
    }

    int writeBombPrivateCountdown1(int slot, int value) {
        bombPrivateCountdown1[slot] = value;
        return bombPrivateCountdown1[slot];
    }

    int postBombPrivateCountdown1(int slot, int delta) {
        int previous = bombPrivateCountdown1[slot];
        bombPrivateCountdown1[slot] += delta;
        return previous;
    }

    int readBombPrivateCountdown3(int slot) {
        return bombPrivateCountdown3[slot];
    }

    int writeBombPrivateCountdown3(int slot, int value) {
        bombPrivateCountdown3[slot] = value;
        return bombPrivateCountdown3[slot];
    }

    int postBombPrivateCountdown3(int slot, int delta) {
        int previous = bombPrivateCountdown3[slot];
        bombPrivateCountdown3[slot] += delta;
        return previous;
    }

    int readBombPrivateState4(int slot) {
        return bombPrivateState4[slot];
    }

    int writeBombPrivateState4(int slot, int value) {
        bombPrivateState4[slot] = value;
        return bombPrivateState4[slot];
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
