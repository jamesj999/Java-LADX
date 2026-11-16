package linksawakening.world.entity;

import java.util.Arrays;
import java.util.Objects;
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
        chestSpeedY[slot] = value;
        return chestSpeedY[slot];
    }

    int writeChestSpeedYAccumulator(int slot, int value) {
        chestSpeedYAccumulator[slot] = value;
        return chestSpeedYAccumulator[slot];
    }

    int[] storageChestSpeedYAccumulator() {
        return chestSpeedYAccumulator;
    }

    int readChestInertia(int slot) {
        return chestInertia[slot];
    }

    int writeChestInertia(int slot, int value) {
        chestInertia[slot] = value;
        return chestInertia[slot];
    }

    int readChestItemBySlot(int slot) {
        return chestItemBySlot[slot];
    }

    int writeChestItemBySlot(int slot, int value) {
        chestItemBySlot[slot] = value;
        return chestItemBySlot[slot];
    }

    int readChestShieldLevel() {
        return chestShieldLevel;
    }

    int writeChestShieldLevel(int value) {
        chestShieldLevel = value;
        return chestShieldLevel;
    }

    int readChestSwordLevel() {
        return chestSwordLevel;
    }

    int writeChestSwordLevel(int value) {
        chestSwordLevel = value;
        return chestSwordLevel;
    }

    boolean readChestPlayerLevelsKnown() {
        return chestPlayerLevelsKnown;
    }

    boolean writeChestPlayerLevelsKnown(boolean value) {
        chestPlayerLevelsKnown = value;
        return chestPlayerLevelsKnown;
    }

    int readChestPowerBraceletLevel() {
        return chestPowerBraceletLevel;
    }

    int writeChestPowerBraceletLevel(int value) {
        chestPowerBraceletLevel = value;
        return chestPowerBraceletLevel;
    }

    {
        Arrays.setAll(heartPiecePickup, ignored -> HeartPiecePickupMotion.inactive());
    }

    HeartPiecePickupMotion readHeartPiecePickup(int slot) {
        return heartPiecePickup[slot];
    }

    HeartPiecePickupMotion writeHeartPiecePickup(int slot, HeartPiecePickupMotion value) {
        heartPiecePickup[slot] = Objects.requireNonNull(value, "pickup sequence");
        return heartPiecePickup[slot];
    }

    HeartPiecePickupMotion[] storageHeartPiecePickup() {
        return heartPiecePickup;
    }

    int readHeartPiecesCount() {
        return heartPiecesCount;
    }

    int writeHeartPiecesCount(int value) {
        heartPiecesCount = value;
        return heartPiecesCount;
    }

    int postHeartPiecesCount(int delta) {
        int previous = heartPiecesCount;
        heartPiecesCount += delta;
        return previous;
    }

    int writeHeartPieceMeterPairs(int slot, int value) {
        heartPieceMeterPairs[slot] = value;
        return heartPieceMeterPairs[slot];
    }

    int[] storageHeartPieceMeterPairs() {
        return heartPieceMeterPairs;
    }
}
