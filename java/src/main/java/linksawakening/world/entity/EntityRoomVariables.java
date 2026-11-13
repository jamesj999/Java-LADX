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
        switchBlockAnimationActive = value;
        return switchBlockAnimationActive;
    }

    boolean readPendingSwitchBlockAnimationRequest() {
        return pendingSwitchBlockAnimationRequest;
    }

    boolean writePendingSwitchBlockAnimationRequest(boolean value) {
        pendingSwitchBlockAnimationRequest = value;
        return pendingSwitchBlockAnimationRequest;
    }

    int readPendingClearedEntityMask() {
        return pendingClearedEntityMask;
    }

    int writePendingClearedEntityMask(int value) {
        pendingClearedEntityMask = value;
        return pendingClearedEntityMask;
    }

    int readPendingRoomStatusMask() {
        return pendingRoomStatusMask;
    }

    int writePendingRoomStatusMask(int value) {
        pendingRoomStatusMask = value;
        return pendingRoomStatusMask;
    }

    int readTransitionSequenceCounter() {
        return transitionSequenceCounter;
    }

    int writeTransitionSequenceCounter(int value) {
        transitionSequenceCounter = value;
        return transitionSequenceCounter;
    }

    boolean readGameplayWorld() {
        return gameplayWorld;
    }

    boolean writeGameplayWorld(boolean value) {
        gameplayWorld = value;
        return gameplayWorld;
    }

    boolean readRoomTransitionActive() {
        return roomTransitionActive;
    }

    boolean writeRoomTransitionActive(boolean value) {
        roomTransitionActive = value;
        return roomTransitionActive;
    }

    int readEntityGoldenLeavesCount() {
        return entityGoldenLeavesCount;
    }

    int writeEntityGoldenLeavesCount(int value) {
        entityGoldenLeavesCount = value;
        return entityGoldenLeavesCount;
    }

    int readInstrument1Flags() {
        return instrument1Flags;
    }

    int writeInstrument1Flags(int value) {
        instrument1Flags = value;
        return instrument1Flags;
    }

    int readInstrument2Flags() {
        return instrument2Flags;
    }

    int writeInstrument2Flags(int value) {
        instrument2Flags = value;
        return instrument2Flags;
    }

    int readEntityInteractionCountdown() {
        return entityInteractionCountdown;
    }

    int writeEntityInteractionCountdown(int value) {
        entityInteractionCountdown = value;
        return entityInteractionCountdown;
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
        freeMovementMode = value;
        return freeMovementMode;
    }

    int readEntityMapId() {
        return entityMapId;
    }

    int writeEntityMapId(int value) {
        entityMapId = value;
        return entityMapId;
    }

    int readEntityRoomId() {
        return entityRoomId;
    }

    int writeEntityRoomId(int value) {
        entityRoomId = value;
        return entityRoomId;
    }

    boolean readDungeonMinibossReady() {
        return dungeonMinibossReady;
    }

    boolean writeDungeonMinibossReady(boolean value) {
        dungeonMinibossReady = value;
        return dungeonMinibossReady;
    }

    int readEntityRoomStatus() {
        return entityRoomStatus;
    }

    int writeEntityRoomStatus(int value) {
        entityRoomStatus = value;
        return entityRoomStatus;
    }
}
