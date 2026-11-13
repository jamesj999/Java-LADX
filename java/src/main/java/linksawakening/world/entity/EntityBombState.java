package linksawakening.world.entity;

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
        return bombDirection[slot] = value;
    }

    int[] storageBombDirection() {
        return bombDirection;
    }

    boolean readPlayerArrowBombArrow(int slot) {
        return playerArrowBombArrow[slot];
    }

    boolean writePlayerArrowBombArrow(int slot, boolean value) {
        return playerArrowBombArrow[slot] = value;
    }

    int readBombPrivateCountdown1(int slot) {
        return bombPrivateCountdown1[slot];
    }

    int writeBombPrivateCountdown1(int slot, int value) {
        return bombPrivateCountdown1[slot] = value;
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
        return bombPrivateCountdown3[slot] = value;
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
        return bombPrivateState4[slot] = value;
    }

    boolean readPlacedBombMotionInitialized(int slot) {
        return placedBombMotionInitialized[slot];
    }

    boolean writePlacedBombMotionInitialized(int slot, boolean value) {
        return placedBombMotionInitialized[slot] = value;
    }

    boolean readEnemyBombMotionInitialized(int slot) {
        return enemyBombMotionInitialized[slot];
    }

    boolean writeEnemyBombMotionInitialized(int slot, boolean value) {
        return enemyBombMotionInitialized[slot] = value;
    }

    int readBombArrowCooldown() {
        return bombArrowCooldown;
    }

    int writeBombArrowCooldown(int value) {
        return bombArrowCooldown = value;
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
        return latestDroppedBombEntityIndex = value;
    }

    boolean readLastBombPlacementPlayedBump() {
        return lastBombPlacementPlayedBump;
    }

    boolean writeLastBombPlacementPlayedBump(boolean value) {
        return lastBombPlacementPlayedBump = value;
    }

    boolean bombFinalPresentationPending(int slot) {
        return bombFinalPresentationPending[slot];
    }

    void setBombFinalPresentationPending(int slot, boolean value) {
        bombFinalPresentationPending[slot] = value;
    }

    void clearBombFinalPresentations() {
        java.util.Arrays.fill(bombFinalPresentationPending, false);
    }
}
