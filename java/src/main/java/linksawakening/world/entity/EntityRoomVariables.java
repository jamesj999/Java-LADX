package linksawakening.world.entity;

/** Owned RoomVariables storage; preserves ROM slot indexing. */
final class EntityRoomVariables {
    private boolean switchBlockAnimationActive;
    private boolean pendingSwitchBlockAnimationRequest;
    private int pendingClearedEntityMask;
    private int pendingRoomStatusMask;
    private int transitionSequenceCounter = 0x04;
    private boolean gameplayWorld = true;
    private boolean roomTransitionActive;
    private int entityGoldenLeavesCount;
    private int instrument1Flags;
    private int instrument2Flags;
    private int entityInteractionCountdown;
    private boolean freeMovementMode;
    private int entityMapId = -1;
    private int entityRoomId = -1;
    private boolean dungeonMinibossReady;
    private int entityRoomStatus;

    boolean readSwitchBlockAnimationActive() {
        return switchBlockAnimationActive;
    }

    boolean writeSwitchBlockAnimationActive(boolean value) {
        return switchBlockAnimationActive = value;
    }

    boolean readPendingSwitchBlockAnimationRequest() {
        return pendingSwitchBlockAnimationRequest;
    }

    boolean writePendingSwitchBlockAnimationRequest(boolean value) {
        return pendingSwitchBlockAnimationRequest = value;
    }

    int readPendingClearedEntityMask() {
        return pendingClearedEntityMask;
    }

    int writePendingClearedEntityMask(int value) {
        return pendingClearedEntityMask = value;
    }

    int readPendingRoomStatusMask() {
        return pendingRoomStatusMask;
    }

    int writePendingRoomStatusMask(int value) {
        return pendingRoomStatusMask = value;
    }

    int readTransitionSequenceCounter() {
        return transitionSequenceCounter;
    }

    int writeTransitionSequenceCounter(int value) {
        return transitionSequenceCounter = value;
    }

    boolean readGameplayWorld() {
        return gameplayWorld;
    }

    boolean writeGameplayWorld(boolean value) {
        return gameplayWorld = value;
    }

    boolean readRoomTransitionActive() {
        return roomTransitionActive;
    }

    boolean writeRoomTransitionActive(boolean value) {
        return roomTransitionActive = value;
    }

    int readEntityGoldenLeavesCount() {
        return entityGoldenLeavesCount;
    }

    int writeEntityGoldenLeavesCount(int value) {
        return entityGoldenLeavesCount = value;
    }

    int readInstrument1Flags() {
        return instrument1Flags;
    }

    int writeInstrument1Flags(int value) {
        return instrument1Flags = value;
    }

    int readInstrument2Flags() {
        return instrument2Flags;
    }

    int writeInstrument2Flags(int value) {
        return instrument2Flags = value;
    }

    int readEntityInteractionCountdown() {
        return entityInteractionCountdown;
    }

    int writeEntityInteractionCountdown(int value) {
        return entityInteractionCountdown = value;
    }

    int postEntityInteractionCountdown(int delta) {
        int previous = entityInteractionCountdown;
        entityInteractionCountdown += delta;
        return previous;
    }

    boolean readFreeMovementMode() {
        return freeMovementMode;
    }

    boolean writeFreeMovementMode(boolean value) {
        return freeMovementMode = value;
    }

    int readEntityMapId() {
        return entityMapId;
    }

    int writeEntityMapId(int value) {
        return entityMapId = value;
    }

    int readEntityRoomId() {
        return entityRoomId;
    }

    int writeEntityRoomId(int value) {
        return entityRoomId = value;
    }

    boolean readDungeonMinibossReady() {
        return dungeonMinibossReady;
    }

    boolean writeDungeonMinibossReady(boolean value) {
        return dungeonMinibossReady = value;
    }

    int readEntityRoomStatus() {
        return entityRoomStatus;
    }

    int writeEntityRoomStatus(int value) {
        return entityRoomStatus = value;
    }
}
