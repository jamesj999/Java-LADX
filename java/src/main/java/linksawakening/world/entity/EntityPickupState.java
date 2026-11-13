package linksawakening.world.entity;

import linksawakening.world.entity.motion.HeartPiecePickupMotion;

/** Shared pickups slot state, independent of behavior dispatch and construction order. */
final class EntityPickupState {
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
}
